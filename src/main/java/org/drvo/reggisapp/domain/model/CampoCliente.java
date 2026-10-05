package org.drvo.reggisapp.domain.model;

import java.util.Objects;

public class CampoCliente {
    private final Long id;
    private final String clave;
    private final String etiqueta;
    private final boolean activo;

    public CampoCliente(Long id, String clave, String etiqueta, boolean activo) {
        this.id = id;
        this.clave = Objects.requireNonNull(clave, "La clave del campo es obligatoria.");
        this.etiqueta = Objects.requireNonNull(etiqueta, "La etiqueta del campo es obligatoria.");
        this.activo = activo;
    }

    public Long getId() { return id; }
    public String getClave() { return clave; }
    public String getEtiqueta() { return etiqueta; }
    public boolean isActivo() { return activo; }
}
