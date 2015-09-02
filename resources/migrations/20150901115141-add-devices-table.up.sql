CREATE TABLE IF NOT EXISTS `devices` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `macaddr` varchar(17) NOT NULL,
  `name` varchar(32) DEFAULT NULL,
  `owner` int(11) DEFAULT NULL,
  `status` enum('present','absent','idle') NOT NULL DEFAULT 'present',
  `last_status_change` timestamp NULL DEFAULT NULL,
  `last_seen` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `macaddr` (`macaddr`),
  KEY `name` (`name`),
  KEY `owner` (`owner`),
  CONSTRAINT `device_to_user` FOREIGN KEY (`owner`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;
