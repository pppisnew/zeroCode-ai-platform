CREATE TABLE IF NOT EXISTS user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS app (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  app_name VARCHAR(128) NOT NULL,
  description TEXT,
  type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  deploy_url VARCHAR(255),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_app_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS app_version (
  id BIGINT PRIMARY KEY,
  app_id BIGINT NOT NULL,
  version_no INT NOT NULL,
  prompt TEXT,
  ai_response LONGTEXT,
  snapshot_url VARCHAR(255),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_app_version (app_id, version_no),
  INDEX idx_app_version_app_id (app_id)
);

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY,
  app_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_chat_message_app_id (app_id)
);

CREATE TABLE IF NOT EXISTS ai_task (
  id BIGINT PRIMARY KEY,
  app_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  request_payload LONGTEXT,
  response_payload LONGTEXT,
  error_message TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_ai_task_app_id (app_id)
);
