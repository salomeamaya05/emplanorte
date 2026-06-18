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
        // CP-40 - El nombre del cliente es obligatorio
        if (cliente.getNombre() == null || cliente.getNombre().isBlank()) {
            throw new RuntimeException("El nombre del cliente es obligatorio");
        }
        // CP-41 - El teléfono, si viene, debe tener formato válido (7 a 15 dígitos,
        // admite un prefijo + opcional). Ej.: 3001234567 o +573001234567
        if (cliente.getTelefono() != null && !cliente.getTelefono().isBlank()
                && !cliente.getTelefono().trim().matches("\\+?\\d{7,15}")) {
            throw new RuntimeException("El número de contacto tiene un formato inválido");
        }
        cliente.setActivo(true);
        return clienteRepository.save(cliente);
    }

    public Cliente actualizar(Long id, Cliente detalles) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        cliente.setNombre(detalles.getNombre());
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
}
