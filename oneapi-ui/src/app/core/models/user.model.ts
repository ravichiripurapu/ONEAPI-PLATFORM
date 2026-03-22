export interface User {
  id: number;
  login: string;
  email: string;
  firstName?: string;
  lastName?: string;
  activated: boolean;
  langKey?: string;
  imageUrl?: string;
  roles: Role[];
}

export interface Role {
  id: number;
  name: string;
  description?: string;
  permissions?: Permission[];
}

export interface Permission {
  id: number;
  resourceType: 'DATABASE' | 'TABLE' | 'COLUMN';
  resourceName: string;
  accessLevel: 'READ' | 'WRITE' | 'ADMIN';
}

export interface UserPreferences {
  userId: string;
  pageSize: number;
  ttlMinutes: number;
  maxConcurrentSessions: number;
}

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface LoginResponse {
  id_token: string;
}
