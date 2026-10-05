package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.domain.model.Cliente;
import org.drvo.reggisapp.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {
    @Mock private ClienteRepository clienteRepository;

    @Test
    void reporteIncluyeLosClientesSeleccionadosUnaSolaVez() {
        when(clienteRepository.buscarPorId(4L)).thenReturn(Optional.of(new Cliente(4L, "Ana")));
        when(clienteRepository.buscarPorId(9L)).thenReturn(Optional.of(new Cliente(9L, "Luis")));
        ReporteService service = new ReporteServiceImpl(clienteRepository);

        List<Cliente> seleccionados = service.seleccionarClientes(List.of(4L, 9L, 4L));

        assertEquals(List.of(4L, 9L), seleccionados.stream().map(Cliente::getId).toList());
        verify(clienteRepository, times(1)).buscarPorId(4L);
        verify(clienteRepository, times(1)).buscarPorId(9L);
    }

    @Test
    void reporteRequiereAlMenosUnCliente() {
        ReporteService service = new ReporteServiceImpl(clienteRepository);

        assertThrows(ReglaNegocioException.class, () -> service.seleccionarClientes(List.of()));
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void reporteFallaSiNoEncuentraUnClienteSeleccionado() {
        when(clienteRepository.buscarPorId(4L)).thenReturn(Optional.empty());
        ReporteService service = new ReporteServiceImpl(clienteRepository);

        assertThrows(ReglaNegocioException.class, () -> service.seleccionarClientes(List.of(4L)));
    }
}
