package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.domain.model.CampoCliente;
import org.drvo.reggisapp.domain.model.Cliente;
import org.drvo.reggisapp.domain.model.RegistroHistorial;
import org.drvo.reggisapp.repository.CampoClienteRepository;
import org.drvo.reggisapp.repository.ClienteRepository;
import org.drvo.reggisapp.repository.HistorialRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ClienteServiceImpl implements ClienteService {
    private final ClienteRepository clienteRepository;
    private final CampoClienteRepository campoClienteRepository;
    private final HistorialRepository historialRepository;
    private final Clock clock;

    public ClienteServiceImpl(ClienteRepository clienteRepository, CampoClienteRepository campoClienteRepository,
                              HistorialRepository historialRepository, Clock clock) {
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
        this.campoClienteRepository = Objects.requireNonNull(campoClienteRepository);
        this.historialRepository = Objects.requireNonNull(historialRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Cliente crear(String nombre) {
        validarNombre(nombre);
        Cliente guardado = clienteRepository.guardar(Cliente.nuevo(nombre));
        if (guardado == null || guardado.getId() == null) {
            throw new ReglaNegocioException("No se pudo guardar el cliente con un identificador válido.");
        }
        historialRepository.guardar(new RegistroHistorial(null, "CLIENTE", guardado.getId(),
                "CLIENTE_CREADO", LocalDateTime.now(clock), null, null, null));
        return guardado;
    }

    @Override
    public Cliente actualizar(Long clienteId, String nombre, Map<String, String> datosAdicionales) {
        validarId(clienteId);
        validarNombre(nombre);
        Cliente actual = clienteRepository.buscarPorId(clienteId)
                .orElseThrow(() -> new ReglaNegocioException("No se encontró el cliente."));
        Cliente actualizado = actual.renombrar(nombre).conDatosAdicionales(
                datosAdicionales == null ? actual.getDatosAdicionales() : datosAdicionales);
        Cliente guardado = clienteRepository.actualizar(actualizado);
        historialRepository.guardar(new RegistroHistorial(null, "CLIENTE", clienteId,
                "CLIENTE_ACTUALIZADO", LocalDateTime.now(clock), null, null, null));
        return guardado;
    }

    @Override
    public Cliente inactivar(Long clienteId) {
        validarId(clienteId);
        Cliente actual = clienteRepository.buscarPorId(clienteId)
                .orElseThrow(() -> new ReglaNegocioException("No se encontró el cliente."));
        Cliente inactivo = clienteRepository.actualizar(actual.inactivar());
        historialRepository.guardar(new RegistroHistorial(null, "CLIENTE", clienteId,
                "CLIENTE_INACTIVADO", LocalDateTime.now(clock), "Inactivación confirmada", null, null));
        return inactivo;
    }

    @Override
    public List<Cliente> listarActivos() {
        return clienteRepository.listarActivos();
    }

    @Override
    public CampoCliente crearCampo(String clave, String etiqueta) {
        if (clave == null || clave.isBlank() || etiqueta == null || etiqueta.isBlank()) {
            throw new ReglaNegocioException("La clave y la etiqueta del campo son obligatorias.");
        }
        String claveNormalizada = clave.trim().toLowerCase();
        boolean existe = campoClienteRepository.listarActivos().stream()
                .anyMatch(campo -> campo.getClave().equalsIgnoreCase(claveNormalizada));
        if (existe) throw new ReglaNegocioException("Ya existe un campo con esa clave.");
        return campoClienteRepository.guardar(new CampoCliente(null, claveNormalizada, etiqueta.trim(), true));
    }

    @Override
    public List<CampoCliente> listarCamposActivos() {
        return campoClienteRepository.listarActivos();
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new ReglaNegocioException("El nombre del cliente es obligatorio.");
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) throw new ReglaNegocioException("El identificador del cliente no es válido.");
    }
}
