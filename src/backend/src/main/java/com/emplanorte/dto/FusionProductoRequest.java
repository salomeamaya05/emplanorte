package com.emplanorte.dto;

public class FusionProductoRequest {

    private Long productoDestinoId;
    private Long productoDuplicadoId;

    public Long getProductoDestinoId() {
        return productoDestinoId;
    }

    public void setProductoDestinoId(Long productoDestinoId) {
        this.productoDestinoId = productoDestinoId;
    }

    public Long getProductoDuplicadoId() {
        return productoDuplicadoId;
    }

    public void setProductoDuplicadoId(Long productoDuplicadoId) {
        this.productoDuplicadoId = productoDuplicadoId;
    }
}
