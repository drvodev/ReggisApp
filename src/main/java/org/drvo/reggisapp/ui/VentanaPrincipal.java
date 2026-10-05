package org.drvo.reggisapp.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.scene.control.ScrollPane;
import javafx.beans.property.SimpleStringProperty;
import org.drvo.reggisapp.domain.model.Cliente;
import org.drvo.reggisapp.domain.model.EstadoPedido;
import org.drvo.reggisapp.domain.model.EstadoCliente;
import org.drvo.reggisapp.domain.model.Moneda;
import org.drvo.reggisapp.domain.model.Pago;
import org.drvo.reggisapp.domain.model.Pedido;
import org.drvo.reggisapp.domain.model.RegistroHistorial;
import org.drvo.reggisapp.domain.model.ResolucionAnulacion;
import org.drvo.reggisapp.domain.service.CobranzaService;
import org.drvo.reggisapp.domain.service.ClienteService;
import org.drvo.reggisapp.domain.service.TiempoService;
import org.drvo.reggisapp.repository.HistorialRepository;
import org.drvo.reggisapp.report.ExcelReportExporter;
import org.drvo.reggisapp.report.ReportePedido;
import org.drvo.reggisapp.report.XlsReportExporter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Vistas JavaFX de clientes, pedidos, pagos y seguimiento. */
public class VentanaPrincipal {
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final BorderPane root = new BorderPane();
    private final StackPane contenido = new StackPane();
    private final ClienteService clienteService;
    private final CobranzaService cobranzaService;
    private final TiempoService tiempoService;
    private final HistorialRepository historialRepository;
    private final ExcelReportExporter excelReportExporter;

    public VentanaPrincipal(ClienteService clienteService, CobranzaService cobranzaService,
                            TiempoService tiempoService, HistorialRepository historialRepository) {
        this(clienteService, cobranzaService, tiempoService, historialRepository, new XlsReportExporter());
    }

    public VentanaPrincipal(ClienteService clienteService, CobranzaService cobranzaService,
                            TiempoService tiempoService, HistorialRepository historialRepository,
                            ExcelReportExporter excelReportExporter) {
        this.clienteService = clienteService;
        this.cobranzaService = cobranzaService;
        this.tiempoService = tiempoService;
        this.historialRepository = historialRepository;
        this.excelReportExporter = excelReportExporter;
        root.getStyleClass().add("app-root");
        root.setTop(crearBarraSuperior());
        root.setCenter(contenido);
        root.setBottom(crearPie());
        mostrarInicio();
    }

    public Parent getRoot() { return root; }

