/* ============================================================
   UTILIDADES COMPARTIDAS - EMPLANORTE S.A.S.
   Sidebar, navegación, sesión, toasts y helpers reutilizables
   ============================================================ */

// ---- Verificar sesión activa ----
function checkSession() {
    const session = localStorage.getItem('emplanorte_session');
    if (!session) {
        window.location.href = '../index.html';
        return null;
    }
    return JSON.parse(session);
}

// ---- Obtener datos del usuario activo ----
function getUser() {
    const session = localStorage.getItem('emplanorte_session');
    return session ? JSON.parse(session) : null;
}

// ---- Cerrar sesión ----
function logout() {
    localStorage.removeItem('emplanorte_session');
    window.location.href = '../index.html';
}

// ---- Formatear moneda COP ----
function formatCurrency(value) {
    const num = Number(value) || 0;
    return '$ ' + num.toLocaleString('es-CO', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

// ---- Formatear fecha legible ----
function formatDate(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });
}

function formatDateTime(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' }) +
           ' ' + d.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
}

// ---- Fecha de hoy en formato YYYY-MM-DD ----
function todayISO() {
    return new Date().toISOString().split('T')[0];
}

// ---- Toast Notifications ----
function showToast(message, type = 'success') {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(30px)';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

// ---- Iniciales del nombre ----
function getInitials(name) {
    if (!name) return '?';
    return name.split(' ').map(w => w[0]).join('').substring(0, 2).toUpperCase();
}

// ---- Avatar: guardar y leer foto de perfil ----
function getAvatarKey(user) {
    return 'emplanorte_avatar_' + (user ? (user.id || user.idUsuario || 'default') : 'default');
}

function getSavedAvatar(user) {
    return localStorage.getItem(getAvatarKey(user));
}

function buildAvatarInner(user) {
    const saved = getSavedAvatar(user);
    if (saved) {
        return `<img src="${saved}" alt="foto"><div class="avatar-overlay">📷</div>`;
    }
    const initials = user ? getInitials(user.nombre) : '?';
    return `<span class="avatar-initials">${initials}</span><div class="avatar-overlay">📷</div>`;
}

// ---- Render Sidebar (inyectar HTML) ----
function renderSidebar(activePage) {
    const user = getUser();
    const userName = user ? user.nombre : 'Usuario';
    const userRole = user ? (user.rol === 'superadmin' ? 'Super Admin' : 'Administrador') : '';

    const sidebarHTML = `
    <!-- Sidebar Toggle (mobile) -->
    <button class="sidebar-toggle" id="sidebarToggle" aria-label="Abrir menú">☰</button>
    <div class="sidebar-overlay" id="sidebarOverlay"></div>
    <input type="file" id="avatarFileInput" accept="image/*" style="display:none">

    <aside class="sidebar" id="sidebar">
        <!-- Brand -->
        <div class="sidebar-brand">
            <img src="../img/Circulo.png" alt="EMPLANORTE" class="brand-icon">
            <span class="brand-name">EMPLANORTE</span>
        </div>

        <!-- User -->
        <div class="sidebar-user">
            <div class="user-avatar" id="userAvatar" title="Cambiar foto de perfil">
                ${buildAvatarInner(user)}
            </div>
            <div class="user-info">
                <div class="user-name">${userName}</div>
                <div class="user-role">${userRole}</div>
            </div>
        </div>

        <!-- Navigation -->
        <nav class="sidebar-nav">
            <div class="nav-section">
                <div class="nav-section-title">Gestiona tu Negocio</div>
                <a href="dashboard.html" class="nav-item ${activePage === 'dashboard' ? 'active' : ''}">
                    <span class="nav-icon">📊</span> Dashboard
                </a>
                <a href="ventas.html" class="nav-item ${activePage === 'ventas' ? 'active' : ''}">
                    <span class="nav-icon">💰</span> Ventas
                </a>
                <a href="gastos.html" class="nav-item ${activePage === 'gastos' ? 'active' : ''}">
                    <span class="nav-icon">📋</span> Gastos
                </a>
                <a href="inventario.html" class="nav-item ${activePage === 'inventario' ? 'active' : ''}">
                    <span class="nav-icon">📦</span> Inventario
                </a>
                <a href="cotizaciones.html" class="nav-item ${activePage === 'cotizaciones' ? 'active' : ''}">
                    <span class="nav-icon">📄</span> Cotizaciones
                </a>
            </div>

            <div class="nav-section">
                <div class="nav-section-title">Gestiona tus Contactos</div>
                <a href="clientes.html" class="nav-item ${activePage === 'clientes' ? 'active' : ''}">
                    <span class="nav-icon">👤</span> Clientes
                </a>
            </div>
        </nav>

        <!-- Footer -->
        <div class="sidebar-footer">
            <button class="btn-logout" id="btnLogout">
                <span>🚪</span> Cerrar Sesión
            </button>
            <div class="sidebar-version">v1.0.0</div>
        </div>
    </aside>
    `;

    // Insertar al inicio del body
    document.body.insertAdjacentHTML('afterbegin', sidebarHTML);

    // Event listeners
    document.getElementById('btnLogout').addEventListener('click', logout);

    // Mobile sidebar toggle
    const toggle = document.getElementById('sidebarToggle');
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');

    toggle.addEventListener('click', () => {
        sidebar.classList.toggle('open');
        overlay.classList.toggle('show');
    });

    overlay.addEventListener('click', () => {
        sidebar.classList.remove('open');
        overlay.classList.remove('show');
    });

    // Avatar: abrir selector de archivo al hacer clic
    const avatarEl = document.getElementById('userAvatar');
    const fileInput = document.getElementById('avatarFileInput');

    avatarEl.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', () => {
        const file = fileInput.files[0];
        if (!file) return;

        const img = new Image();
        const objectUrl = URL.createObjectURL(file);
        img.onload = () => {
            const SIZE = 80;
            const canvas = document.createElement('canvas');
            canvas.width = SIZE;
            canvas.height = SIZE;
            const ctx = canvas.getContext('2d');

            // Recorte centrado (crop cuadrado)
            const side = Math.min(img.naturalWidth, img.naturalHeight);
            const sx = (img.naturalWidth - side) / 2;
            const sy = (img.naturalHeight - side) / 2;
            ctx.drawImage(img, sx, sy, side, side, 0, 0, SIZE, SIZE);

            URL.revokeObjectURL(objectUrl);
            const compressed = canvas.toDataURL('image/jpeg', 0.8);
            localStorage.setItem(getAvatarKey(user), compressed);
            avatarEl.innerHTML = `<img src="${compressed}" alt="foto"><div class="avatar-overlay">📷</div>`;
        };
        img.src = objectUrl;
        fileInput.value = '';
    });
}

// ---- Abrir / Cerrar Modal ----
function openModal(modalId) {
    document.getElementById(modalId).classList.add('show');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

// ---- Limpiar formulario ----
function resetForm(formId) {
    document.getElementById(formId).reset();
}
