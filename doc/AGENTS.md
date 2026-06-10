# AGENTS.md

# ZeroCode AI Platform - AI Engineering Specification

---

# 1. Project Overview

ZeroCode AI Platform is an enterprise-grade AI-powered low-code/no-code Web application generation platform.

The platform allows users to:

* Generate applications using natural language
* Modify applications through conversation
* Preview applications in real time
* Visually edit components
* Export deployable projects

The architecture follows:

* Java Platform Services
* Python AI Services
* Vue Frontend
* Microservice Architecture
* AI Agent Workflow

---

# 2. Core Architecture Rules

---

## 2.1 Mandatory Architecture

The project MUST follow:

```text
Frontend (Vue3)
    ↓
Java Platform Layer
    ↓
Python AI Layer
    ↓
Sandbox Runtime
```

---

## 2.2 Responsibilities

### Java Services

Responsible for:

* User system
* Authentication
* Project management
* File management
* Persistence
* Task scheduling
* Message queue
* API gateway

Java MUST NOT contain:

* Prompt orchestration
* AI workflow logic
* Multi-agent reasoning

---

### Python AI Services

Responsible for:

* Agent workflow
* Prompt orchestration
* Code generation
* Code fixing
* AI planning
* Testing automation

Python MUST NOT contain:

* User system
* Business persistence
* Permission system

---

# 3. Mandatory Tech Stack

---

# Frontend

Must use:

```text
Vue 3
TypeScript
Vite
Pinia
TailwindCSS
Monaco Editor
GrapesJS
```

Forbidden:

```text
jQuery
Vue2
Webpack
```

---

# Java Backend

Must use:

```text
Java 21
Spring Boot 3
MyBatis Plus
MySQL 8
Redis
RabbitMQ
MinIO
Sa-Token
```

Forbidden:

```text
Spring Boot 2
JDK 8
Session-based authentication
```

---

# Python AI Services

Must use:

```text
Python 3.12+
FastAPI
LangGraph
PydanticAI
Playwright
```

Preferred:

```text
CrewAI
Redis
PostgreSQL
```

Forbidden:

```text
Flask
Django
```

---

# 4. Engineering Standards

---

# 4.1 Code Quality

All generated code MUST:

* Be production-grade
* Be modular
* Be maintainable
* Follow SOLID principles
* Avoid duplicated logic
* Avoid giant classes/functions

---

# 4.2 Naming Rules

Must use:

```text
camelCase     -> variables/functions
PascalCase    -> classes
UPPER_SNAKE   -> constants
```

Forbidden:

```text
tmp
test1
aaa
bbb
foo
bar
```

---

# 4.3 File Structure

Frontend MUST follow:

```text
src/
  api/
  components/
  pages/
  stores/
  hooks/
  utils/
  types/
```

Java MUST follow:

```text
controller/
service/
service/impl/
mapper/
model/
dto/
vo/
config/
```

Python MUST follow:

```text
agents/
workflows/
prompts/
models/
tools/
services/
routers/
```

---

# 5. AI Generation Rules

---

# 5.1 Structured Output Only

AI MUST output:

```json
{
  "projectName": "",
  "files": []
}
```

Forbidden:

````markdown
```html
````

````markdown
```java
````

````markdown
```python
````

````

Markdown code block extraction is STRICTLY FORBIDDEN.

---

# 5.2 JSON Schema

All AI outputs MUST support:

```text
response_format=json_schema
strict=true
````

---

# 5.3 Multi-file Generation

AI MUST generate:

```text
Complete project structure
```

Instead of:

```text
Single giant file
```

---

# 5.4 File Constraints

Each generated file MUST:

* Have explicit path
* Have file type
* Have complete content

Example:

```json
{
  "filePath": "src/App.vue",
  "fileType": "vue",
  "content": ""
}
```

---

# 6. Frontend Rules

---

# 6.1 UI Standards

UI MUST:

* Be modern
* Be responsive
* Support dark mode
* Have good spacing
* Use modern typography
* Avoid outdated design

Preferred style:

* Minimal
* AI-native
* Glassmorphism
* Modern SaaS

Forbidden:

* Bootstrap 3 style
* Old admin templates
* Table-heavy layouts

---

# 6.2 CSS Rules

Must use:

```text
TailwindCSS
Flexbox
CSS Grid
```

Forbidden:

```text
inline styles
float layout
!important abuse
```

---

