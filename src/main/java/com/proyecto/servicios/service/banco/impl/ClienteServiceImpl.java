package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.dto.request.banco.*;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.ClienteResponse;
import com.proyecto.servicios.dto.response.banco.DisponibilidadRegistroResponse;
import com.proyecto.servicios.entity.banco.*;
import com.proyecto.servicios.entity.catalogos.*;
import com.proyecto.servicios.enums.EstadoCivil;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.banco.*;
import com.proyecto.servicios.repository.banco.*;
import com.proyecto.servicios.repository.catalogos.*;
import com.proyecto.servicios.repository.specification.ClienteSpecification;
import com.proyecto.servicios.service.banco.BiometriaService;
import com.proyecto.servicios.service.banco.ClienteService;
import com.proyecto.servicios.service.banco.PostaliaCodigoPostalService;
import com.proyecto.servicios.util.PaginacionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CLIENTE_NO_ENCONTRADO_MSG = "No se encontró el cliente con ID: ";

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final BiometriaService biometriaService;
    private final PaisRepository paisRepository;
    private final EstadoCatalogoRepository estadoCatalogoRepository;
    private final CodigoPostalCatalogoRepository codigoPostalCatalogoRepository;
    private final PostaliaCodigoPostalService postaliaCodigoPostalService;

    @Value("${banco.onboarding.cuenta.saldo-inicial-default:1000.00}")
    private BigDecimal saldoInicialDefault;

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTRO
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClienteResponse registrarCliente(RegistroClienteRequest request) {
        log.info("Iniciando onboarding para CURP: {}", request.getCurp());

        // 1. Validaciones de negocio
        validarMayoriaEdad(request.getFechaNacimiento());
        validarUnicidad(request.getCurp(), request.getRfc(), request.getCorreo(), request.getTelefonoMovil());
        validarCatalogosYEstadoCivil(request.getEstadoCivil(), request.getDomicilio());

        String ocupacionNorm = request.getOcupacion().trim();
        boolean esEstudiante = "Estudiante".equalsIgnoreCase(ocupacionNorm);
        String empresaNorm = (request.getEmpresa() != null && !request.getEmpresa().isBlank())
                ? request.getEmpresa().trim()
                : null;
        if (!esEstudiante && empresaNorm == null) {
            throw new ReglaNegocioException("La empresa es obligatoria para la ocupación seleccionada.");
        }
        if (esEstudiante) {
            empresaNorm = null;
        }

        // 2. Construir y persistir Cliente
        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre().trim())
                .segundoNombre(request.getSegundoNombre() != null ? request.getSegundoNombre().trim() : null)
                .apellidoPaterno(request.getApellidoPaterno().trim())
                .apellidoMaterno(request.getApellidoMaterno().trim())
                .fechaNacimiento(request.getFechaNacimiento())
                .curp(request.getCurp().trim().toUpperCase())
                .rfc(request.getRfc().trim().toUpperCase())
                .sexo(request.getSexo())
                .nacionalidad(request.getNacionalidad())
                .estadoCivil(request.getEstadoCivil().toUpperCase())
                .correo(request.getCorreo().trim().toLowerCase())
                .telefonoMovil(request.getTelefonoMovil().trim())
                .telefonoAlternativo(request.getTelefonoAlternativo() != null ? request.getTelefonoAlternativo().trim() : null)
                .ocupacion(ocupacionNorm)
                .empresa(empresaNorm)
                .ingresoMensual(request.getIngresoMensual())
                .activo(true)
                .build();

        // 3. Asociar Domicilio
        DomicilioRequest dDTO = request.getDomicilio();
        Domicilio domicilio = Domicilio.builder()
                .cliente(cliente)
                .calle(dDTO.getCalle().trim())
                .numeroExterior(dDTO.getNumeroExterior().trim())
                .numeroInterior(dDTO.getNumeroInterior() != null ? dDTO.getNumeroInterior().trim() : null)
                .colonia(dDTO.getColonia().trim())
                .municipio(dDTO.getMunicipio().trim())
                .estado(dDTO.getEstado().trim())
                .codigoPostal(dDTO.getCodigoPostal().trim())
                .pais(dDTO.getPais().trim())
                .build();
        cliente.setDomicilio(domicilio);

        cliente = clienteRepository.save(cliente);

        // 4. Crear Cuenta Bancaria Inicial Automática
        Cuenta cuenta = Cuenta.builder()
                .cliente(cliente)
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldo(saldoInicialDefault)
                .estatus(EstatusCuenta.ACTIVA)
                .build();
        cuentaRepository.save(cuenta);

        // 5. Crear Usuario de Acceso
        Usuario usuario = Usuario.builder()
                .cliente(cliente)
                .correo(cliente.getCorreo())
                .password(passwordEncoder.encode(request.getPassword()))
                .activo(true)
                .build();
        usuarioRepository.save(usuario);

        // 6. Almacenar Biometría (opcional)
        if (request.getDatosBiometricosFacialBase64() != null && !request.getDatosBiometricosFacialBase64().trim().isEmpty()) {
            biometriaService.procesarYGuardarBiometria(cliente, request.getDatosBiometricosFacialBase64());
        }

        log.info("Onboarding completado exitosamente para el cliente ID: {}", cliente.getId());

        return ClienteResponse.desdeEntidad(cliente, List.of(cuenta));
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadRegistroResponse verificarDisponibilidad(String curp, String rfc, String correo) {
        return verificarDisponibilidad(curp, rfc, correo, null);
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadRegistroResponse verificarDisponibilidad(String curp, String rfc, String correo, String telefonoMovil) {
        boolean curpDisponible = curp == null || curp.isBlank()
                || !clienteRepository.existsByCurp(curp.trim().toUpperCase());
        boolean rfcDisponible = rfc == null || rfc.isBlank()
                || !clienteRepository.existsByRfc(rfc.trim().toUpperCase());
        boolean correoDisponible = true;
        if (correo != null && !correo.isBlank()) {
            String correoNorm = correo.trim().toLowerCase();
            correoDisponible = !clienteRepository.existsByCorreo(correoNorm)
                    && !usuarioRepository.existsByCorreo(correoNorm);
        }
        boolean telefonoDisponible = telefonoMovil == null || telefonoMovil.isBlank()
                || !clienteRepository.existsTelefonoActivo(telefonoMovil.trim());
        return new DisponibilidadRegistroResponse(curpDisponible, rfcDisponible, correoDisponible, telefonoDisponible);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSULTAS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse consultarPorId(ClienteIdRequest request) {
        Long id = request.getId();
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(CLIENTE_NO_ENCONTRADO_MSG + id));
        return ClienteResponse.desdeEntidad(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse consultarPorNumeroCuenta(NumeroCuentaRequest request) {
        String num = request.getNumeroCuenta().trim();
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(num)
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró la cuenta: " + num));
        return ClienteResponse.desdeEntidad(cuenta.getCliente());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ClienteResponse> consultar(ClienteFiltroRequest request) {
        Specification<Cliente> spec = Specification.allOf(
                ClienteSpecification.nombreContiene(request.getNombre()),
                ClienteSpecification.apellidoPaternoContiene(request.getApellidoPaterno()),
                ClienteSpecification.apellidoMaternoContiene(request.getApellidoMaterno()),
                ClienteSpecification.curpContiene(request.getCurp()),
                ClienteSpecification.rfcContiene(request.getRfc()),
                ClienteSpecification.correoContiene(request.getCorreo()),
                ClienteSpecification.tieneNumeroCuenta(request.getNumeroCuenta()),
                ClienteSpecification.activoIgual(request.getSoloActivos()),
                ClienteSpecification.creadoDesde(request.getFechaInicio()),
                ClienteSpecification.creadoHasta(request.getFechaFin())
        );

        Pageable pageable = PaginacionUtils.crearPageable(
                request.getPaginacion(), request.getOrdenarPor(), "id");

        Page<Cliente> pagina = clienteRepository.findAll(spec, pageable);
        return PaginaResponse.desdePagina(pagina, ClienteResponse::desdeEntidad);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTUALIZACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClienteResponse actualizarCliente(ActualizarClienteRequest patch) {
        Long id = patch.getId();
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(CLIENTE_NO_ENCONTRADO_MSG + id));

        // Aplicación selectiva de campos permitidos (CURP y RFC son inmutables)
        if (patch.getEstadoCivil() != null) {
            validarCatalogosYEstadoCivil(patch.getEstadoCivil(), null);
            cliente.setEstadoCivil(patch.getEstadoCivil().toUpperCase());
        }

        if (patch.getNombre() != null) cliente.setNombre(patch.getNombre().trim());
        if (patch.getSegundoNombre() != null) cliente.setSegundoNombre(patch.getSegundoNombre().trim());
        if (patch.getApellidoPaterno() != null) cliente.setApellidoPaterno(patch.getApellidoPaterno().trim());
        if (patch.getApellidoMaterno() != null) cliente.setApellidoMaterno(patch.getApellidoMaterno().trim());
        if (patch.getSexo() != null) cliente.setSexo(patch.getSexo());
        if (patch.getNacionalidad() != null) cliente.setNacionalidad(patch.getNacionalidad());
        if (patch.getTelefonoMovil() != null) {
            String telNuevo = patch.getTelefonoMovil().trim();
            if (!telNuevo.equals(cliente.getTelefonoMovil())
                    && clienteRepository.existsTelefonoActivoEnOtroCliente(telNuevo, cliente.getId())) {
                throw new TelefonoDuplicadoException(
                        "El teléfono '" + telNuevo + "' ya está registrado en una cuenta activa.");
            }
            cliente.setTelefonoMovil(telNuevo);
        }
        if (patch.getTelefonoAlternativo() != null) cliente.setTelefonoAlternativo(patch.getTelefonoAlternativo().trim());
        if (patch.getOcupacion() != null) cliente.setOcupacion(patch.getOcupacion().trim());
        if (patch.getEmpresa() != null) cliente.setEmpresa(patch.getEmpresa().trim());
        if (patch.getIngresoMensual() != null) cliente.setIngresoMensual(patch.getIngresoMensual());

        // Actualización de correo con validación de unicidad y sincronización con Usuario
        if (patch.getCorreo() != null && !patch.getCorreo().equalsIgnoreCase(cliente.getCorreo())) {
            String correoNuevo = patch.getCorreo().trim().toLowerCase();
            if (clienteRepository.existsByCorreo(correoNuevo)) {
                throw new CorreoElectronicoDuplicadoException(
                        "El correo '" + correoNuevo + "' ya está en uso por otro cliente.");
            }
            cliente.setCorreo(correoNuevo);
            usuarioRepository.findByClienteId(cliente.getId()).ifPresent(u -> {
                u.setCorreo(correoNuevo);
                usuarioRepository.save(u);
            });
        }

        // Actualización del domicilio
        if (patch.getDomicilio() != null) {
            Domicilio d = cliente.getDomicilio();
            if (d == null) {
                d = new Domicilio();
                d.setCliente(cliente);
                cliente.setDomicilio(d);
            }
            DomicilioRequest dto = patch.getDomicilio();
            if (dto.getCalle() != null) d.setCalle(dto.getCalle().trim());
            if (dto.getNumeroExterior() != null) d.setNumeroExterior(dto.getNumeroExterior().trim());
            if (dto.getNumeroInterior() != null) d.setNumeroInterior(dto.getNumeroInterior().trim());
            if (dto.getColonia() != null) d.setColonia(dto.getColonia().trim());
            if (dto.getMunicipio() != null) d.setMunicipio(dto.getMunicipio().trim());
            if (dto.getEstado() != null) d.setEstado(dto.getEstado().trim());
            if (dto.getCodigoPostal() != null) d.setCodigoPostal(dto.getCodigoPostal().trim());
            if (dto.getPais() != null) d.setPais(dto.getPais().trim());

            if (dto.getPais() != null || dto.getEstado() != null || dto.getMunicipio() != null || dto.getCodigoPostal() != null) {
                validarCatalogosYEstadoCivil(null, dto);
            }
        }

        cliente = clienteRepository.save(cliente);
        return ClienteResponse.desdeEntidad(cliente);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BAJA LÓGICA
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bajaLogicaCliente(ClienteIdRequest request) {
        Long id = request.getId();
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(CLIENTE_NO_ENCONTRADO_MSG + id));

        cliente.setActivo(false);
        clienteRepository.save(cliente);

        // Desactivar usuario de acceso asociado
        usuarioRepository.findByClienteId(id).ifPresent(u -> {
            u.setActivo(false);
            usuarioRepository.save(u);
        });

        // Desactivar todas las cuentas asociadas
        List<Cuenta> cuentas = cuentaRepository.findByClienteId(id);
        cuentas.forEach(c -> c.setEstatus(EstatusCuenta.INACTIVA));
        cuentaRepository.saveAll(cuentas);

        log.info("Baja lógica completada para el cliente ID: {}", id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTODOS PRIVADOS DE VALIDACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    private void validarMayoriaEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null || Period.between(fechaNacimiento, LocalDate.now(ZoneId.systemDefault())).getYears() < 18) {
            throw new ReglaNegocioException("El cliente debe ser mayor de edad (18 años cumplidos).");
        }
    }

    private void validarUnicidad(String curp, String rfc, String correo, String telefonoMovil) {
        if (clienteRepository.existsByCurp(curp)) {
            throw new CurpDuplicadaException("La CURP '" + curp + "' ya se encuentra registrada en el sistema.");
        }
        if (clienteRepository.existsByRfc(rfc)) {
            throw new RfcDuplicadoException("El RFC '" + rfc + "' ya se encuentra registrado en el sistema.");
        }
        if (clienteRepository.existsByCorreo(correo)) {
            throw new CorreoElectronicoDuplicadoException("El correo '" + correo + "' ya se encuentra registrado en el sistema.");
        }
        if (telefonoMovil != null && !telefonoMovil.isBlank()
                && clienteRepository.existsTelefonoActivo(telefonoMovil.trim())) {
            throw new TelefonoDuplicadoException(
                    "El teléfono '" + telefonoMovil.trim() + "' ya está registrado en una cuenta activa.");
        }
    }

    private String generarNumeroCuentaUnico() {
        String numero;
        do {
            long sufijo = 1_000_000_000L + (long) (SECURE_RANDOM.nextDouble() * 9_000_000_000L);
            numero = "012" + sufijo;
        } while (cuentaRepository.existsByNumeroCuenta(numero));
        return numero;
    }

    private void validarCatalogosYEstadoCivil(String estadoCivilStr, DomicilioRequest dDTO) {
        if (estadoCivilStr != null) {
            try {
                EstadoCivil.valueOf(estadoCivilStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ReglaNegocioException("Estado civil inválido. Valores permitidos: SOLTERO, CASADO, DIVORCIADO, VIUDO, UNION_LIBRE");
            }
        }

        if (dDTO != null) {
            Pais pais = paisRepository.findAll().stream()
                    .filter(p -> p.getNombre().equalsIgnoreCase(dDTO.getPais()))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("El país proporcionado no se encuentra en el catálogo."));

            String estadoNormalizado = PostaliaCodigoPostalService.normalizarEstado(dDTO.getEstado());
            String estadoComparar = PostaliaCodigoPostalService.normalizarTextoComparacion(estadoNormalizado);
            EstadoCatalogo estado = estadoCatalogoRepository.findByPaisId(pais.getId()).stream()
                    .filter(e -> e.getNombre().equalsIgnoreCase(dDTO.getEstado())
                            || PostaliaCodigoPostalService.normalizarTextoComparacion(e.getNombre()).equals(estadoComparar))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("El estado proporcionado no se encuentra en el catálogo para el país indicado."));

            String cpLimpio = dDTO.getCodigoPostal() == null ? "" : dDTO.getCodigoPostal().trim();
            String municipioComparar = PostaliaCodigoPostalService.normalizarTextoComparacion(dDTO.getMunicipio());
            boolean cpValido = codigoPostalCatalogoRepository.findByEstadoId(estado.getId()).stream()
                    .anyMatch(cp -> cp.getCodigo().equals(cpLimpio)
                            && PostaliaCodigoPostalService.normalizarTextoComparacion(cp.getMunicipio()).equals(municipioComparar));

            if (!cpValido && postaliaCodigoPostalService != null) {
                cpValido = postaliaCodigoPostalService.consultarCodigoPostal(cpLimpio)
                        .map(p -> PostaliaCodigoPostalService.normalizarTextoComparacion(p.estado())
                                .equals(PostaliaCodigoPostalService.normalizarTextoComparacion(estado.getNombre()))
                                && PostaliaCodigoPostalService.normalizarTextoComparacion(p.municipio())
                                .equals(municipioComparar))
                        .orElse(false);
            }

            if (!cpValido) {
                throw new ReglaNegocioException("El código postal o municipio proporcionado no coincide con los registros del catálogo para el estado indicado.");
            }
        }
    }
}
