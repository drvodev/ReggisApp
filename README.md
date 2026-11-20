# Reggis

Reggis nació para resolver un problema cotidiano: llevar el control de los pedidos y las cobranzas de varios clientes sin depender de anotaciones dispersas ni hojas de cálculo difíciles de mantener.

Es una aplicación local para registrar ventas a crédito, gestionar abonos en bolívares o dólares, consultar saldos y conservar el historial de cada operación. No requiere servidor ni conexión a Internet.

## Estado del proyecto

| Plataforma | Versión | Tecnología |
| --- | --- | --- |
| Windows | 1.1.1 | Java 25, JavaFX, Maven, SQLite JDBC |
| Android | 0.1.0 beta | Kotlin, Jetpack Compose, Gradle, SQLite nativo |

Ambas aplicaciones reutilizan el mismo dominio Java y el exportador Excel. Las bases de datos son independientes; todavía no existe sincronización ni migración automática entre dispositivos.

## Funcionalidades

- Gestión de clientes, datos de contacto y campos adicionales.
- Varios pedidos por cliente, con descripción, moneda, tasa de cambio y marcas seleccionadas.
- Abonos parciales o completos, con validación del saldo pendiente.
- Historial, tiempos de pago y estado de cada pedido.
- Anulación justificada, con condonación o traslado de saldo a otro pedido activo del mismo cliente.
- Reportes Excel (`.xls`) por cliente o pedido.

## Ejecutar en escritorio

Necesitas JDK 25 y Maven 3.9 o el Maven incluido en IntelliJ IDEA.

```powershell
mvn clean verify
mvn javafx:run
```

Para generar el instalador Windows necesitas WiX Toolset:

```powershell
./scripts/crear-instalador-windows.ps1
```

## Ejecutar en Android

Abre `android/` en Android Studio y configura el SDK 36. La aplicación admite Android 8.0 (API 26) o posterior.

```powershell
cd android
.\gradlew.bat :core:test :reports:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Consulta la [guía de Android](android/README.md) para instalación, pruebas en dispositivos y firma del APK.

## Arquitectura

El dominio contiene las reglas de negocio y depende de contratos de repositorio, no de JavaFX, Android o JDBC. Cada plataforma proporciona sus adaptadores de persistencia e interfaz. Los servicios coordinan las operaciones transaccionales; los importes usan `BigDecimal` y las consultas parametrizadas separan los datos del SQL.

| Directorio | Responsabilidad |
| --- | --- |
| `src/` | Dominio compartido, escritorio JavaFX, SQLite JDBC y pruebas Maven |
| `android/` | Interfaz Compose, almacenamiento Android y pruebas móviles |
| `core/` | Módulo Gradle que reutiliza los modelos, servicios y contratos Java |
| `reporting/` | Módulo Gradle del exportador Excel con Apache POI |
| `docs/` | Arquitectura, estructura y publicación |

## Datos y límites

En Windows, la base se guarda en `%LOCALAPPDATA%\ReggisApp\reggisapp.db` (alternativa: `~/.reggisapp/reggisapp.db`). Cierra la aplicación antes de hacer una copia. En Android, los datos quedan en almacenamiento privado; borrar los datos de la app o desinstalarla elimina los registros. Los Excel no son copias restaurables de la base.

Reggis no proporciona cifrado adicional, autenticación propia, tasa de cambio automática, sincronización ni actualización automática. La beta Android debe validarse en el dispositivo de destino antes de confiarle datos reales.

## Documentación

- [Arquitectura y reglas de negocio](docs/arquitectura.md)
- [Estructura del proyecto](docs/estructura.md)
- [Instalador y versionado](docs/instalador-y-versiones.md)
- [Núcleo compartido](core/README.md)
- [Historial de cambios](CHANGELOG.md)
