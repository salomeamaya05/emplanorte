/* ============================================================
   COTIZACIÓN COMERCIAL EMPLANORTE
   Vista limpia para cliente, descarga PNG, compartir y PDF.
   No incluye stock, costos internos ni controles del formulario.
   ============================================================ */
const QuoteExport = (() => {
    let current = null;
    const COLORS = {
        navy: '#0b2b83', green: '#98bd24', dark: '#203047', muted: '#718096',
        light: '#f4f7fa', line: '#dfe7ef', white: '#ffffff'
    };

    const money = value => formatCurrency(Number(value) || 0);
    const safe = value => escapeSearchableHtml(value == null ? '' : value);

    function normalize(quote, details) {
        const subtotal = Number(quote.subtotal) || details.reduce((sum, d) => sum + (Number(d.subtotalLinea) || (Number(d.cantidad) || 0) * (Number(d.precioUnitario) || 0)), 0);
        const total = Number(quote.total) || 0;
        const discount = Math.max(0, Number(quote.descuento) || Math.max(0, subtotal - total));
        return {
            id: quote.id,
            number: quote.numeroCotizacion || `COT-${String(quote.id || '').padStart(6, '0')}`,
            date: quote.fechaCotizacion || new Date().toISOString(),
            customer: {
                name: quote.cliente?.nombre || 'Cliente',
                phone: quote.cliente?.telefono || '',
                address: quote.cliente?.direccion || ''
            },
            notes: quote.notas || '',
            subtotal,
            discount,
            total: total || Math.max(0, subtotal - discount),
            items: (details || []).map(d => ({
                name: d.producto?.nombre || 'Producto',
                code: d.producto?.codigo || '',
                quantity: Number(d.cantidad) || 0,
                unitPrice: Number(d.precioUnitario) || 0,
                subtotal: Number(d.subtotalLinea) || (Number(d.cantidad) || 0) * (Number(d.precioUnitario) || 0)
            }))
        };
    }

    function ensureModal() {
        let modal = document.getElementById('quoteShareModal');
        if (modal) return modal;
        modal = document.createElement('div');
        modal.id = 'quoteShareModal';
        modal.className = 'quote-share-modal';
        modal.innerHTML = `
            <div class="quote-share-card" role="dialog" aria-modal="true" aria-labelledby="quoteShareTitle">
                <div class="quote-share-head">
                    <h3 id="quoteShareTitle">Cotización lista para el cliente</h3>
                    <button type="button" class="quote-share-close" onclick="QuoteExport.close()">×</button>
                </div>
                <div class="quote-share-body"><div id="quoteClientPreview"></div></div>
                <div class="quote-share-actions">
                    <button type="button" class="btn-app btn-app-outline" onclick="QuoteExport.downloadPdf()">📄 Descargar PDF</button>
                    <button type="button" class="btn-app btn-app-outline" onclick="QuoteExport.downloadPng()">🖼 Descargar imagen</button>
                    <button type="button" class="btn-app btn-app-success" onclick="QuoteExport.sharePng()">📲 Compartir imagen</button>
                </div>
            </div>`;
        modal.addEventListener('click', event => {
            if (event.target === modal) close();
        });
        document.body.appendChild(modal);
        return modal;
    }

    function previewHtml(vm) {
        const customerMeta = [vm.customer.phone, vm.customer.address].filter(Boolean).join(' · ');
        const rows = vm.items.map(item => `
            <tr>
                <td><span class="quote-doc-product">${safe(item.name)}</span>${item.code ? `<span class="quote-doc-code">Código: ${safe(item.code)}</span>` : ''}</td>
                <td>${item.quantity.toLocaleString('es-CO')}</td>
                <td>${money(item.unitPrice)}</td>
                <td>${money(item.subtotal)}</td>
            </tr>`).join('');
        return `
            <article class="quote-client-document">
                <div class="quote-doc-band"></div>
                <div class="quote-doc-content">
                    <header class="quote-doc-header">
                        <div class="quote-doc-brand"><img src="../img/Logo.png" alt="EMPLANORTE"></div>
                        <div class="quote-doc-title">
                            <h2>COTIZACIÓN</h2>
                            <strong>${safe(vm.number)}</strong>
                            <span>${formatDate(vm.date)}</span>
                        </div>
                    </header>
                    <section class="quote-doc-customer">
                        <small>Preparada para</small>
                        <strong>${safe(vm.customer.name)}</strong>
                        ${customerMeta ? `<span>${safe(customerMeta)}</span>` : ''}
                    </section>
                    <table class="quote-doc-table">
                        <thead><tr><th>Producto</th><th>Cantidad</th><th>Precio unit.</th><th>Subtotal</th></tr></thead>
                        <tbody>${rows}</tbody>
                    </table>
                    <section class="quote-doc-totals">
                        <div class="quote-doc-total-row"><span>Subtotal</span><strong>${money(vm.subtotal)}</strong></div>
                        ${vm.discount > 0 ? `<div class="quote-doc-total-row"><span>Descuento</span><strong>− ${money(vm.discount)}</strong></div>` : ''}
                        <div class="quote-doc-total-row final"><span>Total</span><span>${money(vm.total)}</span></div>
                    </section>
                    ${vm.notes ? `<section class="quote-doc-notes"><strong>Observaciones:</strong> ${safe(vm.notes)}</section>` : ''}
                    <footer class="quote-doc-footer">
                        <span>Valores expresados en pesos colombianos. Cotización sujeta a disponibilidad al momento de confirmar el pedido.</span>
                        <span class="quote-doc-thanks">Gracias por confiar en EMPLANORTE.</span>
                    </footer>
                </div>
            </article>`;
    }

    async function open(quote, details) {
        current = normalize(quote, details);
        const modal = ensureModal();
        document.getElementById('quoteClientPreview').innerHTML = previewHtml(current);
        modal.classList.add('show');
    }

    function close() {
        document.getElementById('quoteShareModal')?.classList.remove('show');
    }

    function loadImage(src) {
        return new Promise((resolve, reject) => {
            const img = new Image();
            img.onload = () => resolve(img);
            img.onerror = reject;
            img.src = src;
        });
    }

    function roundRect(ctx, x, y, w, h, r, fill, stroke) {
        const radius = Math.min(r, w / 2, h / 2);
        ctx.beginPath();
        ctx.moveTo(x + radius, y);
        ctx.arcTo(x + w, y, x + w, y + h, radius);
        ctx.arcTo(x + w, y + h, x, y + h, radius);
        ctx.arcTo(x, y + h, x, y, radius);
        ctx.arcTo(x, y, x + w, y, radius);
        ctx.closePath();
        if (fill) { ctx.fillStyle = fill; ctx.fill(); }
        if (stroke) { ctx.strokeStyle = stroke; ctx.stroke(); }
    }

    function truncate(ctx, text, maxWidth) {
        let value = String(text || '');
        if (ctx.measureText(value).width <= maxWidth) return value;
        while (value.length > 1 && ctx.measureText(value + '…').width > maxWidth) value = value.slice(0, -1);
        return value + '…';
    }

    function wrapText(ctx, text, maxWidth) {
        const words = String(text || '').split(/\s+/).filter(Boolean);
        const lines = [];
        let line = '';
        words.forEach(word => {
            const test = line ? `${line} ${word}` : word;
            if (ctx.measureText(test).width > maxWidth && line) {
                lines.push(line);
                line = word;
            } else line = test;
        });
        if (line) lines.push(line);
        return lines;
    }

    async function buildCanvas() {
        if (!current) throw new Error('No hay una cotización seleccionada.');
        const vm = current;
        const width = 1200;
        const margin = 72;
        const itemHeight = 64;
        const notesHeight = vm.notes ? Math.max(86, 34 + Math.ceil(vm.notes.length / 95) * 28) : 0;
        const height = 330 + 58 + vm.items.length * itemHeight + 205 + notesHeight + 130;
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.fillStyle = COLORS.white;
        ctx.fillRect(0, 0, width, height);
        ctx.fillStyle = COLORS.navy;
        ctx.fillRect(0, 0, width * .7, 15);
        ctx.fillStyle = COLORS.green;
        ctx.fillRect(width * .7, 0, width * .3, 15);

        let logo = null;
        try { logo = await loadImage('../img/Logo.png'); } catch (error) { console.warn('No se pudo cargar el logo:', error); }
        if (logo) {
            const boxW = 300, boxH = 170;
            const ratio = Math.min(boxW / logo.naturalWidth, boxH / logo.naturalHeight);
            ctx.drawImage(logo, margin, 50, logo.naturalWidth * ratio, logo.naturalHeight * ratio);
        } else {
            ctx.fillStyle = COLORS.navy; ctx.font = '700 44px Arial'; ctx.fillText('EMPLANORTE', margin, 120);
        }

        ctx.textAlign = 'right';
        ctx.fillStyle = COLORS.navy; ctx.font = '800 45px Arial'; ctx.fillText('COTIZACIÓN', width - margin, 85);
        ctx.fillStyle = COLORS.dark; ctx.font = '700 24px Arial'; ctx.fillText(vm.number, width - margin, 125);
        ctx.fillStyle = COLORS.muted; ctx.font = '400 18px Arial'; ctx.fillText(formatDate(vm.date), width - margin, 158);
        ctx.textAlign = 'left';

        const customerY = 220;
        roundRect(ctx, margin, customerY, width - margin * 2, 108, 12, COLORS.light, null);
        ctx.fillStyle = COLORS.green; ctx.fillRect(margin, customerY, 7, 108);
        ctx.fillStyle = COLORS.muted; ctx.font = '700 15px Arial'; ctx.fillText('PREPARADA PARA', margin + 28, customerY + 31);
        ctx.fillStyle = COLORS.dark; ctx.font = '700 27px Arial'; ctx.fillText(truncate(ctx, vm.customer.name, 760), margin + 28, customerY + 67);
        const meta = [vm.customer.phone, vm.customer.address].filter(Boolean).join(' · ');
        if (meta) { ctx.fillStyle = COLORS.muted; ctx.font = '400 16px Arial'; ctx.fillText(truncate(ctx, meta, 950), margin + 28, customerY + 92); }

        let y = 362;
        const cols = [margin, 690, 820, 1000, width - margin];
        ctx.fillStyle = COLORS.navy; ctx.fillRect(margin, y, width - margin * 2, 50);
        ctx.fillStyle = COLORS.white; ctx.font = '700 15px Arial';
        ctx.textAlign = 'left'; ctx.fillText('PRODUCTO', cols[0] + 16, y + 31);
        ctx.textAlign = 'right'; ctx.fillText('CANTIDAD', cols[2] - 16, y + 31); ctx.fillText('PRECIO UNIT.', cols[3] - 16, y + 31); ctx.fillText('SUBTOTAL', cols[4] - 16, y + 31);
        y += 50;

        vm.items.forEach((item, index) => {
            if (index % 2 === 1) { ctx.fillStyle = '#fafbfd'; ctx.fillRect(margin, y, width - margin * 2, itemHeight); }
            ctx.fillStyle = COLORS.dark; ctx.font = '650 18px Arial'; ctx.textAlign = 'left';
            ctx.fillText(truncate(ctx, item.name, cols[1] - cols[0] - 35), cols[0] + 16, y + 27);
            if (item.code) { ctx.fillStyle = COLORS.muted; ctx.font = '400 13px Arial'; ctx.fillText(`Código: ${truncate(ctx, item.code, 300)}`, cols[0] + 16, y + 49); }
            ctx.fillStyle = COLORS.dark; ctx.font = '500 17px Arial'; ctx.textAlign = 'right';
            ctx.fillText(item.quantity.toLocaleString('es-CO'), cols[2] - 16, y + 35);
            ctx.fillText(money(item.unitPrice), cols[3] - 16, y + 35);
            ctx.font = '700 17px Arial'; ctx.fillText(money(item.subtotal), cols[4] - 16, y + 35);
            ctx.strokeStyle = COLORS.line; ctx.beginPath(); ctx.moveTo(margin, y + itemHeight); ctx.lineTo(width - margin, y + itemHeight); ctx.stroke();
            y += itemHeight;
        });

        const totalsX = 710;
        y += 25;
        ctx.textAlign = 'left'; ctx.font = '500 18px Arial'; ctx.fillStyle = COLORS.muted; ctx.fillText('Subtotal', totalsX, y + 25);
        ctx.textAlign = 'right'; ctx.fillStyle = COLORS.dark; ctx.font = '700 18px Arial'; ctx.fillText(money(vm.subtotal), width - margin, y + 25);
        if (vm.discount > 0) {
            y += 42; ctx.textAlign = 'left'; ctx.fillStyle = COLORS.muted; ctx.font = '500 18px Arial'; ctx.fillText('Descuento', totalsX, y + 25);
            ctx.textAlign = 'right'; ctx.fillStyle = COLORS.dark; ctx.font = '700 18px Arial'; ctx.fillText(`− ${money(vm.discount)}`, width - margin, y + 25);
        }
        y += 55; ctx.strokeStyle = COLORS.green; ctx.lineWidth = 4; ctx.beginPath(); ctx.moveTo(totalsX, y); ctx.lineTo(width - margin, y); ctx.stroke();
        y += 43; ctx.textAlign = 'left'; ctx.fillStyle = COLORS.navy; ctx.font = '800 28px Arial'; ctx.fillText('TOTAL', totalsX, y);
        ctx.textAlign = 'right'; ctx.font = '800 31px Arial'; ctx.fillText(money(vm.total), width - margin, y);
        ctx.lineWidth = 1;

        if (vm.notes) {
            y += 38;
            ctx.font = '400 16px Arial';
            const lines = wrapText(ctx, vm.notes, width - margin * 2 - 40);
            const boxH = 45 + lines.length * 24;
            roundRect(ctx, margin, y, width - margin * 2, boxH, 10, '#f9fbfd', COLORS.line);
            ctx.fillStyle = COLORS.dark; ctx.font = '700 16px Arial'; ctx.textAlign = 'left'; ctx.fillText('Observaciones', margin + 20, y + 27);
            ctx.fillStyle = COLORS.muted; ctx.font = '400 16px Arial';
            lines.forEach((line, index) => ctx.fillText(line, margin + 20, y + 55 + index * 24));
            y += boxH;
        }

        y += 48; ctx.strokeStyle = COLORS.line; ctx.beginPath(); ctx.moveTo(margin, y); ctx.lineTo(width - margin, y); ctx.stroke();
        y += 38; ctx.fillStyle = COLORS.muted; ctx.font = '400 14px Arial'; ctx.textAlign = 'left';
        const footerLines = wrapText(ctx, 'Valores expresados en pesos colombianos. Cotización sujeta a disponibilidad al momento de confirmar el pedido.', 690);
        footerLines.forEach((line, index) => ctx.fillText(line, margin, y + index * 20));
        ctx.fillStyle = COLORS.navy; ctx.font = '700 16px Arial'; ctx.textAlign = 'right'; ctx.fillText('Gracias por confiar en EMPLANORTE.', width - margin, y + 5);
        return canvas;
    }

    function filename(extension) {
        const clean = String(current?.number || 'cotizacion').replace(/[^a-z0-9_-]+/gi, '_');
        return `${clean}_EMPLANORTE.${extension}`;
    }

    async function canvasBlob() {
        const canvas = await buildCanvas();
        return new Promise(resolve => canvas.toBlob(resolve, 'image/png', 1));
    }

    async function downloadPng() {
        try {
            const blob = await canvasBlob();
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url; link.download = filename('png'); link.click();
            setTimeout(() => URL.revokeObjectURL(url), 1500);
            showToast('Imagen de la cotización descargada', 'success');
        } catch (error) { console.error(error); showToast('No se pudo generar la imagen', 'error'); }
    }

    async function sharePng() {
        try {
            const blob = await canvasBlob();
            const file = new File([blob], filename('png'), { type: 'image/png' });
            if (navigator.canShare && navigator.canShare({ files: [file] }) && navigator.share) {
                await navigator.share({
                    files: [file],
                    title: `Cotización ${current.number} - EMPLANORTE`,
                    text: `Cotización ${current.number} preparada por EMPLANORTE.`
                });
            } else {
                await downloadPng();
                showToast('La imagen se descargó. Puede enviarla por WhatsApp o correo.', 'info');
            }
        } catch (error) {
            if (error?.name !== 'AbortError') { console.error(error); showToast('No se pudo compartir la cotización', 'error'); }
        }
    }

    async function downloadPdf() {
        try {
            const canvas = await buildCanvas();
            const jsPDF = window.jspdf?.jsPDF;
            if (!jsPDF) throw new Error('jsPDF no está disponible');
            const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
            const pageW = pdf.internal.pageSize.getWidth();
            const pageH = pdf.internal.pageSize.getHeight();
            const imgW = pageW - 14;
            const imgH = canvas.height * imgW / canvas.width;
            if (imgH <= pageH - 14) {
                pdf.addImage(canvas.toDataURL('image/png'), 'PNG', 7, 7, imgW, imgH, undefined, 'FAST');
            } else {
                const scale = canvas.width / imgW;
                const sliceHeightPx = Math.floor((pageH - 14) * scale);
                let sourceY = 0;
                let page = 0;
                while (sourceY < canvas.height) {
                    const h = Math.min(sliceHeightPx, canvas.height - sourceY);
                    const part = document.createElement('canvas');
                    part.width = canvas.width; part.height = h;
                    part.getContext('2d').drawImage(canvas, 0, sourceY, canvas.width, h, 0, 0, canvas.width, h);
                    if (page > 0) pdf.addPage();
                    pdf.addImage(part.toDataURL('image/png'), 'PNG', 7, 7, imgW, h / scale, undefined, 'FAST');
                    sourceY += h; page++;
                }
            }
            pdf.save(filename('pdf'));
            showToast('PDF de la cotización descargado', 'success');
        } catch (error) { console.error(error); showToast('No se pudo generar el PDF', 'error'); }
    }

    return { open, close, downloadPng, sharePng, downloadPdf };
})();
