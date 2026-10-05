# Estructura inicial de ReggisApp

La aplicación se organiza por capas para separar la interfaz, los modelos, las reglas de negocio y el acceso a datos.

- `domain.model`: clientes, pedidos, pagos, movimientos de deuda, conversiones, historial y resultados de tiempo.
- `domain.service`: gestión de clientes, cobranza, tasa manual, cálculos de tiempo y selección de clientes para reportes.
- `repository`: contratos de persistencia.
- `repository.sqlite`: espacios para los futuros adaptadores JDBC de SQLite; todavía no conectan con la base de datos.
- `config`: configuración y creación de conexiones.
- `ui`: ventana principal, paneles y diálogos de escritorio.
- `report`: exportación de reportes Excel.
- `src/test`: pruebas de servicios y persistencia.

Los modelos mantienen sus atributos privados y no ofrecen mutadores para alterar el monto de un pedido o los registros del historial. Los servicios dependen de interfaces de repositorio, por lo que se prueban con mocks antes de conectar SQLite. La interfaz de escritorio, las implementaciones JDBC y la exportación de archivos Excel continúan pendientes.

## Reglas cubiertas por las pruebas

- El monto de pedidos y pagos debe ser positivo.
- Los pagos pueden registrarse en BS o USD; se conserva el monto original y la tasa ingresada, y el saldo se calcula en BS.
- Un pago no puede exceder el saldo pendiente. El pedido se completa cuando los pagos cubren el saldo total.
- La anulación requiere motivo y una resolución del saldo: condonarlo o trasladarlo a un pedido activo del mismo cliente.
- Los cálculos de tiempo cuentan desde la fecha del pedido e incluyen el intervalo hasta el primer pago. El promedio se redondea al día entero.
- Clientes inactivos y movimientos históricos se conservan.

Ejecutar las pruebas desde IntelliJ con el panel Maven, o desde una terminal con `mvn test`.
