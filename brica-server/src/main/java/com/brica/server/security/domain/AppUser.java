package com.brica.server.security.domain;

import java.util.LinkedHashMap;

import com.brica.server.shared.validation.domain.InputRules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    @Column(nullable = false, length = 100)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(nullable = false, length = 100)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private UserStatus status;

    protected AppUser() { }

    public AppUser(String username, String name, Role role, String passwordHash) {
        var errors = new LinkedHashMap<String, String>();
        InputRules.requiredText(errors, "username", username, 100);
        InputRules.requiredText(errors, "name", name, 100);
        if (role == null) { errors.put("role", "Selecciona un rol registrado."); }
        InputRules.requiredText(errors, "passwordHash", passwordHash, 255);
        InputRules.rejectErrors(errors);
        this.username = username;
        this.name = name;
        this.role = role;
        this.passwordHash = passwordHash;
        this.status = UserStatus.ACTIVE;
    }

    public void setActive(boolean active) { status = active ? UserStatus.ACTIVE : UserStatus.INACTIVE; }
    public void replacePasswordHash(String hash) {
        var errors = new LinkedHashMap<String, String>();
        InputRules.requiredText(errors, "passwordHash", hash, 255);
        InputRules.rejectErrors(errors);
        passwordHash = hash;
    }
    public Long getId() { return id; }
    public Role getRole() { return role; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
    public boolean isActive() { return status == UserStatus.ACTIVE; }
}
