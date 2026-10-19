# 随机汉字抽取 App — 设计文档

日期：2026-10-15

## 目标

一个 iOS App，从一组汉字中每次随机抽取一个展示。点「下一个」从本轮剩余（未抽过）的汉字里再随机抽一个。用于识字/认字练习。

## 已确认的需求

- **字库来源**：预置一批默认汉字（硬编码），用户也可在 App 内添加/删除。
- **预置字库（60 个字）**：
  欢 给 哈 教 室 师 怎 惊 跳 歌 舞 找 做 故 担 拍 伸 拥 抓 寒 赛 温 湿 暖 冷 办 醒 被 对 杏
  一 二 三 人 个 小 雨 天 车 开 火 门 口 月 羊 只 有 牛 水 中 风 头 土 里 在 日 白 马 手 出
- **抽取行为**：一轮内不重复；每次打开 App 重新洗一轮。
- **抽完之后**：显示「全部抽完」，提供手动「重置」按钮，点了才重新开始。
- **持久化**：用户添加/删除的汉字保存在本地（UserDefaults），下次打开仍在；字号偏好也持久化。
- **朗读**：点击当前显示的大字，用中文语音（zh-CN）朗读；不自动朗读，可反复点。
- **展示增强**：
  - 显示进度：「第 X / 共 Y」。
  - 可调字号：在「管理」页顶部用滑块调整当前字的显示大小（偏好持久化）。
  - 前后切换：底部「上一个 / 下一个」按钮，并支持在主页左右滑动手势。「下一个」= 抽一个新字；「上一个」= 回看上一个抽过的字（按本轮抽过的顺序回退，走到最早一个时禁用）。
  - 手势开关：「管理」页可切换是否启用左右滑动手势（偏好持久化）。关闭后手势不生效，只能用按钮切换。

## 技术栈

- SwiftUI，iOS 16+
- `UserDefaults` 做持久化
- `AVSpeechSynthesizer` 做中文朗读
- `UIPasteboard` 做复制，SwiftUI `ShareLink` 做分享到备忘录
- 无第三方依赖

## 架构与组件

### CharacterStore（ObservableObject）— 核心数据与抽取逻辑

职责：管理全部汉字、当前轮次的抽取状态、持久化。

状态：
- `allCharacters: [String]` — 全部汉字（预置 + 用户增删）。持久化到 UserDefaults。
- `pool: [String]` — 当前轮次剩余可抽的汉字（内存状态，不持久化）。
- `current: String?` — 当前展示的汉字（内存状态）。
- `total: Int` — 本轮汉字总数（= 开始这轮时 allCharacters 的数量），用于进度显示。
- `drawnCount: Int` — 本轮已抽出的数量，用于进度显示「第 drawnCount / 共 total」。
- `isFinished: Bool` — pool 为空且已抽过（即全部抽完）。

方法：
- `drawNext()` — 从 pool 随机取一个移出，设为 current，drawnCount +1。pool 空则进入 finished 状态。
- `reset()` — 把 allCharacters 放回 pool（打乱），drawnCount 归零，重新抽第一个。
- `addCharacters(from text: String)` — 把输入文本拆成单字、过滤非汉字/空白、去重后合并进 allCharacters，保存。去重为双重：(1) 输入文本内部重复的字只保留一个；(2) 已存在于 allCharacters 的字跳过。新字按首次出现顺序追加到末尾。
- `remove(_ character: String)` — 从 allCharacters 删除并保存。
- `exportString() -> String` — 返回 allCharacters 以空格分隔的字符串（如「欢 给 哈」），同时用于复制到剪贴板和分享到备忘录。
- 首次运行（UserDefaults 无数据）用预置 31 字初始化。

边界与约定：
- 空字库：allCharacters 为空时，主页显示提示「请先在管理页添加汉字」，「下一个」不可用。
- 「一个字」= 一个 Unicode 字符（这里都是单个 CJK 汉字）。拆分按字符拆。
- 去重：既跳过已存在于字库的字，也合并输入文本内部的重复（同一个字在一次输入里只加一次）。

