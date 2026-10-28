package com.brica.contracts.domain;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.brica.works.domain.Work;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contract")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_id", nullable = false)
    private Work work;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", nullable = false)
    private Map<String, Object> data;

    protected Contract() {
    }

    public Contract(
            Work work,
            Map<String, Object> data) {

        this.work = work;
        if (work == null) {
            throw new com.brica.server.shared.validation.domain.InvalidDataException(
                    Map.of("workId", "La obra es obligatoria."));
        }
        this.data = ContractData.copy(data);
    }

    public Long getId() {
        return id;
    }

    public Work getWork() {
        return work;
    }

    public Map<String, Object> getData() {
        return ContractData.copy(data);
    }
}
