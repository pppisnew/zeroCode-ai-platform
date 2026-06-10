# 数据库设计

---

## user

```sql id="pv7vqa"
CREATE TABLE user (
  id BIGINT PRIMARY KEY,
  username VARCHAR(64),
  password VARCHAR(255),
  role VARCHAR(32),
  create_time DATETIME
);
```

---

## app

```sql id="k3vjlwm"
CREATE TABLE app (
  id BIGINT PRIMARY KEY,
  user_id BIGINT,
  app_name VARCHAR(128),
  description TEXT,
  type VARCHAR(32),
  status VARCHAR(32),
  deploy_url VARCHAR(255),
  create_time DATETIME
);
```

---

## app_version

```sql id="a9bk7x"
CREATE TABLE app_version (
  id BIGINT PRIMARY KEY,
  app_id BIGINT,
  version_no INT,
  prompt TEXT,
  ai_response LONGTEXT,
  snapshot_url VARCHAR(255),
  create_time DATETIME
);
```

---