import '../../../../core/error/exceptions.dart';
import '../../../../core/network/dio_client.dart';
import '../models/auth_models.dart';
import '../models/registro_cliente_model.dart';

abstract class AuthRemoteDataSource {
  Future<LoginResponseModel> login({
    required String correo,
    required String password,
    required bool forzarCierreSesionAnterior,
  });

  Future<LoginResponseModel> verificarOtp({
    required String correo,
    required String codigo,
  });

  Future<RegistroClienteResult> registrarCliente(RegistroClienteParams params);

  Future<List<CatalogoPostalItem>> obtenerUbicacionesPorEstado(String estado);

  Future<List<CatalogoPostalItem>> buscarPorCodigoPostal(String codigoPostal);

  Future<List<String>> obtenerOcupaciones();

  Future<List<String>> obtenerColoniasPorCp(String codigoPostal);

  Future<bool> obtenerEstadoTotp();

  Future<TotpConfigResult> configurarTotp();

  Future<bool> activarTotp(String codigo);

  Future<bool> desactivarTotp(String password);

  Future<DisponibilidadRegistroResult> verificarDisponibilidad({
    String? curp,
    String? rfc,
    String? correo,
    String? telefonoMovil,
  });

  Future<int> enviarCodigoVerificacionCorreo(String correo);

  Future<bool> confirmarCodigoVerificacionCorreo(String correo, String codigo);

  Future<void> logout();
}

class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
  final DioClient dioClient;

  AuthRemoteDataSourceImpl({required this.dioClient});

  @override
  Future<LoginResponseModel> login({
    required String correo,
    required String password,
    required bool forzarCierreSesionAnterior,
  }) async {
    final response = await dioClient.post(
      '/auth/login',
      data: {
        'correo': correo.trim().toLowerCase(),
        'password': password,
        'dispositivo': 'MOVIL',
        'forzarCierreSesionAnterior': forzarCierreSesionAnterior,
      },
    );

    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    if (datos == null) {
      throw ServerException(
        message: (body['mensaje'] as String?) ?? 'Respuesta inválida de autenticación',
        statusCode: response.statusCode,
      );
    }
    return LoginResponseModel.fromJson(datos);
  }

  @override
  Future<LoginResponseModel> verificarOtp({
    required String correo,
    required String codigo,
  }) async {
    final response = await dioClient.post(
      '/auth/verificar-2fa',
      data: {
        'correo': correo.trim().toLowerCase(),
        'codigo': codigo.trim(),
        'dispositivo': 'MOVIL',
      },
    );

    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    if (datos == null) {
      throw ServerException(
        message: (body['mensaje'] as String?) ?? 'Respuesta inválida al validar 2FA',
        statusCode: response.statusCode,
      );
    }
    return LoginResponseModel.fromJson(datos);
  }

  @override
  Future<RegistroClienteResult> registrarCliente(RegistroClienteParams params) async {
    final response = await dioClient.post(
      '/clientes',
      data: params.toJson(),
    );

    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    if (datos == null) {
      throw ServerException(
        message: (body['mensaje'] as String?) ?? 'Respuesta inválida al registrar cliente',
        statusCode: response.statusCode,
      );
    }
    return RegistroClienteResult.fromJson(datos);
  }

  @override
  Future<List<CatalogoPostalItem>> obtenerUbicacionesPorEstado(String estado) async {
    final response = await dioClient.get(
      '/catalogos/ubicaciones',
      queryParameters: {'estado': estado},
    );
    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as List<dynamic>? ?? [];
    return datos
        .map((e) => CatalogoPostalItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<List<CatalogoPostalItem>> buscarPorCodigoPostal(String codigoPostal) async {
    final response = await dioClient.get('/catalogos/cp/${codigoPostal.trim()}');
    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as List<dynamic>? ?? [];
    return datos
        .map((e) => CatalogoPostalItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<List<String>> obtenerOcupaciones() async {
    final response = await dioClient.get('/catalogos/ocupaciones');
    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as List<dynamic>? ?? [];
    return datos.map((e) => e.toString()).toList();
  }

  @override
  Future<List<String>> obtenerColoniasPorCp(String codigoPostal) async {
    final response = await dioClient.get(
      '/catalogos/colonias',
      queryParameters: {'codigoPostal': codigoPostal.trim()},
    );
    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as List<dynamic>? ?? [];
    return datos.map((e) => e.toString()).toList();
  }

  @override
  Future<bool> obtenerEstadoTotp() async {
    final response = await dioClient.get('/seguridad/totp/estado');
    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    return (datos?['habilitado'] as bool?) ?? false;
  }

  @override
  Future<TotpConfigResult> configurarTotp() async {
    final response = await dioClient.post('/seguridad/totp/configurar');
    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    if (datos == null) {
      throw ServerException(
        message: (body['mensaje'] as String?) ?? 'No se pudo generar clave TOTP',
        statusCode: response.statusCode,
      );
    }
    return TotpConfigResult.fromJson(datos);
  }

  @override
  Future<bool> activarTotp(String codigo) async {
    final response = await dioClient.post(
      '/seguridad/totp/activar',
      data: {'codigo': codigo.trim()},
    );
    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    return (datos?['habilitado'] as bool?) ?? false;
  }

  @override
  Future<bool> desactivarTotp(String password) async {
    final response = await dioClient.post(
      '/seguridad/totp/desactivar',
      data: {'password': password},
    );
    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    return (datos?['habilitado'] as bool?) ?? false;
  }

  @override
  Future<DisponibilidadRegistroResult> verificarDisponibilidad({
    String? curp,
    String? rfc,
    String? correo,
    String? telefonoMovil,
  }) async {
    final query = <String, dynamic>{};
    if (curp != null && curp.trim().isNotEmpty) {
      query['curp'] = curp.trim().toUpperCase();
    }
    if (rfc != null && rfc.trim().isNotEmpty) {
      query['rfc'] = rfc.trim().toUpperCase();
    }
    if (correo != null && correo.trim().isNotEmpty) {
      query['correo'] = correo.trim().toLowerCase();
    }
    if (telefonoMovil != null && telefonoMovil.trim().isNotEmpty) {
      query['telefonoMovil'] = telefonoMovil.trim();
    }

    final response = await dioClient.get(
      '/clientes/disponibilidad',
      queryParameters: query,
    );
    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    if (datos == null) {
      throw ServerException(
        message: (body['mensaje'] as String?) ?? 'No se pudo verificar disponibilidad',
        statusCode: response.statusCode,
      );
    }
    return DisponibilidadRegistroResult.fromJson(datos);
  }

  @override
  Future<int> enviarCodigoVerificacionCorreo(String correo) async {
    final response = await dioClient.post(
      '/clientes/verificar-correo/enviar',
      data: {'correo': correo.trim().toLowerCase()},
    );
    final body = response.data as Map<String, dynamic>;
    final datos = body['datos'] as Map<String, dynamic>?;
    return (datos?['segundosExpiracion'] as num?)?.toInt() ?? 300;
  }

  @override
  Future<bool> confirmarCodigoVerificacionCorreo(
    String correo,
    String codigo,
  ) async {
    final response = await dioClient.post(
      '/clientes/verificar-correo/confirmar',
      data: {
        'correo': correo.trim().toLowerCase(),
        'codigo': codigo.trim(),
      },
    );
    final body = response.data as Map<String, dynamic>;
    return (body['datos'] as bool?) ?? false;
  }

  @override
  Future<void> logout() async {
    try {
      await dioClient.post('/auth/logout');
    } catch (_) {
      // Aun si falla la red al cerrar sesión, la sesión local debe limpiarse.
    }
  }
}

