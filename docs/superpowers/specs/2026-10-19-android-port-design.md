# 随机汉字 Android 版 — 设计文档

日期：2026-10-19

## 目标

把现有 SwiftUI iOS App「随机汉字」完整移植为原生 Android App，功能、交互与视觉对齐，代码放在同一仓库的 `android/` 目录，iOS 工程保持不动。

## 已确认的决策

- 技术栈：Kotlin + Jetpack Compose（原生 Android）。
- 仓库结构：同仓库 `android/` 子目录；iOS 源码、设计文档、截图继续保持在根目录。
- 范围：完整对齐 iOS，包括学习、错题本、复习、设置/字库管理、朗读、田字格、内嵌正楷字体、本地持久化、应用图标与启动页、单元测试。
- 不做：Compose Multiplatform、Flutter、跨端共享代码。

## 技术栈与版本

- 单模块 Gradle 工程，Gradle Kotlin DSL。
- applicationId `com.liuli.hanzirandom`，应用名「随机汉字」。
- `minSdk 26`，`compileSdk 35`，`targetSdk 35`。
- AGP 8.6.x + Gradle 8.7（本机 `~/.gradle` 已缓存该发行版）+ Kotlin 2.0.x + Compose BOM。
- JDK 17（AGP 8.x 要求；本机现有 JDK 11/25/26 不适用，实施时安装 Temurin 17）。
- 无第三方运行时依赖；仅使用 AndroidX（Compose、Lifecycle、Navigation、Core SplashScreen）。
- 不申请任何权限，全程无网络请求。

## 工程结构

```
android/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  gradle/wrapper/…
  gradlew
  app/
    build.gradle.kts
    src/main/AndroidManifest.xml
    src/main/java/com/liuli/hanzirandom/
      MainActivity.kt
      data/KeyValueStore.kt
      data/CharacterRepository.kt
      data/SettingsRepository.kt
      domain/DrawSession.kt
      domain/SpeechService.kt
      ui/App.kt
      ui/StudyScreen.kt
      ui/MistakeBookScreen.kt
      ui/SettingsScreen.kt
      ui/CharacterDetailScreen.kt
      ui/theme/Theme.kt
      ui/components/TianZiGe.kt
      ui/components/PillButtons.kt
    src/main/res/font/lxgw_wenkai_gb_lite_regular.ttf
    src/main/res/…（图标、启动页、字符串、主题）
    src/test/java/com/liuli/hanzirandom/…（单测）
    src/androidTest/java/com/liuli/hanzirandom/…（Compose UI 冒烟测试）
```

## 架构

### KeyValueStore

窄接口：`getString/putString/remove`。
- `SharedPrefsKeyValueStore`：生产实现，基于 `SharedPreferences`。
- `InMemoryKeyValueStore`：测试实现，让仓库逻辑可在纯 JVM 单测中验证。

### CharacterRepository

职责与 iOS `CharacterStore` 对齐：
- 字库 `allCharacters: List<String>`，首次运行用与 iOS 相同的预置字库初始化（同一段字符串、同一去重顺序）。
- 错题 `mistakes: Map<String, Int>`。
- 对外暴露 `StateFlow`，供 Compose 与 ViewModel 订阅。
- 方法：`startRound`、`reset`、`addMistake`、`removeMistake`、`clearMistakes`、`mistakesSorted`、`addCharacters(text)`、`isHanzi`、`remove`、`exportString`、`persistRound`（`restoreSnapshot` 为私有内部实现）。
- 持久化键与 iOS 一致：`allCharacters`、`mistakes`、`roundSnapshot`。
- 字库/轮次用换行分隔的纯文本编码，错题用 `字\t次数` 行；每个条目都是单个汉字（不含换行/制表符），编码无歧义。这样仓库逻辑可在纯 JVM 单测中运行（`org.json` 在 Android 单元测试桩中不可用）。
- 轮次快照存 `pool/history/cursor/total/finished`，编码首行为元信息、其后为池与历史条目。
- 恢复轮次时沿用 iOS 的合法性校验：`total == pool.size + history.size`，且 `(history 为空 && cursor == -1)` 或 `(history 非空 && cursor 在范围内)`。

### SettingsRepository

- `fontSize: Float`（默认 150，范围 60–260）。
- `swipeEnabled: Boolean`（默认 true）。
- 键名与 iOS 一致：`fontSize`、`swipeEnabled`，暴露 `StateFlow`。

### DrawSession

1:1 移植 iOS 状态机，纯 Kotlin、无 Android 依赖：
- 状态：`pool`、`current`、`total`、`drawnCount`、`history`、`cursor`、`isFinished`，以 `StateFlow` 暴露。
- 方法：`restart`、`drawNext`、`appendToPool`、`removeCharacter`、`restore`、`removeCurrent`、`goPrevious`、`goNext`。
- 派生属性：`canGoPrevious`、`canGoNext`、`position`。
- 学习轮次与复习轮次各自持有独立实例。

### SpeechService

- 封装 Android `TextToSpeech`，语言 `Locale.CHINESE` / `zh-CN`。
- 每次朗读使用 `QUEUE_FLUSH`，不排队重叠；初始化失败或中文语音缺失时静默。
- 生命周期内 `shutdown()`。

### ViewModel

