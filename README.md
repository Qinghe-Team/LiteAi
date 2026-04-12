# LiteAi

LiteAi 是一款面向小天才手表（320×360 屏幕）的 AI 对话应用基础工程，最低适配 Android 7.1，包名为 `com.qinghe.liteai`。

## 当前能力
- Material Design 3 风格的手表适配聊天主界面
- 对话历史（SQLite 持久化）
- 模型管理（支持 OpenAI / Claude / Gemini 兼容配置）
- 设置页（流式输出、浅色 / 深色模式，使用 SharedPreferences 保存）
- 关于页（软件名、作者、仓库地址、更新日志）
- GitHub Actions `build` / `release` 工作流

## 构建
```bash
./gradlew assembleDebug
```

## 版本
- versionName: `20260412`
- versionCode: `1`
