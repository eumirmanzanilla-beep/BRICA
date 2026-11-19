package com.brica.extraction.domain;
import com.brica.server.shared.validation.domain.ExtractionRules;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.*;

@Entity @Table(name="extraction_mapping_profiles")
public class MappingProfile {
    public record Mapping(int schemaVersion,Map<String,Integer> columns,int firstRow,int lastRow,String decimalConvention) {
        public Mapping {
            columns=columns==null?Map.of():Map.copyOf(columns);
            ExtractionRules.require(schemaVersion==1 && columns.size()<=6 && firstRow>=1 && lastRow>=firstRow && lastRow<=20000,"mapping","Rango o esquema de mapeo inválido.");
            ExtractionRules.require(Set.of("DECIMAL_POINT","DECIMAL_COMMA").contains(decimalConvention),"decimalConvention","Selecciona una convención numérica explícita.");
            ExtractionRules.require(columns.getOrDefault("description",0)>0,"columns","Asocia la descripción.");
            columns.forEach((key,value)->ExtractionRules.require(Set.of("code","description","unit","quantity","price","amount").contains(key) && value>=0 && value<=256,"columns","Columnas permitidas entre 0 y 256; 0 indica ausente."));
        }
    }
    @Id private UUID id;
    @Column(name="work_id",nullable=false,updatable=false) private Long workId;
    @Column(nullable=false,updatable=false,length=100) private String name;
    @Column(nullable=false,updatable=false) private int version;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,updatable=false,columnDefinition="jsonb") private Mapping mapping;
    @Column(nullable=false,updatable=false,length=200) private String actor;
    @Column(name="created_at",nullable=false,updatable=false) private OffsetDateTime createdAt;
    protected MappingProfile() { }
    public MappingProfile(Long work,String name,int version,Mapping mapping,String actor) {
        ExtractionRules.text("name",name,100,true); id=UUID.randomUUID(); workId=work; this.name=name; this.version=version; this.mapping=mapping; this.actor=actor; createdAt=OffsetDateTime.now();
    }
    public UUID getId() { return id; } public Long getWorkId() { return workId; } public String getName() { return name; }
    public int getVersion() { return version; } public Mapping getMapping() { return mapping; }
}
