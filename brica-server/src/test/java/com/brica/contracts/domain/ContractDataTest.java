package com.brica.contracts.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContractDataTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldPreserveNullAndProtectNestedDataFromMutation() {
        var items = new ArrayList<Object>();
        items.add(null);
        var nested = new HashMap<String, Object>();
        nested.put("items", items);
        var source = new HashMap<String, Object>();
        source.put("amount", null);
        source.put("nested", nested);
        var copy = ContractData.copy(source);
        source.put("amount", 10);
        items.add("later");
        assertNull(copy.get("amount"));
        var copiedNested = (Map<String, Object>) copy.get("nested");
        assertEquals(1, ((List<?>) copiedNested.get("items")).size());
        assertThrows(UnsupportedOperationException.class, () -> copiedNested.put("changed", true));
        assertThrows(UnsupportedOperationException.class, () -> ((List<Object>) copiedNested.get("items")).add(1));
    }
}
