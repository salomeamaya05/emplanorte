package com.emplanorte.service;

import com.emplanorte.model.Producto;
import com.emplanorte.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    public List<Producto> obtenerTodosActivos() {
        return productoRepository.findByActivoTrue();
    }

    // RF04 - Ordenamiento de productos del inventario por diferentes criterios
    public List<Producto> obtenerTodosActivosOrdenados(String criterio) {
        Sort sort = Sort.by(Sort.Direction.ASC, "nombre"); // Criterio por defecto
        
        if (criterio != null) {
            switch (criterio.toLowerCase()) {
                case "nombre":
                    sort = Sort.by(Sort.Direction.ASC, "nombre");
                    break;
                case "tamano":
                case "capacidad":
                    sort = Sort.by(Sort.Direction.ASC, "capacidadMl");
                    break;
                case "categoria":
                    sort = Sort.by(Sort.Direction.ASC, "categoria.nombre");
                    break;
                case "cantidad":
                case "stock":
                    sort = Sort.by(Sort.Direction.ASC, "stockDisponible");
                    break;
            }
        }
        return productoRepository.findAll(sort).stream()
                .filter(Producto::getActivo)
                .toList();
    }

    public Optional<Producto> obtenerPorId(Long id) {
        return productoRepository.findById(id).filter(Producto::getActivo);
    }

    public Producto guardar(Producto producto) {
        validarProducto(producto);

        // CP-04 - Evitar nombres duplicados (entre los productos activos)
        boolean nombreDuplicado = productoRepository.findByActivoTrue().stream()
                .anyMatch(p -> p.getNombre() != null
                        && p.getNombre().trim().equalsIgnoreCase(producto.getNombre().trim()));
        if (nombreDuplicado) {
            throw new RuntimeException("Ya existe un producto con el nombre: " + producto.getNombre().trim());
        }

        producto.setActivo(true);
        return productoRepository.save(producto);
    }

    // RF01/RF03 - Validaciones de integridad de los datos del producto
    private void validarProducto(Producto producto) {
        if (producto.getUnidadesPorPaca() == null) {
            producto.setUnidadesPorPaca(1);
        }
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            throw new RuntimeException("El nombre del producto es obligatorio");
        }
        if (producto.getUnidadesPorPaca() <= 0) {
            throw new RuntimeException("Las unidades por paca deben ser mayores a cero");
        }
        if (producto.getStockDisponible() != null && producto.getStockDisponible() < 0) {
            throw new RuntimeException("El stock disponible no puede ser negativo");
        }
        if (producto.getStockMinimo() != null && producto.getStockMinimo() < 0) {
            throw new RuntimeException("El stock mínimo no puede ser negativo");
        }
        if (producto.getCostoUnitario() != null
                && producto.getCostoUnitario().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El costo unitario no puede ser negativo");
        }
        if (producto.getPrecioVenta() == null
                || producto.getPrecioVenta().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El precio de venta debe ser mayor a cero");
        }
    }

    public Producto actualizar(Long id, Producto productoDetalles) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con el ID: " + id));

        validarProducto(productoDetalles);
        boolean nombreDuplicado = productoRepository.findByActivoTrue().stream()
                .anyMatch(p -> !p.getId().equals(id)
                        && p.getNombre() != null
                        && p.getNombre().trim().equalsIgnoreCase(productoDetalles.getNombre().trim()));
        if (nombreDuplicado) {
            throw new RuntimeException("Ya existe otro producto activo con el nombre: " + productoDetalles.getNombre().trim());
        }

        producto.setCodigo(productoDetalles.getCodigo());
        producto.setNombre(productoDetalles.getNombre());
        producto.setDescripcion(productoDetalles.getDescripcion());
        producto.setCategoria(productoDetalles.getCategoria());
        producto.setCapacidadMl(productoDetalles.getCapacidadMl());
        producto.setUnidadesPorPaca(productoDetalles.getUnidadesPorPaca());
        producto.setCostoUnitario(productoDetalles.getCostoUnitario());
        producto.setPrecioVenta(productoDetalles.getPrecioVenta());
        producto.setStockDisponible(productoDetalles.getStockDisponible());
        producto.setStockMinimo(productoDetalles.getStockMinimo());
        producto.setUnidadMedida(productoDetalles.getUnidadMedida());

        return productoRepository.save(producto);
    }

    // RF02 - Eliminación lógica (desactivar) de productos para no romper históricos de ventas
    public void desactivar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con el ID: " + id));
        producto.setActivo(false);
        productoRepository.save(producto);
    }


    /**
     * Fusiona dos productos que representan el mismo artículo.
     * Conserva el producto destino, suma el stock, recalcula el costo promedio
     * y desactiva el producto duplicado. Los detalles históricos permanecen
     * asociados al producto original para no alterar ventas o compras pasadas.
     */
    @Transactional
    public Producto fusionar(Long productoDestinoId, Long productoDuplicadoId) {
        if (productoDestinoId == null || productoDuplicadoId == null) {
            throw new RuntimeException("Debe seleccionar los dos productos a fusionar");
        }
        if (productoDestinoId.equals(productoDuplicadoId)) {
            throw new RuntimeException("El producto principal y el duplicado no pueden ser el mismo");
        }

        Long primero = Math.min(productoDestinoId, productoDuplicadoId);
        Long segundo = Math.max(productoDestinoId, productoDuplicadoId);
        Producto p1 = productoRepository.buscarPorIdParaActualizar(primero)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        Producto p2 = productoRepository.buscarPorIdParaActualizar(segundo)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Producto destino = p1.getId().equals(productoDestinoId) ? p1 : p2;
        Producto duplicado = p1.getId().equals(productoDuplicadoId) ? p1 : p2;

        if (!Boolean.TRUE.equals(destino.getActivo()) || !Boolean.TRUE.equals(duplicado.getActivo())) {
            throw new RuntimeException("Solo se pueden fusionar productos activos");
        }

        int stockDestino = destino.getStockDisponible() == null ? 0 : destino.getStockDisponible();
        int stockDuplicado = duplicado.getStockDisponible() == null ? 0 : duplicado.getStockDisponible();
        int stockTotal = stockDestino + stockDuplicado;

        BigDecimal costoDestino = destino.getCostoUnitario() == null ? BigDecimal.ZERO : destino.getCostoUnitario();
        BigDecimal costoDuplicado = duplicado.getCostoUnitario() == null ? BigDecimal.ZERO : duplicado.getCostoUnitario();
        BigDecimal nuevoCosto = costoDestino;
        if (stockTotal > 0) {
            BigDecimal valorDestino = costoDestino.multiply(BigDecimal.valueOf(stockDestino));
            BigDecimal valorDuplicado = costoDuplicado.multiply(BigDecimal.valueOf(stockDuplicado));
            nuevoCosto = valorDestino.add(valorDuplicado)
                    .divide(BigDecimal.valueOf(stockTotal), 2, RoundingMode.HALF_UP);
        }

        destino.setStockDisponible(stockTotal);
        destino.setCostoUnitario(nuevoCosto);
        destino.setStockMinimo(Math.max(
                destino.getStockMinimo() == null ? 0 : destino.getStockMinimo(),
                duplicado.getStockMinimo() == null ? 0 : duplicado.getStockMinimo()
        ));

        duplicado.setStockDisponible(0);
        duplicado.setActivo(false);
        productoRepository.save(duplicado);
        return productoRepository.save(destino);
    }

}
