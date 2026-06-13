# Phase 1: 用户系统 + 认证

**日期**: 2026-06-13
**状态**: 已完成
**对应文档**: AGENTS.md §2.2（Java 职责：用户系统、认证）, §15（JWT 认证、RBAC）, other.md（user-service）

## 目标

实现多用户注册/登录/登出，API 受保护，数据按用户隔离。满足 AGENTS.md §15："Must support JWT authentication"（使用 Sa-Token 等效实现）。

## 实施内容

### Java 后端

| 动作 | 文件 | 说明 |
|------|------|------|
| NEW | `model/UserEntity.java` | 映射 `user` 表 |
| NEW | `mapper/UserMapper.java` | MyBatis-Plus BaseMapper |
| NEW | `service/UserService.java` | 接口：register, login, getCurrentUser, logout |
| NEW | `service/impl/UserServiceImpl.java` | BCrypt 密码加密，Sa-Token 会话管理 |
| NEW | `controller/AuthController.java` | POST /auth/register, POST /auth/login, POST /auth/logout, GET /auth/me |
| NEW | `dto/LoginRequest.java` | username (3-64) + password (6-255) |
| NEW | `dto/RegisterRequest.java` | 同上 |
| NEW | `vo/UserVO.java` | id, username, role, createTime |
| NEW | `vo/LoginVO.java` | token + UserVO |
| NEW | `config/SaTokenConfig.java` | SaInterceptor 拦截 /** 除 /auth/**, /health |
| MODIFY | `pom.xml` | 添加 `spring-security-crypto`（BCrypt） |
| MODIFY | `application.yml` | `token-name: ZeroCode-Auth`，`is-read-cookie: false` |
| MODIFY | `GlobalExceptionHandler.java` | `NotLoginException` → 401 响应 |
| MODIFY | `AppServiceImpl.java` | `listApps()` 按 userId 过滤，`getApp()`/`deleteApp()` 校验所有权，`resolveUserId()` 从 Sa-Token 获取或 fallback 到 defaultUserId |

### 前端

| 动作 | 文件 | 说明 |
|------|------|------|
| NEW | `router/index.ts` | /login → LoginPage, / → WorkspacePage (requiresAuth) |
| NEW | `pages/LoginPage.vue` | 登录/注册双模式表单，星穹铁道风格 |
| NEW | `stores/auth.ts` | token/user 状态管理，localStorage 持久化，login/register/logout/fetchMe |
| MODIFY | `main.ts` | 添加 router + 路由守卫（requiresAuth → 重定向 /login） |
| MODIFY | `App.vue` | `<router-view />` + onMounted 检查 token 有效性 |
| MODIFY | `api/client.ts` | 请求自动附加 `ZeroCode-Auth` header，`getAuthHeaders()` 从 localStorage 读取 |
| MODIFY | `package.json` | 添加 `vue-router@^4.6.4` |

## 认证流程

```
用户访问 / → router guard 检查 token
  ├─ 无 token → 重定向 /login
  └─ 有 token → GET /auth/me 验证
       ├─ 有效 → 进入 WorkspacePage
       └─ 无效 → 清除 token，重定向 /login

登录：POST /auth/login → 返回 token + user
注册：POST /auth/register → 创建用户 → 自动登录 → 返回 token + user

后续 API 请求：client.ts 自动附加 ZeroCode-Auth header
  ├─ 无 token → SaInterceptor → 401
  └─ 有效 token → StpUtil.getLoginIdAsLong() → userId 隔离数据
```

## 用户隔离

- `listApps()`: 只返回当前用户创建的 app（`eq(AppEntity::getUserId, userId)`）
- `getApp()` / `deleteApp()`: 校验 app 归属当前用户
- 测试/开发环境：`defaultUserId=1` 作为 fallback（当 Sa-Token 未登录时）
- `resolveUserId()`: 先尝试 `StpUtil.getLoginIdAsLong()`，捕获 `NotLoginException` 后返回 `defaultUserId`

## 验证结果

```bash
# 认证流程测试
curl -X POST :8080/api/auth/register -d '{"username":"test","password":"123456"}'
# → 200, token + user

curl :8080/api/apps
# → 401 (无 token)

curl :8080/api/apps -H "ZeroCode-Auth: $TOKEN"
# → 200, 当前用户的 app 列表

# 全量测试
mvn test  # platform-service: 42 passed
npm test  # frontend: 13 passed
uv run python -m pytest  # ai-orchestrator: 47 passed
```

## 已知限制

- `SaTokenConfig` 排除 `/auth/**` 和 `/health`，但未排除 `/error`（Spring 错误页面）
- 用户 ID 使用 `System.currentTimeMillis()` 生成，非自增（避免分布式冲突，后续可改进）
- RBAC 权限控制未实现（留到 Phase 4）
- 前端未处理 401 响应自动跳转登录页（仅路由守卫处理）
