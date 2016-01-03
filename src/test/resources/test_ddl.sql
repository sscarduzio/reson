DROP TABLE IF EXISTS `order_items`;
CREATE TABLE `order_items` (
  `order_id` int(11) NOT NULL,
  `item_id` int(11) NOT NULL,
  `float` float NOT NULL,
  `double` double DEFAULT NULL,
  `boolean` tinyint(4) DEFAULT NULL,
  `bigint` bigint(20) DEFAULT NULL,
  `decimal` decimal(10,2) DEFAULT NULL,
  `unsignedint` int(11) unsigned DEFAULT NULL,
  `varchar` varchar(44) DEFAULT NULL,
  `date` date DEFAULT NULL,
  `datetime` datetime DEFAULT NULL,
  `timestamp` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `year` year(4) DEFAULT NULL,
  PRIMARY KEY (`order_id`,`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `order_items` (`order_id`, `item_id`, `float`, `double`, `boolean`, `bigint`, `decimal`, `unsignedint`, `varchar`, `date`, `datetime`, `timestamp`, `year`)
VALUES
	(1,1,2.3,2.22,1,93939393,999.22,99,'lolol','2015-12-25','2015-12-25 23:11:33','2015-12-25 21:11:11','2016');

DROP TABLE IF EXISTS `orders`;

CREATE TABLE `orders` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ref` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `orders` (`id`, `ref`)
VALUES
	(1,'RR34567890');
