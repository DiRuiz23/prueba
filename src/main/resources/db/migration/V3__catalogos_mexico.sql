-- =====================================================================
-- MIGRACIÓN V3: CATÁLOGOS DE MÉXICO (NACIONALIDAD, ESTADOS Y CÓDIGOS POSTALES)
-- =====================================================================

-- 1. Nacionalidad asociada a cada país (por ahora solo México -> Mexicana)
ALTER TABLE cat_paises ADD COLUMN IF NOT EXISTS nacionalidad VARCHAR(50);
UPDATE cat_paises SET nacionalidad = 'Mexicana' WHERE codigo_iso = 'MEX';

-- 2. Evitar estados duplicados por país
ALTER TABLE cat_estados
    ADD CONSTRAINT uq_estado_pais_nombre UNIQUE (pais_id, nombre);

-- 3. Las 32 entidades federativas de México
INSERT INTO cat_estados (pais_id, nombre)
SELECT p.id, v.nombre
FROM cat_paises p
CROSS JOIN (VALUES
    ('Aguascalientes'), ('Baja California'), ('Baja California Sur'), ('Campeche'),
    ('Chiapas'), ('Chihuahua'), ('Ciudad de México'), ('Coahuila'),
    ('Colima'), ('Durango'), ('Estado de México'), ('Guanajuato'),
    ('Guerrero'), ('Hidalgo'), ('Jalisco'), ('Michoacán'),
    ('Morelos'), ('Nayarit'), ('Nuevo León'), ('Oaxaca'),
    ('Puebla'), ('Querétaro'), ('Quintana Roo'), ('San Luis Potosí'),
    ('Sinaloa'), ('Sonora'), ('Tabasco'), ('Tamaulipas'),
    ('Tlaxcala'), ('Veracruz'), ('Yucatán'), ('Zacatecas')
) AS v(nombre)
WHERE p.codigo_iso = 'MEX'
ON CONFLICT (pais_id, nombre) DO NOTHING;

-- 4. Evitar códigos postales duplicados para el mismo municipio
ALTER TABLE cat_codigos_postales
    ADD CONSTRAINT uq_cp_codigo_municipio UNIQUE (codigo, municipio);

-- 5. Muestra de códigos postales reales (SEPOMEX) por entidad.
--    Para producción se puede cargar el catálogo completo de SEPOMEX.
INSERT INTO cat_codigos_postales (estado_id, codigo, municipio)
SELECT e.id, v.codigo, v.municipio
FROM (VALUES
    ('Aguascalientes',      '20000', 'Aguascalientes'),
    ('Baja California',     '22000', 'Tijuana'),
    ('Baja California',     '21100', 'Mexicali'),
    ('Baja California Sur', '23000', 'La Paz'),
    ('Campeche',            '24000', 'Campeche'),
    ('Chiapas',             '29000', 'Tuxtla Gutiérrez'),
    ('Chihuahua',           '31000', 'Chihuahua'),
    ('Chihuahua',           '32000', 'Juárez'),
    ('Ciudad de México',    '06000', 'Cuauhtémoc'),
    ('Ciudad de México',    '06600', 'Cuauhtémoc'),
    ('Ciudad de México',    '06700', 'Cuauhtémoc'),
    ('Ciudad de México',    '03100', 'Benito Juárez'),
    ('Ciudad de México',    '11560', 'Miguel Hidalgo'),
    ('Ciudad de México',    '04510', 'Coyoacán'),
    ('Ciudad de México',    '01000', 'Álvaro Obregón'),
    ('Ciudad de México',    '14000', 'Tlalpan'),
    ('Coahuila',            '25000', 'Saltillo'),
    ('Coahuila',            '27000', 'Torreón'),
    ('Colima',              '28000', 'Colima'),
    ('Durango',             '34000', 'Durango'),
    ('Estado de México',    '50000', 'Toluca'),
    ('Estado de México',    '53100', 'Naucalpan de Juárez'),
    ('Guanajuato',          '36000', 'Guanajuato'),
    ('Guanajuato',          '37000', 'León'),
    ('Guerrero',            '39000', 'Chilpancingo de los Bravo'),
    ('Guerrero',            '39300', 'Acapulco de Juárez'),
    ('Hidalgo',             '42000', 'Pachuca de Soto'),
    ('Jalisco',             '44100', 'Guadalajara'),
    ('Jalisco',             '45010', 'Zapopan'),
    ('Jalisco',             '45500', 'San Pedro Tlaquepaque'),
    ('Michoacán',           '58000', 'Morelia'),
    ('Morelos',             '62000', 'Cuernavaca'),
    ('Nayarit',             '63000', 'Tepic'),
    ('Nuevo León',          '64000', 'Monterrey'),
    ('Nuevo León',          '66220', 'San Pedro Garza García'),
    ('Nuevo León',          '66450', 'San Nicolás de los Garza'),
    ('Oaxaca',              '68000', 'Oaxaca de Juárez'),
    ('Puebla',              '72000', 'Puebla'),
    ('Querétaro',           '76000', 'Querétaro'),
    ('Quintana Roo',        '77000', 'Othón P. Blanco'),
    ('Quintana Roo',        '77500', 'Benito Juárez'),
    ('San Luis Potosí',     '78000', 'San Luis Potosí'),
    ('Sinaloa',             '80000', 'Culiacán'),
    ('Sonora',              '83000', 'Hermosillo'),
    ('Tabasco',             '86000', 'Centro'),
    ('Tamaulipas',          '87000', 'Victoria'),
    ('Tlaxcala',            '90000', 'Tlaxcala'),
    ('Veracruz',            '91000', 'Xalapa'),
    ('Veracruz',            '91700', 'Veracruz'),
    ('Yucatán',             '97000', 'Mérida'),
    ('Zacatecas',           '98000', 'Zacatecas')
) AS v(estado, codigo, municipio)
JOIN cat_estados e ON e.nombre = v.estado
JOIN cat_paises p ON p.id = e.pais_id AND p.codigo_iso = 'MEX'
ON CONFLICT (codigo, municipio) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_cp_codigo ON cat_codigos_postales(codigo);
