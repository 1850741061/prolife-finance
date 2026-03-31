# Prolife Finance

Android 离线记账应用，基于 Jetpack Compose 构建。

## 功能

- **自动记账** — 通过通知监听识别微信/支付宝支付，弹出悬浮窗快速分类入账
- **手动记账** — 支出/收入分类管理，支持自定义备注
- **AA 分账** — 自动计算人均金额，支持自定义总人数和入账人数
- **数据统计** — 按周/月/年查看支出趋势图、分类占比、峰值分析
- **奶茶/咖啡追踪** — 独立饮品消费统计模块
- **数据洞察** — 智能生成消费洞察和建议
- **完全离线** — 所有数据存储在本地，无需注册和联网

## 技术栈

- Kotlin + Jetpack Compose
- Material Design 3
- SharedPreferences 本地存储
- NotificationListenerService（通知监听）
- AccessibilityService（悬浮窗覆盖）

## 构建

```bash
./gradlew assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/`

## 权限说明

| 权限 | 用途 |
|------|------|
| 通知访问权限 | 读取微信/支付宝支付通知 |
| 无障碍服务 | 在其他应用上方显示快捷记账悬浮窗 |
| 开机启动 | 恢复通知监听服务 |

## License

[MIT](LICENSE)
