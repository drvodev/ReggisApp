# Instalador y versiones de Reggis

## Versión actual

La versión del proyecto es **1.0.0**, definida en `<version>` de `pom.xml`. El ejecutable que se instala se presenta como Reggis; el JAR principal se llama `ReggisApp-1.0.0.jar` porque conserva el nombre técnico del proyecto.

## Requisitos para crear el instalador

- Windows x64.
- JDK 25 con `jpackage` disponible en `JAVA_HOME` o `PATH`.
- Maven 3.9+ o Maven integrado de IntelliJ IDEA.
- WiX Toolset instalado; `jpackage` lo necesita para producir el paquete de instalación `.exe`.

## Compilar y generar el `.exe`

Primero valida el código y las pruebas:

```powershell
mvn clean verify
```

Después, desde PowerShell y la raíz del proyecto, ejecuta:

```powershell
./scripts/crear-instalador-windows.ps1
```

El script compila el JAR y sus dependencias de ejecución, y llama a `jpackage`. Incluye el icono de Reggis (una “R” blanca sobre fondo azul) en el ejecutable instalado. El instalador se guarda en `dist/Reggis-1.0.0.exe`. El script omite las pruebas durante el empaquetado, por eso se ejecutan antes con `mvn clean verify`.

## Versiones siguientes

Actualiza `<version>` en `pom.xml` para cada publicación. Usa `1.1.0` para una ampliación compatible y `2.0.0` para un cambio mayor. Conserva el valor `--win-upgrade-uuid` del script entre publicaciones para que Windows reconozca cada instalador como una actualización de la misma aplicación.

La versión afecta el nombre del archivo `.exe`; para futuras publicaciones puede añadirse una etiqueta Git, por ejemplo `v1.0.0`, y adjuntar el instalador al lanzamiento correspondiente.

## Base local durante una actualización

La base SQLite permanece en `%LOCALAPPDATA%\ReggisApp\reggisapp.db`, fuera de la carpeta de instalación. Se mantiene esa ruta técnica para que los datos locales creados antes del cambio de marca sigan disponibles. Antes de instalar una versión nueva, cierra Reggis y conserva una copia del archivo de base de datos.

`DatabaseConfig` controla las migraciones mediante `PRAGMA user_version`. El instalador no reemplaza ni migra manualmente la base; la aplicación aplica las migraciones compatibles al arrancar.

## Actualización automática

El instalador permite publicar e instalar versiones nuevas manualmente; no descarga actualizaciones por sí solo. Una actualización automática requeriría un servicio de publicación y un proceso futuro para comprobar, descargar y verificar versiones.
