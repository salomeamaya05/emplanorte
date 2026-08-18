/* ============================================================
   CLIENTE API - EMPLANORTE S.A.S.
   Wrapper de comunicación con el backend (http://localhost:8080/api)
   ============================================================ */
const API_BASE_URL = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8080/api'
    : 'https://emplanorte-2-cx20.onrender.com/api';

// Indicador global para todas las operaciones contra el backend. Usa un contador
// para no ocultarse antes de tiempo cuando una pantalla lanza varias peticiones.
const LoadingIndicator = (() => {
    let solicitudesActivas = 0;
    let overlay = null;

    function obtenerOverlay() {
        if (overlay && document.body.contains(overlay)) return overlay;
        overlay = document.createElement('div');
        overlay.className = 'global-loading-overlay';
        overlay.setAttribute('role', 'status');
        overlay.setAttribute('aria-live', 'polite');
        overlay.setAttribute('aria-label', 'Cargando, por favor espere');
        overlay.innerHTML = `
            <div class="global-loading-card">
                <div class="global-loading-spinner" aria-hidden="true"></div>
                <strong>Cargando</strong>
                <span>Espere un momento, por favor…</span>
            </div>
        `;
        document.body.appendChild(overlay);
        return overlay;
    }

    function mostrar() {
        solicitudesActivas += 1;
        const elemento = obtenerOverlay();
        elemento.classList.add('show');
        elemento.setAttribute('aria-hidden', 'false');
        document.documentElement.classList.add('is-loading');
    }

    function ocultar() {
        solicitudesActivas = Math.max(0, solicitudesActivas - 1);
        if (solicitudesActivas > 0 || !overlay) return;
        overlay.classList.remove('show');
        overlay.setAttribute('aria-hidden', 'true');
        document.documentElement.classList.remove('is-loading');
    }

    return { mostrar, ocultar };
})();

window.LoadingIndicator = LoadingIndicator;

