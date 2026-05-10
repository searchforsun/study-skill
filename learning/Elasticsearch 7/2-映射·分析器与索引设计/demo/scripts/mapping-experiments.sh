#!/usr/bin/env bash
# ============================================================
# Stage 2 映射·分析器与索引设计 — 实验脚本
# 前置：docker-compose up -d（等待 healthcheck 通过）
# 运行：bash mapping-experiments.sh
# ============================================================
set -euo pipefail

ES="http://localhost:9201"
INDEX="articles"
AUTH=""  # 学习环境无认证

echo "=== 1. 集群健康检查 ==="
curl -s ${AUTH} "${ES}/_cluster/health?pretty" | grep -E 'cluster_name|status|number_of_nodes'

echo ""
echo "=== 2. 用 _analyze 测试不同内置分析器 ==="
TEXT="The Quick Brown Fox jumps over the Lazy Dog!"

echo "--- standard ---"
curl -s ${AUTH} "${ES}/_analyze?pretty" -H 'Content-Type: application/json' -d "{
  \"analyzer\": \"standard\",
  \"text\": \"${TEXT}\"
}" | grep -E '"token"'

echo "--- english ---"
curl -s ${AUTH} "${ES}/_analyze?pretty" -H 'Content-Type: application/json' -d "{
  \"analyzer\": \"english\",
  \"text\": \"${TEXT}\"
}" | grep -E '"token"'

echo "--- keyword ---"
curl -s ${AUTH} "${ES}/_analyze?pretty" -H 'Content-Type: application/json' -d "{
  \"analyzer\": \"keyword\",
  \"text\": \"${TEXT}\"
}" | grep -E '"token"'

echo ""
echo "=== 3. 创建 articles 索引（显式 mapping + 自定义分析器 + copy_to） ==="
curl -s ${AUTH} -X PUT "${ES}/${INDEX}?pretty" -H 'Content-Type: application/json' -d '{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "my_article_analyzer": {
          "type": "custom",
          "char_filter": ["html_strip"],
          "tokenizer": "standard",
          "filter": ["lowercase", "stop"]
        }
      }
    }
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "my_article_analyzer",
        "norms": true,
        "copy_to": "full_search",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      },
      "tags": {
        "type": "keyword",
        "copy_to": "full_search"
      },
      "summary": {
        "type": "text",
        "analyzer": "my_article_analyzer",
        "norms": false,
        "copy_to": "full_search"
      },
      "publish_time": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
      },
      "author": {
        "type": "keyword"
      },
      "full_search": {
        "type": "text",
        "analyzer": "my_article_analyzer"
      }
    }
  }
}' | grep -E '"acknowledged"|"index"'

echo ""
echo "=== 4. 验证 mapping ==="
curl -s ${AUTH} "${ES}/${INDEX}/_mapping?pretty" | head -60

echo ""
echo "=== 5. 批量写入示例文档（含 HTML 标签——由自定义分析器清洗） ==="
curl -s ${AUTH} -X POST "${ES}/${INDEX}/_bulk?pretty" -H 'Content-Type: application/json' --data-binary '
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
' | grep -E '"errors"'

echo ""
echo "=== 6. text 字段 match 查询 vs keyword 字段 term 查询 ==="
echo "--- 6.1 对 title (text) 做 match 查询：搜索 'elasticsearch' ---"
curl -s ${AUTH} "${ES}/${INDEX}/_search?pretty" -H 'Content-Type: application/json' -d '{
  "query": { "match": { "title": "elasticsearch" } },
  "_source": ["title", "author"]
}' | grep -E '"title"'

echo ""
echo "--- 6.2 对 title.keyword (keyword) 做 term 查询：精确匹配 ---"
curl -s ${AUTH} "${ES}/${INDEX}/_search?pretty" -H 'Content-Type: application/json' -d '{
  "query": { "term": { "title.keyword": "Elasticsearch Mapping 入门" } },
  "_source": ["title", "author"]
}' | grep -E '"title"'

echo ""
echo "--- 6.3 对 tags (keyword) 做 term 查询：筛选标签 ---"
curl -s ${AUTH} "${ES}/${INDEX}/_search?pretty" -H 'Content-Type: application/json' -d '{
  "query": { "term": { "tags": "elasticsearch" } },
  "_source": ["title", "tags"]
}' | grep -E '"title"|"tags"'

echo ""
echo "=== 7. copy_to 跨字段搜索：搜 full_search 一个字段即可 ==="
curl -s ${AUTH} "${ES}/${INDEX}/_search?pretty" -H 'Content-Type: application/json' -d '{
  "query": { "match": { "full_search": "mapping" } },
  "_source": ["title", "summary", "tags"]
}' | grep -E '"title"|"tags"'

echo ""
echo "=== 8. 对比：multi_match（不依赖 copy_to）达到相似效果 ==="
curl -s ${AUTH} "${ES}/${INDEX}/_search?pretty" -H 'Content-Type: application/json' -d '{
  "query": {
    "multi_match": {
      "query": "mapping",
      "fields": ["title", "summary", "tags"]
    }
  },
  "_source": ["title", "summary"]
}' | grep -E '"title"'

echo ""
echo "=== 9. 按 author (keyword) 聚合：统计作者发文数 ==="
curl -s ${AUTH} "${ES}/${INDEX}/_search?pretty" -H 'Content-Type: application/json' -d '{
  "size": 0,
  "aggs": {
    "by_author": {
      "terms": { "field": "author" }
    }
  }
}' | grep -E '"key"|"doc_count"'

echo ""
echo "=== 10. 按 publish_time (date) 排序 ==="
curl -s ${AUTH} "${ES}/${INDEX}/_search?pretty" -H 'Content-Type: application/json' -d '{
  "query": { "match_all": {} },
  "sort": [
    { "publish_time": { "order": "desc" } }
  ],
  "_source": ["title", "publish_time"]
}' | grep -E '"title"|"publish_time"'

echo ""
echo "=== 11. 动态映射的反模式：尝试写入未定义的字段（dynamic: strict 拒绝） ==="
curl -s ${AUTH} -X POST "${ES}/${INDEX}/_doc?pretty" -H 'Content-Type: application/json' -d '{
  "title": "测试文档",
  "tags": ["test"],
  "summary": "test",
  "publish_time": "2026-05-10 00:00:00",
  "author": "test",
  "unknown_field": "这不应该被接受"
}' | grep -E '"type"|"reason"'

echo ""
echo "=== 12. 查看索引 doc 数量和占用 ==="
curl -s ${AUTH} "${ES}/_cat/indices/${INDEX}?v&h=index,docs.count,store.size,health"

echo ""
echo "=== 实验完成 ==="
echo "提示：完成后可改 mapping 中的某个参数（如 norms），对比搜索排序变化。"
