# Java UT Agent

AI驱动的JUnit 5单元测试生成器，支持覆盖率优化。

## 版本信息

- **版本号**: 1.0.0-SNAPSHOT
- **Java版本**: JDK 17+
- **更新日期**: 2026-02-17

## 功能特性

### 核心功能

- **AI驱动测试生成**: 支持多种LLM提供商（OpenAI、Claude、Ollama、DeepSeek）
- **覆盖率优化**: 迭代优化测试代码以达到目标覆盖率
- **框架智能识别**: 自动检测Spring Boot、MyBatis、MyBatis-Plus、Dubbo等框架
- **智能测试数据生成**: 边界值、参数化测试、Mock配置自动生成
- **高级断言生成**: 根据返回类型自动生成合适的断言代码
- **测试失败自动修复**: 智能分析失败原因并提供修复建议

### 支持的框架

| 框架 | 支持程度 |
|------|---------|
| Spring Boot | 完整支持 |
| Spring MVC | 完整支持 |
| MyBatis | 完整支持 |
| MyBatis-Plus | 完整支持 |
| Dubbo | 完整支持 |
| Reactive (WebFlux) | 基础支持 |

## 快速开始

### 环境要求

- JDK 17 或更高版本
- Maven 3.6+ 或 Gradle 7.0+

### 安装

```bash
git clone https://github.com/your-org/java-ut-agent.git
cd java-ut-agent
mvn clean package -DskipTests
```

### 基本用法

```bash
java -jar target/java-ut-agent-1.0.0-SNAPSHOT.jar [options] <source>
```

### 命令行参数

| 参数 | 简写 | 说明 | 默认值 |
|------|------|------|--------|
| `--target` | `-t` | 目标覆盖率 (0.0-1.0) | 0.8 |
| `--iterations` | `-i` | 最大优化迭代次数 | 10 |
| `--api-key` | `-a` | LLM API密钥 | - |
| `--api-url` | - | 自定义API地址 | - |
| `--provider` | `-p` | LLM提供商 | openai |
| `--model` | - | LLM模型名称 | gpt-4 |
| `--output` | `-o` | 输出目录 | - |
| `--verbose` | `-v` | 详细输出模式 | false |
| `--dry-run` | - | 仅生成测试不执行 | false |
| `--analyze-only` | - | 仅分析覆盖率 | false |
| `--detect-framework` | - | 检测使用的框架 | false |
| `--stream` | - | 启用流式输出 | false |
| `--config` | - | 配置文件路径 | - |
| `--init` | - | 初始化配置文件 | false |

### 使用示例

#### 1. 为单个文件生成测试

```bash
java -jar java-ut-agent.jar src/main/java/com/example/MyService.java
```

#### 2. 为整个目录生成测试

```bash
java -jar java-ut-agent.jar src/main/java/com/example/
```

#### 3. 指定目标覆盖率和迭代次数

```bash
java -jar java-ut-agent.jar -t 0.9 -i 15 src/main/java/com/example/
```

#### 4. 使用不同的LLM提供商

```bash
# 使用Claude
java -jar java-ut-agent.jar -p claude -a $ANTHROPIC_API_KEY src/main/java/

# 使用DeepSeek
java -jar java-ut-agent.jar -p deepseek -a $DEEPSEEK_API_KEY src/main/java/

# 使用本地Ollama
java -jar java-ut-agent.jar -p ollama --api-url http://localhost:11434/api src/main/java/
```

#### 5. 仅分析覆盖率

```bash
java -jar java-ut-agent.jar --analyze-only src/main/java/
```

#### 6. 检测项目使用的框架

```bash
java -jar java-ut-agent.jar --detect-framework src/main/java/
```

#### 7. 干运行模式（不执行测试）

```bash
java -jar java-ut-agent.jar --dry-run src/main/java/com/example/MyService.java
```

## 配置文件

### 初始化配置

```bash
java -jar java-ut-agent.jar --init
```

这将在当前目录创建 `ut-agent.yml` 配置文件。

### 配置文件示例 (ut-agent.yml)

```yaml
llm:
  provider: openai
  api-key: ${OPENAI_API_KEY}
  model: gpt-4
  temperature: 0.7
  max-tokens: 4096
  max-retries: 3

coverage:
  target: 0.8
  max-iterations: 10
  include-branch-coverage: true

generation:
  strategy: ai
  include-negative-tests: true
  include-edge-cases: true
  include-parameterized-tests: false
  test-data-strategy: simple
  verify-mocks: true

output:
  directory: src/test/java
  format: junit5
  verbose: false
  color-output: true
  show-progress: true
```

### 配置项说明

#### LLM配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `provider` | LLM提供商 (openai/claude/ollama/deepseek) | openai |
| `api-key` | API密钥，支持环境变量 `${VAR_NAME}` | - |
| `base-url` | 自定义API地址 | - |
| `model` | 模型名称 | gpt-4 |
| `temperature` | 生成温度 | 0.7 |
| `max-tokens` | 最大Token数 | 4096 |
| `max-retries` | 最大重试次数 | 3 |
| `ca-cert-path` | 自定义CA证书路径 | - |

#### 覆盖率配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `target` | 目标覆盖率 (0.0-1.0) | 0.8 |
| `max-iterations` | 最大优化迭代次数 | 10 |
| `include-branch-coverage` | 是否包含分支覆盖率 | true |

