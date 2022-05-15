CREATE DATABASE `smarthome` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

CREATE TABLE `contracts` (
  `id` 			  int 		   NOT NULL AUTO_INCREMENT,
  `master_uuid`   varchar(45)  NOT NULL,
  `master_task`   varchar(128) NOT NULL,
  `slave_uuid`    varchar(45)  NOT NULL,
  `function_uuid` varchar(45)  NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `smarthome`.`contracts`
(`id`,
`master_uuid`,
`master_task`,
`slave_uuid`,
`function_uuid`)
VALUES
(<{id: }>,
<{master_uuid: }>,
<{master_task: }>,
<{slave_uuid: }>,
<{function_uuid: }>);

CREATE TABLE `friendly_names` (
  `uuid` varchar(45)  NOT NULL,
  `name` varchar(128) NOT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid_UNIQUE` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `smarthome`.`friendly_names`
(`uuid`,
`name`)
VALUES
(<{uuid: }>,
<{name: }>);

CREATE TABLE `schedule_records` (
  `id` 			int 		 NOT NULL AUTO_INCREMENT,
  `device_uuid` varchar(45)  NOT NULL,
  `task_name` 	varchar(128) NOT NULL,
  `date_time` 	datetime 	 NOT NULL,
  `created_at` 	datetime 	 NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'TIMESTAMP has a range of 1970-01-01 00:00:01 UTC to 2038-01-19 03:14:07 UTC.\nDATETIME has a range of 1000-01-01 00:00:00 to 9999-12-31 23:59:59.',
  `state` 		varchar(45)  NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_friendly_names_idx` (`device_uuid`),
  CONSTRAINT `fk_friendly_names` FOREIGN KEY (`device_uuid`) REFERENCES `friendly_names` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Колонка state пока не используется в приложении и всегда содержит строку 'Бездействует'.

INSERT INTO `smarthome`.`schedule_records`
(`id`,
`device_uuid`,
`task_name`,
`date_time`,
`created_at`,
`state`)
VALUES
(<{id: }>,
<{device_uuid: }>,
<{task_name: }>,
<{date_time: }>,
<{created_at: CURRENT_TIMESTAMP}>,
<{state: }>);

CREATE TABLE `users` (
  `id`			int 		NOT NULL AUTO_INCREMENT,
  `login`		varchar(45) NOT NULL,
  `password`	varchar(64) NOT NULL,
  `updated_at`	timestamp 	NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci

-- Умолчальные логин и пароль : admin / admin
-- Значения для них в таблице : admin / '$2a$10$OvajFCiMvmgvt5vH7oNIOeu5gQ.7uPBQfAjAA5a6dQU2EiKu3uS2i'

INSERT INTO `smarthome`.`users`
(`id`,
`login`,
`password`,
`updated_at`)
VALUES
(<{id: }>,
<{login: }>,
<{password: }>,
<{updated_at: CURRENT_TIMESTAMP}>);
