## 改进计划概览

基于代码分析，以下是按优先级排列的改进项：

---

### Phase 1: 资源管理与安全性修复 (高优先级)

**1.1 修复 Process 资源泄漏**
- 文件: [IterativeOptimizer.java:264-285](file:///f:/dev/opensource/java-ut-agent/src/main/java/com/utagent/optimizer/IterativeOptimizer.java#L264)
- 问题: Process 的输入/输出流未关闭
- 方案: 使用 try-with-resources 包装 Process 流

**1.2 补充 GitChangeDetector 单元测试**
- 新增测试文件: `GitChangeDetectorTest.java`
- 覆盖场景: isGitRepository, getChangedFiles, getUncommittedChanges

---

### Phase 2: 代码质量提升 (中优先级)

**2.1 优化异常处理**
- 问题: 24 处 `catch (Exception e)` 过于宽泛
- 重点文件: GitChangeDetector, IterativeOptimizer, CoverageAnalyzer
- 方案: 捕获具体异常类型 (IOException, InterruptedException 等)

**2.2 完善测试覆盖**
- 新增 `LLMProviderFactoryTest.java`
- 增强 `CoverageAnalyzerTest.java` 边界测试

**2.3 优化 TODO 注释**
- 6 处 TODO 在测试生成模板中，改为更明确的占位符注释

---

### Phase 3: 架构优化 (可选)

**3.1 CLI 类拆分**
- CLICommand.java (428行) → 拆分为:
  - CLIController: 核心命令处理
  - OutputFormatter: 输出格式化
  - ConfigMerger: 配置合并逻辑

**3.2 模型类防御性拷贝**
- ClassInfo, MethodInfo 等 record 类添加不可变集合返回

---

### 实施顺序
1. Phase 1 (资源管理) → 运行测试验证
2. Phase 2 (代码质量) → 运行测试验证  
3. Phase 3 (架构优化) → 可选，根据时间安排

预计改动文件: 5-8 个
预计新增测试: 2-3 个