package com.eventos.inventario.foto;

import com.eventos.inventario.evento.Evento;
import com.eventos.inventario.producto.Producto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "foto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Foto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MomentoFoto momento;

    @Column(nullable = false, length = 500)
    private String ruta;

    /** Lo declara el celular: cuándo se tomó la foto. */
    @Column(name = "tomada_en", nullable = false)
    private Instant tomadaEn;

    /** Lo pone el servidor: cuándo llegó. Es la marca de tiempo confiable. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    Foto(Evento evento, Producto producto, MomentoFoto momento, String ruta, Instant tomadaEn) {
        this.evento = evento;
        this.producto = producto;
        this.momento = momento;
        this.ruta = ruta;
        this.tomadaEn = tomadaEn;
    }

    @PrePersist
    void alCrear() {
        this.createdAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Foto otra)) return false;
        return id != null && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return Foto.class.hashCode();
    }
}