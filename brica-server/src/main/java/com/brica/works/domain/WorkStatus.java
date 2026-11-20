package com.brica.works.domain;

public enum WorkStatus {
    IN_PROGRESS("En Proceso"), ON_HOLD("En pausa"), COMPLETED("Terminada"), CANCELLED("Cancelada");
    private final String label;
    WorkStatus(String label) { this.label = label; }
    public String label() { return label; }
}
