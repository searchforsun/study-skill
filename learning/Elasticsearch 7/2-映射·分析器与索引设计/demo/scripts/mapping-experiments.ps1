# ============================================================
# Stage 2 映射·分析器与索引设计 — 实验脚本 (PowerShell)
# 前置：docker-compose up -d（等待 healthcheck 通过）
# 运行：.\mapping-experiments.ps1
# ============================================================
param()

$ES = "http://localhost:9201"
$INDEX = "articles"

function Invoke-ES {
    param([string]$Method, [string]$Path, [string]$Body)
    $headers = @{ 'Content-Type' = 'application/json' }
    $uri = "$ES$Path"
    if ($Body) {
        Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers -Body $Body | ConvertTo-Json -Depth 10
    } else {
        Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers | ConvertTo-Json -Depth 10
    }
}

Write-Host "=== 1. 集群健康检查 ===" -ForegroundColor Cyan
Invoke-RestMethod -Uri "$ES/_cluster/health" | Select-Object cluster_name,status,number_of_nodes

Write-Host "`n=== 2. 用 _analyze 测试不同内置分析器 ===" -ForegroundColor Cyan
$TEXT = "The Quick Brown Fox jumps over the Lazy Dog!"

Write-Host "--- standard ---"
$body1 = @{ analyzer = "standard"; text = $TEXT } | ConvertTo-Json -Compress
(Invoke-RestMethod -Uri "$ES/_analyze" -Method Post -Headers @{'Content-Type'='application/json'} -Body $body1).tokens | Select-Object token

Write-Host "--- english ---"
$body2 = @{ analyzer = "english"; text = $TEXT } | ConvertTo-Json -Compress
(Invoke-RestMethod -Uri "$ES/_analyze" -Method Post -Headers @{'Content-Type'='application/json'} -Body $body2).tokens | Select-Object token

Write-Host "--- keyword ---"
$body3 = @{ analyzer = "keyword"; text = $TEXT } | ConvertTo-Json -Compress
(Invoke-RestMethod -Uri "$ES/_analyze" -Method Post -Headers @{'Content-Type'='application/json'} -Body $body3).tokens | Select-Object token

