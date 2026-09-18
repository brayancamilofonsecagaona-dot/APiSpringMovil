CREATE TABLE usuarios (
    id              TEXT PRIMARY KEY,
    correo          VARCHAR(120) NOT NULL UNIQUE,
    nombre          VARCHAR(80)  NOT NULL,
    nombre_usuario  VARCHAR(40)  NOT NULL UNIQUE,
    foto_url        TEXT,
    fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE materias (
    id                  UUID PRIMARY KEY, 
    usuario_id          TEXT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    nombre              VARCHAR(60) NOT NULL,
    color               VARCHAR(7)  NOT NULL CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    icono               VARCHAR(30) NOT NULL,
    fecha_creacion      TIMESTAMPTZ NOT NULL,
    fecha_modificacion  TIMESTAMPTZ NOT NULL,
    eliminado           BOOLEAN     NOT NULL DEFAULT false
);


CREATE INDEX idx_materias_usuario_id ON materias(usuario_id);

CREATE TABLE notas (
    id                  UUID PRIMARY KEY,
    materia_id          UUID NOT NULL,
    usuario_id          TEXT NOT NULL,
    titulo              VARCHAR(120) NOT NULL,
    texto               TEXT         NOT NULL DEFAULT '',

    imagen_url          TEXT,
    fecha_creacion      TIMESTAMPTZ  NOT NULL,
    fecha_modificacion  TIMESTAMPTZ  NOT NULL,
    eliminado           BOOLEAN      NOT NULL DEFAULT false,
 
    FOREIGN KEY (materia_id) REFERENCES materias(id) ON DELETE RESTRICT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE INDEX idx_notas_materia_id ON notas(materia_id);
CREATE INDEX idx_notas_usuario_id ON notas(usuario_id);




INSERT INTO usuarios (id, correo, nombre, nombre_usuario)
VALUES ('uid-prueba-1', 'brayan@test.com', 'Brayan', 'brayitan');

INSERT INTO materias (id, usuario_id, nombre, color, icono, fecha_creacion, fecha_modificacion)
VALUES ('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', 'uid-prueba-1', 'Móviles', '#FFC93D', 'codigo', now(), now());

INSERT INTO notas (id, materia_id, usuario_id, titulo, fecha_creacion, fecha_modificacion)
VALUES ('3f6c2a1e-8b7d-4c1a-9e2f-5a0b1c2d3e4f', 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', 'uid-prueba-1', 'Estructuras de control', now(), now());