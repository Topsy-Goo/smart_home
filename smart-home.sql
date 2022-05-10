CREATE DATABASE `smarthome` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

CREATE TABLE `contracts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `master_uuid` varchar(45) NOT NULL,
  `master_task` varchar(128) NOT NULL,
  `slave_uuid` varchar(45) NOT NULL,
  `function_uuid` varchar(45) NOT NULL,
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
  `uuid` varchar(45) NOT NULL,
  `name` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid_UNIQUE` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `smarthome`.`friendly_names`
(`uuid`,
`name`)
VALUES
(<{uuid: }>,
<{name: }>);
