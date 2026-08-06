package com.emplanorte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnulacionRequest {
    private Long idUsuario;
    private String contrasena;
    private String motivo;
    private Boolean corregir;
}
