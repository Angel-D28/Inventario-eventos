package com.eventos.inventario.planeador;

import com.eventos.inventario.compartido.exceptions.ResourceNotFoundException;
import com.eventos.inventario.planeador.dto.PlaneadorRequest;
import com.eventos.inventario.planeador.dto.PlaneadorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaneadorService {

    private final PlaneadorRepository repository;

    public List<PlaneadorResponse> listar() {
        return repository.findAllByOrderByNombreAsc().stream()
                .map(PlaneadorResponse::desde)
                .toList();
    }

    public PlaneadorResponse obtener(UUID id) {
        return PlaneadorResponse.desde(buscar(id));
    }

    @Transactional
    public PlaneadorResponse crear(PlaneadorRequest request) {
        Planeador planeador = new Planeador(request.nombre().trim());
        aplicar(planeador, request);
        return PlaneadorResponse.desde(repository.save(planeador));
    }

    @Transactional
    public PlaneadorResponse actualizar(UUID id, PlaneadorRequest request) {
        Planeador planeador = buscar(id);
        planeador.setNombre(request.nombre().trim());
        aplicar(planeador, request);
        return PlaneadorResponse.desde(planeador);
    }

    @Transactional
    public void eliminar(UUID id) {
        Planeador planeador = buscar(id);
        repository.delete(planeador);
        repository.flush(); // fuerza la validación de la clave foránea dentro del método
    }

    private void aplicar(Planeador planeador, PlaneadorRequest request) {
        planeador.setTelefono(request.telefono());
        planeador.setNotas(request.notas());
    }

    private Planeador buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Planeador", id));
    }

    /** Uso interno entre módulos: devuelve la entidad, no un DTO. */
    public Planeador obtenerEntidad(UUID id) {
        return buscar(id);
    }
}