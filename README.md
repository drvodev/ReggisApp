# Reggis

**Reggis 1.0.2** es una aplicación de escritorio para gestionar clientes, pedidos y cobranzas. Está construida con Java y JavaFX, y almacena los datos localmente en SQLite.

Registra pagos parciales o completos en bolívares o dólares, conserva el historial, calcula los tiempos de pago y exporta reportes Excel (`.xls`) por cliente o pedido. Incluye un instalador para Windows.

## Funciones

- **Clientes:** nombre y RIF obligatorios; correo, teléfono y dirección opcionales. Cada cliente puede tener varios pedidos.
- **Pedidos:** descripción, monto, moneda original y tasa de cambio ingresada manualmente. El saldo contable se registra en bolívares (BS).
- **Pagos:** abonos parciales o totales en BS o USD. Cada pago guarda la fecha, el importe original, su equivalente en BS y la tasa aplicada.
- **Seguimiento:** consulta de pagos, historial, promedio de días entre pagos, días hasta el primer pago y duración total de la deuda.
- **Reportes Excel `.xls`:** exporta todos los pedidos y pagos de un cliente, o exporta el pedido seleccionado con sus pagos y métricas.
- **Trazabilidad:** el historial de pagos, cierres y anulaciones se conserva; las anulaciones requieren motivo y resolución.

## Requisitos

- JDK 25
- Maven 3.9+ o el Maven incluido con IntelliJ IDEA
- Windows x64 y WiX Toolset para crear el instalador `.exe`

## Ejecutar la aplicación

En IntelliJ IDEA, abre `pom.xml` como proyecto Maven y ejecuta `javafx:run` desde la ventana Maven. También puedes hacerlo desde PowerShell, en la carpeta del proyecto:

```powershell
mvn javafx:run
```

La ventana principal de Reggis permite registrar un cliente o abrir la lista de clientes. En la lista, haz doble clic sobre un cliente para consultar sus pedidos; también puedes seleccionarlo y usar **Ver pedidos**. En la ficha del cliente puedes crear pedidos, registrar pagos, revisar el historial y exportar reportes `.xls`. La acción **Eliminar cliente** está disponible únicamente en esa ficha y conserva el historial del cliente.

## Datos locales

SQLite crea la base de datos en `%LOCALAPPDATA%\ReggisApp\reggisapp.db`; si Windows no define esa variable, utiliza `~/.reggisapp/reggisapp.db`. La carpeta de datos conserva el nombre técnico anterior para mantener las bases de datos locales existentes. El archivo no se guarda dentro de la instalación de Reggis.

Antes de reinstalar o mover el programa, conserva una copia de ese archivo. Las versiones del esquema se controlan con `PRAGMA user_version`.

## Pruebas

Ejecuta la suite con:

```powershell
mvn clean verify
```

JUnit y Mockito cubren las reglas de clientes, pedidos, pagos, conversión y tiempo. Las pruebas de interfaz verifican botones, navegación y carga de clientes. Las pruebas SQLite revisan persistencia, transacciones y entradas de inyección SQL. Las pruebas del exportador abren los archivos `.xls` generados y comprueban sus datos.

## Generar el instalador `.exe`

Desde PowerShell, en la raíz del proyecto:

```powershell
./scripts/crear-instalador-windows.ps1
```

Se genera `dist/Reggis-1.0.2.exe` con el icono de la aplicación (una “R” blanca sobre fondo azul). El equipo que lo construya necesita JDK 25, Maven y WiX Toolset. La guía de [instalación y versionado](docs/instalador-y-versiones.md) describe la actualización de versiones.

## Estructura y diseño

El código separa presentación (`ui`), casos de uso (`domain.service`), modelos (`domain.model`) y persistencia (`repository`). `AppServices` compone SQLite, repositorios y servicios. Los importes se calculan con `BigDecimal`; los repositorios usan sentencias parametrizadas. Consulta la [arquitectura](docs/arquitectura.md), la [estructura del proyecto](docs/estructura.md) y el [registro de cambios](CHANGELOG.md) para ver las reglas, responsabilidades e historia de versiones.

La versión 1.0 incluye la gestión local de clientes, pedidos, pagos, historial y reportes individuales `.xls`. Los reportes mensuales y la actualización automática de la aplicación están previstos para futuras versiones.
