# Núcleo compartido de Reggis

Módulo Java sin dependencias de Android, JavaFX, JDBC o Apache POI. Android lo consume mediante `implementation project(':core')`.

Se reutilizan directamente los archivos originales de `src/main/java` y las pruebas de `src/test/java`; no hay copias. Maven continúa compilando esas mismas fuentes para escritorio. La separación es mediante los límites del módulo Gradle, sin mover los archivos del proyecto Maven.

Incluye modelos, excepciones, servicios, interfaces de repositorios y los contratos/datos de reportes. Excluye los adaptadores SQLite/JDBC, la configuración de escritorio, las pantallas y el exportador Apache POI. La compilación con Java 17 y sin dependencias de producción ayuda a detectar dependencias accidentales de plataforma.

Desde `android`, ejecutar `./gradlew.bat :core:test :app:assembleDebug :app:lintDebug`.

Las pruebas originales de dominio se ejecutan con JUnit y Mockito en la computadora. Mockito se carga como agente al iniciar la JVM, sin adjuntarse dinámicamente. Son pruebas JVM, no pruebas ejecutadas en el teléfono.

Android activa la adaptación de APIs Java mediante `desugar_jdk_libs` para la compatibilidad con Android 8. El análisis estático y la compilación del APK verifican la integración, pero todavía debe comprobarse en dispositivo.

Las pruebas SQLite de escritorio siguen en Maven. Android cuenta con su propia suite de persistencia en `android/app/src/test`, incluyendo transacciones, historial y migraciones. Las pruebas de dominio no sustituyen las pruebas de los adaptadores ni la validación física.

En los tres servicios que construyen listas desde streams se usa `Collectors.collectingAndThen` con `Collections.unmodifiableList`, conservando orden e inmutabilidad y evitando depender de `Stream.toList` (Java 16) en teléfonos antiguos.
