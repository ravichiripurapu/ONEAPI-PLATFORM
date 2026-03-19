# OneAPI Admin Application - API Documentation

## Table of Contents
1. [Overview](#overview)
2. [Authentication](#authentication)
3. [REST API Endpoints](#rest-api-endpoints)
4. [GraphQL API](#graphql-api)
5. [Query Session System](#query-session-system)
6. [User Preferences](#user-preferences)
7. [Error Handling](#error-handling)
8. [Examples](#examples)

---

## Overview

The OneAPI Admin Application provides REST and GraphQL APIs for managing database connections and querying data with a powerful session-based pagination system.

**Base URL**: `http://localhost:8090`

**Key Features**:
- Session-based pagination for large datasets
- Configurable page size and TTL per user
- Dual caching strategy (Caffeine + Redis)
- REST and GraphQL endpoints
- Real-time schema discovery
- Connection pooling and management

---

## Authentication

### HTTP Basic Authentication

All API endpoints (except public endpoints) require HTTP Basic Authentication.

**Demo Credentials** (Development Only):
- Admin: `admin` / `admin123`
- User1: `user1` / `user123`
- User2: `user2` / `user123`
- Developer: `developer` / `dev123`

**Headers**:
```
Authorization: Basic <base64(username:password)>
```

**Example**:
```bash
curl -u admin:admin123 http://localhost:8090/api/query
```

**Production Note**: Replace in-memory authentication with database/LDAP/OAuth2 in production.

---

## REST API Endpoints

### Query Data API

#### 1. Query Data (Unified Endpoint)

Start a new query or continue an existing session.

**Endpoint**: `POST /api/query`

**Request Body**:
```json
{
  "connectionId": 1,
  "tableName": "users",
  "schema": "public",
  "sessionKey": null
}
```

**Request Fields**:
- `connectionId` (Long, required for new query): Database connection ID
- `tableName` (String, required for new query): Table name to query
- `schema` (String, optional): Schema name (default: public/dbo)
- `sessionKey` (String, required for continuation): Session key from previous response

**Response**:
```json
{
  "sessionKey": "a3b5c7d9-e1f2-4a5b-8c9d-0e1f2a3b4c5d",
  "records": [
    {
      "id": 1,
      "name": "John Doe",
      "email": "john@example.com",
      "created_at": "2024-01-15T10:30:00"
    },
    {
      "id": 2,
      "name": "Jane Smith",
      "email": "jane@example.com",
      "created_at": "2024-01-16T14:20:00"
    }
  ],
  "metadata": {
    "recordCount": 100,
    "totalFetched": 100,
    "hasMore": true,
    "expiresAt": "2024-03-17T15:15:00",
    "pageSize": 100
  }
}
```

**Response Fields**:
- `sessionKey` (String, nullable): Session key for next request (null if no more data)
- `records` (Array): Array of record objects (each table has different fields)
- `metadata` (Object): Query metadata
  - `recordCount` (Integer): Number of records in this response
  - `totalFetched` (Long): Total records fetched so far
  - `hasMore` (Boolean): Whether more data is available
  - `expiresAt` (String, nullable): Session expiry timestamp (ISO 8601)
  - `pageSize` (Integer): Current page size

**Example - New Query**:
```bash
curl -X POST http://localhost:8090/api/query \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "connectionId": 1,
    "tableName": "customers",
    "schema": "public"
  }'
```

**Example - Continue Session**:
```bash
curl -X POST http://localhost:8090/api/query \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "sessionKey": "a3b5c7d9-e1f2-4a5b-8c9d-0e1f2a3b4c5d"
  }'
```

---

#### 2. Get Next Page (Simplified)

Simplified GET endpoint for fetching next page.

**Endpoint**: `GET /api/query?key={sessionKey}`

**Query Parameters**:
- `key` (String, required): Session key from previous response

**Response**: Same as Query Data endpoint

**Example**:
```bash
curl -X GET "http://localhost:8090/api/query?key=a3b5c7d9-e1f2-4a5b-8c9d-0e1f2a3b4c5d" \
  -u admin:admin123
```

---

#### 3. Close Session

Manually close a query session.

**Endpoint**: `POST /api/query/close?key={sessionKey}`

**Query Parameters**:
- `key` (String, required): Session key to close

**Response**: 200 OK

**Example**:
```bash
curl -X POST "http://localhost:8090/api/query/close?key=a3b5c7d9-e1f2-4a5b-8c9d-0e1f2a3b4c5d" \
  -u admin:admin123
```

---

#### 4. Close All Sessions

Close all active sessions for the authenticated user.

**Endpoint**: `POST /api/query/close-all`

**Response**: 200 OK

**Example**:
```bash
curl -X POST http://localhost:8090/api/query/close-all \
  -u admin:admin123
```

---

#### 5. Get Active Sessions

Get count of active sessions for the authenticated user.

**Endpoint**: `GET /api/query/active`

**Response**:
```json
3
```

**Example**:
```bash
curl -X GET http://localhost:8090/api/query/active \
  -u admin:admin123
```

---

### User Preferences API

#### 1. Get User Preferences

Get query preferences for the authenticated user.

**Endpoint**: `GET /api/preferences`

**Response**:
```json
{
  "userId": "admin",
  "pageSize": 200,
  "ttlMinutes": 30,
  "maxConcurrentSessions": 15
}
```

**Example**:
```bash
curl -X GET http://localhost:8090/api/preferences \
  -u admin:admin123
```

---

#### 2. Update User Preferences

Update query preferences for the authenticated user.

**Endpoint**: `PUT /api/preferences`

**Request Body**:
```json
{
  "pageSize": 200,
  "ttlMinutes": 30,
  "maxConcurrentSessions": 15
}
```

**Request Fields** (all optional):
- `pageSize` (Integer): Records per page (min: 10, max: 1000)
- `ttlMinutes` (Integer): Session TTL in minutes (max: 60)
- `maxConcurrentSessions` (Integer): Max concurrent sessions

**Response**: Same as Get User Preferences

**Example**:
```bash
curl -X PUT http://localhost:8090/api/preferences \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "pageSize": 250,
    "ttlMinutes": 45
  }'
```

---

## GraphQL API

**Endpoint**: `POST /graphql`

**GraphiQL IDE**: `http://localhost:8090/graphiql` (Development Only)

### Queries

#### 1. Query Data

Start a new query or continue an existing session.

**Query**:
```graphql
query QueryData($connectionId: ID, $tableName: String, $schema: String, $sessionKey: String) {
  queryData(
    connectionId: $connectionId
    tableName: $tableName
    schema: $schema
    sessionKey: $sessionKey
  ) {
    sessionKey
    records
    metadata {
      recordCount
      totalFetched
      hasMore
      expiresAt
      pageSize
    }
  }
}
```

**Variables - New Query**:
```json
{
  "connectionId": 1,
  "tableName": "users",
  "schema": "public"
}
```

**Variables - Continue Session**:
```json
{
  "sessionKey": "a3b5c7d9-e1f2-4a5b-8c9d-0e1f2a3b4c5d"
}
```

**Example using curl**:
```bash
curl -X POST http://localhost:8090/graphql \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "query": "query { queryData(connectionId: 1, tableName: \"users\") { sessionKey records metadata { recordCount hasMore } } }"
  }'
```

---

#### 2. Get Active Sessions

Get count of active sessions.

**Query**:
```graphql
query {
  activeSessions
}
```

**Response**:
```json
{
  "data": {
    "activeSessions": 3
  }
}
```

---

#### 3. Get User Preferences

Get user query preferences.

**Query**:
```graphql
query {
  userPreferences {
    userId
    pageSize
    ttlMinutes
    maxConcurrentSessions
  }
}
```

---

### Mutations

#### 1. Close Session

Close a specific session.

**Mutation**:
```graphql
mutation CloseSession($sessionKey: String!) {
  closeSession(sessionKey: $sessionKey)
}
```

**Variables**:
```json
{
  "sessionKey": "a3b5c7d9-e1f2-4a5b-8c9d-0e1f2a3b4c5d"
}
```

---

#### 2. Close All Sessions

Close all user sessions.

**Mutation**:
```graphql
mutation {
  closeAllSessions
}
```

---

#### 3. Update User Preferences

Update user query preferences.

**Mutation**:
```graphql
mutation UpdatePreferences($input: UserPreferencesInput!) {
  updateUserPreferences(input: $input) {
    userId
    pageSize
    ttlMinutes
    maxConcurrentSessions
  }
}
```

**Variables**:
```json
{
  "input": {
    "pageSize": 200,
    "ttlMinutes": 30,
    "maxConcurrentSessions": 15
  }
}
```

---

## Query Session System

### How It Works

1. **New Query**: Client provides `connectionId` and `tableName`
2. **Server Response**: Returns first page + `sessionKey`
3. **Continuation**: Client sends `sessionKey` for next page
4. **Completion**: When no more data, `sessionKey` is null in response
5. **Auto-Cleanup**: Sessions expire after TTL or auto-close when complete

### Session Lifecycle

```
┌─────────────┐
│ New Query   │
└──────┬──────┘
       │
       ▼
┌─────────────┐         ┌──────────────┐
│ Create      │────────▶│ Return Page  │
│ Session     │         │ + SessionKey │
└─────────────┘         └──────┬───────┘
                               │
                               ▼
                        ┌──────────────┐
                        │ Client Uses  │
                        │ SessionKey   │
                        └──────┬───────┘
                               │
                        ┌──────┴───────┐
                        │              │
                   ┌────▼────┐   ┌────▼────┐
                   │ hasMore │   │ No More │
                   │ = true  │   │ Data    │
                   └────┬────┘   └────┬────┘
                        │             │
                        │             ▼
                        │      ┌──────────────┐
                        │      │ Auto-Close   │
                        │      │ Session      │
                        │      └──────────────┘
                        │
                        └─────▶ Loop

Expiry Events:
- TTL exceeded
- Manual close
- Complete fetch
```

### Caching Strategies

**Caffeine (Default)**:
- In-memory L1 cache
- Iterator kept in memory (fast)
- Best for single-instance deployments
- Max 1000 sessions

**Redis**:
- Distributed L2 cache
- Offset-based (serializable)
- Best for multi-instance deployments
- Requires Redis server

**BOTH**:
- L1 (Caffeine) + L2 (Redis)
- Falls back to Redis if Caffeine evicts
- Best for high-availability systems

**Configuration**:
```yaml
oneapi:
  query:
    session:
      cache-type: CAFFEINE  # or REDIS or BOTH
```

---

## User Preferences

Each user can customize query behavior:

| Setting | Description | Default | Min | Max |
|---------|-------------|---------|-----|-----|
| `pageSize` | Records per page | 100 | 10 | 1000 |
| `ttlMinutes` | Session TTL | 15 | - | 60 |
| `maxConcurrentSessions` | Max sessions | 10 | 1 | - |

**Storage**: Database (H2) with JPA
**Table**: `user_query_preferences`

---

## Error Handling

### HTTP Status Codes

| Code | Description | Scenario |
|------|-------------|----------|
| 200 | OK | Successful request |
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Missing/invalid credentials |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Session not found / Connection not found |
| 429 | Too Many Requests | Max concurrent sessions exceeded |
| 500 | Internal Server Error | Server error |

### Error Response Format

**REST**:
```json
{
  "timestamp": "2024-03-17T15:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Session not found: a3b5c7d9-...",
  "path": "/api/query"
}
```

**GraphQL**:
```json
{
  "errors": [
    {
      "message": "Session not found: a3b5c7d9-...",
      "locations": [{"line": 2, "column": 3}],
      "path": ["queryData"],
      "extensions": {
        "classification": "DataFetchingException"
      }
    }
  ]
}
```

### Common Errors

#### 1. SessionNotFoundException
**Cause**: Session key not found or expired
**Solution**: Start a new query

#### 2. SessionExpiredException
**Cause**: Session exceeded TTL
**Solution**: Start a new query with longer TTL

#### 3. TooManySessionsException
**Cause**: User exceeded max concurrent sessions
**Solution**: Close unused sessions or increase limit

#### 4. ConnectionNotFoundException
**Cause**: Database connection ID not found
**Solution**: Verify connection ID exists

---

## Examples

### Complete Pagination Flow

#### REST API
```bash
# Step 1: Start new query
RESPONSE=$(curl -s -X POST http://localhost:8090/api/query \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "connectionId": 1,
    "tableName": "orders"
  }')

# Extract session key
SESSION_KEY=$(echo $RESPONSE | jq -r '.sessionKey')
echo "Session Key: $SESSION_KEY"
echo "Records: $(echo $RESPONSE | jq '.records | length')"

# Step 2: Get next page
while [ "$SESSION_KEY" != "null" ]; do
  RESPONSE=$(curl -s -X POST http://localhost:8090/api/query \
    -H "Content-Type: application/json" \
    -u admin:admin123 \
    -d "{\"sessionKey\": \"$SESSION_KEY\"}")

  SESSION_KEY=$(echo $RESPONSE | jq -r '.sessionKey')
  RECORDS=$(echo $RESPONSE | jq '.records | length')
  TOTAL=$(echo $RESPONSE | jq '.metadata.totalFetched')

  echo "Fetched $RECORDS records (Total: $TOTAL)"
done

echo "All data fetched!"
```

---

#### GraphQL API
```javascript
// Using JavaScript/Node.js with axios

const axios = require('axios');

const API_URL = 'http://localhost:8090/graphql';
const AUTH = { username: 'admin', password: 'admin123' };

async function fetchAllData(connectionId, tableName) {
  let sessionKey = null;
  let totalRecords = 0;

  // Start new query
  let response = await axios.post(API_URL, {
    query: `
      query QueryData($connectionId: ID, $tableName: String, $sessionKey: String) {
        queryData(connectionId: $connectionId, tableName: $tableName, sessionKey: $sessionKey) {
          sessionKey
          records
          metadata {
            recordCount
            totalFetched
            hasMore
          }
        }
      }
    `,
    variables: { connectionId, tableName }
  }, { auth: AUTH });

  sessionKey = response.data.data.queryData.sessionKey;
  totalRecords = response.data.data.queryData.metadata.totalFetched;
  console.log(`Fetched ${totalRecords} records`);

  // Continue fetching
  while (sessionKey) {
    response = await axios.post(API_URL, {
      query: `
        query QueryData($sessionKey: String) {
          queryData(sessionKey: $sessionKey) {
            sessionKey
            records
            metadata {
              recordCount
              totalFetched
              hasMore
            }
          }
        }
      `,
      variables: { sessionKey }
    }, { auth: AUTH });

    sessionKey = response.data.data.queryData.sessionKey;
    const metadata = response.data.data.queryData.metadata;
    console.log(`Fetched ${metadata.recordCount} records (Total: ${metadata.totalFetched})`);
  }

  console.log('All data fetched!');
}

fetchAllData(1, 'customers');
```

---

### Update Preferences
```bash
# Update user preferences
curl -X PUT http://localhost:8090/api/preferences \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "pageSize": 500,
    "ttlMinutes": 45,
    "maxConcurrentSessions": 20
  }'
```

---

### Multiple Concurrent Queries
```bash
# Query 1: Orders
curl -X POST http://localhost:8090/api/query \
  -u admin:admin123 \
  -d '{"connectionId": 1, "tableName": "orders"}' &

# Query 2: Customers
curl -X POST http://localhost:8090/api/query \
  -u admin:admin123 \
  -d '{"connectionId": 1, "tableName": "customers"}' &

# Query 3: Products
curl -X POST http://localhost:8090/api/query \
  -u admin:admin123 \
  -d '{"connectionId": 1, "tableName": "products"}' &

wait
echo "All queries started!"
```

---

## Additional Resources

- **Swagger UI**: http://localhost:8090/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8090/api-docs
- **GraphiQL IDE**: http://localhost:8090/graphiql
- **H2 Console**: http://localhost:8090/h2-console

---

## Production Checklist

- [ ] Replace in-memory authentication with database/LDAP/OAuth2
- [ ] Enable HTTPS/TLS
- [ ] Disable H2 console and GraphiQL in production
- [ ] Configure proper Redis for distributed caching
- [ ] Set up monitoring and metrics (Actuator + Prometheus)
- [ ] Implement rate limiting
- [ ] Add audit logging
- [ ] Configure CORS for frontend applications
- [ ] Set up database connection pooling (HikariCP)
- [ ] Enable CSRF protection for production
- [ ] Configure proper session management
- [ ] Set up database backups for user preferences
- [ ] Implement proper error logging (ELK stack)

---

**Version**: 0.0.1
**Last Updated**: 2024-03-17
