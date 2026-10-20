package com.eventos.inventario.producto;

import com.eventos.inventario.compartido.exceptions.ResourceNotFoundException;
import com.eventos.inventario.producto.dto.ProductoRequest;
import com.eventos.inventario.producto.dto.ProductoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.eventos.inventario.compartido.storage.ImageStorage;
import com.eventos.inventario.compartido.storage.StoredImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository repository;
    private final ImageStorage imageStorage;

    public List<ProductoResponse> listar(TipoProducto tipo) {
        List<Producto> productos = (tipo == null)
                ? repository.findByActivoTrueOrderByNombreAsc()
                : repository.findByTipoAndActivoTrueOrderByNombreAsc(tipo);
        return productos.stream().map(ProductoResponse::desde).toList();
    }

    public ProductoResponse obtener(UUID id) {
        return ProductoResponse.desde(buscar(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = new Producto(request.nombre().trim(), request.tipo());
        return ProductoResponse.desde(repository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(UUID id, ProductoRequest request) {
        Producto producto = buscar(id);
        producto.setNombre(request.nombre().trim());
        producto.setTipo(request.tipo());
        return ProductoResponse.desde(producto);
    }

    @Transactional
    public void desactivar(UUID id) {
        buscar(id).setActivo(false);
    }

    private Producto buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
    }
    /** Uso interno entre módulos: devuelve la entidad, no un DTO. */
    public Producto obtenerEntidad(UUID id) {
        return buscar(id);
    }

    @Transactional
    public ProductoResponse subirFoto(UUID id, MultipartFile file) {
        Producto producto = buscar(id);
        String anterior = producto.getFotoReferencia();

        String nueva = imageStorage.store("productos/" + id, file);
        imageStorage.deleteWhenRolledBack(nueva);
        if (anterior != null) {
            imageStorage.deleteWhenCommitted(anterior);
        }

        producto.setFotoReferencia(nueva);
        return ProductoResponse.desde(producto);
    }

    public StoredImage foto(UUID id) {
        String ruta = buscar(id).getFotoReferencia();
        if (ruta == null) {
            throw new ResourceNotFoundException("Foto de referencia del producto", id);
        }
        return imageStorage.read(ruta);
    }

    @Transactional
    public ProductoResponse quitarFoto(UUID id) {
        Producto producto = buscar(id);
        String anterior = producto.getFotoReferencia();
        if (anterior != null) {
            imageStorage.deleteWhenCommitted(anterior);
            producto.setFotoReferencia(null);
        }
        return ProductoResponse.desde(producto);
    }
}