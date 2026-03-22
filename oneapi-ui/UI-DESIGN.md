# OneAPI Platform - Angular UI Design

## Overview
Single Page Application with Admin and Client interfaces for database query management.

## Architecture

### Module Structure
```
oneapi-ui/
├── src/
│   ├── app/
│   │   ├── core/                    # Singleton services, guards, interceptors
│   │   │   ├── services/
│   │   │   │   ├── auth.service.ts
│   │   │   │   ├── api.service.ts
│   │   │   │   └── user.service.ts
│   │   │   ├── guards/
│   │   │   │   ├── auth.guard.ts
│   │   │   │   ├── admin.guard.ts
│   │   │   │   └── role.guard.ts
│   │   │   ├── interceptors/
│   │   │   │   ├── auth.interceptor.ts
│   │   │   │   └── error.interceptor.ts
│   │   │   └── models/
│   │   │       ├── user.model.ts
│   │   │       ├── role.model.ts
│   │   │       └── datasource.model.ts
│   │   │
│   │   ├── shared/                  # Shared components, directives, pipes
│   │   │   ├── components/
│   │   │   │   ├── navbar/
│   │   │   │   ├── sidebar/
│   │   │   │   ├── breadcrumb/
│   │   │   │   ├── loading-spinner/
│   │   │   │   └── data-table/
│   │   │   ├── directives/
│   │   │   └── pipes/
│   │   │
│   │   ├── features/
│   │   │   ├── auth/                # Login/Register
│   │   │   │   ├── login/
│   │   │   │   └── auth.module.ts
│   │   │   │
│   │   │   ├── admin/               # Admin Module
│   │   │   │   ├── dashboard/
│   │   │   │   ├── user-management/
│   │   │   │   │   ├── user-list/
│   │   │   │   │   ├── user-create/
│   │   │   │   │   └── user-edit/
│   │   │   │   ├── role-management/
│   │   │   │   │   ├── role-list/
│   │   │   │   │   ├── role-create/
│   │   │   │   │   └── role-permissions/
│   │   │   │   ├── datasource-management/
│   │   │   │   │   ├── datasource-list/
│   │   │   │   │   ├── datasource-create/
│   │   │   │   │   └── datasource-test/
│   │   │   │   ├── source-assignment/
│   │   │   │   │   └── role-source-mapping/
│   │   │   │   └── admin.module.ts
│   │   │   │
│   │   │   └── client/              # Client Module
│   │   │       ├── dashboard/
│   │   │       │   └── source-list/
│   │   │       ├── data-explorer/
│   │   │       │   ├── domain-browser/
│   │   │       │   ├── entity-browser/
│   │   │       │   └── field-viewer/
│   │   │       ├── query-builder/
│   │   │       │   ├── builder-canvas/
│   │   │       │   ├── field-selector/
│   │   │       │   ├── filter-builder/
│   │   │       │   ├── aggregation-builder/
│   │   │       │   └── preview-panel/
│   │   │       ├── saved-queries/
│   │   │       │   ├── query-list/
│   │   │       │   ├── query-detail/
│   │   │       │   └── query-share/
│   │   │       ├── query-execution/
│   │   │       │   ├── execute-query/
│   │   │       │   ├── results-table/
│   │   │       │   └── export-data/
│   │   │       └── client.module.ts
│   │   │
│   │   ├── app.component.ts
│   │   ├── app.config.ts
│   │   └── app.routes.ts
│   │
│   └── environments/
│       ├── environment.ts
│       └── environment.prod.ts
```

## Routing Structure

### Public Routes
- `/login` - Login page
- `/register` - Registration (if enabled)

### Admin Routes (requires ADMIN role)
- `/admin` - Admin dashboard
- `/admin/users` - User list
- `/admin/users/create` - Create user
- `/admin/users/:id/edit` - Edit user
- `/admin/roles` - Role list
- `/admin/roles/create` - Create role
- `/admin/roles/:id/permissions` - Manage role permissions
- `/admin/datasources` - Datasource list
- `/admin/datasources/create` - Create datasource
- `/admin/datasources/:id/test` - Test connection
- `/admin/sources/assign` - Assign sources to roles

### Client Routes (requires USER role)
- `/client` - Client dashboard with assigned sources
- `/client/sources/:id/explore` - Browse domains/entities
- `/client/sources/:id/query-builder` - Visual query builder
- `/client/queries` - Saved queries list
- `/client/queries/:id` - View/Edit saved query
- `/client/queries/:id/execute` - Execute query and view results

## Features

### Admin Features

#### 1. User Management
- **List Users**: Paginated table with search, filter, sort
- **Create User**: Form with validation (username, email, password, roles)
- **Edit User**: Update user details, change password, assign roles
- **Delete User**: Soft delete with confirmation
- **User Preferences**: Set default pageSize, ttlMinutes per user

#### 2. Role Management
- **List Roles**: Display all roles with permission counts
- **Create Role**: Define role name and description
- **Manage Permissions**: Assign database/table/column level permissions
- **Delete Role**: Remove role (if not assigned to users)

#### 3. Datasource Management
- **List Datasources**: Show all configured data sources
- **Create Datasource**: Form with connection details (type, host, port, database, credentials)
- **Test Connection**: Validate before saving
- **Edit Datasource**: Update connection settings
- **Delete Datasource**: Remove datasource (if not assigned)

#### 4. Source Assignment
- **Role-Source Mapping**: Matrix view to assign sources to roles
- **Permission Levels**: Read, Write, Admin access per source

### Client Features

#### 1. Dashboard
- **Assigned Sources**: Card grid showing accessible data sources
- **Recent Queries**: Quick access to recently executed queries
- **Saved Queries**: Shortcuts to favorite queries
- **Statistics**: Query execution stats, data volume