const ApiClient = {
    // Utilidad interna para peticiones fetch
    async request(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;

        // Configurar encabezados por defecto
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        // Obtener el token de localStorage si existe
        const session = localStorage.getItem('emplanorte_session');
        if (session) {
            const { token } = JSON.parse(session);
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }
        }

        const config = {
            ...options,
            headers
        };

        LoadingIndicator.mostrar();
        try {
            const response = await fetch(url, config);

            if (response.status === 204) {
                return true;
            }

            // Leer el cuerpo como texto primero para manejar respuestas JSON y texto plano
            const text = await response.text();
            let data;
            try {
                data = JSON.parse(text);
            } catch (_) {
                data = text; // El backend devolvió texto plano (ej: mensajes de error)
            }

            if (!response.ok) {
                const msg = (typeof data === 'object' && data !== null)
                    ? (data.message || data.error || JSON.stringify(data))
                    : String(data || 'Error en la petición');
                throw new Error(msg);
            }

            return data;
        } catch (error) {
            console.error(`Error en API request [${url}]:`, error);
            throw error;
        } finally {
            LoadingIndicator.ocultar();
        }
    },

    async requestFormData(endpoint, formData, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const headers = { ...(options.headers || {}) };
        const session = localStorage.getItem('emplanorte_session');
        if (session) {
            const { token } = JSON.parse(session);
            if (token) headers['Authorization'] = `Bearer ${token}`;
        }
        LoadingIndicator.mostrar();
        try {
            const response = await fetch(url, { ...options, headers, body: formData });
            const text = await response.text();
            let data;
            try { data = JSON.parse(text); } catch (_) { data = text; }
            if (!response.ok) {
                const msg = typeof data === 'object' && data !== null ? (data.message || data.error || JSON.stringify(data)) : String(data || 'Error en la petición');
                throw new Error(msg);
            }
            return data;
        } catch (error) {
            console.error(`Error en API request [${url}]:`, error);
            throw error;
        } finally {
            LoadingIndicator.ocultar();
        }
    },

    // ==========================================
    // 1. MÓDULO AUTENTICACIÓN
    // ==========================================
    async login(correo, contrasena) {
        return this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ correo, contrasena })
        });
    },

    logout() {
        localStorage.removeItem('emplanorte_session');
        window.location.href = '../index.html';
    },

    // ==========================================
    // 2. MÓDULO INVENTARIO (RF01-RF04)
    // ==========================================
    async listarProductos(ordenarPor = null) {
        const query = ordenarPor ? `?ordenarPor=${ordenarPor}` : '';
        return this.request(`/productos${query}`, { method: 'GET' });
    },

    async obtenerProducto(id) {
        return this.request(`/productos/${id}`, { method: 'GET' });
    },

    async crearProducto(producto) {
        return this.request('/productos', {
            method: 'POST',
            body: JSON.stringify(producto)
        });
    },

    async actualizarProducto(id, producto) {
        return this.request(`/productos/${id}`, {
            method: 'PUT',
            body: JSON.stringify(producto)
        });
    },

    async eliminarProducto(id) {
        return this.request(`/productos/${id}`, { method: 'DELETE' });
    },

    async fusionarProductos(productoDestinoId, productoDuplicadoId) {
        return this.request('/productos/fusionar', {
            method: 'POST',
            body: JSON.stringify({ productoDestinoId, productoDuplicadoId })
        });
    },

    async listarCategoriasProducto() {
        return this.request('/categorias-producto', { method: 'GET' });
    },

    async crearCategoriaProducto(categoria) {
        return this.request('/categorias-producto', {
            method: 'POST',
            body: JSON.stringify(categoria)
        });
    },

    async actualizarCategoriaProducto(id, categoria) {
        return this.request(`/categorias-producto/${id}`, {
            method: 'PUT',
            body: JSON.stringify(categoria)
        });
    },

    async eliminarCategoriaProducto(id) {
        return this.request(`/categorias-producto/${id}`, { method: 'DELETE' });
    },

    // ==========================================
    // 3. MÓDULO CLIENTES (RF13)
    // ==========================================
    async listarClientes() {
        return this.request('/clientes', { method: 'GET' });
    },

    async obtenerCliente(id) {
        return this.request(`/clientes/${id}`, { method: 'GET' });
    },

    async crearCliente(cliente) {
        return this.request('/clientes', {
            method: 'POST',
            body: JSON.stringify(cliente)
        });
    },

    async actualizarCliente(id, cliente) {
        return this.request(`/clientes/${id}`, {
            method: 'PUT',
            body: JSON.stringify(cliente)
        });
    },

    async eliminarCliente(id) {
        return this.request(`/clientes/${id}`, { method: 'DELETE' });
    },

    // ==========================================
    // 4. MÓDULO GASTOS (RF09, RF10)
    // ==========================================
    async listarGastos() {
        return this.request('/gastos', { method: 'GET' });
    },

    async listarGastosPorRango(desde, hasta) {
        return this.request(`/gastos/rango?desde=${desde}&hasta=${hasta}`, { method: 'GET' });
    },

    async registrarGasto(gasto) {
        return this.request('/gastos', {
            method: 'POST',
            body: JSON.stringify(gasto)
        });
    },
    async actualizarGasto(id, gasto) {
        return this.request(`/gastos/${id}`, {
            method: 'PUT',
            body: JSON.stringify(gasto)
        });
    },

    async eliminarGasto(id) {
        return this.request(`/gastos/${id}`, { method: 'DELETE' });
    },

    async listarAuditoriaGasto(id) {
        return this.request(`/gastos/${id}/auditoria`, { method: 'GET' });
    },

    async listarCategoriasGasto() {
        return this.request('/gastos/categorias', { method: 'GET' });
    },

    async crearCategoriaGasto(categoria) {
        return this.request('/gastos/categorias', {
            method: 'POST',
            body: JSON.stringify(categoria)
        });
    },

    // ==========================================
    // 5. MÓDULO VENTAS (RF05-RF08)
    // ==========================================
    async listarVentas() {
        return this.request('/ventas', { method: 'GET' });
    },

    async registrarVenta(ventaRequest) {
        return this.request('/ventas', {
            method: 'POST',
            body: JSON.stringify(ventaRequest)
        });
    },
    async actualizarVenta(id, ventaRequest) {
        return this.request(`/ventas/${id}`, {
            method: 'PUT',
            body: JSON.stringify(ventaRequest)
        });
    },

    async listarDetalleVenta(id) {
        return this.request(`/ventas/${id}/detalles`, { method: 'GET' });
    },

    async anularVenta(id, idUsuario, contrasena, motivo, corregir = false) {
        return this.request(`/ventas/${id}/anular`, {
            method: 'POST',
            body: JSON.stringify({ idUsuario, contrasena, motivo, corregir })
        });
    },

    async listarAuditoriaVenta(id) {
        return this.request(`/ventas/${id}/auditoria`, { method: 'GET' });
    },

    // ==========================================
    // 6. MÓDULO COTIZACIONES (RF14)
    // ==========================================
    async listarCotizaciones() {
        return this.request('/cotizaciones', { method: 'GET' });
    },

    async crearCotizacion(cotizacionRequest) {
        return this.request('/cotizaciones', {
            method: 'POST',
            body: JSON.stringify(cotizacionRequest)
        });
    },
    async actualizarCotizacion(id, cotizacionRequest) {
        return this.request(`/cotizaciones/${id}`, {
            method: 'PUT',
            body: JSON.stringify(cotizacionRequest)
        });
    },

    async listarDetalleCotizacion(id) {
        return this.request(`/cotizaciones/${id}/detalles`, { method: 'GET' });
    },

    async convertirCotizacionAVenta(id, metodoPago, referenciaPago = null) {
        return this.request(`/cotizaciones/convertir/${id}`, {
            method: 'POST',
            body: JSON.stringify({ metodoPago, referenciaPago })
        });
    },

    async eliminarCotizacion(id) {
        return this.request(`/cotizaciones/${id}`, { method: 'DELETE' });
    },

    // ==========================================
    // 7. MÓDULO PROVEEDORES
    // ==========================================
    async listarProveedores(incluirInactivos = false) { return this.request(`/proveedores?incluirInactivos=${incluirInactivos}`, { method: 'GET' }); },
    async obtenerProveedor(id) { return this.request(`/proveedores/${id}`, { method: 'GET' }); },
    async obtenerResumenProveedor(id) { return this.request(`/proveedores/${id}/resumen`, { method: 'GET' }); },
    async crearProveedor(data) { return this.request('/proveedores', { method: 'POST', body: JSON.stringify(data) }); },
    async actualizarProveedor(id, data) { return this.request(`/proveedores/${id}`, { method: 'PUT', body: JSON.stringify(data) }); },
    async desactivarProveedor(id) { return this.request(`/proveedores/${id}`, { method: 'DELETE' }); },

    // ==========================================
    // 8. MÓDULO COMPRAS
    // ==========================================
    async listarCompras() { return this.request('/compras', { method: 'GET' }); },
    async obtenerCompra(id) { return this.request(`/compras/${id}`, { method: 'GET' }); },
    async listarDetalleCompra(id) { return this.request(`/compras/${id}/detalles`, { method: 'GET' }); },
    async listarAuditoriaCompra(id) { return this.request(`/compras/${id}/auditoria`, { method: 'GET' }); },
    async registrarCompra(data) { return this.request('/compras', { method: 'POST', body: JSON.stringify(data) }); },
    async anularCompra(id, data) { return this.request(`/compras/${id}/anular`, { method: 'POST', body: JSON.stringify(data) }); },

    // ==========================================
    // 9. MÓDULO FACTURAS Y PAGOS A PROVEEDORES
    // ==========================================
    async listarFacturasProveedores() { return this.request('/facturas-proveedores', { method: 'GET' }); },
    async obtenerFacturaProveedor(id) { return this.request(`/facturas-proveedores/${id}`, { method: 'GET' }); },
    async obtenerFacturaPorCompra(idCompra) { return this.request(`/facturas-proveedores/compra/${idCompra}`, { method: 'GET' }); },
    async listarAlertasFacturas(dias = 7) { return this.request(`/facturas-proveedores/alertas?dias=${dias}`, { method: 'GET' }); },
    async registrarFacturaProveedor(data) { return this.request('/facturas-proveedores', { method: 'POST', body: JSON.stringify(data) }); },
    async subirAdjuntoFactura(id, file) { const fd = new FormData(); fd.append('archivo', file); return this.requestFormData(`/facturas-proveedores/${id}/adjunto`, fd, { method: 'POST' }); },
    urlAdjuntoFactura(id) { return `${API_BASE_URL}/facturas-proveedores/${id}/adjunto`; },
    async listarPagosFactura(id) { return this.request(`/facturas-proveedores/${id}/pagos`, { method: 'GET' }); },
    async registrarPagoFactura(id, data) { return this.request(`/facturas-proveedores/${id}/pagos`, { method: 'POST', body: JSON.stringify(data) }); },
    async anularPagoProveedor(id, data) { return this.request(`/facturas-proveedores/pagos/${id}/anular`, { method: 'POST', body: JSON.stringify(data) }); },

    // ==========================================
    // 10. MÓDULO DASHBOARD & REPORTES (RF11, RF12)
    // ==========================================
    async obtenerResumenDashboard(desde, hasta) {
        return this.request(`/dashboard/resumen?desde=${desde}&hasta=${hasta}`, { method: 'GET' });
    },

    async obtenerBalanceCompleto(desde, hasta) {
        return this.request(`/dashboard/balance-completo?desde=${desde}&hasta=${hasta}`, { method: 'GET' });
    }
};
