import 'package:equatable/equatable.dart';

class AuthResultEntity extends Equatable {
  final String? token;
  final String correo;
  final bool requiere2fa;
  final bool sesionActivaDetectada;
  final String? dispositivoSesionActiva;
  final int? tiempoExpiracionOtpSegundos;
  final String? mensaje;

  const AuthResultEntity({
    this.token,
    required this.correo,
    this.requiere2fa = false,
    this.sesionActivaDetectada = false,
    this.dispositivoSesionActiva,
    this.tiempoExpiracionOtpSegundos,
    this.mensaje,
  });

  @override
  List<Object?> get props => [
        token,
        correo,
        requiere2fa,
        sesionActivaDetectada,
        dispositivoSesionActiva,
        tiempoExpiracionOtpSegundos,
        mensaje,
      ];
}

