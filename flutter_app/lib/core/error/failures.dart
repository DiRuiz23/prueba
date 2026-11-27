import 'package:equatable/equatable.dart';

abstract class Failure extends Equatable {
  final String message;
  final int? statusCode;

  const Failure(this.message, [this.statusCode]);

  @override
  List<Object?> get props => [message, statusCode];
}

class ServerFailure extends Failure {
  const ServerFailure(super.message, [super.statusCode]);
}

class TimeoutFailure extends Failure {
  const TimeoutFailure([
    super.message = 'Tiempo de espera agotado. Verifica el estado real antes de reintentar.',
  ]);
}

class ConnectionFailure extends Failure {
  const ConnectionFailure([
    super.message = 'Error de comunicación con el servidor.',
  ]);
}

class UnauthorizedFailure extends Failure {
  const UnauthorizedFailure([
    String message = 'Tu sesión expiró o fue cerrada por otro dispositivo.',
  ]) : super(message, 401);
}

