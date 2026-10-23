#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'

function Login([string]$username, [string]$sysId) {
  $body = @{ username = $username; password = 'DemoOnly12345'; sysId = $sysId } | ConvertTo-Json
  return Invoke-RestMethod -Method Post -Uri "$base/api/v1/auth/sessions" -ContentType 'application/json' -Body $body
}

function Headers([string]$token) { return @{ Authorization = "Bearer $token" } }

$admin = Login 'platform_admin' '1'
$adminHeaders = Headers $admin.data.accessToken
$adminPerms = Invoke-RestMethod -Uri "$base/api/v1/me/permissions" -Headers $adminHeaders
$adminMenus = Invoke-RestMethod -Uri "$base/api/v1/me/menus" -Headers $adminHeaders

if ($adminPerms.data.permissionCodes -notcontains 'sys:role:grant') { throw '平台管理员缺少 sys:role:grant' }
if ($adminMenus.data.Count -ne 4) { throw "平台管理员菜单数异常: $($adminMenus.data.Count)" }

$reader = Login 'chenyu' '1'
$readerHeaders = Headers $reader.data.accessToken
$readerPerms = Invoke-RestMethod -Uri "$base/api/v1/me/permissions" -Headers $readerHeaders
if ($readerPerms.data.permissionCodes -notcontains 'sys:member:read') { throw '陈雨缺少 sys:member:read' }
if ($readerPerms.data.permissionCodes -contains 'sys:role:grant') { throw '陈雨不应拥有 sys:role:grant' }

$status403 = 0
try {
  Invoke-WebRequest -Uri "$base/api/v1/systems/1/roles" -Headers $readerHeaders -UseBasicParsing | Out-Null
} catch {
  $status403 = [int]$_.Exception.Response.StatusCode
}
if ($status403 -ne 403) { throw "统一鉴权未返回 403，实际 $status403" }

$status401 = 0
try {
  Invoke-WebRequest -Uri "$base/api/v1/me/permissions" -UseBasicParsing | Out-Null
} catch {
  $status401 = [int]$_.Exception.Response.StatusCode
}
if ($status401 -ne 401) { throw "未登录请求未返回 401，实际 $status401" }

$systemB = Login 'chenyu' '2'
$systemBPerms = Invoke-RestMethod -Uri "$base/api/v1/me/permissions" -Headers (Headers $systemB.data.accessToken)
if ($systemBPerms.data.sysId -ne '2') { throw '系统 B 上下文错误' }

Write-Host 'PASS 平台管理员权限与菜单' -ForegroundColor Green
Write-Host 'PASS 普通成员有效权限最小集' -ForegroundColor Green
Write-Host 'PASS 越权请求 403' -ForegroundColor Green
Write-Host 'PASS 未认证请求 401' -ForegroundColor Green
Write-Host 'PASS 系统 A/B 上下文隔离' -ForegroundColor Green
