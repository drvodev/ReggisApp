package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.domain.model.*;
import org.drvo.reggisapp.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarcasServiceTest {
    @Test void guardaLaSeleccionAlCrearElPedido() {
        var clientes = mock(ClienteRepository.class);
        var pedidos = mock(PedidoRepository.class);
        var marcas = mock(MarcaRepository.class);
        var servicio = new CobranzaServiceImpl(clientes, pedidos, mock(PagoRepository.class),
                mock(HistorialRepository.class), mock(MovimientoDeudaRepository.class), new TasaCambioManualService(), Clock.systemUTC());
        servicio.configurarMarcas(marcas);
        when(clientes.buscarPorId(1L)).thenReturn(java.util.Optional.of(Cliente.nuevo("Cliente")));
        var guardado = new Pedido(42L, 1L, "Pedido", BigDecimal.TEN, BigDecimal.TEN, Moneda.BS, BigDecimal.TEN, java.time.LocalDate.now(), EstadoPedido.ACTIVO);
        when(pedidos.guardar(any())).thenReturn(guardado);
        assertEquals(42L, servicio.crearPedidoConMarcas(1L, "Pedido", BigDecimal.TEN, Moneda.BS, BigDecimal.TEN, Set.of(3L)).getId());
        verify(marcas).guardarSeleccion(42L, Set.of(3L));
        assertThrows(ReglaNegocioException.class, () -> servicio.crearPedidoConMarcas(1L, "", BigDecimal.TEN, Moneda.BS, BigDecimal.TEN, Set.of()));
        verifyNoMoreInteractions(marcas);
    }
}
