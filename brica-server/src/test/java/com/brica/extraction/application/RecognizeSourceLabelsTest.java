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
}
