import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SourceInfoDTO {
  id?: number;
  name: string;
  type: 'H2' | 'POSTGRESQL' | 'MYSQL' | 'ORACLE' | 'SQLSERVER';
  host?: string;
  port?: number;
  database: string;
  username?: string;
  password?: string;
  additionalParams?: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ConnectionTestResult {
  success: boolean;
  message: string;
  connectionTime?: number;
}

// Keep old interface for backwards compatibility
export interface DatasourceDTO extends SourceInfoDTO {}

@Injectable({
  providedIn: 'root'
})
export class DatasourceService {
  private apiUrl = `${environment.apiUrl}/sources`;

  constructor(private http: HttpClient) {}

  getAllDatasources(): Observable<SourceInfoDTO[]> {
    return this.http.get<SourceInfoDTO[]>(this.apiUrl);
  }

  getDatasourceById(id: number): Observable<SourceInfoDTO> {
    return this.http.get<SourceInfoDTO>(`${this.apiUrl}/${id}`);
  }

  createDatasource(request: SourceInfoDTO): Observable<SourceInfoDTO> {
    return this.http.post<SourceInfoDTO>(this.apiUrl, request);
  }

  updateDatasource(id: number, request: SourceInfoDTO): Observable<SourceInfoDTO> {
    return this.http.put<SourceInfoDTO>(`${this.apiUrl}/${id}`, request);
  }

  deleteDatasource(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  testConnection(id: number): Observable<ConnectionTestResult> {
    return this.http.post<ConnectionTestResult>(`${this.apiUrl}/${id}/test`, {});
  }

  testConnectionWithoutSaving(dto: SourceInfoDTO): Observable<ConnectionTestResult> {
    return this.http.post<ConnectionTestResult>(`${this.apiUrl}/test`, dto);
  }
}
