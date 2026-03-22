# OneAPI Platform - Angular UI Setup Guide

## Project Created ✅

The Angular project has been initialized with the following structure:

```
oneapi-ui/
├── src/
│   ├── app/
│   │   ├── core/               # Core services, guards, interceptors
│   │   ├── shared/             # Shared components
│   │   ├── features/           # Feature modules
│   │   │   ├── auth/           # Authentication
│   │   │   ├── admin/          # Admin features
│   │   │   └── client/         # Client features
│   │   └── app.routes.ts
│   └── environments/
│       ├── environment.ts
│       └── environment.prod.ts
└── package.json
```

## What's Been Set Up

1. ✅ **Angular CLI Project** - Version 19+ with standalone components
2. ✅ **Routing** - Configured for lazy loading
3. ✅ **SCSS** - Styling framework
4. ✅ **Angular Material** - UI component library
5. ✅ **Folder Structure** - Organized by feature modules
6. ✅ **Environment Config** - Development and production settings

## Next Steps

### 1. Install Dependencies (if not done)
```bash
cd oneapi-ui
npm install
```

### 2. Generate Core Files

I recommend using Angular CLI to generate components and services:

#### Core Models
```bash
# User model
ng generate interface core/models/user --type=model

# Role model
ng generate interface core/models/role --type=model

# Datasource model
ng generate interface core/models/datasource --type=model

# Query model
ng generate interface core/models/query --type=model
```

#### Core Services
```bash
# Authentication service
ng generate service core/services/auth

# API service
ng generate service core/services/api

# User service
ng generate service core/services/user
```

#### Guards
```bash
# Auth guard
ng generate guard core/guards/auth

# Admin guard
ng generate guard core/guards/admin

# Role guard
ng generate guard core/guards/role
```

#### Interceptors
```bash
# Auth interceptor (for adding JWT token)
ng generate interceptor core/interceptors/auth

# Error interceptor
ng generate interceptor core/interceptors/error
```

### 3. Generate Feature Components

#### Authentication
```bash
ng generate component features/auth/login --standalone
```

#### Admin Module
```bash
# Admin dashboard
ng generate component features/admin/dashboard --standalone

# User management
ng generate component features/admin/user-management/user-list --standalone
ng generate component features/admin/user-management/user-create --standalone
ng generate component features/admin/user-management/user-edit --standalone

# Role management
ng generate component features/admin/role-management/role-list --standalone
ng generate component features/admin/role-management/role-create --standalone

# Datasource management
ng generate component features/admin/datasource-management/datasource-list --standalone
ng generate component features/admin/datasource-management/datasource-create --standalone

# Source assignment
ng generate component features/admin/source-assignment/role-source-mapping --standalone
```

#### Client Module
```bash
# Client dashboard
ng generate component features/client/dashboard --standalone

# Data explorer
ng generate component features/client/data-explorer/domain-browser --standalone
ng generate component features/client/data-explorer/entity-browser --standalone

# Query builder
ng generate component features/client/query-builder --standalone

# Saved queries
ng generate component features/client/saved-queries/query-list --standalone
ng generate component features/client/saved-queries/query-detail --standalone

# Query execution
ng generate component features/client/query-execution/execute-query --standalone
ng generate component features/client/query-execution/results-table --standalone
```

### 4. Configure Angular Material

Add to your `app.config.ts`:

```typescript
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor]))
  ]
};
```

### 5. Development Server

```bash
npm start
# or
ng serve

# Open http://localhost:4200
```

### 6. Connect to Backend

Make sure your Spring Boot backend is running on port 8088:

```bash
# In the oneapi-platform directory
./gradlew :oneapi-app:bootRun
```

## Recommended File Structure to Create

### 1. Models (`src/app/core/models/`)

**user.model.ts**:
```typescript
export interface User {
  id: number;
  login: string;
  email: string;
  firstName?: string;
  lastName?: string;
  activated: boolean;
  roles: Role[];
}

export interface Role {
  id: number;
  name: string;
  description?: string;
  permissions: Permission[];
}

export interface Permission {
  id: number;
  resourceType: string;  // DATABASE, TABLE, COLUMN
  resourceName: string;
  accessLevel: string;   // READ, WRITE, ADMIN
}
```

**datasource.model.ts**:
```typescript
export interface Datasource {
  id: number;
  name: string;
  type: 'H2' | 'POSTGRES' | 'MYSQL';
  host: string;
  port: number;
  database: string;
  username: string;
  password?: string;
  status?: 'ACTIVE' | 'INACTIVE';
}
```