#### 生成配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `strategy` | 生成策略 (ai/template) | ai |
| `include-negative-tests` | 包含负面测试 | true |
| `include-edge-cases` | 包含边界测试 | true |
| `include-parameterized-tests` | 包含参数化测试 | false |
| `test-data-strategy` | 测试数据策略 (simple/builder/instancio) | simple |
| `verify-mocks` | 验证Mock调用 | true |

#### 输出配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `directory` | 输出目录 | src/test/java |
| `format` | 输出格式 | junit5 |
| `verbose` | 详细输出 | false |
| `color-output` | 彩色输出 | true |
| `show-progress` | 显示进度 | true |

## API密钥配置

支持多种方式配置API密钥：

### 1. 环境变量（推荐）

```bash
export OPENAI_API_KEY=sk-xxx
export ANTHROPIC_API_KEY=sk-ant-xxx
export DEEPSEEK_API_KEY=sk-xxx
```

### 2. 命令行参数

```bash
java -jar java-ut-agent.jar -a sk-xxx src/main/java/
```

### 3. 配置文件

```yaml
llm:
  api-key: sk-xxx
```

### 4. 环境变量引用

```yaml
llm:
  api-key: ${OPENAI_API_KEY}
```

## 支持的LLM提供商

| 提供商 | ID | 默认API地址 | 环境变量 |
|--------|-----|------------|---------|
| OpenAI | openai | https://api.openai.com/v1 | OPENAI_API_KEY |
| Anthropic Claude | claude | https://api.anthropic.com/v1 | ANTHROPIC_API_KEY |
| DeepSeek | deepseek | https://api.deepseek.com/v1 | DEEPSEEK_API_KEY |
| Ollama (本地) | ollama | http://localhost:11434/api | - |

## 测试策略

### Spring Boot 测试策略

自动识别Spring Boot应用并生成：
- `@SpringBootTest` 配置
- `@MockBean` 依赖注入
- Web MVC测试 (`@WebMvcTest`)
- Data JPA测试 (`@DataJpaTest`)

### MyBatis 测试策略

自动识别MyBatis Mapper并生成：
- `@MybatisTest` 配置
- H2内存数据库配置
- SQL脚本初始化

### Dubbo 测试策略

自动识别Dubbo服务并生成：
- `@DubboReference` Mock配置
- 服务降级测试

## CI/CD集成

### GitHub Actions

```yaml
name: Generate Tests
on: [pull_request]

jobs:
  generate-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Generate Tests
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
        run: |
          java -jar java-ut-agent.jar -t 0.8 src/main/java/
      - name: Run Tests
        run: mvn test
```

### GitLab CI

```yaml
generate-tests:
  stage: test
  image: maven:3.9-eclipse-temurin-17
  script:
    - java -jar java-ut-agent.jar -t 0.8 src/main/java/
    - mvn test
  variables:
    OPENAI_API_KEY: $OPENAI_API_KEY
```

## 项目结构

```
java-ut-agent/
├── src/main/java/com/utagent/
│   ├── ai/                    # AI测试生成
│   ├── assertion/             # 智能断言生成
│   ├── build/                 # 构建工具适配
│   ├── cache/                 # 缓存机制
│   ├── ci/                    # CI/CD集成
│   ├── cli/                   # 命令行接口
│   ├── config/                # 配置管理
│   ├── context/               # 项目上下文分析
│   ├── coverage/              # 覆盖率分析
│   ├── di/                    # 依赖注入
│   ├── exception/             # 异常处理
│   ├── explainer/             # 断言解释
│   ├── generator/             # 测试生成核心
│   │   ├── llm/               # LLM客户端
│   │   └── strategy/          # 框架测试策略
│   ├── git/                   # Git变更检测
│   ├── i18n/                  # 国际化支持
│   ├── ide/                   # IDE集成
│   ├── llm/                   # LLM提供商
│   ├── maintenance/           # 测试维护
│   ├── metrics/               # 指标收集
│   ├── model/                 # 数据模型
│   ├── monitoring/            # 性能监控
│   ├── mutation/              # 变异测试
│   ├── optimizer/             # 测试优化
│   ├── parser/                # 代码解析
│   ├── patterns/              # 测试模式库
│   ├── plugin/                # 插件支持
│   ├── quality/               # 质量评分
│   ├── report/                # 报告生成
│   ├── service/               # 服务层
│   ├── team/                  # 团队协作
│   ├── terminal/              # 终端输出
│   ├── testdata/              # 测试数据生成
│   └── util/                  # 工具类
└── src/test/java/             # 测试代码
```

## 限制条件

1. **Java版本**: 要求JDK 17+
2. **测试框架**: 主要支持JUnit 5
3. **Mock框架**: 主要支持Mockito
4. **构建工具**: 支持Maven和Gradle
5. **LLM要求**: 需要有效的API密钥（本地Ollama除外）

## 常见问题

### Q: 如何使用本地Ollama？

```bash
ollama pull codellama
ollama serve
java -jar java-ut-agent.jar -p ollama --model codellama src/main/java/
```

### Q: 如何处理自签名证书？

```yaml
llm:
  ca-cert-path: /path/to/ca.crt
```

### Q: 如何排除特定类？

在配置文件中添加排除规则（计划功能）。

### Q: 生成的测试质量如何？

工具会自动进行：
- 测试异味检测
- 质量评分
- 变异测试分析
- 迭代优化

## 许可证

Apache License 2.0

## 贡献指南

欢迎提交Issue和Pull Request。

## 更新日志

### v1.0.0 (2026-02-17)
- 初始版本发布
- 支持AI驱动的测试生成
- 支持多种LLM提供商
- 支持覆盖率优化
- 支持主流框架识别
