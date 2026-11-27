import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/error/exceptions.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../../core/utils/input_formatters.dart';
import '../../../../core/widgets/auth_aura_background.dart';
import '../../data/models/registro_cliente_model.dart';
import '../cubit/auth_cubit.dart';
import '../cubit/auth_state.dart';

class RegistroPage extends StatefulWidget {
  const RegistroPage({super.key});

  @override
  State<RegistroPage> createState() => _RegistroPageState();
}

class _RegistroPageState extends State<RegistroPage> {
  static final _reNombre = RegExp(r'^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\s]{2,50}$');
  static final _reCurp =
      RegExp(r'^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[A-Z0-9]{2}$', caseSensitive: false);
  static final _reRfc =
      RegExp(r'^[A-Z&Ñ]{3,4}[0-9]{6}[A-Z0-9]{3}$', caseSensitive: false);
  static final _reTel = RegExp(r'^[0-9]{10}$');
  static final _reEmail =
      RegExp(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
  static final _reOcupacion = RegExp(r'^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\s]{2,100}$');
  static final _reNumDomicilio = RegExp(r'^[A-Za-z0-9/\-]{1,6}$');
  static final _rePass =
      RegExp(r'^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!._-]).{8,}$');

  int _paso = 1;
  bool _ocultarPass = true;
  bool _ocultarConfirm = true;
  bool _validandoPaso = false;
  String? _errorPaso;
  String? _errorCurpDuplicada;
  String? _errorRfcDuplicado;
  String? _errorCorreoDuplicado;
  String? _errorTelefonoDuplicado;

  // Verificación de correo en Paso 2
  final _codigoCorreoCtrl = TextEditingController();
  bool _codigoEnviado = false;
  bool _correoVerificado = false;
  String _correoEnviadoPara = '';
  bool _confirmandoCodigo = false;
  String? _errorCodigoCorreo;
  String? _avisoCodigoCorreo;

  final _formPaso1 = GlobalKey<FormState>();
  final _formPaso2 = GlobalKey<FormState>();
  final _formPaso3 = GlobalKey<FormState>();
  final _formPaso4 = GlobalKey<FormState>();

  // Paso 1
  final _nombreCtrl = TextEditingController();
  final _segundoNombreCtrl = TextEditingController();
  final _paternoCtrl = TextEditingController();
  final _maternoCtrl = TextEditingController();
  final _fechaNacCtrl = TextEditingController();
  final _curpCtrl = TextEditingController();
  final _rfcCtrl = TextEditingController();
  String _sexo = 'H';
  String _estadoCivil = 'SOLTERO';
  DateTime? _fechaNacimiento;

  // Paso 2
  final _correoCtrl = TextEditingController();
  final _telMovilCtrl = TextEditingController();
  final _telAltCtrl = TextEditingController();
  List<String> _ocupaciones = ocupacionesFallback;
  String? _ocupacionSeleccionada;
  final _ocupacionOtraCtrl = TextEditingController();
  final _empresaCtrl = TextEditingController();
  double? _ingresoSeleccionado;

  // Paso 3
  late final List<String> _estados =
      catalogoSepomex.map((e) => e.estado).toSet().toList();
  String _estadoSeleccionado = 'Ciudad de México';
  late List<CatalogoPostalItem> _ubicacionesEstadoActual =
      catalogoSepomex.where((e) => e.estado == 'Ciudad de México').toList();
  late CatalogoPostalItem _ubicacionSeleccionada = _ubicacionesEstadoActual.first;
  late final _cpCtrl = TextEditingController(
    text: _ubicacionSeleccionada.codigoPostal,
  );
  bool _buscandoCp = false;
  String? _avisoCp;
  List<String> _colonias = const ['Centro'];
  String? _coloniaSeleccionada;
  final _coloniaOtraCtrl = TextEditingController();
  final _calleCtrl = TextEditingController();
  final _numExtCtrl = TextEditingController();
  final _numIntCtrl = TextEditingController();

  // Paso 4
  final _passCtrl = TextEditingController();
  final _confirmCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _passCtrl.addListener(() {
      if (mounted) setState(() {});
    });
    _cargarUbicacionesEstado(_estadoSeleccionado);
    _cargarOcupaciones();
    _cargarColonias(_ubicacionSeleccionada.codigoPostal);
  }

  @override
  void dispose() {
    _codigoCorreoCtrl.dispose();
    _nombreCtrl.dispose();
    _segundoNombreCtrl.dispose();
    _paternoCtrl.dispose();
    _maternoCtrl.dispose();
    _fechaNacCtrl.dispose();
    _curpCtrl.dispose();
    _rfcCtrl.dispose();
    _correoCtrl.dispose();
    _telMovilCtrl.dispose();
    _telAltCtrl.dispose();
    _ocupacionOtraCtrl.dispose();
    _empresaCtrl.dispose();
    _cpCtrl.dispose();
    _coloniaOtraCtrl.dispose();
    _calleCtrl.dispose();
    _numExtCtrl.dispose();
    _numIntCtrl.dispose();
    _passCtrl.dispose();
    _confirmCtrl.dispose();
    super.dispose();
  }

  List<CatalogoPostalItem> _deduplicar(List<CatalogoPostalItem> lista) {
    final mapa = <String, CatalogoPostalItem>{};
    for (final item in lista) {
      mapa[item.clave] = item;
    }
    return mapa.values.toList();
  }

  Future<void> _cargarUbicacionesEstado(
    String estado, {
    CatalogoPostalItem? preferido,
  }) async {
    final locales = _deduplicar(
      catalogoSepomex.where((e) => e.estado == estado).toList(),
    );
    _aplicarUbicaciones(estado, locales, preferido: preferido);

    final remotas =
        await context.read<AuthCubit>().obtenerUbicacionesPorEstado(estado);
    if (!mounted || _estadoSeleccionado != estado || remotas.isEmpty) return;
    _aplicarUbicaciones(estado, _deduplicar(remotas), preferido: preferido);
  }