Write-Host "`n=== 3. 创建 articles 索引 ===" -ForegroundColor Cyan
$mapping = @{
  settings = @{
    number_of_shards = 1
    number_of_replicas = 0
    analysis = @{
      analyzer = @{
        my_article_analyzer = @{
          type = "custom"
          char_filter = @("html_strip")
          tokenizer = "standard"
          filter = @("lowercase", "stop")
        }
      }
    }
  }
  mappings = @{
    dynamic = "strict"
    properties = @{
      title = @{
        type = "text"
        analyzer = "my_article_analyzer"
        norms = $true
        copy_to = "full_search"
        fields = @{
          keyword = @{
            type = "keyword"
            ignore_above = 256
          }
        }
      }
      tags = @{
        type = "keyword"
        copy_to = "full_search"
      }
      summary = @{
        type = "text"
        analyzer = "my_article_analyzer"
        norms = $false
        copy_to = "full_search"
      }
      publish_time = @{
        type = "date"
        format = "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
      }
      author = @{
        type = "keyword"
      }
      full_search = @{
        type = "text"
        analyzer = "my_article_analyzer"
      }
    }
  }
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "$ES/$INDEX" -Method Put -Headers @{'Content-Type'='application/json'} -Body $mapping |
  Select-Object acknowledged,index

Write-Host "`n=== 4. 批量写入示例文档 ===" -ForegroundColor Cyan
$bulkData = @'
{ "index": { "_id": "1" } }
{ "title": "Elasticsearch Mapping 入门", "tags": ["elasticsearch", "mapping", "beginner"], "summary": "<p>Mapping is the <b>foundation</b> of search quality.</p>", "publish_time": "2026-05-01 10:00:00", "author": "san.zhang" }
{ "index": { "_id": "2" } }
{ "title": "深入理解 text 与 keyword", "tags": ["elasticsearch", "text", "keyword"], "summary": "<p>Choosing between <b>text</b> and keyword changes everything.</p>", "publish_time": "2026-05-03 14:30:00", "author": "si.li" }
{ "index": { "_id": "3" } }
{ "title": "自定义分析器实战", "tags": ["elasticsearch", "analyzer", "advanced"], "summary": "<p>When built-in analyzers are <i>not enough</i>, build your own.</p>", "publish_time": "2026-05-05 09:15:00", "author": "san.zhang" }
{ "index": { "_id": "4" } }
{ "title": "MySQL 索引优化指南", "tags": ["mysql", "database", "indexing"], "summary": "<p>B-Tree indexing strategies for <b>relational databases</b>.</p>", "publish_time": "2026-04-28 16:45:00", "author": "wu.wang" }
{ "index": { "_id": "5" } }
{ "title": "Elasticsearch vs Solr 选型对比", "tags": ["elasticsearch", "solr", "comparison"], "summary": "<p>A comprehensive <b>comparison</b> of search engines.</p>", "publish_time": "2026-05-08 11:00:00", "author": "si.li" }
'@
Invoke-RestMethod -Uri "$ES/$INDEX/_bulk" -Method Post -Headers @{'Content-Type'='application/json'} -Body $bulkData |
  Select-Object errors

Write-Host "`n=== 5. text 字段 match 查询 vs keyword 字段 term 查询 ===" -ForegroundColor Cyan
Write-Host "--- text match ---"
$q1 = @{ query = @{ match = @{ title = "elasticsearch" } }; _source = @("title", "author") } | ConvertTo-Json -Compress
(Invoke-RestMethod -Uri "$ES/$INDEX/_search" -Method Post -Headers @{'Content-Type'='application/json'} -Body $q1).hits.hits |
  ForEach-Object { $_.'_source' }

Write-Host "--- keyword term (精确匹配) ---"
$q2 = @{ query = @{ term = @{ "title.keyword" = "Elasticsearch Mapping 入门" } }; _source = @("title", "author") } | ConvertTo-Json -Compress
(Invoke-RestMethod -Uri "$ES/$INDEX/_search" -Method Post -Headers @{'Content-Type'='application/json'} -Body $q2).hits.hits |
  ForEach-Object { $_.'_source' }

Write-Host "`n=== 6. copy_to 跨字段搜索 ===" -ForegroundColor Cyan
$q3 = @{ query = @{ match = @{ full_search = "mapping" } }; _source = @("title", "tags") } | ConvertTo-Json -Compress
(Invoke-RestMethod -Uri "$ES/$INDEX/_search" -Method Post -Headers @{'Content-Type'='application/json'} -Body $q3).hits.hits |
  ForEach-Object { $_.'_source' }

Write-Host "`n=== 7. 按 author 聚合 ===" -ForegroundColor Cyan
$q4 = @{ size = 0; aggs = @{ by_author = @{ terms = @{ field = "author" } } } } | ConvertTo-Json -Depth 5
(Invoke-RestMethod -Uri "$ES/$INDEX/_search" -Method Post -Headers @{'Content-Type'='application/json'} -Body $q4).aggregations.by_author.buckets |
  Select-Object key, doc_count

Write-Host "`n=== 8. dynamic: strict 拒绝未定义字段 ===" -ForegroundColor Cyan
$badDoc = @{
  title = "测试文档"
  tags = @("test")
  summary = "test"
  publish_time = "2026-05-10 00:00:00"
  author = "test"
  unknown_field = "这不应该被接受"
} | ConvertTo-Json -Compress
try {
  Invoke-RestMethod -Uri "$ES/$INDEX/_doc" -Method Post -Headers @{'Content-Type'='application/json'} -Body $badDoc
} catch {
  Write-Host "拒绝成功：未知字段被 strict 模式拦截"
}

Write-Host "`n=== 实验完成 ===" -ForegroundColor Green
Write-Host "提示：完成后可改 mapping 中的某个参数（如 norms），对比搜索排序变化。"
