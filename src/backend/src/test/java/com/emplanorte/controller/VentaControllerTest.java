package com.emplanorte.controller;

import com.emplanorte.dto.ItemVentaRequest;
import com.emplanorte.dto.VentaRequest;
import com.emplanorte.model.Venta;
import com.emplanorte.service.VentaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.emplanorte.config.SecurityConfig;

@WebMvcTest(VentaController.class)
@Import(SecurityConfig.class)
@DisplayName("VentaController — Integración HTTP /api/ventas")
class VentaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private VentaService ventaService;

    private Venta ventaMock;
    private VentaRequest requestValido;

    @BeforeEach
    void setUp() {
        ventaMock = new Venta();
        ventaMock.setId(1L);
        ventaMock.setNumeroVenta("VTA-000001");
        ventaMock.setFechaVenta(LocalDateTime.now());
        ventaMock.setMetodoPago("efectivo");
        ventaMock.setEstado("completada");
        ventaMock.setSubtotal(new BigDecimal("4500"));
        ventaMock.setDescuento(BigDecimal.ZERO);
        ventaMock.setTotal(new BigDecimal("4500"));
        ventaMock.setTotalCosto(new BigDecimal("3000"));
        ventaMock.setGanancia(new BigDecimal("1500"));

        requestValido = new VentaRequest();
        requestValido.setIdUsuario(1L);
        requestValido.setMetodoPago("efectivo");
        requestValido.setDetalles(List.of(new ItemVentaRequest(1L, 3)));
    }

    // ─── GET /api/ventas ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/ventas — HTTP 200 con lista de ventas")
    void listarVentas_retorna200ConLista() throws Exception {
        when(ventaService.obtenerTodas()).thenReturn(List.of(ventaMock));

        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroVenta").value("VTA-000001"))
                .andExpect(jsonPath("$[0].estado").value("completada"));
    }

    @Test
    @DisplayName("GET /api/ventas sin registros — HTTP 200 con lista vacía")
    void listarVentas_sinRegistros_retorna200ListaVacia() throws Exception {
        when(ventaService.obtenerTodas()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── POST /api/ventas ─────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-13: POST /api/ventas válida — HTTP 201 con venta creada")
    void registrarVenta_exitosa_retorna201ConVenta() throws Exception {
        when(ventaService.registrarVenta(any(VentaRequest.class))).thenReturn(ventaMock);

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroVenta").value("VTA-000001"))
                .andExpect(jsonPath("$.total").value(4500))
                .andExpect(jsonPath("$.estado").value("completada"));
    }

    @Test
    @DisplayName("CP-14: POST /api/ventas con stock insuficiente — HTTP 400 con mensaje")
    void registrarVenta_stockInsuficiente_retorna400() throws Exception {
        when(ventaService.registrarVenta(any(VentaRequest.class)))
                .thenThrow(new RuntimeException("Stock insuficiente para el producto: Botella PET 500ml"));

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Stock insuficiente")));
    }

    @Test
    @DisplayName("CP-15: POST /api/ventas sin productos — HTTP 400 con mensaje")
    void registrarVenta_sinProductos_retorna400() throws Exception {
        when(ventaService.registrarVenta(any(VentaRequest.class)))
                .thenThrow(new RuntimeException("Debe seleccionar al menos un producto"));

        VentaRequest sinDetalles = new VentaRequest();
        sinDetalles.setIdUsuario(1L);
        sinDetalles.setMetodoPago("efectivo");
        sinDetalles.setDetalles(Collections.emptyList());

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sinDetalles)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/ventas error genérico — HTTP 400 propaga el mensaje del servicio")
    void registrarVenta_errorGenerico_retorna400ConMensaje() throws Exception {
        when(ventaService.registrarVenta(any(VentaRequest.class)))
                .thenThrow(new RuntimeException("Usuario/Vendedor no encontrado"));

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Usuario")));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRUEBAS ADICIONALES (rol tester) — documentan cada respuesta del sistema
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST 201 — la respuesta expone TODOS los campos financieros de la venta")
    void registrarVenta_exitosa_exponeTodosLosCampos() throws Exception {
        when(ventaService.registrarVenta(any(VentaRequest.class))).thenReturn(ventaMock);

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroVenta").value("VTA-000001"))
                .andExpect(jsonPath("$.metodoPago").value("efectivo"))
                .andExpect(jsonPath("$.subtotal").value(4500))
                .andExpect(jsonPath("$.descuento").value(0))
                .andExpect(jsonPath("$.total").value(4500))
                .andExpect(jsonPath("$.totalCosto").value(3000))
                .andExpect(jsonPath("$.ganancia").value(1500));
    }

    @Test
    @DisplayName("POST cantidad cero — el servicio rechaza y el controlador responde 400 con mensaje")
    void registrarVenta_cantidadCero_retorna400() throws Exception {
        when(ventaService.registrarVenta(any(VentaRequest.class)))
                .thenThrow(new RuntimeException("La cantidad de cada producto debe ser mayor a cero"));

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mayor a cero")));
    }

    @Test
    @DisplayName("GET /api/ventas/{id}/detalles — HTTP 200 con la lista de detalles")
    void listarDetalles_exitoso_retorna200() throws Exception {
        when(ventaService.obtenerDetalles(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/ventas/1/detalles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/ventas/{id}/detalles de venta inexistente — HTTP 400 con mensaje")
    void listarDetalles_ventaNoExiste_retorna400() throws Exception {
        when(ventaService.obtenerDetalles(99L))
                .thenThrow(new RuntimeException("Venta no encontrada"));

        mockMvc.perform(get("/api/ventas/99/detalles"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Venta no encontrada")));
    }

    @Test
    @DisplayName("POST /api/ventas con JSON malformado — HTTP 400")
    void registrarVenta_jsonMalformado_retorna400() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\": 1, "))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/ventas con Content-Type text/plain — HTTP 415 Unsupported Media Type")
    void registrarVenta_contentTypeInvalido_retorna415() throws Exception {
        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("idUsuario=1"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("DELETE /api/ventas — HTTP 405 Method Not Allowed (no existe ese método)")
    void deleteVentas_retorna405() throws Exception {
        mockMvc.perform(delete("/api/ventas"))
                .andExpect(status().isMethodNotAllowed());
    }
}