  void _aplicarUbicaciones(
    String estado,
    List<CatalogoPostalItem> lista, {
    CatalogoPostalItem? preferido,
  }) {
    final combinada = <CatalogoPostalItem>[
      if (preferido != null && !lista.any((e) => e.clave == preferido.clave))
        preferido,
      ...lista,
    ];
    if (combinada.isEmpty) return;
    if (!_estados.contains(estado)) {
      _estados.add(estado);
    }
    final destino = preferido != null
        ? combinada.firstWhere(
            (e) => e.clave == preferido.clave,
            orElse: () => combinada.first,
          )
        : combinada.first;
    setState(() {
      _estadoSeleccionado = estado;
      _ubicacionesEstadoActual = combinada;
      _ubicacionSeleccionada = destino;
      _cpCtrl.text = destino.codigoPostal;
    });
    _cargarColonias(destino.codigoPostal);
  }

  Future<void> _cargarOcupaciones() async {
    final remotas = await context.read<AuthCubit>().obtenerOcupaciones();
    if (!mounted || remotas.isEmpty) return;
    setState(() => _ocupaciones = remotas);
  }

  Future<void> _cargarColonias(String cp) async {
    final limpio = cp.trim();
    final locales = coloniasFallback[limpio] ?? const <String>[];
    if (locales.isNotEmpty) {
      _aplicarColonias(locales);
    }
    final remotas = await context.read<AuthCubit>().obtenerColoniasPorCp(limpio);
    if (!mounted || _cpCtrl.text.trim() != limpio) return;
    _aplicarColonias(remotas.isNotEmpty ? remotas : locales);
  }

  void _aplicarColonias(List<String> lista) {
    final unicas = lista.toSet().toList();
    setState(() {
      _colonias = unicas;
      if (_coloniaSeleccionada != null &&
          _coloniaSeleccionada != 'Otra' &&
          !unicas.contains(_coloniaSeleccionada)) {
        _coloniaSeleccionada = null;
        _coloniaOtraCtrl.clear();
      }
    });
  }

  Future<void> _alCambiarCodigoPostal(String valor) async {
    final cp = valor.trim();
    if (cp.length < 5) {
      if (_avisoCp != null) setState(() => _avisoCp = null);
      return;
    }

    setState(() {
      _buscandoCp = true;
      _avisoCp = null;
    });
    _cargarColonias(cp);
    var encontrados = await context.read<AuthCubit>().buscarPorCodigoPostal(cp);
    if (encontrados.isEmpty) {
      encontrados =
          catalogoSepomex.where((e) => e.codigoPostal == cp).toList();
    }
    if (!mounted) return;
    setState(() => _buscandoCp = false);

    if (encontrados.isEmpty) {
      setState(
          () => _avisoCp = 'CP no encontrado; selecciona Estado y Municipio.');
      return;
    }

    final primero = encontrados.first;
    setState(() {
      _estadoSeleccionado = primero.estado;
      _avisoCp = '${primero.municipio}, ${primero.estado}';
    });
    await _cargarUbicacionesEstado(primero.estado, preferido: primero);
  }

  Future<void> _seleccionarFecha() async {
    final hoy = DateTime.now();
    final maxFecha = DateTime(hoy.year - 18, hoy.month, hoy.day);
    final minFecha = DateTime(hoy.year - 100, 1, 1);
    final inicial =
        _fechaNacimiento ?? DateTime(hoy.year - 25, hoy.month, hoy.day);

    final elegida = await showDatePicker(
      context: context,
      initialDate: inicial.isAfter(maxFecha) ? maxFecha : inicial,
      firstDate: minFecha,
      lastDate: maxFecha,
    );
    if (elegida != null) {
      setState(() {
        _fechaNacimiento = elegida;
        _fechaNacCtrl.text =
            '${elegida.year.toString().padLeft(4, '0')}-${elegida.month.toString().padLeft(2, '0')}-${elegida.day.toString().padLeft(2, '0')}';
      });
    }
  }

  Future<void> _enviarCodigoCorreo({bool reenvio = false}) async {
    final correo = _correoCtrl.text.trim().toLowerCase();
    setState(() {
      _validandoPaso = !reenvio;
      _confirmandoCodigo = reenvio;
      _errorCodigoCorreo = null;
      _avisoCodigoCorreo = null;
    });
    try {
      await context.read<AuthCubit>().enviarCodigoVerificacionCorreo(correo);
      if (!mounted) return;
      setState(() {
        _validandoPaso = false;
        _confirmandoCodigo = false;
        _codigoEnviado = true;
        _correoEnviadoPara = correo;
        _avisoCodigoCorreo =
            'Enviamos un código de 6 dígitos a $correo. Ingrésalo para validar tu correo.';
      });
    } on ServerException catch (e) {
      if (!mounted) return;
      setState(() {
        _validandoPaso = false;
        _confirmandoCodigo = false;
        _errorCorreoDuplicado = e.message;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _validandoPaso = false;
        _confirmandoCodigo = false;
        _errorPaso =
            'No fue posible enviar el código al correo. Verifica tu conexión.';
      });
    }
  }

  Future<bool> _confirmarCodigoCorreo() async {
    final correo = _correoCtrl.text.trim().toLowerCase();
    final codigo = _codigoCorreoCtrl.text.trim();
    if (codigo.length != 6) {
      setState(() => _errorCodigoCorreo = 'Ingresa los 6 dígitos');
      return false;
    }

    setState(() {
      _confirmandoCodigo = true;
      _errorCodigoCorreo = null;
    });
    try {
      final ok = await context
          .read<AuthCubit>()
          .confirmarCodigoVerificacionCorreo(correo, codigo);
      if (!mounted) return false;
      setState(() {
        _confirmandoCodigo = false;
        _correoVerificado = ok;
        if (!ok) _errorCodigoCorreo = 'Código incorrecto';
      });
      return ok;
    } on ServerException catch (e) {
      if (!mounted) return false;
      setState(() {
        _confirmandoCodigo = false;
        _errorCodigoCorreo = e.message;
      });
      return false;
    } catch (_) {
      if (!mounted) return false;
      setState(() {
        _confirmandoCodigo = false;
        _errorCodigoCorreo = 'Error al validar el código';
      });
      return false;
    }
  }

