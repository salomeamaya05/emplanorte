/* ============================================================
   UTILIDADES COMPARTIDAS - EMPLANORTE S.A.S.
   Sidebar, navegación, sesión, toasts y helpers reutilizables
   ============================================================ */

// ---- Verificar sesión activa ----
function checkSession() {
    const auth = window.EmplanorteAuthSession;
    const session = auth ? auth.get() : null;
    if (!session) {
        if (auth) auth.redirectToLogin();
        else window.location.href = '../index.html';
        return null;
    }
    if (auth && auth.isExpired(session)) {
        auth.redirectToLogin(auth.EXPIRED_MESSAGE);
        return null;
    }
    return session;
}

// ---- Obtener datos del usuario activo ----
function getUser() {
    const auth = window.EmplanorteAuthSession;
    return auth ? auth.get() : null;
}

// ---- Cerrar sesión ----
function logout() {
    const auth = window.EmplanorteAuthSession;
    if (auth) {
        auth.clear();
        window.location.replace(`${window.location.origin}/index.html`);
    } else {
        localStorage.removeItem('emplanorte_session');
        window.location.href = '../index.html';
    }
}

// ---- Formatear moneda COP ----
function formatCurrency(value) {
    const num = Number(value) || 0;
    return '$ ' + num.toLocaleString('es-CO', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

// ---- Formatear fecha legible ----
function formatDate(dateStr) {
    if (!dateStr) return '-';
    // Una fecha sin hora representa un día del negocio, no medianoche UTC.
    // Agregar la hora local evita que 2026-09-25 se muestre como 24 en Colombia.
    const value = String(dateStr);
    const d = /^\d{4}-\d{2}-\d{2}$/.test(value)
        ? new Date(`${value}T00:00:00`)
        : new Date(value);
    if (Number.isNaN(d.getTime())) return '-';
    return d.toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });
}

function formatDateTime(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    if (Number.isNaN(d.getTime())) return '-';
    return d.toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' }) +
           ' ' + d.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
}

// ---- Fecha de hoy en formato YYYY-MM-DD ----
function todayISO() {
    const d = new Date();
    d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
    return d.toISOString().split('T')[0];
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
                <a href="cartera.html" class="nav-item ${activePage === 'cartera' ? 'active' : ''}">
                    <span class="nav-icon">💳</span> Cartera
                </a>
                <a href="compras.html" class="nav-item ${activePage === 'compras' ? 'active' : ''}">
                    <span class="nav-icon">🛒</span> Compras
                </a>
                <a href="facturas-proveedores.html" class="nav-item ${activePage === 'facturas' ? 'active' : ''}">
                    <span class="nav-icon">🧾</span> Facturas y Pagos
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
                <a href="proveedores.html" class="nav-item ${activePage === 'proveedores' ? 'active' : ''}">
                    <span class="nav-icon">🚚</span> Proveedores
                </a>
            </div>
        </nav>

        <!-- Footer -->
        <div class="sidebar-footer">
            <button class="btn-logout" id="btnLogout">
                <span>🚪</span> Cerrar Sesión
            </button>
            <div class="sidebar-version">v2.4.0</div>
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

// ============================================================
// SELECTOR BUSCABLE REUTILIZABLE - EMPLANORTE v2.2
// ============================================================
function normalizeSearchableText(value) {
    return String(value == null ? '' : value)
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .replace(/\s+/g, ' ')
        .trim();
}

function escapeSearchableHtml(value) {
    return String(value == null ? '' : value).replace(/[&<>'"]/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
    }[c]));
}

/**
 * Convierte un <select> existente en un buscador con autocompletado.
 * El select original sigue almacenando el ID y conserva compatibilidad
 * con el código existente. El componente solo cambia la interfaz.
 */

function ensureSearchableSelectStyles() {
    if (document.getElementById('emplanorte-searchable-select-styles')) return;
    const style = document.createElement('style');
    style.id = 'emplanorte-searchable-select-styles';
    style.textContent = `
      .searchable-select{position:relative;width:100%}
      .searchable-select-native{display:none!important}
      .searchable-select-control{position:relative;display:flex;align-items:center}
      .searchable-select-input{box-sizing:border-box;width:100%;min-height:38px;padding:8px 42px 8px 11px;border:1px solid #dce3eb;border-radius:8px;background:#fff;color:#1f2937;font:inherit;outline:none}
      .searchable-select-input:focus{border-color:#3182ce;box-shadow:0 0 0 3px rgba(49,130,206,.14)}
      .searchable-select-input.is-selected{background:#f5fbf7;border-color:#8dc9a1}
      .searchable-select-input.is-invalid{border-color:#dc3545;box-shadow:0 0 0 3px rgba(220,53,69,.10)}
      .searchable-select-clear{position:absolute;right:8px;width:27px;height:27px;border:0;border-radius:50%;background:transparent;color:#8492a6;font-size:18px;line-height:1;cursor:pointer;display:none;align-items:center;justify-content:center}
      .searchable-select.has-value .searchable-select-clear{display:flex}
      .searchable-select-results{position:absolute;z-index:10050;left:0;right:0;top:calc(100% + 5px);display:none;max-height:260px;overflow-y:auto;border:1px solid #d8e1eb;border-radius:10px;background:#fff;box-shadow:0 14px 32px rgba(26,45,68,.18);padding:5px}
      .searchable-select.open .searchable-select-results{display:block}
      .searchable-select-option{width:100%;border:0;border-radius:7px;background:transparent;color:#1f2937;text-align:left;padding:9px 10px;cursor:pointer;display:flex;flex-direction:column;gap:3px;font:inherit}
      .searchable-select-option:hover,.searchable-select-option.active{background:#edf6ff}
      .searchable-select-option-title{font-size:13px;font-weight:650;line-height:1.25}
      .searchable-select-option-meta{color:#6f8094;font-size:11px;line-height:1.3}
      .searchable-select-message{padding:10px;color:#7b8b9f;font-size:12px;text-align:center}
      .searchable-select-error{display:none;color:#dc3545;font-size:11px;margin-top:4px}
      .searchable-select.invalid .searchable-select-error{display:block}
    `;
    document.head.appendChild(style);
}

