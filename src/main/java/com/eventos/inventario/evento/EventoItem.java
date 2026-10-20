package com.eventos.inventario.evento;

import com.eventos.inventario.producto.Producto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "evento_item")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id")
    @Setter(AccessLevel.NONE)
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    @Setter(AccessLevel.NONE)
    private Producto producto;

    @Column(name = "cantidad_recibida", nullable = false)
    private int cantidadRecibida;

    @Column(name = "cantidad_devuelta", nullable = false)
    private int cantidadDevuelta;

    @Column(name = "cantidad_rota", nullable = false)
    private int cantidadRota;

    @Column(name = "cantidad_perdida", nullable = false)
    private int cantidadPerdida;

    @Column(name = "cantidad_consumida", nullable = false)
    private int cantidadConsumida;

    @Column(name = "observaciones_recepcion", columnDefinition = "text")
    private String observacionesRecepcion;

    EventoItem(Evento evento, Producto producto) {
        this.evento = evento;
        this.producto = producto;
    }

    /** Lo que ya está justificado: devuelto + roto + perdido + consumido. */
    public int cantidadContabilizada() {
        return cantidadDevuelta + cantidadRota + cantidadPerdida + cantidadConsumida;
    }

    /** Cero significa que el ítem cuadra. */
    public int diferencia() {
        return cantidadRecibida - cantidadContabilizada();
    }

    /** Reemplaza las cuatro cantidades de la devolución en una sola operación. */
    public void registrarDevolucion(int devuelta, int rota, int perdida, int consumida) {
        this.cantidadDevuelta = devuelta;
        this.cantidadRota = rota;
        this.cantidadPerdida = perdida;
        this.cantidadConsumida = consumida;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventoItem otro)) return false;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return EventoItem.class.hashCode();
    }
}