  String _resumirErroresPaso(int paso) {
    final faltantes = <String>[];
    if (paso == 1) {
      if (!_reNombre.hasMatch(_nombreCtrl.text.trim())) faltantes.add('Nombre');
      final seg = _segundoNombreCtrl.text.trim();
      if (seg.isNotEmpty && !_reNombre.hasMatch(seg)) faltantes.add('Segundo nombre');
      if (!_reNombre.hasMatch(_paternoCtrl.text.trim())) faltantes.add('Apellido paterno');
      if (!_reNombre.hasMatch(_maternoCtrl.text.trim())) faltantes.add('Apellido materno');
      if (_fechaNacimiento == null) faltantes.add('Fecha de nacimiento');
      if (!_reCurp.hasMatch(_curpCtrl.text.trim().toUpperCase())) faltantes.add('CURP');
      if (!_reRfc.hasMatch(_rfcCtrl.text.trim().toUpperCase())) faltantes.add('RFC');
    } else if (paso == 2) {
      if (!_reEmail.hasMatch(_correoCtrl.text.trim())) faltantes.add('Correo electrónico');
      if (!_reTel.hasMatch(_telMovilCtrl.text.trim())) faltantes.add('Teléfono móvil');
      final alt = _telAltCtrl.text.trim();
      if (alt.isNotEmpty && !_reTel.hasMatch(alt)) faltantes.add('Teléfono alternativo');
      if (_ocupacionSeleccionada == null || _ocupacionSeleccionada!.isEmpty) {
        faltantes.add('Ocupación');
      } else if (_ocupacionSeleccionada == 'Otro' &&
          !_reOcupacion.hasMatch(_ocupacionOtraCtrl.text.trim())) {
        faltantes.add('Especifica tu ocupación');
      }
      if (_ocupacionSeleccionada != 'Estudiante' && _empresaCtrl.text.trim().isEmpty) {
        faltantes.add('Empresa');
      }
      if (_ingresoSeleccionado == null || _ingresoSeleccionado! <= 0) {
        faltantes.add('Ingreso mensual');
      }
    } else if (paso == 3) {
      if (_cpCtrl.text.trim().length != 5) faltantes.add('Código postal');
      if (_coloniaSeleccionada == null || _coloniaSeleccionada!.isEmpty) {
        faltantes.add('Colonia');
      } else if (_coloniaSeleccionada == 'Otra' && _coloniaOtraCtrl.text.trim().isEmpty) {
        faltantes.add('Nombre de tu colonia');
      }
      if (_calleCtrl.text.trim().isEmpty) faltantes.add('Calle');
      if (!_reNumDomicilio.hasMatch(_numExtCtrl.text.trim())) {
        faltantes.add('Número exterior');
      }
      final numInt = _numIntCtrl.text.trim();
      if (numInt.isNotEmpty && !_reNumDomicilio.hasMatch(numInt)) {
        faltantes.add('Número interior');
      }
    } else if (paso == 4) {
      if (!_rePass.hasMatch(_passCtrl.text)) faltantes.add('Contraseña');
      if (_confirmCtrl.text != _passCtrl.text) faltantes.add('Confirmación de contraseña');
    }
    if (faltantes.isEmpty) {
      return 'Revisa los campos marcados en rojo antes de continuar.';
    }
    return 'Completa los campos marcados en rojo: ${faltantes.join(', ')}.';
  }

  Future<void> _siguientePaso() async {
    final form = switch (_paso) {
      1 => _formPaso1.currentState,
      2 => _formPaso2.currentState,
      3 => _formPaso3.currentState,
      _ => null,
    };
    if (_validandoPaso || form == null) return;
    if (!form.validate()) {
      setState(() => _errorPaso = _resumirErroresPaso(_paso));
      return;
    }

    setState(() {
      _errorPaso = null;
      if (_paso == 1) {
        _errorCurpDuplicada = null;
        _errorRfcDuplicado = null;
      } else if (_paso == 2) {
        _errorCorreoDuplicado = null;
        _errorTelefonoDuplicado = null;
      }
    });

    if (_paso == 1) {
      setState(() => _validandoPaso = true);
      try {
        final disp = await context.read<AuthCubit>().verificarDisponibilidad(
              curp: _curpCtrl.text,
              rfc: _rfcCtrl.text,
            );
        if (!mounted) return;
        setState(() {
          _validandoPaso = false;
          _errorCurpDuplicada =
              disp.curpDisponible ? null : 'Esta CURP ya está registrada';
          _errorRfcDuplicado =
              disp.rfcDisponible ? null : 'Este RFC ya está registrado';
          if (disp.curpDisponible && disp.rfcDisponible) {
            _paso = 2;
          }
        });
      } catch (_) {
        if (!mounted) return;
        setState(() {
          _validandoPaso = false;
          _errorPaso = 'No se pudo verificar la disponibilidad de CURP y RFC.';
        });
      }
      return;
    }

    if (_paso == 2) {
      setState(() => _validandoPaso = true);
      try {
        final disp = await context.read<AuthCubit>().verificarDisponibilidad(
              correo: _correoCtrl.text,
              telefonoMovil: _telMovilCtrl.text,
            );
        if (!mounted) return;
        if (!disp.correoDisponible || !disp.telefonoDisponible) {
          setState(() {
            _validandoPaso = false;
            _errorCorreoDuplicado =
                disp.correoDisponible ? null : 'Este correo ya está registrado';
            _errorTelefonoDuplicado = disp.telefonoDisponible
                ? null
                : 'Este teléfono ya está asociado a otra cuenta activa';
          });
          return;
        }
      } catch (_) {
        if (!mounted) return;
        setState(() {
          _validandoPaso = false;
          _errorPaso =
              'No se pudo verificar la disponibilidad del correo o teléfono.';
        });
        return;
      }

      if (_correoVerificado) {
        setState(() {
          _validandoPaso = false;
          _paso = 3;
        });
        return;
      }

      if (!_codigoEnviado) {
        await _enviarCodigoCorreo();
        return;
      }

      setState(() => _validandoPaso = false);
      final ok = await _confirmarCodigoCorreo();
      if (ok && mounted) {
        setState(() => _paso = 3);
      }
      return;
    }

    setState(() => _paso++);
  }

