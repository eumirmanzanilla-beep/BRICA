package com.brica.client.api;
import java.math.BigDecimal;
import java.util.*;

public final class ExtractionDtos {
    private ExtractionDtos() { }
    public record Job(UUID id,Long workId,String state,String reviewState,String revision,String diagnostic,int confirmedBlocks) { }
    public record Decision(String target,String value,UUID evidenceId,String justification,boolean correctionOfCapture) { }
    public record Review(String workRevision,String clientRevision,List<Decision> decisions,List<Budget> budgets) { }
    public record Confirmation(UUID id,String block,String actor,String result,String confirmedAt) { }
    public record View(Job job,Review review,List<Long> attachmentIds,List<Confirmation> confirmations) { }
    public record Target(String target,String label,String type,String currentValue,String preferredSource) {
        @Override public String toString() { return label; }
    }
    public record Context(String workRevision,String clientRevision,List<Target> targets) { }
    public record Location(Integer page,String sheet,Integer row,Integer column,String part,Float x,Float y,Float width,Float height) {
        @Override public String toString() {
            if(sheet!=null) { return sheet+(row==null?"":" · fila "+row+", columna "+column); }
            return page==null?part:"Página "+page+" · "+part;
        }
    }
    public record Fragment(UUID id,long attachmentId,Location location,String original,String formula,String storedValue,String kind,boolean hidden,String warning) { }
    public record Section(UUID id,int position,String title) { @Override public String toString() { return title; } }
    public record Item(UUID id,int position,UUID sectionId,String code,String description,String unit,BigDecimal quantity,BigDecimal price,BigDecimal amount,UUID evidenceId,String originalMeasurement) { }
    public record Budget(UUID blockId,Long budgetId,String expectedRevision,String name,String currency,List<Section> sections,List<Item> items,List<String> notes,BigDecimal subtotal,BigDecimal tax,BigDecimal total,String justification) {
        @Override public String toString() { return name+" · "+items.size()+" conceptos"; }
    }
    public record BudgetView(Long id,Long workId,String name,UUID principalRevisionId,String revision) {
        @Override public String toString() { return name+" (#"+id+")"; }
    }
    public record BudgetRevision(UUID id,Long budgetId,int number,UUID sourceJobId,String currency,BigDecimal subtotal,BigDecimal tax,BigDecimal total,String controlPolicy,List<String> notes,List<Difference> differences,String justification) {
        @Override public String toString() { return "Revisión "+number+(total==null?"":" · total declarado "+total); }
    }
    public record Difference(String location,BigDecimal declared,BigDecimal calculated,BigDecimal difference) { }
    public record Mapping(int schemaVersion,Map<String,Integer> columns,int firstRow,int lastRow,String decimalConvention) { }
    public record Profile(UUID id,String name,int version,Mapping mapping) { @Override public String toString() { return name+" · versión "+version; } }
}
