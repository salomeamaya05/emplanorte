# Frontend - EMPLANORTE S.A.S.

Interfaz de usuario del sistema construida utilizando **HTML5**, **CSS3** y **JavaScript (Vanilla)**. Es un desarrollo ágil y ligero que se comunica con el backend mediante la API REST.

## 📂 Organización de Archivos
- `index.html`: Pantalla de inicio de sesión (Login).
- `pages/`: Módulos de la aplicación:
  - `dashboard.html` (Dashboard y resumen financiero)
  - `inventario.html` (Control de productos)
  - `ventas.html` (Registro de pedidos)
  - `gastos.html` (Control de gastos)
  - `clientes.html` (Control de clientes)
  - `cotizaciones.html` (Gestión de propuestas comerciales)
  - `estadisticas.html` (Análisis financiero visual)
- `css/`: Estilos CSS modularizados:
  - `styles.css` (Base y Layout principal)
  - `components.css` (Botones, modales, tablas, alertas)
  - `responsive.css` (Adaptación móvil y pantallas)
- `js/`: Lógica interactiva en archivos JavaScript puros correspondientes a cada página.
  - `api.js` (Cliente fetch para llamar a la API REST)
  - `auth.js` (Manejo del token de sesión)

## 🚀 Integración con el Backend
Toda petición de datos se realiza a través del cliente unificado en `js/api.js` que apunta a los endpoints configurados en el backend (`http://localhost:8080/api/...`).
