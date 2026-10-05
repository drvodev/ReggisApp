package org.drvo.reggisapp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.drvo.reggisapp.config.AppServices;
import org.drvo.reggisapp.ui.VentanaPrincipal;

public class ReggisApp extends Application {
    @Override
    public void start(Stage stage) {
        AppServices servicios;
        try {
            servicios = new AppServices();
        } catch (RuntimeException excepcion) {
            Alert error = new Alert(Alert.AlertType.ERROR,
                    "No se pudo abrir la base de datos local.\n" + excepcion.getMessage());
            error.setTitle("Error de inicio");
            error.setHeaderText("ReggisApp no pudo iniciar");
            error.showAndWait();
            return;
        }
        VentanaPrincipal ventana = new VentanaPrincipal(servicios.clienteService(), servicios.cobranzaService(),
                servicios.tiempoService(), servicios.historialRepository());
        Scene scene = new Scene(ventana.getRoot(), 1024, 700);
        var stylesheet = getClass().getResource("/org/drvo/reggisapp/ui/estilos.css");
        if (stylesheet != null) scene.getStylesheets().add(stylesheet.toExternalForm());

        stage.setTitle("ReggisApp");
        stage.setMinWidth(800);
        stage.setMinHeight(580);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
