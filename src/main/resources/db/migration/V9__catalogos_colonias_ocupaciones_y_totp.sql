-- =====================================================================
-- MIGRACIÓN V9: CATÁLOGOS DE OCUPACIONES Y COLONIAS, EMPRESA OPCIONAL
-- PARA ESTUDIANTES Y SOPORTE TOTP (GOOGLE AUTHENTICATOR)
-- =====================================================================

-- 1. Columnas TOTP en usuarios (2FA con Google Authenticator)
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS totp_secret VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS totp_habilitado BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Hacer empresa opcional en clientes (requerida solo cuando no es Estudiante)
ALTER TABLE clientes ALTER COLUMN empresa DROP NOT NULL;

-- 3. Catálogo de ocupaciones
CREATE TABLE IF NOT EXISTS cat_ocupaciones (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    orden INT NOT NULL DEFAULT 100
);

INSERT INTO cat_ocupaciones (nombre, orden) VALUES
    ('Empleado(a) sector privado', 1),
    ('Empleado(a) sector público', 2),
    ('Estudiante', 3),
    ('Profesionista independiente', 4),
    ('Comerciante', 5),
    ('Empresario(a)', 6),
    ('Docente / Profesor(a)', 7),
    ('Ingeniero(a) / Desarrollador(a)', 8),
    ('Personal de salud', 9),
    ('Técnico(a) especializado(a)', 10),
    ('Trabajador(a) por cuenta propia', 11),
    ('Jubilado(a) / Pensionado(a)', 12),
    ('Hogar', 13)
ON CONFLICT (nombre) DO NOTHING;

-- 4. Catálogo de colonias por código postal (SEPOMEX)
CREATE TABLE IF NOT EXISTS cat_colonias (
    id SERIAL PRIMARY KEY,
    codigo_postal VARCHAR(10) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    CONSTRAINT uq_colonia_cp_nombre UNIQUE (codigo_postal, nombre)
);

CREATE INDEX IF NOT EXISTS idx_cat_colonias_cp ON cat_colonias(codigo_postal);