  void _enviarRegistro() {
    if (!_formPaso4.currentState!.validate()) {
      setState(() => _errorPaso = _resumirErroresPaso(4));
      return;
    }
    setState(() => _errorPaso = null);
    final esEstudiante = _ocupacionSeleccionada == 'Estudiante';
    final ocupacionFinal = _ocupacionSeleccionada == 'Otro'
        ? _ocupacionOtraCtrl.text.trim()
        : (_ocupacionSeleccionada ?? '');
    final coloniaFinal = _coloniaSeleccionada == 'Otra'
        ? _coloniaOtraCtrl.text.trim()
        : (_coloniaSeleccionada ?? '');

    final params = RegistroClienteParams(
      nombre: _nombreCtrl.text,
      segundoNombre: _segundoNombreCtrl.text,
      apellidoPaterno: _paternoCtrl.text,
      apellidoMaterno: _maternoCtrl.text,
      fechaNacimiento: _fechaNacCtrl.text,
      curp: _curpCtrl.text,
      rfc: _rfcCtrl.text,
      sexo: _sexo,
      estadoCivil: _estadoCivil,
      correo: _correoCtrl.text,
      telefonoMovil: _telMovilCtrl.text,
      telefonoAlternativo: _telAltCtrl.text,
      ocupacion: ocupacionFinal,
      empresa: esEstudiante ? null : _empresaCtrl.text,
      ingresoMensual: _ingresoSeleccionado ?? 0,
      calle: _calleCtrl.text,
      numeroExterior: _numExtCtrl.text,
      numeroInterior: _numIntCtrl.text,
      colonia: coloniaFinal,
      municipio: _ubicacionSeleccionada.municipio,
      estado: _estadoSeleccionado,
      codigoPostal: _ubicacionSeleccionada.codigoPostal,
      password: _passCtrl.text,
    );
    context.read<AuthCubit>().registrarCliente(params);
  }

