class CatalogoPostalItem {
  final String estado;
  final String codigoPostal;
  final String municipio;

  const CatalogoPostalItem({
    required this.estado,
    required this.codigoPostal,
    required this.municipio,
  });

  factory CatalogoPostalItem.fromJson(Map<String, dynamic> json) {
    return CatalogoPostalItem(
      estado: (json['estado'] as String?) ?? '',
      codigoPostal: (json['codigoPostal'] as String?) ?? '',
      municipio: (json['municipio'] as String?) ?? '',
    );
  }

  String get clave => '$codigoPostal|$municipio';
}

const List<CatalogoPostalItem> catalogoSepomex = [
  CatalogoPostalItem(estado: 'Aguascalientes', codigoPostal: '20000', municipio: 'Aguascalientes'),
  CatalogoPostalItem(estado: 'Baja California', codigoPostal: '22000', municipio: 'Tijuana'),
  CatalogoPostalItem(estado: 'Baja California', codigoPostal: '21100', municipio: 'Mexicali'),
  CatalogoPostalItem(estado: 'Baja California Sur', codigoPostal: '23000', municipio: 'La Paz'),
  CatalogoPostalItem(estado: 'Campeche', codigoPostal: '24000', municipio: 'Campeche'),
  CatalogoPostalItem(estado: 'Chiapas', codigoPostal: '29000', municipio: 'Tuxtla Gutiérrez'),
  CatalogoPostalItem(estado: 'Chihuahua', codigoPostal: '31000', municipio: 'Chihuahua'),
  CatalogoPostalItem(estado: 'Chihuahua', codigoPostal: '32000', municipio: 'Juárez'),
  CatalogoPostalItem(estado: 'Ciudad de México', codigoPostal: '06000', municipio: 'Cuauhtémoc'),
  CatalogoPostalItem(estado: 'Ciudad de México', codigoPostal: '06600', municipio: 'Cuauhtémoc'),
  CatalogoPostalItem(estado: 'Ciudad de México', codigoPostal: '06700', municipio: 'Cuauhtémoc'),
  CatalogoPostalItem(estado: 'Ciudad de México', codigoPostal: '03100', municipio: 'Benito Juárez'),
  CatalogoPostalItem(estado: 'Ciudad de México', codigoPostal: '11560', municipio: 'Miguel Hidalgo'),
  CatalogoPostalItem(estado: 'Ciudad de México', codigoPostal: '04510', municipio: 'Coyoacán'),
  CatalogoPostalItem(estado: 'Ciudad de México', codigoPostal: '01000', municipio: 'Álvaro Obregón'),
  CatalogoPostalItem(estado: 'Ciudad de México', codigoPostal: '14000', municipio: 'Tlalpan'),
  CatalogoPostalItem(estado: 'Coahuila', codigoPostal: '25000', municipio: 'Saltillo'),
  CatalogoPostalItem(estado: 'Coahuila', codigoPostal: '27000', municipio: 'Torreón'),
  CatalogoPostalItem(estado: 'Colima', codigoPostal: '28000', municipio: 'Colima'),
  CatalogoPostalItem(estado: 'Durango', codigoPostal: '34000', municipio: 'Durango'),
  CatalogoPostalItem(estado: 'Estado de México', codigoPostal: '50000', municipio: 'Toluca'),
  CatalogoPostalItem(estado: 'Estado de México', codigoPostal: '53100', municipio: 'Naucalpan de Juárez'),
  CatalogoPostalItem(estado: 'Guanajuato', codigoPostal: '36000', municipio: 'Guanajuato'),
  CatalogoPostalItem(estado: 'Guanajuato', codigoPostal: '37000', municipio: 'León'),
  CatalogoPostalItem(estado: 'Guerrero', codigoPostal: '39000', municipio: 'Chilpancingo de los Bravo'),
  CatalogoPostalItem(estado: 'Guerrero', codigoPostal: '39300', municipio: 'Acapulco de Juárez'),
  CatalogoPostalItem(estado: 'Hidalgo', codigoPostal: '42000', municipio: 'Pachuca de Soto'),
  CatalogoPostalItem(estado: 'Jalisco', codigoPostal: '44100', municipio: 'Guadalajara'),
  CatalogoPostalItem(estado: 'Jalisco', codigoPostal: '45010', municipio: 'Zapopan'),
  CatalogoPostalItem(estado: 'Jalisco', codigoPostal: '45500', municipio: 'San Pedro Tlaquepaque'),
  CatalogoPostalItem(estado: 'Michoacán', codigoPostal: '58000', municipio: 'Morelia'),
  CatalogoPostalItem(estado: 'Morelos', codigoPostal: '62000', municipio: 'Cuernavaca'),
  CatalogoPostalItem(estado: 'Nayarit', codigoPostal: '63000', municipio: 'Tepic'),
  CatalogoPostalItem(estado: 'Nuevo León', codigoPostal: '64000', municipio: 'Monterrey'),
  CatalogoPostalItem(estado: 'Nuevo León', codigoPostal: '66220', municipio: 'San Pedro Garza García'),
  CatalogoPostalItem(estado: 'Nuevo León', codigoPostal: '66450', municipio: 'San Nicolás de los Garza'),
  CatalogoPostalItem(estado: 'Oaxaca', codigoPostal: '68000', municipio: 'Oaxaca de Juárez'),
  CatalogoPostalItem(estado: 'Puebla', codigoPostal: '72000', municipio: 'Puebla'),
  CatalogoPostalItem(estado: 'Querétaro', codigoPostal: '76000', municipio: 'Querétaro'),
  CatalogoPostalItem(estado: 'Quintana Roo', codigoPostal: '77000', municipio: 'Othón P. Blanco'),
  CatalogoPostalItem(estado: 'Quintana Roo', codigoPostal: '77500', municipio: 'Benito Juárez'),
  CatalogoPostalItem(estado: 'San Luis Potosí', codigoPostal: '78000', municipio: 'San Luis Potosí'),
  CatalogoPostalItem(estado: 'Sinaloa', codigoPostal: '80000', municipio: 'Culiacán'),
  CatalogoPostalItem(estado: 'Sonora', codigoPostal: '83000', municipio: 'Hermosillo'),
  CatalogoPostalItem(estado: 'Tabasco', codigoPostal: '86000', municipio: 'Centro'),
  CatalogoPostalItem(estado: 'Tamaulipas', codigoPostal: '87000', municipio: 'Victoria'),
  CatalogoPostalItem(estado: 'Tlaxcala', codigoPostal: '90000', municipio: 'Tlaxcala'),
  CatalogoPostalItem(estado: 'Veracruz', codigoPostal: '91000', municipio: 'Xalapa'),
  CatalogoPostalItem(estado: 'Veracruz', codigoPostal: '91700', municipio: 'Veracruz'),
  CatalogoPostalItem(estado: 'Yucatán', codigoPostal: '97000', municipio: 'Mérida'),
  CatalogoPostalItem(estado: 'Zacatecas', codigoPostal: '98000', municipio: 'Zacatecas'),
];

