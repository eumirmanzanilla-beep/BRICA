package com.brica.contracts.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.brica.server.shared.validation.domain.InvalidDataException;

/** JSON source data is independent of the canonical information of a Work. */
final class ContractData {
    private ContractData() { }

    static Map<String, Object> copy(Map<String, Object> data) {
        var result = new LinkedHashMap<String, Object>();
        if (data != null) {
            data.forEach((key, value) -> {
                if (key == null || key.isBlank()) {
                    throw new InvalidDataException(Map.of("data", "Las claves no pueden estar vacías."));
                }
                result.put(key, copyValue(value));
            });
        }
        return Collections.unmodifiableMap(result);
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            var result = new LinkedHashMap<String, Object>();
            map.forEach((key, nested) -> {
                if (!(key instanceof String text) || text.isBlank()) {
                    throw new InvalidDataException(Map.of("data", "Las claves deben ser texto no vacío."));
                }
                result.put(text, copyValue(nested));
            });
            return Collections.unmodifiableMap(result);
        }
        if (value instanceof List<?> list) {
            var result = new ArrayList<Object>();
            list.forEach(item -> result.add(copyValue(item)));
            return Collections.unmodifiableList(result);
        }
        if (value == null || value instanceof String || value instanceof Boolean
                || value instanceof java.math.BigDecimal || value instanceof java.math.BigInteger
                || value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof Double number && Double.isFinite(number)
                || value instanceof Float number && Float.isFinite(number)) {
            return value;
        }
        throw new InvalidDataException(Map.of("data", "El valor debe ser compatible con JSON."));
    }
}
