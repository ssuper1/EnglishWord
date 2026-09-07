# WordMate 功能更新说明

## 已实现的功能

### 1. 底部导航栏架构
- ✅ 创建了 `MainActivityNew` 作为新的主 Activity
- ✅ 实现了双 Tab 切换：词库 / 分组
- ✅ 使用 Fragment 架构管理页面

### 2. Fragment 页面
- ✅ `WordListFragment` - 词库页面（原有功能迁移）
- ✅ `CustomGroupsFragment` - 自定义分组页面（待完善）

### 3. 词典导入功能
- ✅ `UniversalDictParser` - 多格式词典解析器，支持：
  - JSON 格式（每行一个对象）
  - 带音标格式：`word [phonetic] translation`
  - Tab 分隔格式：`word\ttranslation`
  - 空格分隔格式：`word translation`
- ✅ `SettingsDialogNew` - 设置对话框，包含"导入词典"按钮
- ✅ `ImportProgressDialog` - 导入进度对话框
- ✅ `DictWord` 数据模型

### 4. UI 资源
- ✅ 底部导航菜单
- ✅ 图标资源（书本、文件夹、添加按钮）
- ✅ 布局文件（Fragment、对话框）
- ✅ 示例词典文件

## 文件清单

### 新增 Kotlin 文件
```
app/src/main/java/com/example/englishword/
├── MainActivityNew.kt                      # 新主 Activity
├── fragment/
│   ├── WordListFragment.kt                # 词库 Fragment
│   └── CustomGroupsFragment.kt            # 分组 Fragment
├── dialog/
│   ├── SettingsDialogNew.kt              # 新设置对话框
│   └── ImportProgressDialog.kt           # 导入进度对话框
├── data/
│   └── UniversalDictParser.kt            # 词典解析器
└── model/
    └── DictWord.kt                        # 词典数据模型
```

### 新增 XML 资源文件
```
app/src/main/res/
├── layout/
│   ├── activity_main_new.xml             # 新主布局
│   ├── fragment_word_list.xml            # 词库 Fragment 布局
│   ├── fragment_custom_groups.xml        # 分组 Fragment 布局
│   ├── dialog_settings_new.xml           # 设置对话框布局
│   └── dialog_import_progress.xml        # 导入进度对话框布局
├── menu/
│   └── bottom_navigation_menu.xml        # 底部导航菜单
├── drawable/
│   ├── ic_book.xml                       # 书本图标
│   ├── ic_group.xml                      # 文件夹图标
│   └── ic_add.xml                        # 添加图标
└── color/
    └── bottom_nav_color.xml              # 导航栏颜色选择器
```

### 更新的文件
- `AndroidManifest.xml` - 注册了新 Activity
- `build.gradle.kts` - 添加了 Kotlin 和协程依赖
- `gradle/libs.versions.toml` - 添加了 Kotlin 插件配置
- `strings.xml` - 添加了导航栏文本

## 使用方法

### 切换到新界面
应用已配置为启动 `MainActivityNew`，底部有两个 Tab：
1. **词库** - 原有的单词列表功能
2. **分组** - 自定义分组功能（基础框架已搭建）

### 导入词典
1. 打开应用，点击右上角 ⚙ 设置按钮
2. 点击"📚 导入词典"按钮
3. 选择词典文件（支持 .txt 或 .json）
4. 等待导入完成

### 支持的词典格式示例

已在 `app/src/main/assets/sample_dict.txt` 中提供了示例。

## 待完善功能

### 短期任务
1. ❌ 实现词典数据持久化（SharedPreferences 或 Room 数据库）
2. ❌ 完善 SettingsDialog 的词典管理界面
3. ❌ 实现自定义分组的创建、编辑、删除功能
4. ❌ 实现分组适配器和列表展示

### 长期任务
1. ❌ 单词卡片播放界面
2. ❌ 自动播放功能
3. ❌ 学习进度追踪

## 技术说明

### 词典解析逻辑
`UniversalDictParser` 使用正则表达式自动识别文件格式：
- 检测 JSON：行首为 `{`
- 检测音标格式：包含 `[...]`
- 检测 Tab 分隔：包含 `\t`
- 检测空格分隔：单词后跟非字母字符

### 架构设计
- **MVP 模式**：Fragment 负责 UI，数据解析独立
- **协程**：异步导入词典，不阻塞主线程
- **Material Design 3**：使用 Material 组件库

## 构建说明

确保已安装：
- Android Studio Hedgehog 或更新版本
- Gradle 8.x
- Kotlin 1.9.0

运行：
```bash
./gradlew assembleDebug
```
