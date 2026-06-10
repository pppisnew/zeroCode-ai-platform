# 数据库设计

---

## user

```sql id="pv7vqa"
CREATE TABLE user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## app

```sql id="k3vjlwm"
CREATE TABLE app (
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
```

---

## app_version

```sql id="a9bk7x"
CREATE TABLE app_version (
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
```

---

## chat_message

```sql
CREATE TABLE chat_message (
  id BIGINT PRIMARY KEY,
  app_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(32) NOT NULL,
  content TEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_chat_message_app_id (app_id)
);
```

---

## ai_task

```sql
CREATE TABLE ai_task (
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
```

---

当前初始化脚本位置：

```text
infra/mysql/init.sql
```
