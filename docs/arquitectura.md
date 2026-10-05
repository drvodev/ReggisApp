# Arquitectura y reglas de Reggis

## 1. Propósito

Reggis organiza los cobros de clientes con uno o varios pedidos activos. Cada pago queda ligado a un pedido específico. El sistema calcula el saldo y conserva los registros necesarios para conocer cuándo se creó el pedido, cuándo se realizaron los pagos y cuándo se completó o anuló.

Esta documentación describe la aplicación actual: la interfaz JavaFX usa los servicios de negocio y los datos se guardan en SQLite local.

## 2. Capas

```text
JavaFX (interfaz de escritorio)
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

- `Cliente`: identificador, nombre, estado y mapa de contacto. En el alta, nombre y RIF son obligatorios; correo, teléfono y dirección son opcionales.
- `Pedido`: cliente, descripción, monto base en BS, importe/moneda originales, tasa, fecha de creación y estado.
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
- `ExcelReportExporter`: contrato de exportación. `XlsReportExporter` genera libros binarios `.xls` con Apache POI HSSF.
- `TasaCambioService`: convertir importes; `TasaCambioManualService` implementa la conversión usando la tasa ingresada en el momento.

Los servicios reciben sus dependencias por el constructor. Esto se llama inyección de dependencias: hace explícito qué necesita cada clase y permite sustituir un repositorio real por un mock en pruebas.

### Persistencia (`repository`, `repository.sqlite`)

`ClienteRepository`, `PedidoRepository`, `PagoRepository` y los demás repositorios son contratos para guardar y consultar información. En `repository/sqlite` están los adaptadores JDBC activos. `DatabaseConfig` crea el archivo local y el esquema versionado; `SqliteConnectionFactory` abre conexiones y coordina transacciones. `AppServices` configura y compone repositorios y servicios. Las consultas usan `PreparedStatement` para separar el SQL de los datos ingresados.

Esta frontera permite que los servicios no conozcan sentencias SQL. Más adelante se puede agregar otro adaptador JDBC para PostgreSQL sin reescribir las reglas de cobranza, implementando los mismos contratos.

### Presentación (`ui`)

`ReggisApp` arranca JavaFX con el nombre visible **Reggis** y asigna el icono de una “R” blanca sobre el azul de la aplicación. `VentanaPrincipal` muestra el inicio, la lista de clientes y la ficha de pedidos. La ficha se abre con doble clic en una fila de cliente o seleccionándolo y usando el botón **Ver pedidos**. Desde allí se crean pedidos, se registran pagos, se revisan los tiempos y el historial, y se exporta el reporte del cliente o del pedido seleccionado. **Eliminar cliente** aparece solo en la ficha del cliente; las anulaciones de pedidos e inactivaciones de clientes requieren confirmación doble. La base local se conserva al cerrar la aplicación.

Descripción de las pantallas:

- **Inicio:** resume la cantidad de clientes activos y enlaza al registro o a la lista de clientes.
- **Clientes:** muestra nombre, RIF y estado; permite abrir o modificar los datos del cliente.
- **Pedidos del cliente:** separa los pedidos de la persona/empresa y muestra el estado de cada uno. Al seleccionar uno aparecen pagos e historial; los botones permiten crear pedidos, registrar pagos, anular un pedido, exportar reportes o iniciar la inactivación del cliente.
- **Alta/edición de cliente:** solicita nombre y RIF. Correo, teléfono y dirección se pueden dejar vacíos.
- **Reporte `.xls`:** el reporte del cliente incluye todos sus pedidos; el reporte de pedido contiene solo el pedido seleccionado. Ambos agregan sus pagos y los datos de seguimiento del tiempo.

### Reportes (`report`)

`ExcelReportExporter` define el contrato y `XlsReportExporter` genera archivos `.xls` con los datos de contacto del cliente, sus pedidos, pagos, saldos y métricas de tiempo. Se puede generar el reporte completo del cliente o el de un solo pedido. Los montos y tasas se escriben como celdas numéricas; el texto ingresado se exporta como texto. Los libros `.xls` tienen el límite de 65.536 filas por hoja.

## 3. Reglas de negocio codificadas

### Clientes

- El nombre es obligatorio.
- El RIF también es obligatorio y se normaliza al guardar.
- Correo, teléfono y dirección son opcionales.
- Un cliente puede estar activo o inactivo y conserva sus pedidos e historial.
- Los datos de contacto se guardan en el mapa de campos adicionales del cliente.
- Inactivar evita crear nuevos pedidos; la eliminación física no forma parte del modelo actual.

### Pedidos y pagos

- El monto ingresado debe ser mayor que cero.
- Cada pedido requiere una descripción y no se permite editar el monto después de crearlo.
- Cada pedido pertenece a un cliente y su importe base se normaliza a BS.
- Se conserva el importe original, su moneda y la tasa usada para mantener el contexto del registro.
- El saldo es monto del pedido, más entradas de deuda trasladada, menos pagos y salidas de deuda trasladada.
- No se acepta un pago que supere el saldo pendiente.
- Cuando el saldo queda en cero, el pedido cambia a `COMPLETADO` y deja de ser activo.
- La fecha del pedido y la fecha de cada pago se determinan mediante `Clock`, lo que también permite fijar el tiempo en pruebas.
- El pago se registra con la fecha del sistema. No hay vencimientos: el tiempo del primer pago se mide desde la fecha de creación del pedido.

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

- `ReglaNegocioException` comunica rechazos esperados, como nombre/RIF ausente, monto inválido, cliente inexistente o pago excesivo.
- `PersistenciaException` está reservada para errores de almacenamiento.
- Los servicios validan la entrada antes de persistir.
- Las implementaciones JDBC capturan `SQLException`, la convierten a `PersistenciaException` y usan transacciones para que pago, cambio de estado e historial no queden a medias.
- La UI JavaFX presenta esos errores en alertas; las reglas permanecen en los servicios y no se duplican en la vista.

## 5. Pruebas

Las pruebas están en `src/test/java`. JUnit Jupiter ejecuta los casos y Mockito simula los repositorios en las pruebas unitarias. Las pruebas JavaFX verifican estilos, controles, navegación y carga de nombres en la tabla. Las pruebas SQLite usan una base temporal para validar persistencia, rollback, entradas de inyección SQL como texto inerte e inmutabilidad del historial. Las pruebas del exportador generan y vuelven a abrir archivos `.xls` para verificar cliente, pedidos y pagos.

Los casos cubiertos incluyen validación de importes, conversión manual, exceso de pago, cierre de pedido, anulación y traslado de saldo, selección de clientes, métricas de tiempo, presentación de la UI y persistencia segura en SQLite.

## 6. Herramientas y creación del proyecto

- **Maven** administra dependencias, la estructura convencional `src/main/java` y `src/test/java`, y el ciclo de compilación/pruebas.
- **Java 25** está definido en `maven.compiler.release`.
- **JUnit Jupiter** organiza y ejecuta pruebas automatizadas.
- **Mockito** crea dobles de los repositorios y permite verificar llamadas y resultados.
- **Apache POI HSSF** crea los archivos `.xls` de Excel 97–2003.
- **Git** lleva el historial de cambios.
- La estructura sigue separación por capas, inversión de dependencias e inyección por constructor; los modelos usan estado encapsulado y cambios por nuevas instancias.

No se adoptó todavía un framework de inyección, ORM ni una arquitectura completa de persistencia. El objetivo es mantener pocas dependencias y hacer explícitas las conexiones entre capas.

## 7. JavaFX y presentación

Maven incorpora `javafx-controls` y el plugin de ejecución de OpenJFX. `ReggisApp` extiende `Application`; los estilos están en `src/main/resources/org/drvo/reggisapp/ui/estilos.css`. Se eligieron controles JavaFX básicos para facilitar ajustes visuales: navegación clara, espacios amplios, azul para acciones principales y rojo para eliminar. Los formularios indican los campos requeridos con `*`; las pantallas incluyen textos breves para describir la acción y los reportes disponibles.

La versión 1.0 ya integra las pantallas con servicios y SQLite. Siguen pendientes los reportes mensuales, una comprobación del instalador en otro equipo Windows y la validación de aceptación del usuario final.

## 8. Pendientes técnicos

1. Añadir reportes mensuales de clientes.
2. Expandir las pruebas de aceptación para escenarios y resolución de pantalla del equipo de destino.
3. Generar y validar el instalador Windows con WiX Toolset.
