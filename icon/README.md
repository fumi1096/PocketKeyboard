# 图标素材说明

- `app_icon.jpg`：1024×1024 应用图标源图，由 1344×1792 的原插画**居中裁成正方形**得到（裁掉了底部生成水印）。
- `preview_circle.png` / `preview_round.png`：圆形/圆角预览，用圆形/圆角蒙版从 `app_icon.jpg` 生成。
- `app/src/main/res/mipmap-*/ic_launcher*.png`：多分辨率启动图标，由脚本生成。

## 重新生成图标

替换 `icon/app_icon.jpg`（建议 1024×1024 正方形）后执行：

```powershell
pwsh tools/make-icons.ps1 -Source icon/app_icon.jpg -ResDir app/src/main/res
```

会生成 mdpi～xxxhdpi 的 `ic_launcher.png`、`ic_launcher_round.png`、`ic_launcher_bg.png`（自适应图标背景）
与透明的 `ic_launcher_fg.png`。

圆形/圆角预览图目前是单独用 GDI+ 蒙版生成的，如需重新生成，可在 PowerShell 里用
`System.Drawing` 对 `app_icon.jpg` 分别做圆形/圆角裁剪后保存到 `icon/`。

## 版权提醒

该插画为 AI 生成素材（用户提供），**不包含在本仓库的 MIT 许可内**。
公开分发（GitHub Release、应用商店等）前，请确认你拥有该素材的分发授权；
否则建议替换为自己的或 CC0 素材再重新生成图标。
