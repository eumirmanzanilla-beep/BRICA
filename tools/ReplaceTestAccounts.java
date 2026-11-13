import java.sql.DriverManager;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import com.brica.server.security.domain.PasswordPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

/** Explicit local administration after backup; never invoked by application startup. */
public class ReplaceTestAccounts {
    public static void main(String[] args) throws Exception {
        if (!"brica".equals(System.getenv("BRICA_CONFIRM_REPLACE_ACCOUNTS"))) {
            throw new IllegalStateException("Falta autorización explícita para sustituir las tres cuentas indicadas.");
        }
        String password = System.getenv("BRICA_TEST_PASSWORD"); PasswordPolicy.validate(password);
        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        try (var connection = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"))) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("LOCK TABLE app_users, roles, role_permissions IN EXCLUSIVE MODE");
                try (var result = statement.executeQuery("SELECT current_database(), extract(epoch FROM pg_postmaster_start_time())::text")) {
                    result.next();
                    if (!result.getString(1).equals("brica") || !result.getString(2).equals(System.getenv("BRICA_EXPECTED_DB_START"))) {
                        throw new IllegalStateException("La identidad de la base cambió. No se sustituyó ninguna cuenta.");
                    }
                }
                var actual = new HashSet<String>();
                try (var result = statement.executeQuery("SELECT username FROM app_users")) { while (result.next()) { actual.add(result.getString(1)); } }
                if (!actual.equals(Set.of("arqAdiran", "UserTest", "CarlosYonson"))) {
                    throw new IllegalStateException("El conjunto de cuentas cambió. Se requiere revisión antes de sustituirlo.");
                }
                try (var result = statement.executeQuery("SELECT count(*) FROM roles WHERE (name='Gerencial' AND hierarchy_level=4 OR name='Administrativo' AND hierarchy_level=3) AND status=1")) {
                    result.next(); if (result.getInt(1) != 2) { throw new IllegalStateException("Roles estándar ausentes o inconsistentes."); }
                }
                int deleted = statement.executeUpdate("DELETE FROM app_users WHERE username IN ('arqAdiran','UserTest','CarlosYonson')");
                if (deleted != 3) { throw new IllegalStateException("No coincide el conjunto de cuentas autorizado."); }
                try (var insert = connection.prepareStatement("INSERT INTO app_users(role_id,username,password_hash,name,status) SELECT id_role,?,?,?,'ACTIVE' FROM roles WHERE name=?")) {
                    for (var entry : Map.of("UsuarioGerencial", "Gerencial", "UsuarioAdministrativo", "Administrativo").entrySet()) {
                        insert.setString(1, entry.getKey()); insert.setString(2, encoder.encode(password));
                        insert.setString(3, entry.getKey()); insert.setString(4, entry.getValue());
                        if (insert.executeUpdate() != 1) { throw new IllegalStateException("No se pudo asignar el rol."); }
                    }
                }
                connection.commit();
            } catch (Exception failure) { connection.rollback(); throw failure; }
        }
        System.out.println("Sustituidas únicamente las tres cuentas autorizadas por UsuarioGerencial y UsuarioAdministrativo.");
    }
}
