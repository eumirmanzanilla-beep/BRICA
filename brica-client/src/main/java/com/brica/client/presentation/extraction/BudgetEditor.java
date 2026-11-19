package com.brica.client.presentation.extraction;

import com.brica.client.api.ExtractionDtos.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import java.math.BigDecimal;
import java.util.*;

final class BudgetEditor {
    private BudgetEditor() { }
    static Optional<Budget> show(Window owner,Budget original,List<BudgetView> existing) {
        var dialog=new Dialog<Budget>(); dialog.initOwner(owner); dialog.setTitle("Revisar presupuesto"); dialog.setResizable(true);
        var name=new TextField(original.name()); var currency=new TextField(original.currency());
        var subtotal=new TextField(text(original.subtotal())); var tax=new TextField(text(original.tax())); var total=new TextField(text(original.total()));
        var reason=new TextArea(original.justification()); reason.setPromptText("Justificación de diferencias o de captura manual"); reason.setPrefRowCount(2);
        var notes=new TextArea(String.join("\n",original.notes())); notes.setPromptText("Notas comerciales separadas de los conceptos"); notes.setPrefRowCount(3);
        var destination=new ComboBox<BudgetView>(); destination.getItems().add(null); destination.getItems().addAll(existing);
        destination.setConverter(new javafx.util.StringConverter<>() {
            public String toString(BudgetView b) { return b==null?"Crear presupuesto nuevo":b.name()+" · nueva revisión"; }
            public BudgetView fromString(String s) { return null; }
        });
        existing.stream().filter(b->Objects.equals(b.id(),original.budgetId())).findFirst().ifPresent(destination::setValue);
        var rows=FXCollections.observableArrayList(original.items()); var sections=new ArrayList<>(original.sections());
        var table=new TableView<Item>(rows); table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        column(table,"Clave",i->i.code()); column(table,"Descripción completa",Item::description); column(table,"Unidad",Item::unit);
        column(table,"Cantidad",i->text(i.quantity())); column(table,"Precio",i->text(i.price())); column(table,"Importe declarado",i->text(i.amount()));
        column(table,"Partida",i->sections.stream().filter(s->s.id().equals(i.sectionId())).map(Section::title).findFirst().orElse("Sin partida"));
        var add=new Button("Añadir concepto"); var edit=new Button("Editar"); var remove=new Button("Excluir concepto");
        var up=new Button("Subir"); var down=new Button("Bajar"); var section=new Button("Asignar partida"); var note=new Button("Pasar a nota");
        add.setOnAction(e->item(owner,null,sections).ifPresent(rows::add));
        edit.setOnAction(e->{ int index=table.getSelectionModel().getSelectedIndex(); if(index>=0) { item(owner,rows.get(index),sections).ifPresent(value->rows.set(index,value)); } });
        table.setOnMouseClicked(e->{ if(e.getClickCount()==2) { edit.fire(); } });
        remove.setOnAction(e->{ var selected=List.copyOf(table.getSelectionModel().getSelectedItems()); rows.removeAll(selected); });
        up.setOnAction(e->move(table,rows,-1)); down.setOnAction(e->move(table,rows,1));
        section.setOnAction(e->{
            var input=new TextInputDialog(); input.initOwner(owner); input.setTitle("Partida (un nivel)"); input.setHeaderText("Escribe la partida. Se asignará a los conceptos seleccionados.");
            input.showAndWait().filter(s->!s.isBlank()).ifPresent(title->{
                var group=sections.stream().filter(s->s.title().equals(title)).findFirst().orElseGet(()->{ var s=new Section(UUID.randomUUID(),0,title); sections.add(s); return s; });
                for(var selected:List.copyOf(table.getSelectionModel().getSelectedItems())) { int at=rows.indexOf(selected); rows.set(at,new Item(selected.id(),selected.position(),group.id(),selected.code(),selected.description(),selected.unit(),selected.quantity(),selected.price(),selected.amount(),selected.evidenceId(),selected.originalMeasurement())); }
            });
        });
        note.setOnAction(e->{ for(var selected:List.copyOf(table.getSelectionModel().getSelectedItems())) { notes.appendText((notes.getText().isBlank()?"":"\n")+Objects.toString(selected.description(),"")); rows.remove(selected); } });
        var header=new GridPane(); header.setHgap(10); header.setVgap(7);
        header.addRow(0,new Label("Nombre"),name,new Label("Destino"),destination);
        header.addRow(1,new Label("Moneda declarada"),currency,new Label("Subtotal declarado"),subtotal);
        header.addRow(2,new Label("Impuesto declarado"),tax,new Label("Total declarado"),total);
        var warning=new Label("Los cálculos de control no sustituyen importes declarados. Un campo vacío es sin registrar, no cero. Confirmar no selecciona automáticamente una revisión principal."); warning.setWrapText(true);
        var error=new Label(); error.setWrapText(true);
        var content=new VBox(10,header,warning,table,new HBox(7,add,edit,remove,up,down,section,note),new Label("Notas y condiciones"),notes,new Label("Justificación"),reason,error);
        VBox.setVgrow(table,Priority.ALWAYS); dialog.getDialogPane().setContent(content); dialog.getDialogPane().setPrefSize(1050,740);
        var accept=new ButtonType("Aplicar al borrador",ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(accept,ButtonType.CANCEL);
        Budget[] captured={null};
        dialog.getDialogPane().lookupButton(accept).addEventFilter(javafx.event.ActionEvent.ACTION,e->{
            try {
                if(name.getText().isBlank() || rows.isEmpty()) { throw new IllegalArgumentException("Indica un nombre y al menos un concepto."); }
                var ordered=new ArrayList<Item>();
                for(int i=0;i<rows.size();i++) { var v=rows.get(i); ordered.add(new Item(v.id(),i,v.sectionId(),v.code(),v.description(),v.unit(),v.quantity(),v.price(),v.amount(),v.evidenceId(),v.originalMeasurement())); }
                var groups=new ArrayList<Section>(); for(int i=0;i<sections.size();i++) { var s=sections.get(i); groups.add(new Section(s.id(),rows.size()+i,s.title())); }
                var selected=destination.getValue();
                captured[0]=new Budget(original.blockId(),selected==null?null:selected.id(),selected==null?null:selected.revision(),name.getText(),blank(currency.getText())==null?null:currency.getText().toUpperCase(Locale.ROOT),List.copyOf(groups),List.copyOf(ordered),notes.getText().lines().filter(s->!s.isBlank()).toList(),number(subtotal.getText()),number(tax.getText()),number(total.getText()),reason.getText());
            } catch(Exception failure) { e.consume(); error.setText(failure.getMessage()); }
        });
        dialog.setResultConverter(button->button==accept?captured[0]:null); return dialog.showAndWait();
    }
    private static void move(TableView<Item> table,javafx.collections.ObservableList<Item> rows,int offset) {
        int index=table.getSelectionModel().getSelectedIndex(),to=index+offset;
        if(index>=0 && to>=0 && to<rows.size()) { var value=rows.remove(index); rows.add(to,value); table.getSelectionModel().select(to); }
    }
    private static Optional<Item> item(Window owner,Item original,List<Section> sections) {
        var d=new Dialog<Item>(); d.initOwner(owner); d.setTitle("Concepto"); d.setResizable(true);
        var code=new TextField(original==null?null:original.code()); var description=new TextArea(original==null?null:original.description()); description.setWrapText(true); description.setPrefRowCount(7);
        var unit=new TextField(original==null?null:original.unit()); var quantity=new TextField(original==null?null:text(original.quantity()));
        var price=new TextField(original==null?null:text(original.price())); var amount=new TextField(original==null?null:text(original.amount()));
        var error=new Label(); var grid=new GridPane(); grid.setHgap(10); grid.setVgap(8);
        grid.addRow(0,new Label("Clave (opcional)"),code); grid.addRow(1,new Label("Descripción completa"),description);
        grid.addRow(2,new Label("Unidad"),unit); grid.addRow(3,new Label("Cantidad"),quantity); grid.addRow(4,new Label("Precio"),price); grid.addRow(5,new Label("Importe declarado"),amount); grid.add(error,0,6,2,1);
        d.getDialogPane().setContent(grid); d.getDialogPane().setPrefWidth(650);
        var save=new ButtonType("Aplicar",ButtonBar.ButtonData.OK_DONE); d.getDialogPane().getButtonTypes().addAll(save,ButtonType.CANCEL); Item[] result={null};
        d.getDialogPane().lookupButton(save).addEventFilter(javafx.event.ActionEvent.ACTION,e->{ try {
            if(description.getText().isBlank()) { throw new IllegalArgumentException("Conserva una descripción completa del concepto."); }
            result[0]=new Item(original==null?UUID.randomUUID():original.id(),original==null?0:original.position(),original==null?null:original.sectionId(),blank(code.getText()),description.getText(),blank(unit.getText()),number(quantity.getText()),number(price.getText()),number(amount.getText()),original==null?null:original.evidenceId(),original==null?null:original.originalMeasurement());
        } catch(Exception failure) { e.consume(); error.setText(failure.getMessage()); } });
        d.setResultConverter(b->b==save?result[0]:null); return d.showAndWait();
    }
    static BigDecimal number(String value) { if(value==null || value.isBlank()) { return null; } try { return new BigDecimal(value.strip()); } catch(NumberFormatException failure) { throw new IllegalArgumentException("Indica decimales con punto, sin separador de miles; conserva vacíos los ausentes."); } }
    static String text(Object value) { return value==null?"":value.toString(); }
    static String blank(String value) { return value==null || value.isBlank()?null:value; }
    static <T> void column(TableView<T> table,String name,java.util.function.Function<T,String> read) {
        var column=new TableColumn<T,String>(name); column.setCellValueFactory(c->new ReadOnlyStringWrapper(Objects.toString(read.apply(c.getValue()),""))); table.getColumns().add(column);
    }
}
