# Registro de cambios

## 1.0.2 — 2026-10-05

### Corregido

- Se incluye JavaFX en el runtime empaquetado para evitar el error "Failed to launch JVM" al abrir la aplicación instalada.

## 1.0.1 — 2026-10-05

### Corregido

- Configuración del lanzador de Windows para cargar explícitamente JavaFX al abrir Reggis.

## 1.0.0 — 2026-10-04

Primera versión de escritorio de Reggis para registrar clientes y administrar sus cobranzas localmente.

### Incluido

- Interfaz JavaFX minimalista con accesos directos a alta de cliente y lista de clientes.
- Acción **Eliminar cliente** disponible exclusivamente dentro de la ficha individual del cliente, con confirmación e historial conservado.
- Icono de aplicación con una “R” blanca sobre fondo azul, aplicado a la ventana y al instalador `.exe`.
- Alta y edición de cliente con nombre y RIF obligatorios; correo, teléfono y dirección opcionales.
- Lista con estado del cliente y acceso a los pedidos mediante doble clic o el botón **Ver pedidos**.
- Varios pedidos por cliente, cada uno con su descripción, fecha de creación, monto en BS, moneda original y tasa de cambio manual.
- Registro de pagos parciales o totales en BS o USD, con fecha, tasa y equivalente contable en bolívares.
- Cierre automático de un pedido cuando su saldo llega a cero; cambia a estado `COMPLETADO` y deja de contarse como deuda pendiente.
- Cálculo del promedio entre pagos, espera hasta el primer pago y duración total de la deuda.
- Anulación con motivo, resolución, confirmación doble e historial de auditoría no modificable desde la aplicación ni mediante `UPDATE`/`DELETE` SQLite.
- Almacenamiento local SQLite versionado y operaciones transaccionales.
- Exportación de un cliente con todos sus pedidos, o de un pedido seleccionado, a archivos Excel `.xls` con datos de contacto, pagos y métricas.
- Pruebas JUnit/Mockito para reglas de negocio, controles JavaFX, exportación y seguridad de persistencia.
- Script de PowerShell para generar el instalador Windows `Reggis-1.0.0.exe` con `jpackage` y WiX Toolset.

### Fuera del alcance de 1.0.0

- Reportes mensuales consolidados.
- Consulta automática de tasa de cambio desde una API.
- Descarga e instalación automática de actualizaciones.
