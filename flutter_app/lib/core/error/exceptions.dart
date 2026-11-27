class ServerException implements Exception {
  final String message;
  final int? statusCode;
  final dynamic data;

  const ServerException({
    required this.message,
    this.statusCode,
    this.data,
  });
}

class NetworkTimeoutException implements Exception {
  final String message;

  const NetworkTimeoutException([
    this.message = 'Tiempo de espera agotado al conectar con el servidor.',
  ]);
}

class NetworkConnectionException implements Exception {
  final String message;

  const NetworkConnectionException([
    this.message = 'No hay conexión con el servidor. Verifica tu red.',
  ]);
}

class UnauthorizedException implements Exception {
  final String message;

  const UnauthorizedException([
    this.message = 'Sesión inválida, expirada o finalizada en otro dispositivo.',
  ]);
}

