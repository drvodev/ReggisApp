# Instalador y versiones de ReggisApp

## Generar el instalador de Windows

Requisitos del equipo de compilación:

- Windows x64 y JDK 25.
- Maven.
- WiX Toolset, necesario por `jpackage` para producir instaladores Windows `.exe`.

En PowerShell, desde la raíz del proyecto:

```powershell
./scripts/crear-instalador-windows.ps1
```

El instalador versionado se genera en `dist/ReggisApp-<version>.exe`. El script compila sin ejecutar pruebas; las pruebas se deben ejecutar antes de publicar la versión.

## Publicar versiones posteriores

La versión proviene de `<version>` en `pom.xml`. Para una actualización compatible, cambia `1.0.0` a `1.1.0`; para una versión mayor con cambios incompatibles, usa `2.0.0`. Genera un instalador nuevo y distribúyelo. El UUID configurado en el script debe conservarse en todas las versiones para identificar las actualizaciones de la misma aplicación.

La aplicación guarda la base SQLite bajo `%LOCALAPPDATA%/ReggisApp/reggisapp.db`, fuera de la carpeta de instalación. Una actualización del programa no debe reemplazar ese archivo. Las migraciones del esquema se controlan aparte con `PRAGMA user_version` en `DatabaseConfig`.

Este mecanismo genera instaladores de actualización; no instala nuevas versiones automáticamente. Para autoactualización a futuro hará falta un canal de publicación y un proceso de descarga/verificación de versiones.
