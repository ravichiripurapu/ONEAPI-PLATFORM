import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { DatasourceService, SourceInfoDTO } from '../../../core/services/datasource.service';
import { MetadataService, DomainInfoDTO, EntityInfoDTO, FieldInfoDTO } from '../../../core/services/metadata.service';
import { QueryService, SavedQueryDTO } from '../../../core/services/query.service';
import { QueryMode, QueryTable, QueryJoin, QueryColumn as QColumn } from '../../../core/models/query.model';

interface TableWithFields {
  table: QueryTable;
  fields: FieldInfoDTO[];
  expanded: boolean;
}

@Component({
  selector: 'app-query-executor',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatCheckboxModule,
    MatBadgeModule,
    MatDialogModule,
    MatExpansionModule
  ],
  templateUrl: './query-executor.html',
  styleUrl: './query-executor.scss',
})
export class QueryExecutor implements OnInit {
  // Tab selection
  selectedTabIndex = signal(0);
  queryMode = signal<QueryMode>('visual');

  // Data sources
  sources = signal<SourceInfoDTO[]>([]);
  selectedSource = signal<SourceInfoDTO | null>(null);

  // === TAB 1: VISUAL QUERY BUILDER ===
  visualQuery = signal({
    tables: [] as TableWithFields[],
    joins: [] as QueryJoin[],
    columns: [] as QColumn[],
    filters: [] as any[],
    groupBy: [] as string[],
    orderBy: [] as any[],
    limit: null as number | null
  });

  availableDomains = signal<DomainInfoDTO[]>([]);
  availableEntities = signal<EntityInfoDTO[]>([]);
  selectedDomain = signal<DomainInfoDTO | null>(null);
  selectedEntityToAdd = signal<EntityInfoDTO | null>(null);

  joinTypes = ['INNER', 'LEFT', 'RIGHT', 'FULL'];
  operators = ['=', '!=', '>', '<', '>=', '<=', 'LIKE', 'IN', 'IS NULL', 'IS NOT NULL'];
  aggregateFunctions = ['COUNT', 'SUM', 'AVG', 'MIN', 'MAX'];

  // === TAB 2: SQL QUERY ===
  sqlQuery = signal({
    sqlText: '',
    name: '',
    description: '',
    isPublic: false
  });

  // === TAB 3: NATURAL LANGUAGE QUERY ===
  nlQuery = signal({
    question: '',
    generatedSQL: '',
    explanation: '',
    confidence: 0
  });

  // Results
  generatedSQL = signal('');
  queryResults = signal<any[]>([]);
  resultColumns = signal<string[]>([]);
  showResults = signal(false);
  loading = signal(false);

  // Save dialog
  showSaveDialog = signal(false);
  queryName = '';
  queryDescription = '';
  isPublic = false;
  isFavorite = false;

  constructor(
    private datasourceService: DatasourceService,
    private metadataService: MetadataService,
    private queryService: QueryService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadSources();
  }

  loadSources(): void {
    this.datasourceService.getAllDatasources().subscribe({
      next: (sources) => this.sources.set(sources),
      error: (error) => {
        console.error('Error loading sources:', error);
        this.snackBar.open('Error loading data sources', 'Close', { duration: 3000 });
      }
    });
  }

  onSourceChange(): void {
    const source = this.selectedSource();
    if (!source || !source.id) return;

    this.resetVisualQuery();
    this.metadataService.getDomainsBySource(source.id).subscribe({
      next: (domains) => this.availableDomains.set(domains),
      error: (error) => console.error('Error loading domains:', error)
    });
  }

  onDomainChange(): void {
    const domain = this.selectedDomain();
    if (!domain) return;

    this.metadataService.getEntitiesByDomain(domain.id).subscribe({
      next: (entities) => this.availableEntities.set(entities),
      error: (error) => console.error('Error loading entities:', error)
    });
  }

  // === TAB 1: VISUAL QUERY BUILDER METHODS ===

