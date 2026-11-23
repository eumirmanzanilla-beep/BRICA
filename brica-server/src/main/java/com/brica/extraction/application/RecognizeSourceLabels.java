package com.brica.extraction.application;

import com.brica.extraction.domain.SourceResult;
import java.text.Normalizer;
import java.util.*;

/** Conservative, literal labels. Unknown/ambiguous layouts remain available for manual mapping. */
public final class RecognizeSourceLabels {
    private record Cell(String sheet,Integer row,Integer column) { }
    private static final Set<String> AMOUNTS=Set.of("WORK_CONTRACTED_AMOUNT","WORK_AMOUNT_BEFORE_TAX","WORK_TAX_AMOUNT");
    private static final Map<String,String> LABELS=Map.ofEntries(
        Map.entry("NUMEROCONTRATO","WORK_CONTRACT_NUMBER"),Map.entry("NUMERODECONTRATO","WORK_CONTRACT_NUMBER"),
        Map.entry("CONTRATISTA","WORK_CONTRACTOR"),Map.entry("FECHAINICIOCONTRACTUAL","WORK_START_DATE"),
        Map.entry("FECHADEINICIOCONTRACTUAL","WORK_START_DATE"),Map.entry("FECHATERMINACIONCONTRACTUAL","WORK_END_DATE"),
        Map.entry("FECHADETERMINACIONCONTRACTUAL","WORK_END_DATE"),Map.entry("MONTOCONTRATADO","WORK_CONTRACTED_AMOUNT"),
        Map.entry("MONTOANTESDEIVA","WORK_AMOUNT_BEFORE_TAX"),Map.entry("IMPORTEDEIVA","WORK_TAX_AMOUNT"),
        Map.entry("DENOMINACIONDELAOBRA","WORK_DOCUMENT_NAME"),Map.entry("NOMBREDELAOBRA","WORK_DOCUMENT_NAME"),
        Map.entry("DENOMINACIONDELCLIENTE","CLIENT_DOCUMENT_NAME"),Map.entry("RAZONSOCIALDELCLIENTE","CLIENT_DOCUMENT_NAME"),
        Map.entry("REPRESENTANTELEGAL","CLIENT_LEGAL_REPRESENTATIVE"),Map.entry("UBICACION","WORK_LOCATION"));
    private RecognizeSourceLabels() { }
    public static List<SourcePrecedencePolicy.Candidate> propose(SourceResult result,SourcePrecedencePolicy.SourceClass source) {
        var candidates=new ArrayList<SourcePrecedencePolicy.Candidate>();
        var cells=new HashMap<Cell,SourceResult.Fragment>(); var ambiguous=new HashSet<Cell>();
        for(var f:result.fragments()) {
            if(f.hidden() || f.kind().equals("MERGED") || f.location().sheet()==null || f.location().row()==null || f.location().column()==null) { continue; }
            var cell=new Cell(f.location().sheet(),f.location().row(),f.location().column());
            if(cells.putIfAbsent(cell,f)!=null) { ambiguous.add(cell); }
        }
        for(var fragment:result.fragments()) {
            if(fragment.original()==null || fragment.hidden()) { continue; }
            var location=fragment.location();
            var labelCell=new Cell(location.sheet(),location.row(),location.column());
            if(ambiguous.contains(labelCell)) { continue; }
            String[] parts=fragment.original().split(":",2);
            String target=LABELS.get(normalize(parts[0]));
            if(target==null) { continue; }
            if(parts.length==2 && !parts[1].isBlank()) {
                String value=parts[1].strip();
                if(!value.contains(":")) { candidates.add(new SourcePrecedencePolicy.Candidate(fragment.id(),result.attachmentId(),target,value,source)); }
            } else if(location.sheet()!=null && location.row()!=null && location.column()!=null) {
                var next=new Cell(location.sheet(),location.row(),location.column()+1);
                var f=ambiguous.contains(next)?null:cells.get(next);
                if(f!=null && f.original()!=null && !f.original().isBlank()) {
                    // A second recognized label can begin another field, not supply this value.
                    if(LABELS.containsKey(normalize(f.original().split(":",2)[0]))) { continue; }
                    String value=AMOUNTS.contains(target) && f.storedValue()!=null && (f.kind().equals("NUMERIC") || f.kind().equals("FORMULA"))?f.storedValue():f.original();
                    candidates.add(new SourcePrecedencePolicy.Candidate(f.id(),result.attachmentId(),target,value,source));
                }
            }
        }
        return List.copyOf(candidates);
    }
    private static String normalize(String label) { return Normalizer.normalize(label,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]",""); }
}
