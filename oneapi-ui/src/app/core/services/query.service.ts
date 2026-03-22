import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SavedQueryDTO {
  id?: number;
  name: string;
  description?: string;
  queryText: string;
  datasourceId: number;
  connectionName?: string;
  isPublic?: boolean;
  isFavorite?: boolean;
  executionCount?: number;
  lastExecutedAt?: string;
  avgExecutionTimeMs?: number;
  createdBy?: string;
  createdDate?: string;
  lastModifiedBy?: string;
  lastModifiedDate?: string;
}

export interface QueryMetadata {
  recordCount: number;
  totalFetched: number;
  hasMore: boolean;
  expiresAt: string;
  pageSize: number;
  requestCount: number;
}

export interface QueryResponse {
  sessionKey: string | null;
  records: any[];
  metadata: QueryMetadata;
}

export interface NaturalLanguageQueryRequest {
  question: string;
  datasourceId: number;
  context?: {
    tables?: string[];
    previousQueries?: string[];
    hint?: string;
  };
}

export interface NaturalLanguageQueryResponse {
  generatedSQL: string;
  explanation: string;
  confidence: number;
  suggestedTables?: string[];
  warnings?: string[];
  alternatives?: Array<{
    sql: string;
    description: string;
    confidence: number;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class QueryService {
  private apiUrl = `${environment.apiUrl}/v1/queries`;

  constructor(private http: HttpClient) {}

  // Saved Query Management
  create(query: SavedQueryDTO): Observable<SavedQueryDTO> {
    return this.http.post<SavedQueryDTO>(this.apiUrl, query);
  }

  update(id: number, query: SavedQueryDTO): Observable<SavedQueryDTO> {
    return this.http.put<SavedQueryDTO>(`${this.apiUrl}/${id}`, query);
  }

  getById(id: number): Observable<SavedQueryDTO> {
    return this.http.get<SavedQueryDTO>(`${this.apiUrl}/${id}`);
  }

  getAll(): Observable<SavedQueryDTO[]> {
    return this.http.get<SavedQueryDTO[]>(this.apiUrl);
  }

  getMyQueries(): Observable<SavedQueryDTO[]> {
    return this.http.get<SavedQueryDTO[]>(`${this.apiUrl}/my`);
  }

  getBySource(sourceId: number): Observable<SavedQueryDTO[]> {
    return this.http.get<SavedQueryDTO[]>(`${this.apiUrl}/source/${sourceId}`);
  }

  getPublicQueries(): Observable<SavedQueryDTO[]> {
    return this.http.get<SavedQueryDTO[]>(`${this.apiUrl}/public`);
  }

  getFavorites(): Observable<SavedQueryDTO[]> {
    return this.http.get<SavedQueryDTO[]>(`${this.apiUrl}/favorites`);
  }

  getAccessibleQueries(): Observable<SavedQueryDTO[]> {
    return this.http.get<SavedQueryDTO[]>(`${this.apiUrl}/accessible`);
  }

  search(query: string): Observable<SavedQueryDTO[]> {
    return this.http.get<SavedQueryDTO[]>(`${this.apiUrl}/search`, { params: { q: query } });
  }

  toggleFavorite(id: number): Observable<SavedQueryDTO> {
    return this.http.post<SavedQueryDTO>(`${this.apiUrl}/${id}/favorite`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Query Execution
  executeQuery(id: number, sessionKey?: string, parameters?: any): Observable<QueryResponse> {
    return this.http.post<QueryResponse>(
      `${this.apiUrl}/${id}/execute`,
      parameters || {},
      { params: sessionKey ? { sessionKey } : {} }
    );
  }

  previewQuery(datasourceId: number, sql: string): Observable<any[]> {
    return this.http.post<any[]>(
      `${this.apiUrl}/preview`,
      sql,
      {
        params: { datasourceId: datasourceId.toString() },
        headers: { 'Content-Type': 'text/plain' }
      }
    );
  }

  recordExecution(id: number, executionTimeMs: number): Observable<void> {
    return this.http.post<void>(
      `${this.apiUrl}/${id}/record-execution`,
      null,
      { params: { executionTimeMs: executionTimeMs.toString() } }
    );
  }

  // Natural Language Query
  convertNaturalLanguageToSQL(request: NaturalLanguageQueryRequest): Observable<NaturalLanguageQueryResponse> {
    return this.http.post<NaturalLanguageQueryResponse>(
      `${this.apiUrl}/natural-language`,
      request
    );
  }

  explainSQL(datasourceId: number, sql: string): Observable<string> {
    return this.http.post<string>(
      `${this.apiUrl}/explain-sql`,
      sql,
      {
        params: { datasourceId: datasourceId.toString() },
        headers: { 'Content-Type': 'text/plain' },
        responseType: 'text' as 'json'
      }
    );
  }

  checkNaturalLanguageServiceStatus(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/natural-language/status`);
  }
}
