package com.brica.budgets.domain;
import org.junit.jupiter.api.Test;
import com.brica.server.shared.validation.domain.InvalidDataException;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BudgetDraftTest {
    @Test void checksDeclaredItemSumEvenWhenQuantitiesAndPricesAreMissing() {
        var items=List.of(new BudgetDraft.Item(UUID.randomUUID(),0,null,"A","Primero",null,null,null,new BigDecimal("10"),null,null),
                new BudgetDraft.Item(UUID.randomUUID(),1,null,"A","Segundo",null,null,null,new BigDecimal("20"),null,null));
        var draft=new BudgetDraft(UUID.randomUUID(),null,null,"Prueba",null,List.of(),items,List.of(),new BigDecimal("25"),null,null,"La fuente declara un ajuste global pendiente");
        var differences=draft.validate();
        assertEquals(1,differences.size()); assertEquals("subtotal:declared-items",differences.getFirst().location());
        assertEquals(new BigDecimal("-5"),differences.getFirst().difference());
    }
    @Test void roundsPerConceptWithoutChangingSourceAmountsAndRequiresJustification() {
        var items=new ArrayList<BudgetDraft.Item>();
        for(int i=0;i<3;i++) { items.add(new BudgetDraft.Item(UUID.randomUUID(),i,null,"0012","Descripción","u",BigDecimal.ONE,new BigDecimal("0.335"),null,null,null)); }
        var invalid=new BudgetDraft(UUID.randomUUID(),null,null,"Prueba",null,List.of(),items,List.of(),new BigDecimal("1.01"),null,null,null);
        assertThrows(InvalidDataException.class,invalid::validate);
        var valid=new BudgetDraft(invalid.blockId(),null,null,"Prueba",null,List.of(),items,List.of(),invalid.subtotal(),null,null,"La fuente redondea al final del presupuesto");
        var difference=valid.validate().getFirst();
        assertEquals(new BigDecimal("1.02"),difference.calculated()); assertEquals(new BigDecimal("-0.01"),difference.difference());
        assertEquals(new BigDecimal("1.01"),valid.subtotal()); assertEquals(new BigDecimal("0.335"),valid.items().getFirst().price());
    }
}
