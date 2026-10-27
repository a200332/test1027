# 每日要闻（DailyBrief）

一个原生 Kotlin 的 Android 新闻聚合客户端：每天从多个公开 RSS 新闻源并发采集最新内容，按时间排序、去重并缓存到本地，支持分类浏览、搜索、原文跳转和每日通知。

> 客户端直接读取各媒体公开发布的 RSS，不依赖自建后端，也不需要 API Key。

## 功能

- **多源并发采集**：Google 新闻、BBC 中文、纽约时报中文网、36氪、少数派、界面新闻，单源失败不影响其他源。
- **每日最新**：下拉刷新 / 右上角刷新；按发布时间倒序排列，跨源按链接与标题去重。
- **分类与搜索**：全部、头条、国际、科技、财经；支持按标题、摘要、来源实时搜索。
- **离线阅读**：最近一次成功结果写入本地 JSON，断网时自动展示缓存并给出提示。
- **新闻详情**：标题、来源、发布时间、摘要/正文、配图，支持分享和用浏览器阅读原文。
- **每日通知**：WorkManager 每天 08:00 拉取并推送当日头条摘要（Android 13+ 会请求通知权限）。
- **新闻源管理**：设置页可自由启用/停用新闻源，至少保留一个；修改后返回首页自动刷新。
- **深色模式**：Material 3 DayNight 主题，跟随系统；自适应启动图标。

## 技术栈

| 层 | 方案 |
| --- | --- |
| 语言 / UI | Kotlin 1.9.24、XML View、Material 3、ViewBinding |
| 架构 | Activity + MVVM（ViewModel + StateFlow）、Repository |
| 网络 | OkHttp 4.12（并发协程请求、按 XML 声明解码字符集） |
| 解析 | Android `XmlPullParser`（RSS 2.0 + Atom），流式解析，无第三方解析库 |
| 图片 | Coil 2.7 |
| 缓存 | 应用私有目录 `news_cache.json`（原子写入），最多 300 条 |
| 后台任务 | WorkManager 2.9（每日唯一周期任务） |
| 构建 | AGP 8.5.2、Gradle 8.7、JDK 17、compileSdk 34、minSdk 26 |

## 项目结构

```text
app/src/main/java/com/dailybrief/app/
├── DailyNewsApp.kt              # Application：通知渠道 + 调度每日任务
├── data/
│   ├── Article.kt               # 新闻实体
│   ├── NewsSource.kt            # 内置新闻源与分类
│   ├── FeedParser.kt            # RSS/Atom 流式解析、HTML 清洗、日期解析
│   ├── NewsRepository.kt        # 并发抓取、合并去重、缓存写入
│   ├── NewsCache.kt             # 本地 JSON 缓存
│   └── SettingsStore.kt         # 新闻源开关、通知开关
├── ui/
│   ├── MainActivity.kt          # 列表、搜索、分类、下拉刷新、状态渲染
│   ├── MainViewModel.kt         # 刷新/缓存/筛选状态
│   ├── ArticleAdapter.kt
│   ├── DetailActivity.kt        # 详情、分享、原文
│   ├── SettingsActivity.kt
│   └── SourceSettingsAdapter.kt
├── util/
│   ├── DateUtils.kt             # RFC 822 / ISO 8601 日期解析与相对时间
│   ├── HtmlUtils.kt             # HTML → 纯文本、图片提取、实体解码
│   ├── NotificationHelper.kt    # 每日摘要通知
│   └── DigestScheduler.kt       # 每天 08:00 的唯一周期任务
└── work/DailyDigestWorker.kt    # 后台刷新并推送通知
```

## 构建与运行

要求：JDK 17、Android SDK Platform 34、Build-Tools 34.0.0。

```bash
# 克隆后使用项目自带的 Gradle Wrapper
./gradlew assembleDebug

# 安装到已连接的设备 / 模拟器
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

或直接用 Android Studio（Ladybug 及以上）打开根目录运行 `app`。

`local.properties` 由 Android Studio 自动生成，已加入 `.gitignore`。

### 测试

```bash
./gradlew testDebugUnitTest
```

测试覆盖：

- RSS 2.0 / Atom 解析（CDATA、`media:thumbnail`、`content:encoded`、相对链接、缺少日期、坏条目）。
- 6 个真实新闻源的离线快照（`app/src/test/resources/feeds/`），验证真实 XML 结构与特殊日期格式。
- HTML 清洗、实体解码、图片提取。
- RFC 822（GMT / +0800）与 ISO 8601 日期解析、中文日期格式化。
- Robolectric 界面冒烟测试：设置页与详情页真实 inflate，验证主题、列表与数据绑定。

## 数据流

1. `MainViewModel` 启动时先读取本地缓存（如果有），再触发一次静默刷新。
2. `NewsRepository` 根据设置并发请求所有启用的 RSS，单源失败被隔离。
3. `FeedParser` 流式解析每个 `<item>` / `<entry>`，抽取标题、摘要、正文、配图、来源与时间。
4. 合并结果按链接、标题去重，按发布时间倒序，最多保留 300 条，并写入本地缓存。
5. `StateFlow` 驱动列表、分类/搜索筛选、更新时间、失败源数量和离线提示。

## 新增新闻源

编辑 `data/NewsSource.kt` 中的 `all` 列表即可：

```kotlin
NewsSource(
    id = "my-source",
    name = "我的新闻源",
    url = "https://example.com/rss.xml",
    category = NewsSources.CATEGORY_WORLD,
    homepage = "https://example.com"
)
```

分类新增时同步修改 `categories` 和首页 `ChipGroup`。

## 说明与限制

- 新闻内容、图片版权归原媒体所有，本应用仅做个人阅读聚合，详情页会展示来源并提供“阅读原文”。
- 部分源（如 Google 新闻）只提供标题与跳转链接，不提供摘要；界面会提示阅读原文。
- RSS 源可能调整地址或反爬策略，若某个源持续失败，可在设置中停用或更新 `NewsSource.url`。
- 当前 Debug 构建使用 Android 默认调试签名；正式发布前请配置自己的签名和 `versionCode`/`versionName`。
