package com.emplanorte.service;

import com.emplanorte.model.CategoriaProducto;
import com.emplanorte.repository.CategoriaProductoRepository;
import com.emplanorte.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaProductoServiceTest {

    @Mock private CategoriaProductoRepository categoriaRepository;
    @Mock private ProductoRepository productoRepository;

    private CategoriaProductoService service;
    private CategoriaProducto categoria;

    @BeforeEach
    void setUp() {
        service = new CategoriaProductoService(categoriaRepository, productoRepository);
        categoria = new CategoriaProducto(1L, "Envases PET", "Envases plásticos", true);
    }

    @Test
    void crearNormalizaLosDatosYActivaLaCategoria() {
        CategoriaProducto nueva = new CategoriaProducto(null, "  Tapas   plásticas  ", "  Por color  ", false);
        when(categoriaRepository.findByNombreIgnoreCase("Tapas plásticas")).thenReturn(Optional.empty());
        when(categoriaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        CategoriaProducto resultado = service.crear(nueva);

        assertThat(resultado.getNombre()).isEqualTo("Tapas plásticas");
        assertThat(resultado.getDescripcion()).isEqualTo("Por color");
        assertThat(resultado.getActivo()).isTrue();
    }

    @Test
    void crearConNombreDeCategoriaInactivaLaReactiva() {
        categoria.setActivo(false);
        when(categoriaRepository.findByNombreIgnoreCase("Envases PET")).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        CategoriaProducto resultado = service.crear(
                new CategoriaProducto(null, "Envases PET", "Nueva descripción", true)
        );

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getActivo()).isTrue();
        assertThat(resultado.getDescripcion()).isEqualTo("Nueva descripción");
    }

    @Test
    void actualizarRechazaUnNombreQuePerteneceAOtraCategoria() {
        CategoriaProducto otra = new CategoriaProducto(2L, "Tapas", null, true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.findByNombreIgnoreCase("Tapas")).thenReturn(Optional.of(otra));

        assertThatThrownBy(() -> service.actualizar(1L, new CategoriaProducto(null, "Tapas", null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe");
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void eliminarCategoriaSinProductosActivosLaDesactiva() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.existsByCategoriaIdAndActivoTrue(1L)).thenReturn(false);

        service.eliminar(1L);

        assertThat(categoria.getActivo()).isFalse();
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void eliminarCategoriaConProductosActivosExplicaComoResolverlo() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.existsByCategoriaIdAndActivoTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reasigne esos productos");
        verify(categoriaRepository, never()).save(any());
    }
}
