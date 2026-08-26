CREATE ALIAS IF NOT EXISTS fn_generar_numero_compra AS $$
String generarNumeroCompra() {
    return "COMP-E2E-" + java.util.UUID.randomUUID().toString().substring(0, 8);
}
$$^^^

CREATE ALIAS IF NOT EXISTS fn_generar_numero_venta AS $$
String generarNumeroVenta() {
    return "VENT-E2E-" + java.util.UUID.randomUUID().toString().substring(0, 8);
}
$$^^^

CREATE ALIAS IF NOT EXISTS fn_generar_numero_cotizacion AS $$
String generarNumeroCotizacion() {
    return "COT-E2E-" + java.util.UUID.randomUUID().toString().substring(0, 8);
}
$$^^^
