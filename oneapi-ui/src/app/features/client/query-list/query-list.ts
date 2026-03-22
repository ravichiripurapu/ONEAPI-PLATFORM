import { Component, OnInit, signal, Optional } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatMenuModule } from '@angular/material/menu';
import { QueryService, SavedQueryDTO, QueryResponse } from '../../../core/services/query.service';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { ClientNavigationService } from '../dashboard/dashboard';

interface QueryExecution {
  query: SavedQueryDTO;
  results: any[];
  columns: string[];
  sessionKey: string | null;
  loading: boolean;
  hasMore: boolean;
  totalFetched: number;
  expanded: boolean;
}

@Component({
  selector: 'app-query-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTableModule,
    MatTooltipModule,
    MatChipsModule,
    MatExpansionModule,
    MatMenuModule,
    ScrollingModule
  ],
  templateUrl: './query-list.html',
  styleUrl: './query-list.scss',
})
export class QueryList implements OnInit {
  queries = signal<SavedQueryDTO[]>([]);
  loading = signal(false);
  executions = signal<Map<number, QueryExecution>>(new Map());

  constructor(
    private queryService: QueryService,
    private snackBar: MatSnackBar,
    @Optional() private navigationService: ClientNavigationService
  ) {}

  ngOnInit(): void {
    this.loadQueries();
  }

  loadQueries(): void {
    this.loading.set(true);
    this.queryService.getMyQueries().subscribe({
      next: (queries) => {
        this.queries.set(queries);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading queries:', error);
        this.snackBar.open('Error loading queries', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  toggleFavorite(query: SavedQueryDTO): void {
    if (!query.id) return;
    this.queryService.toggleFavorite(query.id).subscribe({
      next: (updated) => {
        query.isFavorite = updated.isFavorite;
        this.snackBar.open(
          updated.isFavorite ? 'Added to favorites' : 'Removed from favorites',
          'Close',
          { duration: 2000 }
        );
      },
      error: (error) => {
        console.error('Error toggling favorite:', error);
        this.snackBar.open('Error updating favorite status', 'Close', { duration: 3000 });
      }
    });
  }

  executeQuery(query: SavedQueryDTO): void {
    if (!query.id) return;

    const execMap = this.executions();
    let execution = execMap.get(query.id);

    if (!execution) {
      execution = {
        query,
        results: [],
        columns: [],
        sessionKey: null,
        loading: false,
        hasMore: false,
        totalFetched: 0,
        expanded: true
      };
      execMap.set(query.id, execution);
      this.executions.set(new Map(execMap));
    }

    execution.loading = true;
    execution.expanded = true;
    this.executions.set(new Map(execMap));

    const startTime = Date.now();

    this.queryService.executeQuery(query.id).subscribe({
      next: (response: QueryResponse) => {
        const execTime = Date.now() - startTime;
        this.queryService.recordExecution(query.id!, execTime).subscribe();

        execution!.results = response.records;
        execution!.sessionKey = response.sessionKey;
        execution!.hasMore = response.metadata.hasMore;
        execution!.totalFetched = response.metadata.totalFetched;
        execution!.loading = false;

        if (response.records.length > 0) {
          execution!.columns = Object.keys(response.records[0]);
        }

        this.executions.set(new Map(execMap));
        this.snackBar.open(`Loaded ${response.records.length} rows`, 'Close', { duration: 2000 });
      },
      error: (error) => {
        console.error('Error executing query:', error);
        execution!.loading = false;
        this.executions.set(new Map(execMap));
        this.snackBar.open('Error executing query', 'Close', { duration: 3000 });
      }
    });
  }

  loadMore(query: SavedQueryDTO): void {
    if (!query.id) return;

    const execMap = this.executions();
    const execution = execMap.get(query.id);
    if (!execution || !execution.sessionKey) return;

    execution.loading = true;
    this.executions.set(new Map(execMap));

    this.queryService.executeQuery(query.id, execution.sessionKey).subscribe({
      next: (response: QueryResponse) => {
        execution.results = [...execution.results, ...response.records];
        execution.sessionKey = response.sessionKey;
        execution.hasMore = response.metadata.hasMore;
        execution.totalFetched = response.metadata.totalFetched;
        execution.loading = false;

        this.executions.set(new Map(execMap));
        this.snackBar.open(`Loaded ${response.records.length} more rows`, 'Close', { duration: 2000 });
      },
      error: (error) => {
        console.error('Error loading more results:', error);
        execution.loading = false;
        this.executions.set(new Map(execMap));
        this.snackBar.open('Error loading more results', 'Close', { duration: 3000 });
      }
    });
  }

  getExecution(queryId: number): QueryExecution | undefined {
    return this.executions().get(queryId);
  }

  toggleExecution(query: SavedQueryDTO): void {
    if (!query.id) return;

    const execMap = this.executions();
    const execution = execMap.get(query.id);

    if (execution) {
      execution.expanded = !execution.expanded;
      this.executions.set(new Map(execMap));
    }
  }

  deleteQuery(query: SavedQueryDTO): void {
    if (!query.id) return;

    if (confirm(`Delete query: ${query.name}?`)) {
      this.queryService.delete(query.id).subscribe({
        next: () => {
          this.snackBar.open('Query deleted', 'Close', { duration: 3000 });
          // Remove from executions
          const execMap = this.executions();
          execMap.delete(query.id!);
          this.executions.set(new Map(execMap));
          this.loadQueries();
        },
        error: (error) => {
          console.error('Error deleting query:', error);
          this.snackBar.open('Error deleting query', 'Close', { duration: 3000 });
        }
      });
    }
  }

  formatDate(date?: string): string {
    if (!date) return 'Never';
    return new Date(date).toLocaleString();
  }

  navigateToQueryBuilder(): void {
    if (this.navigationService) {
      this.navigationService.navigateToBuilder();
    }
  }
}
