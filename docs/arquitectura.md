# Arquitectura y reglas de ReggisApp

## 1. Propósito

ReggisApp organiza los cobros de clientes con uno o varios pedidos activos. Cada pago queda ligado a un pedido específico. El sistema calcula el saldo y conserva los registros necesarios para conocer cuándo se creó el pedido, cuándo se realizaron los pagos y cuándo se completó o anuló.

Esta documentación describe la aplicación actual: la interfaz JavaFX usa los servicios de negocio y los datos se guardan en SQLite local.

## 2. Capas

```text
JavaFX (vista preliminar)
        │ invoca
        ▼
Interfaces de servicio ──► implementaciones de servicios
                                  │
                                  ├──► modelos y reglas de dominio
                                  └──► interfaces de repositorio
                                              │
                                              ▼
                                    adaptadores SQLite/JDBC
```

### Dominio (`domain.model`)

Contiene objetos del negocio sin depender de la interfaz ni del motor de base de datos. Sus atributos son privados y de solo lectura. Para cambiar el estado se crea una nueva instancia; por ejemplo, `Pedido.marcarCompletado()` devuelve el pedido con estado `COMPLETADO`.

Modelos principales:

- `Cliente`: identificador, nombre, estado y mapa de datos opcionales del cliente.
- `Pedido`: cliente, monto base en BS, importe/moneda originales, tasa, fecha de creación y estado.
- `Pago`: pedido, fecha de pago, valor en BS y valor/moneda/tasa ingresados.
- `MovimientoDeuda`: traspaso de un saldo desde un pedido anulado a otro pedido.
- `RegistroHistorial`: evento inmutable de auditoría asociado a una entidad.
- `ConversionMonetaria` y `Moneda`: resultado y unidad de la conversión.
- `ResumenTiempo`: promedios y duración del pago.
- `EstadoPedido`, `EstadoCliente` y `ResolucionAnulacion`: opciones representadas con enums.

### Servicios (`domain.service`)

Las interfaces describen operaciones que la interfaz gráfica puede invocar. Las clases `*Impl` implementan las reglas y coordinan repositorios.

- `CobranzaService`: crear pedido, registrar pago, calcular saldo, listar pedidos y anular un pedido con motivo y resolución.
- `ClienteService`: crear, actualizar e inactivar clientes; crear y consultar campos adicionales configurables.
- `TiempoService`: resumir los intervalos entre pagos y el tiempo total hasta saldar.
- `ReporteService`: validar y seleccionar clientes para un reporte.
- `TasaCambioService`: convertir importes; `TasaCambioManualService` implementa la conversión usando la tasa ingresada en el momento.

Los servicios reciben sus dependencias por el constructor. Esto se llama inyección de dependencias: hace explícito qué necesita cada clase y permite sustituir un repositorio real por un mock en pruebas.

### Persistencia (`repository`, `repository.sqlite`)

`ClienteRepository`, `PedidoRepository`, `PagoRepository` y los demás repositorios son contratos para guardar y consultar información. En `repository/sqlite` están los adaptadores JDBC activos. `DatabaseConfig` crea el archivo local y el esquema versionado; `SqliteConnectionFactory` abre conexiones y coordina transacciones. `AppServices` configura y compone repositorios y servicios. Las consultas usan `PreparedStatement` para separar el SQL de los datos ingresados.

Esta frontera permite que los servicios no conozcan sentencias SQL. Más adelante se puede agregar otro adaptador JDBC para PostgreSQL sin reescribir las reglas de cobranza, implementando los mismos contratos.

### Presentación (`ui`)

`ReggisApp` arranca JavaFX y `VentanaPrincipal` contiene navegación entre bienvenida, clientes y pedidos. Las vistas consultan los servicios, registran operaciones reales y solicitan confirmación doble para las acciones destructivas. La base local se conserva al cerrar la aplicación.

### Reportes (`report`)

`ExcelReportExporter` define el contrato de exportación y `XlsxReportExporter` reserva su implementación. Aún no se crea un archivo Excel.

## 3. Reglas de negocio codificadas

### Clientes

- El nombre es obligatorio.
- Un cliente puede estar activo o inactivo y conserva sus pedidos e historial.
- Se pueden agregar campos definidos por el usuario, como RIF, teléfono o correo.
- Inactivar evita crear nuevos pedidos; la eliminación física no forma parte del modelo actual.

### Pedidos y pagos