const List<String> ocupacionesFallback = [
  'Empleado(a) sector privado',
  'Empleado(a) sector público',
  'Estudiante',
  'Profesionista independiente',
  'Comerciante',
  'Empresario(a)',
  'Docente / Profesor(a)',
  'Ingeniero(a) / Desarrollador(a)',
  'Personal de salud',
  'Técnico(a) especializado(a)',
  'Trabajador(a) por cuenta propia',
  'Jubilado(a) / Pensionado(a)',
  'Hogar',
];

const Map<String, List<String>> coloniasFallback = {
  '62000': ['Cuernavaca Centro', 'Las Palmas', 'Carolina', 'Gualupita', 'San Antón', 'Vista Hermosa', 'Lomas de la Selva', 'Delicias', 'Acapantzingo', 'Tlaltenango'],
  '62550': ['Jiutepec Centro', 'Civac', 'Tejalpa', 'Tlahuapan', 'Progreso', 'La Joya', 'Pedregal de las Fuentes'],
  '62580': ['Temixco Centro', 'Burgos', 'Lomas del Carril', 'Rubén Jaramillo', 'Acatlipa', 'Pueblo Viejo'],
  '62740': ['Cuautla Centro', 'Emiliano Zapata', 'Gabriel Tepepa', 'Otilio Montaño', 'Año de Juárez'],
  '62760': ['Emiliano Zapata Centro', 'Tres de Mayo', 'Tezoyuca', 'Benito Juárez', 'Palo Escrito'],
  '62730': ['Yautepec Centro', 'Oaxtepec', 'Cocoyoc', 'Rancho Nuevo'],
  '62790': ['Xochitepec Centro', 'Alpuyeca', 'Chiconcuac', 'Real del Puente'],
  '62900': ['Jojutla Centro', 'Panchimalco', 'Tehuixtla'],
  '62780': ['Zacatepec Centro', 'Galeana', 'Lázaro Cárdenas'],
  '62520': ['Tepoztlán Centro', 'San Miguel', 'Santo Domingo', 'Amatlán'],
  '06000': ['Centro Histórico', 'Guerrero', 'Tabacalera'],
  '06600': ['Juárez', 'Zona Rosa'],
  '06700': ['Roma Norte', 'Roma Sur', 'Condesa'],
  '03100': ['Del Valle Centro', 'Del Valle Norte', 'Narvarte'],
  '11560': ['Polanco IV Sección', 'Polanco V Sección', 'Anzures', 'Granada'],
  '04510': ['Ciudad Universitaria', 'Pedregal de Santo Domingo', 'Copilco'],
  '01000': ['San Ángel', 'Chimalistac', 'Guadalupe Inn'],
  '14000': ['Tlalpan Centro', 'Toriello Guerra', 'Isidro Fabela'],
  '44100': ['Guadalajara Centro', 'Americana', 'Lafayette'],
  '64000': ['Monterrey Centro', 'Obispado', 'Barrio Antiguo'],
  '72000': ['Puebla Centro', 'Analco', 'La Paz'],
  '76000': ['Querétaro Centro', 'Álamos', 'Jurica'],
};