# 6.3 JavaScript Rules

Must use:

```text
ES2022+
async/await
```

Forbidden:

```text
jQuery
var
callback hell
```

---

# 7. Backend Rules

---

# 7.1 API Standards

All APIs MUST follow:

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

---

# 7.2 RESTful Design

Must follow:

```text
GET    /apps
POST   /apps
PUT    /apps/{id}
DELETE /apps/{id}
```

Forbidden:

```text
/getAppList
/updateApp
/deleteById
```

---

# 7.3 DTO/VO Separation

MUST separate:

* DTO
* Entity
* VO

Forbidden:

```text
Direct Entity exposure
```

---

# 8. AI Agent Workflow Rules

---

# 8.1 Mandatory Workflow

AI workflow MUST follow:

```text
Planner Agent
    ↓
UI Designer Agent
    ↓
Code Generator Agent
    ↓
Code Fixer Agent
    ↓
Sandbox Tester Agent
```

Skipping workflow stages is FORBIDDEN.

---

# 8.2 Agent Responsibilities

Planner Agent:

* Analyze requirements
* Split tasks

UI Agent:

* Generate UI structure
* Define layout

Code Agent:

* Generate code

Fix Agent:

* Fix syntax/runtime issues

Test Agent:

* Run Playwright tests
* Verify rendering

---

# 9. Sandbox Rules

---

# 9.1 Security

Generated code MUST run in sandbox.

Mandatory:

```text
iframe sandbox
Docker isolation
```

Forbidden:

```text
Direct host execution
eval()
dangerous scripts
```

---

# 9.2 Runtime

Frontend preview MUST support:

* Hot reload
* Real-time rendering
* Error overlay

---

# 10. Prompt Engineering Rules

---

# 10.1 Prompt Structure

All prompts MUST contain:

```text
Role
Task
Constraints
Output Schema
Examples
```

---

# 10.2 Forbidden Prompt Patterns

Forbidden:

```text
Generate any code you want
```

Forbidden:

```text
Return markdown
```

Forbidden:

```text
Output explanations
```

---

# 10.3 Preferred Prompting

Preferred:

```text
Strict structured output
Explicit schema
Few-shot examples
Deterministic rules
```

---

# 11. Performance Constraints

---

# 11.1 Response Time

Target:

```text
AI generation < 15s
Preview render < 2s
```

---

# 11.2 Scalability

Architecture MUST support:

* Horizontal scaling
* Multi-tenant
* Distributed queues
* Distributed caching

---

# 12. Database Standards

---

# 12.1 Mandatory Tables

```text
user
app
app_version
chat_message
task
```

---

# 12.2 Constraints

Must use:

* indexes
* logical delete
* create/update timestamps

---

# 13. Logging Standards

Must use:

```text
structured logging
traceId
requestId
```

Forbidden:

```text
System.out.println
```

---

# 14. Error Handling

Must use:

```text
Global exception handling
Business exception classes
Error codes
```

Forbidden:

```text
catch(Exception e){}
```

---

# 15. Security Standards

Must support:

* JWT authentication
* RBAC
* Rate limiting
* XSS protection
* CSRF protection
* SQL injection prevention

---

# 16. Deployment Standards

Must support:

```text
Docker
Docker Compose
Kubernetes
CI/CD
```

Preferred:

```text
GitHub Actions
```

---

# 17. Testing Standards

Frontend:

```text
Vitest
Playwright
```

Backend:

```text
JUnit 5
pytest
```

Minimum coverage:

```text
70%
```

---

# 18. AI Coding Constraints

AI MUST NEVER:

* Generate placeholder TODO implementations
* Generate incomplete methods
* Generate pseudo-code
* Omit imports
* Omit dependencies
* Omit configurations

AI MUST ALWAYS:

* Generate runnable code
* Generate complete files
* Generate dependency configuration
* Generate production-ready structure

---

# 19. Long-term Architecture Goals

Future support MUST consider:

* MCP integration
* Multi-agent orchestration
* Plugin ecosystem
* Auto deployment
* Visual workflow editor
* AI memory system
* Team collaboration

---

# 20. Final Principles

The project is NOT:

* a demo
* a toy project
* a single-page generator

The project IS:

* an enterprise-grade AI application platform
* an extensible AI operating system
* a multi-agent application generation framework

All generated code MUST prioritize:

```text
Maintainability
Scalability
Security
Consistency
Production-readiness
```
