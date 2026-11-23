package com.proyecto.servicios.service.banco.impl;

import com.proyecto.servicios.dto.request.banco.*;
import com.proyecto.servicios.dto.response.PaginaResponse;
import com.proyecto.servicios.dto.response.banco.ClienteResponse;
import com.proyecto.servicios.entity.banco.Cliente;
import com.proyecto.servicios.entity.banco.Cuenta;
import com.proyecto.servicios.entity.banco.Domicilio;
import com.proyecto.servicios.entity.banco.Usuario;
import com.proyecto.servicios.entity.catalogos.CodigoPostalCatalogo;
import com.proyecto.servicios.entity.catalogos.EstadoCatalogo;
import com.proyecto.servicios.entity.catalogos.Pais;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.banco.*;
import com.proyecto.servicios.repository.banco.ClienteRepository;
import com.proyecto.servicios.repository.banco.CuentaRepository;
import com.proyecto.servicios.repository.banco.UsuarioRepository;
import com.proyecto.servicios.repository.catalogos.CodigoPostalCatalogoRepository;
import com.proyecto.servicios.repository.catalogos.EstadoCatalogoRepository;
import com.proyecto.servicios.repository.catalogos.PaisRepository;
import com.proyecto.servicios.service.banco.BiometriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reglas de Negocio: ClienteServiceImpl")
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BiometriaService biometriaService;
    @Mock
    private PaisRepository paisRepository;
    @Mock
    private EstadoCatalogoRepository estadoCatalogoRepository;
    @Mock
    private CodigoPostalCatalogoRepository codigoPostalCatalogoRepository;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(clienteService, "saldoInicialDefault", new BigDecimal("1000.00"));
    }

    private DomicilioRequest crearDomicilioValido() {
        DomicilioRequest d = new DomicilioRequest();
        d.setCalle("Av. Reforma");
        d.setNumeroExterior("123");
        d.setColonia("Juárez");
        d.setMunicipio("Cuauhtémoc");
        d.setEstado("Ciudad de México");
        d.setCodigoPostal("06600");
        d.setPais("México");
        return d;
    }

    private RegistroClienteRequest crearRegistroValido() {
        RegistroClienteRequest req = new RegistroClienteRequest();
        req.setNombre("Carlos");
        req.setApellidoPaterno("Hernández");
        req.setApellidoMaterno("López");
        req.setFechaNacimiento(LocalDate.of(1995, 5, 20));
        req.setCurp("HELC950520HDFRRN01");
        req.setRfc("HELC950520ABC");
        req.setSexo("M");
        req.setNacionalidad("MEXICANA");
        req.setEstadoCivil("SOLTERO");
        req.setCorreo("carlos.hernandez@test.com");
        req.setTelefonoMovil("5512345678");
        req.setOcupacion("Ingeniero");
        req.setEmpresa("Tech SA");
        req.setIngresoMensual(new BigDecimal("35000.00"));
        req.setPassword("PasswordSeguro123!");
        req.setDomicilio(crearDomicilioValido());
        return req;
    }

    private void mockCatalogosValidos() {
        Pais pais = new Pais();
        pais.setId(1);
        pais.setNombre("México");

        EstadoCatalogo estado = new EstadoCatalogo();
        estado.setId(10);
        estado.setNombre("Ciudad de México");
        estado.setPais(pais);

        CodigoPostalCatalogo cp = new CodigoPostalCatalogo();
        cp.setId(100);
        cp.setCodigo("06600");
        cp.setMunicipio("Cuauhtémoc");
        cp.setEstado(estado);

        when(paisRepository.findAll()).thenReturn(List.of(pais));
        when(estadoCatalogoRepository.findByPaisId(1)).thenReturn(List.of(estado));
        when(codigoPostalCatalogoRepository.findByEstadoId(10)).thenReturn(List.of(cp));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTRO Y ONBOARDING
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("registrarCliente: flujo exitoso sin biometría crea cliente, cuenta y usuario")
    void registrarCliente_exitoso_sinBiometria() {
        RegistroClienteRequest request = crearRegistroValido();
        mockCatalogosValidos();

        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        ClienteResponse response = clienteService.registrarCliente(request);

        assertNotNull(response);
        assertEquals("Carlos", response.getNombre());
        assertEquals("carlos.hernandez@test.com", response.getCorreo());
        assertNotNull(response.getNumeroCuentas());
        assertEquals(1, response.getNumeroCuentas().size());

        verify(clienteRepository).save(any(Cliente.class));
        verify(cuentaRepository).save(any(Cuenta.class));
        verify(usuarioRepository).save(any(Usuario.class));
        verifyNoInteractions(biometriaService);
    }

    @Test
    @DisplayName("registrarCliente: flujo exitoso con biometría facial llama a BiometriaService")
    void registrarCliente_exitoso_conBiometria() {
        RegistroClienteRequest request = crearRegistroValido();
        request.setDatosBiometricosFacialBase64("base64PhotoData...");
        mockCatalogosValidos();

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        ClienteResponse response = clienteService.registrarCliente(request);

        assertNotNull(response);
        verify(biometriaService).procesarYGuardarBiometria(any(Cliente.class), eq("base64PhotoData..."));
    }

    @Test
    @DisplayName("registrarCliente: menor de 18 años lanza ReglaNegocioException")
    void registrarCliente_menorDeEdad() {
        RegistroClienteRequest request = crearRegistroValido();
        request.setFechaNacimiento(LocalDate.now().minusYears(17));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> clienteService.registrarCliente(request));
        assertTrue(ex.getMessage().contains("mayor de edad"));
    }

    @Test
    @DisplayName("registrarCliente: fecha de nacimiento nula lanza ReglaNegocioException")
    void registrarCliente_fechaNacimientoNula() {
        RegistroClienteRequest request = crearRegistroValido();
        request.setFechaNacimiento(null);

        assertThrows(ReglaNegocioException.class, () -> clienteService.registrarCliente(request));
    }

    @Test
    @DisplayName("registrarCliente: CURP duplicada lanza CurpDuplicadaException")
    void registrarCliente_curpDuplicada() {
        RegistroClienteRequest request = crearRegistroValido();
        when(clienteRepository.existsByCurp(request.getCurp().trim().toUpperCase())).thenReturn(true);

        assertThrows(CurpDuplicadaException.class, () -> clienteService.registrarCliente(request));
    }

    @Test
    @DisplayName("registrarCliente: RFC duplicado lanza RfcDuplicadoException")
    void registrarCliente_rfcDuplicado() {
        RegistroClienteRequest request = crearRegistroValido();
        when(clienteRepository.existsByRfc(request.getRfc().trim().toUpperCase())).thenReturn(true);

        assertThrows(RfcDuplicadoException.class, () -> clienteService.registrarCliente(request));
    }

    @Test
    @DisplayName("registrarCliente: Correo duplicado lanza CorreoElectronicoDuplicadoException")
    void registrarCliente_correoDuplicado() {
        RegistroClienteRequest request = crearRegistroValido();
        when(clienteRepository.existsByCorreo(request.getCorreo().trim().toLowerCase())).thenReturn(true);

        assertThrows(CorreoElectronicoDuplicadoException.class, () -> clienteService.registrarCliente(request));
    }

    @Test
    @DisplayName("registrarCliente: Estado civil no válido lanza ReglaNegocioException")
    void registrarCliente_estadoCivilInvalido() {
        RegistroClienteRequest request = crearRegistroValido();
        request.setEstadoCivil("INVENTADO");

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> clienteService.registrarCliente(request));
        assertTrue(ex.getMessage().contains("Estado civil inválido"));
    }

    @Test
    @DisplayName("registrarCliente: País fuera de catálogo lanza ReglaNegocioException")
    void registrarCliente_paisFueraDeCatalogo() {
        RegistroClienteRequest request = crearRegistroValido();
        Pais pais = new Pais();
        pais.setId(1);
        pais.setNombre("México");
        when(paisRepository.findAll()).thenReturn(List.of(pais));

        request.getDomicilio().setPais("España");

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> clienteService.registrarCliente(request));
        assertTrue(ex.getMessage().contains("país proporcionado no se encuentra en el catálogo"));
    }

    @Test
    @DisplayName("registrarCliente: Estado fuera de catálogo lanza ReglaNegocioException")
    void registrarCliente_estadoFueraDeCatalogo() {
        RegistroClienteRequest request = crearRegistroValido();
        Pais pais = new Pais();
        pais.setId(1);
        pais.setNombre("México");
        when(paisRepository.findAll()).thenReturn(List.of(pais));
        when(estadoCatalogoRepository.findByPaisId(1)).thenReturn(List.of());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> clienteService.registrarCliente(request));
        assertTrue(ex.getMessage().contains("estado proporcionado no se encuentra en el catálogo"));
    }

    @Test
    @DisplayName("registrarCliente: Código postal/municipio discrepante lanza ReglaNegocioException")
    void registrarCliente_cpNoCoincide() {
        RegistroClienteRequest request = crearRegistroValido();
        Pais pais = new Pais();
        pais.setId(1);
        pais.setNombre("México");

        EstadoCatalogo estado = new EstadoCatalogo();
        estado.setId(10);
        estado.setNombre("Ciudad de México");
        estado.setPais(pais);

        when(paisRepository.findAll()).thenReturn(List.of(pais));
        when(estadoCatalogoRepository.findByPaisId(1)).thenReturn(List.of(estado));
        when(codigoPostalCatalogoRepository.findByEstadoId(10)).thenReturn(List.of());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> clienteService.registrarCliente(request));
        assertTrue(ex.getMessage().contains("código postal o municipio proporcionado no coincide"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSULTAS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("consultarPorId: cliente existe retorna ClienteResponse")
    void consultarPorId_exitoso() {
        Cliente c = Cliente.builder().id(5L).nombre("Ana").activo(true).build();
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(c));

        ClienteIdRequest req = new ClienteIdRequest();
        req.setId(5L);

        ClienteResponse response = clienteService.consultarPorId(req);
        assertNotNull(response);
        assertEquals(5L, response.getId());
    }

    @Test
    @DisplayName("consultarPorId: no existe lanza ClienteNoEncontradoException")
    void consultarPorId_noExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        ClienteIdRequest req = new ClienteIdRequest();
        req.setId(99L);

        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.consultarPorId(req));
    }

    @Test
    @DisplayName("consultarPorNumeroCuenta: cuenta existe retorna titular ClienteResponse")
    void consultarPorNumeroCuenta_exitoso() {
        Cliente c = Cliente.builder().id(5L).nombre("Ana").build();
        Cuenta cuenta = Cuenta.builder().numeroCuenta("0120000001").cliente(c).build();

        when(cuentaRepository.findByNumeroCuenta("0120000001")).thenReturn(Optional.of(cuenta));

        NumeroCuentaRequest req = new NumeroCuentaRequest();
        req.setNumeroCuenta(" 0120000001 ");

        ClienteResponse res = clienteService.consultarPorNumeroCuenta(req);
        assertNotNull(res);
        assertEquals(5L, res.getId());
    }

    @Test
    @DisplayName("consultarPorNumeroCuenta: no existe cuenta lanza CuentaNoEncontradaException")
    void consultarPorNumeroCuenta_noExiste() {
        when(cuentaRepository.findByNumeroCuenta("0120000001")).thenReturn(Optional.empty());

        NumeroCuentaRequest req = new NumeroCuentaRequest();
        req.setNumeroCuenta("0120000001");

        assertThrows(CuentaNoEncontradaException.class, () -> clienteService.consultarPorNumeroCuenta(req));
    }

    @Test
    @DisplayName("consultar: búsqueda con filtros y paginación")
    void consultar_paginacion() {
        Cliente c = Cliente.builder().id(1L).nombre("Carlos").build();
        Page<Cliente> page = new PageImpl<>(List.of(c));

        when(clienteRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        ClienteFiltroRequest req = new ClienteFiltroRequest();
        req.setNombre("Carlos");

        PaginaResponse<ClienteResponse> resp = clienteService.consultar(req);
        assertNotNull(resp);
        assertEquals(1, resp.getContenido().size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTUALIZACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("actualizarCliente: actualización parcial de datos y cambio de correo con sincronización a Usuario")
    void actualizarCliente_exitoso() {
        Cliente existente = Cliente.builder()
                .id(1L)
                .nombre("Pedro")
                .correo("pedro.antiguo@test.com")
                .estadoCivil("SOLTERO")
                .domicilio(new Domicilio())
                .build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByCorreo("pedro.nuevo@test.com")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario u = Usuario.builder().id(10L).correo("pedro.antiguo@test.com").build();
        when(usuarioRepository.findByClienteId(1L)).thenReturn(Optional.of(u));

        ActualizarClienteRequest patch = new ActualizarClienteRequest();
        patch.setId(1L);
        patch.setNombre("Pedro Antonio");
        patch.setCorreo("pedro.nuevo@test.com");
        patch.setEstadoCivil("CASADO");

        ClienteResponse response = clienteService.actualizarCliente(patch);

        assertNotNull(response);
        assertEquals("Pedro Antonio", existente.getNombre());
        assertEquals("pedro.nuevo@test.com", existente.getCorreo());
        assertEquals("CASADO", existente.getEstadoCivil());

        verify(usuarioRepository).save(argThat(user -> "pedro.nuevo@test.com".equals(user.getCorreo())));
    }

    @Test
    @DisplayName("actualizarCliente: nuevo correo ya ocupado lanza CorreoElectronicoDuplicadoException")
    void actualizarCliente_correoDuplicado() {
        Cliente existente = Cliente.builder().id(1L).correo("correo1@test.com").build();
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByCorreo("correo.ocupado@test.com")).thenReturn(true);

        ActualizarClienteRequest patch = new ActualizarClienteRequest();
        patch.setId(1L);
        patch.setCorreo("correo.ocupado@test.com");

        assertThrows(CorreoElectronicoDuplicadoException.class, () -> clienteService.actualizarCliente(patch));
    }

    @Test
    @DisplayName("actualizarCliente: cliente no encontrado lanza ClienteNoEncontradoException")
    void actualizarCliente_noEncontrado() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        ActualizarClienteRequest patch = new ActualizarClienteRequest();
        patch.setId(99L);

        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.actualizarCliente(patch));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BAJA LÓGICA
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("bajaLogicaCliente: desactiva cliente, usuario de acceso y cuentas bancarias")
    void bajaLogicaCliente_exitosa() {
        Cliente c = Cliente.builder().id(1L).activo(true).build();
        Usuario u = Usuario.builder().id(10L).cliente(c).activo(true).build();
        Cuenta c1 = Cuenta.builder().id(101L).cliente(c).estatus(EstatusCuenta.ACTIVA).build();
        Cuenta c2 = Cuenta.builder().id(102L).cliente(c).estatus(EstatusCuenta.ACTIVA).build();

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
        when(usuarioRepository.findByClienteId(1L)).thenReturn(Optional.of(u));
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(c1, c2));

        ClienteIdRequest req = new ClienteIdRequest();
        req.setId(1L);

        clienteService.bajaLogicaCliente(req);

        assertFalse(c.getActivo());
        assertFalse(u.getActivo());
        assertEquals(EstatusCuenta.INACTIVA, c1.getEstatus());
        assertEquals(EstatusCuenta.INACTIVA, c2.getEstatus());

        verify(clienteRepository).save(c);
        verify(usuarioRepository).save(u);
        verify(cuentaRepository).saveAll(List.of(c1, c2));
    }

    @Test
    @DisplayName("bajaLogicaCliente: cliente no encontrado lanza ClienteNoEncontradoException")
    void bajaLogicaCliente_noExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        ClienteIdRequest req = new ClienteIdRequest();
        req.setId(99L);

        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.bajaLogicaCliente(req));
    }
}

