# 提醒助手 — 测试计划 v1.0

> 测试框架：JUnit 5 + AssertJ + Mockito Kotlin + Coroutines Test + Room Test

---

## 一、测试策略

| 层级 | 测试类型 | 工具 | 目标覆盖率 |
|------|---------|------|-----------|
| util | 单元测试 | JUnit 5 + AssertJ | ≥ 95% |
| data/model | 单元测试 | JUnit 5 + AssertJ | ≥ 90% |
| widget | 单元测试 | JUnit 5 | ≥ 90% |
| data/backup | 单元测试 | Gson | ≥ 85% |
| data/repository | 集成测试 | Room In-Memory + Coroutines Test | ≥ 80% |
| scheduler | 单元测试 | Mockito | ≥ 70% |
| ui | Compose UI Test | Compose Test | ≥ 50% |

---

## 二、冒烟测试（Smoke Test）

> 验证核心功能能否「基本跑通」，失败则阻塞后续测试。

| 编号 | 测试用例 | 前置条件 | 预期结果 | 模块 |
|------|---------|---------|---------|------|
| S01 | UrgencyCalculator.calculate 对有效 Task 返回非空结果 | 正常的 Task 对象 | 返回 UrgencyState 非空 | util |
| S02 | UrgencyCalculator.maxUrgency 对空列表返回 CALM | 空列表 | 返回 CALM | util |
| S03 | ReminderCalculator.firstNotifyTime 对 LIGHT 级别返回 09:00 | 到期日=今天,级别=LIGHT | 返回今天 09:00 | util |
| S04 | DateExt.daysUntil 对同一天返回 0 | 两个相同日期 | 返回 0 | util |
| S05 | Task.toDomain 和 toEntity 双向转换一致性 | 完整 Task 对象 | 转换后数据一致 | model |
| S06 | UrgencyState.color() 返回非空 Color | 任意状态 | Color 非空 | model |
| S07 | WidgetColorMapper.colorFor 返回非零值 | 任意状态 | 返回值 ≠ 0 | widget |
| S08 | BackupExporter 导出非空 JSON | 提供示例数据 | JSON 字符串非空 | backup |

---

## 三、覆盖测试（Coverage Test）

### 3.1 UrgencyCalculator

| 编号 | 测试用例 | 输入 | 预期 |
|------|---------|------|------|
| C01 | 今天到期 → CRITICAL | daysLeft=0 | CRITICAL |
| C02 | 明天到期 → URGENT | daysLeft=1 | URGENT |
| C03 | 2天后到期 → URGENT | daysLeft=2 | 实际规则为 ≤1 → URGENT，所以 2 天 → NOTICE |
| C04 | 3天后到期 → NOTICE | daysLeft=3 | NOTICE |
| C05 | 4天后到期 → CALM | daysLeft=4 | CALM |
| C06 | 已过期1天 → CRITICAL | daysLeft=-1 | CRITICAL |
| C07 | 已过期7天 → CRITICAL | daysLeft=-7 | CRITICAL |
| C08 | 半年后到期 → CALM | daysLeft=180 | CALM |
| C09 | maxUrgency 取全集最高 | 混合 4 个级别 | 返回 CRITICAL |
| C10 | maxUrgency 全 CALM | 全 CALM 任务 | 返回 CALM |

### 3.2 ReminderCalculator

| 编号 | 测试用例 | 输入 | 预期 |
|------|---------|------|------|
| C11 | LIGHT 级别首次提醒 | 到期日=2026-05-20, LIGHT | 2026-05-20 09:00 |
| C12 | MEDIUM 级别首次提醒 | 到期日=2026-05-20, MEDIUM | 2026-05-17 09:00 |
| C13 | HEAVY 级别首次提醒 | 到期日=2026-05-20, HEAVY | 2026-05-05 09:00 |
| C14 | CUSTOM 级别首次提醒(7天) | 到期日=2026-05-20, CUSTOM, customAdvanceDays=7 | 2026-05-13 09:00 |
| C15 | CUSTOM 级别首次提醒(30天) | 到期日=2026-05-20, CUSTOM, customAdvanceDays=30 | 2026-04-20 09:00 |
| C16 | ONCE 任务 nextDueDate = dueDate | ONCE 任务, dueDate=X | nextDueDate = X |
| C17 | RECURRING 任务完成后的 nextDueDate | 上次完成=5月10日, interval=3天 | nextDueDate = 5月13日 |
| C18 | RECURRING 任务完成后的 nextDueDate | 上次完成=5月1日, interval=2周 | nextDueDate = 5月15日 |
| C19 | RECURRING 任务完成后的 nextDueDate | 上次完成=1月15日, interval=1月 | nextDueDate = 2月15日 |
| C20 | countdownDays 准确计算 | 到期日-今天=5 | 返回 5 |

