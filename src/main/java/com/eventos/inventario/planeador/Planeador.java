package com.eventos.inventario.planeador;

import com.eventos.inventario.compartido.EntidadBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "planeador")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Planeador extends EntidadBase {

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    @Column(columnDefinition = "text")
    private String notas;

    public Planeador(String nombre) {
        this.nombre = nombre;
    }
}