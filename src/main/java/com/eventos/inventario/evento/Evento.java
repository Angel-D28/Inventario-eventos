package com.eventos.inventario.evento;

import com.eventos.inventario.compartido.EntidadBase;
import com.eventos.inventario.planeador.Planeador;
import com.eventos.inventario.producto.Producto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "evento")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Evento extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planeador_id")
    private Planeador planeador;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 200)
    private String lugar;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEvento estado = EstadoEvento.PLANEADO;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<EventoItem> items = new ArrayList<>();

    public Evento(Planeador planeador, String nombre, String lugar, LocalDate fecha) {
        this.planeador = planeador;
        this.nombre = nombre;
        this.lugar = lugar;
        this.fecha = fecha;
    }

    public Optional<EventoItem> buscarItem(UUID productoId) {
        return items.stream()
                .filter(i -> i.getProducto().getId().equals(productoId))
                .findFirst();
    }
    public List<EventoItem> itemsSinCuadrar() {
        return items.stream()
                .filter(i -> i.diferencia() != 0)
                .toList();
    }

    public EventoItem agregarItem(Producto producto) {
        EventoItem item = new EventoItem(this, producto);
        items.add(item);
        return item;
    }

    public void quitarItem(EventoItem item) {
        items.remove(item);
    }
}