# Niagara Launcher (Android 原生极简启动器)

本项目是一款高精度复刻 **Niagara Launcher** 经典交互与极简美学的 Android 原生桌面启动器（Home App）。

## 核心特性
1. **字母波浪滚动条 (Alphabet Wave Scrollbar)**：
   - 基于高斯/余弦物理形变曲线，随拇指滑动实时产生柔滑波浪凸起与字号放大动效。
   - 越过字母边界时触发触觉震动反馈（Haptic Tick）。
   - 浮动字母预览气泡（Indicator Pill）。
   - 支持左手/右手操作模式自由切换。
2. **极简首屏融合布局**：
   - 顶部 Niagara Glance：大字号轻量时钟、年月日与星期、电池电量指示、快速搜索按钮。
   - 常用收藏夹（Favorites）：快速置顶并随心管理。
   - A-Z 全局应用抽屉与分类索引。
3. **中文拼音与极速本地搜索**：
   - 毫秒级拼音首字母匹配（如 `wx` 快速定位「微信」、`zfb` 检索「支付宝」）。
   - 拼音全拼及中文模糊包含匹配。
   - 无本地匹配时一键直达系统默认浏览器网页搜索。
4. **长按应用管理弹窗 (App Actions ModalBottomSheet)**：
   - 设为收藏 / 取消收藏
   - 隐藏应用
   - 打开系统设置应用详情（App Info）
   - 一键呼出系统卸载流程
5. **系统桌面级集成**：
   - 注册 `CATEGORY_HOME` 与 `CATEGORY_DEFAULT`。
   - `android:windowShowWallpaper="true"` 支持系统动态/静态壁纸原生穿透。
   - 广播接收器监听应用安装、卸载、更新并实时刷新应用列表。

## 技术栈与工程规范
- **开发语言**: Kotlin 2.0+
- **UI 框架**: Jetpack Compose + Material 3 (Material You 动态壁纸取色)
- **依赖管理**: Gradle Version Catalog (`gradle/libs.versions.toml`)
- **状态与架构**: MVVM + Coroutines + StateFlow
- **本地持久化**: Jetpack Preferences DataStore
- **目标 SDK**: compileSdk 35 / minSdk 26 (Android 8.0 Oreo+)

## 编译与运行
在项目根目录下执行：

```bash
# 运行单元测试
./gradlew testDebugUnitTest

# 构建 Debug APK
./gradlew assembleDebug
```

生成的 APK 位于：
`app/build/outputs/apk/debug/app-debug.apk`
