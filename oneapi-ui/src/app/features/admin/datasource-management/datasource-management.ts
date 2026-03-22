import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatTreeModule, MatTreeNestedDataSource } from '@angular/material/tree';
import { NestedTreeControl } from '@angular/cdk/tree';
import { DatasourceService, SourceInfoDTO } from '../../../core/services/datasource.service';
import { MetadataService, DomainInfoDTO, EntityInfoDTO, FieldInfoDTO } from '../../../core/services/metadata.service';
import { environment } from '../../../../environments/environment';
import { forkJoin } from 'rxjs';
import { MetadataTreeExplorer } from '../../../shared/components/metadata-tree-explorer/metadata-tree-explorer';

interface DiscoveryResult {
  sourceId: number;
  domainsCount: number;
  entitiesCount: number;
  fieldsCount: number;
  discoveryTimeMs: number;
}

interface DiscoveryTreeNode {
  name: string;
  type: 'source' | 'domain' | 'entity';
  id: number;
  data?: any;
  children?: DiscoveryTreeNode[];
}

@Component({
  selector: 'app-datasource-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatTreeModule,
    MetadataTreeExplorer
  ],
  templateUrl: './datasource-management.html',
  styleUrl: './datasource-management.scss',
})
export class DatasourceManagement implements OnInit {
  datasources = signal<SourceInfoDTO[]>([]);
  loading = signal(false);
  displayedColumns = ['name', 'type', 'host', 'port', 'database', 'active', 'actions'];
  showCreateForm = signal(false);
  editingSource = signal<SourceInfoDTO | null>(null);
  viewingSource = signal<SourceInfoDTO | null>(null);
  testingSource = signal<number | null>(null);
  testingConnection = signal(false);
  
  // Discovery-related signals
  discoveringSource = signal<SourceInfoDTO | null>(null);
  discoveringSourceId = signal<number | null>(null);
  discoveryLoading = signal(false);
  discoveryError = signal<string | null>(null);
  discoveryResult = signal<DiscoveryResult | null>(null);
  discoveredDomains = signal<DomainInfoDTO[]>([]);
  selectedDiscoveryNode = signal<DiscoveryTreeNode | null>(null);
  selectedDiscoveryDomain = signal<DomainInfoDTO | null>(null);
  selectedDiscoveryEntity = signal<EntityInfoDTO | null>(null);
  discoveryEntityFields = signal<FieldInfoDTO[]>([]);
  discoveryFieldColumns = ['columnName', 'dataType', 'nullable', 'key', 'default'];

  // Resync-related signals
  resyncingSourceId = signal<number | null>(null);

  // Discovery tree
  discoveryTreeControl = new NestedTreeControl<DiscoveryTreeNode>(node => node.children);
  discoveryTreeDataSource = new MatTreeNestedDataSource<DiscoveryTreeNode>();

  // Resizable panel
  treePanelWidth = signal(30); // Default 30%
  isResizing = signal(false);
  private startX = 0;
  private startWidth = 0;

  newSource: SourceInfoDTO = {
    name: '',
    type: 'H2',
    database: '',
    username: '',
    password: '',
    host: 'localhost',
    port: 9092,
    active: true
  };

  databaseTypes = ['H2', 'POSTGRESQL', 'MYSQL', 'ORACLE', 'SQLSERVER'];

  constructor(
    private datasourceService: DatasourceService,
    private metadataService: MetadataService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loadDatasources();
  }

