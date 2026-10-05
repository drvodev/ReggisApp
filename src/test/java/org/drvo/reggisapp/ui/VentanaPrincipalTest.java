package org.drvo.reggisapp.ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.image.WritableImage;
import org.drvo.reggisapp.domain.model.Cliente;
import org.drvo.reggisapp.domain.model.EstadoCliente;
import org.drvo.reggisapp.domain.service.CobranzaService;
import org.drvo.reggisapp.domain.service.ClienteService;
import org.drvo.reggisapp.domain.service.TiempoService;
import org.drvo.reggisapp.repository.HistorialRepository;
import org.drvo.reggisapp.report.ExcelReportExporter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VentanaPrincipalTest {
    @BeforeAll
    static void iniciarJavaFx() throws Exception {
        FutureTask<Void> inicio = new FutureTask<>(() -> {
            Platform.startup(() -> { });
            return null;
        });
        new Thread(inicio, "javafx-test-startup").start();
        inicio.get();
    }

    @AfterAll
    static void cerrarJavaFx() throws Exception {
        enJavaFx(() -> {
            Platform.exit();
            return null;
        });
    }

    @Test
    void inicioMuestraAccionesPrincipalesYNoIncluyeEliminacion() throws Exception {
        enJavaFx(() -> {
            ClienteService clientes = mock(ClienteService.class);
            when(clientes.listarActivos()).thenReturn(List.of());
            VentanaPrincipal ventana = crearVentana(clientes);
            Scene scene = new Scene(ventana.getRoot(), 1024, 700);
            scene.getStylesheets().add(getClass().getResource("/org/drvo/reggisapp/ui/estilos.css").toExternalForm());
            scene.getRoot().applyCss();
            scene.getRoot().resize(1024, 700);
            scene.getRoot().layout();

            Button registrar = buscarBoton(scene.getRoot(), "Registrar cliente");
            assertNotNull(registrar);
            assertNull(buscarBoton(scene.getRoot(), "Eliminar cliente"));
            assertNull(buscarLabel(scene.getRoot(), "Acción de seguridad"));
            assertNull(buscarBoton(scene.getRoot(), "Ver estado de clientes"));
            assertNotNull(buscarLabel(scene.getRoot(), "Reggis"));
            assertNotNull(buscarBoton(scene.getRoot(), "Ver clientes"));
            assertTrue(registrar.getStyleClass().contains("button-primary"));
            WritableImage captura = scene.getRoot().snapshot(null, null);
            assertTrue(captura.getWidth() >= 800);
            assertTrue(captura.getHeight() >= 500);
            return null;
        });
    }

    @Test
    void listaMuestraClientesYLasCeldasLeenSusNombres() throws Exception {
        enJavaFx(() -> {
            ClienteServicioPrueba servicios = crearServiciosConClientes();
            VentanaPrincipal ventana = crearVentana(servicios.clientes());
            Scene scene = new Scene(ventana.getRoot(), 1024, 700);
            scene.getRoot().applyCss();
            scene.getRoot().resize(1024, 700);
            scene.getRoot().layout();
            buscarBoton(scene.getRoot(), "Ver clientes").fire();
            scene.getRoot().applyCss();
            scene.getRoot().layout();

            @SuppressWarnings("unchecked")
            TableView<Cliente> tabla = (TableView<Cliente>) (TableView<?>) buscarTabla(scene.getRoot());
            assertNotNull(tabla);
            assertEquals(2, tabla.getItems().size());
            assertEquals("Panadería HP", tabla.getColumns().get(0).getCellObservableValue(0).getValue());
            assertEquals("Mercado Central", tabla.getColumns().get(0).getCellObservableValue(1).getValue());
            assertEquals("RIF", tabla.getColumns().get(1).getText());
            assertNull(buscarBoton(scene.getRoot(), "Eliminar cliente"));
            return null;
        });
    }

    @Test
    void dobleClickEnClienteAbreLaVistaDePedidosDelCliente() throws Exception {
        enJavaFx(() -> {
            CobranzaService cobranza = mock(CobranzaService.class);
            Cliente cliente = new Cliente(9L, "Ana", EstadoCliente.ACTIVO, java.util.Map.of("rif", "J-9"));
            VentanaPrincipal ventana = new VentanaPrincipal(mock(ClienteService.class), cobranza,
                    mock(TiempoService.class), mock(HistorialRepository.class), mock(ExcelReportExporter.class));

            ventana.abrirClientePorDobleClic(cliente, 1);
            org.mockito.Mockito.verifyNoInteractions(cobranza);
            ventana.abrirClientePorDobleClic(cliente, 2);

            org.mockito.Mockito.verify(cobranza).listarPedidos(9L, null);
            Scene scene = new Scene(ventana.getRoot(), 1024, 700);
            scene.getRoot().applyCss();
            assertNotNull(buscarBoton(scene.getRoot(), "Exportar cliente (.xls)"));
            assertNotNull(buscarBoton(scene.getRoot(), "Exportar pedido (.xls)"));
            Button eliminarCliente = buscarBoton(scene.getRoot(), "Eliminar cliente");
            assertNotNull(eliminarCliente);
            assertTrue(eliminarCliente.getStyleClass().contains("button-danger"));
            return null;
        });
    }

    private static VentanaPrincipal crearVentana(ClienteService clienteService) {
        return new VentanaPrincipal(clienteService, mock(CobranzaService.class), mock(TiempoService.class),
                mock(HistorialRepository.class));
    }

    private static ClienteServicioPrueba crearServiciosConClientes() {
        ClienteService servicio = mock(ClienteService.class);
        List<Cliente> clientes = List.of(
                new Cliente(1L, "Panadería HP", EstadoCliente.ACTIVO, java.util.Map.of()),
                new Cliente(2L, "Mercado Central", EstadoCliente.ACTIVO, java.util.Map.of()));
        when(servicio.listarActivos()).thenReturn(clientes);
        when(servicio.listarTodos()).thenReturn(clientes);
        return new ClienteServicioPrueba(servicio);
    }

    private static Button buscarBoton(Node raiz, String texto) {
        if (raiz instanceof Button boton && texto.equals(boton.getText())) return boton;
        if (raiz instanceof Parent parent) {
            for (Node hijo : parent.getChildrenUnmodifiable()) {
                Button resultado = buscarBoton(hijo, texto);
                if (resultado != null) return resultado;
            }
        }
        return null;
    }

    private static TableView<?> buscarTabla(Node raiz) {
        if (raiz instanceof TableView<?> tabla) return tabla;
        if (raiz instanceof Parent parent) {
            for (Node hijo : parent.getChildrenUnmodifiable()) {
                TableView<?> resultado = buscarTabla(hijo);
                if (resultado != null) return resultado;
            }
        }
        return null;
    }

    private static Label buscarLabel(Node raiz, String texto) {
        if (raiz instanceof Label label && texto.equals(label.getText())) return label;
        if (raiz instanceof Parent parent) {
            for (Node hijo : parent.getChildrenUnmodifiable()) {
                Label resultado = buscarLabel(hijo, texto);
                if (resultado != null) return resultado;
            }
        }
        return null;
    }

    private static <T> T enJavaFx(Callable<T> tarea) throws Exception {
        if (Platform.isFxApplicationThread()) return tarea.call();
        FutureTask<T> futura = new FutureTask<>(tarea);
        Platform.runLater(futura);
        return futura.get();
    }

    private record ClienteServicioPrueba(ClienteService clientes) { }
}
