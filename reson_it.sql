# ************************************************************
# Sequel Pro SQL dump
# Version 4096
#
# http://www.sequelpro.com/
# http://code.google.com/p/sequel-pro/
#
# Host: localhost (MySQL 5.6.27)
# Database: reson_it
# Generation Time: 2016-01-25 13:25:23 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table order_items
# ------------------------------------------------------------

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

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;

INSERT INTO `order_items` (`order_id`, `item_id`, `float`, `double`, `boolean`, `bigint`, `decimal`, `unsignedint`, `varchar`, `date`, `datetime`, `timestamp`, `year`)
VALUES
	(1,1,2.3,2.22,1,93939393,999.22,99,'lolol','2015-12-25','2015-12-25 23:11:33','2015-12-25 21:11:11','2016');

/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table orders
# ------------------------------------------------------------

DROP TABLE IF EXISTS `orders`;

CREATE TABLE `orders` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ref` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;

INSERT INTO `orders` (`id`, `ref`)
VALUES
	(1,'RR34567890'),
	(5,'UU123');

/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;


# Dump of table orders_view
# ------------------------------------------------------------

DROP VIEW IF EXISTS `orders_view`;

CREATE TABLE `orders_view` (
   `id` INT(11) NOT NULL DEFAULT '0',
   `ref` VARCHAR(255) NOT NULL
) ENGINE=MyISAM;





# Replace placeholder table for orders_view with correct view syntax
# ------------------------------------------------------------

DROP TABLE `orders_view`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `orders_view` AS (select `orders`.`id` AS `id`,`orders`.`ref` AS `ref` from `orders`);

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
