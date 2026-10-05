package org.drvo.reggisapp.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Cliente {
    private final Long id;
    private final String nombre;
    private final EstadoCliente estado;
    private final Map<String, String> datosAdicionales;

    public Cliente(Long id, String nombre) {
        this(id, nombre, EstadoCliente.ACTIVO, Map.of());
    }

    public Cliente(Long id, String nombre, EstadoCliente estado, Map<String, String> datosAdicionales) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio.");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.estado = Objects.requireNonNull(estado, "El estado del cliente es obligatorio.");
        this.datosAdicionales = Collections.unmodifiableMap(new LinkedHashMap<>(
                datosAdicionales == null ? Map.of() : datosAdicionales));
    }

    public static Cliente nuevo(String nombre) { return new Cliente(null, nombre); }
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public EstadoCliente getEstado() { return estado; }
    public Map<String, String> getDatosAdicionales() { return datosAdicionales; }
    public Cliente renombrar(String nuevoNombre) { return new Cliente(id, nuevoNombre, estado, datosAdicionales); }
    public Cliente conDatosAdicionales(Map<String, String> datos) { return new Cliente(id, nombre, estado, datos); }
    public Cliente inactivar() { return new Cliente(id, nombre, EstadoCliente.INACTIVO, datosAdicionales); }
    public Cliente conId(Long nuevoId) { return new Cliente(nuevoId, nombre, estado, datosAdicionales); }
}
