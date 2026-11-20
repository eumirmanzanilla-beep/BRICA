import java.sql.DriverManager;
import java.util.Map;

import com.brica.server.security.domain.PasswordPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

/** Local review tooling only; not a Spring component, HTTP endpoint or migration. */
public class ReviewAccounts {
    public static void main(String[] args) throws Exception {
        String database = System.getenv("BRICA_REVIEW_DATABASE");
        String url = System.getenv("DB_URL");
        if (database == null || !database.matches("brica_review_[a-f0-9]{12}")
                || url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/" + database)) {
            throw new IllegalStateException("Solo se permite una base aislada creada por review-environment.py.");
        }
        String password = System.getenv("BRICA_REVIEW_PASSWORD");
        PasswordPolicy.validate(password);
        String hash = PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(password);
        try (var connection = DriverManager.getConnection(url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"))) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("LOCK TABLE app_users IN EXCLUSIVE MODE");
                try (var result = statement.executeQuery("SELECT current_database(), (SELECT count(*) FROM app_users)")) {
                    result.next();
                    if (!database.equals(result.getString(1)) || result.getLong(2) != 0) {
                        throw new IllegalStateException("La base debe ser la de revisión y no contener usuarios.");
                    }
                }
                try (var insert = connection.prepareStatement("""
                        INSERT INTO app_users (role_id, username, password_hash, name, status)
                        SELECT id_role, ?, ?, ?, 'ACTIVE' FROM roles WHERE name = ? AND status = 1
                        """)) {
                    for (var entry : Map.of("UsuarioGerencial", "Gerencial", "UsuarioAdministrativo", "Administrativo").entrySet()) {
                        insert.setString(1, entry.getKey()); insert.setString(2, hash);
                        insert.setString(3, entry.getKey()); insert.setString(4, entry.getValue());
                        if (insert.executeUpdate() != 1) { throw new IllegalStateException("Rol de revisión ausente."); }
                    }
                }
                connection.commit();
            } catch (Exception failure) { connection.rollback(); throw failure; }
        }
        System.out.println("Creadas únicamente UsuarioGerencial y UsuarioAdministrativo en la base aislada.");
    }
}
