-- Quien te presta el inventario en cada evento
CREATE TABLE planeador (
                           id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           nombre      VARCHAR(150) NOT NULL,
                           telefono    VARCHAR(30),
                           notas       TEXT,
                           created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                           updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Catálogo genérico, tuyo, reutilizable en cualquier evento
CREATE TABLE producto (
                          id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          nombre           VARCHAR(150) NOT NULL,
                          tipo             VARCHAR(20)  NOT NULL,
                          foto_referencia  VARCHAR(500),
                          activo           BOOLEAN      NOT NULL DEFAULT TRUE,
                          created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
                          updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
                          CONSTRAINT ck_producto_tipo
                              CHECK (tipo IN ('LICOR', 'MENAJE', 'CRISTALERIA'))
);

CREATE TABLE evento (
                        id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        planeador_id  UUID         NOT NULL REFERENCES planeador (id),
                        nombre        VARCHAR(200) NOT NULL,
                        lugar         VARCHAR(200),
                        fecha         DATE         NOT NULL,
                        estado        VARCHAR(20)  NOT NULL DEFAULT 'PLANEADO',
                        created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                        updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                        CONSTRAINT ck_evento_estado
                            CHECK (estado IN ('PLANEADO', 'RECIBIDO', 'DEVUELTO', 'CERRADO'))
);

CREATE INDEX idx_evento_planeador ON evento (planeador_id);
CREATE INDEX idx_evento_fecha     ON evento (fecha);

-- Cada producto dentro de un evento, con su cuadre
CREATE TABLE evento_item (
                             id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             evento_id                UUID    NOT NULL REFERENCES evento (id) ON DELETE CASCADE,
                             producto_id              UUID    NOT NULL REFERENCES producto (id),
                             cantidad_recibida        INTEGER NOT NULL DEFAULT 0,
                             cantidad_devuelta        INTEGER NOT NULL DEFAULT 0,
                             cantidad_rota            INTEGER NOT NULL DEFAULT 0,
                             cantidad_perdida         INTEGER NOT NULL DEFAULT 0,
                             cantidad_consumida       INTEGER NOT NULL DEFAULT 0,
                             observaciones_recepcion  TEXT,
                             CONSTRAINT uq_evento_item UNIQUE (evento_id, producto_id),
                             CONSTRAINT ck_evento_item_cantidades CHECK (
                                 cantidad_recibida  >= 0 AND cantidad_devuelta  >= 0 AND
                                 cantidad_rota      >= 0 AND cantidad_perdida   >= 0 AND
                                 cantidad_consumida >= 0
                                 )
);

CREATE INDEX idx_evento_item_producto ON evento_item (producto_id);

-- Evidencia fotográfica de cada evento
CREATE TABLE foto (
                      id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      evento_id    UUID         NOT NULL REFERENCES evento (id) ON DELETE CASCADE,
                      producto_id  UUID         REFERENCES producto (id),
                      momento      VARCHAR(20)  NOT NULL,
                      ruta         VARCHAR(500) NOT NULL,
                      tomada_en    TIMESTAMPTZ  NOT NULL,
                      created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
                      CONSTRAINT ck_foto_momento
                          CHECK (momento IN ('RECEPCION', 'DEVOLUCION'))
);

CREATE INDEX idx_foto_evento ON foto (evento_id);