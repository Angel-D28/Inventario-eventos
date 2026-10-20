package com.eventos.inventario.producto;

import com.eventos.inventario.compartido.EntidadBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Producto extends EntidadBase {

    @Column(nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoProducto tipo;

    @Column(name = "foto_referencia", length = 500)
    private String fotoReferencia;

    @Column(nullable = false)
    private boolean activo = true;

    public Producto(String nombre, TipoProducto tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }
}