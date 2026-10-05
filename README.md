# ReggisApp

Aplicación desktop preliminar para registrar clientes, pedidos, pagos e historial de cobranza. La interfaz está construida con JavaFX y la persistencia local usa SQLite por JDBC.

## Requisitos

- JDK 25
- Maven (incluido en IntelliJ IDEA o Maven 3.9+)
- Windows para generar el instalador `.exe`

## Ejecutar en IntelliJ

Abre `pom.xml` como proyecto Maven y ejecuta `javafx:run` desde la ventana Maven. Desde PowerShell, en la raíz del proyecto:

```powershell
mvn javafx:run
```

La base de datos se crea bajo `%LOCALAPPDATA%\ReggisApp\reggisapp.db` (o `~/.reggisapp` como alternativa). No se guarda junto al ejecutable, por lo que puede mantenerse al instalar una versión nueva.

## Pruebas

```powershell
mvn test
```

JUnit y Mockito cubren las reglas de servicios; las pruebas de interfaz ejecutan JavaFX y verifican controles, estilos y valores de la tabla. Las pruebas SQLite usan bases temporales e incluyen intentos de inyección SQL, persistencia literal de texto y rollback transaccional.

## Diseño

El código separa presentación (`ui`), servicios de negocio (`domain.service`), modelos (`domain.model`) y contratos de persistencia (`repository`). `AppServices` configura SQLite y compone las dependencias. Los repositorios JDBC usan `PreparedStatement`; los servicios agrupan operaciones relacionadas en transacciones. Los importes contables se manejan como `BigDecimal` en BS, preservando la moneda y tasa originales por operación.

Ver [arquitectura](docs/arquitectura.md), [estructura](docs/estructura.md) e [instalador y versiones](docs/instalador-y-versiones.md).

## Alcance de esta versión

Incluye clientes, pedidos, pagos, conversión manual de moneda, historial local y cálculos de tiempo. Los reportes XLSX y reportes mensuales siguen pendientes. La interfaz es una primera versión y no sustituye todavía una revisión de aceptación en el puesto final.
