package org.drvo.reggisapp.repository;

import org.drvo.reggisapp.domain.model.RegistroHistorial;

import java.util.List;

public interface HistorialRepository {
    RegistroHistorial guardar(RegistroHistorial registro);
    List<RegistroHistorial> listarPorEntidad(String tipoEntidad, Long entidadId);
}
