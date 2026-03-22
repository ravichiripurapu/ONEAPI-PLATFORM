import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UserDTO {
  id: number;
  login: string;
  email: string;
  firstName?: string;
  lastName?: string;
  activated: boolean;
  langKey?: string;
  createdBy?: string;
  createdDate?: string;
  lastModifiedBy?: string;
  lastModifiedDate?: string;
}

export interface CreateUserRequest {
  login: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  activated: boolean;
  langKey?: string;
}

export interface UpdateUserRequest {
  email?: string;
  firstName?: string;
  lastName?: string;
  activated?: boolean;
  langKey?: string;
}

export interface UserPreferencesDTO {
  id?: number;
  userId: string;
  pageSize?: number;
  ttlMinutes?: number;
  maxConcurrentSessions?: number;
}

export interface UserRoleDTO {
  id: number;
  userId: number;
  roleId: number;
  roleName?: string;
}

export interface AssignRoleRequest {
  userId: number;
  roleId: number;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/users`;
  private preferencesUrl = `${environment.apiUrl}/preferences`;
  private userRolesUrl = `${environment.apiUrl}/user-roles`;

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<UserDTO[]> {
    return this.http.get<UserDTO[]>(this.apiUrl);
  }

  getUserById(id: number): Observable<UserDTO> {
    return this.http.get<UserDTO>(`${this.apiUrl}/${id}`);
  }

  getUserByLogin(login: string): Observable<UserDTO> {
    return this.http.get<UserDTO>(`${this.apiUrl}/login/${login}`);
  }

  createUser(request: CreateUserRequest): Observable<UserDTO> {
    return this.http.post<UserDTO>(this.apiUrl, request);
  }

  updateUser(id: number, request: UpdateUserRequest): Observable<UserDTO> {
    return this.http.put<UserDTO>(`${this.apiUrl}/${id}`, request);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // User Preferences (for current user)
  getUserPreferences(): Observable<UserPreferencesDTO> {
    return this.http.get<UserPreferencesDTO>(this.preferencesUrl);
  }

  updateUserPreferences(preferences: UserPreferencesDTO): Observable<UserPreferencesDTO> {
    return this.http.put<UserPreferencesDTO>(this.preferencesUrl, preferences);
  }

  // Admin: Manage preferences for any user
  getUserPreferencesByUserId(userId: string): Observable<UserPreferencesDTO> {
    return this.http.get<UserPreferencesDTO>(`${this.preferencesUrl}/user/${userId}`);
  }

  updateUserPreferencesByUserId(userId: string, preferences: UserPreferencesDTO): Observable<UserPreferencesDTO> {
    return this.http.put<UserPreferencesDTO>(`${this.preferencesUrl}/user/${userId}`, preferences);
  }

  // User Roles
  getUserRoles(userId: number): Observable<UserRoleDTO[]> {
    return this.http.get<UserRoleDTO[]>(`${this.userRolesUrl}/user/${userId}`);
  }

  assignRole(request: AssignRoleRequest): Observable<UserRoleDTO> {
    return this.http.post<UserRoleDTO>(this.userRolesUrl, request);
  }

  revokeUserRole(userRoleId: number): Observable<void> {
    return this.http.delete<void>(`${this.userRolesUrl}/${userRoleId}`);
  }
}
