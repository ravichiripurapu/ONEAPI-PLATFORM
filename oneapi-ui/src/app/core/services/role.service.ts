import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RoleDTO {
  id: number;
  name: string;
  description?: string;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface CreateRoleRequest {
  name: string;
  description?: string;
}

export interface UpdateRoleRequest {
  name?: string;
  description?: string;
}

export interface PermissionDTO {
  id: number;
  roleId: number;
  roleName?: string;
  sourceId?: number;
  sourceName?: string;
  domainId?: number;
  domainName?: string;
  entityId?: number;
  entityName?: string;
  fieldId?: number;
  fieldName?: string;
  permissionType: 'READ' | 'WRITE' | 'DELETE';
  level: number; // 0=Super Admin, 1=Source, 2=Domain, 3=Entity, 4=Field
  createdAt?: string;
  createdBy?: string;
}

export interface CreatePermissionRequest {
  roleId: number;
  sourceId?: number;
  domainId?: number;
  entityId?: number;
  fieldId?: number;
  permissionType: 'READ' | 'WRITE' | 'DELETE';
}

@Injectable({
  providedIn: 'root'
})
export class RoleService {
  private apiUrl = `${environment.apiUrl}/roles`;
  private permissionsUrl = `${environment.apiUrl}/permissions`;

  constructor(private http: HttpClient) {}

  // Role CRUD
  getAllRoles(): Observable<RoleDTO[]> {
    return this.http.get<RoleDTO[]>(this.apiUrl);
  }

  getRoleById(id: number): Observable<RoleDTO> {
    return this.http.get<RoleDTO>(`${this.apiUrl}/${id}`);
  }

  createRole(request: CreateRoleRequest): Observable<RoleDTO> {
    return this.http.post<RoleDTO>(this.apiUrl, request);
  }

  updateRole(id: number, request: UpdateRoleRequest): Observable<RoleDTO> {
    return this.http.put<RoleDTO>(`${this.apiUrl}/${id}`, request);
  }

  deleteRole(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Permission Management
  getPermissionsByRole(roleId: number): Observable<PermissionDTO[]> {
    return this.http.get<PermissionDTO[]>(`${this.permissionsUrl}/role/${roleId}`);
  }

  createPermission(request: CreatePermissionRequest): Observable<PermissionDTO> {
    return this.http.post<PermissionDTO>(this.permissionsUrl, request);
  }

  deletePermission(permissionId: number): Observable<void> {
    return this.http.delete<void>(`${this.permissionsUrl}/${permissionId}`);
  }

  // Bulk operations
  bulkCreatePermissions(requests: CreatePermissionRequest[]): Observable<PermissionDTO[]> {
    return this.http.post<PermissionDTO[]>(`${this.permissionsUrl}/bulk`, requests);
  }
}
