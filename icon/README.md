# 图标素材说明

- `app_icon.jpg`：项目发起时使用的原始插画（用户提供）。
- `preview_circle.png` / `preview_round.png`：圆形/圆角预览，由 `tools/make-icons.ps1` 生成。
- 应用内的 `app/src/main/res/mipmap-*/ic_launcher*.png` 也由该脚本从 `app_icon.jpg` 生成。

## 版权提醒

该插画的版权归原作者所有，**不包含在本仓库的 MIT 许可内**。
如果要公开发布（GitHub Release、应用商店等），请先确认你拥有分发授权；
否则建议替换为自己的或 CC0 素材，然后重新运行：

```powershell
pwsh tools/make-icons.ps1
```

生成新的多分辨率图标。
