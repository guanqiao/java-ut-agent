# Java UT Agent Feature 增强文档

## 版本信息
- **版本号**: 1.1.0
- **更新日期**: 2026-02-17
- **开发方式**: TDD (Test-Driven Development)

## 功能描述

本次更新基于竞品对标分析，新增了三大核心模块，显著提升了Java UT Agent的测试生成能力和开发体验。

---

## 一、测试数据生成模块 (testdata/)

### 1.1 TestDataFactory
智能测试数据工厂，支持多种类型自动生成。

**核心能力**:
- 基础类型生成（String, Integer, Long, Double, Boolean等）
- 集合类型生成（List, Map, Set）
- 自定义类型生成（通过反射自动构造）
- 多场景数据生成（NORMAL, EMPTY, NULL, BOUNDARY）
- 可配置的随机种子（支持可重复生成）

**使用示例**:
```java
TestDataFactory factory = new TestDataFactory(12345L);
TestDataValue value = factory.generate(String.class);
List<TestDataValue> scenarios = factory.generateScenarios(Integer.class);
```

### 1.2 BoundaryValueGenerator
边界值生成器，自动生成测试边界数据。

**支持的类型**:
- 整型：MIN_VALUE, MAX_VALUE, 0, -1, 1
- 浮点型：MIN_VALUE, MAX_VALUE, NaN, Infinity
- 字符串：空串、空格、特殊字符、超长字符串
- 自定义范围边界生成

### 1.3 SmartMockBuilder
智能Mock构建器，自动检测依赖并生成Mock配置。

**核心能力**:
- 自动检测类依赖关系
- 生成Mockito配置代码
- 智能Stub生成
- Spy配置支持

### 1.4 ParameterizedTestGenerator
参数化测试生成器，自动生成JUnit 5参数化测试。

**支持的注解**:
- `@ValueSource` - 单参数测试
- `@CsvSource` - 多参数测试
- `@MethodSource` - 复杂类型测试

---

## 二、高级断言模块 (assertion/)

### 2.1 SmartAssertionGenerator
智能断言生成器，根据返回类型自动生成合适的断言代码。

**支持的返回类型**:
| 类型 | 生成的断言 |
|------|-----------|
| boolean | `isTrue()` / `isFalse()` |
| String | `isNotNull()`, `isNotEmpty()` |
| 数值类型 | `isGreaterThanOrEqualTo(0)` |
| Collection | `isNotNull()`, `isNotEmpty()` |
| Optional | `isPresent()` |
| 自定义对象 | `isNotNull()` |

**额外能力**:
- 异常断言生成（`assertThrows`）
- 行为验证生成（`verify`）
- 自动生成断言描述消息

---

## 三、测试维护模块 (maintenance/)

### 3.1 TestFailureAutoFixer
测试失败自动修复器，智能分析失败原因并提供修复建议。

**支持的失败类型**:
| 类型 | 修复策略 |
|------|---------|
| ASSERTION_FAILURE | 更新预期值 |
| NULL_POINTER | 添加@Mock注解 |
| MOCK_CONFIGURATION | 完善Stub配置 |
| TIMEOUT | 增加超时配置 |

**置信度评分**:
- 简单断言修复: 85%
- Null指针修复: 75%
- Mock配置修复: 70%
- 复杂问题: 30%（需人工审查）

---

## 四、IDE集成模块 (ide/)

### 4.1 IDEIntegrationService
IDE集成核心服务，提供统一的测试生成接口。

**核心功能**:
- 测试生成请求/响应模型
- 快速修复建议生成
- 测试代码预览格式化
- Diff高亮显示
- 测试文件位置解析

### 4.2 IDEOptions
IDE配置选项，支持灵活的测试生成配置。

**配置项**:
```java
IDEOptions options = IDEOptions.builder()
    .includePrivateMethods(true)
    .targetCoverage(0.9)
    .generateParameterizedTests(true)
    .includeNegativeTests(true)
    .build();
```

---

## 五、项目上下文模块 (context/)

### 5.1 ProjectContextAnalyzer
项目上下文分析器，提供项目级别的上下文理解。

**分析能力**:
- 项目结构分析（源码目录、测试目录）
- 依赖检测（Maven/Gradle）
- 框架识别（Spring Boot、MyBatis等）
- 测试框架检测（JUnit 5、Mockito、AssertJ）
- 类依赖关系图构建
- 测试模式检测
- 命名规范识别

**缓存机制**:
- 分析结果自动缓存
- 文件变更时自动失效

---

## 六、CI/CD集成模块 (ci/)

### 6.1 PRCommentGenerator
PR评论生成器，自动生成覆盖率报告和质量评分评论。

**生成的评论类型**:
- 覆盖率摘要报告（Markdown表格格式）
- 覆盖率变化趋势（对比基线）
- 质量评分报告（多维度评分）
- 新增测试列表
- PR状态评论（成功/失败）

**Markdown格式示例**:
```markdown
## 📊 Coverage Report

| Type | Coverage | Status |
|------|----------|--------|
| Line | 85% | 🟢 |
| Branch | 72% | 🟡 |
| Instruction | 90% | 🟢 |
| Method | 88% | 🟢 |

**Overall Coverage:** 84%
```

---

## 限制条件

1. **Java版本**: 要求JDK 17+
2. **测试框架**: 主要支持JUnit 5
3. **Mock框架**: 主要支持Mockito
4. **构建工具**: 支持Maven和Gradle

---

## 测试覆盖

| 模块 | 测试类数 | 测试用例数 |
|------|---------|-----------|
| testdata | 4 | 69 |
| assertion | 1 | 12 |
| maintenance | 2 | 30 |
| ide | 1 | 14 |
| context | 1 | 10 |
| ci | 1 | 13 |
| **总计** | **10** | **148** |

---

## 后续规划

### Phase 4 待开发
1. IntelliJ IDEA 原生插件
2. VS Code 扩展
3. GitHub Action 深度集成
4. GitLab CI 支持
5. HTML可视化报告

### 持续优化
1. 对标Diffblue Cover的强化学习优化
2. 对标CodiumAI的项目级上下文理解增强
3. 对标Symflower的自然语言断言解释
