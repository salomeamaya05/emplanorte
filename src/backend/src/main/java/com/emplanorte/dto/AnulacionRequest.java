package com.emplanorte.dto;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class AnulacionRequest {
    private Long idUsuario;
    private String contrasena;
    private String motivo;
}
