package com.brica.server.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class SecurityTestData {
    private SecurityTestData() { }

    public static void installAdmin(JdbcTemplate jdbc, PasswordEncoder passwords) {
        install(jdbc, passwords, "IntegrationAdmin", "all");
    }

    public static void install(JdbcTemplate jdbc, PasswordEncoder passwords, String username, String... keys) {
        String roleName = "test-role-" + username;
        jdbc.update("INSERT INTO roles (name, hierarchy_level, status) VALUES (?, 1, 1) ON CONFLICT (name) DO NOTHING", roleName);
        Long roleId = jdbc.queryForObject("SELECT id_role FROM roles WHERE name = ?", Long.class, roleName);
        if (keys.length == 1 && keys[0].equals("all")) {
            jdbc.update("INSERT INTO role_permissions SELECT ?, id_permission FROM permissions ON CONFLICT DO NOTHING", roleId);
        } else {
            for (String key : keys) {
                jdbc.update("INSERT INTO role_permissions SELECT ?, id_permission FROM permissions WHERE key = ? ON CONFLICT DO NOTHING", roleId, key);
            }
        }
        jdbc.update("""
                INSERT INTO app_users (role_id, username, name, password_hash, status)
                VALUES (?, ?, 'Usuario de prueba', ?, 'ACTIVE') ON CONFLICT (username) DO NOTHING
                """, roleId, username, passwords.encode("TestUser123"));
    }
}