- `StudyViewModel`：学习轮次、下一个/上一个、加入错题、重置、朗读。
- `StudyViewModel` 同时支持复习模式（独立 `DrawSession`、删除错题）。
- `MistakeBookViewModel`：排序列表、删除、清空、发起复习。
- `SettingsViewModel`：字号、手势开关、批量添加、删除、导出、重新抽卡。
- 应用通过 `ProcessLifecycleOwner` 在 `ON_STOP` 时调用 `persistRound()`。

## 界面与交互

### 根界面

- 单 Activity + Compose；底部导航两个 Tab：「学习」「错题本」。
- 视觉沿用 iOS：暖橙主色 `#FF9E40` / 深橙 `#F27326`、暖黄背景、圆角渐变卡片、胶囊按钮、浅橙虚线田字格。
- 内容栏最大宽度约 560dp 居中，适配平板/折叠屏；浅色主题。

### 学习页

- 顶部：「第 X / 共 Y」+ 进度条。
- 中部：田字格 + 正楷大字（字号取设置值，自动缩放）。
- 点大字 = 切下一个；左右滑 = 下一个/上一个（可关闭手势）。
- 操作区：「读一读」「加入错题本」（短暂显示「已加入 ✓」+ 触感反馈）；复习模式下额外有「删除错题」。
- 底部：「上一个」「下一个」；抽完时显示「全部抽完」+「重置」。
- 空字库：显示「请先在设置里添加汉字」+「去添加汉字」。
- 右上角设置入口。

### 错题本

- 空状态提示（iOS 同款文案语义）。
- 顶部「开始复习（N 个）」；列表按错误次数降序，同次数按字库顺序。
- 每行：正楷字卡、「错 X 次」胶囊、删除按钮；点行进入详情。
- 清空需确认对话框；空列表时隐藏复习/清空入口。
- 设置入口常驻。

### 复习模式

- 全屏独立轮次，标题「错题复习」，可关闭返回。
- 删除错题同时从错题记录和当前复习轮次中移除。

### 设置

- 字号区块：示例字 + 滑块（60–260）+ 数值。
- 手势区块：左右滑动开关。
- 抽卡区块：「重新开始抽卡」。
- 添加区块：多行输入 +「添加」，提示「已添加 N 个汉字」/「没有新增汉字」。
- 导出区块：「复制到剪贴板」（短暂变「已复制」）、「分享/存到备忘录」（系统分享面板，文本为空格分隔字库）。
- 字库区块：数量标题、每行正楷字 + 复制按钮、点击进详情、滑删；顶部「完成」返回。

### 详情页

- 田字格 + 放大汉字，点字朗读。
- 「读一读」「复制」按钮，关闭返回。

### 图标与启动页

- 用现有 `Sources/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png` 生成各密度 `ic_launcher` 与 Android 8+ 自适应图标。
- SplashScreen：暖橙红底色（#FF5B49，与图标一致）+ 应用图标，Android 12+ 原生启动画面，低版本用 AndroidX core-splashscreen。

## 字体

- 复制 `Sources/Resources/Fonts/LXGWWenKaiGBLite-Regular.ttf` 到 `android/app/src/main/res/font/lxgw_wenkai_gb_lite_regular.ttf`（Android 资源名必须小写）。
- 通过 Compose `FontFamily(Font(R.font.…))` 使用；保留 `OFL.txt` 授权说明。
- 字体是教学正楷的关键，不替换为系统字体。

## 数据流

- 设置页修改字库/错题 → Repository 更新 StateFlow 并写入 SharedPreferences。
- 学习页只读字库启动/重置轮次；所有抽卡操作在 `DrawSession` 内完成。
- 退到后台 → 保存轮次快照；下次启动恢复。
- 点击朗读 / 复制 / 分享 → 调对应平台服务。

## 错误与边界

- 空字库：禁用「下一个」，显示引导。
- 添加文本含非汉字/重复：静默过滤与去重，不报错。
- 朗读引擎不可用：静默失败，不崩溃。
- 轮次快照非法：丢弃并正常启动。
- TTS 在 `ViewModel.onCleared` 或 Application 生命周期结束时释放。

## 测试

单元测试（JVM）：
- `DrawSession`：一轮不重复、抽完状态、重置、追加新字、删除当前字/历史字、前后切换、快照恢复。
- `CharacterRepository`：预置初始化、批量添加双重去重/非汉字过滤/顺序、删除、错题计数与排序、清空、导出字符串、轮次持久化与恢复、非法快照丢弃。
- `SettingsRepository`：默认值、读写。

Compose UI 冒烟测试（androidTest，模拟器运行）：
- 启动后显示 Tab 与学习页汉字；切换到错题本显示空状态。

## 验收

1. `./gradlew assembleDebug` 成功。
2. 单元测试通过。
3. 安装到本机 `Pixel_Fold_API_35` 模拟器，逐页截图验证：学习页、错题本、设置页、详情页。
4. 朗读、复制、滑动手势、轮次恢复手动验证（TTS 依赖模拟器语音引擎，无中文语音时属预期静默）。

## 不做的事（YAGNI）

- 拼音标注、笔顺动画、自动朗读。
- 账号、云同步、跨设备数据迁移。
- 深色主题、动态取色。
- 多语言界面（仅中文）。
