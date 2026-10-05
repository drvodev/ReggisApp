# Estructura del proyecto

Reggis sigue la estructura estándar de Maven. El nombre que ve el usuario es Reggis; el artefacto Maven, el paquete Java y la carpeta de la base local conservan `ReggisApp` para mantener compatibilidad con el proyecto y los datos existentes.

```text
ReggisApp/
├── pom.xml
├── scripts/
│   └── crear-instalador-windows.ps1   creación del instalador .exe
├── docs/                              documentación técnica y de versiones
└── src/
    ├── main/
    │   ├── java/org/drvo/reggisapp/
    │   │   ├── config/                inicialización SQLite y composición de servicios
    │   │   ├── domain/
    │   │   │   ├── exception/         errores de negocio y persistencia
    │   │   │   ├── model/             clientes, pedidos, pagos y valores de dominio
    │   │   │   └── service/           reglas y operaciones de negocio
    │   │   ├── repository/            contratos de persistencia y sqlite/
    │   │   ├── report/                contrato, datos y exportación de archivos .xls
    │   │   ├── ui/                    pantallas JavaFX
    │   │   └── ReggisApp.java         punto de entrada JavaFX
    │   └── resources/org/drvo/reggisapp/ui/
    │       └── estilos.css            estilos CSS de la aplicación
    └── test/java/org/drvo/reggisapp/
        ├── domain/service/            pruebas unitarias JUnit y Mockito
        ├── report/                    pruebas de archivos .xls
        ├── repository/sqlite/         pruebas de persistencia y seguridad SQL
        └── ui/                        pruebas de navegación y controles JavaFX
```

## Flujo de una operación

1. `ReggisApp` inicia JavaFX y crea `AppServices`.
2. `AppServices` configura la base local, los repositorios SQLite y las implementaciones de servicio.
3. `VentanaPrincipal` muestra la interfaz y envía las acciones del usuario a los servicios.
4. Los servicios validan las reglas y usan los repositorios para guardar los datos y el historial dentro de transacciones.
5. `XlsReportExporter` genera reportes que la interfaz guarda en la ubicación elegida.

La descripción de las capas, reglas monetarias, estados y pruebas está en [arquitectura](arquitectura.md). Los pasos para generar una versión instalable están en [instalador y versiones](instalador-y-versiones.md).
