```
BRICA/
├── README.md                         # Presentación, instalación y uso del proyecto completo.
├── AGENTS.md                         # Instrucciones de trabajo ya existentes.
├── CONTRIBUTING.md                   # Convenciones de colaboración ya existentes.
├── pom.xml                           # POM padre: versiones y reactor Maven.
├── mvnw                               # Wrapper Maven para Linux y WSL.
├── mvnw.cmd                           # Wrapper Maven para Windows.
├── .mvn/                              # Configuración y binarios del Wrapper.
├── .gitignore                         # Excluye salidas, secretos y archivos locales.
├── .dockerignore                      # Reduce el contexto de construcción Docker.
├── .env.example                       # Variables de ejemplo, sin credenciales reales.
├── compose.yml                        # PostgreSQL y servicios locales definidos para el proyecto.
├── docs/                              # ERS, especificaciones y decisiones existentes del sistema.
├── brica-client/                       # Aplicación JavaFX; proyecto Maven hermano.
└── brica-server/                       # Aplicación Spring Boot; un proyecto Maven.
    ├── pom.xml                        # Dependencias y empaquetado exclusivos del servidor.
    ├── Dockerfile                     # Construcción y ejecución de su imagen.
    └── src/
        ├── main/
        │   ├── java/com/brica/        # Código Java bajo el paquete raíz existente.
        │   ├── python/                # Adaptador Python existente para análisis DOCX.
        │   └── resources/             # Configuración y migraciones incluidas en el artefacto.
        └── test/
            ├── java/com/brica/        # Pruebas Java organizadas por capacidad.
            └── resources/            # Fixtures y configuración exclusivos de pruebas.
```
