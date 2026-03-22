import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { DatasourceService, SourceInfoDTO } from '../../../core/services/datasource.service';
import { MetadataService, DomainInfoDTO, EntityInfoDTO, FieldInfoDTO } from '../../../core/services/metadata.service';
import { QueryService, SavedQueryDTO } from '../../../core/services/query.service';

interface QueryColumn {
  field: FieldInfoDTO;
  alias?: string;
  aggregate?: 'SUM' | 'COUNT' | 'AVG' | 'MIN' | 'MAX' | null;
}

interface QueryFilter {
  field: FieldInfoDTO;
  operator: '=' | '!=' | '>' | '<' | '>=' | '<=' | 'LIKE' | 'IN' | 'IS NULL' | 'IS NOT NULL';
  value?: string;
}

@Component({
  selector: 'app-query-builder',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatCheckboxModule
  ],
  templateUrl: './query-builder.html',
  styleUrl: './query-builder.scss',
})
export class QueryBuilder implements OnInit {
  sources = signal<SourceInfoDTO[]>([]);
  domains = signal<DomainInfoDTO[]>([]);
  entities = signal<EntityInfoDTO[]>([]);
  fields = signal<FieldInfoDTO[]>([]);

  selectedSource = signal<SourceInfoDTO | null>(null);
  selectedDomain = signal<DomainInfoDTO | null>(null);
  selectedEntity = signal<EntityInfoDTO | null>(null);

  selectedColumns = signal<QueryColumn[]>([]);
  filters = signal<QueryFilter[]>([]);
  orderByField = signal<FieldInfoDTO | null>(null);
  orderDirection = signal<'ASC' | 'DESC'>('ASC');
  limitValue = signal<number | null>(null);

  generatedSQL = signal<string>('');
  previewData = signal<any[]>([]);
  previewColumns = signal<string[]>([]);
  showPreview = signal(false);
  loading = signal(false);

  queryName = '';
  queryDescription = '';
  isPublic = false;

  aggregateFunctions = ['SUM', 'COUNT', 'AVG', 'MIN', 'MAX'];
  operators = ['=', '!=', '>', '<', '>=', '<=', 'LIKE', 'IN', 'IS NULL', 'IS NOT NULL'];

  constructor(
    private datasourceService: DatasourceService,
    private metadataService: MetadataService,
    private queryService: QueryService,
    private snackBar: MatSnackBar
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

    this.selectedDomain.set(null);
    this.selectedEntity.set(null);
    this.domains.set([]);
    this.entities.set([]);
    this.fields.set([]);
    this.clearQuery();

    this.metadataService.getDomainsBySource(source.id).subscribe({
      next: (domains) => this.domains.set(domains),
      error: (error) => console.error('Error loading domains:', error)
    });
  }

  onDomainChange(): void {
    const domain = this.selectedDomain();
    if (!domain) return;

    this.selectedEntity.set(null);
    this.entities.set([]);
    this.fields.set([]);
    this.clearQuery();

    this.metadataService.getEntitiesByDomain(domain.id).subscribe({
      next: (entities) => this.entities.set(entities),
      error: (error) => console.error('Error loading entities:', error)
    });
  }

  onEntityChange(): void {
    const entity = this.selectedEntity();
    if (!entity) return;

    this.clearQuery();

    this.metadataService.getFieldsByEntity(entity.id).subscribe({
      next: (fields) => this.fields.set(fields),
      error: (error) => console.error('Error loading fields:', error)
    });
  }

  addColumn(field: FieldInfoDTO): void {
    const cols = this.selectedColumns();
    if (!cols.find(c => c.field.id === field.id)) {
      this.selectedColumns.set([...cols, { field, aggregate: null }]);
      this.generateSQL();
    }
  }

  removeColumn(index: number): void {
    const cols = this.selectedColumns();
    cols.splice(index, 1);
    this.selectedColumns.set([...cols]);
    this.generateSQL();
  }

