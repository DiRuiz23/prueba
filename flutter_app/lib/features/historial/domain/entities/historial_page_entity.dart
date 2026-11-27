import 'package:equatable/equatable.dart';
import '../../../compra/domain/entities/transaccion_entity.dart';

class HistorialPageEntity extends Equatable {
  final List<TransaccionEntity> items;
  final int paginaActual;
  final int totalPaginas;
  final int totalElementos;
  final bool esUltima;

  const HistorialPageEntity({
    required this.items,
    required this.paginaActual,
    required this.totalPaginas,
    required this.totalElementos,
    required this.esUltima,
  });

  @override
  List<Object?> get props => [items, paginaActual, totalPaginas, totalElementos, esUltima];
}

