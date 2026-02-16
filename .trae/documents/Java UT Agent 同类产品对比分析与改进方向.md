# Java UT Agent 改进计划 - TDD 方式

## 开发原则
1. **Red** - 先编写失败的测试用例
2. **Green** - 编写最小实现使测试通过
3. **Refactor** - 重构代码，保持测试通过

---

## Phase 1: 变异测试支持（TDD）

### 1.1 变异测试核心功能
**测试先行**：
```
test/java/com/utagent/mutation/
├── MutationAnalyzerTest.java      # 变异分析器测试
├── MutationScoreCalculatorTest.java # 变异得分计算测试
└── MutationReportGeneratorTest.java # 变异报告生成测试
```

**实现顺序**：
1. 编写 `MutationAnalyzerTest` - 测试 PITest 集成
2. 实现 `MutationAnalyzer` - 使测试通过
3. 编写 `MutationScoreCalculatorTest` - 测试得分计算
4. 实现 `MutationScoreCalculator` - 使测试通过
5. 编写 `MutationReportGeneratorTest` - 测试报告生成
6. 实现 `MutationReportGenerator` - 使测试通过

### 1.2 与测试生成流程集成
**测试先行**：
```
test/java/com/utagent/optimizer/
├── IterativeOptimizerMutationTest.java # 变异测试集成测试
└── TestQualityEvaluatorTest.java       # 测试质量评估测试
```

---

## Phase 2: 测试质量评估体系（TDD）

### 2.1 测试质量评分
**测试先行**：
```
test/java/com/utagent/quality/
├── TestQualityScorerTest.java      # 质量评分测试
├── TestSmellDetectorTest.java      # 测试异味检测测试
└── TestReadabilityAnalyzerTest.java # 可读性分析测试
```

**实现顺序**：
1. 编写 `TestQualityScorerTest` - 定义评分规则
2. 实现 `TestQualityScorer` - 覆盖率 + 变异得分 + 可读性
3. 编写 `TestSmellDetectorTest` - 检测重复、冗余测试
4. 实现 `TestSmellDetector` - 使测试通过

---

## Phase 3: 测试自动维护（TDD）

### 3.1 测试失效检测
**测试先行**：
```
test/java/com/utagent/maintenance/
├── TestFailureDetectorTest.java    # 失效检测测试
├── TestUpdaterTest.java            # 测试更新测试
└── ChangeImpactAnalyzerTest.java   # 变更影响分析测试
```

**实现顺序**：
1. 编写 `TestFailureDetectorTest` - 检测编译失败、运行失败
2. 实现 `TestFailureDetector` - 使测试通过
3. 编写 `ChangeImpactAnalyzerTest` - 分析代码变更影响
4. 实现 `ChangeImpactAnalyzer` - 使测试通过

---

## TDD 开发流程示例

### 示例：MutationAnalyzer

**Step 1: Red - 编写失败测试**
```java
// MutationAnalyzerTest.java
@Test
void shouldRunMutationAnalysisAndReturnScore() {
    // Given
    File testClass = new File("src/test/java/SampleTest.java");
    File targetClass = new File("src/main/java/Sample.java");
    MutationAnalyzer analyzer = new MutationAnalyzer(projectRoot);
    
    // When
    MutationReport report = analyzer.analyze(targetClass, testClass);
    
    // Then
    assertThat(report.getMutationScore()).isGreaterThan(0.0);
    assertThat(report.getKilledMutants()).isNotEmpty();
}
```

**Step 2: Green - 最小实现**
```java
// MutationAnalyzer.java
public MutationReport analyze(File targetClass, File testClass) {
    // 调用 PITest CLI
    // 解析结果
    // 返回报告
}
```

**Step 3: Refactor - 优化代码**
- 提取接口
- 优化异常处理
- 添加缓存

---

## 实施计划

### Week 1: 变异测试基础
- Day 1-2: 编写 MutationAnalyzer 测试 + 实现
- Day 3-4: 编写 MutationScoreCalculator 测试 + 实现
- Day 5: 编写 MutationReportGenerator 测试 + 实现

### Week 2: 质量评估 + 集成
- Day 1-2: 编写 TestQualityScorer 测试 + 实现
- Day 3-4: 集成到 IterativeOptimizer
- Day 5: 端到端测试

### Week 3: 自动维护
- Day 1-2: 编写 TestFailureDetector 测试 + 实现
- Day 3-4: 编写 ChangeImpactAnalyzer 测试 + 实现
- Day 5: 集成测试

---

## 验收标准

每个功能模块必须：
1. ✅ 测试覆盖率 ≥ 80%
2. ✅ 所有测试通过
3. ✅ SpotBugs 无高优先级问题
4. ✅ 代码审查通过

是否开始按 TDD 方式实施？