# Reggis Android — beta 0.1.0

Aplicación local para Android 8.0 o posterior, con SQLite interno y sin permiso de internet. Se mantiene la lógica Java de la aplicación de escritorio en `:core` y su exportador Excel en `:reports`; la interfaz se adapta a pantalla táctil con Compose.

## Funciones

- Clientes: registro, edición, búsqueda, campos adicionales, historial e inactivación con doble confirmación.
- Pedidos en BS/USD con tasa manual, marcas y conservación del catálogo elegido al crear el pedido.
- Pagos parciales y completos, prevención de sobrepagos, saldos y estadísticas temporales.
- Anulación con motivo: condonación o traslado a otro pedido activo del mismo cliente, con doble confirmación.
- Reportes Excel `.xls` por cliente o pedido mediante el selector de documentos de Android.
- Conversión manual BS/USD.

La base Android es independiente de Windows: no se importa ni sincroniza automáticamente. La beta es para uso en un dispositivo; no incluye bloqueo por identificador físico del teléfono. El diseño conserva la organización de información y las funciones, con controles nativos adaptados al móvil.

## Instalación y prueba

El APK firmado se entrega en `../dist/Reggis-Android-0.1.0-beta.apk`. Copiarlo al teléfono, abrirlo y autorizar la instalación para la aplicación desde la que se abre. No requiere una cuenta ni conexión a internet.

Probar primero con datos ficticios: crear cliente, crear pedido, registrar pago parcial, cerrar y volver a abrir, completar el pago y exportar el reporte. Repetir en modo avión. Comprobar también anulación y traslado de saldo con pedidos ficticios.

Los datos quedan en el almacenamiento privado de la aplicación. Desinstalarla o borrar sus datos elimina sus registros. Los reportes Excel son documentos de consulta, no copias restaurables de la base. La copia automática y la transferencia de datos entre dispositivos están desactivadas.

Si ya se instaló un APK de desarrollo, Android puede rechazar la actualización por usar otra firma. No desinstalar una copia con datos que se necesiten conservar; primero resolver su migración. Las próximas betas deben conservar la clave local de firma y aumentar `versionCode`.

## Desarrollo

Abrir esta carpeta en Android Studio y configurar el SDK mediante `local.properties` o la configuración del IDE. Gradle 9.3.1, AGP 9.1.1, JDK de Android Studio (25), fuentes Java 17, compile/target SDK 36, mínimo 26.

```powershell
.\gradlew.bat :core:test :reports:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:assembleRelease :app:lintRelease
```

La firma de beta usa `../.signing/reggis-beta.p12` y `../.signing/beta.properties` (propiedad `password`). Son archivos locales excluidos de Git. Conservarlos de forma privada para poder actualizar la aplicación; sin ellos la compilación release queda sin firmar. No incluirlos en la entrega del APK.

## Verificación

Pruebas locales: reglas originales, reportes originales, SQLite nativo con Robolectric en API 26 y 35, migración de esquema 1 a 2, precisión monetaria, concurrencia, rollback, aislamiento por cliente, consultas parametrizadas e historial inmutable. Pruebas Compose: creación/edición, recreación de pantalla y recorrido completo de pedidos, pagos, traslado e inactivación.

Validación realizada: 28 pruebas Android locales, 24 del núcleo compartido, 2 del exportador y 38 de escritorio, todas sin fallos. El análisis estático release terminó sin errores (una advertencia de versión de Gradle y dos sugerencias de optimización de estado).

`DeviceSmokeTest` también pasó dentro del emulador Android 16/API 36: creación de cliente y pedido, pago parcial, saldo exacto y generación/lectura de un Excel real. Se comprobó además la apertura de la aplicación. El primer arranque necesitó aceleración WHPX y varios minutos. La revisión en el teléfono físico de destino todavía debe realizarse; API 26 se cubrió con pruebas locales, no con un dispositivo físico.

El APK release firmado se instaló y abrió en frío en modo avión (estado confirmado). Se revisó visualmente la pantalla inicial y no hubo excepciones de AndroidRuntime durante esta comprobación. Evidencia local: `../dist/Reggis-Android-beta.png`.

Seguridad: sin permiso de red ni acceso amplio al almacenamiento, base privada, copias automáticas desactivadas, transacciones para cambios e historial, consultas parametrizadas, triggers de historial inmutable y APK release sin depuración. La base no tiene cifrado adicional ni autenticación propia; se apoya en el bloqueo y aislamiento del dispositivo, como alcance de esta beta.
