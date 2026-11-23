package com.brica.client.presentation.extraction;

import com.brica.client.api.*;
import com.brica.client.api.ExtractionDtos.*;
import com.brica.client.config.ClientConfig;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Independent capture/review window. All business changes go through server commands. */
public final class ExtractionWindow {
    private final long workId; private final Stage stage=new Stage();
    private final ExtractionApiClient api=new ExtractionApiClient(ClientConfig.serverBaseUri());
    private final WorksApiClient works=new WorksApiClient(ClientConfig.serverBaseUri());
    private final Label status=new Label(),state=new Label("Selecciona fuentes o retoma un borrador."),evidence=new Label();
    private final VBox root=new VBox(12); private final TabPane tabs=new TabPane();
    private final ListView<WorksApiClient.Attachment> sources=new ListView<>();
    private final ListView<Job> jobs=new ListView<>();
    private final ObservableList<Decision> decisions=FXCollections.observableArrayList();
    private final ObservableList<Budget> drafts=FXCollections.observableArrayList();
    private final TableView<Decision> decisionsTable=new TableView<>(decisions);
    private final ListView<Budget> draftList=new ListView<>(drafts);
    private final TableView<Fragment> candidateTable=new TableView<>();
    private final Map<UUID,List<Suggestion>> suggestionsByFragment=new HashMap<>();
    private final TextArea original=new TextArea(),confirmationLog=new TextArea();
    private final List<Fragment> candidates=new ArrayList<>(); private List<BudgetView> existing=List.of();
    private final Map<String,UUID> operations=new HashMap<>();
    private Context context,rebaseContext; private View current; private boolean busy,dirty; private long generation,requestGeneration;
    private final Timeline polling=new Timeline(new KeyFrame(Duration.seconds(2),e->poll()));
    private final Button save=new Button("Guardar borrador"),confirmWork=new Button("Confirmar obra"),confirmClient=new Button("Confirmar cliente"),confirmBudget=new Button("Confirmar presupuesto seleccionado");
    private final ComboBox<BudgetView> savedBudgets=new ComboBox<>(); private final ComboBox<BudgetRevision> revisions=new ComboBox<>();
    private final TableView<Item> savedItems=new TableView<>(); private final TextArea revisionInfo=new TextArea(); private int itemPage;

