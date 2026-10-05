package org.drvo.reggisapp.repository;

import org.drvo.reggisapp.domain.model.Pago;

import java.math.BigDecimal;
import java.util.List;

public interface PagoRepository {
    Pago guardar(Pago pago);
    List<Pago> listarPorPedido(Long pedidoId);
    BigDecimal sumarPagosBs(Long pedidoId);
}