class TotpConfigResult {
  final String secreto;
  final String otpAuthUri;
  final String qrBase64;
  final String correo;

  const TotpConfigResult({
    required this.secreto,
    required this.otpAuthUri,
    this.qrBase64 = '',
    required this.correo,
  });

  factory TotpConfigResult.fromJson(Map<String, dynamic> json) {
    return TotpConfigResult(
      secreto: (json['secreto'] as String?) ?? '',
      otpAuthUri: (json['otpAuthUri'] as String?) ?? '',
      qrBase64: (json['qrBase64'] as String?) ?? '',
      correo: (json['correo'] as String?) ?? '',
    );
  }
}

class RegistroClienteParams {
  final String nombre;
  final String? segundoNombre;
  final String apellidoPaterno;
  final String apellidoMaterno;
  final String fechaNacimiento;
  final String curp;
  final String rfc;
  final String sexo;
  final String estadoCivil;
  final String correo;
  final String telefonoMovil;
  final String? telefonoAlternativo;
  final String ocupacion;
  final String? empresa;
  final double ingresoMensual;
  final String calle;
  final String numeroExterior;
  final String? numeroInterior;
  final String colonia;
  final String municipio;
  final String estado;
  final String codigoPostal;
  final String password;

  const RegistroClienteParams({
    required this.nombre,
    this.segundoNombre,
    required this.apellidoPaterno,
    required this.apellidoMaterno,
    required this.fechaNacimiento,
    required this.curp,
    required this.rfc,
    required this.sexo,
    required this.estadoCivil,
    required this.correo,
    required this.telefonoMovil,
    this.telefonoAlternativo,
    required this.ocupacion,
    this.empresa,
    required this.ingresoMensual,
    required this.calle,
    required this.numeroExterior,
    this.numeroInterior,
    required this.colonia,
    required this.municipio,
    required this.estado,
    required this.codigoPostal,
    required this.password,
  });

