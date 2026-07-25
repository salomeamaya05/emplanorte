/* ============================================================
   CLIENTE API - EMPLANORTE S.A.S.
   Wrapper de comunicación con el backend (http://localhost:8080/api)
   ============================================================ */
// URL del backend desplegado en Render. Debe coincidir con el nombre del
// servicio web definido en render.yaml (emplanorte-backend). Si Render le
// asigna un sufijo distinto al crear el servicio, actualiza esta línea.
const API_BASE_URL = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8080/api'
    : 'https://emplanorte-2.onrender.com/api';

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

    async anularVenta(id, idUsuario, contrasena) {
        return this.request(`/ventas/${id}/anular`, {
            method: 'POST',
            body: JSON.stringify({ idUsuario, contrasena })
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
    // 7. MÓDULO DASHBOARD & REPORTES (RF11, RF12)
    // ==========================================
    async obtenerResumenDashboard(desde, hasta) {
        return this.request(`/dashboard/resumen?desde=${desde}&hasta=${hasta}`, { method: 'GET' });
    }
};
