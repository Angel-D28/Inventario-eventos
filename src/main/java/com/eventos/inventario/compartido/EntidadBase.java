package com.eventos.inventario.compartido;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
public abstract class EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        this.createdAt = ahora;
        this.updatedAt = ahora;
    }

    @PreUpdate
    void alActualizar() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntidadBase otra)) return false;
        return getId() != null && getId().equals(otra.getId());
    }

    @Override
    public int hashCode() {
        return EntidadBase.class.hashCode();
    }
}