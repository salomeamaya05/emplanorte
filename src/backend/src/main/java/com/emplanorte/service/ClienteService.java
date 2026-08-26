package com.emplanorte.service;

import com.emplanorte.model.Cliente;
import com.emplanorte.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    public List<Cliente> obtenerTodosActivos() {
        return clienteRepository.findByActivoTrue();
    }

    public Optional<Cliente> obtenerPorId(Long id) {
        return clienteRepository.findById(id).filter(Cliente::getActivo);
    }

    public Cliente guardar(Cliente cliente) {
        validarYNormalizar(cliente, null);
        cliente.setActivo(true);
        return clienteRepository.save(cliente);
    }

    public Cliente actualizar(Long id, Cliente detalles) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        validarYNormalizar(detalles, id);
        cliente.setNombre(detalles.getNombre());
        cliente.setDocumento(detalles.getDocumento());
        cliente.setTelefono(detalles.getTelefono());
        cliente.setDireccion(detalles.getDireccion());
        cliente.setObservaciones(detalles.getObservaciones());

        return clienteRepository.save(cliente);
    }

    public void desactivar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    private void validarYNormalizar(Cliente cliente, Long idActual) {
        if (cliente == null || cliente.getNombre() == null || cliente.getNombre().isBlank()) {
            throw new RuntimeException("El nombre del cliente es obligatorio");
        }
        cliente.setNombre(cliente.getNombre().trim());

        String telefono = cliente.getTelefono() == null ? "" : cliente.getTelefono().trim();
        if (!telefono.isBlank() && !telefono.matches("\\+?\\d{7,15}")) {
            throw new RuntimeException("El número de contacto tiene un formato inválido");
        }
        cliente.setTelefono(telefono);

        String documento = cliente.getDocumento() == null ? "" : cliente.getDocumento().trim();
        if (documento.isBlank()) {
            cliente.setDocumento(null);
            return;
        }
        if (!documento.matches("[A-Za-z0-9][A-Za-z0-9 .-]{2,39}")) {
            throw new RuntimeException("El documento tiene un formato inválido");
        }
        clienteRepository.findByDocumentoIgnoreCase(documento).ifPresent(existente -> {
            if (idActual == null || !existente.getId().equals(idActual)) {
                throw new RuntimeException("Ya existe un cliente con ese documento");
            }
        });
        cliente.setDocumento(documento);
    }
}
