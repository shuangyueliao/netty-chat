/*
Navicat MySQL Data Transfer

Source Server         : link1
Source Server Version : 50722
Source Host           : localhost:3306
Source Database       : nettychat

Target Server Type    : MYSQL
Target Server Version : 50722
File Encoding         : 65001

Date: 2019-08-28 12:27:37
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `account`
-- ----------------------------
DROP TABLE IF EXISTS `account`;
CREATE TABLE `account` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `groupNumber` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of account
-- ----------------------------
INSERT INTO `account` VALUES ('1', 'test1', '123456', '123');
INSERT INTO `account` VALUES ('2', 'test2', '123456', '123');
INSERT INTO `account` VALUES ('3', 'test3', '123456', '123');
INSERT INTO `account` VALUES ('4', 'test4', '123456', '234');
INSERT INTO `account` VALUES ('5', 'test5', '123456', '234');
INSERT INTO `account` VALUES ('6', 'test6', '123456', '234');
INSERT INTO `account` VALUES ('7', 'test7', '123456', '345');
INSERT INTO `account` VALUES ('8', 'test8', '123456', '345');
INSERT INTO `account` VALUES ('9', 'test9', '123456', '345');