- El monto ingresado debe ser mayor que cero.
- Cada pedido pertenece a un cliente y su importe base se normaliza a BS.
- Se conserva el importe original, su moneda y la tasa usada para mantener el contexto del registro.
- El saldo es monto del pedido, más entradas de deuda trasladada, menos pagos y salidas de deuda trasladada.
- No se acepta un pago que supere el saldo pendiente.
- Cuando el saldo queda en cero, el pedido cambia a `COMPLETADO` y deja de ser activo.
- La fecha del pedido y la fecha de cada pago se determinan mediante `Clock`, lo que también permite fijar el tiempo en pruebas.

### Anulación y trazabilidad

- Solo se anulan pedidos activos con saldo positivo.
- La anulación exige motivo y resolución.
- `CONDONAR_SALDO` deja constancia de la decisión en historial.
- `TRASLADAR_SALDO` requiere un pedido destino activo y del mismo cliente; el sistema registra un `MovimientoDeuda`.
- El historial registra creación y actualización de cliente, pedido, pago, cierre y anulación.
- La clase `RegistroHistorial` no expone setters y los repositorios solo agregan y consultan eventos. Desde el esquema SQLite versión 2, triggers también rechazan `UPDATE` y `DELETE` directos sobre la tabla de historial.

### Moneda

- `BigDecimal` representa los importes y evita errores binarios típicos de `double`.
- BS es la unidad contable para los saldos y límites de pago.
- Se permiten entradas en BS o USD con una tasa manual positiva.
- El resultado se redondea a dos decimales usando `HALF_UP`.
- No hay integración con una API externa para obtener tasas.

### Métrica de tiempo

`TiempoServiceImpl` ordena pagos por fecha. El primer intervalo cuenta desde la creación del pedido hasta el primer pago; los demás, entre pagos consecutivos. Calcula el promedio de esos intervalos y lo redondea al día entero más cercano. La duración total se informa cuando los pagos acumulados alcanzan el monto del pedido.

## 4. Manejo de errores

- `ReglaNegocioException` comunica rechazos esperados, como monto inválido, cliente inexistente o pago excesivo.
- `PersistenciaException` está reservada para errores de almacenamiento.
- Los servicios validan la entrada antes de persistir.
- Las implementaciones JDBC capturan `SQLException`, la convierten a `PersistenciaException` y usan transacciones para que pago, cambio de estado e historial no queden a medias.
- La futura UI JavaFX traducirá esos errores en mensajes claros; no debería duplicar las reglas del servicio.

## 5. Pruebas

Las pruebas están en `src/test/java`. JUnit Jupiter ejecuta los casos y Mockito simula los repositorios en las pruebas unitarias. Las pruebas JavaFX verifican estilos, captura y carga de nombres en la tabla. Las pruebas SQLite usan una base temporal para validar persistencia, rollback y cadenas de inyección SQL como entradas literales.

Los casos cubiertos incluyen validación de importes, conversión manual, exceso de pago, cierre de pedido, anulación y traslado de saldo, selección de clientes, métricas de tiempo, presentación de la UI y persistencia segura en SQLite.

## 6. Herramientas y creación del proyecto

- **Maven** administra dependencias, la estructura convencional `src/main/java` y `src/test/java`, y el ciclo de compilación/pruebas.
- **Java 25** está definido en `maven.compiler.release`.
- **JUnit Jupiter** organiza y ejecuta pruebas automatizadas.
- **Mockito** crea dobles de los repositorios y permite verificar llamadas y resultados.
- **Git** lleva el historial de cambios.
- La estructura sigue separación por capas, inversión de dependencias e inyección por constructor; los modelos usan estado encapsulado y cambios por nuevas instancias.

No se adoptó todavía un framework de inyección, ORM ni una arquitectura completa de persistencia. El objetivo es mantener pocas dependencias y hacer explícitas las conexiones entre capas.

## 7. JavaFX y presentación

Maven incorpora `javafx-controls` y el plugin de ejecución de OpenJFX. `ReggisApp` extiende `Application`; los estilos están en `src/main/resources/org/drvo/reggisapp/ui/estilos.css`. Se eligieron controles JavaFX básicos para facilitar ajustes visuales: navegación clara, espacios amplios, azul para acciones principales y rojo para eliminar.

La vista aún es una primera iteración. XLSX y reportes mensuales siguen pendientes, y debe hacerse una validación de aceptación en el equipo de destino antes de producción.

## 8. Pendientes técnicos

1. Añadir exportación XLSX y reportes mensuales.
2. Expandir las pruebas de aceptación para escenarios y resolución de pantalla del equipo de destino.
3. Generar y validar el instalador Windows con WiX Toolset.
