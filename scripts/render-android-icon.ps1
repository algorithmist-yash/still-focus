Add-Type -AssemblyName System.Drawing
$taskOutput = Join-Path $PSScriptRoot '../android/branding/play-store-icon.png'
[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($taskOutput)) | Out-Null
$taskBitmap = [System.Drawing.Bitmap]::new(512, 512)
$taskGraphics = [System.Drawing.Graphics]::FromImage($taskBitmap)
$taskGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$taskGraphics.Clear([System.Drawing.ColorTranslator]::FromHtml('#18251E'))
$taskLime = [System.Drawing.ColorTranslator]::FromHtml('#D6EBAD')
$taskPen = [System.Drawing.Pen]::new($taskLime, 22)
$taskBrush = [System.Drawing.SolidBrush]::new($taskLime)
$taskGraphics.FillPie($taskBrush, 112, 112, 288, 288, 0, 180)
$taskGraphics.DrawEllipse($taskPen, 112, 112, 288, 288)
$taskBitmap.Save($taskOutput, [System.Drawing.Imaging.ImageFormat]::Png)
$taskBrush.Dispose(); $taskPen.Dispose(); $taskGraphics.Dispose(); $taskBitmap.Dispose()
Write-Output $taskOutput

# Play Store feature graphic, authored from the same vector geometry and palette.
$taskFeature = [System.Drawing.Bitmap]::new(1024,500)
$taskCanvas = [System.Drawing.Graphics]::FromImage($taskFeature)
$taskCanvas.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$taskCanvas.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$taskCanvas.Clear([System.Drawing.ColorTranslator]::FromHtml('#18251E'))
$taskFeatureBrush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#D6EBAD'))
$taskFeaturePen = [System.Drawing.Pen]::new($taskFeatureBrush,14)
$taskWhite = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#F5F7EF'))
$taskSoft = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#AABCA6'))
$taskTitle = [System.Drawing.Font]::new('Segoe UI',66,[System.Drawing.FontStyle]::Bold,[System.Drawing.GraphicsUnit]::Pixel)
$taskSubtitle = [System.Drawing.Font]::new('Segoe UI',30,[System.Drawing.FontStyle]::Regular,[System.Drawing.GraphicsUnit]::Pixel)
$taskSmall = [System.Drawing.Font]::new('Segoe UI',20,[System.Drawing.FontStyle]::Regular,[System.Drawing.GraphicsUnit]::Pixel)
$taskCanvas.FillPie($taskFeatureBrush,80,140,220,220,0,180)
$taskCanvas.DrawEllipse($taskFeaturePen,80,140,220,220)
$taskCanvas.DrawString('Still.', $taskTitle, $taskWhite, 370,128)
$taskCanvas.DrawString('Make space for focus.', $taskSubtitle, $taskFeatureBrush, 375,226)
$taskCanvas.DrawString('Focus timer  /  Stopwatch  /  Activity insights', $taskSmall, $taskSoft, 376,302)
$taskFeature.Save((Join-Path $PSScriptRoot '../android/branding/feature-graphic.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$taskFeaturePen.Dispose();$taskFeatureBrush.Dispose();$taskWhite.Dispose();$taskSoft.Dispose();$taskTitle.Dispose();$taskSubtitle.Dispose();$taskSmall.Dispose();$taskCanvas.Dispose();$taskFeature.Dispose()
