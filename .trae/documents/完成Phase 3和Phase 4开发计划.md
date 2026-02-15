# Phase 3 和 Phase 4 开发计划

## 现状分析

通过代码审查，发现项目的主要功能模块已经基本实现：

### 已完成的模块：
1. **代码解析模块 (Phase 3)** - 基本完成
   - `JavaCodeParser` - 使用 JavaParser 解析 Java 源代码
   - `FrameworkDetector` - 检测 Spring/MyBatis 等框架
   - `FrameworkType` - 框架类型枚举
   - 相关模型类 (ClassInfo, MethodInfo, FieldInfo, AnnotationInfo, ParameterInfo)

2. **测试生成模块 (Phase 4)** - 基本完成
   - `TestGenerator` - 主生成器，支持 AI 和模板两种模式
   - `TestGenerationStrategy` - 策略接口
   - `SpringMvcTestStrategy` - Spring MVC 测试策略
   - `SpringBootTestStrategy` - Spring Boot 测试策略
   - `MyBatisTestStrategy` - MyBatis 测试策略
   - `MyBatisPlusTestStrategy` - MyBatis Plus 测试策略

3. **其他核心模块：**
   - `CoverageAnalyzer` - JaCoCo 覆盖率分析
   - `IterativeOptimizer` - 迭代优化器
   - `CLICommand` - 命令行接口
   - LLM 相关模块 - OpenAI/Claude/Ollama/DeepSeek 支持

## 需要完善的工作

### 1. 增强测试覆盖
- 为 `JavaCodeParser` 添加更多边界测试
- 为 `FrameworkDetector` 添加更多框架检测测试
- 为各 `TestGenerationStrategy` 实现类添加完整测试

### 2. 修复潜在问题
- `MyBatisPlusTestStrategy.generateMapperTestMethod` 调用基类方法时参数类型不匹配
- 部分策略类缺少 `@MybatisTest` 等必要注解

### 3. 添加缺失的测试文件
- `SpringBootTestStrategyTest.java`
- `MyBatisTestStrategyTest.java`
- `MyBatisPlusTestStrategyTest.java`
- `CoverageAnalyzerTest.java`
- `IterativeOptimizerTest.java`

### 4. 完善 PromptBuilder
- 优化 LLM 提示词模板
- 添加更多上下文信息

## 实施步骤

### 步骤 1: 修复现有代码问题
- 修复 `MyBatisPlusTestStrategy` 的方法调用问题
- 完善策略类的注解支持

### 步骤 2: 增强现有测试
- 扩展 `JavaCodeParserTest`
- 扩展 `FrameworkDetectorTest`
- 扩展 `TestGeneratorTest`

### 步骤 3: 添加缺失的测试类
- 为所有策略类创建测试
- 为核心模块创建测试

### 步骤 4: 运行测试并修复问题
- 执行 Maven 测试
- 修复发现的问题

请确认此计划后，我将开始实施具体的代码修改。