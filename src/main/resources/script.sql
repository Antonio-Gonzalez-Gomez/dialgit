CREATE TABLE `server` (
  `id` bigint NOT NULL,
  `url` varchar(40) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `channel` (
  `id` bigint NOT NULL,
  `url` varchar(40) NOT NULL,
  `installation` int NOT NULL,
  `server_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `server_id_idx` (`server_id`),
  CONSTRAINT `server_id` FOREIGN KEY (`server_id`) REFERENCES `server` (`id`) ON DELETE CASCADE
);

CREATE TABLE `member` (
  `id` bigint NOT NULL,
  `name` varchar(60) NOT NULL,
  PRIMARY KEY (`id`)
);