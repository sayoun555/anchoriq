#!/bin/bash
# ============================================
# AnchorIQ Elasticsearch Index Initialization
# Run after Elasticsearch is healthy
# ============================================

ES_URL="${ES_URL:-http://localhost:9200}"

echo "Waiting for Elasticsearch..."
until curl -sf "${ES_URL}/_cluster/health" > /dev/null 2>&1; do
  sleep 2
done
echo "Elasticsearch is ready."

# ILM Policies
echo "Creating ILM policies..."

curl -sf -X PUT "${ES_URL}/_ilm/policy/anchoriq-retention" -H 'Content-Type: application/json' -d '{
  "policy": {
    "phases": {
      "hot": { "actions": {} },
      "delete": {
        "min_age": "90d",
        "actions": { "delete": {} }
      }
    }
  }
}'

curl -sf -X PUT "${ES_URL}/_ilm/policy/anchoriq-retention-long" -H 'Content-Type: application/json' -d '{
  "policy": {
    "phases": {
      "hot": { "actions": {} },
      "delete": {
        "min_age": "180d",
        "actions": { "delete": {} }
      }
    }
  }
}'

# news index
echo "Creating news index..."
curl -sf -X PUT "${ES_URL}/news" -H 'Content-Type: application/json' -d '{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "index.lifecycle.name": "anchoriq-retention"
  },
  "mappings": {
    "properties": {
      "title":       { "type": "text" },
      "source":      { "type": "keyword" },
      "url":         { "type": "keyword" },
      "keywords":    { "type": "keyword" },
      "publishedAt": { "type": "date" },
      "createdAt":   { "type": "date" }
    }
  }
}'

# ai-decisions index
echo "Creating ai-decisions index..."
curl -sf -X PUT "${ES_URL}/ai-decisions" -H 'Content-Type: application/json' -d '{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "index.lifecycle.name": "anchoriq-retention-long"
  },
  "mappings": {
    "properties": {
      "type":              { "type": "keyword" },
      "riskLevel":         { "type": "keyword" },
      "vesselImo":         { "type": "keyword" },
      "vesselName":        { "type": "text" },
      "chokepoint":        { "type": "keyword" },
      "reason":            { "type": "text" },
      "recommendedAction": { "type": "text" },
      "aiConfidence":      { "type": "float" },
      "createdAt":         { "type": "date" }
    }
  }
}'

# anomaly-events index
echo "Creating anomaly-events index..."
curl -sf -X PUT "${ES_URL}/anomaly-events" -H 'Content-Type: application/json' -d '{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "index.lifecycle.name": "anchoriq-retention-long"
  },
  "mappings": {
    "properties": {
      "anomalyType":  { "type": "keyword" },
      "vesselImo":    { "type": "keyword" },
      "vesselName":   { "type": "text" },
      "lastPosition": { "type": "geo_point" },
      "description":  { "type": "text" },
      "detectedAt":   { "type": "date" }
    }
  }
}'

# geopolitical-events index
echo "Creating geopolitical-events index..."
curl -sf -X PUT "${ES_URL}/geopolitical-events" -H 'Content-Type: application/json' -d '{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "index.lifecycle.name": "anchoriq-retention"
  },
  "mappings": {
    "properties": {
      "eventType":    { "type": "keyword" },
      "region":       { "type": "keyword" },
      "location":     { "type": "geo_point" },
      "severity":     { "type": "keyword" },
      "description":  { "type": "text" },
      "source":       { "type": "keyword" },
      "timestamp":    { "type": "date" }
    }
  }
}'

echo "All indices created successfully."
