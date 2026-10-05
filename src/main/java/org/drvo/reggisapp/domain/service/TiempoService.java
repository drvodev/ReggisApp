package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.model.Pago;
import org.drvo.reggisapp.domain.model.Pedido;
import org.drvo.reggisapp.domain.model.ResumenTiempo;

import java.util.List;

public interface TiempoService {
    ResumenTiempo calcularTiempo(Pedido pedido, List<Pago> pagos);
}
