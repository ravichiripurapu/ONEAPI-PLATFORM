# Dashboard & Card Design - OneAPI Platform

**Version:** 1.0
**Date:** March 22, 2026
**Status:** Design Document (No Implementation Yet)

---

## Table of Contents

1. [Core Philosophy & Differentiation](#1-core-philosophy--differentiation)
2. [Card Type Taxonomy](#2-card-type-taxonomy)
3. [Card DSL - Complete Schema](#3-card-dsl---complete-schema)
4. [Query Shape Analysis Engine](#4-query-shape-analysis-engine)
5. [Recommendation Engine Design](#5-recommendation-engine-design)
6. [AI-Powered Recommendation Prompt](#6-ai-powered-recommendation-prompt)
7. [Service Architecture](#7-service-architecture)
8. [Dashboard Design](#8-dashboard-design)
9. [Unique Features (Our Spin)](#9-unique-features-our-spin)
10. [Example Card Definitions](#10-example-card-definitions)

---

## 1. Core Philosophy & Differentiation

### Our Unique Spin:
1. **Intent-First Design** - Users express what they want to know, not how to visualize it
2. **Smart Defaults with Override** - AI recommends, user approves/modifies
3. **Query-as-Data** - All queries are versionable, shareable, composable artifacts
4. **Governance-First** - Security, lineage, and certification baked in from day 1
5. **Semantic Layer Native** - Business terms, not SQL columns

### Key Differentiators from Metabase:
- **Enterprise-grade governance** - Built-in certification, lineage, RLS
- **AI-powered recommendations** - Not just rule-based, but intelligent ranking
- **Semantic layer integration** - Business glossary, metrics catalog
- **Version control** - Git-like versioning for cards and dashboards
- **Advanced caching** - Smart invalidation based on data changes
- **Mobile-first** - Responsive cards that adapt to device

---

## 2. Card Type Taxonomy

### Business-Facing Card Types (not chart types!)

```
📊 Analytics Cards (What users think about)
├── 📈 Insight Cards
│   ├── KPI Card           → Single metric tracking
│   ├── Trend Card         → Changes over time
│   ├── Comparison Card    → Side-by-side analysis
│   └── Forecast Card      → Predictive trends
│
├── 🔍 Discovery Cards
│   ├── Distribution Card  → How things break down
│   ├── Ranking Card       → Top/Bottom N
│   ├── Composition Card   → Part-to-whole
│   └── Correlation Card   → Relationship between metrics
│
├── 📋 Detail Cards
│   ├── Detail Table Card  → Row-level data
│   ├── Pivot Card         → Multi-dimensional view
│   └── Drillthrough Card  → Interactive exploration
│
└── 🎯 Operational Cards
    ├── Funnel Card        → Process flow
    ├── Cohort Card        → Retention/behavior
    ├── Alert Card         → Threshold monitoring
    └── Scorecard Card     → Multiple KPIs
```

### Visualization Types (Internal mapping)

```
🎨 Visualization Primitives
├── number             → Single value
├── gauge              → Progress to goal
├── table              → Tabular data
├── pivot              → Cross-tab
├── bar                → Category comparison
├── horizontal_bar     → Ranking
├── stacked_bar        → Composition comparison
├── grouped_bar        → Multi-metric comparison
├── line               → Time series
├── area               → Cumulative trend
├── multi_line         → Multiple trends
├── combo              → Line + Bar
├── pie                → Distribution (≤6 categories)
├── donut              → Distribution with total
├── treemap            → Hierarchical distribution
├── scatter            → Correlation
├── bubble             → 3-variable relationship
├── heatmap            → 2D density
├── funnel             → Stage conversion
├── waterfall          → Sequential contribution
├── map                → Geographic
└── sankey             → Flow between nodes
```

---

## 3. Card DSL - Complete Schema

### 3.1 Top-Level Card Structure

```json
{
  "card": {
    "id": "card_uuid_v4",
    "version": "2.1",
    "schemaVersion": "1.0",
    "metadata": {
      "type": "comparison",
      "subtype": "yoy",
      "tags": ["revenue", "sales", "finance"],
      "status": "published",
      "certified": true
    }
  },

  "identity": {
    "title": "Revenue Year-over-Year",
    "description": "Monthly revenue comparison: current year vs previous year",
    "owner": "user_123",
    "team": "finance_analytics",
    "createdAt": "2026-01-15T10:30:00Z",
    "modifiedAt": "2026-03-22T14:20:00Z",
    "createdBy": "john.doe@company.com",
    "lastModifiedBy": "jane.smith@company.com"
  },

  "dataset": {
    "connection": {
      "datasourceId": "oracle_prod_finance",
      "type": "oracle",
      "schema": "SALES",
      "catalog": null
    },
    "source": {
      "type": "table",
      "entity": "SALES_ORDER",
      "alias": "so",
      "businessName": "Sales Orders",
      "semanticModel": "sales_analytics_v2"
    }
  },

  "query": {
    "mode": "builder",
    "dialect": "oracle",
    "grain": "month",

    "dimensions": [
      {
        "field": "ORDER_DATE",
        "alias": "order_month",
        "role": "dimension",
        "dataType": "date",
        "semanticType": "temporal",
        "timeBucket": {
          "unit": "month",
          "format": "YYYY-MM"
        },
        "businessName": "Order Month"
      }
    ],

    "measures": [
      {
        "field": "REVENUE",
        "alias": "total_revenue",
        "role": "measure",
        "dataType": "decimal",
        "semanticType": "currency",
        "aggregation": "sum",
        "format": {
          "type": "currency",
          "currency": "USD",
          "decimals": 2
        },
        "businessName": "Total Revenue"
      }
    ],

    "filters": [
      {
        "id": "filter_1",
        "field": "ORDER_DATE",
        "operator": "between",
        "value": ["2024-01-01", "2026-12-31"],
        "dataType": "date",
        "required": true,
        "userEditable": false
      },
      {
        "id": "filter_2",
        "field": "STATUS",
        "operator": "in",
        "value": ["COMPLETED", "SHIPPED"],
        "dataType": "string",
        "required": false,
        "userEditable": true,
        "displayName": "Order Status"
      }
    ],

    "groupBy": [
      {
        "field": "ORDER_DATE",
        "timeBucket": "month",
        "order": 1
      }
    ],

    "sort": [
      {
        "field": "ORDER_DATE",
        "direction": "asc",
        "priority": 1
      }
    ],

    "limit": 1000,
    "offset": 0,

    "joins": [],

    "calculatedFields": [
      {
        "name": "revenue_prev_year",
        "expression": "LAG(SUM(REVENUE), 12) OVER (ORDER BY TRUNC(ORDER_DATE, 'MM'))",
        "dataType": "decimal",
        "businessName": "Previous Year Revenue"
      },
      {
        "name": "yoy_growth",
        "expression": "((total_revenue - revenue_prev_year) / NULLIF(revenue_prev_year, 0)) * 100",
        "dataType": "decimal",
        "format": {
          "type": "percent",
          "decimals": 1
        },
        "businessName": "YoY Growth %"
      }
    ],

    "parameters": [
      {
        "name": "fiscal_year",
        "type": "integer",
        "defaultValue": 2026,
        "userPrompt": "Select Fiscal Year",
        "validation": {
          "min": 2020,
          "max": 2030
        }
      }
    ],

    "having": [],

    "windowFunctions": [],

    "rawSQL": null,
    "sqlOverride": null
  },

  "visualization": {
    "type": "bar",
    "subtype": "grouped",

    "axes": {
      "x": {
        "field": "order_month",
        "label": "Month",
        "scale": "time",
        "format": "MMM YYYY",
        "rotation": 45
      },
      "y": [
        {
          "field": "total_revenue",
          "label": "Revenue (USD)",
          "scale": "linear",
          "format": "currency",
          "position": "left",
          "min": 0,
          "max": "auto"
        }
      ]
    },

    "series": [
      {
        "field": "total_revenue",
        "label": "Current Year",
        "type": "bar",
        "color": "#2563eb",
        "showLabels": false
      },
      {
        "field": "revenue_prev_year",
        "label": "Previous Year",
        "type": "bar",
        "color": "#94a3b8",
        "showLabels": false
      }
    ],

    "options": {
      "stacking": "none",
      "legend": {
        "show": true,
        "position": "top"
      },
      "tooltip": {
        "enabled": true,
        "shared": true
      },
      "gridLines": {
        "show": true,
        "color": "#e2e8f0"
      },
      "animation": true,
      "responsive": true,
      "colorPalette": "default"
    }
  },

  "recommendation": {
    "engine": "hybrid",
    "version": "1.2",

    "primary": {
      "cardType": "comparison",
      "visualizationType": "bar",
      "confidence": 0.94,
      "reasons": [
        "Time-bucketed monthly data detected",
        "YoY comparison intent from calculated fields",
        "Two series comparison (current vs previous)",
        "Moderate data points (24 months)"
      ],
      "metadata": {
        "ruleMatches": ["time_comparison", "dual_series"],
        "aiScore": 0.91,
        "userFeedback": null
      }
    },

    "alternatives": [
      {
        "visualizationType": "line",
        "confidence": 0.82,
        "reason": "Good for trend visibility",
        "whenToUse": "If focus shifts to continuous trend rather than period comparison"
      },
      {
        "visualizationType": "combo",
        "confidence": 0.76,
        "reason": "Can show YoY % as line overlay",
        "whenToUse": "To highlight growth rate alongside absolute values"
      },
      {
        "visualizationType": "table",
        "confidence": 0.45,
        "reason": "Fallback for detailed numbers",
        "whenToUse": "For exact value analysis or data export"
      }
    ],

    "warnings": [],

    "improvementSuggestions": [
      "Consider adding sparklines for quick trend indication",
      "Add drill-down to daily granularity for anomaly investigation"
    ]
  },

  "governance": {
    "ownership": {
      "owner": "analytics_team",
      "steward": "finance_team",
      "contacts": ["analytics@company.com"]
    },

    "certification": {
      "certified": true,
      "certifiedBy": "data_governance_board",
      "certifiedAt": "2026-02-15T10:00:00Z",
      "expiresAt": "2026-08-15T10:00:00Z",
      "certificationLevel": "gold"
    },

    "security": {
      "visibility": "team",
      "rowLevelSecurity": {
        "enabled": true,
        "policyId": "finance_region_policy",
        "filterExpression": "REGION = :user_region"
      },
      "columnLevelSecurity": {
        "enabled": false,
        "maskedFields": []
      },
      "permittedRoles": ["ROLE_FINANCE", "ROLE_ADMIN"]
    },

    "compliance": {
      "dataClassification": "internal",
      "retentionPeriod": "7_years",
      "piiFields": [],
      "gdprCompliant": true
    },

    "lineage": {
      "enabled": true,
      "upstreamDependencies": [
        {
          "type": "table",
          "name": "SALES.SALES_ORDER",
          "columns": ["ORDER_DATE", "REVENUE", "STATUS"]
        }
      ],
      "downstreamConsumers": [
        {
          "type": "dashboard",
          "id": "dashboard_executive_summary",
          "name": "Executive Dashboard"
        }
      ]
    },

    "tags": ["revenue", "yoy", "finance", "certified"],

    "metadata": {
      "businessGlossary": {
        "revenue": "glossary_term_123",
        "fiscal_year": "glossary_term_456"
      },
      "dataQuality": {
        "freshnessCheck": true,
        "lastDataUpdate": "2026-03-22T02:00:00Z",
        "qualityScore": 0.98
      }
    }
  },

  "execution": {
    "caching": {
      "enabled": true,
      "ttl": 3600,
      "strategy": "query_hash",
      "refreshSchedule": "0 2 * * *"
    },

    "performance": {
      "maxExecutionTime": 30000,
      "sampleSize": null,
      "approximateResults": false
    },

    "history": {
      "avgExecutionTime": 2340,
      "lastExecutionTime": 2180,
      "executionCount": 127,
      "lastExecutedAt": "2026-03-22T14:15:00Z",
      "lastExecutedBy": "user_456"
    }
  },

  "interactivity": {
    "drillDown": {
      "enabled": true,
      "levels": [
        {
          "dimension": "ORDER_DATE",
          "granularity": "day"
        },
        {
          "dimension": "PRODUCT_CATEGORY"
        }
      ]
    },

    "crossFilter": {
      "enabled": true,
      "targetCards": ["card_regional_breakdown", "card_product_mix"]
    },

    "export": {
      "formats": ["csv", "xlsx", "pdf"],
      "includeVisualization": true
    }
  }
}
```

---

## 4. Query Shape Analysis Engine

### 4.1 Query Shape Fingerprint

```json
{
  "queryFingerprint": {
    "dimensions": {
      "count": 1,
      "types": {
        "temporal": 1,
        "categorical": 0,
        "geographic": 0,
        "hierarchical": 0
      },
      "hasPrimaryTime": true,
      "hasSecondaryDimension": false
    },

    "measures": {
      "count": 1,
      "aggregations": ["sum"],
      "semanticTypes": ["currency"],
      "hasRatio": false,
      "hasPercent": false,
      "hasCount": false
    },

    "characteristics": {
      "hasGroupBy": true,
      "hasTimeBucket": true,
      "hasFilters": true,
      "hasJoins": false,
      "hasCalculatedFields": true,
      "hasWindowFunctions": true,
      "hasSubqueries": false
    },

    "complexity": {
      "score": 6.5,
      "level": "moderate",
      "factors": {
        "joinComplexity": 0,
        "filterComplexity": 2,
        "calculationComplexity": 4,
        "aggregationComplexity": 3
      }
    },

    "intent": {
      "primary": "comparison",
      "secondary": "trend",
      "keywords": ["yoy", "monthly", "comparison"],
      "confidence": 0.92
    }
  }
}
```

### 4.2 Result Profile (after preview execution)

```json
{
  "resultProfile": {
    "rowCount": 24,
    "columnCount": 4,

    "dimensions": [
      {
        "field": "order_month",
        "distinctCount": 24,
        "nullCount": 0,
        "nullRate": 0.0,
        "cardinality": "moderate",
        "dataType": "date",
        "isContinuous": true,
        "hasGaps": false,
        "range": {
          "min": "2025-01-01",
          "max": "2026-12-31"
        }
      }
    ],

    "measures": [
      {
        "field": "total_revenue",
        "min": 45000.00,
        "max": 280000.00,
        "avg": 165000.00,
        "median": 158000.00,
        "stdDev": 42000.00,
        "nullCount": 0,
        "outlierCount": 2,
        "distribution": "normal"
      }
    ],

    "dataQuality": {
      "completeness": 1.0,
      "consistency": 0.98,
      "freshness": 0.95,
      "accuracy": 0.97
    },

    "visualizationHints": {
      "suitableForPie": false,
      "suitableForLine": true,
      "suitableForBar": true,
      "suitableForScatter": false,
      "categoryCountOK": true,
      "timeSeriesOK": true
    }
  }
}
```

---

## 5. Recommendation Engine Design

### 5.1 Multi-Layer Recommendation Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     User Intent Layer                        │
│  Natural language → Intent classification → Card type       │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│              Rule-Based Recommendation Engine                │
│  Query shape + Result profile → Deterministic rules         │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│               AI Ranking & Validation Layer                  │
│  LLM scores alternatives + Explains reasoning                │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│              Guardrail & Quality Checks                      │
│  Validate feasibility + Performance impact                   │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│              Personalization Layer (Future)                  │
│  User preferences + Past choices → Adjust ranking           │
└──────────────────────────────────────────────────────────────┘
```

### 5.2 Deterministic Rules Matrix

```json
{
  "recommendationRules": [
    {
      "ruleId": "R001",
      "name": "Single KPI",
      "conditions": {
        "dimensionCount": 0,
        "measureCount": 1,
        "expectedRows": 1,
        "hasAggregation": true
      },
      "recommendation": {
        "cardType": "kpi",
        "visualization": "number",
        "confidence": 0.99
      }
    },

    {
      "ruleId": "R002",
      "name": "Time Series Trend",
      "conditions": {
        "hasPrimaryTimeDimension": true,
        "measureCount": 1,
        "groupByCount": 1,
        "expectedRows": ">10"
      },
      "recommendation": {
        "cardType": "trend",
        "visualization": "line",
        "confidence": 0.95
      }
    },

    {
      "ruleId": "R003",
      "name": "Small Category Distribution",
      "conditions": {
        "hasCategoricalDimension": true,
        "categoryCount": "<=6",
        "measureCount": 1,
        "groupByCount": 1
      },
      "recommendation": {
        "cardType": "distribution",
        "visualization": "pie",
        "confidence": 0.88
      }
    },

    {
      "ruleId": "R004",
      "name": "Large Category Comparison",
      "conditions": {
        "hasCategoricalDimension": true,
        "categoryCount": ">6",
        "measureCount": 1,
        "groupByCount": 1
      },
      "recommendation": {
        "cardType": "comparison",
        "visualization": "bar",
        "confidence": 0.92
      }
    },

    {
      "ruleId": "R005",
      "name": "YoY Comparison",
      "conditions": {
        "intent": "yoy_comparison",
        "hasTimeDimension": true,
        "hasCalculatedField": true,
        "seriesCount": ">=2"
      },
      "recommendation": {
        "cardType": "comparison",
        "visualization": "bar",
        "subtype": "grouped",
        "confidence": 0.94
      }
    },

    {
      "ruleId": "R006",
      "name": "Multi-Metric Time Series",
      "conditions": {
        "hasTimeDimension": true,
        "measureCount": ">=2",
        "seriesCount": "<=4"
      },
      "recommendation": {
        "cardType": "trend",
        "visualization": "multi_line",
        "confidence": 0.87
      }
    },

    {
      "ruleId": "R007",
      "name": "Top N Ranking",
      "conditions": {
        "hasSort": true,
        "hasLimit": true,
        "limitValue": "<=20",
        "intent": "ranking"
      },
      "recommendation": {
        "cardType": "ranking",
        "visualization": "horizontal_bar",
        "confidence": 0.91
      }
    },

    {
      "ruleId": "R008",
      "name": "Detail Exploration",
      "conditions": {
        "hasGroupBy": false,
        "rowCount": ">100"
      },
      "recommendation": {
        "cardType": "detail",
        "visualization": "table",
        "confidence": 0.85
      }
    },

    {
      "ruleId": "R009",
      "name": "Funnel Analysis",
      "conditions": {
        "intent": "funnel",
        "hasOrderedStages": true,
        "measureCount": 1
      },
      "recommendation": {
        "cardType": "funnel",
        "visualization": "funnel",
        "confidence": 0.96
      }
    },

    {
      "ruleId": "R010",
      "name": "Geographic Distribution",
      "conditions": {
        "hasGeographicDimension": true,
        "measureCount": 1
      },
      "recommendation": {
        "cardType": "distribution",
        "visualization": "map",
        "confidence": 0.89
      }
    }
  ]
}
```

### 5.3 Guardrail Rules

```json
{
  "guardrails": [
    {
      "id": "G001",
      "rule": "Never recommend pie chart with >7 categories",
      "action": "fallback_to_bar",
      "severity": "error"
    },
    {
      "id": "G002",
      "rule": "Never recommend line chart for unordered categorical X-axis",
      "action": "suggest_bar_instead",
      "severity": "error"
    },
    {
      "id": "G003",
      "rule": "Never recommend KPI card if result has multiple rows",
      "action": "suggest_table_or_aggregation",
      "severity": "error"
    },
    {
      "id": "G004",
      "rule": "Warn if stacked chart has >5 series",
      "action": "suggest_grouped_or_multi_line",
      "severity": "warning"
    },
    {
      "id": "G005",
      "rule": "Warn if bar chart has >50 categories",
      "action": "suggest_top_n_or_table",
      "severity": "warning"
    },
    {
      "id": "G006",
      "rule": "Never recommend visualization if query exceeds 60s timeout",
      "action": "suggest_optimize_or_sample",
      "severity": "error"
    },
    {
      "id": "G007",
      "rule": "Fallback to table when uncertain (confidence <0.5)",
      "action": "use_table_as_safe_default",
      "severity": "info"
    },
    {
      "id": "G008",
      "rule": "Warn if using unsupported visualization for mobile",
      "action": "suggest_mobile_friendly_alternative",
      "severity": "warning"
    }
  ]
}
```

---

## 6. AI-Powered Recommendation Prompt

### 6.1 Structured Prompt for LLM

```
System Role:
You are an expert data visualization consultant for an enterprise analytics platform.
Your job is to recommend the best visualization and explain why, based on:
1. User intent
2. Query structure
3. Data profile
4. Analytical best practices
5. Cognitive load principles

Context:
{
  "intent": "{{user_intent}}",
  "queryShape": {{query_fingerprint_json}},
  "resultProfile": {{result_profile_json}},
  "candidateVisualizations": {{candidate_list}},
  "userContext": {
    "persona": "{{business_user|analyst|executive}}",
    "device": "{{desktop|tablet|mobile}}",
    "experience": "{{novice|intermediate|expert}}"
  }
}

Rules:
1. Prefer simplicity over complexity
2. Match visualization to analytical intent
3. Line charts only for time series or ordered continuous data
4. Pie charts only for part-to-whole with ≤6 categories
5. Bar charts for comparisons across categories
6. Number cards for single KPIs
7. Tables for detail exploration or when visualization would obscure data
8. Never use misleading scales or truncated axes

Response Format (JSON):
{
  "primaryRecommendation": {
    "cardType": "",
    "visualizationType": "",
    "confidence": 0.0,
    "reasons": [],
    "configurationHints": {}
  },
  "alternatives": [
    {
      "visualizationType": "",
      "confidence": 0.0,
      "reason": "",
      "whenToUse": ""
    }
  ],
  "warnings": [],
  "improvements": []
}

Task:
Analyze the provided context and recommend the best visualization.
```

---

## 7. Service Architecture

### 7.1 Microservices Breakdown

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway                             │
│              (Authentication, Rate Limiting)                 │
└──────┬───────────────────────────────────────────────┬──────┘
       │                                               │
┌──────▼──────────┐                          ┌────────▼────────┐
│  Card Service   │                          │ Dashboard Service│
│  - CRUD         │                          │ - Layout        │
│  - Versioning   │                          │ - Organization  │
│  - Sharing      │                          │ - Filters       │
└──────┬──────────┘                          └────────┬────────┘
       │                                               │
┌──────▼───────────────────────────────────────────────▼──────┐
│              Query Builder Service                           │
│  - UI → DSL transformation                                   │
│  - Query validation                                          │
│  - Parameter substitution                                    │
└──────┬──────────────────────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────────────────────┐
│              Query Compiler Service                          │
│  - DSL → SQL/jOOQ                                           │
│  - Dialect-specific optimization                             │
│  - Query plan analysis                                       │
└──────┬──────────────────────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────────────────────┐
│          Query Execution Service                             │
│  - Connection pooling                                        │
│  - Streaming results                                         │
│  - Caching                                                   │
│  - Timeout management                                        │
└──────┬──────────────────────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────────────────────┐
│         Metadata & Profiling Service                         │
│  - Schema discovery                                          │
│  - Semantic layer                                            │
│  - Result profiling                                          │
│  - Data quality checks                                       │
└──────┬──────────────────────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────────────────────┐
│      Recommendation Engine Service                           │
│  - Rule matching                                             │
│  - AI ranking (OpenAI/Gemini)                               │
│  - Guardrail validation                                      │
│  - Explainability                                            │
└──────┬──────────────────────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────────────────────┐
│           Governance Service                                 │
│  - RLS enforcement                                           │
│  - Certification workflow                                    │
│  - Lineage tracking                                          │
│  - Audit logging                                             │
└──────────────────────────────────────────────────────────────┘
```

### 7.2 API Contracts

#### **POST /api/v1/cards/recommend**
```json
Request:
{
  "intent": "Show me revenue trends by month",
  "dataset": {
    "datasourceId": "oracle_prod",
    "entity": "SALES_ORDER"
  },
  "selectedFields": [
    {"name": "ORDER_DATE", "role": "dimension"},
    {"name": "REVENUE", "role": "measure", "aggregation": "sum"}
  ],
  "groupBy": [{"field": "ORDER_DATE", "timeBucket": "month"}],
  "userContext": {
    "persona": "business_user",
    "device": "desktop"
  }
}

Response:
{
  "cardType": "trend",
  "visualizationType": "line",
  "confidence": 0.93,
  "reasons": [
    "Time series detected with monthly granularity",
    "Single measure over continuous time",
    "Intent keyword 'trends' suggests line chart"
  ],
  "alternatives": [
    {"type": "bar", "confidence": 0.78, "reason": "Good for period comparisons"}
  ],
  "suggestedQuery": {...},
  "suggestedVisualization": {...}
}
```

#### **POST /api/v1/cards**
```json
Request:
{
  "card": {...full card DSL...}
}

Response:
{
  "cardId": "card_uuid",
  "version": "1.0",
  "status": "created",
  "url": "/cards/card_uuid"
}
```

#### **POST /api/v1/cards/{cardId}/execute**
```json
Request:
{
  "parameters": {
    "fiscal_year": 2026
  },
  "limit": 100,
  "preview": false
}

Response:
{
  "data": [...],
  "metadata": {
    "rowCount": 24,
    "columnCount": 4,
    "executionTime": 234,
    "cached": false
  },
  "visualization": {...}
}
```

#### **GET /api/v1/cards/{cardId}/preview**
```
Returns first 10 rows + result profile
```

#### **POST /api/v1/query/profile**
```json
Request:
{
  "datasourceId": "oracle_prod",
  "query": {...DSL...}
}

Response:
{
  "queryFingerprint": {...},
  "estimatedRows": 24,
  "estimatedExecutionTime": 2000,
  "warnings": []
}
```

---

## 8. Dashboard Design

### 8.1 Dashboard Structure

```json
{
  "dashboard": {
    "id": "dash_uuid",
    "version": "1.0",

    "identity": {
      "title": "Executive Sales Dashboard",
      "description": "High-level sales metrics and KPIs",
      "owner": "user_123",
      "team": "executive_team"
    },

    "layout": {
      "type": "grid",
      "columns": 12,
      "rowHeight": 80,

      "cards": [
        {
          "cardId": "card_total_revenue",
          "position": {
            "x": 0,
            "y": 0,
            "w": 3,
            "h": 2
          },
          "title": "Total Revenue",
          "refreshInterval": 300
        },
        {
          "cardId": "card_revenue_trend",
          "position": {
            "x": 3,
            "y": 0,
            "w": 9,
            "h": 4
          },
          "title": "Revenue Trend",
          "refreshInterval": 600
        },
        {
          "cardId": "card_top_products",
          "position": {
            "x": 0,
            "y": 4,
            "w": 6,
            "h": 4
          },
          "title": "Top 10 Products",
          "refreshInterval": 900
        }
      ]
    },

    "globalFilters": [
      {
        "id": "filter_date_range",
        "type": "date_range",
        "field": "ORDER_DATE",
        "defaultValue": "last_30_days",
        "appliesToCards": ["card_revenue_trend", "card_top_products"]
      },
      {
        "id": "filter_region",
        "type": "multi_select",
        "field": "REGION",
        "options": ["US", "EMEA", "APAC"],
        "defaultValue": ["US"],
        "appliesToCards": "*"
      }
    ],

    "interactivity": {
      "crossFiltering": {
        "enabled": true,
        "mode": "click"
      },
      "drillDown": {
        "enabled": true
      }
    },

    "scheduling": {
      "autoRefresh": true,
      "interval": 300,
      "emailSubscription": {
        "enabled": true,
        "frequency": "daily",
        "recipients": ["exec@company.com"]
      }
    },

    "governance": {
      "certified": true,
      "visibility": "team",
      "permittedRoles": ["ROLE_EXECUTIVE", "ROLE_ADMIN"]
    }
  }
}
```

---

## 9. Unique Features (Our Spin)

### 9.1 Smart Query Builder with Intent Detection
- User types natural language → System detects intent → Suggests card type + fields
- "Show me top 10 customers by revenue" → Ranking Card + pre-filled query

### 9.2 Query Lineage & Impact Analysis
- Track which dashboards/reports use a card
- Show data lineage from source table to card
- Impact analysis before modifying shared queries

### 9.3 Certification Workflow
- Draft → Review → Certified → Published
- Expiration dates for certifications
- Quality scores based on data freshness, accuracy

### 9.4 Semantic Layer Integration
- Business terms automatically mapped
- Glossary terms linked to cards
- Metrics defined once, reused everywhere

### 9.5 Version Control for Cards
- Git-like versioning
- Diff between versions
- Rollback capability
- Fork and modify certified cards

### 9.6 Smart Caching with Invalidation
- Query hash-based caching
- Automatic invalidation on source data changes
- Partial cache hits for similar queries

### 9.7 AI-Powered Anomaly Detection
- Automatically detect unusual patterns in results
- Flag outliers in visualizations
- Suggest drill-down paths for investigation

### 9.8 Mobile-First Responsive Cards
- Automatically switch visualization types for mobile
- Progressive disclosure of details
- Touch-optimized interactions

### 9.9 Collaborative Features
- Comments on cards
- Share with context (filters applied)
- Suggested improvements from community

### 9.10 Performance Budgets
- Set max execution time per card
- Automatic query optimization suggestions
- Sample-based previews for large datasets

---

## 10. Example Card Definitions

### 10.1 KPI Card
```json
{
  "cardType": "kpi",
  "title": "Total Orders This Month",
  "query": {
    "measures": [
      {"field": "ORDER_ID", "aggregation": "count"}
    ],
    "filters": [
      {"field": "ORDER_DATE", "operator": "current_month"}
    ]
  },
  "visualization": {
    "type": "number",
    "format": "integer",
    "comparison": {
      "previousPeriod": true,
      "showChange": true,
      "showPercentChange": true
    }
  }
}
```

### 10.2 Trend Card
```json
{
  "cardType": "trend",
  "title": "Daily Revenue Last 90 Days",
  "query": {
    "dimensions": [
      {"field": "ORDER_DATE", "timeBucket": "day"}
    ],
    "measures": [
      {"field": "REVENUE", "aggregation": "sum"}
    ],
    "filters": [
      {"field": "ORDER_DATE", "operator": "last_n_days", "value": 90}
    ]
  },
  "visualization": {
    "type": "area",
    "xAxis": {"field": "ORDER_DATE"},
    "yAxis": [{"field": "REVENUE", "format": "currency"}],
    "trendLine": true,
    "annotations": []
  }
}
```

### 10.3 Distribution Card
```json
{
  "cardType": "distribution",
  "title": "Revenue by Product Category",
  "query": {
    "dimensions": [
      {"field": "PRODUCT_CATEGORY"}
    ],
    "measures": [
      {"field": "REVENUE", "aggregation": "sum"}
    ],
    "groupBy": [{"field": "PRODUCT_CATEGORY"}],
    "sort": [{"field": "REVENUE", "direction": "desc"}]
  },
  "visualization": {
    "type": "donut",
    "showPercentages": true,
    "showTotal": true
  }
}
```

### 10.4 Ranking Card
```json
{
  "cardType": "ranking",
  "title": "Top 10 Customers by Lifetime Value",
  "query": {
    "dimensions": [
      {"field": "CUSTOMER_NAME"}
    ],
    "measures": [
      {"field": "REVENUE", "aggregation": "sum"}
    ],
    "groupBy": [{"field": "CUSTOMER_NAME"}],
    "sort": [{"field": "REVENUE", "direction": "desc"}],
    "limit": 10
  },
  "visualization": {
    "type": "horizontal_bar",
    "showLabels": true,
    "colorByValue": true
  }
}
```

### 10.5 Comparison Card (YoY)
```json
{
  "cardType": "comparison",
  "subtype": "yoy",
  "title": "Revenue Year-over-Year",
  "query": {
    "dimensions": [
      {"field": "ORDER_DATE", "timeBucket": "month"}
    ],
    "measures": [
      {"field": "REVENUE", "aggregation": "sum"}
    ],
    "calculatedFields": [
      {
        "name": "revenue_prev_year",
        "expression": "LAG(SUM(REVENUE), 12)"
      },
      {
        "name": "yoy_growth",
        "expression": "((revenue - revenue_prev_year) / revenue_prev_year) * 100"
      }
    ]
  },
  "visualization": {
    "type": "bar",
    "subtype": "grouped",
    "series": [
      {"field": "revenue", "label": "Current Year"},
      {"field": "revenue_prev_year", "label": "Previous Year"}
    ]
  }
}
```

### 10.6 Funnel Card
```json
{
  "cardType": "funnel",
  "title": "Sales Funnel - Lead to Close",
  "query": {
    "dimensions": [
      {"field": "STAGE"}
    ],
    "measures": [
      {"field": "OPPORTUNITY_ID", "aggregation": "count"}
    ],
    "filters": [
      {"field": "CREATED_DATE", "operator": "last_quarter"}
    ],
    "sort": [
      {"field": "STAGE_ORDER", "direction": "asc"}
    ]
  },
  "visualization": {
    "type": "funnel",
    "showConversionRates": true,
    "orientation": "vertical"
  }
}
```

### 10.7 Pivot Card
```json
{
  "cardType": "detail",
  "subtype": "pivot",
  "title": "Sales by Region and Product",
  "query": {
    "dimensions": [
      {"field": "REGION"},
      {"field": "PRODUCT_CATEGORY"}
    ],
    "measures": [
      {"field": "REVENUE", "aggregation": "sum"}
    ],
    "groupBy": ["REGION", "PRODUCT_CATEGORY"]
  },
  "visualization": {
    "type": "pivot",
    "rows": ["REGION"],
    "columns": ["PRODUCT_CATEGORY"],
    "values": ["REVENUE"],
    "showTotals": true,
    "showSubtotals": true
  }
}
```

---

## 11. Implementation Roadmap (Future Reference)

### Phase 1: Foundation (Months 1-2)
- [ ] Card DSL schema definition
- [ ] Query builder service (UI → DSL)
- [ ] Query compiler service (DSL → SQL)
- [ ] Basic card types (KPI, Table, Bar, Line, Pie)
- [ ] Simple dashboard layout engine

### Phase 2: Intelligence (Months 3-4)
- [ ] Query shape analyzer
- [ ] Result profiler
- [ ] Rule-based recommendation engine
- [ ] Guardrail validation
- [ ] AI integration (OpenAI/Gemini for ranking)

### Phase 3: Governance (Months 5-6)
- [ ] Certification workflow
- [ ] RLS enforcement
- [ ] Data lineage tracking
- [ ] Version control for cards
- [ ] Audit logging

### Phase 4: Advanced Features (Months 7-9)
- [ ] Advanced card types (Funnel, Cohort, Geo)
- [ ] Cross-filtering
- [ ] Drill-down paths
- [ ] Smart caching with invalidation
- [ ] Mobile-responsive cards

### Phase 5: Collaboration (Months 10-12)
- [ ] Comments and annotations
- [ ] Sharing with context
- [ ] Scheduled reports
- [ ] Email subscriptions
- [ ] Performance budgets and optimization

---

## 12. Key Design Decisions

### Why Hybrid Recommendation Engine?
- **Rules first**: Fast, predictable, explainable for common cases
- **AI second**: Handles edge cases, provides nuanced ranking, learns from feedback
- **Guardrails always**: Prevents obviously wrong suggestions

### Why Separate Card Type from Visualization?
- **Business thinking**: Users think "I want to compare" not "I want a bar chart"
- **Flexibility**: Same card type can have multiple visualization options
- **Intent capture**: Better for recommendations and semantic understanding

### Why Query DSL Instead of Direct SQL?
- **Database agnostic**: Works across Oracle, SQL Server, PostgreSQL, etc.
- **Optimization**: Can apply query rewrites and optimizations
- **Security**: Prevents SQL injection, easier to apply RLS
- **Lineage**: Easier to track what fields are being used

### Why Governance-First Design?
- **Enterprise ready**: Certification, RLS, lineage are table stakes for enterprise
- **Trust**: Certified cards build confidence in analytics
- **Compliance**: GDPR, data classification, retention policies built-in

---

## 13. Success Metrics

### User Adoption
- % of users creating cards vs. just viewing
- Time to first card creation
- Card reuse rate (shared vs. private)

### Recommendation Quality
- Recommendation acceptance rate
- User overrides (indicates bad recommendations)
- Confidence score vs. actual user satisfaction

### Performance
- Average query execution time
- Cache hit rate
- Cards loading in <2 seconds

### Governance
- % of cards certified
- Lineage coverage
- RLS policy compliance rate

---

## Document History

| Version | Date       | Author | Changes |
|---------|------------|--------|---------|
| 1.0     | 2026-03-22 | System | Initial design document |

---

**Note:** This is a design document. No implementation has been done yet. This document serves as the blueprint for the dashboard and card system in the OneAPI platform.