### 3.3 DateExt

| 编号 | 测试用例 | 输入 | 预期 |
|------|---------|------|------|
| C21 | toLocalDate 正确转换 | epochMillis=某个值 | 对应 LocalDate |
| C22 | toEpochMillis 往返一致性 | LocalDate→Long→LocalDate | 得到相同日期 |
| C23 | daysUntil (正数) | 5月20日 vs 5月15日 | 5 |
| C24 | daysUntil (今天) | 5月15日 vs 5月15日 | 0 |
| C25 | daysUntil (已过期，负数) | 5月10日 vs 5月15日 | -5 |
| C26 | formatDisplay 中文格式 | 2026-05-15 | "5月15日" |
| C27 | formatDisplayFull 含年份 | 2026-05-15 | "2026年5月15日" |
| C28 | formatISO 标准格式 | 2026-05-15 | "2026-05-15" |
| C29 | toStartOfDay | 任意日期 | 当天 00:00:00 |
| C30 | formatDisplayDate (Long) | x 的 epochMillis | 格式化后的中文日期 |

### 3.4 Task Mapping

| 编号 | 测试用例 | 输入 | 预期 |
|------|---------|------|------|
| C31 | toDomain/tEntity 双向 ONCE 任务 | 完整 Task(ONCE) | 往返一致 |
| C32 | toDomain/tEntity 双向 RECURRING 任务 | 完整 Task(RECURRING) | 往返一致 |
| C33 | toDomain/tEntity 双向 CUSTOM 级别 | 完整 Task(CUSTOM) | 往返一致 |
| C34 | toDomain/tEntity 处理 null 可选字段 | Task(ONCE, intervalUnit=null) | null 字段保持 null |
| C35 | toDomain/tEntity 所有 enum 值 | ReminderType, ReminderLevel, IntervalUnit, CustomNotifyFreq 所有值 | 转换后字符串一致 |

### 3.5 UrgencyState

| 编号 | 测试用例 | 输入 | 预期 |
|------|---------|------|------|
| C36 | CALM.color() = #2E8B57 | CALM | Color(0xFF2E8B57) |
| C37 | NOTICE.color() = #FFC107 | NOTICE | Color(0xFFFFC107) |
| C38 | URGENT.color() = #FF9800 | URGENT | Color(0xFFFF9800) |
| C39 | CRITICAL.color() = #F44336 | CRITICAL | Color(0xFFF44336) |
| C40 | CALM.label() = "安心" | CALM | "安心" |
| C41 | NOTICE.label() = "注意" | NOTICE | "注意" |
| C42 | URGENT.label() = "紧迫" | URGENT | "紧迫" |
| C43 | CRITICAL.label() = "紧急" | CRITICAL | "紧急" |
| C44 | colorHex() 正确格式 | 所有状态 | 包含 # 和 6 位 hex |

### 3.6 WidgetColorMapper

| 编号 | 测试用例 | 输入 | 预期 |
|------|---------|------|------|
| C45 | colorFor(CALM) = 0xFF2E8B57 | CALM | 0xFF2E8B57.toInt() |
| C46 | colorFor(NOTICE) = 0xFFFFC107 | NOTICE | 0xFFFFC107.toInt() |
| C47 | colorFor(URGENT) = 0xFFFF9800 | URGENT | 0xFFFF9800.toInt() |
| C48 | colorFor(CRITICAL) = 0xFFF44336 | CRITICAL | 0xFFF44336.toInt() |
| C49 | hexFor 返回正确的 hex 字符串 | CRITICAL | "#F44336" |
| C50 | stateForColor 反向映射 | 颜色值=0xFF2E8B57 | CALM |

---

## 四、回归测试（Regression Test）

> 保护已修复的 bug 不再复现 + 边界条件验证。

### 4.1 UrgencyCalculator 回归

