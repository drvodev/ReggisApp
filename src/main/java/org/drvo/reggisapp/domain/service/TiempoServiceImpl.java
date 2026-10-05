package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.domain.model.Pago;
import org.drvo.reggisapp.domain.model.Pedido;
import org.drvo.reggisapp.domain.model.ResumenTiempo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class TiempoServiceImpl implements TiempoService {
    @Override
    public ResumenTiempo calcularTiempo(Pedido pedido, List<Pago> pagos) {
        Objects.requireNonNull(pedido, "El pedido es obligatorio.");
        if (pagos == null || pagos.isEmpty()) return new ResumenTiempo(0, 0, null);

        List<Pago> ordenados = pagos.stream()
                .sorted(Comparator.comparing(Pago::getFechaPago))
                .toList();
        LocalDate fechaAnterior = pedido.getFechaCreacion();
        long sumaDias = 0;
        long intervalos = 0;
        long diasPrimerPago = 0;
        BigDecimal acumulado = BigDecimal.ZERO;
        Long duracionTotal = null;

        for (Pago pago : ordenados) {
            if (!pedido.getId().equals(pago.getPedidoId())) {
                throw new ReglaNegocioException("La lista incluye un pago de otro pedido.");
            }
            if (pago.getFechaPago().isBefore(fechaAnterior)) {
                throw new ReglaNegocioException("Las fechas de pago no pueden preceder a la creación del pedido.");
            }
            long dias = ChronoUnit.DAYS.between(fechaAnterior, pago.getFechaPago());
            if (intervalos == 0) diasPrimerPago = dias;
            sumaDias += dias;
            intervalos++;
            acumulado = acumulado.add(pago.getMontoBs());
            fechaAnterior = pago.getFechaPago();
            if (acumulado.compareTo(pedido.getMontoPedidoBs()) >= 0) {
                duracionTotal = ChronoUnit.DAYS.between(pedido.getFechaCreacion(), pago.getFechaPago());
                break;
            }
        }

        long promedio = intervalos == 0 ? 0 : BigDecimal.valueOf(sumaDias)
                .divide(BigDecimal.valueOf(intervalos), 0, RoundingMode.HALF_UP).longValue();
        return new ResumenTiempo(promedio, diasPrimerPago, duracionTotal);
    }
}