  void _volverALogin() {
    context.read<AuthCubit>().cancelarConflicto();
    context.go('/login');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: AuthAuraBackground(
        child: BlocBuilder<AuthCubit, AuthState>(
          builder: (context, state) {
            if (state is AuthRegistroExitoso) {
              return _VistaExito(
                resultado: state.resultado,
                onContinuar: _volverALogin,
              );
            }

            final isLoading = state is AuthLoading;
            final error = _errorPaso ??
                (state is AuthError && !state.duranteOtp ? state.mensaje : null);

            return SafeArea(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(12, 8, 16, 0),
                    child: Align(
                      alignment: Alignment.centerLeft,
                      child: IconButton(
                        icon: const Icon(
                          Icons.arrow_back_ios_new_rounded,
                          size: 20,
                          color: AppColors.ink,
                        ),
                        onPressed: () {
                          if (_paso > 1 && !isLoading && !_validandoPaso) {
                            setState(() {
                              _errorPaso = null;
                              _paso--;
                            });
                          } else {
                            _volverALogin();
                          }
                        },
                      ),
                    ),
                  ),
                  Expanded(
                    child: SingleChildScrollView(
                      padding: const EdgeInsets.fromLTRB(24, 6, 24, 24),
                      child: Center(
                        child: ConstrainedBox(
                          constraints: const BoxConstraints(maxWidth: 460),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              const Text(
                                'Crear cuenta',
                                style: TextStyle(
                                  fontSize: 30,
                                  fontWeight: FontWeight.w800,
                                  letterSpacing: -0.4,
                                  color: AppColors.ink,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Wrap(
                                crossAxisAlignment: WrapCrossAlignment.center,
                                children: [
                                  const Text(
                                    '¿Ya tienes cuenta? ',
                                    style: TextStyle(
                                      color: AppColors.ink2,
                                      fontSize: 14,
                                    ),
                                  ),
                                  GestureDetector(
                                    onTap: isLoading ? null : _volverALogin,
                                    child: const Text(
                                      'Iniciar sesión',
                                      style: TextStyle(
                                        color: AppColors.brand,
                                        fontWeight: FontWeight.w700,
                                        fontSize: 14,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 18),
                              _BarraPasos(pasoActual: _paso),
                              const SizedBox(height: 18),
                              if (error != null)
                                Container(
                                  margin: const EdgeInsets.only(bottom: 14),
                                  padding: const EdgeInsets.symmetric(
                                      horizontal: 14, vertical: 12),
                                  decoration: BoxDecoration(
                                    color: AppColors.dangerSoft,
                                    borderRadius: BorderRadius.circular(14),
                                  ),
                                  child: Text(
                                    error,
                                    style: const TextStyle(
                                      color: AppColors.danger,
                                      fontSize: 13.5,
                                    ),
                                  ),
                                ),
                              switch (_paso) {
                                1 => _construirPaso1(isLoading),
                                2 => _construirPaso2(isLoading),
                                3 => _construirPaso3(isLoading),
                                _ => _construirPaso4(isLoading),
                              },
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _construirPaso1(bool isLoading) {
    return Form(
      key: _formPaso1,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextFormField(
            controller: _nombreCtrl,
            enabled: !isLoading && !_validandoPaso,
            inputFormatters: AppInputFormatters.soloLetras(50),
            decoration: AppTheme.pillInputDecoration(
              hintText: 'Nombre',
              prefixIcon: Icons.person_outline_rounded,
            ),
            validator: (v) => (v == null || !_reNombre.hasMatch(v.trim()))
                ? 'Solo letras (2 a 50)'
                : null,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _segundoNombreCtrl,
            enabled: !isLoading && !_validandoPaso,
            inputFormatters: AppInputFormatters.soloLetras(50),
            decoration: AppTheme.pillInputDecoration(
              hintText: 'Segundo nombre (opcional)',
              prefixIcon: Icons.person_outline_rounded,
            ),
            validator: (v) {
              final t = v?.trim() ?? '';
              if (t.isNotEmpty && !_reNombre.hasMatch(t)) {
                return 'Solo letras (2 a 50)';
              }
              return null;
            },
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _paternoCtrl,
            enabled: !isLoading && !_validandoPaso,
            inputFormatters: AppInputFormatters.soloLetras(50),
            decoration: AppTheme.pillInputDecoration(
              hintText: 'Apellido paterno',
              prefixIcon: Icons.badge_outlined,
            ),
            validator: (v) => (v == null || !_reNombre.hasMatch(v.trim()))
                ? 'Solo letras (2 a 50)'
                : null,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _maternoCtrl,
            enabled: !isLoading && !_validandoPaso,
            inputFormatters: AppInputFormatters.soloLetras(50),
            decoration: AppTheme.pillInputDecoration(
              hintText: 'Apellido materno',
              prefixIcon: Icons.badge_outlined,
            ),
            validator: (v) => (v == null || !_reNombre.hasMatch(v.trim()))
                ? 'Solo letras (2 a 50)'
                : null,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _fechaNacCtrl,
            readOnly: true,
            enabled: !isLoading && !_validandoPaso,
            onTap: _seleccionarFecha,
            decoration: AppTheme.pillInputDecoration(
              hintText: 'Fecha de nacimiento (YYYY-MM-DD)',
              prefixIcon: Icons.calendar_today_outlined,
            ),
            validator: (v) {
              if (v == null || v.trim().isEmpty || _fechaNacimiento == null) {
                return 'Selecciona tu fecha de nacimiento';
              }
              return null;
            },
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _DropdownCampo(
                  label: 'Sexo',
                  value: _sexo,
                  items: const [
                    DropdownMenuItem(value: 'H', child: Text('H')),
                    DropdownMenuItem(value: 'M', child: Text('M')),
                    DropdownMenuItem(
                        value: 'Indefinido', child: Text('Indefinido')),
                  ],
                  onChanged: (isLoading || _validandoPaso)
                      ? null
                      : (v) => setState(() => _sexo = v),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _DropdownCampo(
                  label: 'Estado civil',
                  value: _estadoCivil,
                  items: const [
                    DropdownMenuItem(value: 'SOLTERO', child: Text('Soltero/a')),
                    DropdownMenuItem(value: 'CASADO', child: Text('Casado/a')),
                    DropdownMenuItem(
                        value: 'DIVORCIADO', child: Text('Divorciado/a')),
                    DropdownMenuItem(value: 'VIUDO', child: Text('Viudo/a')),
                    DropdownMenuItem(
                        value: 'UNION_LIBRE', child: Text('Unión libre')),
                  ],
                  onChanged: (isLoading || _validandoPaso)
                      ? null
                      : (v) => setState(() => _estadoCivil = v),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _curpCtrl,
            enabled: !isLoading && !_validandoPaso,
            inputFormatters: AppInputFormatters.curp,
            onChanged: (_) {
              if (_errorCurpDuplicada != null) {
                setState(() => _errorCurpDuplicada = null);
              }
            },
            decoration: AppTheme.pillInputDecoration(
              hintText: 'CURP (18 caracteres)',
              prefixIcon: Icons.fingerprint_rounded,
              errorText: _errorCurpDuplicada,
            ),
            validator: (v) =>
                (v == null || !_reCurp.hasMatch(v.trim().toUpperCase()))
                    ? 'Formato: 4 letras + 6 números + H/M + 5 letras + 2 homoclave'
                    : null,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _rfcCtrl,
            enabled: !isLoading && !_validandoPaso,
            inputFormatters: AppInputFormatters.rfc,
            onChanged: (_) {
              if (_errorRfcDuplicado != null) {
                setState(() => _errorRfcDuplicado = null);
              }
            },
            decoration: AppTheme.pillInputDecoration(
              hintText: 'RFC (12 o 13 caracteres)',
              prefixIcon: Icons.assignment_ind_outlined,
              errorText: _errorRfcDuplicado,
            ),
            validator: (v) =>
                (v == null || !_reRfc.hasMatch(v.trim().toUpperCase()))
                    ? 'Formato: 4 letras + 6 números (AAMMDD) + 3 homoclave'
                    : null,
          ),
          const SizedBox(height: 22),
          Align(
            alignment: Alignment.centerRight,
            child: PillActionButton(
              label: 'Continuar',
              loading: _validandoPaso,
              onPressed: isLoading ? null : _siguientePaso,
            ),
          ),
        ],
      ),
    );
  }

  Widget _construirPaso2(bool isLoading) {
    final esEstudiante = _ocupacionSeleccionada == 'Estudiante';
    return Form(
      key: _formPaso2,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextFormField(
            controller: _correoCtrl,
            enabled: !isLoading && !_validandoPaso,
            keyboardType: TextInputType.emailAddress,
            inputFormatters: AppInputFormatters.correo,
            onChanged: (v) {
              final norm = v.trim().toLowerCase();
              setState(() {
                _errorCorreoDuplicado = null;
                if (norm != _correoEnviadoPara) {
                  _correoVerificado = false;
                  _codigoEnviado = false;
                  _errorCodigoCorreo = null;
                  _avisoCodigoCorreo = null;
                  _codigoCorreoCtrl.clear();
                }
              });
            },
            decoration: AppTheme.pillInputDecoration(
              hintText: 'Correo electrónico',
              helperText:
                  'Aceptamos Gmail, Outlook, Hotmail, Yahoo, institucional, etc.',
              prefixIcon: Icons.mail_outline_rounded,
              errorText: _errorCorreoDuplicado,
              suffixIcon: _correoVerificado
                  ? const Icon(Icons.verified_rounded,
                      color: AppColors.brand, size: 20)
                  : null,
            ),
            validator: (v) {
              final t = v?.trim() ?? '';
              if (!_reEmail.hasMatch(t)) {
                return 'Ingresa un correo electrónico válido';
              }
              return null;
            },
          ),
          if (_codigoEnviado && !_correoVerificado) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppColors.brandSoft,
                borderRadius: BorderRadius.circular(18),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (_avisoCodigoCorreo != null)
                    Text(
                      _avisoCodigoCorreo!,
                      style: const TextStyle(
                        color: AppColors.brand,
                        fontSize: 12.5,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: TextFormField(
                          controller: _codigoCorreoCtrl,
                          enabled: !_confirmandoCodigo,
                          keyboardType: TextInputType.number,
                          maxLength: 6,
                          inputFormatters: [
                            FilteringTextInputFormatter.digitsOnly,
                          ],
                          decoration: AppTheme.pillInputDecoration(
                            hintText: 'Código de 6 dígitos',
                            counterText: '',
                            errorText: _errorCodigoCorreo,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      FilledButton(
                        style: FilledButton.styleFrom(
                          minimumSize: const Size(0, 48),
                          padding: const EdgeInsets.symmetric(horizontal: 16),
                        ),
                        onPressed:
                            _confirmandoCodigo ? null : _confirmarCodigoCorreo,
                        child: _confirmandoCodigo
                            ? const SizedBox(
                                width: 16,
                                height: 16,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : const Text('Validar'),
                      ),
                    ],
                  ),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton(
                      onPressed: _confirmandoCodigo
                          ? null
                          : () => _enviarCodigoCorreo(reenvio: true),
                      child: const Text('Reenviar código'),
                    ),
                  ),
                ],
              ),
            ),
          ],
          const SizedBox(height: 12),
          TextFormField(
            controller: _telMovilCtrl,
            enabled: !isLoading && !_validandoPaso,
            keyboardType: TextInputType.phone,
            inputFormatters: AppInputFormatters.soloDigitos(10),
            onChanged: (_) {
              if (_errorTelefonoDuplicado != null) {
                setState(() => _errorTelefonoDuplicado = null);
              }
            },
            decoration: AppTheme.pillInputDecoration(
              hintText: 'Teléfono móvil (10 dígitos)',
              prefixIcon: Icons.phone_outlined,
              errorText: _errorTelefonoDuplicado,
            ),
            validator: (v) => (v == null || !_reTel.hasMatch(v.trim()))
                ? '10 dígitos numéricos'
                : null,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _telAltCtrl,
            enabled: !isLoading && !_validandoPaso,
            keyboardType: TextInputType.phone,
            inputFormatters: AppInputFormatters.soloDigitos(10),
            decoration: AppTheme.pillInputDecoration(
              hintText: 'Teléfono alternativo (opcional)',
              prefixIcon: Icons.phone_outlined,
            ),
            validator: (v) {
              final t = v?.trim() ?? '';
              if (t.isNotEmpty && !_reTel.hasMatch(t)) {
                return '10 dígitos numéricos';
              }
              return null;
            },
          ),
          const SizedBox(height: 12),
          _DropdownCampo(
            label: 'Ocupación',
            hintText: 'Selecciona una ocupación',
            value: _ocupacionSeleccionada,
            items: [
              ..._ocupaciones.map(
                (oc) => DropdownMenuItem(
                  value: oc,
                  child: Text(oc, overflow: TextOverflow.ellipsis),
                ),
              ),
              const DropdownMenuItem(
                value: 'Otro',
                child: Text('Otro (especificar)'),
              ),
            ],
            onChanged: (isLoading || _validandoPaso)
                ? null
                : (v) {
                    setState(() {
                      _ocupacionSeleccionada = v;
                      if (v != 'Otro') _ocupacionOtraCtrl.clear();
                      if (v == 'Estudiante') _empresaCtrl.clear();
                    });
                  },
            validator: (v) =>
                (v == null || v.isEmpty) ? 'Selecciona una ocupación' : null,
          ),
          if (_ocupacionSeleccionada == 'Otro') ...[
            const SizedBox(height: 12),
            TextFormField(
              controller: _ocupacionOtraCtrl,
              enabled: !isLoading && !_validandoPaso,
              inputFormatters: AppInputFormatters.soloLetras(100),
              decoration: AppTheme.pillInputDecoration(
                hintText: 'Especifica tu ocupación',
                prefixIcon: Icons.work_outline_rounded,
              ),
              validator: (v) =>
                  (v == null || !_reOcupacion.hasMatch(v.trim()))
                      ? 'Solo letras (2 a 100 caracteres)'
                      : null,
            ),
          ],
          const SizedBox(height: 12),
          TextFormField(
            controller: _empresaCtrl,
            enabled: !isLoading && !_validandoPaso && !esEstudiante,
            inputFormatters: AppInputFormatters.textoLimpio(100),
            decoration: AppTheme.pillInputDecoration(
              hintText:
                  esEstudiante ? 'No aplica para estudiantes' : 'Empresa',
              prefixIcon: Icons.business_outlined,
              enabled: !esEstudiante,
            ),
            validator: esEstudiante
                ? null
                : (v) => (v == null || v.trim().isEmpty)
                    ? 'Ingresa el nombre de tu empresa'
                    : null,
          ),
          const SizedBox(height: 12),
          _DropdownCampo(
            label: 'Ingreso mensual (MXN)',
            hintText: 'Selecciona un rango de ingresos',
            value: _ingresoSeleccionado?.toStringAsFixed(0),
            items: rangosIngresoMensual
                .map(
                  (r) => DropdownMenuItem(
                    value: r.valor.toStringAsFixed(0),
                    child: Text(r.etiqueta, overflow: TextOverflow.ellipsis),
                  ),
                )
                .toList(),
            onChanged: (isLoading || _validandoPaso)
                ? null
                : (v) => setState(
                      () => _ingresoSeleccionado = double.tryParse(v),
                    ),
            validator: (v) => (v == null || v.isEmpty)
                ? 'Selecciona un rango de ingreso mensual'
                : null,
          ),
          const SizedBox(height: 22),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              OutlinedButton(
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size(0, 48),
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                ),
                onPressed: _validandoPaso
                    ? null
                    : () => setState(() {
                          _errorPaso = null;
                          _paso = 1;
                        }),
                child: const Text('Atrás'),
              ),
              PillActionButton(
                label: _codigoEnviado && !_correoVerificado
                    ? 'Verificar y continuar'
                    : 'Continuar',
                loading: _validandoPaso || _confirmandoCodigo,
                onPressed: isLoading ? null : _siguientePaso,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _construirPaso3(bool isLoading) {
    final ubicaciones = _ubicacionesEstadoActual;
    return Form(
      key: _formPaso3,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextFormField(
            controller: _cpCtrl,
            enabled: !isLoading,
            keyboardType: TextInputType.number,
            inputFormatters: AppInputFormatters.soloDigitos(5),
            onChanged: _alCambiarCodigoPostal,
            decoration: AppTheme.pillInputDecoration(
              hintText: 'Código postal (autocompletar)',
              prefixIcon: Icons.location_on_outlined,
              helperText: _avisoCp,
              suffixIcon: _buscandoCp
                  ? const Padding(
                      padding: EdgeInsets.all(14),
                      child: SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                    )
                  : null,
            ),
            validator: (v) => (v == null || v.trim().length != 5)
                ? 'Código postal de 5 dígitos'
                : null,
          ),
          const SizedBox(height: 12),
          _DropdownCampo(
            label: 'Estado',
            value: _estadoSeleccionado,
            items: _estados
                .map((e) => DropdownMenuItem(value: e, child: Text(e)))
                .toList(),
            onChanged: isLoading
                ? null
                : (v) {
                    setState(() => _avisoCp = null);
                    _cargarUbicacionesEstado(v);
                  },
          ),
          const SizedBox(height: 12),
          _DropdownCampo(
            label: 'Municipio · CP',
            value: _ubicacionSeleccionada.clave,
            items: ubicaciones
                .map(
                  (u) => DropdownMenuItem(
                    value: u.clave,
                    child: Text('${u.municipio} (${u.codigoPostal})'),
                  ),
                )
                .toList(),
            onChanged: isLoading
                ? null
                : (v) {
                    final match =
                        ubicaciones.firstWhere((u) => u.clave == v);
                    setState(() {
                      _ubicacionSeleccionada = match;
                      _cpCtrl.text = match.codigoPostal;
                      _avisoCp = null;
                    });
                    _cargarColonias(match.codigoPostal);
                  },
          ),
          const SizedBox(height: 12),
          _DropdownCampo(
            label: 'Colonia',
            hintText: 'Selecciona una colonia',
            value: _coloniaSeleccionada,
            items: [
              ..._colonias.map(
                (col) => DropdownMenuItem(
                  value: col,
                  child: Text(col, overflow: TextOverflow.ellipsis),
                ),
              ),
              const DropdownMenuItem(
                value: 'Otra',
                child: Text('Otra (escribir colonia)'),
              ),
            ],
            onChanged: isLoading
                ? null
                : (v) {
                    setState(() {
                      _coloniaSeleccionada = v;
                      if (v != 'Otra') _coloniaOtraCtrl.clear();
                    });
                  },
            validator: (v) =>
                (v == null || v.isEmpty) ? 'Selecciona una colonia' : null,
          ),
          if (_coloniaSeleccionada == 'Otra') ...[
            const SizedBox(height: 12),
            TextFormField(
              controller: _coloniaOtraCtrl,
              enabled: !isLoading,
              inputFormatters: AppInputFormatters.textoLimpio(100),
              decoration: AppTheme.pillInputDecoration(
                hintText: 'Nombre de tu colonia',
                prefixIcon: Icons.map_outlined,
              ),
              validator: (v) => (v == null || v.trim().isEmpty)
                  ? 'Ingresa el nombre de tu colonia'
                  : null,
            ),
          ],
          const SizedBox(height: 12),
          TextFormField(
            controller: _calleCtrl,
            enabled: !isLoading,
            inputFormatters: AppInputFormatters.textoLimpio(100),
            decoration: AppTheme.pillInputDecoration(
              hintText: 'Calle',
              prefixIcon: Icons.signpost_outlined,
            ),
            validator: (v) => (v == null || v.trim().isEmpty)
                ? 'Ingresa el nombre de la calle'
                : null,
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: TextFormField(
                  controller: _numExtCtrl,
                  enabled: !isLoading,
                  inputFormatters: AppInputFormatters.numeroDomicilio,
                  decoration: AppTheme.pillInputDecoration(
                    hintText: 'Núm. ext. (máx. 6)',
                  ),
                  validator: (v) =>
                      (v == null || !_reNumDomicilio.hasMatch(v.trim()))
                          ? 'Máx. 6 car. (A-Z, 0-9, -, /)'
                          : null,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextFormField(
                  controller: _numIntCtrl,
                  enabled: !isLoading,
                  inputFormatters: AppInputFormatters.numeroDomicilio,
                  decoration: AppTheme.pillInputDecoration(
                    hintText: 'Interior (opc., máx. 6)',
                  ),
                  validator: (v) {
                    final t = v?.trim() ?? '';
                    if (t.isNotEmpty && !_reNumDomicilio.hasMatch(t)) {
                      return 'Máx. 6 car. (A-Z, 0-9, -, /)';
                    }
                    return null;
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 22),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              OutlinedButton(
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size(0, 48),
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                ),
                onPressed: () => setState(() => _paso = 2),
                child: const Text('Atrás'),
              ),
              PillActionButton(
                label: 'Continuar',
                onPressed: _siguientePaso,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _construirPaso4(bool isLoading) {
    final pass = _passCtrl.text;
    final tieneLen = pass.length >= 8;
    final tieneMayusMinus =
        RegExp(r'[a-z]').hasMatch(pass) && RegExp(r'[A-Z]').hasMatch(pass);
    final tieneNum = RegExp(r'[0-9]').hasMatch(pass);
    final tieneSimbolo = RegExp(r'[@#$%^&+=!._-]').hasMatch(pass);

    return Form(
      key: _formPaso4,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextFormField(
            controller: _passCtrl,
            obscureText: _ocultarPass,
            enabled: !isLoading,
            inputFormatters: AppInputFormatters.password,
            decoration: AppTheme.pillInputDecoration(
              hintText: 'Contraseña',
              prefixIcon: Icons.lock_outline_rounded,
              suffixIcon: IconButton(
                icon: Icon(
                  _ocultarPass
                      ? Icons.visibility_outlined
                      : Icons.visibility_off_outlined,
                  size: 20,
                  color: AppColors.ink3,
                ),
                onPressed: () => setState(() => _ocultarPass = !_ocultarPass),
              ),
            ),
            validator: (v) => (v == null || !_rePass.hasMatch(v))
                ? 'La contraseña no cumple los requisitos'
                : null,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _confirmCtrl,
            obscureText: _ocultarConfirm,
            enabled: !isLoading,
            inputFormatters: AppInputFormatters.password,
            decoration: AppTheme.pillInputDecoration(
              hintText: 'Confirmar contraseña',
              prefixIcon: Icons.lock_outline_rounded,
              suffixIcon: IconButton(
                icon: Icon(
                  _ocultarConfirm
                      ? Icons.visibility_outlined
                      : Icons.visibility_off_outlined,
                  size: 20,
                  color: AppColors.ink3,
                ),
                onPressed: () =>
                    setState(() => _ocultarConfirm = !_ocultarConfirm),
              ),
            ),
            validator: (v) =>
                (v != _passCtrl.text) ? 'Las contraseñas no coinciden' : null,
          ),
          const SizedBox(height: 14),
          _ReglaPass('Mínimo 8 caracteres', tieneLen),
          _ReglaPass('Mayúscula y minúscula', tieneMayusMinus),
          _ReglaPass('Al menos un número', tieneNum),
          _ReglaPass('Un símbolo (@#\$%^&+=!._-)', tieneSimbolo),
          const SizedBox(height: 22),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              OutlinedButton(
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size(0, 48),
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                ),
                onPressed:
                    isLoading ? null : () => setState(() => _paso = 3),
                child: const Text('Atrás'),
              ),
              PillActionButton(
                label: 'Crear cuenta',
                loading: isLoading,
                onPressed: _enviarRegistro,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _BarraPasos extends StatelessWidget {
  final int pasoActual;

  const _BarraPasos({required this.pasoActual});

  @override
  Widget build(BuildContext context) {
    const nombres = ['Personal', 'Contacto', 'Domicilio', 'Seguridad'];
    return Row(
      children: [
        for (var i = 0; i < nombres.length; i++) ...[
          if (i > 0) const SizedBox(width: 6),
          Expanded(
            child: Column(
              children: [
                Text(
                  '${i + 1}. ${nombres[i]}',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 11.5,
                    fontWeight: FontWeight.w600,
                    color: (i + 1) == pasoActual
                        ? AppColors.brand
                        : AppColors.ink2,
                  ),
                ),
                const SizedBox(height: 6),
                Container(
                  height: 3,
                  decoration: BoxDecoration(
                    color: (i + 1) <= pasoActual
                        ? AppColors.brand
                        : AppColors.line,
                    borderRadius: BorderRadius.circular(99),
                  ),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }
}

class _DropdownCampo extends StatelessWidget {
  final String label;
  final String? hintText;
  final String? value;
  final List<DropdownMenuItem<String>> items;
  final ValueChanged<String>? onChanged;
  final FormFieldValidator<String>? validator;

  const _DropdownCampo({
    required this.label,
    required this.value,
    required this.items,
    required this.onChanged,
    this.hintText,
    this.validator,
  });

  @override
  Widget build(BuildContext context) {
    return FormField<String>(
      key: ValueKey('${label}_${value ?? ''}'),
      initialValue: value,
      validator: validator,
      builder: (fieldState) => InputDecorator(
        decoration: AppTheme.pillInputDecoration(
          hintText: hintText ?? label,
          labelText: label,
          errorText: fieldState.errorText,
        ),
        child: DropdownButtonHideUnderline(
          child: DropdownButton<String>(
            value: value,
            hint: hintText != null
                ? Text(
                    hintText!,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(color: AppColors.ink3, fontSize: 14.5),
                  )
                : null,
            isExpanded: true,
            isDense: true,
            items: items,
            onChanged: onChanged == null
                ? null
                : (v) {
                    fieldState.didChange(v);
                    if (v != null) onChanged!(v);
                  },
          ),
        ),
      ),
    );
  }
}

class _ReglaPass extends StatelessWidget {
  final String texto;
  final bool cumplida;

  const _ReglaPass(this.texto, this.cumplida);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4),
      child: Row(
        children: [
          Icon(
            cumplida ? Icons.check_circle_rounded : Icons.circle_outlined,
            size: 15,
            color: cumplida ? AppColors.brand : AppColors.ink3,
          ),
          const SizedBox(width: 6),
          Text(
            texto,
            style: TextStyle(
              fontSize: 12.5,
              color: cumplida ? AppColors.brand : AppColors.ink2,
              fontWeight: cumplida ? FontWeight.w600 : FontWeight.normal,
            ),
          ),
        ],
      ),
    );
  }
}

class _VistaExito extends StatelessWidget {
  final RegistroClienteResult resultado;
  final VoidCallback onContinuar;

  const _VistaExito({required this.resultado, required this.onContinuar});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(22),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: AppColors.brandSoft,
                          borderRadius: BorderRadius.circular(999),
                        ),
                        child: const Text(
                          'Cuenta creada',
                          style: TextStyle(
                            color: AppColors.brand,
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    const Text(
                      'Registro completado',
                      style:
                          TextStyle(fontSize: 22, fontWeight: FontWeight.w800),
                    ),
                    const SizedBox(height: 16),
                    _FilaResumen('Titular', resultado.nombreCompleto),
                    const Divider(),
                    _FilaResumen('Correo', resultado.correo),
                    const Divider(),
                    _FilaResumen('Cuenta', resultado.numeroCuenta),
                    const Divider(),
                    const _FilaResumen(
                      'Saldo inicial',
                      '\$1,000.00 MXN',
                      resaltar: true,
                    ),
                    const SizedBox(height: 22),
                    Align(
                      alignment: Alignment.centerRight,
                      child: PillActionButton(
                        label: 'Iniciar sesión',
                        icon: Icons.login_rounded,
                        onPressed: onContinuar,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _FilaResumen extends StatelessWidget {
  final String etiqueta;
  final String valor;
  final bool resaltar;

  const _FilaResumen(this.etiqueta, this.valor, {this.resaltar = false});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(etiqueta, style: const TextStyle(color: AppColors.ink2)),
          const SizedBox(width: 12),
          Flexible(
            child: Text(
              valor,
              textAlign: TextAlign.right,
              style: TextStyle(
                fontWeight: FontWeight.w700,
                color: resaltar ? AppColors.brand : AppColors.ink,
                fontFeatures: const [FontFeature.tabularFigures()],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
