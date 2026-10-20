package com.eventos.inventario.planeador;

import com.eventos.inventario.planeador.dto.PlaneadorRequest;
import com.eventos.inventario.planeador.dto.PlaneadorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/planeadores")
@RequiredArgsConstructor
public class PlaneadorController {

    private final PlaneadorService service;

    @GetMapping
    public List<PlaneadorResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public PlaneadorResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<PlaneadorResponse> crear(@Valid @RequestBody PlaneadorRequest request) {
        PlaneadorResponse creado = service.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public PlaneadorResponse actualizar(@PathVariable UUID id,
                                        @Valid @RequestBody PlaneadorRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}