function enhanceSearchableSelect(selectOrId, config = {}) {
    ensureSearchableSelectStyles();
    const select = typeof selectOrId === 'string'
        ? document.getElementById(selectOrId)
        : selectOrId;
    if (!select) return null;

    if (select._searchableSelect) {
        select._searchableSelect.updateConfig(config);
        select._searchableSelect.refresh();
        return select._searchableSelect;
    }

    const originalRequired = select.required;
    select.required = false;
    select.classList.add('searchable-select-native');

    const wrapper = document.createElement('div');
    wrapper.className = 'searchable-select';
    const control = document.createElement('div');
    control.className = 'searchable-select-control';
    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'searchable-select-input';
    input.autocomplete = 'off';
    input.spellcheck = false;
    input.required = originalRequired;
    const clear = document.createElement('button');
    clear.type = 'button';
    clear.className = 'searchable-select-clear';
    clear.setAttribute('aria-label', 'Limpiar selección');
    clear.textContent = '×';
    const results = document.createElement('div');
    results.className = 'searchable-select-results';
    results.setAttribute('role', 'listbox');
    const error = document.createElement('div');
    error.className = 'searchable-select-error';
    error.textContent = 'Seleccione una opción válida.';

    select.parentNode.insertBefore(wrapper, select);
    wrapper.appendChild(select);
    wrapper.appendChild(control);
    control.appendChild(input);
    control.appendChild(clear);
    wrapper.appendChild(results);
    wrapper.appendChild(error);

    let cfg = {};
    let activeIndex = -1;
    let visibleOptions = [];
    let selectedLabel = '';

    function updateConfig(next = {}) {
        cfg = Object.assign({
            placeholder: 'Escriba para buscar...',
            minChars: 1,
            maxResults: 8,
            emptyMessage: 'No se encontraron resultados.',
            promptMessage: 'Escriba para buscar.',
            optionLabel: option => option.textContent.trim(),
            optionSearchText: option => `${option.textContent} ${option.dataset.search || ''}`,
            optionMeta: option => option.dataset.meta || '',
            onSelect: null,
            onClear: null
        }, cfg, next);
        input.placeholder = cfg.placeholder;
    }

    function allOptions() {
        return Array.from(select.options).filter(option => String(option.value || '').trim() !== '');
    }

    function getLabel(option) {
        return option ? String(cfg.optionLabel(option) || '').trim() : '';
    }

    function closeResults() {
        wrapper.classList.remove('open');
        activeIndex = -1;
    }

    function setInvalid(invalid) {
        wrapper.classList.toggle('invalid', Boolean(invalid));
        input.classList.toggle('is-invalid', Boolean(invalid));
        input.setCustomValidity(invalid ? 'Seleccione una opción válida.' : '');
    }

    function clearSelection({ dispatch = true, keepText = false } = {}) {
        select.value = '';
        selectedLabel = '';
        input.dataset.selectedValue = '';
        input.classList.remove('is-selected');
        wrapper.classList.remove('has-value');
        if (!keepText) input.value = '';
        setInvalid(false);
        if (dispatch) select.dispatchEvent(new Event('change', { bubbles: true }));
        if (typeof cfg.onClear === 'function') cfg.onClear(select);
    }

    function selectOption(option, { dispatch = true } = {}) {
        if (!option) return;
        select.value = option.value;
        selectedLabel = getLabel(option);
        input.value = selectedLabel;
        input.dataset.selectedValue = String(option.value);
        input.classList.add('is-selected');
        wrapper.classList.add('has-value');
        setInvalid(false);
        closeResults();
        if (dispatch) select.dispatchEvent(new Event('change', { bubbles: true }));
        if (typeof cfg.onSelect === 'function') cfg.onSelect(option, select);
    }

    function renderResults() {
        const query = normalizeSearchableText(input.value);
        activeIndex = -1;
        visibleOptions = [];
        results.innerHTML = '';

        if (query.length < Number(cfg.minChars || 0)) {
            results.innerHTML = `<div class="searchable-select-message">${escapeSearchableHtml(cfg.promptMessage)}</div>`;
            wrapper.classList.add('open');
            return;
        }

        visibleOptions = allOptions()
            .filter(option => normalizeSearchableText(cfg.optionSearchText(option)).includes(query))
            .slice(0, Number(cfg.maxResults || 8));

        if (!visibleOptions.length) {
            results.innerHTML = `<div class="searchable-select-message">${escapeSearchableHtml(cfg.emptyMessage)}</div>`;
            wrapper.classList.add('open');
            return;
        }

        visibleOptions.forEach((option, index) => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'searchable-select-option';
            button.dataset.index = String(index);
            const title = document.createElement('span');
            title.className = 'searchable-select-option-title';
            title.textContent = getLabel(option);
            button.appendChild(title);
            const metaText = String(cfg.optionMeta(option) || '').trim();
            if (metaText) {
                const meta = document.createElement('span');
                meta.className = 'searchable-select-option-meta';
                meta.textContent = metaText;
                button.appendChild(meta);
            }
            button.addEventListener('mousedown', event => event.preventDefault());
            button.addEventListener('click', () => selectOption(option));
            results.appendChild(button);
        });
        wrapper.classList.add('open');
    }

    function setActive(index) {
        const buttons = Array.from(results.querySelectorAll('.searchable-select-option'));
        buttons.forEach(btn => btn.classList.remove('active'));
        if (!buttons.length) return;
        activeIndex = (index + buttons.length) % buttons.length;
        buttons[activeIndex].classList.add('active');
        buttons[activeIndex].scrollIntoView({ block: 'nearest' });
    }

    function syncFromSelect() {
        const option = select.selectedOptions && select.selectedOptions[0];
        if (option && option.value) selectOption(option, { dispatch: false });
        else clearSelection({ dispatch: false });
    }

    function refresh() {
        const currentValue = select.value;
        if (currentValue) {
            const option = allOptions().find(item => String(item.value) === String(currentValue));
            if (option) selectOption(option, { dispatch: false });
            else clearSelection({ dispatch: false });
        } else if (input.value && !selectedLabel) {
            renderResults();
        }
    }

    input.addEventListener('focus', () => renderResults());
    input.addEventListener('input', () => {
        if (selectedLabel && input.value !== selectedLabel) {
            select.value = '';
            selectedLabel = '';
            input.dataset.selectedValue = '';
            input.classList.remove('is-selected');
            wrapper.classList.remove('has-value');
            select.dispatchEvent(new Event('change', { bubbles: true }));
        }
        setInvalid(Boolean(input.value.trim()) && !select.value);
        renderResults();
    });
    input.addEventListener('keydown', event => {
        if (event.key === 'ArrowDown') {
            event.preventDefault();
            if (!wrapper.classList.contains('open')) renderResults();
            setActive(activeIndex + 1);
        } else if (event.key === 'ArrowUp') {
            event.preventDefault();
            setActive(activeIndex - 1);
        } else if (event.key === 'Enter' && wrapper.classList.contains('open')) {
            const option = visibleOptions[activeIndex >= 0 ? activeIndex : 0];
            if (option) {
                event.preventDefault();
                selectOption(option);
            }
        } else if (event.key === 'Escape') {
            closeResults();
        }
    });
    input.addEventListener('blur', () => {
        setTimeout(() => {
            closeResults();
            if (input.value.trim() && !select.value) setInvalid(true);
        }, 120);
    });
    clear.addEventListener('click', () => {
        clearSelection();
        input.focus();
        renderResults();
    });
    select.addEventListener('change', () => {
        const option = select.selectedOptions && select.selectedOptions[0];
        if (option && option.value) {
            selectedLabel = getLabel(option);
            input.value = selectedLabel;
            input.dataset.selectedValue = String(option.value);
            input.classList.add('is-selected');
            wrapper.classList.add('has-value');
            setInvalid(false);
        } else if (!document.activeElement || document.activeElement !== input) {
            clearSelection({ dispatch: false });
        }
    });
    document.addEventListener('click', event => {
        if (!wrapper.contains(event.target)) closeResults();
    });

    updateConfig(config);
    const api = {
        select,
        input,
        wrapper,
        refresh,
        syncFromSelect,
        clear: clearSelection,
        setValue(value, options = {}) {
            select.value = value == null ? '' : String(value);
            syncFromSelect();
            if (options.dispatch) select.dispatchEvent(new Event('change', { bubbles: true }));
        },
        updateConfig
    };
    select._searchableSelect = api;
    syncFromSelect();
    return api;
}

function syncSearchableSelect(selectOrId) {
    const select = typeof selectOrId === 'string' ? document.getElementById(selectOrId) : selectOrId;
    if (select && select._searchableSelect) select._searchableSelect.syncFromSelect();
}
