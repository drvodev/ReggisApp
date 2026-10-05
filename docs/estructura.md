# Estructura del proyecto

ReggisApp utiliza la estructura estándar de Maven y separa el código de producción de las pruebas:

```text
ReggisApp/
├── pom.xml
├── src/
│   ├── main/java/org/drvo/reggisapp/
│   │   ├── config/             configuración y conexiones
│   │   ├── domain/
│   │   │   ├── exception/      excepciones de dominio y persistencia
│   │   │   ├── model/          modelos, estados y valores del dominio
│   │   │   └── service/        interfaces y lógica de aplicación
│   │   ├── repository/         contratos de persistencia
│   │   │   └── sqlite/         futuros adaptadores SQLite/JDBC
│   │   ├── report/             contratos para exportar reportes
│   │   ├── ui/                 vistas y estilos JavaFX preliminares
│   │   └── ReggisApp.java      punto de entrada pendiente
│   └── test/java/org/drvo/reggisapp/
│       └── domain/service/     pruebas JUnit y Mockito
└── docs/                       documentación del proyecto
```

`README.md` resume requisitos, ejecución y estado. [arquitectura.md](arquitectura.md) explica las decisiones, reglas y componentes pendientes. La interfaz contiene una primera vista navegable con datos ficticios temporales. Los paquetes `config` y `repository/sqlite`, al igual que la exportación, aún no implementan almacenamiento real.
