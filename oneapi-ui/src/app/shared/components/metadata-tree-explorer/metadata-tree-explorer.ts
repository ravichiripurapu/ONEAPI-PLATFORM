import { Component, OnInit, Input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTreeModule, MatTreeNestedDataSource } from '@angular/material/tree';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { NestedTreeControl } from '@angular/cdk/tree';
import { DatasourceService, SourceInfoDTO } from '../../../core/services/datasource.service';
import { MetadataService, DomainInfoDTO, EntityInfoDTO, FieldInfoDTO } from '../../../core/services/metadata.service';
import { forkJoin } from 'rxjs';

interface TreeNode {
  name: string;
  type: 'source' | 'domain' | 'entity';
  id: number;
  data?: any;
  children?: TreeNode[];
}

@Component({
  selector: 'app-metadata-tree-explorer',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTreeModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatTableModule,
    MatChipsModule
  ],
  templateUrl: './metadata-tree-explorer.html',
  styleUrl: './metadata-tree-explorer.scss',
})
export class MetadataTreeExplorer implements OnInit {
  @Input() sources: SourceInfoDTO[] = []; // Option 1: Pass sources from parent
  @Input() autoLoadSources = true; // Option 2: Auto-load all sources

  // Tree-based navigation
  treeControl = new NestedTreeControl<TreeNode>(node => node.children);
  dataSource = new MatTreeNestedDataSource<TreeNode>();

  loading = signal(false);
  selectedNode = signal<TreeNode | null>(null);
  selectedSource = signal<SourceInfoDTO | null>(null);
  selectedDomain = signal<DomainInfoDTO | null>(null);
  selectedEntity = signal<EntityInfoDTO | null>(null);
  entityFields = signal<FieldInfoDTO[]>([]);

  fieldColumns = ['columnName', 'businessName', 'dataType', 'nullable', 'key', 'default'];
  fieldDisplayColumns = this.fieldColumns;

  // Resizable panel
  treePanelWidth = signal(25);
  isResizing = signal(false);
  private startX = 0;
  private startWidth = 0;

  constructor(
    private datasourceService: DatasourceService,
    private metadataService: MetadataService
  ) {}

  ngOnInit(): void {
    if (this.autoLoadSources) {
      this.loadSources();
    } else if (this.sources.length > 0) {
      this.buildTreeFromSources(this.sources);
    }
  }

  loadSources(): void {
    this.loading.set(true);
    this.datasourceService.getAllDatasources().subscribe({
      next: (sources) => {
        console.log('Loaded sources:', sources);
        this.buildTreeFromSources(sources);
      },
      error: (error) => {
        console.error('Error loading sources:', error);
        this.loading.set(false);
      }
    });
  }

  buildTreeFromSources(sources: SourceInfoDTO[]): void {
    if (sources.length === 0) {
      this.dataSource.data = [];
      this.loading.set(false);
      return;
    }

    // Load all metadata for all sources upfront
    const sourceTreePromises = sources.map(source => this.buildSourceTree(source));

    Promise.all(sourceTreePromises).then(treeNodes => {
      this.dataSource.data = treeNodes.filter(n => n !== null) as TreeNode[];
      this.loading.set(false);
      console.log('Complete tree built:', this.dataSource.data);

      // Auto-expand first source
      if (this.dataSource.data.length > 0) {
        setTimeout(() => {
          this.treeControl.expand(this.dataSource.data[0]);
        }, 100);
      }
    });
  }

  private buildSourceTree(source: SourceInfoDTO): Promise<TreeNode> {
    return new Promise((resolve) => {
      // Load all domains for this source
      this.metadataService.getDomainsBySource(source.id!).subscribe({
        next: (domains) => {
          console.log(`Loaded ${domains.length} domains for source: ${source.name}`);

          if (domains.length === 0) {
            // No domains
            resolve({
              name: source.name,
              type: 'source',
              id: source.id!,
              data: source,
              children: []
            });
            return;
          }

          // Load all entities for all domains in parallel
          const entityRequests = domains.map(domain =>
            this.metadataService.getEntitiesByDomain(domain.id)
          );

          forkJoin(entityRequests).subscribe({
            next: (allEntities) => {
              console.log(`Loaded entities for ${source.name}:`, allEntities.flat().length);

              // Create source root node
              const sourceNode: TreeNode = {
                name: source.name,
                type: 'source',
                id: source.id!,
                data: source,
                children: []
              };

              // Create domain nodes with their entities
              domains.forEach((domain, index) => {
                const entities = allEntities[index];

                const domainNode: TreeNode = {
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

              resolve(sourceNode);
            },
            error: (error) => {
              console.error(`Error loading entities for ${source.name}:`, error);
              // Still create node with just domains
              resolve({
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
              });
            }
          });
        },
        error: (error) => {
          console.error(`Error loading domains for ${source.name}:`, error);
          resolve({
            name: source.name,
            type: 'source',
            id: source.id!,
            data: source,
            children: []
          });
        }
      });
    });
  }

  hasChild = (_: number, node: TreeNode) => node.children !== undefined && node.children.length >= 0;

  onNodeClick(node: TreeNode): void {
    console.log('=== onNodeClick ===');
    console.log('Node clicked:', node);
    console.log('Node type:', node.type);

    this.selectedNode.set(node);

    if (node.type === 'source') {
      console.log('Source selected:', node.data);
      this.selectedSource.set(node.data);
      this.selectedDomain.set(null);
      this.selectedEntity.set(null);
      this.entityFields.set([]);
    } else if (node.type === 'domain') {
      console.log('Domain selected:', node.data);
      this.selectedSource.set(null);
      this.selectedDomain.set(node.data);
      this.selectedEntity.set(null);
      this.entityFields.set([]);
    } else if (node.type === 'entity') {
      console.log('Entity selected:', node.data);
      this.selectedSource.set(null);
      this.selectedDomain.set(null);
      this.selectedEntity.set(node.data);
      this.loadEntityFields(node.data.id);
    }
  }

  loadEntityFields(entityId: number): void {
    this.metadataService.getFieldsByEntity(entityId).subscribe({
      next: (fields) => {
        console.log('Loaded fields:', fields);
        this.entityFields.set(fields);
      },
      error: (error) => {
        console.error('Error loading fields:', error);
      }
    });
  }

  getIconForNode(node: TreeNode): string {
    if (node.type === 'source') return 'storage';
    if (node.type === 'domain') return 'schema';
    return 'table_chart';
  }

  formatNumber(num: number | undefined): string {
    if (num === undefined || num === null) return '0';
    return num.toLocaleString();
  }

  formatBytes(bytes: number | undefined): string {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
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

    const constrainedWidth = Math.max(15, Math.min(50, newWidth));
    this.treePanelWidth.set(constrainedWidth);
  }

  private stopResize(onMouseMove: (e: MouseEvent) => void, onMouseUp: () => void): void {
    this.isResizing.set(false);
    document.removeEventListener('mousemove', onMouseMove);
    document.removeEventListener('mouseup', onMouseUp);
  }
}