    private HBox crearBarraSuperior() {
        Label marca = new Label("R");
        marca.getStyleClass().add("brand-mark");
        Label nombre = new Label("Reggis");
        nombre.getStyleClass().add("brand-name");
        HBox marcaCompleta = new HBox(11, marca, nombre);
        marcaCompleta.setAlignment(Pos.CENTER_LEFT);
        Region espacio = crearEspaciador();
        Label estado = new Label("DATOS LOCALES · SQLITE");
        estado.getStyleClass().add("preview-badge");
        HBox barra = new HBox(marcaCompleta, espacio, estado);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(18, 42, 18, 42));
        barra.getStyleClass().add("top-bar");
        return barra;
    }

    private Label crearPie() {
        Label pie = new Label("Los cobros se registran en BS. La tasa usada queda guardada con cada pedido y pago.");
        pie.getStyleClass().add("footer-label");
        BorderPane.setAlignment(pie, Pos.CENTER_LEFT);
        BorderPane.setMargin(pie, new Insets(12, 42, 15, 42));
        return pie;
    }

    private void mostrarInicio() {
        List<Cliente> activos = clienteService.listarActivos();
        Label saludo = new Label("Hola, bienvenido a Reggis");
        saludo.getStyleClass().add("page-title");
        Label descripcion = new Label("Administra clientes, pedidos y pagos desde un solo lugar.");
        descripcion.getStyleClass().add("page-subtitle");
        Label cantidad = new Label(Integer.toString(activos.size()));
        cantidad.getStyleClass().add("stat-number");
        Label rotulo = new Label("clientes activos");
        rotulo.getStyleClass().add("stat-caption");
        VBox estadistica = new VBox(4, cantidad, rotulo);
        estadistica.getStyleClass().add("stat-card");
        estadistica.setPadding(new Insets(15, 20, 15, 20));
        estadistica.setMaxWidth(170);
        VBox encabezado = new VBox(10, saludo, descripcion, estadistica);
        encabezado.setPadding(new Insets(0, 0, 26, 0));

        Button registrar = boton("Registrar cliente", "button-primary", this::registrarCliente);
        Button ver = boton("Ver clientes", "button-secondary", this::mostrarClientes);
        registrar.setMaxWidth(Double.MAX_VALUE);
        ver.setMaxWidth(Double.MAX_VALUE);

        VBox acciones = new VBox(14,
                crearTarjetaAccion("01", "Registrar cliente", "Añade una persona o negocio a tu lista.", registrar),
                crearTarjetaAccion("02", "Ver clientes", "Busca un cliente para ver y administrar sus pedidos.", ver));
        acciones.setMaxWidth(640);

        VBox vista = new VBox(0, encabezado, acciones);
        vista.setPadding(new Insets(46, 36, 42, 72));
        vista.getStyleClass().add("page");
        mostrarVista(vista);
    }

    private VBox crearTarjetaAccion(String numero, String titulo, String detalle, Button accion) {
        Label numeroLabel = new Label(numero);
        numeroLabel.getStyleClass().add("action-number");
        Label tituloLabel = new Label(titulo);
        tituloLabel.getStyleClass().add("action-title");
        Label detalleLabel = new Label(detalle);
        detalleLabel.getStyleClass().add("action-description");
        VBox textos = new VBox(5, tituloLabel, detalleLabel);
        HBox fila = new HBox(14, numeroLabel, textos);
        fila.setAlignment(Pos.CENTER_LEFT);
        VBox tarjeta = new VBox(15, fila, accion);
        tarjeta.getStyleClass().add("action-card");
        tarjeta.setPadding(new Insets(17));
        return tarjeta;
    }

    private void mostrarClientes() {
        Label titulo = new Label("Clientes");
        titulo.getStyleClass().add("page-title");
        Label subtitulo = new Label("Haz doble clic en un cliente para abrir sus pedidos, o selecciónalo y usa Ver pedidos.");
        subtitulo.getStyleClass().add("page-subtitle");

        ObservableList<Cliente> filas = FXCollections.observableArrayList(clienteService.listarTodos());
        TableView<Cliente> tabla = new TableView<>(filas);
        tabla.setPlaceholder(new Label("Aún no hay clientes. Registra el primero para comenzar."));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.getStyleClass().add("data-table");
        tabla.getColumns().addAll(List.of(
                columnaCliente("Cliente", Cliente::getNombre, 0.5),
                columnaCliente("RIF", cliente -> cliente.getDatosAdicionales().getOrDefault("rif", ""), 0.28),
                columnaCliente("Estado", cliente -> cliente.getEstado() == EstadoCliente.ACTIVO ? "Activo" : "Inactivo", 0.22)));
        tabla.setRowFactory(vistaTabla -> {
            TableRow<Cliente> fila = new TableRow<>();
            fila.setOnMouseClicked(evento -> abrirClientePorDobleClic(fila.getItem(), evento.getClickCount()));
            return fila;
        });
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button abrir = boton("Ver pedidos", "button-primary", () -> {
            Cliente seleccionado = tabla.getSelectionModel().getSelectedItem();
            if (seleccionado == null) mostrarAviso("Selecciona un cliente para continuar.");
            else mostrarPedidos(seleccionado);
        });
        Button modificar = boton("Modificar", "button-secondary", () -> {
            Cliente seleccionado = tabla.getSelectionModel().getSelectedItem();
            if (seleccionado == null) mostrarAviso("Selecciona un cliente para modificar.");
            else modificarCliente(seleccionado);
        });
        Button volver = boton("Volver al inicio", "button-link", this::mostrarInicio);
        HBox acciones = new HBox(10, abrir, modificar, crearEspaciador(), volver);
        acciones.setAlignment(Pos.CENTER_LEFT);
        VBox vista = new VBox(18, titulo, subtitulo, tabla, acciones);
        vista.setPadding(new Insets(50, 64, 42, 64));
        vista.getStyleClass().add("page");
        mostrarVista(vista);
    }

    private void mostrarPedidos(Cliente cliente) {
        Label titulo = new Label(cliente.getNombre());
        titulo.getStyleClass().add("page-title");
        Label subtitulo = new Label("Pedidos, pagos y tiempo de cancelación");
        subtitulo.getStyleClass().add("page-subtitle");

        ObservableList<Pedido> filas = FXCollections.observableArrayList(cobranzaService.listarPedidos(cliente.getId(), null));
        TableView<Pedido> tablaPedidos = new TableView<>(filas);
        tablaPedidos.setPlaceholder(new Label("Este cliente aún no tiene pedidos."));
        tablaPedidos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaPedidos.getStyleClass().add("data-table");
        tablaPedidos.getColumns().addAll(List.of(
                columnaPedido("Pedido", pedido -> "#" + pedido.getId() + " · " + pedido.getDescripcion(), 0.46),
                columnaPedido("Monto original", pedido -> moneda(pedido.getMontoOriginal(), pedido.getMonedaOriginal()), 0.2),
                columnaPedido("Total en BS", pedido -> moneda(pedido.getMontoPedidoBs(), Moneda.BS), 0.18),
                columnaPedido("Estado", pedido -> etiquetaEstado(pedido.getEstado()), 0.16)));
        tablaPedidos.setPrefHeight(230);

        TableView<Pago> tablaPagos = new TableView<>();
        tablaPagos.setPlaceholder(new Label("Selecciona un pedido para ver los pagos."));
        tablaPagos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaPagos.getStyleClass().add("data-table");
        tablaPagos.getColumns().addAll(List.of(
                columnaPago("Fecha", pago -> FORMATO_FECHA.format(pago.getFechaPago()), 0.27),
                columnaPago("Ingresado", pago -> moneda(pago.getMontoOriginal(), pago.getMonedaOriginal()), 0.3),
                columnaPago("Equivalente BS", pago -> moneda(pago.getMontoBs(), Moneda.BS), 0.27),
                columnaPago("Tasa", pago -> pago.getTasaCambio().toPlainString(), 0.16)));
        tablaPagos.setPrefHeight(155);

        Label resumenTiempo = new Label("Selecciona un pedido para ver el resumen de tiempo.");
        resumenTiempo.getStyleClass().add("summary-card");
        ListView<String> historial = new ListView<>();
        historial.setPlaceholder(new Label("Selecciona un pedido para consultar su historial."));
        historial.setPrefHeight(110);

        tablaPedidos.getSelectionModel().selectedItemProperty().addListener((observable, anterior, pedido) -> {
            if (pedido != null) actualizarDetallePedido(pedido, tablaPagos, resumenTiempo, historial);
        });

        Button nuevo = boton("Nuevo pedido", "button-primary", () -> crearPedido(cliente));
        nuevo.setDisable(cliente.getEstado() != EstadoCliente.ACTIVO);
        Button pago = boton("Registrar pago", "button-secondary", () -> {
            Pedido seleccionado = tablaPedidos.getSelectionModel().getSelectedItem();
            if (seleccionado == null) mostrarAviso("Selecciona un pedido para registrar el pago.");
            else if (seleccionado.getEstado() != EstadoPedido.ACTIVO) mostrarAviso("El pedido ya no está activo.");
            else registrarPago(cliente, seleccionado);
        });
        Button anular = boton("Anular pedido", "button-danger", () -> {
            Pedido seleccionado = tablaPedidos.getSelectionModel().getSelectedItem();
            if (seleccionado == null) mostrarAviso("Selecciona un pedido para anular.");
            else anularPedido(cliente, seleccionado);
        });
        Button exportarCliente = boton("Exportar cliente (.xls)", "button-secondary", () -> exportarCliente(cliente));
        Button exportarPedido = boton("Exportar pedido (.xls)", "button-secondary", () -> {
            Pedido seleccionado = tablaPedidos.getSelectionModel().getSelectedItem();
            if (seleccionado == null) mostrarAviso("Selecciona un pedido para exportarlo.");
            else exportarPedido(cliente, seleccionado);
        });
        exportarPedido.setDisable(true);
        tablaPedidos.getSelectionModel().selectedItemProperty().addListener((observable, anterior, seleccionado) ->
                exportarPedido.setDisable(seleccionado == null));
        Button eliminarCliente = boton("Eliminar cliente", "button-danger", () -> inactivarCliente(cliente));
        eliminarCliente.setDisable(cliente.getEstado() != EstadoCliente.ACTIVO);
        Button volver = boton("Volver a clientes", "button-link", this::mostrarClientes);
        HBox acciones = new HBox(10, nuevo, pago, anular, crearEspaciador(), volver);
        acciones.setAlignment(Pos.CENTER_LEFT);
        HBox exportaciones = new HBox(10, exportarCliente, exportarPedido);
        exportaciones.setAlignment(Pos.CENTER_LEFT);
        Label descripcionEliminar = new Label("Retira este cliente de la lista activa; sus pedidos e historial se conservan.");
        descripcionEliminar.getStyleClass().add("page-subtitle");
        VBox accionesCliente = new VBox(8, tituloSeccion("Acción del cliente"),
                new HBox(12, eliminarCliente, descripcionEliminar));

        Label pagosTitulo = tituloSeccion("Pagos registrados");
        Label historialTitulo = tituloSeccion("Historial del pedido");
        VBox vista = new VBox(14, titulo, subtitulo, tablaPedidos, acciones, exportaciones, accionesCliente,
                pagosTitulo, tablaPagos, resumenTiempo, historialTitulo, historial);
        vista.setPadding(new Insets(42, 64, 42, 64));
        vista.getStyleClass().add("page");
        mostrarVista(vista);
    }

    void abrirClientePorDobleClic(Cliente cliente, int cantidadClics) {
        if (cantidadClics == 2 && cliente != null) mostrarPedidos(cliente);
    }

    private void actualizarDetallePedido(Pedido pedido, TableView<Pago> tablaPagos,
                                         Label resumenTiempo, ListView<String> historial) {
        List<Pago> pagos = cobranzaService.listarPagos(pedido.getId());
        tablaPagos.setItems(FXCollections.observableArrayList(pagos));
        var resumen = tiempoService.calcularTiempo(pedido, pagos);
        String cancelacion = resumen.getDiasHastaCancelacion() == null
                ? "Todavía pendiente" : resumen.getDiasHastaCancelacion() + " días";
        resumenTiempo.setText("Promedio entre pagos: " + resumen.getPromedioDiasEntrePagos()
                + " días  ·  Primer pago: " + resumen.getDiasHastaPrimerPago()
                + " días  ·  Cancelación: " + cancelacion);

        ObservableList<String> eventos = FXCollections.observableArrayList();
        for (RegistroHistorial evento : historialRepository.listarPorEntidad("PEDIDO", pedido.getId())) {
            eventos.add(FORMATO_FECHA.format(evento.getFechaRegistro().toLocalDate()) + "  ·  " +
                    evento.getTipoEvento() + (evento.getMotivo() == null ? "" : "  ·  " + evento.getMotivo()));
        }
        for (Pago pago : pagos) {
            for (RegistroHistorial evento : historialRepository.listarPorEntidad("PAGO", pago.getId())) {
                eventos.add(FORMATO_FECHA.format(evento.getFechaRegistro().toLocalDate()) + "  ·  " + evento.getTipoEvento());
            }
        }
        historial.setItems(eventos);
    }

    private void registrarCliente() {
        TextField nombre = new TextField();
        nombre.setPromptText("Ej. Panadería HP");
        TextField rif = new TextField();
        rif.setPromptText("Obligatorio");
        TextField email = new TextField();
        email.setPromptText("Opcional");
        TextField telefono = new TextField();
        telefono.setPromptText("Opcional");
        TextField direccion = new TextField();
        direccion.setPromptText("Opcional");
        Dialog<ButtonType> dialogo = dialogoFormulario("Registrar cliente", formulario(
                new String[]{"Nombre *", "RIF *", "Correo", "Teléfono", "Dirección"},
                new javafx.scene.Node[]{nombre, rif, email, telefono, direccion}), "Registrar");
        dialogo.showAndWait().filter(tipo -> tipo.getButtonData() == ButtonBar.ButtonData.OK_DONE).ifPresent(tipo ->
                ejecutarSeguro(() -> {
                    clienteService.crear(nombre.getText(), datosContacto(rif, email, telefono, direccion));
                    mostrarClientes();
                }));
    }

    private void modificarCliente(Cliente cliente) {
        TextField nombre = new TextField(cliente.getNombre());
        TextField rif = campo(cliente, "rif");
        TextField email = campo(cliente, "email");
        TextField telefono = campo(cliente, "telefono");
        TextField direccion = campo(cliente, "direccion");
        Dialog<ButtonType> dialogo = dialogoFormulario("Modificar cliente", formulario(
                new String[]{"Nombre *", "RIF *", "Correo", "Teléfono", "Dirección"},
                new javafx.scene.Node[]{nombre, rif, email, telefono, direccion}), "Guardar");
        dialogo.showAndWait().filter(tipo -> tipo.getButtonData() == ButtonBar.ButtonData.OK_DONE).ifPresent(tipo ->
                ejecutarSeguro(() -> {
                    clienteService.actualizar(cliente.getId(), nombre.getText(), datosContacto(rif, email, telefono, direccion));
                    mostrarClientes();
                }));
    }

    private TextField campo(Cliente cliente, String clave) {
        TextField campo = new TextField(cliente.getDatosAdicionales().getOrDefault(clave, ""));
        if (!clave.equals("rif")) campo.setPromptText("Opcional");
        return campo;
    }

    private Map<String, String> datosContacto(TextField rif, TextField email, TextField telefono, TextField direccion) {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("rif", rif.getText());
        datos.put("email", email.getText());
        datos.put("telefono", telefono.getText());
        datos.put("direccion", direccion.getText());
        return datos;
    }

    private void exportarCliente(Cliente cliente) {
        List<ReportePedido> reportes = cobranzaService.listarPedidos(cliente.getId(), null).stream()
                .map(this::prepararReportePedido).toList();
        Path destino = elegirDestino("Cliente-" + cliente.getId() + "-" + nombreArchivo(cliente.getNombre()) + ".xls");
        if (destino == null) return;
        try {
            excelReportExporter.exportarCliente(cliente, reportes, destino);
            mostrarAviso("Reporte del cliente exportado correctamente.");
        } catch (IOException | RuntimeException excepcion) {
            mostrarError("No se pudo exportar el reporte: " + mensaje(excepcion));
        }
    }

    private void exportarPedido(Cliente cliente, Pedido pedido) {
        Path destino = elegirDestino("Pedido-" + pedido.getId() + "-" + nombreArchivo(cliente.getNombre()) + ".xls");
        if (destino == null) return;
        try {
            excelReportExporter.exportarPedido(cliente, prepararReportePedido(pedido), destino);
            mostrarAviso("Reporte del pedido exportado correctamente.");
        } catch (IOException | RuntimeException excepcion) {
            mostrarError("No se pudo exportar el reporte: " + mensaje(excepcion));
        }
    }

    private ReportePedido prepararReportePedido(Pedido pedido) {
        List<Pago> pagos = cobranzaService.listarPagos(pedido.getId());
        return new ReportePedido(pedido, pagos, cobranzaService.obtenerSaldoPendiente(pedido.getId()),
                tiempoService.calcularTiempo(pedido, pagos));
    }

    private Path elegirDestino(String nombreInicial) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Exportar reporte de cobranza");
        selector.setInitialFileName(nombreInicial);
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 97-2003 (*.xls)", "*.xls"));
        var escena = root.getScene();
        File archivo = selector.showSaveDialog(escena == null ? null : escena.getWindow());
        if (archivo == null) return null;
        String ruta = archivo.getAbsolutePath();
        return Path.of(ruta.toLowerCase().endsWith(".xls") ? ruta : ruta + ".xls");
    }

    private String nombreArchivo(String nombre) {
        return nombre.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String mensaje(Exception excepcion) {
        return excepcion.getMessage() == null ? "Ocurrió un error inesperado." : excepcion.getMessage();
    }

    private void crearPedido(Cliente cliente) {
        TextField descripcion = new TextField();
        descripcion.setPromptText("Descripción del pedido");
        TextField monto = new TextField();
        monto.setPromptText("Ej. 1232000.00");
        TextField tasa = new TextField();
        tasa.setPromptText("Bolívares por dólar");
        ComboBox<Moneda> moneda = new ComboBox<>(FXCollections.observableArrayList(Moneda.values()));
        moneda.getSelectionModel().select(Moneda.BS);
        moneda.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Moneda item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item == Moneda.BS ? "BS" : "USD ($)");
            }
        });
        moneda.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Moneda item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : item == Moneda.BS ? "BS" : "USD ($)");
            }
        });
        GridPane formulario = formulario(
                new String[]{"Descripción", "Monto", "Moneda", "Tasa BS por USD"},
                new javafx.scene.Node[]{descripcion, monto, moneda, tasa});
        Dialog<ButtonType> dialogo = dialogoFormulario("Nuevo pedido", formulario, "Crear pedido");
        dialogo.showAndWait().filter(tipo -> tipo.getButtonData() == ButtonBar.ButtonData.OK_DONE).ifPresent(tipo ->
                ejecutarSeguro(() -> {
                    cobranzaService.crearPedido(cliente.getId(), descripcion.getText(), decimal(monto.getText()),
                            moneda.getValue(), decimal(tasa.getText()));
                    mostrarPedidos(cliente);
                }));
    }

    private void registrarPago(Cliente cliente, Pedido pedido) {
        TextField monto = new TextField();
        monto.setPromptText("Monto parcial o total");
        TextField tasa = new TextField();
        tasa.setPromptText("Bolívares por dólar");
        ComboBox<Moneda> moneda = new ComboBox<>(FXCollections.observableArrayList(Moneda.values()));
        moneda.getSelectionModel().select(Moneda.BS);
        moneda.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Moneda valor) { return valor == Moneda.USD ? "USD ($)" : "BS"; }
            @Override public Moneda fromString(String valor) { return "USD ($)".equals(valor) ? Moneda.USD : Moneda.BS; }
        });
        Dialog<ButtonType> dialogo = dialogoFormulario("Registrar pago · #" + pedido.getId(), formulario(
                new String[]{"Monto recibido", "Moneda", "Tasa BS por USD"},
                new javafx.scene.Node[]{monto, moneda, tasa}), "Registrar pago");
        dialogo.showAndWait().filter(tipo -> tipo.getButtonData() == ButtonBar.ButtonData.OK_DONE).ifPresent(tipo ->
                ejecutarSeguro(() -> {
                    cobranzaService.registrarPago(pedido.getId(), decimal(monto.getText()), moneda.getValue(), decimal(tasa.getText()));
                    mostrarPedidos(cliente);
                }));
    }

    private void anularPedido(Cliente cliente, Pedido pedido) {
        if (pedido.getEstado() != EstadoPedido.ACTIVO) {
            mostrarAviso("Solo se pueden anular pedidos activos.");
            return;
        }
        TextArea motivo = new TextArea();
        motivo.setPromptText("Escribe el motivo de la anulación");
        motivo.setPrefRowCount(3);
        ComboBox<ResolucionAnulacion> resolucion = new ComboBox<>(FXCollections.observableArrayList(ResolucionAnulacion.values()));
        resolucion.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ResolucionAnulacion valor) {
                if (valor == null) return "";
                return valor == ResolucionAnulacion.CONDONAR_SALDO ? "Condonar saldo" : "Trasladar a otro pedido";
            }
            @Override public ResolucionAnulacion fromString(String valor) { return null; }
        });
        resolucion.getSelectionModel().select(ResolucionAnulacion.CONDONAR_SALDO);
        List<Pedido> destinosDisponibles = cobranzaService.listarPedidos(cliente.getId(), EstadoPedido.ACTIVO).stream()
                .filter(destino -> !destino.getId().equals(pedido.getId())).toList();
        ComboBox<Pedido> destino = new ComboBox<>(FXCollections.observableArrayList(destinosDisponibles));
        destino.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Pedido valor) { return valor == null ? "" : "#" + valor.getId() + " · " + valor.getDescripcion(); }
            @Override public Pedido fromString(String valor) { return null; }
        });
        destino.setDisable(true);
        resolucion.valueProperty().addListener((observable, anterior, valor) ->
                destino.setDisable(valor != ResolucionAnulacion.TRASLADAR_SALDO));
        GridPane formulario = formulario(new String[]{"Motivo", "Resolución", "Pedido destino"},
                new javafx.scene.Node[]{motivo, resolucion, destino});
        Dialog<ButtonType> dialogo = dialogoFormulario("Anular pedido · #" + pedido.getId(), formulario, "Continuar");
        dialogo.showAndWait().filter(tipo -> tipo.getButtonData() == ButtonBar.ButtonData.OK_DONE).ifPresent(tipo -> {
            if (!confirmar("Confirmar anulación", "Se guardará el motivo y la resolución en el historial. ¿Continuar?")) return;
            if (!confirmar("Confirmación final", "La anulación y su registro de auditoría no se pueden editar. ¿Confirmas?")) return;
            ejecutarSeguro(() -> {
                cobranzaService.anularPedido(pedido.getId(), motivo.getText(), resolucion.getValue(),
                        destino.getValue() == null ? null : destino.getValue().getId());
                mostrarPedidos(cliente);
            });
        });
    }

    private void inactivarCliente(Cliente cliente) {
        if (!confirmar("Inactivar cliente", "¿Deseas retirar a " + cliente.getNombre() + " de la lista activa?")) return;
        if (!confirmar("Confirmación final", "El historial y los pedidos se conservarán. ¿Confirmas?")) return;
        ejecutarSeguro(() -> {
            clienteService.inactivar(cliente.getId());
            mostrarClientes();
        });
    }

    private TableColumn<Cliente, String> columnaCliente(String titulo, java.util.function.Function<Cliente, String> valor, double ancho) {
        TableColumn<Cliente, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(celda -> new SimpleStringProperty(valor.apply(celda.getValue())));
        columna.setPrefWidth(ancho * 800);
        return columna;
    }

    private TableColumn<Pedido, String> columnaPedido(String titulo, java.util.function.Function<Pedido, String> valor, double ancho) {
        TableColumn<Pedido, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(celda -> new SimpleStringProperty(valor.apply(celda.getValue())));
        columna.setPrefWidth(ancho * 800);
        return columna;
    }

    private TableColumn<Pago, String> columnaPago(String titulo, java.util.function.Function<Pago, String> valor, double ancho) {
        TableColumn<Pago, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(celda -> new SimpleStringProperty(valor.apply(celda.getValue())));
        columna.setPrefWidth(ancho * 800);
        return columna;
    }

    private GridPane formulario(String[] etiquetas, javafx.scene.Node[] controles) {
        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(12);
        formulario.setPadding(new Insets(8, 4, 4, 4));
        ColumnConstraints columnaEtiqueta = new ColumnConstraints();
        columnaEtiqueta.setMinWidth(140);
        formulario.getColumnConstraints().addAll(columnaEtiqueta, new ColumnConstraints());
        for (int i = 0; i < etiquetas.length; i++) {
            formulario.add(new Label(etiquetas[i]), 0, i);
            formulario.add(controles[i], 1, i);
            GridPane.setHgrow(controles[i], Priority.ALWAYS);
        }
        return formulario;
    }

    private Dialog<String> dialogoSimple(String titulo, String encabezado, TextField campo, String textoAceptar) {
        Dialog<String> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(encabezado);
        dialogo.getDialogPane().setContent(campo);
        dialogo.getDialogPane().setPadding(new Insets(20));
        dialogo.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE),
                new ButtonType(textoAceptar, ButtonBar.ButtonData.OK_DONE));
        return dialogo;
    }

    private Dialog<ButtonType> dialogoFormulario(String titulo, GridPane formulario, String textoAceptar) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(null);
        dialogo.getDialogPane().setContent(formulario);
        dialogo.getDialogPane().setPadding(new Insets(20));
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType aceptar = new ButtonType(textoAceptar, ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(cancelar, aceptar);
        dialogo.setResultConverter(boton -> boton);
        return dialogo;
    }

    private boolean confirmar(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensaje,
                new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE),
                new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE));
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        return alerta.showAndWait().filter(boton -> boton.getButtonData() == ButtonBar.ButtonData.OK_DONE).isPresent();
    }

    private Label tituloSeccion(String texto) {
        Label titulo = new Label(texto);
        titulo.getStyleClass().add("section-label");
        return titulo;
    }

    private String moneda(BigDecimal valor, Moneda moneda) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString() + (moneda == Moneda.BS ? " BS" : " $");
    }

    private String etiquetaEstado(EstadoPedido estado) {
        return switch (estado) {
            case ACTIVO -> "Pendiente";
            case COMPLETADO -> "Pagado";
            case ANULADO -> "Anulado";
        };
    }

    private BigDecimal decimal(String texto) {
        if (texto == null || texto.isBlank()) throw new IllegalArgumentException("Completa todos los montos requeridos.");
        return new BigDecimal(texto.trim().replace(',', '.'));
    }

    private void ejecutarSeguro(Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException excepcion) {
            mostrarError(excepcion.getMessage() == null ? "Ocurrió un error inesperado." : excepcion.getMessage());
        }
    }

    private void mostrarAviso(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alerta.setTitle("Reggis");
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        alerta.setTitle("No se pudo completar la operación");
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }

    private Button boton(String texto, String estilo, Runnable accion) {
        Button boton = new Button(texto);
        boton.getStyleClass().add(estilo);
        boton.setOnAction(evento -> accion.run());
        return boton;
    }

    private Region crearEspaciador() {
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        return espacio;
    }

    private void mostrarVista(VBox vista) {
        ScrollPane desplazamiento = new ScrollPane(vista);
        desplazamiento.setFitToWidth(true);
        desplazamiento.getStyleClass().add("page-scroll");
        contenido.getChildren().setAll(desplazamiento);
    }
}
