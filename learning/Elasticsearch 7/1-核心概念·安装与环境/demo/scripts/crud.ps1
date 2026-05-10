# 阶段 1 最小 CRUD + bulk 示例（PowerShell）
# 前置：docker compose up -d 且集群可访问 http://127.0.0.1:9200

$Base = "http://127.0.0.1:9200"
$Index = "learning_stage1_demo"

function Invoke-ES {
    param(
        [ValidateSet("Get", "Put", "Post", "Delete")] [string]$Method,
        [string]$Path,
        [string]$Body = $null
    )
    $uri = "$Base$Path"
    $params = @{ Uri = $uri; Method = $Method; ContentType = "application/json" }
    if ($null -ne $Body) {
        $params["Body"] = $Body
    }
    # 使用默认 UTF-8 发送 JSON
    Invoke-RestMethod @params
}

Write-Host "=== GET / (集群根信息) ===" -ForegroundColor Cyan
Invoke-ES -Method Get -Path "/"

Write-Host "`n=== GET /_cluster/health ===" -ForegroundColor Cyan
Invoke-ES -Method Get -Path "/_cluster/health"

Write-Host "`n=== PUT 创建索引 $Index ===" -ForegroundColor Cyan
Invoke-ES -Method Put -Path "/$Index" -Body '{}'

Write-Host "`n=== PUT 写入文档 _id=1 ===" -ForegroundColor Cyan
$doc1 = '{"title":"first doc","tags":["es","stage1"]}'
Invoke-ES -Method Put -Path "/$Index/_doc/1" -Body $doc1

Write-Host "`n=== GET 读取文档 1 ===" -ForegroundColor Cyan
Invoke-ES -Method Get -Path "/$Index/_doc/1"

Write-Host "`n=== POST _bulk 两条 ===" -ForegroundColor Cyan
# bulk 体：每两行一组（action 元数据 + 可选 source），行尾必须换行
# bulk 最后一行必须是换行结尾（NDJSON 约定）
$bulk = @"
{ ""index"": { ""_index"": ""$Index"", ""_id"": ""2"" } }
{ ""title"": ""bulk-a"", ""n"": 1 }
{ ""index"": { ""_index"": ""$Index"", ""_id"": ""3"" } }
{ ""title"": ""bulk-b"", ""n"": 2 }

"@
Invoke-RestMethod -Method Post -Uri "$Base/_bulk" -ContentType "application/x-ndjson" -Body $bulk

Write-Host "`n=== GET /_cat/indices?v ===" -ForegroundColor Cyan
Invoke-RestMethod -Method Get -Uri "$Base/_cat/indices?v"

Write-Host "`n=== DELETE 文档 3 ===" -ForegroundColor Cyan
Invoke-ES -Method Delete -Path "/$Index/_doc/3"

Write-Host "`n=== DELETE 索引 $Index（清理） ===" -ForegroundColor Cyan
Invoke-ES -Method Delete -Path "/$Index"

Write-Host "`n完成。若需保留索引做实验，可注释掉最后 DELETE 索引一步。" -ForegroundColor Green
