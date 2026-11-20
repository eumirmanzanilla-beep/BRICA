package com.brica.client.presentation.extraction;

import com.brica.client.api.ExtractionDtos.*;
import com.brica.client.api.ExtractionApiClient;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import java.math.BigDecimal;
import java.util.*;

final class TableMappingDialog {
    private TableMappingDialog() { }
    record Table(long source,String sheet) { @Override public String toString() { return "Archivo #"+source+" · "+sheet; } }
    static List<Budget> show(Window owner,List<Fragment> fragments,ExtractionApiClient api,long work) {
        var dialog=new Dialog<List<Budget>>(); dialog.initOwner(owner); dialog.setTitle("Mapear tablas y agrupar partes"); dialog.setResizable(true);
        var groups=new ListView<Table>(); groups.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        groups.getItems().setAll(fragments.stream().filter(f->f.location().sheet()!=null && f.location().row()!=null).map(f->new Table(f.attachmentId(),f.location().sheet())).distinct().toList());
        var role=new ComboBox<String>(); role.getItems().setAll("Partes del mismo presupuesto","Alternativas (presupuestos separados)"); role.setValue(role.getItems().getFirst());
        var first=new Spinner<Integer>(1,20000,2); var last=new Spinner<Integer>(1,20000,20000); first.setEditable(true); last.setEditable(true);
        String[] labels={"Clave","Descripción","Unidad","Cantidad","Precio","Importe declarado"};
        var columns=new ArrayList<Spinner<Integer>>(); var grid=new GridPane(); grid.setHgap(10); grid.setVgap(8);
        for(int i=0;i<labels.length;i++) { var spinner=new Spinner<Integer>(0,256,i+1); spinner.setEditable(true); columns.add(spinner); grid.addRow(i,new Label(labels[i]+" (0 = ausente)"),spinner); }
        var numeric=new ComboBox<String>(); numeric.getItems().setAll("Punto decimal; coma de miles","Coma decimal; punto de miles"); numeric.setPromptText("Elige la convención del texto numérico");
        var hidden=new CheckBox("Incluir filas/columnas ocultas (selección explícita)");
        var message=new Label("Selecciona las partes en orden de aparición. Un resumen o total repetido no debe seleccionarse como otra parte. Se conservarán códigos repetidos y ausencias; revisa y reclasifica las filas después de importar."); message.setWrapText(true);
        var error=new Label(); error.setWrapText(true);
        var profiles=new ComboBox<Profile>(); profiles.setPromptText("Perfil opcional de esta obra (solo sugiere)");
        String[] keys={"code","description","unit","quantity","price","amount"};
        profiles.valueProperty().addListener((o,a,p)->{ if(p!=null) {
            for(int i=0;i<keys.length;i++) { columns.get(i).getValueFactory().setValue(p.mapping().columns().getOrDefault(keys[i],0)); }
            first.getValueFactory().setValue(p.mapping().firstRow()); last.getValueFactory().setValue(p.mapping().lastRow()); numeric.getSelectionModel().select(p.mapping().decimalConvention().equals("DECIMAL_POINT")?0:1);
        } });
        api.profiles(work).whenComplete((values,failure)->javafx.application.Platform.runLater(()->{ if(dialog.isShowing()) { if(failure==null) { profiles.getItems().setAll(values); } else { error.setText("No se pudieron consultar perfiles; el mapeo manual sigue disponible."); } } }));
        var saveProfile=new Button("Guardar mapeo como versión de perfil"); saveProfile.setOnAction(e->{
            var prompt=new TextInputDialog(); prompt.initOwner(owner); prompt.setHeaderText("Nombre del perfil de esta obra. No se guardan valores del documento.");
            prompt.showAndWait().filter(s->!s.isBlank()).ifPresent(name->{
                if(numeric.getValue()==null) { error.setText("Selecciona una convención numérica antes de guardar el perfil."); return; }
                var values=new LinkedHashMap<String,Integer>(); for(int i=0;i<keys.length;i++) { values.put(keys[i],columns.get(i).getValue()); }
                saveProfile.setDisable(true);
                api.profile(work,name,new Mapping(1,values,first.getValue(),last.getValue(),numeric.getSelectionModel().getSelectedIndex()==0?"DECIMAL_POINT":"DECIMAL_COMMA"))
                    .whenComplete((saved,failure)->javafx.application.Platform.runLater(()->{ if(dialog.isShowing()) { saveProfile.setDisable(false); if(failure==null) { profiles.getItems().add(0,saved); profiles.setValue(saved); error.setText("Perfil versionado guardado; importa solo después de revisar el mapeo."); } else { error.setText("No se guardó el perfil: "+failure.getMessage()); } } }));
            });
        });
        var content=new VBox(10,message,groups,role,new HBox(10,new Label("Primera fila"),first,new Label("Última fila"),last),grid,numeric,hidden,profiles,saveProfile,error);
        dialog.getDialogPane().setContent(content); dialog.getDialogPane().setPrefSize(720,730);
        var apply=new ButtonType("Importar al borrador",ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(apply,ButtonType.CANCEL);
        List<Budget>[] result=new List[]{List.of()};
        dialog.getDialogPane().lookupButton(apply).addEventFilter(javafx.event.ActionEvent.ACTION,e->{ try {
            if(groups.getSelectionModel().isEmpty() || columns.get(1).getValue()==0 || numeric.getValue()==null || first.getValue()>last.getValue()) { throw new IllegalArgumentException("Selecciona tablas, descripción, rango y convención numérica."); }
            var selected=List.copyOf(groups.getSelectionModel().getSelectedItems()); var output=new ArrayList<Budget>(); var together=new ArrayList<Item>();
            for(var table:selected) {
                var rows=new TreeMap<Integer,Map<Integer,Fragment>>();
                fragments.stream().filter(f->f.attachmentId()==table.source() && table.sheet().equals(f.location().sheet()) && f.location().row()!=null && f.location().column()!=null
                        && !f.kind().equals("MERGED") && f.location().row()>=first.getValue() && f.location().row()<=last.getValue() && (!f.hidden()||hidden.isSelected()))
                        .forEach(f->rows.computeIfAbsent(f.location().row(),ignored->new HashMap<>()).put(f.location().column(),f));
                var items=new ArrayList<Item>();
                for(var row:rows.values()) {
                    var mapped=columns.stream().map(c->row.get(c.getValue())).toList();
                    if(mapped.stream().allMatch(f->f==null || f.original()==null || f.original().isBlank())) { continue; }
                    var description=mapped.get(1);
                    items.add(new Item(UUID.randomUUID(),items.size(),null,value(mapped.get(0)),Objects.toString(value(description),""),value(mapped.get(2)),decimal(mapped.get(3),numeric.getSelectionModel().getSelectedIndex()),decimal(mapped.get(4),numeric.getSelectionModel().getSelectedIndex()),decimal(mapped.get(5),numeric.getSelectionModel().getSelectedIndex()),description==null?null:description.id(),value(mapped.get(3))));
                }
                if(role.getSelectionModel().getSelectedIndex()==1) { output.add(budget(table.sheet(),items)); }
                else { together.addAll(items); }
            }
            if(role.getSelectionModel().getSelectedIndex()==0) { output.add(budget("Presupuesto revisado",together)); }
            if(output.stream().anyMatch(b->b.items().isEmpty())) { throw new IllegalArgumentException("El rango no contiene conceptos mapeados."); }
            result[0]=List.copyOf(output);
        } catch(Exception failure) { e.consume(); error.setText(failure.getMessage()); } });
        dialog.setResultConverter(b->b==apply?result[0]:null); return dialog.showAndWait().orElse(List.of());
    }
    private static Budget budget(String name,List<Item> rows) {
        var ordered=new ArrayList<Item>(); for(int i=0;i<rows.size();i++) { var v=rows.get(i); ordered.add(new Item(v.id(),i,null,v.code(),v.description(),v.unit(),v.quantity(),v.price(),v.amount(),v.evidenceId(),v.originalMeasurement())); }
        return new Budget(UUID.randomUUID(),null,null,name,null,List.of(),List.copyOf(ordered),List.of(),null,null,null,null);
    }
    private static String value(Fragment f) { return f==null?null:f.original(); }
    private static BigDecimal decimal(Fragment f,int convention) {
        if(f==null || f.storedValue()==null && f.formula()!=null) { return null; }
        if(f.storedValue()!=null && (f.kind().equals("NUMERIC") || f.kind().equals("FORMULA"))) { return BudgetEditor.number(f.storedValue()); }
        String text=value(f); if(text==null || text.isBlank()) { return null; }
        return BudgetEditor.number(convention==0?text.replace(",",""):text.replace(".","").replace(',','.'));
    }
}
