-- MySQL dump 10.13  Distrib 8.0.43, for Linux (aarch64)
--
-- Host: localhost    Database: sis
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `attendance_records`
--

DROP TABLE IF EXISTS `attendance_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_records` (
  `record_id` int NOT NULL AUTO_INCREMENT,
  `session_id` int NOT NULL COMMENT 'FK -> attendance_sessions.session_id, đợt điểm danh',
  `enrollment_id` int NOT NULL COMMENT 'FK -> enrollments.enrollment_id, mã ghi danh',
  `student_id` int NOT NULL COMMENT 'FK -> students.student_id, mã học viên',
  `status` enum('ABSENT','PRESENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  `created_by` int DEFAULT NULL COMMENT 'Người tạo bản ghi',
  `updated_by` int DEFAULT NULL COMMENT 'Người cập nhật bản ghi',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Đánh dấu xóa mềm',
  `deleted_at` datetime DEFAULT NULL COMMENT 'Thời điểm xóa',
  `deleted_by` int DEFAULT NULL COMMENT 'Người xóa bản ghi',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_attendance_records_session_student` (`session_id`,`student_id`),
  KEY `fk_attendance_records_enrollment` (`enrollment_id`),
  KEY `fk_attendance_records_created_by` (`created_by`),
  KEY `fk_attendance_records_updated_by` (`updated_by`),
  KEY `fk_attendance_records_deleted_by` (`deleted_by`),
  KEY `idx_attendance_records_session` (`session_id`) COMMENT 'Tìm bản ghi điểm danh theo đợt điểm danh',
  KEY `idx_attendance_records_student` (`student_id`,`created_at` DESC) COMMENT 'Tra cứu điểm danh của học viên',
  KEY `idx_attendance_records_status` (`session_id`,`status`) COMMENT 'Lọc theo trạng thái có mặt/vắng mặt',
  CONSTRAINT `fk_attendance_records_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_attendance_records_deleted_by` FOREIGN KEY (`deleted_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_attendance_records_enrollment` FOREIGN KEY (`enrollment_id`) REFERENCES `enrollments` (`enrollment_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_attendance_records_session` FOREIGN KEY (`session_id`) REFERENCES `attendance_sessions` (`session_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_attendance_records_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_attendance_records_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_records`
--

LOCK TABLES `attendance_records` WRITE;
/*!40000 ALTER TABLE `attendance_records` DISABLE KEYS */;
INSERT INTO `attendance_records` VALUES (1,1,5,4,'ABSENT','sinh viên nước ngoài',5,5,'2026-04-02 03:28:01','2026-07-02 09:24:36',0,NULL,NULL),(2,1,4,3,'ABSENT',NULL,5,5,'2026-04-02 03:28:01','2026-07-02 09:24:36',0,NULL,NULL),(3,1,3,2,'PRESENT',NULL,5,5,'2026-04-02 03:28:01','2026-07-02 09:24:36',0,NULL,NULL),(4,2,5,4,'PRESENT',NULL,5,5,'2026-07-13 08:37:09','2026-07-13 08:55:47',0,NULL,NULL),(5,2,4,3,'PRESENT',NULL,5,5,'2026-07-13 08:37:09','2026-07-13 08:55:47',0,NULL,NULL),(6,2,3,2,'PRESENT',NULL,5,5,'2026-07-13 08:37:09','2026-07-13 08:55:47',0,NULL,NULL),(7,3,14,4,'PRESENT','Điểm danh bằng mã lúc 2026-07-23T16:08',5,25,'2026-07-23 09:07:44','2026-07-23 09:08:34',0,NULL,NULL),(8,3,13,3,'ABSENT',NULL,5,NULL,'2026-07-23 09:07:44','2026-07-23 09:07:44',0,NULL,NULL),(9,3,12,5,'ABSENT',NULL,5,NULL,'2026-07-23 09:07:44','2026-07-23 09:07:44',0,NULL,NULL),(10,3,11,2,'ABSENT',NULL,5,NULL,'2026-07-23 09:07:44','2026-07-23 09:07:44',0,NULL,NULL);
/*!40000 ALTER TABLE `attendance_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_sessions`
--

DROP TABLE IF EXISTS `attendance_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_sessions` (
  `session_id` int NOT NULL AUTO_INCREMENT,
  `class_id` int NOT NULL COMMENT 'FK -> classes.class_id, lớp học',
  `teacher_id` int NOT NULL COMMENT 'FK -> users.user_id, giảng viên điểm danh',
  `attendance_date` date NOT NULL COMMENT 'Ngày điểm danh',
  `notes` text COLLATE utf8mb4_unicode_ci,
  `total_students` int NOT NULL DEFAULT '0' COMMENT 'Tổng số học viên',
  `present_count` int NOT NULL DEFAULT '0' COMMENT 'Số học viên có mặt',
  `absent_count` int NOT NULL DEFAULT '0' COMMENT 'Số học viên vắng mặt',
  `created_by` int DEFAULT NULL COMMENT 'Người tạo đợt điểm danh',
  `updated_by` int DEFAULT NULL COMMENT 'Người cập nhật đợt điểm danh',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Đánh dấu xóa mềm',
  `deleted_at` datetime DEFAULT NULL COMMENT 'Thời điểm xóa',
  `deleted_by` int DEFAULT NULL COMMENT 'Người xóa đợt điểm danh',
  `study_days` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Original study days when attendance was taken - preserved even if class schedule changes',
  `study_time` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Original study time when attendance was taken - preserved even if class schedule changes',
  `attendance_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MÃ£ Ä‘iá»ƒm danh do giáº£ng viÃªn Ä‘áº·t',
  `code_enabled` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Báºt/táº¯t Ä‘iá»ƒm danh báº±ng mÃ£',
  `code_expires_at` datetime DEFAULT NULL COMMENT 'Thá»i Ä‘iá»ƒm háº¿t háº¡n mÃ£',
  PRIMARY KEY (`session_id`),
  UNIQUE KEY `uk_attendance_sessions_class_date` (`class_id`,`attendance_date`),
  KEY `fk_attendance_sessions_created_by` (`created_by`),
  KEY `fk_attendance_sessions_updated_by` (`updated_by`),
  KEY `fk_attendance_sessions_deleted_by` (`deleted_by`),
  KEY `idx_attendance_sessions_class_date` (`class_id`,`attendance_date` DESC,`session_id` DESC) COMMENT 'Tìm đợt điểm danh theo lớp và ngày',
  KEY `idx_attendance_sessions_teacher_date` (`teacher_id`,`attendance_date` DESC,`session_id` DESC) COMMENT 'Tìm đợt điểm danh theo giảng viên và ngày',
  KEY `idx_attendance_sessions_code` (`attendance_code`,`code_enabled`),
  CONSTRAINT `fk_attendance_sessions_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_attendance_sessions_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_attendance_sessions_deleted_by` FOREIGN KEY (`deleted_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_attendance_sessions_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_attendance_sessions_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_sessions`
--

LOCK TABLES `attendance_sessions` WRITE;
/*!40000 ALTER TABLE `attendance_sessions` DISABLE KEYS */;
INSERT INTO `attendance_sessions` VALUES (1,5,5,'2026-04-06','',3,1,2,5,5,'2026-04-02 03:28:01','2026-07-02 09:24:36',0,NULL,NULL,'MONDAY,TUESDAY','AFTERNOON',NULL,0,NULL),(2,5,5,'2026-06-30','',3,3,0,5,5,'2026-07-13 08:37:09','2026-07-13 08:55:47',0,NULL,NULL,'MONDAY,TUESDAY','AFTERNOON','TOICE123',1,NULL),(3,10,5,'2026-07-23','',4,1,3,5,5,'2026-07-23 09:07:44','2026-07-23 09:08:34',0,NULL,NULL,'WEDNESDAY,THURSDAY,FRIDAY','EVENING','ABC12',1,NULL);
/*!40000 ALTER TABLE `attendance_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `centers`
--

DROP TABLE IF EXISTS `centers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `centers` (
  `center_id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `established_date` date DEFAULT NULL,
  `description` text,
  `deleted_at` datetime DEFAULT NULL,
  `created_by` int DEFAULT NULL,
  `updated_by` int DEFAULT NULL,
  `address_line` varchar(500) NOT NULL,
  `province` varchar(100) NOT NULL,
  `district` varchar(100) NOT NULL,
  `ward` varchar(100) NOT NULL,
  PRIMARY KEY (`center_id`),
  UNIQUE KEY `code` (`code`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `uk_centers_code` (`code`),
  UNIQUE KEY `uk_centers_name` (`name`),
  KEY `idx_centers_deleted_at` (`deleted_at`),
  KEY `idx_centers_province` (`province`),
  KEY `idx_centers_district` (`district`),
  KEY `idx_centers_code` (`code`),
  KEY `idx_centers_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `centers`
--

LOCK TABLES `centers` WRITE;
/*!40000 ALTER TABLE `centers` DISABLE KEYS */;
INSERT INTO `centers` VALUES (1,'HN01','trung tâm hà nội 1','2025-10-10 08:15:02','2025-10-13 02:17:11','email@education.vn','0987667589','2000-09-09',NULL,NULL,NULL,NULL,'AB09','ABv','hanoi','12'),(2,'DN01','Trung tâm Đà Nẵng','2026-07-02 09:19:34','2026-07-02 09:19:34','trung@gmail.com','0912345678','2027-03-02',NULL,NULL,NULL,NULL,'924 đường đà nẵng','Đà Nẵng','Quận Sơn Trà','Xã Bình Mỹ'),(3,'HCM3','trung tâm hcm 3','2026-07-29 06:42:23','2026-07-29 06:42:23','hcm@ecu.vn','0985157807','2026-07-29',NULL,NULL,NULL,NULL,'lk9 quận 1','TP. Hồ Chí Minh','Quận 1','Phường 10');
/*!40000 ALTER TABLE `centers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_messages`
--

DROP TABLE IF EXISTS `chat_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_messages` (
  `message_id` int NOT NULL AUTO_INCREMENT,
  `completion_ms` int DEFAULT NULL,
  `content` text NOT NULL,
  `cost_usd` decimal(10,6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `model` varchar(50) DEFAULT NULL,
  `role` enum('assistant','system','user') NOT NULL,
  `sources` json DEFAULT NULL,
  `tokens_used` int DEFAULT NULL,
  `session_id` int NOT NULL,
  PRIMARY KEY (`message_id`),
  KEY `FK3cpkdtwdxndrjhrx3gt9q5ux9` (`session_id`),
  CONSTRAINT `FK3cpkdtwdxndrjhrx3gt9q5ux9` FOREIGN KEY (`session_id`) REFERENCES `chat_sessions` (`session_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_messages`
--

LOCK TABLES `chat_messages` WRITE;
/*!40000 ALTER TABLE `chat_messages` DISABLE KEYS */;
INSERT INTO `chat_messages` VALUES (1,NULL,'người yêu tôi là ai',NULL,'2026-04-10 07:43:29.303822',NULL,'user',NULL,NULL,3),(2,1894,'Hiện tại tôi chưa có thông tin này trong tài liệu. Tôi không tìm thấy dữ liệu về người yêu của bạn trong hệ thống.',NULL,'2026-04-10 07:43:31.192200','command-r-plus','assistant','[]',NULL,3),(3,NULL,'t muốn hỏi về lịch sử điểm danh của minhf',NULL,'2026-07-23 16:49:56.566647',NULL,'user',NULL,NULL,9),(4,5424,'Chào bạn lil k,\n\nTheo thông tin trong hệ thống, bạn đang theo học hai lớp:\n\n- Lớp IELTS 5.5 (ID: 5) tại trung tâm Hà Nội 1, với lịch học vào các ngày thứ Hai và thứ Ba vào buổi chiều.\n- Lớp TOEIC 700 (ID: 10) tại trung tâm Hà Nội 1, với lịch học vào các ngày thứ Tư, thứ Năm, và thứ Sáu vào buổi tối.\n\nHiện tại, tôi chưa có thông tin chi tiết về lịch sử điểm danh của bạn trong các lớp học này. Bạn có thể liên hệ với giáo viên hoặc quản lý lớp học để cập nhật và nhận thông tin chính xác về lịch sử điểm danh.\n\nNếu bạn có thêm thắc mắc hoặc cần hỗ trợ khác, xin vui lòng cho tôi biết. Tôi sẵn sàng giúp đỡ bạn trong khả năng có thể.',NULL,'2026-07-23 16:50:01.988217','command-r-plus','assistant','[]',NULL,9),(5,NULL,'Lịch học tuần này của tôi như thế nào?',NULL,'2026-07-23 17:13:35.626293',NULL,'user',NULL,NULL,10),(6,7834,'Xin chào lil k, tôi là trợ lý AI của CodeGym.\n\nLịch học của bạn trong tuần này là vào thứ Tư, thứ Năm và thứ Sáu, vào buổi tối. Lớp học được tổ chức tại phòng B102-2.\n\nNếu bạn có thắc mắc thêm về lịch học hoặc cần hỗ trợ khác, xin hãy cho tôi biết!',NULL,'2026-07-23 17:13:43.456781','command-r-plus','assistant','[]',NULL,10),(7,NULL,'Lịch học tuần này của tôi như thế nào?',NULL,'2026-07-23 17:17:44.039026',NULL,'user',NULL,NULL,11),(8,7855,'Xin chào lil k!\n\nLịch học của bạn trong tuần này là vào các buổi tối thứ Tư, thứ Năm và thứ Sáu. Thời gian học cụ thể từ ngày 20/07/2026 đến 30/09/2026.\n\nBạn đang theo học lớp TOEIC 700 (ID: 10) tại trung tâm Hà Nội 1, và lớp học này đang ở trạng thái PLANNED. Phòng học được sắp xếp là phòng B102-2.\n\nNếu bạn có thắc mắc thêm về lịch học hoặc các thông tin khác, xin vui lòng cho tôi biết! Tôi sẵn sàng hỗ trợ.',NULL,'2026-07-23 17:17:51.889054','command-r-plus','assistant','[]',NULL,11),(9,NULL,'Thống kê tổng quan hệ thống hiện tại?',NULL,'2026-07-29 06:42:53.982851',NULL,'user',NULL,NULL,17),(10,3444,'Theo dữ liệu cập nhật vào ngày 2026-07-29, đây là thống kê tổng quan về hệ thống CodeGym:\n\n- Tổng số người dùng: 17 người\n- Tổng số học viên: 7 người\n- Số lượng học viên đang hoạt động: 4 người\n- Tổng số giảng viên: 4 người\n- Tổng số quản trị viên: 5 người\n\nĐây là thông tin mới nhất về tình trạng hệ thống tính đến hôm nay.',NULL,'2026-07-29 06:42:57.419497','command-r-plus','assistant','[]',NULL,17);
/*!40000 ALTER TABLE `chat_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_participants`
--

DROP TABLE IF EXISTS `chat_participants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_participants` (
  `participant_id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint NOT NULL,
  `user_id` int NOT NULL,
  `last_read_at` datetime DEFAULT NULL,
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`participant_id`),
  UNIQUE KEY `uk_room_user` (`room_id`,`user_id`),
  KEY `fk_chat_participants_user` (`user_id`),
  CONSTRAINT `fk_chat_participants_room` FOREIGN KEY (`room_id`) REFERENCES `chat_rooms` (`room_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_chat_participants_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_participants`
--

LOCK TABLES `chat_participants` WRITE;
/*!40000 ALTER TABLE `chat_participants` DISABLE KEYS */;
INSERT INTO `chat_participants` VALUES (17,9,5,'2026-07-14 17:13:14','2026-07-14 16:56:11'),(18,9,25,'2026-07-23 09:10:20','2026-07-14 16:56:11'),(19,10,5,'2026-07-28 17:02:21','2026-07-14 17:02:50'),(20,10,22,'2026-07-14 17:09:24','2026-07-14 17:02:50'),(21,10,25,'2026-07-23 09:10:31','2026-07-14 17:02:50'),(22,10,12,NULL,'2026-07-14 17:02:50'),(23,11,25,'2026-07-23 09:11:02','2026-07-23 09:10:58'),(24,11,12,NULL,'2026-07-23 09:10:58');
/*!40000 ALTER TABLE `chat_participants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_rooms`
--

DROP TABLE IF EXISTS `chat_rooms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_rooms` (
  `room_id` bigint NOT NULL AUTO_INCREMENT,
  `room_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'DIRECT or CLASS_GROUP',
  `class_id` int DEFAULT NULL COMMENT 'FK to classes.class_id if CLASS_GROUP',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Room name (e.g. class name or direct chat label)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`room_id`),
  KEY `fk_chat_rooms_class` (`class_id`),
  CONSTRAINT `fk_chat_rooms_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_rooms`
--

LOCK TABLES `chat_rooms` WRITE;
/*!40000 ALTER TABLE `chat_rooms` DISABLE KEYS */;
INSERT INTO `chat_rooms` VALUES (9,'DIRECT',NULL,'lil k','2026-07-14 16:56:11','2026-07-14 17:03:01'),(10,'CLASS_GROUP',5,'Nhóm lớp lớp Ielts 5.5','2026-07-14 17:02:50','2026-07-14 17:09:03'),(11,'DIRECT',NULL,'Nam','2026-07-23 09:10:58','2026-07-23 09:11:02');
/*!40000 ALTER TABLE `chat_rooms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chat_sessions`
--

DROP TABLE IF EXISTS `chat_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chat_sessions` (
  `session_id` int NOT NULL AUTO_INCREMENT,
  `context` json DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`session_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chat_sessions`
--

LOCK TABLES `chat_sessions` WRITE;
/*!40000 ALTER TABLE `chat_sessions` DISABLE KEYS */;
INSERT INTO `chat_sessions` VALUES (1,'{}','2026-04-01 16:03:55.751934','Trò chuyện với AI','2026-04-01 16:03:55.751940',7),(2,'{}','2026-04-02 10:36:32.345617','Trò chuyện với AI','2026-04-02 10:36:32.345633',7),(3,'{}','2026-04-10 07:43:12.141169','Trò chuyện với AI','2026-04-10 07:43:12.141171',7),(4,'{}','2026-04-10 08:18:29.966284','Trò chuyện với AI','2026-04-10 08:18:29.966287',25),(5,'{}','2026-07-23 09:20:40.689773','Trò chuyện với AI','2026-07-23 09:20:40.689775',5),(6,'{}','2026-07-23 16:47:27.068279','Trò chuyện với AI','2026-07-23 16:47:27.068282',7),(7,'{}','2026-07-23 16:48:08.202757','Trò chuyện với AI','2026-07-23 16:48:08.202758',7),(8,'{}','2026-07-23 16:49:16.848326','Trò chuyện với AI','2026-07-23 16:49:16.848327',7),(9,'{}','2026-07-23 16:49:38.787253','Trò chuyện với AI','2026-07-23 16:49:38.787254',25),(10,'{}','2026-07-23 17:13:28.124639','Trò chuyện với AI','2026-07-23 17:13:28.124641',25),(11,'{}','2026-07-23 17:17:42.771341','Trò chuyện với AI','2026-07-23 17:17:42.771344',25),(12,'{}','2026-07-28 17:02:23.936973','Trò chuyện với AI','2026-07-28 17:02:23.936974',5),(13,'{}','2026-07-28 17:04:34.308582','Trò chuyện với AI','2026-07-28 17:04:34.308584',5),(14,'{}','2026-07-28 17:04:46.640277','Trò chuyện với AI','2026-07-28 17:04:46.640279',5),(15,'{}','2026-07-28 17:06:18.335411','Trò chuyện với AI','2026-07-28 17:06:18.335412',5),(16,'{}','2026-07-28 17:06:23.595693','Trò chuyện với AI','2026-07-28 17:06:23.595694',5),(17,'{}','2026-07-29 06:40:13.212213','Trò chuyện với AI','2026-07-29 06:40:13.212214',7);
/*!40000 ALTER TABLE `chat_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `class_journals`
--

DROP TABLE IF EXISTS `class_journals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_journals` (
  `journal_id` int NOT NULL AUTO_INCREMENT,
  `class_id` int NOT NULL COMMENT 'FK -> classes.class_id',
  `teacher_id` int NOT NULL COMMENT 'FK -> users.user_id (giảng viên viết nhật ký)',
  `title` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Tiêu đề nhật ký',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `journal_date` date NOT NULL COMMENT 'Ngày ghi nhật ký (YYYY-MM-DD)',
  `journal_type` enum('ANNOUNCEMENT','ISSUE','NOTE','OTHER','PROGRESS') COLLATE utf8mb4_unicode_ci NOT NULL,
  `deleted_at` datetime DEFAULT NULL COMMENT 'Thời điểm soft delete',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` int DEFAULT NULL COMMENT 'ID user tạo nhật ký',
  `updated_by` int DEFAULT NULL COMMENT 'ID user cập nhật cuối',
  `journal_time` time DEFAULT NULL COMMENT 'Giờ:phút ghi nhật ký (HH:mm)',
  PRIMARY KEY (`journal_id`),
  KEY `fk_class_journals_created_by` (`created_by`),
  KEY `fk_class_journals_updated_by` (`updated_by`),
  KEY `idx_journals_class_date_id` (`class_id`,`journal_date` DESC,`journal_id` DESC),
  KEY `idx_journals_teacher_date_id` (`teacher_id`,`journal_date` DESC,`journal_id` DESC),
  KEY `idx_journals_class_type_date` (`class_id`,`journal_type`,`journal_date` DESC),
  KEY `idx_journals_deleted_at` (`deleted_at`),
  KEY `idx_journals_class_notdeleted_date` (`class_id`,`deleted_at`,`journal_date` DESC),
  CONSTRAINT `fk_class_journals_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_class_journals_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_class_journals_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_class_journals_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `chk_journal_content_length` CHECK ((char_length(`content`) >= 10)),
  CONSTRAINT `chk_journal_title_length` CHECK ((char_length(`title`) >= 3))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Nhật ký lớp học - SUPER_ADMIN có thể tạo cho bất kỳ lớp, TEACHER phải được phân công';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_journals`
--

LOCK TABLES `class_journals` WRITE;
/*!40000 ALTER TABLE `class_journals` DISABLE KEYS */;
/*!40000 ALTER TABLE `class_journals` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `trg_validate_journal_teacher_before_insert` BEFORE INSERT ON `class_journals` FOR EACH ROW BEGIN
    DECLARE teacher_assigned INT DEFAULT 0;
    DECLARE is_super_admin INT DEFAULT 0;
    
    -- Bước 1: Kiểm tra user có phải SUPER_ADMIN không
    -- Không check deleted_at vì user_roles và roles không có cột này
    SELECT COUNT(*) INTO is_super_admin
    FROM user_roles ur
    JOIN roles r ON ur.role_id = r.role_id
    WHERE ur.user_id = NEW.teacher_id
      AND r.code = 'SUPER_ADMIN';
    
    -- Bước 2: Nếu là SUPER_ADMIN → BỎ QUA validation
    IF is_super_admin > 0 THEN
        -- SUPER_ADMIN có thể tạo journal cho bất kỳ lớp nào
        SET teacher_assigned = 1;
    ELSE
        -- User thường (TEACHER) → phải kiểm tra phân công
        -- Lưu ý: class_teachers KHÔNG có deleted_at, dùng end_date để check timeline
        SELECT COUNT(*) INTO teacher_assigned
        FROM class_teachers
        WHERE class_id = NEW.class_id
          AND teacher_id = NEW.teacher_id
          AND start_date <= NEW.journal_date
          AND (end_date IS NULL OR end_date >= NEW.journal_date);
    END IF;
    
    -- Bước 3: Nếu không hợp lệ → REJECT
    IF teacher_assigned = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Giảng viên không được phân công vào lớp này hoặc không còn hiệu lực tại ngày nhật ký';
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `trg_validate_journal_teacher_before_update` BEFORE UPDATE ON `class_journals` FOR EACH ROW BEGIN
    DECLARE teacher_assigned INT DEFAULT 0;
    DECLARE is_super_admin INT DEFAULT 0;
    
    -- Chỉ validate khi thay đổi class_id, teacher_id hoặc journal_date
    IF NEW.class_id <> OLD.class_id 
       OR NEW.teacher_id <> OLD.teacher_id 
       OR NEW.journal_date <> OLD.journal_date THEN
        
        -- Bước 1: Kiểm tra user có phải SUPER_ADMIN không
        -- Không check deleted_at vì user_roles và roles không có cột này
        SELECT COUNT(*) INTO is_super_admin
        FROM user_roles ur
        JOIN roles r ON ur.role_id = r.role_id
        WHERE ur.user_id = NEW.teacher_id
          AND r.code = 'SUPER_ADMIN';
        
        -- Bước 2: Nếu là SUPER_ADMIN → BỎ QUA validation
        IF is_super_admin > 0 THEN
            SET teacher_assigned = 1;
        ELSE
            -- User thường → phải kiểm tra phân công
            -- Lưu ý: class_teachers KHÔNG có deleted_at, dùng end_date để check timeline
            SELECT COUNT(*) INTO teacher_assigned
            FROM class_teachers
            WHERE class_id = NEW.class_id
              AND teacher_id = NEW.teacher_id
              AND start_date <= NEW.journal_date
              AND (end_date IS NULL OR end_date >= NEW.journal_date);
        END IF;
        
        -- Bước 3: Nếu không hợp lệ → REJECT
        IF teacher_assigned = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Giảng viên không được phân công vào lớp này hoặc không còn hiệu lực tại ngày nhật ký';
        END IF;
    END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `class_teachers`
--

DROP TABLE IF EXISTS `class_teachers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `class_teachers` (
  `class_teacher_id` int NOT NULL AUTO_INCREMENT,
  `class_id` int NOT NULL COMMENT 'FK -> classes.class_id',
  `teacher_id` int NOT NULL COMMENT 'FK -> users.user_id',
  `start_date` date NOT NULL COMMENT 'Ngày bắt đầu phụ trách',
  `end_date` date DEFAULT NULL COMMENT 'Ngày kết thúc (NULL = còn hiệu lực)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `assigned_by` int DEFAULT NULL COMMENT 'User thực hiện gán giảng viên',
  `revoked_by` int DEFAULT NULL COMMENT 'User kết thúc phân công',
  `note` text COLLATE utf8mb4_unicode_ci,
  `eff_end_date` date GENERATED ALWAYS AS (coalesce(`end_date`,_utf8mb4'9999-12-31')) VIRTUAL COMMENT 'Dùng để filter “đang hiệu lực” nhanh (index-friendly)',
  PRIMARY KEY (`class_teacher_id`),
  UNIQUE KEY `uk_ct_class_teacher_start` (`class_id`,`teacher_id`,`start_date`),
  KEY `fk_ct_assigned_by` (`assigned_by`),
  KEY `fk_ct_revoked_by` (`revoked_by`),
  KEY `idx_ct_class_start_id` (`class_id`,`start_date`,`class_teacher_id`),
  KEY `idx_ct_teacher_start_id` (`teacher_id`,`start_date`,`class_teacher_id`),
  KEY `idx_ct_class_teacher_range` (`class_id`,`teacher_id`,`start_date`,`end_date`),
  KEY `idx_ct_class_effend_start_id` (`class_id`,`eff_end_date`,`start_date`,`class_teacher_id`),
  CONSTRAINT `fk_class_teachers_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_class_teachers_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ct_assigned_by` FOREIGN KEY (`assigned_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_ct_revoked_by` FOREIGN KEY (`revoked_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `chk_ct_dates_order` CHECK (((`end_date` is null) or (`start_date` <= `end_date`)))
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `class_teachers`
--

LOCK TABLES `class_teachers` WRITE;
/*!40000 ALTER TABLE `class_teachers` DISABLE KEYS */;
INSERT INTO `class_teachers` (`class_teacher_id`, `class_id`, `teacher_id`, `start_date`, `end_date`, `created_at`, `updated_at`, `assigned_by`, `revoked_by`, `note`) VALUES (3,4,4,'2026-03-04',NULL,'2026-03-04 09:48:04','2026-03-04 09:48:04',7,NULL,NULL),(4,5,5,'2026-04-01',NULL,'2026-04-01 12:31:22','2026-04-01 12:31:22',7,NULL,NULL),(6,10,26,'2026-07-13','2026-07-13','2026-07-13 05:55:38','2026-07-13 05:56:04',7,NULL,NULL),(7,10,5,'2026-07-13',NULL,'2026-07-13 05:56:01','2026-07-13 05:56:01',7,NULL,NULL);
/*!40000 ALTER TABLE `class_teachers` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `trg_ct_no_overlap_ins` BEFORE INSERT ON `class_teachers` FOR EACH ROW BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM class_teachers t
    WHERE t.class_id = NEW.class_id
      AND t.teacher_id = NEW.teacher_id
      AND NEW.start_date <= COALESCE(t.end_date, '9999-12-31')
      AND t.start_date <= COALESCE(NEW.end_date, '9999-12-31');
    IF cnt > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Overlapping assignment for (class_id, teacher_id)';
END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `trg_ct_no_overlap_upd` BEFORE UPDATE ON `class_teachers` FOR EACH ROW BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM class_teachers t
    WHERE t.class_id = NEW.class_id
      AND t.teacher_id = NEW.teacher_id
      AND t.class_teacher_id <> OLD.class_teacher_id
      AND NEW.start_date <= COALESCE(t.end_date, '9999-12-31')
      AND t.start_date <= COALESCE(NEW.end_date, '9999-12-31');
    IF cnt > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Overlapping assignment for (class_id, teacher_id)';
END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `classes`
--

DROP TABLE IF EXISTS `classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `classes` (
  `class_id` int NOT NULL AUTO_INCREMENT,
  `center_id` int NOT NULL COMMENT 'Trung tâm tổ chức lớp học',
  `program_id` int NOT NULL COMMENT 'Chương trình học',
  `name` varchar(255) NOT NULL COMMENT 'Tên lớp học',
  `description` text,
  `start_date` date DEFAULT NULL COMMENT 'Ngày khai giảng',
  `end_date` date DEFAULT NULL COMMENT 'Ngày kết thúc',
  `status` enum('CANCELLED','FINISHED','ONGOING','PLANNED') NOT NULL,
  `room` varchar(100) DEFAULT NULL COMMENT 'Phòng học',
  `capacity` int unsigned DEFAULT NULL COMMENT 'Sĩ số tối đa',
  `deleted_at` datetime DEFAULT NULL COMMENT 'Thời điểm soft delete',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` int DEFAULT NULL COMMENT 'ID user tạo lớp học',
  `updated_by` int DEFAULT NULL COMMENT 'ID user cập nhật cuối',
  `study_days` varchar(255) DEFAULT NULL,
  `study_time` enum('AFTERNOON','EVENING','MORNING') DEFAULT NULL,
  PRIMARY KEY (`class_id`),
  UNIQUE KEY `uk_classes_center_name` (`center_id`,`name`),
  KEY `fk_classes_created_by` (`created_by`),
  KEY `fk_classes_updated_by` (`updated_by`),
  KEY `idx_classes_center_status_start_id` (`center_id`,`status`,`start_date`,`class_id`),
  KEY `idx_classes_program` (`program_id`),
  KEY `idx_classes_study_time` (`study_time`),
  CONSTRAINT `fk_classes_center` FOREIGN KEY (`center_id`) REFERENCES `centers` (`center_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_classes_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_classes_program` FOREIGN KEY (`program_id`) REFERENCES `programs` (`program_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_classes_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `chk_class_capacity_positive` CHECK (((`capacity` is null) or (`capacity` > 0))),
  CONSTRAINT `chk_class_dates_order` CHECK (((`start_date` is null) or (`end_date` is null) or (`start_date` <= `end_date`)))
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `classes`
--

LOCK TABLES `classes` WRITE;
/*!40000 ALTER TABLE `classes` DISABLE KEYS */;
INSERT INTO `classes` VALUES (2,1,2,'Lớp Python Advanced 2025','Lớp học Python nâng cao cho developer','2025-02-01','2025-05-01','PLANNED','Phòng A102',25,'2026-03-04 08:54:46','2025-10-13 02:20:00','2026-03-04 08:54:46',3,7,NULL,NULL),(4,1,3,'Lớp Ielts 5.0',NULL,'2026-03-04','2026-03-29','ONGOING','Phòng A102',30,NULL,'2026-03-04 09:47:36','2026-03-04 09:47:36',7,7,'[\"TUESDAY\", \"THURSDAY\"]','AFTERNOON'),(5,1,4,'lớp Ielts 5.5',NULL,'2026-04-01','2026-07-20','ONGOING','Phòng A102',30,NULL,'2026-04-01 11:29:51','2026-04-01 11:29:51',7,7,'[\"MONDAY\",\"TUESDAY\"]','AFTERNOON'),(6,1,4,'lớp ielts 6.0',NULL,'2026-04-11','2026-04-13','PLANNED','Phòng A102p3',30,NULL,'2026-04-10 08:12:58','2026-04-10 08:12:58',7,7,'[\"MONDAY\"]','AFTERNOON'),(7,1,5,'công nghệ thông tin',NULL,'2026-06-30','2026-09-30','PLANNED','Phòng A102',30,'2026-07-28 17:07:36','2026-06-17 09:27:16','2026-07-28 17:07:36',7,7,'[\"MONDAY\",\"WEDNESDAY\"]','AFTERNOON'),(8,2,6,'lớp học Ielts 7.0',NULL,'2026-09-20','2026-12-28','PLANNED','Phòng A102p3',30,NULL,'2026-07-02 09:21:18','2026-07-02 09:21:18',7,7,'[\"MONDAY\",\"WEDNESDAY\"]','AFTERNOON'),(10,1,7,'Lớp toeic 700',NULL,'2026-07-20','2026-09-30','PLANNED','phòng B102-2',30,NULL,'2026-07-13 04:19:25','2026-07-13 04:19:25',7,7,'[\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\"]','EVENING');
/*!40000 ALTER TABLE `classes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `enrollments`
--

DROP TABLE IF EXISTS `enrollments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enrollments` (
  `enrollment_id` int NOT NULL AUTO_INCREMENT,
  `class_id` int NOT NULL COMMENT 'FK -> classes.class_id, mã lớp học',
  `student_id` int NOT NULL COMMENT 'FK -> students.student_id, mã học viên',
  `status` enum('ACTIVE','DROPPED','GRADUATED','SUSPENDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `enrolled_at` date NOT NULL COMMENT 'Ngày bắt đầu ghi danh',
  `left_at` date DEFAULT NULL COMMENT 'Ngày kết thúc ghi danh (NULL = còn hiệu lực)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm tạo',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời điểm cập nhật',
  `assigned_by` int DEFAULT NULL COMMENT 'Người gán học viên vào lớp',
  `revoked_by` int DEFAULT NULL COMMENT 'Người hủy ghi danh',
  `revoked_at` datetime DEFAULT NULL COMMENT 'Thời điểm hủy ghi danh (xóa mềm)',
  `note` text COLLATE utf8mb4_unicode_ci,
  `eff_end_date` date GENERATED ALWAYS AS (coalesce(`left_at`,DATE'9999-12-31')) VIRTUAL COMMENT 'Cột sinh: phục vụ lọc nhanh bản ghi còn hiệu lực',
  PRIMARY KEY (`enrollment_id`),
  UNIQUE KEY `uk_enrollments_class_student_enrolled` (`class_id`,`student_id`,`enrolled_at`),
  KEY `fk_enrollments_assigned_by` (`assigned_by`),
  KEY `fk_enrollments_revoked_by` (`revoked_by`),
  KEY `idx_enrollments_class_status` (`class_id`,`status`) COMMENT 'Lọc trạng thái theo lớp',
  KEY `idx_enrollments_student_start_id` (`student_id`,`enrolled_at`,`enrollment_id`) COMMENT 'Tra cứu theo học viên',
  KEY `idx_enrollments_class_effend_start_id` (`class_id`,`eff_end_date`,`enrolled_at`,`enrollment_id`) COMMENT 'Lọc danh sách hiệu lực theo lớp',
  CONSTRAINT `fk_enrollments_assigned_by` FOREIGN KEY (`assigned_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_enrollments_class` FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_enrollments_revoked_by` FOREIGN KEY (`revoked_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_enrollments_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_enroll_dates_order` CHECK (((`left_at` is null) or (`enrolled_at` <= `left_at`)))
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `enrollments`
--

LOCK TABLES `enrollments` WRITE;
/*!40000 ALTER TABLE `enrollments` DISABLE KEYS */;
INSERT INTO `enrollments` (`enrollment_id`, `class_id`, `student_id`, `status`, `enrolled_at`, `left_at`, `created_at`, `updated_at`, `assigned_by`, `revoked_by`, `revoked_at`, `note`) VALUES (1,4,1,'DROPPED','2026-03-04','2026-03-04','2026-03-04 09:51:00','2026-03-05 08:26:21',7,7,'2026-03-05 08:26:21','Note: [2026-03-04]\n[REVOKED 2026-03-05] Xóa bởi giáo viên/quản trị'),(2,4,2,'GRADUATED','2026-03-04','2026-04-01','2026-03-04 10:22:52','2026-04-01 07:55:53',7,NULL,NULL,'Tự động tốt nghiệp khi lớp hoàn thành'),(3,5,2,'GRADUATED','2026-04-01','2026-07-23','2026-04-01 11:30:00','2026-07-23 06:28:55',7,NULL,NULL,'Tự động tốt nghiệp khi lớp hoàn thành'),(4,5,3,'GRADUATED','2026-04-01','2026-07-23','2026-04-01 12:43:09','2026-07-23 06:28:55',7,NULL,NULL,'Tự động tốt nghiệp khi lớp hoàn thành'),(5,5,4,'GRADUATED','2026-04-02','2026-07-23','2026-04-02 02:39:41','2026-07-23 06:28:55',7,NULL,NULL,'Tự động tốt nghiệp khi lớp hoàn thành'),(6,6,5,'GRADUATED','2026-04-10','2026-06-17','2026-04-10 08:14:48','2026-06-17 09:12:26',7,NULL,NULL,'Tự động tốt nghiệp khi lớp hoàn thành'),(7,7,5,'ACTIVE','2026-06-17',NULL,'2026-06-17 09:27:33','2026-06-17 09:27:33',7,NULL,NULL,''),(11,10,2,'ACTIVE','2026-07-13',NULL,'2026-07-13 04:20:13','2026-07-13 04:20:13',7,NULL,NULL,''),(12,10,5,'ACTIVE','2026-07-13',NULL,'2026-07-13 05:32:56','2026-07-13 05:32:56',7,NULL,NULL,''),(13,10,3,'ACTIVE','2026-07-13',NULL,'2026-07-13 05:32:56','2026-07-13 05:32:56',7,NULL,NULL,''),(14,10,4,'ACTIVE','2026-07-13',NULL,'2026-07-13 05:32:56','2026-07-13 05:32:56',7,NULL,NULL,'');
/*!40000 ALTER TABLE `enrollments` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `trg_enroll_no_overlap_ins` BEFORE INSERT ON `enrollments` FOR EACH ROW BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM enrollments e
    WHERE e.class_id   = NEW.class_id
      AND e.student_id = NEW.student_id
      AND NEW.enrolled_at <= COALESCE(e.left_at, DATE '9999-12-31')
      AND e.enrolled_at   <= COALESCE(NEW.left_at, DATE '9999-12-31');
    IF cnt > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Ghi danh trùng lấn thời gian cho (class_id, student_id)';
END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `tr_enrollment_sync_student_status` AFTER INSERT ON `enrollments` FOR EACH ROW BEGIN
    UPDATE students 
    SET overall_status = sync_student_status_from_enrollment(NEW.student_id),
        updated_at = CURRENT_TIMESTAMP
    WHERE student_id = NEW.student_id;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `after_enrollments_insert` AFTER INSERT ON `enrollments` FOR EACH ROW BEGIN
    UPDATE students
    SET overall_status = sync_student_status_from_enrollment(NEW.student_id)
    WHERE student_id = NEW.student_id;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`%`*/ /*!50003 TRIGGER `trg_enroll_no_overlap_upd` BEFORE UPDATE ON `enrollments` FOR EACH ROW BEGIN
    DECLARE cnt INT;
    SELECT COUNT(*) INTO cnt
    FROM enrollments e
    WHERE e.class_id   = NEW.class_id
      AND e.student_id = NEW.student_id
      AND e.enrollment_id <> OLD.enrollment_id
      AND NEW.enrolled_at <= COALESCE(e.left_at, DATE '9999-12-31')
      AND e.enrolled_at   <= COALESCE(NEW.left_at, DATE '9999-12-31');
    IF cnt > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Ghi danh trùng lấn thời gian cho (class_id, student_id)';
END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode    
<truncated 100276 bytes>

NOTE: The output was truncated because it was too long. Use a more targeted query or a smaller range to get the information you need.