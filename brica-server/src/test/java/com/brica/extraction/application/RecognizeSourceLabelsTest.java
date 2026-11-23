package com.brica.extraction.application;
import com.brica.extraction.domain.SourceResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RecognizeSourceLabelsTest {
    private SourceResult.Fragment cell(int column,String text,String raw) { return new SourceResult.Fragment(UUID.randomUUID(),1,new SourceResult.Location(null,"Part",1,column,"cell:0000",null,null,null,null),text,null,raw,raw==null?"STRING":"NUMERIC",false,null); }
    @Test void textualContractNumbersKeepDisplayedLeadingZerosAndAmountsKeepStoredPrecision() {
        var code=new SourceResult(1,"a".repeat(64),"v1",List.of(cell(1,"Número de contrato",null),cell(2,"0012","12")),List.of());
        var result=RecognizeSourceLabels.propose(code,SourcePrecedencePolicy.SourceClass.CONTRACT);
        assertEquals("0012",result.getFirst().value());
        var amount=new SourceResult(1,"a".repeat(64),"v1",List.of(cell(1,"Monto contratado",null),cell(2,"1,234.57","1234.5678")),List.of());
        assertEquals("1234.5678",RecognizeSourceLabels.propose(amount,SourcePrecedencePolicy.SourceClass.CONTRACT).getFirst().value());
    }
    @Test void duplicateLocationsAndUnknownLabelsNeverChooseAnArbitraryValue() {
        var source=new SourceResult(1,"a".repeat(64),"v1",List.of(cell(1,"Número de contrato",null),cell(2,"A",null),cell(2,"B",null),cell(3,"Etiqueta desconocida",null)),List.of());
        assertTrue(RecognizeSourceLabels.propose(source,SourcePrecedencePolicy.SourceClass.CONTRACT).isEmpty());
    }
    @Test void recognizesAColonTerminatedTableLabelUsingTheValueCellAsEvidence() {
        var value=cell(2,"0012",null);
        var source=new SourceResult(1,"a".repeat(64),"v1",List.of(cell(1,"Número de contrato: ",null),value),List.of());
        var proposal=RecognizeSourceLabels.propose(source,SourcePrecedencePolicy.SourceClass.CONTRACT).getFirst();
        assertEquals("0012",proposal.value());
        assertEquals(value.id(),proposal.fragmentId());
        assertEquals("WORK_CONTRACT_NUMBER",proposal.target());
    }
    @Test void doesNotUseAnAdjacentLabelOrDuplicateLabelCellAsEvidence() {
        var value=cell(3,"Empresa de prueba",null);
        var source=new SourceResult(1,"a".repeat(64),"v1",List.of(cell(1,"Número de contrato:",null),cell(2,"Contratista:",null),value),List.of());
        var proposals=RecognizeSourceLabels.propose(source,SourcePrecedencePolicy.SourceClass.CONTRACT);
        assertEquals(1,proposals.size());
        assertEquals("WORK_CONTRACTOR",proposals.getFirst().target());
        var duplicate=new SourceResult(1,"a".repeat(64),"v1",List.of(cell(2,"Contratista:",null),cell(2,"Ubicación:",null),value),List.of());
        assertTrue(RecognizeSourceLabels.propose(duplicate,SourcePrecedencePolicy.SourceClass.CONTRACT).isEmpty());
    }
}
