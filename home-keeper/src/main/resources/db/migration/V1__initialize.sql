CREATE TABLE ourusers
(	id			bigserial,
	login		VARCHAR(36) NOT NULL UNIQUE,	-- 36 — чтобы в cartKeyByLogin() прошёл uuid (36 символов)
	password	VARCHAR(64) NOT NULL,	-- размер 64 не для пароля юзера, а для хэша (хэш, похоже, всегда занимает 60 символов. Даже для пароля длиннее в 128 символов)
	activCode   VARCHAR(33) NOT NULL UNIQUE,
	created_at	TIMESTAMP DEFAULT current_timestamp,
	updated_at	TIMESTAMP DEFAULT current_timestamp,
	PRIMARY KEY (id)
);
INSERT INTO ourusers (login, password, email) VALUES
	('admin',
	 '$2a$12$c4HYjryn7vo1bYQfSzkUDe8jPhYIpInbUKZmv5lGnmcyrQPLIWnVu',
	 '8K7CA5-CB41-48Q2-9C69-136E-2D472L');
	 -- пароль 100
-- ----------------------------------------------------------------------
