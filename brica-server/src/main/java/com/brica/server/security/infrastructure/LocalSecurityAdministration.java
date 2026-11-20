package com.brica.server.security.infrastructure;

import java.io.Console;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.brica.server.security.application.SecurityAdministrationService;
import com.brica.server.security.application.port.SecurityAdministrationStore;
import com.brica.server.shared.validation.domain.InvalidDataException;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("security-admin")
@ConditionalOnNotWebApplication
public class LocalSecurityAdministration {
    @Bean
    SecurityAdministrationService securityAdministrationService(SecurityAdministrationStore store, PasswordEncoder passwords) {
        return new SecurityAdministrationService(store, passwords);
    }

    @Bean
    ApplicationRunner securityAdministrationRunner(SecurityAdministrationService service) {
        return args -> {
            Console console = System.console();
            if (console == null) { throw new IllegalStateException("Ejecuta la administración desde una terminal interactiva, sin redirección de entrada."); }
            console.printf("Administración local BRICA. Acceso restringido por el sistema operativo.%n");
            while (true) {
                String action = console.readLine("Acción [role/user/permissions/user-state/role-state/password/exit]: ");
                if (action == null || action.equals("exit")) { System.exit(0); }
                try {
                    switch (action) {
                        case "role" -> {
                            var role = service.createRole(console.readLine("Nombre del rol: "),
                                    Integer.parseInt(console.readLine("Nivel jerárquico: ")),
                                    console.readLine("Descripción: "), keys(console));
                            console.printf("Rol creado: %d%n", role.getId());
                        }
                        case "user" -> {
                            String username = console.readLine("Usuario (exacto): ");
                            String name = console.readLine("Nombre visible: ");
                            Long roleId = Long.valueOf(console.readLine("ID del rol: "));
                            withPassword(console, password -> service.createUser(username, name, roleId, password));
                            console.printf("Usuario creado.%n");
                        }
                        case "permissions" -> service.replaceRolePermissions(Long.valueOf(console.readLine("ID del rol: ")), keys(console));
                        case "user-state" -> service.setUserActive(console.readLine("Usuario: "), active(console));
                        case "role-state" -> service.setRoleActive(Long.valueOf(console.readLine("ID del rol: ")), active(console));
                        case "password" -> {
                            String username = console.readLine("Usuario: ");
                            withPassword(console, password -> service.resetPassword(username, password));
                        }
                        default -> console.printf("Acción no reconocida.%n");
                    }
                    console.printf("Operación completada.%n");
                } catch (InvalidDataException failure) {
                    console.printf("Revisa los datos: %s%n", failure.getErrors());
                } catch (DataIntegrityViolationException failure) {
                    console.printf("No se guardó la operación. Comprueba duplicados y referencias.%n");
                } catch (NumberFormatException failure) {
                    console.printf("Indica un número válido.%n");
                }
            }
        };
    }

    private static Set<String> keys(Console console) {
        String input = console.readLine("Permisos separados por coma (vacío = ninguno): ");
        return input.isBlank() ? Set.of() : Arrays.stream(input.split(",")).map(String::strip).collect(Collectors.toSet());
    }
    private static boolean active(Console console) {
        String value = console.readLine("Estado [1=activo, 0=inactivo]: ");
        if (!value.equals("1") && !value.equals("0")) { throw new InvalidDataException(java.util.Map.of("status", "Usa 0 o 1.")); }
        return value.equals("1");
    }
    private static void withPassword(Console console, java.util.function.Consumer<CharSequence> action) {
        char[] password = console.readPassword("Contraseña: ");
        char[] confirmation = console.readPassword("Confirmar contraseña: ");
        if (password == null || confirmation == null) { throw new IllegalStateException("Entrada cancelada."); }
        try {
            if (!Arrays.equals(password, confirmation)) {
                throw new InvalidDataException(java.util.Map.of("password", "Las contraseñas no coinciden."));
            }
            action.accept(CharBuffer.wrap(password));
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
        }
    }
}