#### 2. Data Explorer
- **Domain Browser**: Tree view of available domains
- **Entity Browser**: List of tables/entities with metadata
- **Field Viewer**: Column details (name, type, nullable, constraints)
- **Sample Data**: Preview first 10 rows

#### 3. Query Builder (Metabase-style)
- **Visual Builder**:
  - Select table from dropdown
  - Add filters with visual operators (equals, contains, greater than, etc.)
  - Choose columns to display
  - Add aggregations (count, sum, avg, min, max)
  - Group by fields
  - Sort results
  - Limit rows
- **SQL Preview**: Show generated SQL
- **Run Query**: Execute and show results
- **Save Query**: Name and save for later

#### 4. Saved Queries
- **List Queries**: All user's saved queries with tags
- **Search/Filter**: Find queries by name, tags, datasource
- **View Query**: See query definition and SQL
- **Execute**: Run saved query with pagination
- **Edit**: Modify query builder settings
- **Clone**: Duplicate and modify
- **Share**: Share with other users (if permitted)
- **Delete**: Remove saved query

#### 5. Query Execution
- **Execute Query**: Run query with real-time progress
- **Paginated Results**: 20 rows per page with session management
- **Column Sorting**: Click headers to sort
- **Export Options**: CSV, JSON, Excel
- **Visualizations**: Basic charts (if time permits)

## UI/UX Design

### Theme
- **Primary Color**: Blue (#1976d2)
- **Accent Color**: Orange (#ff9800)
- **Dark Mode**: Toggle for dark/light theme

### Components
- **Material Design**: Angular Material components
- **Responsive**: Mobile-first, works on tablet/desktop
- **Icons**: Material Icons
- **Tables**: Sortable, filterable, paginated
- **Forms**: Reactive forms with validation
- **Notifications**: Snackbar for success/error messages

### Query Builder UI (Metabase-inspired)
```
┌────────────────────────────────────────────────────────┐
│ Query Builder                                    [Save]│
├────────────────────────────────────────────────────────┤
│ 1. Pick your data                                      │
│    [Select Table ▼] SALES.CUSTOMERS                    │
├────────────────────────────────────────────────────────┤
│ 2. Filter                                         [+]  │
│    ┌────────────────────────────────────────────┐     │
│    │ [Field ▼] [Operator ▼] [Value...........]  │ [×] │
│    └────────────────────────────────────────────┘     │
├────────────────────────────────────────────────────────┤
│ 3. Summarize                                           │
│    [Aggregation ▼] Count   [Group by ▼] None          │
├────────────────────────────────────────────────────────┤
│ 4. Sort & Limit                                        │
│    [Sort by ▼] ID  [Order ▼] ASC  [Limit] 100        │
├────────────────────────────────────────────────────────┤
│ SQL Preview:                                    [Show] │
│    SELECT * FROM SALES.CUSTOMERS LIMIT 100             │
├────────────────────────────────────────────────────────┤
│                                         [Run Query ▶]  │
└────────────────────────────────────────────────────────┘
```

## API Integration

### Base URL
```typescript
environment.apiUrl = 'http://localhost:8088/api'
```

### Endpoints Used

#### Authentication
- POST `/authenticate` - Login
- GET `/account` - Get current user

#### Admin - Users
- GET `/users` - List users
- POST `/users` - Create user
- GET `/users/:id` - Get user
- PUT `/users/:id` - Update user
- DELETE `/users/:id` - Delete user

#### Admin - Roles
- GET `/roles` - List roles
- POST `/roles` - Create role
- GET `/roles/:id` - Get role
- PUT `/roles/:id` - Update role
- DELETE `/roles/:id` - Delete role

#### Admin - Permissions
- GET `/roles/:id/permissions` - Get role permissions
- POST `/permissions/grant` - Grant permission
- DELETE `/permissions/:id` - Revoke permission

#### Admin - Datasources
- GET `/admin/sources` - List datasources
- POST `/admin/sources` - Create datasource
- GET `/admin/sources/:id` - Get datasource
- PUT `/admin/sources/:id` - Update datasource
- DELETE `/admin/sources/:id` - Delete datasource
- POST `/admin/sources/:id/test` - Test connection

#### Client - Data Explorer
- GET `/sources` - List assigned sources
- GET `/metadata/domains/:sourceId` - Get domains
- GET `/metadata/entities/:sourceId` - Get entities
- GET `/metadata/fields/:entityId` - Get fields

#### Client - Queries
- GET `/v1/queries` - List saved queries
- POST `/v1/queries` - Create query
- GET `/v1/queries/:id` - Get query
- PUT `/v1/queries/:id` - Update query
- DELETE `/v1/queries/:id` - Delete query
- POST `/v1/queries/:id/execute` - Execute query (with pagination)

## State Management
- **Services**: Centralized state in services
- **BehaviorSubjects**: For reactive data streams
- **Local Storage**: JWT token, user preferences

## Security
- **JWT Authentication**: Bearer token in Authorization header
- **Route Guards**: Protect admin/client routes
- **Role-based Access**: Check user roles for features
- **Input Validation**: Client-side validation before API calls
- **XSS Protection**: Angular built-in sanitization

## Development Phases

### Phase 1: Core Setup (Now)
- Project structure
- Authentication
- Routing
- Core services

### Phase 2: Admin Module
- User management
- Role management
- Datasource management
- Source assignment

### Phase 3: Client Module
- Dashboard
- Data explorer
- Saved queries

### Phase 4: Query Builder
- Visual builder
- SQL generation
- Query execution

### Phase 5: Polish
- Styling
- Responsive design
- Error handling
- Performance optimization
