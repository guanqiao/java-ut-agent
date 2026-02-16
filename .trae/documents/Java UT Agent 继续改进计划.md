
# Java UT Agent 继续改进计划

## 当前状态分析
项目已完成高优先级改进（异常处理、安全增强、依赖注入、缓存、监控、SPI），SpotBugs 发现了 70+ 个问题需要修复。

## 改进计划

### Phase 1: 修复 SpotBugs 高优先级问题
1. **修复默认编码依赖问题** (High)
   - `DM_DEFAULT_ENCODING` 问题：在 `LLMResponseCache`、`ParseResultCache`、`GitChangeDetector`、`IterativeOptimizer` 中显式指定 UTF-8 编码
   
2. **修复数组 hashCode 调用问题** (Medium)
   - `DMI_INVOKING_HASHCODE_ON_ARRAY`：在 `ParseResultCache.computeHashBytes` 中使用 `Arrays.hashCode()`

### Phase 2: 修复 SpotBugs 中优先级问题
1. **修复文件删除返回值忽略问题** (Medium)
   - `RV_RETURN_VALUE_IGNORED_BAD_PRACTICE`：在缓存类中检查 `File.delete()` 返回值

2. **修复空指针风险问题** (Medium)
   - `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE`：在 `LLMClient` 和 `AbstractLLMProvider` 中添加空值检查

3. **修复内部表示暴露问题** (Medium)
   - `EI_EXPOSE_REP` / `EI_EXPOSE_REP2`：在模型类中返回不可修改的集合副本

4. **修复其他问题**
   - 未读字段、格式字符串换行、switch 缺少 default 等

### Phase 3: 性能与质量优化（可选）
1. **代码生成模板化** - 使用模板引擎替代字符串拼接
2. **HTTP 连接池优化** - 共享连接池，实现自适应线程池
3. **Checkstyle 问题修复** - 代码风格规范化

## 实施步骤
1. 先修复 High 优先级问题
2. 再修复 Medium 优先级问题（按影响范围排序）
3. 运行完整测试验证
4. 可选：继续 Phase 3 优化

请问您希望从哪个阶段开始？