INSERT INTO cat_colonias (codigo_postal, nombre) VALUES
    -- Morelos
    ('62000', 'Cuernavaca Centro'),
    ('62000', 'Las Palmas'),
    ('62000', 'Carolina'),
    ('62000', 'Gualupita'),
    ('62000', 'San Antón'),
    ('62000', 'Vista Hermosa'),
    ('62000', 'Lomas de la Selva'),
    ('62000', 'Delicias'),
    ('62000', 'Acapantzingo'),
    ('62000', 'Tlaltenango'),

    ('62550', 'Jiutepec Centro'),
    ('62550', 'Civac'),
    ('62550', 'Tejalpa'),
    ('62550', 'Tlahuapan'),
    ('62550', 'Progreso'),
    ('62550', 'La Joya'),
    ('62550', 'Pedregal de las Fuentes'),

    ('62580', 'Temixco Centro'),
    ('62580', 'Burgos'),
    ('62580', 'Lomas del Carril'),
    ('62580', 'Rubén Jaramillo'),
    ('62580', 'Acatlipa'),
    ('62580', 'Pueblo Viejo'),

    ('62740', 'Cuautla Centro'),
    ('62740', 'Emiliano Zapata'),
    ('62740', 'Gabriel Tepepa'),
    ('62740', 'Otilio Montaño'),
    ('62740', 'Año de Juárez'),

    ('62760', 'Emiliano Zapata Centro'),
    ('62760', 'Tres de Mayo'),
    ('62760', 'Tezoyuca'),
    ('62760', 'Benito Juárez'),
    ('62760', 'Palo Escrito'),

    ('62730', 'Yautepec Centro'),
    ('62730', 'Oaxtepec'),
    ('62730', 'Cocoyoc'),
    ('62730', 'Rancho Nuevo'),

    ('62790', 'Xochitepec Centro'),
    ('62790', 'Alpuyeca'),
    ('62790', 'Chiconcuac'),
    ('62790', 'Real del Puente'),

    ('62900', 'Jojutla Centro'),
    ('62900', 'Panchimalco'),
    ('62900', 'Tehuixtla'),

    ('62780', 'Zacatepec Centro'),
    ('62780', 'Galeana'),
    ('62780', 'Lázaro Cárdenas'),

    ('62520', 'Tepoztlán Centro'),
    ('62520', 'San Miguel'),
    ('62520', 'Santo Domingo'),
    ('62520', 'Amatlán'),

    -- Ciudad de México
    ('06000', 'Centro Histórico'),
    ('06000', 'Guerrero'),
    ('06000', 'Tabacalera'),
    ('06600', 'Juárez'),
    ('06600', 'Zona Rosa'),
    ('06700', 'Roma Norte'),
    ('06700', 'Roma Sur'),
    ('06700', 'Condesa'),
    ('03100', 'Del Valle Centro'),
    ('03100', 'Del Valle Norte'),
    ('03100', 'Narvarte'),
    ('11560', 'Polanco IV Sección'),
    ('11560', 'Polanco V Sección'),
    ('11560', 'Anzures'),
    ('11560', 'Granada'),
    ('04510', 'Ciudad Universitaria'),
    ('04510', 'Pedregal de Santo Domingo'),
    ('04510', 'Copilco'),
    ('04000', 'Villa Coyoacán'),
    ('04000', 'Del Carmen'),
    ('04000', 'Santa Catarina'),
    ('01000', 'San Ángel'),
    ('01000', 'Chimalistac'),
    ('01000', 'Guadalupe Inn'),
    ('14000', 'Tlalpan Centro'),
    ('14000', 'Toriello Guerra'),
    ('14000', 'Isidro Fabela'),
    ('02000', 'Azcapotzalco Centro'),
    ('02000', 'Clavería'),
    ('07000', 'Aragón'),
    ('07000', 'Lindavista'),
    ('07000', 'Guadalupe Tepeyac'),
    ('08000', 'Iztacalco'),
    ('08000', 'Agrícola Oriental'),
    ('08000', 'Viaducto Piedad'),
    ('09000', 'Iztapalapa Centro'),
    ('09000', 'Santa Cruz Meyehualco'),
    ('09000', 'Escuadrón 201'),
    ('15000', 'Jardín Balbuena'),
    ('15000', 'Moctezuma'),
    ('15000', 'Romero Rubio'),
    ('16000', 'Xochimilco Centro'),
    ('16000', 'San Pedro'),
    ('16000', 'Tepepan'),

    -- Estado de México
    ('50000', 'Toluca Centro'),
    ('50000', 'Santa Clara'),
    ('50000', 'Universidad'),
    ('53100', 'Ciudad Satélite'),
    ('53100', 'Boulevares'),
    ('53100', 'Naucalpan Centro'),

    -- Jalisco
    ('44100', 'Guadalajara Centro'),
    ('44100', 'Americana'),
    ('44100', 'Lafayette'),
    ('45010', 'Zapopan Centro'),
    ('45010', 'Ciudad Granja'),
    ('45010', 'Chapalita'),
    ('45500', 'Tlaquepaque Centro'),
    ('45500', 'El Álamo'),

    -- Nuevo León
    ('64000', 'Monterrey Centro'),
    ('64000', 'Obispado'),
    ('64000', 'Barrio Antiguo'),
    ('66220', 'Del Valle'),
    ('66220', 'San Pedro Centro'),
    ('66450', 'San Nicolás Centro'),
    ('66450', 'Anáhuac'),

    -- Puebla
    ('72000', 'Puebla Centro'),
    ('72000', 'Analco'),
    ('72000', 'La Paz'),
    ('72760', 'San Pedro Cholula Centro'),
    ('72760', 'Santiago Mixquitla'),
    ('72810', 'San Andrés Cholula Centro'),
    ('72810', 'Angelópolis'),

    -- Querétaro
    ('76000', 'Querétaro Centro'),
    ('76000', 'Álamos'),
    ('76000', 'Jurica'),

    -- Guanajuato
    ('36000', 'Guanajuato Centro'),
    ('36000', 'Marfil'),
    ('37000', 'León Centro'),
    ('37000', 'Jardines del Moral'),
    ('37000', 'San Juan Bosco'),

    -- Baja California
    ('22000', 'Tijuana Centro'),
    ('22000', 'Zona Río'),
    ('22000', 'Chapultepec'),
    ('21100', 'Mexicali Centro'),
    ('21100', 'Nueva'),

    -- Yucatán & Quintana Roo
    ('97000', 'Mérida Centro'),
    ('97000', 'Itzimná'),
    ('97000', 'Paseo de Montejo'),
    ('77500', 'Cancún Centro'),
    ('77500', 'Zona Hotelera'),
    ('77500', 'Supermanzana 15'),
    ('77000', 'Chetumal Centro'),
    ('77000', 'Adolfo López Mateos'),

    -- Veracruz & Guerrero
    ('91000', 'Xalapa Centro'),
    ('91000', 'Ánimas'),
    ('91700', 'Veracruz Centro'),
    ('91700', 'Reforma'),
    ('39000', 'Chilpancingo Centro'),
    ('39000', 'Universitaria'),
    ('39300', 'Acapulco Centro'),
    ('39300', 'Costa Azul'),

    -- Michoacán, Oaxaca & Hidalgo
    ('58000', 'Morelia Centro'),
    ('58000', 'Chapultepec Norte'),
    ('68000', 'Oaxaca Centro'),
    ('68000', 'Jalatlaco'),
    ('68000', 'Reforma'),
    ('42000', 'Pachuca Centro'),
    ('42000', 'Periodistas'),

    -- Resto de capitales y ciudades principales
    ('20000', 'Aguascalientes Centro'),
    ('20000', 'San Marcos'),
    ('25000', 'Saltillo Centro'),
    ('25000', 'República'),
    ('27000', 'Torreón Centro'),
    ('27000', 'Campestre La Rosita'),
    ('31000', 'Chihuahua Centro'),
    ('31000', 'San Felipe'),
    ('32000', 'Juárez Centro'),
    ('32000', 'Pronaf'),
    ('83000', 'Hermosillo Centro'),
    ('83000', 'Pitic'),
    ('80000', 'Culiacán Centro'),
    ('80000', 'Las Quintas'),
    ('78000', 'San Luis Potosí Centro'),
    ('78000', 'Lomas'),
    ('86000', 'Villahermosa Centro'),
    ('86000', 'Tabasco 2000'),
    ('87000', 'Ciudad Victoria Centro'),
    ('87000', 'Tamazunchale'),
    ('90000', 'Tlaxcala Centro'),
    ('90000', 'Ocotlán'),
    ('98000', 'Zacatecas Centro'),
    ('98000', 'Lomas de la Soledad'),
    ('28000', 'Colima Centro'),
    ('28000', 'Lomas de Circunvalación'),
    ('34000', 'Durango Centro'),
    ('34000', 'Los Remedios'),
    ('63000', 'Tepic Centro'),
    ('63000', 'Ciudad del Valle'),
    ('23000', 'La Paz Centro'),
    ('23000', 'Esterito'),
    ('24000', 'Campeche Centro'),
    ('24000', 'San Román'),
    ('29000', 'Tuxtla Gutiérrez Centro'),
    ('29000', 'Terán')
ON CONFLICT (codigo_postal, nombre) DO NOTHING;

-- Garantizar al menos una colonia ('Centro') para cada CP existente en el catálogo
INSERT INTO cat_colonias (codigo_postal, nombre)
SELECT DISTINCT cp.codigo, 'Centro'
FROM cat_codigos_postales cp
ON CONFLICT (codigo_postal, nombre) DO NOTHING;
