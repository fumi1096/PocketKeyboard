param(
    [Parameter(Mandatory = $true)][string]$Source,
    [Parameter(Mandatory = $true)][string]$ResDir
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$img = [System.Drawing.Image]::FromFile($Source)

$densities = @(
    @{ Name = 'mdpi'; Scale = 1.0 },
    @{ Name = 'hdpi'; Scale = 1.5 },
    @{ Name = 'xhdpi'; Scale = 2.0 },
    @{ Name = 'xxhdpi'; Scale = 3.0 },
    @{ Name = 'xxxhdpi'; Scale = 4.0 }
)

function Save-Scaled {
    param([System.Drawing.Image]$Src, [int]$W, [int]$H, [string]$Path)
    $bmp = New-Object -TypeName System.Drawing.Bitmap -ArgumentList $W, $H
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.DrawImage($Src, 0, 0, $W, $H)
    $g.Dispose()
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function Save-Transparent {
    param([int]$W, [int]$H, [string]$Path)
    $bmp = New-Object -TypeName System.Drawing.Bitmap -ArgumentList $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.Dispose()
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

function Save-Round {
    param([System.Drawing.Image]$Src, [int]$W, [int]$H, [string]$Path)
    $bmp = New-Object -TypeName System.Drawing.Bitmap -ArgumentList $W, $H, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $p.AddEllipse(0, 0, $W - 1, $H - 1)
    $g.SetClip($p)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($Src, 0, 0, $W, $H)
    $g.Dispose()
    $p.Dispose()
    $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

foreach ($d in $densities) {
    $dir = Join-Path $ResDir ('mipmap-' + $d.Name)
    New-Item -ItemType Directory -Force -Path $dir | Out-Null

    $legacy = [int][math]::Round(48 * $d.Scale)
    Save-Scaled $img $legacy $legacy (Join-Path $dir 'ic_launcher.png')
    Save-Round $img $legacy $legacy (Join-Path $dir 'ic_launcher_round.png')

    # 自适应图标画布固定 108dp，内容由启动器按形状裁切
    $adapt = [int][math]::Round(108 * $d.Scale)
    Save-Scaled $img $adapt $adapt (Join-Path $dir 'ic_launcher_bg.png')
    Save-Transparent $adapt $adapt (Join-Path $dir 'ic_launcher_fg.png')
}

$img.Dispose()
Write-Host 'ICONS-OK'
