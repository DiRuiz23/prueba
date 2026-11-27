import 'package:equatable/equatable.dart';
import '../../data/models/registro_cliente_model.dart';

abstract class AuthState extends Equatable {
  const AuthState();

  @override
  List<Object?> get props => [];
}

class AuthInitial extends AuthState {
  const AuthInitial();
}

class AuthLoading extends AuthState {
  final String mensaje;

  const AuthLoading([this.mensaje = 'Procesando...']);

  @override
  List<Object?> get props => [mensaje];
}

class AuthActiveSessionConflict extends AuthState {
  final String correo;
  final String password;
  final String dispositivoActivo;
  final String mensaje;

  const AuthActiveSessionConflict({
    required this.correo,
    required this.password,
    required this.dispositivoActivo,
    required this.mensaje,
  });

  @override
  List<Object?> get props => [correo, password, dispositivoActivo, mensaje];
}

class AuthOtpRequired extends AuthState {
  final String correo;
  final int segundosExpiracion;
  final String mensaje;
  final String metodo2fa;
  final bool requiereConfiguracionTotp;
  final String? totpSecreto;
  final String? totpOtpAuthUri;
  final String? totpQrBase64;

  const AuthOtpRequired({
    required this.correo,
    required this.segundosExpiracion,
    required this.mensaje,
    this.metodo2fa = 'GOOGLE_AUTHENTICATOR',
    this.requiereConfiguracionTotp = false,
    this.totpSecreto,
    this.totpOtpAuthUri,
    this.totpQrBase64,
  });

  @override
  List<Object?> get props => [
        correo,
        segundosExpiracion,
        mensaje,
        metodo2fa,
        requiereConfiguracionTotp,
        totpSecreto,
        totpOtpAuthUri,
        totpQrBase64,
      ];
}

class AuthAuthenticated extends AuthState {
  final String correo;

  const AuthAuthenticated({required this.correo});

  @override
  List<Object?> get props => [correo];
}

class AuthUnauthenticated extends AuthState {
  final String? motivo;

  const AuthUnauthenticated([this.motivo]);

  @override
  List<Object?> get props => [motivo];
}

class AuthError extends AuthState {
  final String mensaje;
  final bool duranteOtp;
  final String? correoEnProceso;

  const AuthError({
    required this.mensaje,
    this.duranteOtp = false,
    this.correoEnProceso,
  });

  @override
  List<Object?> get props => [mensaje, duranteOtp, correoEnProceso];
}

class AuthRegistroExitoso extends AuthState {
  final RegistroClienteResult resultado;

  const AuthRegistroExitoso(this.resultado);

  @override
  List<Object?> get props => [resultado];
}