  updateColumnAggregate(index: number, aggregate: string | null): void {
    const cols = this.selectedColumns();
    cols[index].aggregate = aggregate as any;
    this.selectedColumns.set([...cols]);
    this.generateSQL();
  }

  addFilter(): void {
    const availableFields = this.fields();
    if (availableFields.length > 0) {
      this.filters.set([...this.filters(), {
        field: availableFields[0],
        operator: '=',
        value: ''
      }]);
      this.generateSQL();
    }
  }

  removeFilter(index: number): void {
    const currentFilters = this.filters();
    currentFilters.splice(index, 1);
    this.filters.set([...currentFilters]);
    this.generateSQL();
  }

  updateFilter(): void {
    this.generateSQL();
  }

  clearQuery(): void {
    this.selectedColumns.set([]);
    this.filters.set([]);
    this.orderByField.set(null);
    this.limitValue.set(null);
    this.generatedSQL.set('');
    this.previewData.set([]);
    this.showPreview.set(false);
  }

  generateSQL(): void {
    const entity = this.selectedEntity();
    const domain = this.selectedDomain();
    const cols = this.selectedColumns();

    if (!entity || !domain || cols.length === 0) {
      this.generatedSQL.set('');
      return;
    }

    let sql = 'SELECT ';

    // Columns
    const columnParts = cols.map(col => {
      let part = '';
      if (col.aggregate) {
        part = `${col.aggregate}(${col.field.columnName})`;
      } else {
        part = col.field.columnName;
      }
      if (col.alias) {
        part += ` AS ${col.alias}`;
      }
      return part;
    });
    sql += columnParts.join(', ');

    // FROM
    sql += `\nFROM ${domain.schemaName}.${entity.tableName}`;

    // WHERE
    const whereFilters = this.filters().filter(f =>
      f.operator !== 'IS NULL' && f.operator !== 'IS NOT NULL' ? f.value : true
    );
    if (whereFilters.length > 0) {
      sql += '\nWHERE ';
      const filterParts = whereFilters.map(f => {
        if (f.operator === 'IS NULL' || f.operator === 'IS NOT NULL') {
          return `${f.field.columnName} ${f.operator}`;
        }
        return `${f.field.columnName} ${f.operator} '${f.value}'`;
      });
      sql += filterParts.join(' AND ');
    }

    // ORDER BY
    const orderField = this.orderByField();
    if (orderField) {
      sql += `\nORDER BY ${orderField.columnName} ${this.orderDirection()}`;
    }

    // LIMIT
    const limit = this.limitValue();
    if (limit) {
      sql += `\nLIMIT ${limit}`;
    }

    this.generatedSQL.set(sql);
  }

  previewQuery(): void {
    const source = this.selectedSource();
    const sql = this.generatedSQL();

    if (!source || !source.id || !sql) {
      this.snackBar.open('Please build a query first', 'Close', { duration: 3000 });
      return;
    }

    this.loading.set(true);
    this.queryService.previewQuery(source.id, sql).subscribe({
      next: (data) => {
        this.previewData.set(data);
        if (data.length > 0) {
          this.previewColumns.set(Object.keys(data[0]));
        }
        this.showPreview.set(true);
        this.loading.set(false);
        this.snackBar.open(`Preview loaded: ${data.length} rows`, 'Close', { duration: 2000 });
      },
      error: (error) => {
        console.error('Error previewing query:', error);
        this.snackBar.open('Error executing preview', 'Close', { duration: 3000 });
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
      isPublic: this.isPublic
    };

    this.loading.set(true);
    this.queryService.create(query).subscribe({
      next: () => {
        this.snackBar.open('Query saved successfully', 'Close', { duration: 3000 });
        this.queryName = '';
        this.queryDescription = '';
        this.isPublic = false;
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error saving query:', error);
        this.snackBar.open('Error saving query', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }
}
