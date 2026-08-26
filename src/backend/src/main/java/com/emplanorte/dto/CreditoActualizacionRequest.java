package com.emplanorte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditoActualizacionRequest {
    private LocalDate fechaVencimiento;
    private String observaciones;
    private Long idUsuario;
    private String contrasena;
}
