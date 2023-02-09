CREATE DATABASE datamessie /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;
USE datamessie;

CREATE TABLE user (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  username varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  passwordSalt tinyblob NOT NULL,
  passwordHash tinyblob NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY user_id_version (id,version),
  UNIQUE KEY user_username (username)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE project (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  name varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  crawlingEnabled tinyint(1) NOT NULL,
  crawlingInterval int DEFAULT NULL,
  preprocessingEnabled tinyint(1) NOT NULL,
  cleaningMethod int DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY project_id_version (id,version),
  UNIQUE KEY project_name (name)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE project_user (
  project_id bigint(20),
  user_id bigint(20) NOT NULL,
  PRIMARY KEY (project_id,user_id),
  KEY FK_project_user_project_id (project_id),
  KEY FK_project_user_user_id (user_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE source (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  name varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  language varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  url varchar(1023) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  cookie varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  crawlingEnabled tinyint(1) NOT NULL,
  visible tinyint(1) NOT NULL,
  statisticsChecking tinyint(1) NOT NULL,
  notes varchar(4096) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY source_id_version (id,version),
  UNIQUE KEY source_name (name)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE project_source (
  project_id bigint(20),
  source_id bigint(20) NOT NULL,
  PRIMARY KEY (project_id,source_id),
  KEY FK_project_source_project_id (project_id),
  KEY FK_project_source_source_id (source_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sourceType (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  name varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY sourceType_id_version (id,version),
  UNIQUE KEY sourceType_name (name)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE source_sourceType (
  source_id bigint(20) NOT NULL,
  sourceType_id bigint(20) NOT NULL,
  PRIMARY KEY (source_id,sourceType_id),
  KEY FK_source_sourceType_source_id (source_id),
  KEY FK_source_sourceType_sourceType_id (sourceType_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE redirectingRule (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  regex varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  regexGroup int DEFAULT NULL,
  activeFrom date DEFAULT NULL,
  activeTo date DEFAULT NULL,
  mode int DEFAULT NULL,
  position int DEFAULT NULL,
  source_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY redirectingRule_id_version (id,version),
  KEY FK_redirectingRule_source_id (source_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE deletingRule (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  selector varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  activeFrom date DEFAULT NULL,
  activeTo date DEFAULT NULL,
  mode int DEFAULT NULL,
  position int DEFAULT NULL,
  source_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY deletingRule_id_version (id,version),
  KEY FK_deletingRule_source_id (source_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tagSelectingRule (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  selector varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  activeFrom date DEFAULT NULL,
  activeTo date DEFAULT NULL,
  mode int DEFAULT NULL,
  position int DEFAULT NULL,
  source_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY tagSelectingRule_id_version (id,version),
  KEY FK_tagSelectingRule_source_id (source_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE crawling (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  started datetime DEFAULT NULL,
  completed datetime DEFAULT NULL,
  project_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY crawling_id_version (id,version),
  KEY FK_crawling_project_id (project_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE document (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  title varchar(511) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  stemmedTitle varchar(511) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  url varchar(511) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  description longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  stemmedDescription longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  published datetime DEFAULT NULL,
  downloaded datetime DEFAULT NULL,
  state int DEFAULT NULL,
  statusCode int DEFAULT NULL,
  crawling_id bigint(20) NOT NULL,
  source_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY document_id_version (id,version),
  KEY document_downloaded_state_source_id (downloaded,state,source_id),
  KEY document_published_source_id_state (published,source_id,state),
  KEY FK_document_crawling_id (crawling_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content (
  document_id bigint(20) NOT NULL,
  version bigint(20) DEFAULT NULL,
  rawContent longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (document_id),
  UNIQUE KEY rawContent_document_id_version (document_id,version)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cleanedContent (
  document_id bigint(20) NOT NULL,
  version bigint(20) DEFAULT NULL,
  content longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (document_id),
  UNIQUE KEY cleanedContent_document_id_version (document_id,version)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stemmedContent (
  document_id bigint(20) NOT NULL,
  version bigint(20) DEFAULT NULL,
  content longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (document_id),
  UNIQUE KEY stemmedContent_document_id_version (document_id,version)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE download (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  url varchar(511) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  source_id bigint(20) NOT NULL,
  document_id bigint(20) NOT NULL,
  success tinyint(1) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY download_id_version (id,version),
  UNIQUE KEY download_url_source_id (url,source_id),
  KEY FK_download_document_id (document_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE namedEntity (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  name varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY namedEntity_id_version (id,version),
  UNIQUE KEY namedEntity_name (name)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE namedEntityCategory (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  namedEntity_id bigint(20) NOT NULL,
  categoryNamedEntity_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY namedEntityCategory_id_version (id,version),
  UNIQUE KEY namedEntityCategory_namedEntity_id_categoryNamedEntity_id (namedEntity_id,categoryNamedEntity_id),
  KEY FK_namedEntityCategory_namedEntity_id (namedEntity_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE namedEntityOccurrence (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  namedEntity_id bigint(20) NOT NULL,
  parentNamedEntity_id bigint(20) NOT NULL,
  type int NOT NULL,
  quantity int NOT NULL,
  document_id bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY namedEntityOccurrence_id_version (id,version),
  UNIQUE KEY namedEntityOccurrence_namedEntity_id_type_document_id (namedEntity_id,type,document_id),
  KEY namedEntityOccurrence_type_document_id (type,document_id),
  KEY FK_namedEntityOccurrence_namedEntity_id (namedEntity_id),
  KEY FK_namedEntityOccurrence_document_id (document_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE statistics (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  source_id bigint(20) NOT NULL,
  published date NOT NULL,
  state int NOT NULL,
  documents bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY statistics_id_version (id,version),
  UNIQUE KEY statistics_source_id_published_state (source_id,published,state),
  UNIQUE KEY statistics_published_source_id_state (published,source_id,state)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE twitterUser (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  twitterId bigint(20) NOT NULL,
  screenName varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  name varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  createdAt datetime DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY twitterUser_id_version (id,version),
  UNIQUE KEY twitterUser_twitterId (twitterId)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE twitterUserStatistics (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  twitterUser_id bigint(20) NOT NULL,
  timestamp datetime DEFAULT NULL,
  numberOfTweets int DEFAULT NULL,
  numberOfFollowing int DEFAULT NULL,
  numberOfFollowers int DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY twitterUserStatistics_id_version (id,version),
  KEY FK_twitterUserStatistics_twitterUser_id (twitterUser_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE twitterUserList (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  version bigint(20) DEFAULT NULL,
  ownerScreenName varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  slug varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY twitterUserList_id_version (id,version),
  UNIQUE KEY twitterUserList_ownerScreenName_slug (ownerScreenName, slug)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE twitterUserList_twitterUser (
  twitterUserList_id bigint(20) NOT NULL,
  twitterUser_id bigint(20) NOT NULL,
  PRIMARY KEY (twitterUserList_id,twitterUser_id),
  KEY FK_twitterUserList_twitterUser_twitterUserList_id (twitterUserList_id),
  KEY FK_twitterUserList_twitterUser_twitterUser_id (twitterUser_id)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
