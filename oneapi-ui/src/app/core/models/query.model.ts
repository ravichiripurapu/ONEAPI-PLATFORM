export interface SavedQuery {
  id: number;
  name: string;
  description?: string;
  datasourceId: number;
  sqlQuery: string;
  tags?: string[];
  isPublic?: boolean;
  createdBy: string;
  createdDate: Date;
  lastModifiedBy?: string;
  lastModifiedDate?: Date;
}

export interface QueryRequest {
  datasourceId?: number;
  sqlQuery?: string;
  sessionKey?: string;
  tableName?: string;
  schema?: string;
  filters?: any[];
  isContinuation?: boolean;
  isSqlQuery?: boolean;
}

export interface QueryResponse {
  sessionKey: string | null;
  records: any[];
  metadata: QueryMetadata;
}

export interface QueryMetadata {
  recordCount: number;
  totalFetched: number;
  hasMore: boolean;
  expiresAt: Date | null;
  pageSize: number;
  requestCount: number;
}

// Enhanced Query Builder with Multi-Table Support
export interface QueryBuilder {
  datasourceId: number;
  tables: QueryTable[];
  joins: QueryJoin[];
  columns: QueryColumn[];
  filters: QueryFilter[];
  aggregations: QueryAggregation[];
  groupBy: string[];
  orderBy: QueryOrderBy[];
  limit?: number;
}

export interface QueryTable {
  entityId: number;
  alias?: string;
  schemaName: string;
  tableName: string;
}

export interface QueryJoin {
  sourceTable: string;
  sourceColumn: string;
  targetTable: string;
  targetColumn: string;
  joinType: 'INNER' | 'LEFT' | 'RIGHT' | 'FULL';
}

export interface QueryColumn {
  table: string;
  column: string;
  alias?: string;
  aggregate?: 'COUNT' | 'SUM' | 'AVG' | 'MIN' | 'MAX';
}

export interface QueryFilter {
  table?: string;
  field: string;
  operator: 'equals' | 'notEquals' | 'contains' | 'startsWith' | 'endsWith' |
            'greaterThan' | 'lessThan' | 'greaterThanOrEqual' | 'lessThanOrEqual' |
            'in' | 'notIn' | 'isNull' | 'isNotNull';
  value: any;
}

export interface QueryAggregation {
  function: 'count' | 'sum' | 'avg' | 'min' | 'max';
  field: string;
  alias?: string;
}

export interface QueryOrderBy {
  field: string;
  direction: 'ASC' | 'DESC';
}

// Natural Language Query
export interface NaturalLanguageQuery {
  question: string;
  datasourceId: number;
  context?: {
    tables?: string[];
    previousQueries?: string[];
  };
}

export interface NaturalLanguageResponse {
  generatedSQL: string;
  explanation: string;
  confidence: number;
  suggestedTables: string[];
}

// Query Mode
export type QueryMode = 'visual' | 'sql' | 'natural';

// SQL Query Mode
export interface SQLQueryInput {
  name?: string;
  description?: string;
  datasourceId: number;
  sqlText: string;
  isPublic?: boolean;
}
