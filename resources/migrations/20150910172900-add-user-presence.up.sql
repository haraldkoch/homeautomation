ALTER TABLE  `users` ADD  `presence` ENUM(  'HOME',  'AWAY',  'UNKNOWN' ) NOT NULL DEFAULT  'UNKNOWN' AFTER  `username`;