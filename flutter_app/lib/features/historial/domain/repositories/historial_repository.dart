import '../entities/historial_page_entity.dart';

abstract class HistorialRepository {
  Future<HistorialPageEntity> obtenerHistorialExitoso({
    int pagina = 0,
    int tamanio = 10,
  });
}