  loadDatasources(): void {
    this.loading.set(true);
    this.datasourceService.getAllDatasources().subscribe({
      next: (datasources) => {
        this.datasources.set(datasources);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading datasources:', error);
        this.snackBar.open('Error loading datasources', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  openCreateDialog(): void {
    this.editingSource.set(null);
    this.newSource = {
      name: '',
      type: 'H2',
      database: '',
      username: '',
      password: '',
      host: 'localhost',
      port: 9092,
      active: true
    };
    this.showCreateForm.set(true);
  }

  openEditDialog(ds: SourceInfoDTO): void {
    this.editingSource.set(ds);
    this.newSource = { ...ds };
    this.showCreateForm.set(true);
  }

  viewSource(ds: SourceInfoDTO): void {
    this.viewingSource.set(ds);
  }

  closeView(): void {
    this.viewingSource.set(null);
  }

  editFromView(): void {
    const viewing = this.viewingSource();
    if (viewing) {
      this.viewingSource.set(null);
      this.openEditDialog(viewing);
    }
  }

  cancelForm(): void {
    this.showCreateForm.set(false);
    this.editingSource.set(null);
  }

  saveSource(): void {
    console.log('saveSource called');
    console.log('newSource:', this.newSource);
    console.log('editingSource:', this.editingSource());

    // Validate required fields
    if (!this.newSource.name || !this.newSource.type || !this.newSource.database) {
      this.snackBar.open('Please fill in all required fields (Name, Type, Database)', 'Close', { duration: 5000 });
      return;
    }

    if (!this.newSource.host || this.newSource.host.trim() === '') {
      this.snackBar.open('Host is required', 'Close', { duration: 5000 });
      return;
    }

    if (!this.newSource.port) {
      this.snackBar.open('Port is required', 'Close', { duration: 5000 });
      return;
    }

    const sourceData = { ...this.newSource };
    console.log('Sending source data:', sourceData);

    const editing = this.editingSource();
    if (editing && editing.id) {
      console.log('Updating existing source:', editing.id);
      this.datasourceService.updateDatasource(editing.id, sourceData).subscribe({
        next: (response) => {
          console.log('Update successful:', response);
          this.snackBar.open('Source updated successfully', 'Close', { duration: 3000 });
          this.showCreateForm.set(false);
          this.loadDatasources();
        },
        error: (error) => {
          console.error('Error updating source:', error);
          console.error('Error details:', error.error);
          console.error('Error status:', error.status);

          let errorMsg = 'Unknown error occurred';

          // Handle validation errors (400 Bad Request)
          if (error.status === 400 && error.error) {
            if (error.error.errors && Array.isArray(error.error.errors)) {
              // Spring Boot validation errors format
              errorMsg = error.error.errors.map((e: any) => e.defaultMessage || e.message).join(', ');
            } else if (error.error.message) {
              errorMsg = error.error.message;
            }
          }
          // Handle other HTTP errors
          else if (error.error?.message) {
            errorMsg = error.error.message;
          } else if (error.message) {
            errorMsg = error.message;
          }

          this.snackBar.open('Error updating source: ' + errorMsg, 'Close', { duration: 5000 });
        }
      });
    } else {
      console.log('Creating new source');
      this.datasourceService.createDatasource(sourceData).subscribe({
        next: (response) => {
          console.log('Create successful:', response);
          this.snackBar.open('Source created successfully', 'Close', { duration: 3000 });
          this.showCreateForm.set(false);
          this.loadDatasources();
        },
        error: (error) => {
          console.error('Error creating source:', error);
          console.error('Error details:', error.error);
          console.error('Error status:', error.status);

          let errorMsg = 'Unknown error occurred';

          // Handle validation errors (400 Bad Request)
          if (error.status === 400 && error.error) {
            if (error.error.errors && Array.isArray(error.error.errors)) {
              // Spring Boot validation errors format
              errorMsg = error.error.errors.map((e: any) => e.defaultMessage || e.message).join(', ');
            } else if (error.error.message) {
              errorMsg = error.error.message;
            }
          }
          // Handle other HTTP errors
          else if (error.error?.message) {
            errorMsg = error.error.message;
          } else if (error.message) {
            errorMsg = error.message;
          }

          this.snackBar.open('Error creating source: ' + errorMsg, 'Close', { duration: 5000 });
        }
      });
    }
  }

  testConnection(ds: SourceInfoDTO): void {
    if (!ds.id) return;
    this.testingSource.set(ds.id);
    this.datasourceService.testConnection(ds.id).subscribe({
      next: (result) => {
        this.testingSource.set(null);
        if (result.success) {
          const timeMsg = result.connectionTime ? ` (${result.connectionTime}ms)` : '';
          this.snackBar.open(`Connection successful!${timeMsg}`, 'Close', { duration: 3000 });
        } else {
          this.snackBar.open(`Connection failed: ${result.message}`, 'Close', { duration: 5000 });
        }
      },
      error: (error) => {
        console.error('Error testing connection:', error);
        this.testingSource.set(null);
        this.snackBar.open('Error testing connection', 'Close', { duration: 3000 });
      }
    });
  }

  testConnectionForm(): void {
    this.testingConnection.set(true);
    this.datasourceService.testConnectionWithoutSaving(this.newSource).subscribe({
      next: (result) => {
        this.testingConnection.set(false);
        if (result.success) {
          const timeMsg = result.connectionTime ? ` (${result.connectionTime}ms)` : '';
          this.snackBar.open(`Connection successful!${timeMsg}`, 'Close', { duration: 3000 });
        } else {
          this.snackBar.open(`Connection failed: ${result.message}`, 'Close', { duration: 5000 });
        }
      },
      error: (error) => {
        console.error('Error testing connection:', error);
        this.testingConnection.set(false);
        this.snackBar.open('Error testing connection', 'Close', { duration: 3000 });
      }
    });
  }

  discoverSource(ds: SourceInfoDTO): void {
    if (!ds.id) return;
    this.discoveringSourceId.set(ds.id);
    this.discoveryLoading.set(true);
    this.discoveryError.set(null);
    
    // Call discovery endpoint
    this.http.post<DiscoveryResult>(`${environment.apiUrl}/v1/metadata/discover/${ds.id}`, {}).subscribe({
      next: (result) => {
        this.discoveryResult.set(result);
        this.discoveringSource.set(ds);
        this.discoveringSourceId.set(null);
        this.loadDiscoveredMetadata(ds.id!);
      },
      error: (error) => {
        console.error('Error discovering metadata:', error);
        this.discoveryError.set(error.error?.message || 'Failed to discover metadata');
        this.discoveryLoading.set(false);
        this.discoveringSourceId.set(null);
        this.snackBar.open('Error discovering metadata', 'Close', { duration: 3000 });
      }
    });
  }

  loadDiscoveredMetadata(sourceId: number): void {
    this.metadataService.getDomainsBySource(sourceId).subscribe({
      next: (domains) => {
        this.discoveredDomains.set(domains);
        this.buildDiscoveryTree(domains, sourceId);
        this.discoveryLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading discovered metadata:', error);
        this.discoveryError.set('Failed to load discovered metadata');
        this.discoveryLoading.set(false);
      }
    });
  }

  buildDiscoveryTree(domains: DomainInfoDTO[], sourceId: number): void {
    const source = this.discoveringSource();
    if (!source) return;

    if (domains.length === 0) {
      // No domains, just create empty source node
      const sourceNode: DiscoveryTreeNode = {
        name: source.name,
        type: 'source',
        id: source.id!,
        data: source,
        children: []
      };
      this.discoveryTreeDataSource.data = [sourceNode];
      this.discoveryTreeControl.expand(sourceNode);
      return;
    }

    // Load all entities for all domains in parallel
    const entityRequests = domains.map(domain =>
      this.metadataService.getEntitiesByDomain(domain.id)
    );

    forkJoin(entityRequests).subscribe({
      next: (allEntities) => {
        // Create source root node
        const sourceNode: DiscoveryTreeNode = {
          name: source.name,
          type: 'source',
          id: source.id!,
          data: source,
          children: []
        };

        // Create domain nodes with their entities
        domains.forEach((domain, index) => {
          const entities = allEntities[index];

          const domainNode: DiscoveryTreeNode = {
            name: domain.schemaName,
            type: 'domain',
            id: domain.id,
            data: domain,
            children: entities.map(entity => ({
              name: entity.tableName,
              type: 'entity',
              id: entity.id,
              data: entity,
              children: undefined
            }))
          };

          sourceNode.children!.push(domainNode);
        });

        // Set the complete tree data
        this.discoveryTreeDataSource.data = [sourceNode];

        // Expand source by default
        setTimeout(() => {
          this.discoveryTreeControl.expand(sourceNode);
        }, 0);
      },
      error: (error) => {
        console.error('Error loading entities:', error);
        // Still create the tree with domains even if entities fail to load
        const sourceNode: DiscoveryTreeNode = {
          name: source.name,
          type: 'source',
          id: source.id!,
          data: source,
          children: domains.map(domain => ({
            name: domain.schemaName,
            type: 'domain',
            id: domain.id,
            data: domain,
            children: []
          }))
        };
        this.discoveryTreeDataSource.data = [sourceNode];
        this.discoveryTreeControl.expand(sourceNode);
      }
    });
  }

  hasDiscoveryChild = (_: number, node: DiscoveryTreeNode) => 
    node.children !== undefined && node.children.length >= 0;

  onDiscoveryNodeClick(node: DiscoveryTreeNode): void {
    console.log('=== onDiscoveryNodeClick ===');
    console.log('Node clicked:', node);
    console.log('Node type:', node.type);

    this.selectedDiscoveryNode.set(node);

    if (node.type === 'source') {
      console.log('Source selected:', node.data);
      // Show source details
      this.selectedDiscoveryDomain.set(null);
      this.selectedDiscoveryEntity.set(null);
      this.discoveryEntityFields.set([]);
    } else if (node.type === 'domain') {
      console.log('Domain selected:', node.data);
      this.selectedDiscoveryDomain.set(node.data);
      this.selectedDiscoveryEntity.set(null);
      this.discoveryEntityFields.set([]);
    } else if (node.type === 'entity') {
      console.log('Entity selected:', node.data);
      this.selectedDiscoveryDomain.set(null);
      this.selectedDiscoveryEntity.set(null);
      this.selectedDiscoveryEntity.set(node.data);
      this.loadDiscoveryEntityFields(node.data.id);
    }
  }

  loadDiscoveryEntityFields(entityId: number): void {
    this.metadataService.getFieldsByEntity(entityId).subscribe({
      next: (fields) => {
        this.discoveryEntityFields.set(fields);
      },
      error: (error) => {
        console.error('Error loading entity fields:', error);
      }
    });
  }

  getDiscoveryNodeIcon(node: DiscoveryTreeNode): string {
    if (node.type === 'source') return 'storage';
    if (node.type === 'domain') return 'schema';
    return 'table_chart';
  }

  closeDiscovery(): void {
    this.discoveringSource.set(null);
    this.discoveryResult.set(null);
    this.discoveredDomains.set([]);
    this.selectedDiscoveryNode.set(null);
    this.selectedDiscoveryDomain.set(null);
    this.selectedDiscoveryEntity.set(null);
    this.discoveryEntityFields.set([]);
    this.discoveryError.set(null);
  }

  retryDiscovery(): void {
    const source = this.discoveringSource();
    if (source) {
      this.closeDiscovery();
      this.discoverSource(source);
    }
  }

  resyncSource(ds: SourceInfoDTO): void {
    if (!ds.id) return;
    this.resyncingSourceId.set(ds.id);

    this.http.post<any>(`${environment.apiUrl}/v1/metadata/sync/${ds.id}`, {}).subscribe({
      next: (result) => {
        this.resyncingSourceId.set(null);
        const msg = `Resync completed: ${result.newSchemas || 0} new schemas, ${result.newTables || 0} new tables, ${result.newColumns || 0} new columns`;
        this.snackBar.open(msg, 'Close', { duration: 5000 });

        // Reload the discovered metadata if this source is currently being viewed
        if (this.discoveringSource()?.id === ds.id && ds.id) {
          this.loadDiscoveredMetadata(ds.id);
        }
      },
      error: (error) => {
        console.error('Error resyncing metadata:', error);
        this.resyncingSourceId.set(null);
        this.snackBar.open('Error resyncing metadata', 'Close', { duration: 3000 });
      }
    });
  }

  deleteDatasource(ds: SourceInfoDTO): void {
    if (!ds.id) return;
    if (confirm(`Delete source: ${ds.name}?`)) {
      this.datasourceService.deleteDatasource(ds.id).subscribe({
        next: () => {
          this.snackBar.open('Source deleted', 'Close', { duration: 3000 });
          this.loadDatasources();
        },
        error: (error) => {
          console.error('Error deleting source:', error);
          this.snackBar.open('Error deleting source', 'Close', { duration: 3000 });
        }
      });
    }
  }

  formatDate(date?: any): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleString();
  }

  // Resizable panel methods
  startResize(event: MouseEvent): void {
    event.preventDefault();
    this.isResizing.set(true);
    this.startX = event.clientX;
    this.startWidth = this.treePanelWidth();

    const onMouseMove = (e: MouseEvent) => this.onResize(e);
    const onMouseUp = () => this.stopResize(onMouseMove, onMouseUp);

    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  }

  private onResize(event: MouseEvent): void {
    if (!this.isResizing()) return;

    const container = document.querySelector('.explorer-container');
    if (!container) return;

    const containerWidth = container.clientWidth;
    const delta = event.clientX - this.startX;
    const deltaPercent = (delta / containerWidth) * 100;
    const newWidth = this.startWidth + deltaPercent;

    // Constrain width between 20% and 50%
    const constrainedWidth = Math.max(20, Math.min(50, newWidth));
    this.treePanelWidth.set(constrainedWidth);
  }

  private stopResize(onMouseMove: (e: MouseEvent) => void, onMouseUp: () => void): void {
    this.isResizing.set(false);
    document.removeEventListener('mousemove', onMouseMove);
    document.removeEventListener('mouseup', onMouseUp);
  }
}
