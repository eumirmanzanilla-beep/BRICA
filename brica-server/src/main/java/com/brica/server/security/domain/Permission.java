package com.brica.server.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permission")
    private Long id;
    @Column(nullable = false, length = 100)
    private String key;
    @Column(nullable = false, length = 150)
    private String name;
    private String description;

    protected Permission() { }

    public Long getId() { return id; }
    public String getKey() { return key; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
