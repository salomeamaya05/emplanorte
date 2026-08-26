package com.emplanorte.config;

import com.emplanorte.model.CategoriaGasto;
import com.emplanorte.model.CategoriaProducto;
import com.emplanorte.model.Cliente;
import com.emplanorte.model.Producto;
import com.emplanorte.model.Proveedor;
import com.emplanorte.model.Usuario;
import com.emplanorte.repository.CategoriaGastoRepository;
import com.emplanorte.repository.CategoriaProductoRepository;
import com.emplanorte.repository.ClienteRepository;
import com.emplanorte.repository.ProductoRepository;
import com.emplanorte.repository.ProveedorRepository;
import com.emplanorte.repository.UsuarioRepository;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

/**
 * Protecciones y datos ficticios del entorno local.
 * Esta configuración nunca se carga en Render porque exige el perfil "local".
 */
@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalEnvironmentConfiguration {

    /**
     * Se ejecuta antes de crear el DataSource/JPA. Si una variable de entorno
     * intenta reemplazar H2 por PostgreSQL/Supabase, el proceso falla sin tocarla.
     */
    @Bean
    static BeanFactoryPostProcessor impedirBaseRemotaEnLocal() {
        return beanFactory -> {
            Environment environment = beanFactory.getBean(Environment.class);
            String url = environment.getProperty("spring.datasource.url", "");
            String address = environment.getProperty("server.address", "");
            boolean almacenamientoLocal = environment.getProperty(
                    "storage.local.enabled", Boolean.class, false);

            if (!url.startsWith("jdbc:h2:file:./.local-data/")) {
                throw new BeanCreationException(
                        "ARRANQUE LOCAL DETENIDO: la base debe estar dentro de "
                                + "jdbc:h2:file:./.local-data/ y nunca apuntar a Supabase.");
            }
            if (!("127.0.0.1".equals(address) || "localhost".equalsIgnoreCase(address))) {
                throw new BeanCreationException(
                        "ARRANQUE LOCAL DETENIDO: el servidor local debe escuchar solo en 127.0.0.1.");
            }
            if (!almacenamientoLocal) {
                throw new BeanCreationException(
                        "ARRANQUE LOCAL DETENIDO: el almacenamiento de archivos debe ser local.");
            }
        };
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    ApplicationRunner prepararDatosLocales(
            UsuarioRepository usuarios,
            CategoriaProductoRepository categoriasProducto,
            ProductoRepository productos,
            ClienteRepository clientes,
            ProveedorRepository proveedores,
            CategoriaGastoRepository categoriasGasto,
            PasswordEncoder passwordEncoder,
            @Value("${app.local.admin.email}") String adminEmail,
            @Value("${app.local.admin.password}") String adminPassword
    ) {
        return args -> {
            if (adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException(
                        "ARRANQUE LOCAL DETENIDO: configure LOCAL_ADMIN_PASSWORD "
                                + "en src/backend/.env.localhost.");
            }
            Usuario admin = usuarios.findByCorreo(adminEmail).orElseGet(Usuario::new);
            admin.setNombre("Administración Local");
            admin.setCorreo(adminEmail);
            admin.setContrasenaHash(passwordEncoder.encode(adminPassword));
            admin.setRol("administrador");
            admin.setActivo(true);
            usuarios.save(admin);

            CategoriaProducto categoria = categoriasProducto.findByNombreIgnoreCase("Pruebas local")
                    .orElseGet(CategoriaProducto::new);
            categoria.setNombre("Pruebas local");
            categoria.setDescripcion("Datos ficticios exclusivos del localhost");
            categoria.setActivo(true);
            categoria = categoriasProducto.save(categoria);

            guardarProductoLocal(
                    productos, categoria, "LOCAL-5L", "Envase local 5 litros",
                    new BigDecimal("5000"), 32, new BigDecimal("2450"),
                    new BigDecimal("4200"), 640, 64);
            guardarProductoLocal(
                    productos, categoria, "LOCAL-150", "Envase local 150 ml",
                    new BigDecimal("150"), 150, new BigDecimal("500"),
                    new BigDecimal("900"), 3000, 300);

            Cliente cliente = clientes.findFirstByNombreIgnoreCase("Cliente Pruebas Local")
                    .orElseGet(Cliente::new);
            cliente.setNombre("Cliente Pruebas Local");
            cliente.setDocumento("LOCAL-CC-0001");
            cliente.setTelefono("3000000000");
            cliente.setDireccion("Dirección ficticia");
            cliente.setObservaciones("No corresponde a una persona real");
            cliente.setActivo(true);
            clientes.save(cliente);

            Proveedor proveedor = proveedores.findByNitDocumentoIgnoreCase("LOCAL-900000000")
                    .orElseGet(Proveedor::new);
            proveedor.setNitDocumento("LOCAL-900000000");
            proveedor.setRazonSocial("Proveedor Pruebas Local");
            proveedor.setContactoNombre("Contacto ficticio");
            proveedor.setTelefono("3000000001");
            proveedor.setCorreo("proveedor.local@emplanorte.test");
            proveedor.setDireccion("Dirección ficticia");
            proveedor.setCiudad("Localhost");
            proveedor.setCondicionesPago("Contado");
            proveedor.setObservaciones("No corresponde a un proveedor real");
            proveedor.setActivo(true);
            proveedores.save(proveedor);

            CategoriaGasto categoriaGasto = categoriasGasto.findByNombreIgnoreCase("Pruebas local")
                    .orElseGet(CategoriaGasto::new);
            categoriaGasto.setNombre("Pruebas local");
            categoriaGasto.setDescripcion("Gastos ficticios del entorno local");
            categoriaGasto.setActivo(true);
            categoriasGasto.save(categoriaGasto);
        };
    }

    private static void guardarProductoLocal(
            ProductoRepository productos,
            CategoriaProducto categoria,
            String codigo,
            String nombre,
            BigDecimal capacidadMl,
            int unidadesPorPaca,
            BigDecimal costo,
            BigDecimal precio,
            int stock,
            int stockMinimo
    ) {
        Producto producto = productos.findByCodigo(codigo).orElseGet(Producto::new);
        boolean productoNuevo = producto.getId() == null;
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setDescripcion("Producto ficticio exclusivo para pruebas locales");
        producto.setCategoria(categoria);
        producto.setCapacidadMl(capacidadMl);
        producto.setUnidadesPorPaca(unidadesPorPaca);
        producto.setPrecioVenta(precio);
        producto.setStockMinimo(stockMinimo);
        producto.setUnidadMedida("unidades");
        producto.setActivo(true);
        // El inventario y el costo solo son valores semilla. En reinicios posteriores
        // se conservan los movimientos realizados durante las pruebas locales.
        if (productoNuevo) {
            producto.setCostoUnitario(costo);
            producto.setStockDisponible(stock);
        }
        productos.save(producto);
    }
}
