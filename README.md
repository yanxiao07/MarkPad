# MarkPad - Android Markdown Editor

MarkPad 是一款极简优雅的 Android Markdown 笔记应用，完全对标 Typora，支持实时渲染（WYSIWYG）、平板适配、LaTeX 公式及 Mermaid 图表。

## 核心特性
- **所见即所得 (WYSIWYG)**: 实时在编辑器中应用 Markdown 样式。
- **极简设计**: 基于 Material 3，无冗余装饰，专注写作。
- **大屏适配**: 完美适配平板与折叠屏，支持横屏分栏。
- **功能完整**: 支持表格、代码块、任务列表、LaTeX、Mermaid。
- **导出分享**: 支持导出为 PDF、HTML。
- **本地优先**: 使用 Room 存储元数据，支持本地文件系统。

## 技术栈
- **UI**: Jetpack Compose (Material 3)
- **架构**: MVVM + StateFlow
- **Markdown**: Flexmark-java
- **导出**: iText7 (html2pdf)
- **存储**: Room Database
- **自适应布局**: WindowSizeClass

## 测试说明
项目包含两类测试：
1. **单元测试 (Unit Tests)**: 位于 `app/src/test`。
   - `EditorViewModelTest`: 验证编辑器核心逻辑。
   - `ExportUtilsTest`: 验证 Markdown 解析与导出逻辑。
   - **运行方法**: 在 Android Studio 中右键点击 `java (test)` 目录选择 `Run 'All Tests'`，或执行 `./gradlew test`。

2. **UI 测试 (Instrumented Tests)**: 位于 `app/src/androidTest`。
   - 验证 Compose 组件在模拟器上的实际表现。
   - **运行方法**: 需要连接模拟器或真机，右键点击 `java (androidTest)` 选择 `Run 'All Tests'`。

## 平板适配说明
应用会自动检测屏幕宽度：
- **手机模式 (< 600dp)**: 使用侧边抽屉式文件管理器，编辑区全屏。
- **平板模式 (≥ 840dp)**: 开启固定侧边栏分栏模式，左侧显示文件列表，右侧为编辑区。
- **折叠屏**: 支持在展开态自动切换到分栏模式。

## 导出说明
点击顶部工具栏的导出按钮，可选择导出为 PDF 或 HTML。导出文件将保存在应用的外部缓存目录或用户选择的路径。
