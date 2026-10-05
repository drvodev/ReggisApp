package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.model.EstadoPedido;
import org.drvo.reggisapp.domain.model.Moneda;
import org.drvo.reggisapp.domain.model.Pago;
import org.drvo.reggisapp.domain.model.Pedido;
import org.drvo.reggisapp.domain.model.ResumenTiempo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TiempoServiceTest {
    private final TiempoService service = new TiempoServiceImpl();

    @Test
    void promedioIncluyeDiasHastaPrimerPagoYDuracionHastaPagoFinal() {
        Pedido pedido = pedido(LocalDate.of(2026, 10, 1));
        List<Pago> pagos = List.of(
                pago(1L, LocalDate.of(2026, 10, 4), "40.00"),
                pago(1L, LocalDate.of(2026, 10, 9), "30.00"),
                pago(1L, LocalDate.of(2026, 10, 16), "30.00"));

        ResumenTiempo resumen = service.calcularTiempo(pedido, pagos);

        assertEquals(5, resumen.getPromedioDiasEntrePagos());
        assertEquals(15, resumen.getDiasHastaCancelacion());
        assertEquals(3, resumen.getDiasHastaPrimerPago());
    }

    @Test
    void sinPagosNoInventaIntervalosNiDuracionDeCancelacion() {
        ResumenTiempo resumen = service.calcularTiempo(pedido(LocalDate.of(2026, 10, 1)), List.of());

        assertEquals(0, resumen.getPromedioDiasEntrePagos());
        assertEquals(0, resumen.getDiasHastaPrimerPago());
        assertNull(resumen.getDiasHastaCancelacion());
    }

    @Test
    void calculaSoloHastaElPagoQueCierraLaDeuda() {
        Pedido pedido = pedido(LocalDate.of(2026, 10, 1));
        List<Pago> pagos = List.of(
                pago(1L, LocalDate.of(2026, 10, 4), "40.00"),
                pago(1L, LocalDate.of(2026, 10, 9), "60.00"),
                pago(1L, LocalDate.of(2026, 10, 20), "25.00"));

        ResumenTiempo resumen = service.calcularTiempo(pedido, pagos);

        assertEquals(4, resumen.getPromedioDiasEntrePagos());
        assertEquals(8, resumen.getDiasHastaCancelacion());
    }

    private Pedido pedido(LocalDate creada) {
        return new Pedido(1L, 1L, new BigDecimal("100.00"), new BigDecimal("100.00"), Moneda.BS,
                new BigDecimal("840.00"), creada, EstadoPedido.ACTIVO);
    }

    private Pago pago(long pedidoId, LocalDate fecha, String monto) {
        return new Pago(null, pedidoId, fecha, new BigDecimal(monto), new BigDecimal(monto),
                Moneda.BS, new BigDecimal("840.00"));
    }
}