### SpeechService — 朗读封装

职责：封装 `AVSpeechSynthesizer`。
- `speak(_ text: String)` — 用 `AVSpeechSynthesisVoice(language: "zh-CN")` 朗读；每次朗读前停止上一次，避免排队重叠。

### 视图层

- **DrawView（主页/抽取页）**
  - 顶部：进度「第 X / 共 Y」。
  - 中间：大字显示 current，点击 → SpeechService.speak。
  - 字号滑块：调整当前字字号（偏好存 UserDefaults）。
  - 底部：「下一个」按钮；isFinished 时显示「全部抽完」文案 + 「重置」按钮。
  - 导航栏：进入 ManageView 的入口。
- **ManageView（管理页）**
  - 顶部：多行输入框，可批量粘贴一大段文字；「添加」按钮 → addCharacters。
  - 导出区（两个并排操作，内容都是 exportString() 空格分隔的全部汉字）：
    - 「复制到剪贴板」按钮：写入 `UIPasteboard.general.string`，复制后给简短提示（按钮文案短暂变「已复制」）。
    - 「存到备忘录」：用 SwiftUI `ShareLink`（iOS 16+）弹出系统分享菜单，用户选「备忘录」即存成一条笔记，也可分享到微信/邮件等。（iOS 无公开 API 直接写入备忘录，分享菜单是唯一正规做法。）
  - 列表：展示 allCharacters，支持左滑删除（remove）；点某一行 → 弹出 CharacterDetailView（sheet）。
- **CharacterDetailView（字详情，sheet 弹出）**
  - 把该字放大居中显示；点击大字 → SpeechService.speak 中文朗读。
  - 「复制」按钮：复制这一个字到剪贴板。
  - 关闭按钮返回管理页。
- **单字复制（贯穿各处）**
  - 主页当前大字：长按出现「复制」菜单（contextMenu），复制当前这个字。
  - 管理页列表每行：行尾「复制」按钮 / 左滑或 contextMenu，复制该字。
  - 字详情：如上，有「复制」按钮。
  - 均通过 `UIPasteboard.general.string = 单字` 实现。

## 数据流

- ManageView 修改 allCharacters → CharacterStore 保存 UserDefaults。
- DrawView 只读 allCharacters 填充 pool；reset / drawNext 操作内存中的 pool/current。
- current 变化 → DrawView 刷新；点字 → SpeechService 朗读。

## 错误/边界处理

- 空字库：禁用「下一个」，显示引导文案。
- 输入里含非汉字字符或重复字：addCharacters 自动过滤与去重，不报错。
- 朗读：中文语音不可用时静默失败（不崩溃）。

## 测试

对 CharacterStore 写单元测试（纯逻辑，可测）：
- drawNext 在一轮内不重复抽字。
- 抽到最后一个后 isFinished 为 true、pool 为空。
- reset 后 pool 恢复为全部汉字、drawnCount 归零。
- addCharacters 去重（跟已有库去重 + 输入内部去重两种情况）、过滤空白/非汉字、保留首次出现顺序、合并正确。
- remove 正确删除。
- exportString 以空格分隔、顺序与 allCharacters 一致。
- 进度计数（drawnCount / total）正确。

朗读（AVSpeechSynthesizer）涉及音频硬件，不做自动化测试，手动验证。

## 工程结构

项目建在 `~/HanziRandom`（独立目录，已初始化 git）。用 SwiftUI App 模板，`@main` App 持有一个 `CharacterStore` 作为 `@StateObject` 并以 `environmentObject` 下发。

## 不做的事（YAGNI）

- 不做拼音标注、笔顺动画（本次明确不要）。
- 不做跨设备同步、账号系统。
- 不做自动朗读。