**query.model.ts**:
```typescript
export interface SavedQuery {
  id: number;
  name: string;
  description?: string;
  datasourceId: number;
  sqlQuery: string;
  createdBy: string;
  createdDate: Date;
}

export interface QueryRequest {
  datasourceId: number;
  sqlQuery: string;
  sessionKey?: string;
}

export interface QueryResponse {
  sessionKey: string | null;
  records: any[];
  metadata: {
    recordCount: number;
    totalFetched: number;
    hasMore: boolean;
    expiresAt: Date | null;
    pageSize: number;
    requestCount: number;
  };
}
```

### 2. Authentication Service (`src/app/core/services/auth.service.ts`)

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private currentUserSubject: BehaviorSubject<User | null>;
  public currentUser: Observable<User | null>;

  constructor(private http: HttpClient) {
    const storedUser = localStorage.getItem(environment.userKey);
    this.currentUserSubject = new BehaviorSubject<User | null>(
      storedUser ? JSON.parse(storedUser) : null
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  login(username: string, password: string) {
    return this.http.post<any>(`${environment.apiUrl}/authenticate`, {
      username,
      password,
      rememberMe: false
    }).pipe(map(response => {
      // Store JWT token
      localStorage.setItem(environment.tokenKey, response.token);

      // Get user details
      return this.getCurrentUser();
    }));
  }

  getCurrentUser() {
    return this.http.get<User>(`${environment.apiUrl}/account`).pipe(
      map(user => {
        localStorage.setItem(environment.userKey, JSON.stringify(user));
        this.currentUserSubject.next(user);
        return user;
      })
    );
  }

  logout() {
    localStorage.removeItem(environment.tokenKey);
    localStorage.removeItem(environment.userKey);
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem(environment.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  hasRole(role: string): boolean {
    const user = this.currentUserValue;
    return user?.roles.some(r => r.name === role) || false;
  }

  isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN');
  }
}
```

### 3. Auth Guard (`src/app/core/guards/auth.guard.ts`)

```typescript
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};

export const adminGuard = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated() && authService.isAdmin()) {
    return true;
  }

  router.navigate(['/']);
  return false;
};
```

### 4. Auth Interceptor (`src/app/core/interceptors/auth.interceptor.ts`)

```typescript
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req);
};
```

### 5. Routes (`src/app/app.routes.ts`)

```typescript
import { Routes } from '@angular/router';
import { authGuard, adminGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component')
      .then(m => m.LoginComponent)
  },
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadChildren: () => import('./features/admin/admin.routes')
      .then(m => m.ADMIN_ROUTES)
  },
  {
    path: 'client',
    canActivate: [authGuard],
    loadChildren: () => import('./features/client/client.routes')
      .then(m => m.CLIENT_ROUTES)
  }
];
```

## Key Features to Implement

### Admin Features
1. **User Management** - CRUD operations for users
2. **Role Management** - Create roles and assign permissions
3. **Datasource Management** - Configure database connections
4. **Source Assignment** - Map datasources to roles

### Client Features
1. **Dashboard** - View assigned datasources
2. **Data Explorer** - Browse domains, entities, fields
3. **Query Builder** - Visual SQL builder (Metabase-style)
4. **Saved Queries** - Save and manage queries
5. **Query Execution** - Run queries with pagination

## Design Reference

See `UI-DESIGN.md` for detailed architecture and wireframes.

## Backend Integration

The Angular app connects to the Spring Boot backend:
- **Base URL**: http://localhost:8088/api
- **Authentication**: JWT Bearer tokens
- **Endpoints**: See UI-DESIGN.md for full API documentation

## Styling

Use Angular Material components for consistent UI:
```bash
# Import in your component
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatInputModule } from '@angular/material/input';
// ... etc
```

## Tips

1. **Use Standalone Components** - Angular 19+ defaults to standalone
2. **Lazy Loading** - Use loadChildren for better performance
3. **Reactive Forms** - For all forms with validation
4. **RxJS** - For state management and async operations
5. **Material Design** - Consistent UI across the app

## Next Phase

Once you have the basic structure, you can start implementing:
1. Login component and authentication flow
2. Admin dashboard with user management
3. Client dashboard with query builder
4. Results table with pagination

Would you like me to generate specific components or services?