| 编号 | 测试用例 | 描述 | 预期 |
|------|---------|------|------|
| R01 | 闰年 2月29日 | daysLeft 跨闰年2月 | 正确计算天数 |
| R02 | 跨年边界 | 12月31日→1月1日 | 正确识别 CRITICAL |
| R03 | null categoryId 任务 | categoryId=null 的 task | 不抛异常 |
| R04 | isActive=false 的任务 | 非活跃任务 | 不计入 maxUrgency |
| R05 | 大量任务 (1000) | 1000个活跃任务 | 性能不降级，maxUrgency 正确 |

### 4.2 ReminderCalculator 回归

| 编号 | 测试用例 | 描述 | 预期 |
|------|---------|------|------|
| R06 | 间隔值为 0 | intervalValue=0 | 不抛异常/抛预期异常 |
| R07 | 间隔值为负数 | intervalValue=-1 | 不抛异常/抛预期异常 |
| R08 | CUSTOM customAdvanceDays=0 | 自定义提前0天 | 当天 09:00 |
| R09 | CUSTOM customAdvanceDays 很大 (365) | 提前365天 | 正确计算 |
| R10 | ONCE 任务无 interval 字段 | intervalUnit=null | 不抛异常 |

### 4.3 DateExt 回归

| 编号 | 测试用例 | 描述 | 预期 |
|------|---------|------|------|
| R11 | epochMillis=0 | 1970-01-01 | 不抛异常，返回正确日期 |
| R12 | epochMillis=Long.MAX_VALUE | 极远未来 | 不抛异常 |
| R13 | daysUntil 跨时区 | UTC+8 时区 | 按系统时区计算 |
| R14 | formatDisplay 单月单日 | 2026-01-01 | "1月1日"（非 01月01日） |

### 4.4 状态机回归

| 编号 | 测试用例 | 描述 | 预期 |
|------|---------|------|------|
| R15 | ONCE 任务完成后 isActive=false | markCompleted | isActive 变为 false |
| R16 | RECURRING 任务完成后 isActive 保持 true | markCompleted | isActive 仍为 true，nextDueDate 更新 |
| R17 | 任务删除后 cascade 清理 | CASCADE onDelete | reminder_log 关联记录同步删除 |
| R18 | 分类删除后 cascade 清理任务 | CASCADE onDelete | 关联任务同步删除 |

### 4.5 通知 Color 回归（Theme.kt bug 修复验证）

| 编号 | 测试用例 | 描述 | 预期 |
|------|---------|------|------|
| R19 | CalmColor alpha 不为 0 | CalmColor 值 | alpha=FF |
| R20 | CriticalColor alpha 不为 0 | CriticalColor 值 | alpha=FF |
| R21 | NoticeColor 和 UrgencyState.NOTICE 一致 | 交叉验证 | 颜色值一致 |
| R22 | CalmColor 和 WidgetColorMapper.CALM 一致 | 交叉验证 | 颜色值一致 |

### 4.6 Backup 回归

| 编号 | 测试用例 | 描述 | 预期 |
|------|---------|------|------|
| R23 | 空数据库导出 | 无数据 | 导出合法 JSON，import 恢复空库 |
| R24 | 大量数据导出 | 100 个分类×10 个任务 | JSON 合法，不截断 |
| R25 | 特殊字符标题 | "浇花🌸 第3盆" | JSON 正确转义 |
| R26 | 损坏 JSON 导入 | 非法 JSON | importResult 返回 Error |

---

## 五、测试文件清单

| 文件 | 测试对象 | 用例数 |
|------|---------|--------|
| `util/UrgencyCalculatorTest.kt` | UrgencyCalculator | S01-S02, C01-C10, R01-R05 |
| `util/ReminderCalculatorTest.kt` | ReminderCalculator | S03, C11-C20, R06-R10 |
| `util/DateExtTest.kt` | DateExt | S04, C21-C30, R11-R14 |
| `data/model/TaskMappingTest.kt` | Task Entity↔Domain | S05, C31-C35 |
| `data/model/UrgencyStateTest.kt` | UrgencyState | S06, C36-C44 |
| `widget/WidgetColorMapperTest.kt` | WidgetColorMapper | S07, C45-C50 |
| `scheduler/ThemeColorRegressionTest.kt` | Theme.kt | R19-R22 |
| `data/backup/BackupExportImportTest.kt` | Backup | S08, R23-R26 |