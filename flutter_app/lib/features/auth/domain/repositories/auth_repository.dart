import '../../data/models/registro_cliente_model.dart';
import '../entities/auth_result_entity.dart';

abstract class AuthRepository {
  Future<AuthResultEntity> login({
    required String correo,
    required String password,
    bool forzarCierreSesionAnterior = false,
  });

  Future<AuthResultEntity> verificarOtp({
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

  Future<String?> obtenerUsuarioAutenticado();

  Future<bool> tieneSesionLocalVigente();
}

