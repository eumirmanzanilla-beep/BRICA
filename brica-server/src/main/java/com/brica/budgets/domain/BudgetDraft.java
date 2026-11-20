package com.brica.budgets.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import com.brica.server.shared.validation.domain.ExtractionRules;

/** Reviewed values only; declared amounts are never replaced by control calculations. */
public record BudgetDraft(UUID blockId, Long budgetId, String expectedRevision, String name, String currency,
        List<Section> sections, List<Item> items, List<String> notes,
        BigDecimal subtotal, BigDecimal tax, BigDecimal total, String justification) {
    public record Section(UUID id, int position, String title) { }
    public record Item(UUID id, int position, UUID sectionId, String code, String description, String unit,
            BigDecimal quantity, BigDecimal price, BigDecimal amount, UUID evidenceId, String originalMeasurement) { }
    public record Difference(String location, BigDecimal declared, BigDecimal calculated, BigDecimal difference) { }

    public BudgetDraft {
        sections = sections == null ? List.of() : List.copyOf(sections);
        items = items == null ? List.of() : List.copyOf(items);
        notes = notes == null ? List.of() : List.copyOf(notes);
        ExtractionRules.require(items.size()<=20000 && sections.size()<=200 && notes.size()<=200,"budget","El presupuesto supera el límite de filas.");
        ExtractionRules.text("name",name,255,false); ExtractionRules.text("justification",justification,2000,false);
        validateNumber("subtotal",subtotal); validateNumber("tax",tax); validateNumber("total",total);
        for(var item:items) {
            validateNumber("quantity",item.quantity()); validateNumber("price",item.price()); validateNumber("amount",item.amount());
            ExtractionRules.text("description",item.description(),16384,false); ExtractionRules.text("code",item.code(),255,false);
            ExtractionRules.text("unit",item.unit(),255,false); ExtractionRules.text("originalMeasurement",item.originalMeasurement(),1000,false);
        }
        sections.forEach(section->ExtractionRules.text("title",section.title(),1000,false));
        notes.forEach(note->ExtractionRules.text("note",note,16384,false));
    }
    public List<Difference> validate() {
        ExtractionRules.require(blockId != null, "blockId", "Identifica el bloque de presupuesto.");
        ExtractionRules.text("name", name, 255, true);
        ExtractionRules.require(currency == null || currency.matches("[A-Z]{3}"), "currency", "Moneda ISO de tres letras o sin registrar.");
        ExtractionRules.require(!items.isEmpty() && items.size() <= 20_000 && sections.size() <= 200 && notes.size() <= 200,
                "items", "Incluye entre 1 y 20000 conceptos y hasta 200 partidas/notas.");
        Set<UUID> ids = new HashSet<>(), sectionIds = new HashSet<>();
        Set<Integer> positions = new HashSet<>();
        for (var section : sections) {
            ExtractionRules.require(section.id() != null && ids.add(section.id()) && section.position() >= 0 && positions.add(section.position()), "sections", "Identidades y orden únicos.");
            ExtractionRules.text("section.title", section.title(), 1000, true);
            sectionIds.add(section.id());
        }
        var differences = new ArrayList<Difference>();
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal declaredSum = BigDecimal.ZERO;
        boolean complete = true;
        boolean declaredComplete = true;
        for (var item : items) {
            ExtractionRules.require(item.id() != null && ids.add(item.id()) && item.position() >= 0 && positions.add(item.position()), "items", "Identidades y orden únicos; las claves originales sí pueden repetirse.");
            ExtractionRules.require(item.sectionId() == null || sectionIds.contains(item.sectionId()), "sectionId", "La partida debe pertenecer a esta revisión.");
            ExtractionRules.text("description", item.description(), 16384, true);
            ExtractionRules.text("code", item.code(), 255, false);
            ExtractionRules.text("unit", item.unit(), 255, false);
            ExtractionRules.text("originalMeasurement", item.originalMeasurement(), 1000, false);
            validateNumber("quantity", item.quantity()); validateNumber("price", item.price()); validateNumber("amount", item.amount());
            if (item.amount() == null) { declaredComplete = false; } else { declaredSum = declaredSum.add(item.amount()); }
            if (item.quantity() != null && item.price() != null) {
                BigDecimal calculated = item.quantity().multiply(item.price()).setScale(2, RoundingMode.HALF_UP);
                sum = sum.add(calculated);
                difference(differences, item.id().toString(), item.amount(), calculated);
            } else { complete = false; }
        }
        notes.forEach(note -> ExtractionRules.text("note", note, 16384, true));
        for (BigDecimal value : new BigDecimal[]{subtotal, tax, total}) {
            if (value != null) { ExtractionRules.number("total", value.toPlainString()); }
        }
        if (complete) { difference(differences, "subtotal", subtotal, sum); }
        if (declaredComplete) { difference(differences, "subtotal:declared-items", subtotal, declaredSum); }
        if (subtotal != null && tax != null) { difference(differences, "total", total, subtotal.add(tax)); }
        boolean negative = items.stream().anyMatch(i -> i.quantity() != null && i.quantity().signum() < 0
                || i.price() != null && i.price().signum() < 0 || i.amount() != null && i.amount().signum() < 0);
        if (!differences.isEmpty() || negative) {
            ExtractionRules.text("justification", justification, 2000, true);
            ExtractionRules.require(justification.strip().length() >= 10, "justification", "Justifica expresamente las diferencias (mínimo 10 caracteres).");
        }
        return List.copyOf(differences);
    }
    private static void difference(List<Difference> result, String location, BigDecimal declared, BigDecimal calculated) {
        if (declared != null && declared.compareTo(calculated) != 0) {
            result.add(new Difference(location, declared, calculated, declared.subtract(calculated)));
        }
    }
    private static void validateNumber(String field, BigDecimal value) { if (value != null) { ExtractionRules.number(field, value.toString()); } }
}