  addTable(): void {
    const entity = this.selectedEntityToAdd();
    const domain = this.selectedDomain();

    if (!entity || !domain) {
      this.snackBar.open('Please select a domain and table', 'Close', { duration: 3000 });
      return;
    }

    const query = this.visualQuery();
    const tableExists = query.tables.find(t => t.table.entityId === entity.id);

    if (tableExists) {
      this.snackBar.open('Table already added', 'Close', { duration: 3000 });
      return;
    }

    const newTable: QueryTable = {
      entityId: entity.id,
      schemaName: domain.schemaName,
      tableName: entity.tableName,
      alias: query.tables.length > 0 ? `t${query.tables.length + 1}` : undefined
    };

    // Load fields for this table
    this.metadataService.getFieldsByEntity(entity.id).subscribe({
      next: (fields) => {
        const tableWithFields: TableWithFields = {
          table: newTable,
          fields: fields,
          expanded: true
        };

        this.visualQuery.set({
          ...query,
          tables: [...query.tables, tableWithFields]
        });

        this.generateSQLFromVisual();
        this.snackBar.open(`Added table: ${entity.tableName}`, 'Close', { duration: 2000 });
      },
      error: (error) => {
        console.error('Error loading fields:', error);
        this.snackBar.open('Error loading table fields', 'Close', { duration: 3000 });
      }
    });
  }

  removeTable(index: number): void {
    const query = this.visualQuery();
    const removedTable = query.tables[index];

    // Remove joins related to this table
    const tableIdentifier = removedTable.table.alias || removedTable.table.tableName;
    const newJoins = query.joins.filter(j =>
      j.sourceTable !== tableIdentifier && j.targetTable !== tableIdentifier
    );

    // Remove columns from this table
    const newColumns = query.columns.filter(c => c.table !== tableIdentifier);

    // Remove filters from this table
    const newFilters = query.filters.filter(f => f.table !== tableIdentifier);

    const newTables = [...query.tables];
    newTables.splice(index, 1);

    this.visualQuery.set({
      ...query,
      tables: newTables,
      joins: newJoins,
      columns: newColumns,
      filters: newFilters
    });

    this.generateSQLFromVisual();
  }

  addJoin(): void {
    const query = this.visualQuery();

    if (query.tables.length < 2) {
      this.snackBar.open('Add at least 2 tables to create a join', 'Close', { duration: 3000 });
      return;
    }

    const firstTable = query.tables[0].table.alias || query.tables[0].table.tableName;
    const secondTable = query.tables[1].table.alias || query.tables[1].table.tableName;
    const firstColumn = query.tables[0].fields[0]?.columnName || '';
    const secondColumn = query.tables[1].fields[0]?.columnName || '';

    const newJoin: QueryJoin = {
      sourceTable: firstTable,
      sourceColumn: firstColumn,
      targetTable: secondTable,
      targetColumn: secondColumn,
      joinType: 'INNER'
    };

    this.visualQuery.set({
      ...query,
      joins: [...query.joins, newJoin]
    });

    this.generateSQLFromVisual();
  }

  removeJoin(index: number): void {
    const query = this.visualQuery();
    const newJoins = [...query.joins];
    newJoins.splice(index, 1);

    this.visualQuery.set({
      ...query,
      joins: newJoins
    });

    this.generateSQLFromVisual();
  }

  updateJoin(): void {
    this.generateSQLFromVisual();
  }

  addColumn(table: TableWithFields, field: FieldInfoDTO): void {
    const query = this.visualQuery();
    const tableIdentifier = table.table.alias || table.table.tableName;

    const columnExists = query.columns.find(c =>
      c.table === tableIdentifier && c.column === field.columnName
    );

    if (columnExists) {
      this.snackBar.open('Column already added', 'Close', { duration: 2000 });
      return;
    }

    const newColumn: QColumn = {
      table: tableIdentifier,
      column: field.columnName
    };

    this.visualQuery.set({
      ...query,
      columns: [...query.columns, newColumn]
    });

    this.generateSQLFromVisual();
  }

  removeColumn(index: number): void {
    const query = this.visualQuery();
    const newColumns = [...query.columns];
    newColumns.splice(index, 1);

    this.visualQuery.set({
      ...query,
      columns: newColumns
    });

    this.generateSQLFromVisual();
  }

  updateColumnAggregate(index: number, aggregate: string | null): void {
    const query = this.visualQuery();
    const newColumns = [...query.columns];
    newColumns[index] = {
      ...newColumns[index],
      aggregate: aggregate as any
    };

    this.visualQuery.set({
      ...query,
      columns: newColumns
    });

    this.generateSQLFromVisual();
  }

