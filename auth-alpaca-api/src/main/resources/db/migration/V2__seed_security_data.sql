-- V2__seed_security_data.sql

INSERT INTO permissions(created_at,updated_at,id,created_by,name,updated_by) VALUES ('2026-05-30 00:09:14.383091-05','2026-05-30 00:09:14.383091-05','019e7749-51ff-74ce-9ff0-eeb5122af7eb',NULL,'READ',NULL);
INSERT INTO permissions(created_at,updated_at,id,created_by,name,updated_by) VALUES ('2026-05-30 00:09:14.427208-05','2026-05-30 00:09:14.427208-05','019e7749-523b-7029-ba63-bf8cc1dab798',NULL,'UPDATE',NULL);
INSERT INTO permissions(created_at,updated_at,id,created_by,name,updated_by) VALUES ('2026-05-30 00:09:14.431381-05','2026-05-30 00:09:14.431381-05','019e7749-523f-7b74-9861-2664dc5f8195',NULL,'DELETE',NULL);
INSERT INTO permissions(created_at,updated_at,id,created_by,name,updated_by) VALUES ('2026-05-30 00:09:14.435966-05','2026-05-30 00:09:14.435966-05','019e7749-5243-7144-a6f0-5a26156fe33c',NULL,'CREATE',NULL);

INSERT INTO roles(created_at,updated_at,id,created_by,description,name,updated_by) VALUES ('2026-05-30 00:09:14.445076-05','2026-05-30 00:09:14.445076-05','019e7749-524a-794c-a5fc-2e0446d712fe',NULL,'It''s an admin','ADMIN',NULL);
INSERT INTO roles(created_at,updated_at,id,created_by,description,name,updated_by) VALUES ('2026-05-30 00:09:14.497924-05','2026-05-30 00:09:14.497924-05','019e7749-5281-7115-9cd2-7363b0d205a0',NULL,'It''s a user','USER',NULL);
INSERT INTO roles(created_at,updated_at,id,created_by,description,name,updated_by) VALUES ('2026-05-30 00:09:14.521468-05','2026-05-30 00:09:14.521468-05','019e7749-5299-737e-84ff-afc2a12b1f3d',NULL,'It''s a manager','MANAGER',NULL);

INSERT INTO role_permissions(created_at,id,permission_id,role_id) VALUES ('2026-05-30 00:09:14.456086-05','019e7749-5256-7225-80f4-6c5801a42d4d','019e7749-51ff-74ce-9ff0-eeb5122af7eb','019e7749-524a-794c-a5fc-2e0446d712fe');
INSERT INTO role_permissions(created_at,id,permission_id,role_id) VALUES ('2026-05-30 00:09:14.459642-05','019e7749-525b-7be0-b97c-f71f1dabebd2','019e7749-523b-7029-ba63-bf8cc1dab798','019e7749-524a-794c-a5fc-2e0446d712fe');
INSERT INTO role_permissions(created_at,id,permission_id,role_id) VALUES ('2026-05-30 00:09:14.460269-05','019e7749-525c-7d88-a563-6646775d8782','019e7749-5243-7144-a6f0-5a26156fe33c','019e7749-524a-794c-a5fc-2e0446d712fe');
INSERT INTO role_permissions(created_at,id,permission_id,role_id) VALUES ('2026-05-30 00:09:14.461081-05','019e7749-525c-7d88-a563-6646775d8783','019e7749-523f-7b74-9861-2664dc5f8195','019e7749-524a-794c-a5fc-2e0446d712fe');
INSERT INTO role_permissions(created_at,id,permission_id,role_id) VALUES ('2026-05-30 00:09:14.498677-05','019e7749-5282-7024-be00-e9e813d42f3d','019e7749-51ff-74ce-9ff0-eeb5122af7eb','019e7749-5281-7115-9cd2-7363b0d205a0');
INSERT INTO role_permissions(created_at,id,permission_id,role_id) VALUES ('2026-05-30 00:09:14.499105-05','019e7749-5282-7024-be00-e9e813d42f3e','019e7749-5243-7144-a6f0-5a26156fe33c','019e7749-5281-7115-9cd2-7363b0d205a0');
INSERT INTO role_permissions(created_at,id,permission_id,role_id) VALUES ('2026-05-30 00:09:14.522337-05','019e7749-529a-7a72-9899-7b8d35bb0f88','019e7749-51ff-74ce-9ff0-eeb5122af7eb','019e7749-5299-737e-84ff-afc2a12b1f3d');
INSERT INTO role_permissions(created_at,id,permission_id,role_id) VALUES ('2026-05-30 00:09:14.522795-05','019e7749-529a-7a72-9899-7b8d35bb0f89','019e7749-5243-7144-a6f0-5a26156fe33c','019e7749-5299-737e-84ff-afc2a12b1f3d');
INSERT INTO role_permissions(created_at,id,permission_id,role_id) VALUES ('2026-05-30 00:09:14.523124-05','019e7749-529b-7e20-92aa-1e9327385d78','019e7749-523b-7029-ba63-bf8cc1dab798','019e7749-5299-737e-84ff-afc2a12b1f3d');