package org.drvo.reggisapp.repository;

import org.drvo.reggisapp.domain.model.MovimientoDeuda;

import java.math.BigDecimal;

public interface MovimientoDeudaRepository {
    MovimientoDeuda guardar(MovimientoDeuda movimiento);
    BigDecimal sumarEntradasBs(Long pedidoId);
    BigDecimal sumarSalidasBs(Long pedidoId);
}