    public static void show(Window owner,long workId,String name,Long attachmentId) { new ExtractionWindow(owner,workId,name,attachmentId).stage.show(); }
    private ExtractionWindow(Window owner,long workId,String name,Long attachmentId) {
        this.workId=workId; stage.initOwner(owner); stage.setTitle("Extraer y revisar · "+name);
        sources.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        jobs.setCellFactory(v->new ListCell<>() { @Override protected void updateItem(Job job,boolean empty) { super.updateItem(job,empty); setText(empty||job==null?null:job.id().toString().substring(0,8)+" · "+job.state()+" · "+job.reviewState()+" · "+job.confirmedBlocks()+" bloques confirmados"); } });
        sources.setCellFactory(v->new ListCell<>() { @Override protected void updateItem(WorksApiClient.Attachment a,boolean empty) { super.updateItem(a,empty); setText(empty||a==null?null:"#"+a.id()+" · "+a.kind()+" · "+a.originalName()); } });
        root.setPadding(new javafx.geometry.Insets(18)); status.setWrapText(true); state.setWrapText(true); evidence.setWrapText(true);
        tabs.getTabs().setAll(tab("Fuentes y borradores",sourcePane()),tab("Evidencia y campos",reviewPane()),tab("Presupuestos por confirmar",budgetPane()),tab("Revisiones confirmadas",historyPane()));
        VBox.setVgrow(tabs,Priority.ALWAYS); var actions=new HBox(8,save,confirmWork,confirmClient,confirmBudget);
        root.getChildren().setAll(new Label("Obra #"+workId+" · "+name),state,tabs,actions,status);
        save.setOnAction(e->save()); confirmWork.setOnAction(e->confirm("WORK",null)); confirmClient.setOnAction(e->confirm("CLIENT",null));
        confirmBudget.setOnAction(e->{ var b=draftList.getSelectionModel().getSelectedItem(); if(b!=null) { confirm("BUDGET",b.blockId()); } });
        draftList.getSelectionModel().selectedItemProperty().addListener((o,a,b)->buttons());
        var scene=new Scene(root,1220,820); var css=getClass().getResource("/com/brica/client/presentation/styles/app.css"); if(css!=null) { scene.getStylesheets().add(css.toExternalForm()); }
        stage.setScene(scene); stage.setMinWidth(1000); stage.setMinHeight(650);
        stage.setOnCloseRequest(e->{ if(dirty&&!ask("Hay cambios locales sin guardar. ¿Cerrar sin guardar esos cambios? El trabajo del servidor no se cancela.")) { e.consume(); } });
        stage.setOnHidden(e->{ polling.stop(); generation++; }); polling.setCycleCount(Animation.INDEFINITE); polling.play(); buttons();
        run(api.context(workId),value->{ context=value; loadJobs(); });
        works.allAttachments(workId).whenComplete((values,error)->Platform.runLater(()->{ if(error!=null) { failure(error); } else { sources.getItems().setAll(values); if(attachmentId!=null) { values.stream().filter(a->attachmentId.equals(a.id())).findFirst().ifPresent(a->sources.getSelectionModel().select(a)); } } }));
        refreshBudgets();
    }
    private boolean can(String permission) { var user=ServerSession.shared(ClientConfig.serverBaseUri()).user(); return user!=null&&user.can(permission); }
    private static Tab tab(String text,javafx.scene.Node node) { var tab=new Tab(text,node); tab.setClosable(false); return tab; }
    private javafx.scene.Node sourcePane() {
        var create=new Button("Analizar fuentes seleccionadas"); create.setDisable(!can("WORKS_WRITE"));
        create.setOnAction(e->{
            if(context==null || sources.getSelectionModel().isEmpty()) { status.setText("Selecciona entre uno y cuatro adjuntos."); return; }
            var ids=sources.getSelectionModel().getSelectedItems().stream().map(WorksApiClient.Attachment::id).toList();
            if(ids.size()>4) { status.setText("Selecciona hasta cuatro fuentes por extracción."); return; }
            if(dirty&&!ask("¿Abrir otro trabajo sin guardar los cambios locales?")) { return; }
            UUID key=operations.computeIfAbsent("create:"+ids,ignored->UUID.randomUUID());
            run(api.create(workId,key,ids),v->{ adopt(v,true); loadJobs(); tabs.getSelectionModel().select(1); });
        });
        var upload=new Button("Adjuntar archivo"); upload.setDisable(!can("WORKS_WRITE")); var kind=new ComboBox<String>(); kind.getItems().setAll("CONTRACT","BASE_BUDGET"); kind.setValue("CONTRACT");
        upload.setOnAction(e->{ var chooser=new FileChooser(); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fuentes PDF, Excel y Word","*.pdf","*.xls","*.xlsx","*.docx")); var file=chooser.showOpenDialog(stage); if(file!=null) { run(works.upload(workId,kind.getValue(),file.toPath()),a->{ sources.getItems().add(a); sources.getSelectionModel().select(a); }); } });
        var reopen=new Button("Retomar seleccionado"); reopen.setOnAction(e->{ var j=jobs.getSelectionModel().getSelectedItem(); if(j!=null && (!dirty||ask("¿Descartar cambios locales y cargar el borrador guardado?"))) { run(api.get(j.id()),v->adopt(v,true)); } });
        var refresh=new Button("Actualizar lista"); refresh.setOnAction(e->loadJobs());
        var reanalyze=new Button("Reanalizar seleccionado"); reanalyze.setDisable(!can("WORKS_WRITE"));
        reanalyze.setOnAction(e->{
            var j=jobs.getSelectionModel().getSelectedItem();
            if(j==null || dirty&&!ask("¿Abrir otro trabajo sin guardar los cambios locales?")) { return; }
            String key="reanalyze:"+j.id();
            var operation=operations.computeIfAbsent(key,ignored->UUID.randomUUID());
            run(api.reanalyze(j.id(),operation),v->{ operations.remove(key); adopt(v,true); loadJobs(); tabs.getSelectionModel().select(1); });
        });
        var attempts=new Button("Consultar intentos"); attempts.setOnAction(e->{
            var j=jobs.getSelectionModel().getSelectedItem();
            if(j!=null) { run(api.attempts(j.id()),values->{
                var dialog=new Alert(Alert.AlertType.INFORMATION);
                dialog.initOwner(stage); dialog.setHeaderText("Historial de lectura");
                var text=new TextArea(values.isEmpty()?"No hay intentos registrados.":String.join("\n\n",values.stream().map(Attempt::toString).toList()));
                text.setEditable(false); dialog.getDialogPane().setContent(text); dialog.showAndWait();
            }); }
        });
        var cancel=new Button("Cancelar lo pendiente"); cancel.setDisable(!can("WORKS_WRITE")); cancel.setOnAction(e->{ if(current!=null&&ask("Cancelar impide nuevas confirmaciones. Los bloques ya confirmados NO se revierten. ¿Continuar?")) { run(api.cancel(current),v->adopt(v,true)); } });
        var complete=new Button("Finalizar revisión"); complete.setDisable(!can("WORKS_WRITE")); complete.setOnAction(e->{ if(current!=null&&!dirty) { run(api.complete(current),v->adopt(v,true)); } else { status.setText("Guarda antes de finalizar."); } });
        var left=new VBox(10,new Label("Archivos originales de esta obra"),new HBox(8,kind,upload),sources,create);
        var right=new VBox(10,new Label("Trabajos y borradores guardados"),jobs,new HBox(8,reopen,refresh),new HBox(8,reanalyze,attempts),new HBox(8,cancel,complete));
        VBox.setVgrow(sources,Priority.ALWAYS); VBox.setVgrow(jobs,Priority.ALWAYS); var split=new SplitPane(left,right); split.setDividerPositions(0.5); return split;
    }
    private javafx.scene.Node reviewPane() {
        candidateTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        candidateTable.setPlaceholder(new Label("Selecciona fuentes y solicita su análisis."));
        BudgetEditor.column(candidateTable,"Archivo / ubicación",f->"#"+f.attachmentId()+" · "+f.location());
        BudgetEditor.column(candidateTable,"Texto / valor original",Fragment::original);
        BudgetEditor.column(candidateTable,"Dato sugerido",f->suggestionsByFragment.getOrDefault(f.id(),List.of()).stream()
                .map(s->targetLabel(s.target())+": "+s.proposedValue()).collect(java.util.stream.Collectors.joining(" · ")));
        BudgetEditor.column(candidateTable,"Advertencias",f->(f.hidden()?"Oculto; ":"")+Objects.toString(f.warning(),""));
        original.setEditable(false); original.setWrapText(true); original.setPrefRowCount(5);
        candidateTable.getSelectionModel().selectedItemProperty().addListener((o,a,f)->{ if(f!=null) { original.setText("Origen: archivo #"+f.attachmentId()+" · "+f.location()+"\n\n"+Objects.toString(f.original(),"")+"\nFórmula (no ejecutada): "+Objects.toString(f.formula(),"—")+"\nResultado guardado: "+Objects.toString(f.storedValue(),"Sin registrar")); } });
        var use=new Button("Proponer campo desde fragmento"); use.setOnAction(e->{
            var fragment=candidateTable.getSelectionModel().getSelectedItem();
            if(fragment==null) { status.setText("Selecciona un fragmento de evidencia."); return; }
            var options=suggestionsByFragment.getOrDefault(fragment.id(),List.of());
            decision(null,fragment,options.size()==1?options.getFirst():null);
        });
        var manual=new Button("Capturar campo manualmente"); manual.setOnAction(e->decision(null,null));
        var edit=new Button("Editar propuesta"); edit.setOnAction(e->{ var d=decisionsTable.getSelectionModel().getSelectedItem(); if(d!=null) { decision(d,null); } });
        var remove=new Button("Excluir propuesta"); remove.setOnAction(e->{ var d=decisionsTable.getSelectionModel().getSelectedItem(); if(d!=null) { decisions.remove(d); changed(); } });
        var contextRefresh=new Button("Consultar valores vigentes"); contextRefresh.setOnAction(e->run(api.context(workId),v->{
            context=v; decisionsTable.refresh();
            if(current!=null && current.job().reviewState().equals("DRAFT") && can("WORKS_WRITE")
                    && ask("¿Usar estas revisiones como base al guardar? Compara los valores vigentes con tus propuestas antes de confirmar.")) {
                rebaseContext=v; changed();
            }
            status.setText("Valores consultados. No se aplicó ningún cambio.");
        }));
        decisionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        BudgetEditor.column(decisionsTable,"Destino",d->context==null?d.target():context.targets().stream().filter(t->t.target().equals(d.target())).map(Target::label).findFirst().orElse(d.target()));
        BudgetEditor.column(decisionsTable,"Valor vigente",d->context==null?null:context.targets().stream().filter(t->t.target().equals(d.target())).map(t->Objects.toString(t.currentValue(),"Sin registrar")).findFirst().orElse(""));
        BudgetEditor.column(decisionsTable,"Valor propuesto",Decision::value); BudgetEditor.column(decisionsTable,"Justificación",Decision::justification);
        var suggestions=new Button("Consultar sugerencias"); suggestions.setOnAction(e->showSuggestions());
        var top=new VBox(8,candidateTable,original,new HBox(8,use,manual,suggestions)); VBox.setVgrow(candidateTable,Priority.ALWAYS);
        var bottom=new VBox(8,decisionsTable,new HBox(8,edit,remove,contextRefresh)); VBox.setVgrow(decisionsTable,Priority.ALWAYS);
        var split=new SplitPane(top,bottom); split.setOrientation(javafx.geometry.Orientation.VERTICAL); split.setDividerPositions(0.58); return split;
    }
    private void decision(Decision previous,Fragment fragment) {
        decision(previous,fragment,null);
    }
    private String targetLabel(String key) {
        return context==null?key:context.targets().stream().filter(t->t.target().equals(key)).map(Target::label).findFirst().orElse(key);
    }
    private void showSuggestions() {
        if(current==null || context==null) { return; }
        run(api.suggestions(current.job().id()),values->{
            if(values.isEmpty()) { status.setText("No hay sugerencias automáticas; puedes revisar los fragmentos y mapearlos manualmente."); return; }
            var byId=new HashMap<UUID,Fragment>(); candidates.forEach(f->byId.put(f.id(),f));
            var labels=new HashMap<String,String>(); context.targets().forEach(t->labels.put(t.target(),t.label()));
            var dialog=new Dialog<Suggestion>(); dialog.initOwner(stage); dialog.setTitle("Sugerencias y alternativas");
            var table=new TableView<Suggestion>(FXCollections.observableArrayList(values));
            BudgetEditor.column(table,"Destino",s->labels.getOrDefault(s.target(),s.target()));
            BudgetEditor.column(table,"Valor sugerido",Suggestion::proposedValue);
            BudgetEditor.column(table,"Evidencia",s->{ var f=byId.get(s.fragmentId()); return "#"+s.attachmentId()+(f==null?"":" · "+f.location()+" · "+Objects.toString(f.original(),"")); });
            BudgetEditor.column(table,"Precedencia",Suggestion::reason);
            table.setPrefSize(850,360); dialog.getDialogPane().setContent(table);
            var review=new ButtonType("Revisar selección",ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(review,ButtonType.CANCEL);
            dialog.getDialogPane().lookupButton(review).disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
            dialog.setResultConverter(button->button==review?table.getSelectionModel().getSelectedItem():null);
            dialog.showAndWait().ifPresent(s->{
                var fragment=byId.get(s.fragmentId());
                if(fragment==null) { status.setText("La evidencia todavía no está disponible. Vuelve a consultar el trabajo."); return; }
                decision(null,fragment,s);
            });
        });
    }
    private void decision(Decision previous,Fragment fragment,Suggestion suggestion) {
        if(current==null || context==null || !can("WORKS_WRITE")) { status.setText("Selecciona un borrador editable."); return; }
        var dialog=new Dialog<Decision>(); dialog.initOwner(stage); dialog.setTitle("Revisar campo antes de confirmar");
        var target=new ComboBox<Target>(FXCollections.observableArrayList(context.targets())); target.setMaxWidth(Double.MAX_VALUE);
        if(previous!=null) { context.targets().stream().filter(t->t.target().equals(previous.target())).findFirst().ifPresent(target::setValue); }
        var value=new TextArea(previous!=null?previous.value():suggestion!=null?suggestion.proposedValue():fragment==null?"":Objects.toString(fragment.storedValue(),fragment.original())); value.setPrefRowCount(5); value.setWrapText(true);
        var reason=new TextArea(previous==null?null:previous.justification()); reason.setPrefRowCount(3);
        var correction=new CheckBox("Es corrección de captura, NO convenio ni cambio de condiciones contractuales"); correction.setSelected(previous!=null&&previous.correctionOfCapture());
        var hint=new Label(); hint.setWrapText(true); target.valueProperty().addListener((o,a,t)->{ if(t!=null) { hint.setText("Vigente: "+Objects.toString(t.currentValue(),"Sin registrar")+"\nTipo: "+t.type()+". Fechas AAAA-MM-DD; importes decimales con punto. Fuente sugerida: "+Objects.toString(t.preferredSource(),"sin preferencia automática")+". El cliente es compartido entre obras."); } });
        if(suggestion!=null) { context.targets().stream().filter(t->t.target().equals(suggestion.target())).findFirst().ifPresent(target::setValue); }
        var error=new Label(); dialog.getDialogPane().setContent(new VBox(10,target,hint,new Label("Valor propuesto (editar no confirma)"),value,correction,new Label("Justificación; obligatoria en captura manual y correcciones"),reason,error));
        dialog.getDialogPane().setPrefWidth(680); var accept=new ButtonType("Aplicar al borrador",ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(accept,ButtonType.CANCEL);
        Decision[] result={null}; dialog.getDialogPane().lookupButton(accept).addEventFilter(javafx.event.ActionEvent.ACTION,e->{ if(target.getValue()==null) { e.consume(); error.setText("Selecciona un destino."); } else { result[0]=new Decision(target.getValue().target(),BudgetEditor.blank(value.getText()),previous!=null?previous.evidenceId():fragment==null?null:fragment.id(),reason.getText(),correction.isSelected()); } });
        dialog.setResultConverter(b->b==accept?result[0]:null); dialog.showAndWait().ifPresent(d->{ decisions.removeIf(old->old.target().equals(d.target()) || old==previous); decisions.add(d); changed(); });
    }
    private javafx.scene.Node budgetPane() {
        var map=new Button("Mapear tablas / agrupar hojas"); map.setOnAction(e->{ if(current!=null && can("WORKS_WRITE")) { var mapped=TableMappingDialog.show(stage,candidates,api,workId); drafts.addAll(mapped); if(!mapped.isEmpty()) { draftList.getSelectionModel().select(mapped.getFirst()); changed(); } } });
        var add=new Button("Capturar presupuesto manual"); add.setOnAction(e->editBudget(new Budget(UUID.randomUUID(),null,null,"Nuevo presupuesto",null,List.of(),List.of(),List.of(),null,null,null,null),false));
        var edit=new Button("Revisar conceptos, partidas y totales"); edit.setOnAction(e->{ var value=draftList.getSelectionModel().getSelectedItem(); if(value!=null) { editBudget(value,true); } });
        var remove=new Button("Excluir presupuesto"); remove.setOnAction(e->{ var value=draftList.getSelectionModel().getSelectedItem(); if(value!=null) { drafts.remove(value); changed(); } });
        var fromFragment=new Button("Crear concepto con fragmento seleccionado"); fromFragment.setOnAction(e->{
            var f=candidateTable.getSelectionModel().getSelectedItem(); if(f==null) { status.setText("Selecciona un fragmento en Evidencia y campos."); return; }
            var item=new Item(UUID.randomUUID(),0,null,null,Objects.toString(f.original(),""),null,null,null,null,f.id(),null);
            editBudget(new Budget(UUID.randomUUID(),null,null,"Presupuesto de fragmentos",null,List.of(),List.of(item),List.of(),null,null,null,null),false);
        });
        confirmationLog.setEditable(false); confirmationLog.setWrapText(true); confirmationLog.setPrefRowCount(5);
        var note=new Label("Cada presupuesto se confirma por separado. Partes y alternativas solo se agrupan por tu selección. Los resúmenes y condiciones pueden conservarse como totales/notas sin duplicar conceptos."); note.setWrapText(true);
        var pane=new VBox(10,note,new HBox(8,map,add),draftList,new HBox(8,edit,remove),fromFragment,new Label("Resultados de confirmación"),confirmationLog); VBox.setVgrow(draftList,Priority.ALWAYS); return pane;
    }
    private void editBudget(Budget value,boolean replace) { if(current==null || !can("WORKS_WRITE")) { return; } BudgetEditor.show(stage,value,existing).ifPresent(b->{ if(replace) { drafts.set(drafts.indexOf(value),b); } else { drafts.add(b); } draftList.getSelectionModel().select(b); changed(); }); }
    private javafx.scene.Node historyPane() {
        var refresh=new Button("Consultar presupuestos"); refresh.setOnAction(e->refreshBudgets());
        var choose=new Button("Elegir revisión principal"); choose.setDisable(!can("WORKS_WRITE")); choose.setOnAction(e->{ var b=savedBudgets.getValue(); var r=revisions.getValue(); if(b!=null&&r!=null&&ask("¿Elegir la revisión "+r.number()+" como principal? Su contenido y las revisiones anteriores se conservarán.")) { run(api.principal(b,r.id()),v->{ refreshBudgets(); status.setText("Revisión principal elegida explícitamente."); }); } });
        savedBudgets.valueProperty().addListener((o,a,b)->{ if(b!=null) { run(api.revisions(b.id()),values->{ revisions.getItems().setAll(values); revisions.setValue(values.isEmpty()?null:values.getFirst()); }); } });
        revisions.valueProperty().addListener((o,a,r)->{ if(r!=null) { itemPage=0; loadItems(); revisionInfo.setText("Política de control: "+r.controlPolicy()+"\nSubtotal declarado: "+r.subtotal()+"\nImpuesto declarado: "+r.tax()+"\nTotal declarado: "+r.total()+"\nDiferencias: "+r.differences()+"\nJustificación: "+r.justification()+"\nNotas: "+r.notes()); } });
        savedItems.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        BudgetEditor.column(savedItems,"Clave",Item::code); BudgetEditor.column(savedItems,"Descripción",Item::description); BudgetEditor.column(savedItems,"Unidad",Item::unit); BudgetEditor.column(savedItems,"Cantidad",i->BudgetEditor.text(i.quantity())); BudgetEditor.column(savedItems,"Precio",i->BudgetEditor.text(i.price())); BudgetEditor.column(savedItems,"Importe",i->BudgetEditor.text(i.amount()));
        var previous=new Button("Conceptos anteriores"); previous.setOnAction(e->{ if(itemPage>0) { itemPage--; loadItems(); } }); var next=new Button("Más conceptos"); next.setOnAction(e->{ if(savedItems.getItems().size()==100) { itemPage++; loadItems(); } });
        revisionInfo.setEditable(false); revisionInfo.setWrapText(true); revisionInfo.setPrefRowCount(6);
        var pane=new VBox(10,new HBox(8,savedBudgets,refresh),new HBox(8,revisions,choose),revisionInfo,savedItems,new HBox(8,previous,next)); VBox.setVgrow(savedItems,Priority.ALWAYS); return pane;
    }
    private void loadItems() { var r=revisions.getValue(); if(r!=null) { run(api.items(r.id(),itemPage),values->savedItems.getItems().setAll(values)); } }
    private void refreshBudgets() { api.budgets(workId).whenComplete((values,error)->Platform.runLater(()->{ if(error!=null) { failure(error); } else { existing=values; savedBudgets.getItems().setAll(values); } })); }
    private void changed() { dirty=true; status.setText("Cambios locales. Guarda el borrador antes de confirmar."); buttons(); }
    private void save() {
        if(current==null || context==null) { return; }
        var base=current.review();
        var review=new Review(rebaseContext!=null&&!confirmed("WORK")?rebaseContext.workRevision():base.workRevision(),
                rebaseContext!=null&&!confirmed("CLIENT")?rebaseContext.clientRevision():base.clientRevision(),List.copyOf(decisions),List.copyOf(drafts));
        run(api.save(current,review),v->{ adopt(v,true); status.setText("Borrador guardado en el servidor."); });
    }
    private void confirm(String target,UUID budget) {
        if(current==null || dirty) { status.setText("Guarda y revisa el borrador antes de confirmar."); return; }
        if(!ask(target.equals("CLIENT")?"El cliente es compartido por otras obras. ¿Confirmar únicamente este bloque?":"¿Confirmar únicamente el bloque seleccionado? Los otros bloques seguirán pendientes.")) { return; }
        String block=target+":"+budget; UUID operation=operations.computeIfAbsent(current.job().id()+":"+block,ignored->UUID.randomUUID());
        run(api.confirm(current,target,budget,operation),confirmed->{ status.setText(confirmed.result()); run(api.get(current.job().id()),v->{ adopt(v,true); refreshBudgets(); }); });
    }
    private void adopt(View view,boolean replace) {
        if(current==null || !current.job().id().equals(view.job().id())) {
            candidates.clear(); candidateTable.getItems().clear(); suggestionsByFragment.clear(); original.clear();
        }
        current=view; state.setText(view.job().state()+" · Revisión "+view.job().reviewState()+" · "+view.job().confirmedBlocks()+" bloque(s) confirmados"+(view.job().diagnostic()==null?"":"\n"+view.job().diagnostic()));
        var emptyMessage=switch(view.job().state()) {
            case "QUEUED" -> "El análisis está en cola. Aún no hay evidencia.";
            case "RUNNING" -> "El lector está procesando las fuentes. Aún no hay evidencia.";
            case "FAILED" -> "No se produjo evidencia. Consulta el diagnóstico del trabajo arriba.";
            case "CANCELLED" -> "El análisis se canceló. No hay evidencia para mostrar.";
            default -> "El trabajo no tiene fragmentos de evidencia para mostrar.";
        };
        var placeholder=new Label(emptyMessage); placeholder.setWrapText(true); candidateTable.setPlaceholder(placeholder);
        confirmationLog.setText(String.join("\n",view.confirmations().stream().map(c->c.block()+": "+c.result()).toList()));
        if(replace) { decisions.setAll(view.review().decisions()); drafts.setAll(view.review().budgets()); dirty=false; rebaseContext=null; }
        buttons(); long epoch=++generation; UUID id=view.job().id();
        api.allCandidates(id).whenComplete((values,error)->Platform.runLater(()->{
            if(!isCurrentEvidenceRequest(epoch,id)) { return; }
            if(error!=null) { failure(error); return; }
            candidates.clear(); candidates.addAll(values); candidateTable.getItems().setAll(values);
            evidence.setText(values.size()+" fragmentos de evidencia");
            if(!values.isEmpty() && (view.job().state().equals("COMPLETED") || view.job().state().equals("PARTIAL"))) {
                api.suggestions(id).whenComplete((suggestions,suggestionError)->Platform.runLater(()->{
                    if(!isCurrentEvidenceRequest(epoch,id) || suggestionError!=null) { return; }
                    suggestionsByFragment.clear();
                    suggestions.forEach(s->suggestionsByFragment.computeIfAbsent(s.fragmentId(),ignored->new ArrayList<>()).add(s));
                    candidateTable.refresh();
                }));
            }
        }));
    }
    private boolean isCurrentEvidenceRequest(long epoch,UUID id) {
        return epoch==generation && stage.isShowing() && current!=null && current.job().id().equals(id);
    }
    private void loadJobs() { run(api.jobs(workId),values->jobs.getItems().setAll(values)); }
    private void poll() {
        if(current==null || busy || !(current.job().state().equals("QUEUED")||current.job().state().equals("RUNNING"))) { return; }
        View snapshot=current;
        api.get(snapshot.job().id()).whenComplete((value,error)->Platform.runLater(()->{ if(!stage.isShowing() || busy || current!=snapshot) { return; } if(error!=null) { failure(error); } else { adopt(value,false); } }));
    }
    private void buttons() {
        tabs.setDisable(busy);
        boolean editable=current!=null && current.job().reviewState().equals("DRAFT") && !busy;
        save.setDisable(!editable||!can("WORKS_WRITE"));
        confirmWork.setDisable(!editable||dirty||confirmed("WORK")||!can("WORKS_WRITE")||decisions.stream().noneMatch(d->!d.target().startsWith("CLIENT_")));
        confirmClient.setDisable(!editable||dirty||confirmed("CLIENT")||!can("CLIENTS_WRITE")||!can("CLIENTS_READ")||decisions.stream().noneMatch(d->d.target().startsWith("CLIENT_")));
        var selected=draftList.getSelectionModel().getSelectedItem();
        confirmBudget.setDisable(!editable||dirty||!can("WORKS_WRITE")||selected==null||confirmed("BUDGET:"+(selected==null?"":selected.blockId())));
    }
    private boolean confirmed(String block) { return current!=null && current.confirmations().stream().anyMatch(c->c.block().equals(block)); }
    private <T> void run(CompletableFuture<T> request,Consumer<T> success) {
        long epoch=++requestGeneration; busy=true; buttons(); status.setText("Consultando servidor…");
        request.whenComplete((value,error)->Platform.runLater(()->{ if(epoch!=requestGeneration || !stage.isShowing()) { return; } busy=false; buttons(); if(error!=null) { failure(error); } else { success.accept(value); } }));
    }
    private void failure(Throwable error) { while(error.getCause()!=null && (error instanceof java.util.concurrent.CompletionException || error instanceof java.util.concurrent.ExecutionException)) { error=error.getCause(); } status.setText(error.getMessage()+"\nLa captura local se conserva. Puedes retomar el estado guardado o consultar valores vigentes."); }
    private boolean ask(String message) { var alert=new Alert(Alert.AlertType.CONFIRMATION,message,ButtonType.OK,ButtonType.CANCEL); alert.initOwner(stage); return alert.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK; }
}
