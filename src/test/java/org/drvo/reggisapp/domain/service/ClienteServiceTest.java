package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.domain.model.CampoCliente;
import org.drvo.reggisapp.domain.model.Cliente;
import org.drvo.reggisapp.domain.model.EstadoCliente;
import org.drvo.reggisapp.repository.CampoClienteRepository;
import org.drvo.reggisapp.repository.ClienteRepository;
import org.drvo.reggisapp.repository.HistorialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {
    @Mock private ClienteRepository clienteRepository;
    @Mock private CampoClienteRepository campoClienteRepository;
    @Mock private HistorialRepository historialRepository;

    private ClienteService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC);
        service = new ClienteServiceImpl(clienteRepository, campoClienteRepository, historialRepository, clock);
    }

    @Test
    void crearClienteRequiereNombreYRegistraHistorial() {
        when(clienteRepository.guardar(any(Cliente.class))).thenAnswer(invocation ->
                ((Cliente) invocation.getArgument(0)).conId(8L));

        Cliente cliente = service.crear("  Ana Pérez  ");

        assertEquals(8L, cliente.getId());
        assertEquals("Ana Pérez", cliente.getNombre());
        assertEquals(EstadoCliente.ACTIVO, cliente.getEstado());
        verify(historialRepository).guardar(any());
        assertThrows(ReglaNegocioException.class, () -> service.crear(" "));
    }

    @Test
    void actualizarClientePermiteCamposAdicionalesElegidosPorElUsuario() {
        Cliente cliente = new Cliente(8L, "Ana");
        when(clienteRepository.buscarPorId(8L)).thenReturn(Optional.of(cliente));
        when(clienteRepository.actualizar(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente actualizado = service.actualizar(8L, "Ana Pérez", Map.of("rif", "J-123", "telefono", "0412"));

        assertEquals("Ana Pérez", actualizado.getNombre());
        assertEquals("J-123", actualizado.getDatosAdicionales().get("rif"));
        assertEquals("0412", actualizado.getDatosAdicionales().get("telefono"));
        verify(historialRepository).guardar(any());
    }

    @Test
    void inactivarClienteConservaSuRegistroYAnotaElCambio() {
        when(clienteRepository.buscarPorId(8L)).thenReturn(Optional.of(new Cliente(8L, "Ana")));
        when(clienteRepository.actualizar(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente cliente = service.inactivar(8L);

        assertEquals(EstadoCliente.INACTIVO, cliente.getEstado());
        verify(historialRepository).guardar(any());
    }

    @Test
    void noPermiteDefinirDosCamposActivosConLaMismaClave() {
        when(campoClienteRepository.listarActivos()).thenReturn(List.of(
                new CampoCliente(1L, "rif", "RIF", true)));

        assertThrows(ReglaNegocioException.class, () -> service.crearCampo("RIF", "Identificación fiscal"));
        verify(campoClienteRepository, never()).guardar(any());
    }
}
