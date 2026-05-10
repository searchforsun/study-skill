#!/usr/bin/env bash
# 阶段 1 最小 CRUD + bulk 示例（Bash：Git Bash / WSL / Linux / macOS）
set -euo pipefail

BASE="${ES_BASE:-http://127.0.0.1:9200}"
INDEX="${ES_INDEX:-learning_stage1_demo}"

echo "=== GET / (集群根信息) ==="
curl -sS "$BASE/" | jq .

echo -e "\n=== GET /_cluster/health ==="
curl -sS "$BASE/_cluster/health" | jq .

echo -e "\n=== PUT 创建索引 $INDEX ==="
curl -sS -X PUT "$BASE/$INDEX" -H 'Content-Type: application/json' -d '{}' | jq .

echo -e "\n=== PUT 写入文档 _id=1 ==="
curl -sS -X PUT "$BASE/$INDEX/_doc/1" -H 'Content-Type: application/json' \
  -d '{"title":"first doc","tags":["es","stage1"]}' | jq .

echo -e "\n=== GET 读取文档 1 ==="
curl -sS "$BASE/$INDEX/_doc/1" | jq .

echo -e "\n=== POST _bulk 两条 ==="
curl -sS -X POST "$BASE/_bulk" -H 'Content-Type: application/x-ndjson' --data-binary @- <<EOF | jq .
{"index":{"_index":"$INDEX","_id":"2"}}
{"title":"bulk-a","n":1}
{"index":{"_index":"$INDEX","_id":"3"}}
{"title":"bulk-b","n":2}
EOF

echo -e "\n=== GET /_cat/indices?v ==="
curl -sS "$BASE/_cat/indices?v"

echo -e "\n=== DELETE 文档 3 ==="
curl -sS -X DELETE "$BASE/$INDEX/_doc/3" | jq .

echo -e "\n=== DELETE 索引 $INDEX（清理） ==="
curl -sS -X DELETE "$BASE/$INDEX" | jq .

echo "完成。若需保留索引做实验，可注释掉最后 DELETE 索引一步。"