  addFilter(): void {
    const query = this.visualQuery();

    if (query.tables.length === 0) {
      this.snackBar.open('Add tables first', 'Close', { duration: 3000 });
      return;
    }

    const firstTable = query.tables[0];
    const tableIdentifier = firstTable.table.alias || firstTable.table.tableName;
    const firstField = firstTable.fields[0];

    if (!firstField) {
      this.snackBar.open('No fields available', 'Close', { duration: 3000 });
      return;
    }

    const newFilter = {
      table: tableIdentifier,
      field: firstField.columnName,
      operator: '=',
      value: ''
    };

    this.visualQuery.set({
      ...query,
      filters: [...query.filters, newFilter]
    });

    this.generateSQLFromVisual();
  }

  removeFilter(index: number): void {
    const query = this.visualQuery();
    const newFilters = [...query.filters];
    newFilters.splice(index, 1);

    this.visualQuery.set({
      ...query,
      filters: newFilters
    });

    this.generateSQLFromVisual();
  }

  updateFilter(): void {
    this.generateSQLFromVisual();
  }

  resetVisualQuery(): void {
    this.visualQuery.set({
      tables: [],
      joins: [],
      columns: [],
      filters: [],
      groupBy: [],
      orderBy: [],
      limit: null
    });
    this.generatedSQL.set('');
    this.showResults.set(false);
  }

  generateSQLFromVisual(): void {
    const query = this.visualQuery();

    if (query.tables.length === 0 || query.columns.length === 0) {
      this.generatedSQL.set('');
      return;
    }

    let sql = 'SELECT ';

    // Columns
    const columnParts = query.columns.map(col => {
      let part = `${col.table}.${col.column}`;
      if (col.aggregate) {
        part = `${col.aggregate}(${col.table}.${col.column})`;
      }
      if (col.alias) {
        part += ` AS ${col.alias}`;
      }
      return part;
    });
    sql += columnParts.join(', ');

    // FROM clause
    const firstTable = query.tables[0].table;
    sql += `\nFROM ${firstTable.schemaName}.${firstTable.tableName}`;
    if (firstTable.alias) {
      sql += ` ${firstTable.alias}`;
    }

    // JOINs
    query.joins.forEach(join => {
      sql += `\n${join.joinType} JOIN `;

      // Find the target table
      const targetTable = query.tables.find(t =>
        (t.table.alias || t.table.tableName) === join.targetTable
      );

      if (targetTable) {
        sql += `${targetTable.table.schemaName}.${targetTable.table.tableName}`;
        if (targetTable.table.alias) {
          sql += ` ${targetTable.table.alias}`;
        }
        sql += ` ON ${join.sourceTable}.${join.sourceColumn} = ${join.targetTable}.${join.targetColumn}`;
      }
    });

    // WHERE
    if (query.filters.length > 0) {
      sql += '\nWHERE ';
      const filterParts = query.filters.map(f => {
        if (f.operator === 'IS NULL' || f.operator === 'IS NOT NULL') {
          return `${f.table}.${f.field} ${f.operator}`;
        }
        return `${f.table}.${f.field} ${f.operator} '${f.value}'`;
      });
      sql += filterParts.join(' AND ');
    }

    // LIMIT
    if (query.limit) {
      sql += `\nLIMIT ${query.limit}`;
    }

    this.generatedSQL.set(sql);
  }

  // === TAB 2: SQL QUERY METHODS ===

  formatSQL(): void {
    const sql = this.sqlQuery();
    // Basic SQL formatting
    const formatted = sql.sqlText
      .replace(/\bSELECT\b/gi, '\nSELECT')
      .replace(/\bFROM\b/gi, '\nFROM')
      .replace(/\bWHERE\b/gi, '\nWHERE')
      .replace(/\bJOIN\b/gi, '\nJOIN')
      .replace(/\bON\b/gi, '\nON')
      .replace(/\bGROUP BY\b/gi, '\nGROUP BY')
      .replace(/\bORDER BY\b/gi, '\nORDER BY')
      .replace(/\bLIMIT\b/gi, '\nLIMIT')
      .trim();

    this.sqlQuery.set({
      ...sql,
      sqlText: formatted
    });
    this.generatedSQL.set(formatted);
  }

