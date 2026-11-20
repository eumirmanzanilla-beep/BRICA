package com.brica.server.security.domain;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import com.brica.server.shared.validation.domain.InputRules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "hierarchy_level", nullable = false)
    private int hierarchyLevel;
    private String description;
    @Column(nullable = false)
    private int status;
    @ManyToMany
    @JoinTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    protected Role() { }

    public Role(String name, int hierarchyLevel, String description, Set<Permission> permissions) {
        var errors = new LinkedHashMap<String, String>();
        InputRules.requiredText(errors, "name", name, 100);
        InputRules.rejectErrors(errors);
        this.name = name;
        this.hierarchyLevel = hierarchyLevel;
        this.description = description;
        this.status = 1;
        replacePermissions(permissions);
    }

    public void replacePermissions(Set<Permission> permissions) {
        var copy = Set.copyOf(permissions);
        this.permissions.clear();
        this.permissions.addAll(copy);
    }

    public void setActive(boolean active) { status = active ? 1 : 0; }
    public boolean isActive() { return status == 1; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public int getHierarchyLevel() { return hierarchyLevel; }
    public Set<Permission> getPermissions() { return Set.copyOf(permissions); }
}
