export interface Datasource {
  id: number;
  name: string;
  type: 'H2' | 'POSTGRES' | 'MYSQL' | 'SQLSERVER';
  host: string;
  port: number;
  database: string;
  username: string;
  password?: string;
  schema?: string;
  additionalParams?: string;
  status?: 'ACTIVE' | 'INACTIVE';
  createdBy?: string;
  createdDate?: Date;
  lastModifiedBy?: string;
  lastModifiedDate?: Date;
}

export interface DatasourceTest {
  success: boolean;
  message: string;
  error?: string;
}

export interface Domain {
  id: number;
  name: string;
  sourceId: number;
  entities: Entity[];
}

export interface Entity {
  id: number;
  name: string;
  schema?: string;
  domainId: number;
  fields: Field[];
  recordCount?: number;
}

export interface Field {
  id: number;
  name: string;
  dataType: string;
  nullable: boolean;
  primaryKey: boolean;
  foreignKey: boolean;
  entityId: number;
}
