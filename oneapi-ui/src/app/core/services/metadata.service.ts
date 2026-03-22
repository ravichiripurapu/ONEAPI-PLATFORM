import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DomainInfoDTO {
  id: number;
  datasourceId: number;
  connectionName: string;
  schemaName: string;
  tableCount?: number;
  viewCount?: number;
  discoveredAt?: string;
  lastSyncedAt?: string;
  createdDate?: string;
}

export interface EntityInfoDTO {
  id: number;
  schemaId: number;
  schemaName: string;
  tableName: string;
  tableType: string; // TABLE, VIEW, MATERIALIZED_VIEW, SYSTEM_TABLE
  tableComment?: string;
  estimatedRowCount?: number;
  sizeInBytes?: number;
  discoveredAt?: string;
  lastSyncedAt?: string;
  createdDate?: string;
  // AI-generated enrichment fields
  businessName?: string;
  businessDescription?: string;
}

export interface FieldInfoDTO {
  id: number;
  tableId: number;
  tableName: string;
  schemaName: string;
  columnName: string;
  dataType: string;
  jdbcType?: string;
  columnSize?: number;
  decimalDigits?: number;
  nullable?: boolean;
  defaultValue?: string;
  columnComment?: string;
  isPrimaryKey?: boolean;
  isForeignKey?: boolean;
  isUnique?: boolean;
  isIndexed?: boolean;
  isAutoIncrement?: boolean;
  ordinalPosition?: number;
  discoveredAt?: string;
  createdDate?: string;
  // AI-generated enrichment fields
  businessName?: string;
  businessDescription?: string;
}

@Injectable({
  providedIn: 'root'
})
export class MetadataService {
  private apiUrl = `${environment.apiUrl}/v1/metadata`;

  constructor(private http: HttpClient) {}

  // Domain Operations
  getDomainsBySource(sourceId: number): Observable<DomainInfoDTO[]> {
    return this.http.get<DomainInfoDTO[]>(`${this.apiUrl}/domains/source/${sourceId}`);
  }

  getDomainById(domainId: number): Observable<DomainInfoDTO> {
    return this.http.get<DomainInfoDTO>(`${this.apiUrl}/domains/${domainId}`);
  }

  // Entity Operations
  getEntitiesBySource(sourceId: number): Observable<EntityInfoDTO[]> {
    return this.http.get<EntityInfoDTO[]>(`${this.apiUrl}/entities/source/${sourceId}`);
  }

  getEntitiesByDomain(domainId: number): Observable<EntityInfoDTO[]> {
    return this.http.get<EntityInfoDTO[]>(`${this.apiUrl}/entities/domain/${domainId}`);
  }

  getEntityById(entityId: number): Observable<EntityInfoDTO> {
    return this.http.get<EntityInfoDTO>(`${this.apiUrl}/entities/${entityId}`);
  }

  searchEntitiesByName(tableName: string, sourceId?: number): Observable<EntityInfoDTO[]> {
    const params: any = { tableName };
    if (sourceId) {
      params.sourceId = sourceId;
    }
    return this.http.get<EntityInfoDTO[]>(`${this.apiUrl}/entities/search`, { params });
  }

  // Field Operations
  getFieldsByEntity(entityId: number): Observable<FieldInfoDTO[]> {
    return this.http.get<FieldInfoDTO[]>(`${this.apiUrl}/entities/${entityId}/fields`);
  }

  getFieldById(fieldId: number): Observable<FieldInfoDTO> {
    return this.http.get<FieldInfoDTO>(`${this.apiUrl}/fields/${fieldId}`);
  }

  getPrimaryKeyFields(entityId: number): Observable<FieldInfoDTO[]> {
    return this.http.get<FieldInfoDTO[]>(`${this.apiUrl}/entities/${entityId}/primary-keys`);
  }

  searchFieldsByName(columnName: string): Observable<FieldInfoDTO[]> {
    return this.http.get<FieldInfoDTO[]>(`${this.apiUrl}/fields/search`, { params: { columnName } });
  }
}
