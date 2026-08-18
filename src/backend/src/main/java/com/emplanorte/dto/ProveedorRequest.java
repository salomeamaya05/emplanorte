package com.emplanorte.dto;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class ProveedorRequest {
    private String nitDocumento;
    private String razonSocial;
    private String contactoNombre;
    private String telefono;
    private String correo;
    private String direccion;
    private String ciudad;
    private String condicionesPago;
    private String observaciones;
    private Boolean activo;
}
