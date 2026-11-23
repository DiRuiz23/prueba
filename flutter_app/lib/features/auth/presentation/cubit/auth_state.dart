import 'package:equatable/equatable.dart';

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

  const AuthOtpRequired({
    required this.correo,
    required this.segundosExpiracion,
    required this.mensaje,
  });

  @override
  List<Object?> get props => [correo, segundosExpiracion, mensaje];
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