  // === TAB 3: NATURAL LANGUAGE QUERY METHODS ===

  askQuestion(): void {
    const nl = this.nlQuery();
    const source = this.selectedSource();

    if (!nl.question || !source || !source.id) {
      this.snackBar.open('Please enter a question and select a datasource', 'Close', { duration: 3000 });
      return;
    }

    this.loading.set(true);

    // Call backend API to convert natural language to SQL
    this.queryService.convertNaturalLanguageToSQL({
      question: nl.question,
      datasourceId: source.id,
      context: {
        tables: [], // Could be populated with selected tables from visual query
        previousQueries: [],
        hint: ''
      }
    }).subscribe({
      next: (response) => {
        this.nlQuery.set({
          ...nl,
          generatedSQL: response.generatedSQL,
          explanation: response.explanation,
          confidence: response.confidence
        });

        this.generatedSQL.set(response.generatedSQL);
        this.loading.set(false);

        if (response.warnings && response.warnings.length > 0) {
          this.snackBar.open(
            `SQL generated with ${response.warnings.length} warning(s). Confidence: ${(response.confidence * 100).toFixed(0)}%`,
            'Close',
            { duration: 4000 }
          );
        } else {
          this.snackBar.open(
            `SQL generated! Confidence: ${(response.confidence * 100).toFixed(0)}%`,
            'Close',
            { duration: 3000 }
          );
        }
      },
      error: (error) => {
        console.error('Error generating SQL from natural language:', error);
        this.loading.set(false);
        this.snackBar.open(
          `Failed to generate SQL: ${error.error?.message || error.message || 'Unknown error'}`,
          'Close',
          { duration: 5000 }
        );
      }
    });
  }

  useGeneratedSQL(): void {
    const nl = this.nlQuery();
    this.sqlQuery.set({
      ...this.sqlQuery(),
      sqlText: nl.generatedSQL
    });
    this.selectedTabIndex.set(1); // Switch to SQL tab
    this.snackBar.open('SQL copied to SQL Editor tab', 'Close', { duration: 2000 });
  }

  // === COMMON METHODS ===

  executeQuery(): void {
    const source = this.selectedSource();
    const sql = this.generatedSQL();

    if (!source || !source.id || !sql) {
      this.snackBar.open('Please build a query and select a datasource', 'Close', { duration: 3000 });
      return;
    }

    this.loading.set(true);
    this.queryService.previewQuery(source.id, sql).subscribe({
      next: (data) => {
        this.queryResults.set(data);
        if (data.length > 0) {
          this.resultColumns.set(Object.keys(data[0]));
        }
        this.showResults.set(true);
        this.loading.set(false);
        this.snackBar.open(`Query executed: ${data.length} rows`, 'Close', { duration: 2000 });
      },
      error: (error) => {
        console.error('Error executing query:', error);
        this.snackBar.open(`Error: ${error.error?.message || 'Failed to execute query'}`, 'Close', { duration: 5000 });
        this.loading.set(false);
      }
    });
  }

  saveQuery(): void {
    const source = this.selectedSource();
    const sql = this.generatedSQL();

    if (!this.queryName || !source || !source.id || !sql) {
      this.snackBar.open('Please provide query name and build a query', 'Close', { duration: 3000 });
      return;
    }

    const query: SavedQueryDTO = {
      name: this.queryName,
      description: this.queryDescription,
      queryText: sql,
      datasourceId: source.id,
      isPublic: this.isPublic,
      isFavorite: this.isFavorite
    };

    this.loading.set(true);
    this.queryService.create(query).subscribe({
      next: () => {
        this.snackBar.open('Query saved successfully', 'Close', { duration: 3000 });
        this.queryName = '';
        this.queryDescription = '';
        this.isPublic = false;
        this.isFavorite = false;
        this.showSaveDialog.set(false);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error saving query:', error);
        this.snackBar.open('Error saving query', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  getTableFields(table: TableWithFields, fieldName: string): FieldInfoDTO | undefined {
    return table.fields.find(f => f.columnName === fieldName);
  }

  getFieldsForTable(tableIdentifier: string): FieldInfoDTO[] {
    const query = this.visualQuery();
    const table = query.tables.find(t =>
      (t.table.alias || t.table.tableName) === tableIdentifier
    );
    return table?.fields || [];
  }
}
