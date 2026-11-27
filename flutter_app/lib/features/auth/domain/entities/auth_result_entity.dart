import 'package:equatable/equatable.dart';

class AuthResultEntity extends Equatable {
  final String? token;
  final String correo;
  final bool requiere2fa;
  final String metodo2fa;
  final bool requiereConfiguracionTotp;
  final String? totpSecreto;
  final String? totpOtpAuthUri;
  final String? totpQrBase64;
  final bool sesionActivaDetectada;
  final String? dispositivoSesionActiva;
  final int? tiempoExpiracionOtpSegundos;
  final String? mensaje;

  const AuthResultEntity({
    this.token,
    required this.correo,
    this.requiere2fa = false,
    this.metodo2fa = 'GOOGLE_AUTHENTICATOR',
    this.requiereConfiguracionTotp = false,
    this.totpSecreto,
    this.totpOtpAuthUri,
    this.totpQrBase64,
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
        metodo2fa,
        requiereConfiguracionTotp,
        totpSecreto,
        totpOtpAuthUri,
        totpQrBase64,
        sesionActivaDetectada,
        dispositivoSesionActiva,
        tiempoExpiracionOtpSegundos,
        mensaje,
      ];
}

