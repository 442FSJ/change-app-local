# 游标 · Youbiao

<p align="center"><img src="docs/youbiao/youbiao-icon.png" width="128" alt="游标图标"></p>

基于 [ZCShou/GoGoGo（影梭）](https://github.com/ZCShou/GoGoGo) 修改的 Android 模拟定位应用。保留地点搜索、地图选点、手动坐标、历史记录和悬浮摇杆等功能，加入个人签名配置及新版 Android 的兼容性修复。

当前版本：`1.12.3-youbiao.1`（1126）。支持 ARM64，最低 Android 8.1。

## 这个版本改了什么

- 应用名称改为“游标”，替换桌面、主题及通知图标。
- 使用自己的百度 Android AK 和 APK 签名，配置文件不进入 Git。
- 修复定位失败回调或迟到的首次定位覆盖用户选点的问题。
- 为地点搜索加入防抖、过时结果过滤和错误提示，无当前城市时也能搜索。
- 修复手动 WGS84 坐标的经纬度顺序、悬浮窗资源释放与服务空启动问题。
- compileSdk 升至 36，保留 targetSdk 32，并声明 Android 16 页大小兼容模式。
- 调整首次定位权限申请，关闭上游自动更新，保留个人版本的配置。

修改详情见 [PERSONAL_BUILD.txt](PERSONAL_BUILD.txt)。

## 本地构建

准备 JDK 17、Android SDK Platform 36 和 Build Tools 36.0.0。仓库包含 Gradle 8.13 Wrapper。

```sh
cp local.properties.example local.properties
cp signing.properties.example signing.properties
```

在 `local.properties` 中填写 Android SDK 路径、个人百度 Android AK，以及 `SHA1;com.zcshou.gogogo` 形式的安全码。

在 `signing.properties` 中填写自己的签名文件路径、别名及密码。签名私钥需自行保存；缺少签名配置时构建会明确报错。

**签名密码不要写入 `local.properties`**：地图密钥插件会将其中的属性生成到 APK 的 BuildConfig。签名配置必须保存在独立的 `signing.properties` 中。

```sh
./gradlew :app:assembleRelease
```

输出文件：

```text
app/build/outputs/apk/release/Youbiao_1.12.3-youbiao.1_arm64-v8a_release.apk
```

百度应用的包名为 `com.zcshou.gogogo`，后台 SHA1 必须与实际签名证书对应。仓库不包含个人 AK、签名私钥、签名密码或预先签好的 APK。

## 安装与使用

1. 安装自己的构建包并授予定位权限。
2. 在开发者选项中，将“选择模拟位置信息应用”设为“游标”。
3. 搜索或手动选择位置，点击左下角按钮开始/停止模拟定位。
4. 使用悬浮摇杆时授予悬浮窗权限。

如果旧版与新包的签名不同，无法直接覆盖安装。先备份需要保留的收藏、历史和设置，再卸载旧版安装新包。

## 验证范围

2026-10-12，实际发布构建在 Android 16 / API 36 / ARM64 / 16KB 页大小模拟器中通过了百度鉴权、地图底图、地点搜索、选点、系统模拟 GPS 与停止流程的验证。应用名和新图标也已检查。尚未在红米 K80 Pro 真机验证。

框架级联网测试入口为 `ReleaseSmokeInstrumentation`，鉴权诊断入口为 `BaiduAuthInstrumentation`。需要自行配置可用的百度 AK，在隔离模拟器中授权定位、通知、悬浮窗和模拟位置权限；具体命令见 [构建记录](PERSONAL_BUILD.txt)。

上游 GitHub Actions 配置存档在 [docs/upstream-workflows](docs/upstream-workflows)，本仓库暂未启用自动构建和发布。

## 来源与许可证

- 上游项目：[ZCShou/GoGoGo](https://github.com/ZCShou/GoGoGo)
- 基于上游提交：`de0d596190c57b8ca71481f60ce6b9e50af5107f`
- 原作者：ZCShou；保留原有版权和 [GPL-3.0-only 许可证](LICENSE)。
- 原项目说明：[中文](README.upstream.md) / [English](README.upstream.en.md)。
- 本次个人修改日期：2026-10-12；修改记录见 [PERSONAL_BUILD.txt](PERSONAL_BUILD.txt)。
- 百度 SDK 与在线服务另遵循其相应条款。
- 新图标由内置 image_gen 生成，完整提示词见 [icon-prompt.txt](docs/youbiao/icon-prompt.txt)。

分发修改版本时，应保留版权和许可声明，并按照 GPL 要求提供对应源码。
