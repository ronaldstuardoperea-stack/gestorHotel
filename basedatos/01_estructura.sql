-- =====================================================================
--  SISTEMA DE GESTION HOTELERA  -  Proyecto Final Programacion II
--  Universidad Mariano Galvez de Guatemala
--  Motor: MariaDB 10.4 (XAMPP)
--  Archivo: 01_estructura.sql  -> Crea la base de datos y sus tablas
-- =====================================================================

DROP DATABASE IF EXISTS hotel_db;
CREATE DATABASE hotel_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_spanish_ci;
USE hotel_db;

-- ---------------------------------------------------------------------
-- USUARIO: personal del hotel que puede ingresar al sistema.
-- La contrasena NUNCA se guarda en texto plano: se guarda el hash
-- SHA-256 de (salt + contrasena). El salt es distinto para cada usuario,
-- de modo que dos personas con la misma clave tengan hashes diferentes.
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id_usuario      INT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(120) NOT NULL,
    usuario         VARCHAR(40)  NOT NULL,
    salt            CHAR(32)     NOT NULL,
    clave_hash      CHAR(64)     NOT NULL,
    rol             ENUM('ADMINISTRADOR','RECEPCION','AUDITOR') NOT NULL DEFAULT 'RECEPCION',
    activo          TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_registro  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso   DATETIME     NULL,
    CONSTRAINT uq_usuario_nombre UNIQUE (usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- TIPO_HABITACION: catalogo de categorias (Sencilla, Doble, Suite...).
-- Aqui vive la tarifa base; la habitacion individual hereda de su tipo.
-- ---------------------------------------------------------------------
CREATE TABLE tipo_habitacion (
    id_tipo            INT AUTO_INCREMENT PRIMARY KEY,
    nombre             VARCHAR(60)   NOT NULL,
    descripcion        VARCHAR(255)  NULL,
    capacidad_adultos  INT           NOT NULL DEFAULT 2,
    capacidad_ninos    INT           NOT NULL DEFAULT 0,
    tarifa_base        DECIMAL(10,2) NOT NULL,
    activo             TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT uq_tipo_nombre UNIQUE (nombre),
    CONSTRAINT ck_tipo_tarifa CHECK (tarifa_base >= 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- HABITACION: la unidad fisica que se renta.
-- El campo 'estado' refleja la situacion ACTUAL (hoy) de la habitacion.
-- La disponibilidad en un rango de fechas futuro NO se lee de aqui: se
-- calcula cruzando las reservas y los bloqueos vigentes.
-- ---------------------------------------------------------------------
CREATE TABLE habitacion (
    id_habitacion INT AUTO_INCREMENT PRIMARY KEY,
    numero        VARCHAR(10)  NOT NULL,
    id_tipo       INT          NOT NULL,
    piso          INT          NOT NULL DEFAULT 1,
    estado        ENUM('DISPONIBLE','OCUPADA','LIMPIEZA','MANTENIMIENTO','BLOQUEADA')
                  NOT NULL DEFAULT 'DISPONIBLE',
    descripcion   VARCHAR(255) NULL,
    activo        TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT uq_habitacion_numero UNIQUE (numero),
    CONSTRAINT fk_habitacion_tipo FOREIGN KEY (id_tipo)
        REFERENCES tipo_habitacion (id_tipo)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- TEMPORADA: periodos con recargo o descuento sobre la tarifa base.
-- 'factor' es un multiplicador: 1.00 = precio normal, 1.35 = +35%,
-- 0.80 = promocion con 20% de descuento.
-- ---------------------------------------------------------------------
CREATE TABLE temporada (
    id_temporada INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(60)  NOT NULL,
    fecha_inicio DATE         NOT NULL,
    fecha_fin    DATE         NOT NULL,
    factor       DECIMAL(5,2) NOT NULL DEFAULT 1.00,
    activo       TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT ck_temporada_rango  CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT ck_temporada_factor CHECK (factor > 0)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- BLOQUEO_HABITACION: saca una habitacion de la venta durante un rango
-- de fechas (mantenimiento, remodelacion, uso interno). Un bloqueo
-- activo impide reservar esa habitacion en esas fechas.
-- ---------------------------------------------------------------------
CREATE TABLE bloqueo_habitacion (
    id_bloqueo     INT AUTO_INCREMENT PRIMARY KEY,
    id_habitacion  INT          NOT NULL,
    fecha_inicio   DATE         NOT NULL,
    fecha_fin      DATE         NOT NULL,
    motivo         VARCHAR(150) NOT NULL,
    id_usuario     INT          NOT NULL,
    fecha_registro DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo         TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT ck_bloqueo_rango CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT fk_bloqueo_habitacion FOREIGN KEY (id_habitacion)
        REFERENCES habitacion (id_habitacion),
    CONSTRAINT fk_bloqueo_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- CLIENTE: el titular de la reserva y, normalmente, quien paga.
-- Puede o no ser uno de los ocupantes de la habitacion.
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
    id_cliente     INT AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(80)  NOT NULL,
    apellido       VARCHAR(80)  NOT NULL,
    tipo_documento ENUM('DPI','PASAPORTE','NIT','OTRO') NOT NULL DEFAULT 'DPI',
    documento      VARCHAR(25)  NOT NULL,
    nit            VARCHAR(20)  NOT NULL DEFAULT 'C/F',
    telefono       VARCHAR(25)  NULL,
    correo         VARCHAR(120) NULL,
    direccion      VARCHAR(180) NULL,
    pais           VARCHAR(60)  NOT NULL DEFAULT 'Guatemala',
    notas          VARCHAR(255) NULL,
    activo         TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_registro DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cliente_documento UNIQUE (tipo_documento, documento)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- RESERVA: el corazon del sistema. Une cliente + habitacion + fechas.
-- 'tarifa_noche' se CONGELA al momento de reservar: aunque despues
-- cambie el precio de lista o la temporada, la reserva respeta el
-- precio que se le prometio al cliente.
-- ---------------------------------------------------------------------
CREATE TABLE reserva (
    id_reserva     INT AUTO_INCREMENT PRIMARY KEY,
    codigo         VARCHAR(20)   NOT NULL,
    id_cliente     INT           NOT NULL,
    id_habitacion  INT           NOT NULL,
    fecha_entrada  DATE          NOT NULL,
    fecha_salida   DATE          NOT NULL,
    adultos        INT           NOT NULL DEFAULT 1,
    ninos          INT           NOT NULL DEFAULT 0,
    tarifa_noche   DECIMAL(10,2) NOT NULL,
    noches         INT           NOT NULL,
    estado         ENUM('PENDIENTE','CONFIRMADA','EN_CASA','FINALIZADA','CANCELADA','NO_SHOW')
                   NOT NULL DEFAULT 'PENDIENTE',
    fecha_checkin  DATETIME      NULL,
    fecha_checkout DATETIME      NULL,
    observaciones  VARCHAR(255)  NULL,
    id_usuario     INT           NOT NULL,
    fecha_creacion DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reserva_codigo  UNIQUE (codigo),
    CONSTRAINT ck_reserva_rango   CHECK (fecha_salida > fecha_entrada),
    CONSTRAINT ck_reserva_adultos CHECK (adultos >= 1),
    CONSTRAINT fk_reserva_cliente FOREIGN KEY (id_cliente)
        REFERENCES cliente (id_cliente),
    CONSTRAINT fk_reserva_habitacion FOREIGN KEY (id_habitacion)
        REFERENCES habitacion (id_habitacion),
    CONSTRAINT fk_reserva_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

CREATE INDEX ix_reserva_fechas ON reserva (fecha_entrada, fecha_salida);
CREATE INDEX ix_reserva_estado ON reserva (estado);

-- ---------------------------------------------------------------------
-- OCUPANTE: las personas que realmente duermen en la habitacion.
-- Se registran para poder facturarle consumos a un huesped concreto.
-- ---------------------------------------------------------------------
CREATE TABLE ocupante (
    id_ocupante     INT AUTO_INCREMENT PRIMARY KEY,
    id_reserva      INT          NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    documento       VARCHAR(25)  NULL,
    edad            INT          NULL,
    es_titular      TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT ck_ocupante_edad CHECK (edad IS NULL OR (edad >= 0 AND edad <= 120)),
    CONSTRAINT fk_ocupante_reserva FOREIGN KEY (id_reserva)
        REFERENCES reserva (id_reserva) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- FACTURA: documento emitido al cliente.
-- 'tipo' es lo que permite la facturacion flexible que pide el
-- enunciado:
--   COMPLETA  -> hospedaje + todos los consumos
--   HOSPEDAJE -> solo las noches de habitacion
--   CONSUMOS  -> solo los gastos (restaurante, minibar, etc.)
--   OCUPANTE  -> solo los consumos de un huesped en particular
-- ---------------------------------------------------------------------
CREATE TABLE factura (
    id_factura      INT AUTO_INCREMENT PRIMARY KEY,
    serie           VARCHAR(10)   NOT NULL DEFAULT 'A',
    numero          INT           NOT NULL,
    id_reserva      INT           NOT NULL,
    id_cliente      INT           NOT NULL,
    id_ocupante     INT           NULL,
    nombre_receptor VARCHAR(150)  NOT NULL,
    nit_receptor    VARCHAR(20)   NOT NULL DEFAULT 'C/F',
    tipo            ENUM('COMPLETA','HOSPEDAJE','CONSUMOS','OCUPANTE') NOT NULL DEFAULT 'COMPLETA',
    subtotal        DECIMAL(10,2) NOT NULL DEFAULT 0,
    descuento       DECIMAL(10,2) NOT NULL DEFAULT 0,
    total           DECIMAL(10,2) NOT NULL DEFAULT 0,
    estado          ENUM('EMITIDA','ANULADA') NOT NULL DEFAULT 'EMITIDA',
    observaciones   VARCHAR(255)  NULL,
    fecha_emision   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario      INT           NOT NULL,
    CONSTRAINT uq_factura_serie_numero UNIQUE (serie, numero),
    CONSTRAINT fk_factura_reserva FOREIGN KEY (id_reserva)
        REFERENCES reserva (id_reserva),
    CONSTRAINT fk_factura_cliente FOREIGN KEY (id_cliente)
        REFERENCES cliente (id_cliente),
    CONSTRAINT fk_factura_ocupante FOREIGN KEY (id_ocupante)
        REFERENCES ocupante (id_ocupante),
    CONSTRAINT fk_factura_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- DETALLE_FACTURA: los renglones impresos en la factura.
-- Se guarda la descripcion y el precio ya calculados, para que la
-- factura siga siendo fiel aunque luego cambien tarifas o consumos.
-- ---------------------------------------------------------------------
CREATE TABLE detalle_factura (
    id_detalle      INT AUTO_INCREMENT PRIMARY KEY,
    id_factura      INT           NOT NULL,
    descripcion     VARCHAR(200)  NOT NULL,
    cantidad        DECIMAL(10,2) NOT NULL DEFAULT 1,
    precio_unitario DECIMAL(10,2) NOT NULL,
    importe         DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_factura FOREIGN KEY (id_factura)
        REFERENCES factura (id_factura) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- CARGO: consumos y gastos que se anotan a la cuenta de la reserva.
-- Si 'id_ocupante' viene lleno, el consumo queda atribuido a ese
-- huesped y puede facturarse por separado.
-- 'id_factura' se llena cuando el cargo ya fue facturado, para que no
-- se cobre dos veces.
-- ---------------------------------------------------------------------
CREATE TABLE cargo (
    id_cargo        INT AUTO_INCREMENT PRIMARY KEY,
    id_reserva      INT           NOT NULL,
    id_ocupante     INT           NULL,
    concepto        VARCHAR(150)  NOT NULL,
    categoria       ENUM('RESTAURANTE','MINIBAR','LAVANDERIA','SPA','TELEFONO','TRANSPORTE','OTRO')
                    NOT NULL DEFAULT 'OTRO',
    cantidad        DECIMAL(10,2) NOT NULL DEFAULT 1,
    precio_unitario DECIMAL(10,2) NOT NULL,
    monto           DECIMAL(10,2) NOT NULL,
    id_factura      INT           NULL,
    fecha           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario      INT           NOT NULL,
    CONSTRAINT ck_cargo_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_cargo_precio   CHECK (precio_unitario >= 0),
    CONSTRAINT fk_cargo_reserva FOREIGN KEY (id_reserva)
        REFERENCES reserva (id_reserva) ON DELETE CASCADE,
    CONSTRAINT fk_cargo_ocupante FOREIGN KEY (id_ocupante)
        REFERENCES ocupante (id_ocupante),
    CONSTRAINT fk_cargo_factura FOREIGN KEY (id_factura)
        REFERENCES factura (id_factura),
    CONSTRAINT fk_cargo_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- PAGO: todo el dinero que entra (o sale) asociado a una reserva.
-- El ANTICIPO que pide el enunciado es simplemente un pago registrado
-- antes del check-in.
-- ---------------------------------------------------------------------
CREATE TABLE pago (
    id_pago    INT AUTO_INCREMENT PRIMARY KEY,
    id_reserva INT           NOT NULL,
    tipo       ENUM('ANTICIPO','ABONO','LIQUIDACION','REEMBOLSO') NOT NULL DEFAULT 'ABONO',
    metodo     ENUM('EFECTIVO','TARJETA','TRANSFERENCIA','DEPOSITO') NOT NULL DEFAULT 'EFECTIVO',
    monto      DECIMAL(10,2) NOT NULL,
    referencia VARCHAR(80)   NULL,
    fecha      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT           NOT NULL,
    CONSTRAINT ck_pago_monto CHECK (monto > 0),
    CONSTRAINT fk_pago_reserva FOREIGN KEY (id_reserva)
        REFERENCES reserva (id_reserva) ON DELETE CASCADE,
    CONSTRAINT fk_pago_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
) ENGINE=InnoDB;

-- =====================================================================
--  VISTAS DE APOYO
-- =====================================================================

-- Estado de cuenta de cada reserva: hospedaje + consumos - pagos.
-- El sistema la usa para mostrar saldos en check-out y facturacion.
-- Una reserva CANCELADA no cobra hospedaje: el huesped nunca ocupo la
-- habitacion, asi que solo quedan vivos sus consumos y sus pagos (que
-- normalmente se devuelven con un REEMBOLSO).
CREATE OR REPLACE VIEW vw_estado_cuenta AS
SELECT
    r.id_reserva,
    r.codigo,
    r.id_cliente,
    CONCAT(c.nombre, ' ', c.apellido)                       AS cliente,
    h.numero                                                AS habitacion,
    r.fecha_entrada,
    r.fecha_salida,
    r.noches,
    r.tarifa_noche,
    r.estado,
    ROUND(CASE WHEN r.estado IN ('CANCELADA','NO_SHOW') THEN 0
               ELSE r.noches * r.tarifa_noche END, 2)       AS total_hospedaje,
    COALESCE((SELECT SUM(cg.monto) FROM cargo cg
              WHERE cg.id_reserva = r.id_reserva), 0)       AS total_consumos,
    COALESCE((SELECT SUM(CASE WHEN p.tipo = 'REEMBOLSO' THEN -p.monto ELSE p.monto END)
              FROM pago p WHERE p.id_reserva = r.id_reserva), 0) AS total_pagado,
    ROUND(
        CASE WHEN r.estado IN ('CANCELADA','NO_SHOW') THEN 0
             ELSE r.noches * r.tarifa_noche END
        + COALESCE((SELECT SUM(cg.monto) FROM cargo cg
                    WHERE cg.id_reserva = r.id_reserva), 0)
        - COALESCE((SELECT SUM(CASE WHEN p.tipo = 'REEMBOLSO' THEN -p.monto ELSE p.monto END)
                    FROM pago p WHERE p.id_reserva = r.id_reserva), 0)
    , 2)                                                    AS saldo_pendiente
FROM reserva r
JOIN cliente    c ON c.id_cliente    = r.id_cliente
JOIN habitacion h ON h.id_habitacion = r.id_habitacion;

-- Historial economico del cliente: cuantas veces vino y cuanto dejo.
-- Responde al requerimiento "historial de clientes con datos
-- economicos, visitas y temporadas".
CREATE OR REPLACE VIEW vw_historial_cliente AS
SELECT
    c.id_cliente,
    CONCAT(c.nombre, ' ', c.apellido) AS cliente,
    c.documento,
    c.telefono,
    c.correo,
    COUNT(r.id_reserva)                                      AS total_reservas,
    SUM(CASE WHEN r.estado = 'FINALIZADA' THEN 1 ELSE 0 END) AS visitas_completadas,
    SUM(CASE WHEN r.estado = 'CANCELADA'  THEN 1 ELSE 0 END) AS cancelaciones,
    COALESCE(SUM(CASE WHEN r.estado <> 'CANCELADA'
                      THEN r.noches ELSE 0 END), 0)          AS noches_totales,
    COALESCE(SUM(CASE WHEN r.estado <> 'CANCELADA'
                      THEN ec.total_hospedaje + ec.total_consumos ELSE 0 END), 0) AS consumo_historico,
    COALESCE(SUM(CASE WHEN r.estado <> 'CANCELADA'
                      THEN ec.saldo_pendiente ELSE 0 END), 0) AS saldo_actual,
    MAX(r.fecha_entrada)                                     AS ultima_visita
FROM cliente c
LEFT JOIN reserva          r  ON r.id_cliente  = c.id_cliente
LEFT JOIN vw_estado_cuenta ec ON ec.id_reserva = r.id_reserva
GROUP BY c.id_cliente, c.nombre, c.apellido, c.documento, c.telefono, c.correo;
