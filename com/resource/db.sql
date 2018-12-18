/*
SQLyog Ultimate - MySQL GUI v8.2 
MySQL - 5.5.29 : Database - coolcode
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`coolcode` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `coolcode`;

/*Table structure for table `mailworker_log` */

DROP TABLE IF EXISTS `mailworker_log`;

CREATE TABLE `mailworker_log` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `from` varchar(50) DEFAULT NULL COMMENT '发件人',
  `to` varchar(50) DEFAULT NULL COMMENT '收件人',
  `cc` varchar(50) DEFAULT NULL COMMENT '抄送人',
  `bcc` varchar(50) DEFAULT NULL COMMENT '密送人',
  `title` varchar(200) DEFAULT NULL COMMENT '标题',
  `content` text COMMENT '内容',
  `attachment_names` varchar(200) DEFAULT NULL COMMENT '附件名称',
  `ip` varchar(20) DEFAULT NULL COMMENT '对方ip',
  `flag` char(1) DEFAULT NULL COMMENT '成功标记0-成功,1-失败，2-特殊处理',
  `retry_times` smallint(2) DEFAULT '0' COMMENT '失败次数',
  `message` varchar(500) DEFAULT NULL COMMENT '发送返回信息',
  `sendtime` datetime DEFAULT NULL COMMENT '开始时间',
  `endtime` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8;

/*Data for the table `mailworker_log` */

insert  into `mailworker_log`(`id`,`from`,`to`,`cc`,`bcc`,`title`,`content`,`attachment_names`,`ip`,`flag`,`retry_times`,`message`,`sendtime`,`endtime`) values (37,'xxxx@xxxx.com','chenxiaodan@genscript.com','','','邮件12345','<html><body>wo men  shi ces youj a</body></html>','02_test20131108105947058.xls','','0',0,'解析MX记录为空,无法解析获取邮件服务器地址','2013-11-08 10:11:11','2013-11-08 11:03:13'),(38,'test@test.com','jackw@xxxxx.com','','','xml数据','','','','0',0,NULL,'2013-11-28 08:55:35','2013-11-28 08:55:35'),(39,'test@test.com','3213213123131@pano.com','','','xml数据','1111111111111111','','','1',0,'连接对方邮件服务器[mailstore1.secureserver.net.]失败,原因:Connection timed out: connect','2013-12-20 09:25:58','2013-12-20 09:27:22'),(40,'test@test.com',' 321331313@genscript.com','','','xml数据','1111111111111111','','','0',0,'send to [321331313@genscript.com] is success','2013-12-20 09:26:45','2013-12-20 09:27:22');

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
