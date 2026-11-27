package com.proyecto.servicios.controller.banco;

import com.proyecto.servicios.dto.response.GenericResponse;
import com.proyecto.servicios.dto.response.banco.UbicacionCatalogoResponse;
import com.proyecto.servicios.entity.catalogos.CodigoPostalCatalogo;
import com.proyecto.servicios.entity.catalogos.ColoniaCatalogo;
import com.proyecto.servicios.entity.catalogos.OcupacionCatalogo;
import com.proyecto.servicios.repository.catalogos.CodigoPostalCatalogoRepository;
import com.proyecto.servicios.repository.catalogos.ColoniaCatalogoRepository;
import com.proyecto.servicios.repository.catalogos.OcupacionCatalogoRepository;
import com.proyecto.servicios.service.banco.PostaliaCodigoPostalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/catalogos")
@RequiredArgsConstructor
@Tag(name = "Catálogos", description = "Consulta pública de estados, municipios y códigos postales de México")
public class CatalogoUbicacionController {

    private final CodigoPostalCatalogoRepository codigoPostalRepository;
    private final ColoniaCatalogoRepository coloniaRepository;
    private final OcupacionCatalogoRepository ocupacionRepository;
    private final PostaliaCodigoPostalService postaliaCodigoPostalService;

    @GetMapping("/ubicaciones")
    @Operation(summary = "Listar municipios y códigos postales, opcionalmente filtrados por estado")
    public ResponseEntity<GenericResponse<List<UbicacionCatalogoResponse>>> listarUbicaciones(
            @RequestParam(required = false) String estado) {
        List<CodigoPostalCatalogo> lista = (estado != null && !estado.isBlank())
                ? codigoPostalRepository.buscarConEstadoPorNombreEstado(estado.trim())
                : codigoPostalRepository.buscarTodasConEstado();

        List<UbicacionCatalogoResponse> respuesta = lista.stream()
                .map(UbicacionCatalogoResponse::desdeEntidad)
                .toList();

        return ResponseEntity.ok(GenericResponse.exito("Ubicaciones consultadas correctamente", respuesta));
    }

    @GetMapping("/cp/{codigo}")
    @Operation(summary = "Buscar estado y municipio por código postal de 5 dígitos")
    public ResponseEntity<GenericResponse<List<UbicacionCatalogoResponse>>> buscarPorCodigoPostal(
            @PathVariable String codigo) {
        String cpLimpio = codigo == null ? "" : codigo.trim();
        Optional<PostaliaCodigoPostalService.CodigoPostalPostalia> postaliaOpt =
                postaliaCodigoPostalService.consultarCodigoPostal(cpLimpio);

        List<UbicacionCatalogoResponse> respuesta = postaliaOpt
                .map(p -> List.of(new UbicacionCatalogoResponse(p.estado(), p.municipio(), p.codigoPostal())))
                .orElseGet(() -> codigoPostalRepository
                        .buscarConEstadoPorCodigo(cpLimpio)
                        .stream()
                        .map(UbicacionCatalogoResponse::desdeEntidad)
                        .toList());

        return ResponseEntity.ok(GenericResponse.exito("Código postal consultado correctamente", respuesta));
    }

    @GetMapping("/colonias")
    @Operation(summary = "Listar colonias registradas para un código postal")
    public ResponseEntity<GenericResponse<List<String>>> listarColonias(
            @RequestParam String codigoPostal) {
        String cpLimpio = codigoPostal == null ? "" : codigoPostal.trim();
        List<String> respuesta = postaliaCodigoPostalService.consultarCodigoPostal(cpLimpio)
                .map(PostaliaCodigoPostalService.CodigoPostalPostalia::colonias)
                .filter(lista -> !lista.isEmpty())
                .orElseGet(() -> coloniaRepository
                        .findByCodigoPostalOrderByNombreAsc(cpLimpio)
                        .stream()
                        .map(ColoniaCatalogo::getNombre)
                        .toList());

        return ResponseEntity.ok(GenericResponse.exito("Colonias consultadas correctamente", respuesta));
    }

    @GetMapping("/ocupaciones")
    @Operation(summary = "Listar catálogo de ocupaciones disponibles para registro")
    public ResponseEntity<GenericResponse<List<String>>> listarOcupaciones() {
        List<String> respuesta = ocupacionRepository
                .findAllByOrderByOrdenAscNombreAsc()
                .stream()
                .map(OcupacionCatalogo::getNombre)
                .toList();

        return ResponseEntity.ok(GenericResponse.exito("Ocupaciones consultadas correctamente", respuesta));
    }
}
