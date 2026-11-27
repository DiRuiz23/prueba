import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../../../../core/config/env_config.dart';
import '../../../../core/network/auth_interceptor.dart';
import '../../domain/entities/auth_result_entity.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_datasource.dart';
import '../models/registro_cliente_model.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource remoteDataSource;
  final FlutterSecureStorage secureStorage;
  final AuthInterceptor? authInterceptor;

  AuthRepositoryImpl({
    required this.remoteDataSource,
    required this.secureStorage,
    this.authInterceptor,
  });

  @override
  Future<AuthResultEntity> login({
    required String correo,
    required String password,
    bool forzarCierreSesionAnterior = false,
  }) {
    return remoteDataSource.login(
      correo: correo,
      password: password,
      forzarCierreSesionAnterior: forzarCierreSesionAnterior,
    );
  }

  @override
  Future<AuthResultEntity> verificarOtp({
    required String correo,
    required String codigo,
  }) async {
    final resultado = await remoteDataSource.verificarOtp(
      correo: correo,
      codigo: codigo,
    );

    if (resultado.token != null && resultado.token!.isNotEmpty) {
      authInterceptor?.actualizarToken(resultado.token);
      await secureStorage.write(
        key: EnvConfig.tokenStorageKey,
        value: resultado.token,
      );
      await secureStorage.write(
        key: EnvConfig.emailStorageKey,
        value: resultado.correo,
      );
    }

    return resultado;
  }

  @override
  Future<RegistroClienteResult> registrarCliente(RegistroClienteParams params) {
    return remoteDataSource.registrarCliente(params);
  }

  @override
  Future<List<CatalogoPostalItem>> obtenerUbicacionesPorEstado(String estado) {
    return remoteDataSource.obtenerUbicacionesPorEstado(estado);
  }

  @override
  Future<List<CatalogoPostalItem>> buscarPorCodigoPostal(String codigoPostal) {
    return remoteDataSource.buscarPorCodigoPostal(codigoPostal);
  }

  @override
  Future<List<String>> obtenerOcupaciones() {
    return remoteDataSource.obtenerOcupaciones();
  }

  @override
  Future<List<String>> obtenerColoniasPorCp(String codigoPostal) {
    return remoteDataSource.obtenerColoniasPorCp(codigoPostal);
  }

  @override
  Future<bool> obtenerEstadoTotp() {
    return remoteDataSource.obtenerEstadoTotp();
  }

  @override
  Future<TotpConfigResult> configurarTotp() {
    return remoteDataSource.configurarTotp();
  }

  @override
  Future<bool> activarTotp(String codigo) {
    return remoteDataSource.activarTotp(codigo);
  }

  @override
  Future<bool> desactivarTotp(String password) {
    return remoteDataSource.desactivarTotp(password);
  }

  @override
  Future<DisponibilidadRegistroResult> verificarDisponibilidad({
    String? curp,
    String? rfc,
    String? correo,
    String? telefonoMovil,
  }) {
    return remoteDataSource.verificarDisponibilidad(
      curp: curp,
      rfc: rfc,
      correo: correo,
      telefonoMovil: telefonoMovil,
    );
  }

  @override
  Future<int> enviarCodigoVerificacionCorreo(String correo) {
    return remoteDataSource.enviarCodigoVerificacionCorreo(correo);
  }

  @override
  Future<bool> confirmarCodigoVerificacionCorreo(String correo, String codigo) {
    return remoteDataSource.confirmarCodigoVerificacionCorreo(correo, codigo);
  }

  @override
  Future<void> logout() async {
    await remoteDataSource.logout();
    authInterceptor?.actualizarToken(null);
    await secureStorage.delete(key: EnvConfig.tokenStorageKey);
    await secureStorage.delete(key: EnvConfig.emailStorageKey);
  }

  @override
  Future<String?> obtenerUsuarioAutenticado() {
    return secureStorage.read(key: EnvConfig.emailStorageKey);
  }

  @override
  Future<bool> tieneSesionLocalVigente() async {
    final token = await secureStorage.read(key: EnvConfig.tokenStorageKey);
    if (token == null || token.isEmpty) {
      return false;
    }
    try {
      final partes = token.split('.');
      if (partes.length != 3) return false;
      final normalized = base64Url.normalize(partes[1]);
      final payloadJson = utf8.decode(base64Url.decode(normalized));
      final payload = jsonDecode(payloadJson) as Map<String, dynamic>;
      final exp = payload['exp'] as int?;
      final vigente = exp == null || DateTime.now().millisecondsSinceEpoch < exp * 1000;
      if (vigente) {
        authInterceptor?.actualizarToken(token);
      }
      return vigente;
    } catch (_) {
      return false;
    }
  }
}

