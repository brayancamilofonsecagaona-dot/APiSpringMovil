-- Borra los datos de prueba que insertó V1__esquema.sql.
-- V1 no se edita porque Flyway guarda su checksum.
-- Orden: notas, materias y usuario, por las llaves foráneas (ON DELETE RESTRICT).

DELETE FROM notas
WHERE id = '3f6c2a1e-8b7d-4c1a-9e2f-5a0b1c2d3e4f';

DELETE FROM materias
WHERE id = 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d';

DELETE FROM usuarios
WHERE id = 'uid-prueba-1';