  String? _nullable(String? v) {
    final trimmed = v?.trim() ?? '';
    return trimmed.isEmpty ? null : trimmed;
  }

  Map<String, dynamic> toJson() => {
        'nombre': nombre.trim(),
        'segundoNombre': _nullable(segundoNombre),
        'apellidoPaterno': apellidoPaterno.trim(),
        'apellidoMaterno': apellidoMaterno.trim(),
        'fechaNacimiento': fechaNacimiento.trim(),
        'curp': curp.trim().toUpperCase(),
        'rfc': rfc.trim().toUpperCase(),
        'sexo': sexo,
        'nacionalidad': 'Mexicana',
        'estadoCivil': estadoCivil,
        'correo': correo.trim().toLowerCase(),
        'telefonoMovil': telefonoMovil.trim(),
        'telefonoAlternativo': _nullable(telefonoAlternativo),
        'ocupacion': ocupacion.trim(),
        'empresa': _nullable(empresa),
        'ingresoMensual': ingresoMensual,
        'domicilio': {
          'calle': calle.trim(),
          'numeroExterior': numeroExterior.trim(),
          'numeroInterior': _nullable(numeroInterior),
          'colonia': colonia.trim(),
          'municipio': municipio,
          'estado': estado,
          'codigoPostal': codigoPostal,
          'pais': 'México',
        },
        'password': password,
      };
}

class RegistroClienteResult {
  final int id;
  final String nombreCompleto;
  final String correo;
  final String numeroCuenta;

  const RegistroClienteResult({
    required this.id,
    required this.nombreCompleto,
    required this.correo,
    required this.numeroCuenta,
  });

  factory RegistroClienteResult.fromJson(Map<String, dynamic> json) {
    final cuentas = (json['numeroCuentas'] as List<dynamic>? ?? [])
        .map((e) => e.toString())
        .toList();
    final partes = [
      (json['nombre'] as String?) ?? '',
      (json['apellidoPaterno'] as String?) ?? '',
      (json['apellidoMaterno'] as String?) ?? '',
    ].where((p) => p.trim().isNotEmpty).join(' ');

    return RegistroClienteResult(
      id: (json['id'] as num?)?.toInt() ?? 0,
      nombreCompleto: partes,
      correo: (json['correo'] as String?) ?? '',
      numeroCuenta: cuentas.isNotEmpty ? cuentas.first : '',
    );
  }
}

class RangoIngresoItem {
  final String etiqueta;
  final double valor;

  const RangoIngresoItem(this.etiqueta, this.valor);
}

const List<RangoIngresoItem> rangosIngresoMensual = [
  RangoIngresoItem('Menos de \$5,000 MXN', 5000),
  RangoIngresoItem('\$5,001 a \$10,000 MXN', 10000),
  RangoIngresoItem('\$10,001 a \$20,000 MXN', 20000),
  RangoIngresoItem('\$20,001 a \$35,000 MXN', 35000),
  RangoIngresoItem('\$35,001 a \$50,000 MXN', 50000),
  RangoIngresoItem('\$50,001 a \$100,000 MXN', 100000),
  RangoIngresoItem('Más de \$100,000 MXN', 150000),
];

class DisponibilidadRegistroResult {
  final bool curpDisponible;
  final bool rfcDisponible;
  final bool correoDisponible;
  final bool telefonoDisponible;

  const DisponibilidadRegistroResult({
    required this.curpDisponible,
    required this.rfcDisponible,
    required this.correoDisponible,
    this.telefonoDisponible = true,
  });

  factory DisponibilidadRegistroResult.fromJson(Map<String, dynamic> json) {
    return DisponibilidadRegistroResult(
      curpDisponible: (json['curpDisponible'] as bool?) ?? true,
      rfcDisponible: (json['rfcDisponible'] as bool?) ?? true,
      correoDisponible: (json['correoDisponible'] as bool?) ?? true,
      telefonoDisponible: (json['telefonoDisponible'] as bool?) ?? true,
    );
  }
}
