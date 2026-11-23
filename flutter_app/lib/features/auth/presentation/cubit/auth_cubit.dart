import 'dart:async';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../../../../core/error/exceptions.dart';
import '../../domain/repositories/auth_repository.dart';
import '../../domain/usecases/auth_usecases.dart';
import 'auth_state.dart';

class AuthCubit extends Cubit<AuthState> {
  final LoginUseCase loginUseCase;
  final VerificarOtpUseCase verificarOtpUseCase;
  final LogoutUseCase logoutUseCase;
  final AuthRepository authRepository;
  final Stream<String>? sessionExpiredStream;

  StreamSubscription<String>? _sessionSub;

  AuthCubit({
    required this.loginUseCase,
    required this.verificarOtpUseCase,
    required this.logoutUseCase,
    required this.authRepository,
    this.sessionExpiredStream,
  }) : super(const AuthInitial()) {
    _sessionSub = sessionExpiredStream?.listen((motivo) {
      emit(AuthUnauthenticated(motivo));
    });
  }

  Future<void> verificarSesionInicial() async {
    final vigente = await authRepository.tieneSesionLocalVigente();
    if (vigente) {
      final correo = await authRepository.obtenerUsuarioAutenticado();
      if (correo != null && correo.isNotEmpty) {
        emit(AuthAuthenticated(correo: correo));
        return;
      }
    }
    emit(const AuthUnauthenticated());
  }

  Future<void> iniciarSesion({
    required String correo,
    required String password,
    bool forzarCierre = false,
  }) async {
    if (state is AuthLoading) return;
    emit(const AuthLoading('Validando credenciales...'));

    try {
      final res = await loginUseCase(
        correo: correo,
        password: password,
        forzarCierreSesionAnterior: forzarCierre,
      );

      if (res.sesionActivaDetectada) {
        emit(AuthActiveSessionConflict(
          correo: res.correo,
          password: password,
          dispositivoActivo: res.dispositivoSesionActiva ?? 'OTRO DISPOSITIVO',
          mensaje: res.mensaje ??
              'Ya existe una sesión activa. ¿Deseas cerrarla para continuar?',
        ));
        return;
      }

      if (res.requiere2fa) {
        emit(AuthOtpRequired(
          correo: res.correo,
          segundosExpiracion: res.tiempoExpiracionOtpSegundos ?? 300,
          mensaje: res.mensaje ?? 'Código de verificación enviado.',
        ));
        return;
      }

      emit(AuthAuthenticated(correo: res.correo));
    } on ServerException catch (e) {
      emit(AuthError(mensaje: e.message));
    } on NetworkTimeoutException catch (e) {
      emit(AuthError(mensaje: e.message));
    } on NetworkConnectionException catch (e) {
      emit(AuthError(mensaje: e.message));
    } catch (_) {
      emit(const AuthError(mensaje: 'Error inesperado al iniciar sesión.'));
    }
  }

  Future<void> validarOtp({
    required String correo,
    required String codigo,
  }) async {
    if (state is AuthLoading) return;
    emit(const AuthLoading('Verificando segundo factor (2FA)...'));

    try {
      final res = await verificarOtpUseCase(correo: correo, codigo: codigo);
      if (res.token != null && res.token!.isNotEmpty) {
        emit(AuthAuthenticated(correo: res.correo));
      } else {
        emit(AuthError(
          mensaje: res.mensaje ?? 'No se pudo obtener el token de acceso.',
          duranteOtp: true,
          correoEnProceso: correo,
        ));
      }
    } on ServerException catch (e) {
      emit(AuthError(
        mensaje: e.message,
        duranteOtp: true,
        correoEnProceso: correo,
      ));
    } on NetworkTimeoutException catch (e) {
      emit(AuthError(
        mensaje: e.message,
        duranteOtp: true,
        correoEnProceso: correo,
      ));
    } on NetworkConnectionException catch (e) {
      emit(AuthError(
        mensaje: e.message,
        duranteOtp: true,
        correoEnProceso: correo,
      ));
    } catch (_) {
      emit(AuthError(
        mensaje: 'Error inesperado al validar el código OTP.',
        duranteOtp: true,
        correoEnProceso: correo,
      ));
    }
  }

  Future<void> cerrarSesion([String? motivo]) async {
    await logoutUseCase();
    emit(AuthUnauthenticated(motivo));
  }

  void cancelarConflicto() {
    emit(const AuthUnauthenticated());
  }

  @override
  Future<void> close() {
    _sessionSub?.cancel();
    return super.close();
  }
}

