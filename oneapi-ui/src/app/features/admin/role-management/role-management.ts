import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatBadgeModule } from '@angular/material/badge';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { RoleService, RoleDTO, CreateRoleRequest, UpdateRoleRequest, PermissionDTO, CreatePermissionRequest } from '../../../core/services/role.service';
import { DatasourceService, SourceInfoDTO } from '../../../core/services/datasource.service';
import { MetadataService, DomainInfoDTO, EntityInfoDTO, FieldInfoDTO } from '../../../core/services/metadata.service';

@Component({
  selector: 'app-role-management',
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
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatExpansionModule,
    MatBadgeModule,
    MatChipsModule,
    MatDividerModule,
    MatListModule
  ],
  templateUrl: './role-management.html',
  styleUrl: './role-management.scss',
})
export class RoleManagement implements OnInit {
  // Role management
  roles = signal<RoleDTO[]>([]);
  selectedRole = signal<RoleDTO | null>(null);
  loading = signal(false);
  displayedColumns = ['name', 'description', 'actions'];
  showCreateForm = signal(false);
  editingRole = signal<RoleDTO | null>(null);

  // Permission management
  permissions = signal<PermissionDTO[]>([]);
  permissionsLoading = signal(false);
  showPermissionForm = signal(false);

  // Data sources for permissions
  sources = signal<SourceInfoDTO[]>([]);
  domains = signal<DomainInfoDTO[]>([]);
  entities = signal<EntityInfoDTO[]>([]);
  fields = signal<FieldInfoDTO[]>([]);

  // Selected hierarchical data
  selectedSource = signal<SourceInfoDTO | null>(null);
  selectedDomain = signal<DomainInfoDTO | null>(null);
  selectedEntity = signal<EntityInfoDTO | null>(null);
  selectedField = signal<FieldInfoDTO | null>(null);
  permissionLevel = signal<number>(1); // 1=Source, 2=Schema, 3=Table, 4=Field

  // Resizable panel
  rolePanelWidth = signal(30);
  isResizing = signal(false);
  private startX = 0;
  private startWidth = 0;

  newRole: CreateRoleRequest = {
    name: '',
    description: ''
  };

  updateRequest: UpdateRoleRequest = {
    name: '',
    description: ''
  };

  get currentRoleName(): string {
    return this.editingRole() ? (this.updateRequest.name || '') : (this.newRole.name || '');
  }

  set currentRoleName(value: string) {
    if (this.editingRole()) {
      this.updateRequest.name = value;
    } else {
      this.newRole.name = value;
    }
  }

  get currentRoleDescription(): string {
    return this.editingRole() ? (this.updateRequest.description || '') : (this.newRole.description || '');
  }

  set currentRoleDescription(value: string) {
    if (this.editingRole()) {
      this.updateRequest.description = value;
    } else {
      this.newRole.description = value;
    }
  }

  constructor(
    private roleService: RoleService,
    private datasourceService: DatasourceService,
    private metadataService: MetadataService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadRoles();
    this.loadSources();
  }

  loadRoles(): void {
    this.loading.set(true);
    this.roleService.getAllRoles().subscribe({
      next: (roles) => {
        this.roles.set(roles);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading roles:', error);
        this.snackBar.open('Error loading roles', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  openCreateDialog(): void {
    this.editingRole.set(null);
    this.newRole = {
      name: '',
      description: ''
    };
    this.showCreateForm.set(true);
  }

  openEditDialog(role: RoleDTO): void {
    this.editingRole.set(role);
    this.updateRequest = {
      name: role.name,
      description: role.description
    };
    this.showCreateForm.set(true);
  }

  cancelForm(): void {
    this.showCreateForm.set(false);
    this.editingRole.set(null);
  }

  saveRole(): void {
    const editing = this.editingRole();
    if (editing) {
      this.roleService.updateRole(editing.id, this.updateRequest).subscribe({
        next: () => {
          this.snackBar.open('Role updated successfully', 'Close', { duration: 3000 });
          this.showCreateForm.set(false);
          this.loadRoles();
        },
        error: (error) => {
          console.error('Error updating role:', error);
          this.snackBar.open('Error updating role', 'Close', { duration: 3000 });
        }
      });
    } else {
      this.roleService.createRole(this.newRole).subscribe({
        next: () => {
          this.snackBar.open('Role created successfully', 'Close', { duration: 3000 });
          this.showCreateForm.set(false);
          this.loadRoles();
        },
        error: (error) => {
          console.error('Error creating role:', error);
          this.snackBar.open('Error creating role', 'Close', { duration: 3000 });
        }
      });
    }
  }

  deleteRole(role: RoleDTO): void {
    if (confirm(`Are you sure you want to delete role: ${role.name}?`)) {
      this.roleService.deleteRole(role.id).subscribe({
        next: () => {
          this.snackBar.open('Role deleted successfully', 'Close', { duration: 3000 });
          if (this.selectedRole()?.id === role.id) {
            this.selectedRole.set(null);
            this.permissions.set([]);
          }
          this.loadRoles();
        },
        error: (error) => {
          console.error('Error deleting role:', error);
          this.snackBar.open('Error deleting role', 'Close', { duration: 3000 });
        }
      });
    }
  }

  // Permission Management Methods
  selectRole(role: RoleDTO): void {
    this.selectedRole.set(role);
    this.loadPermissions(role.id);
  }

  loadPermissions(roleId: number): void {
    this.permissionsLoading.set(true);
    this.roleService.getPermissionsByRole(roleId).subscribe({
      next: (permissions) => {
        this.permissions.set(permissions);
        this.permissionsLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading permissions:', error);
        this.snackBar.open('Error loading permissions', 'Close', { duration: 3000 });
        this.permissionsLoading.set(false);
      }
    });
  }

  loadSources(): void {
    this.datasourceService.getAllDatasources().subscribe({
      next: (sources: SourceInfoDTO[]) => {
        this.sources.set(sources);
      },
      error: (error: any) => {
        console.error('Error loading sources:', error);
      }
    });
  }

  openPermissionForm(): void {
    this.showPermissionForm.set(true);
    this.permissionLevel.set(1);
    this.resetPermissionSelections();
  }

  resetPermissionSelections(): void {
    this.selectedSource.set(null);
    this.selectedDomain.set(null);
    this.selectedEntity.set(null);
    this.selectedField.set(null);
    this.domains.set([]);
    this.entities.set([]);
    this.fields.set([]);
  }

  onSourceChange(sourceId: number): void {
    const source = this.sources().find(s => s.id === sourceId);
    this.selectedSource.set(source || null);
    this.selectedDomain.set(null);
    this.selectedEntity.set(null);
    this.selectedField.set(null);

    if (sourceId) {
      this.metadataService.getDomainsBySource(sourceId).subscribe({
        next: (domains) => this.domains.set(domains),
        error: (error) => console.error('Error loading domains:', error)
      });
    }
  }

  onDomainChange(domainId: number): void {
    const domain = this.domains().find(d => d.id === domainId);
    this.selectedDomain.set(domain || null);
    this.selectedEntity.set(null);
    this.selectedField.set(null);

    if (domainId) {
      this.metadataService.getEntitiesByDomain(domainId).subscribe({
        next: (entities) => this.entities.set(entities),
        error: (error) => console.error('Error loading entities:', error)
      });
    }
  }

  onEntityChange(entityId: number): void {
    const entity = this.entities().find(e => e.id === entityId);
    this.selectedEntity.set(entity || null);
    this.selectedField.set(null);

    if (entityId) {
      this.metadataService.getFieldsByEntity(entityId).subscribe({
        next: (fields) => this.fields.set(fields),
        error: (error) => console.error('Error loading fields:', error)
      });
    }
  }

  addPermission(): void {
    const role = this.selectedRole();
    if (!role) return;

    const level = this.permissionLevel();
    const request: CreatePermissionRequest = {
      roleId: role.id,
      permissionType: 'READ'
    };

    // Validate and add IDs based on level
    if (level >= 1) {
      if (!this.selectedSource()) {
        this.snackBar.open('Please select a source', 'Close', { duration: 3000 });
        return;
      }
      request.sourceId = this.selectedSource()!.id;
    }

    if (level >= 2) {
      if (!this.selectedDomain()) {
        this.snackBar.open('Please select a schema', 'Close', { duration: 3000 });
        return;
      }
      request.domainId = this.selectedDomain()!.id;
    }

    if (level >= 3) {
      if (!this.selectedEntity()) {
        this.snackBar.open('Please select a table', 'Close', { duration: 3000 });
        return;
      }
      request.entityId = this.selectedEntity()!.id;
    }

    if (level >= 4) {
      if (!this.selectedField()) {
        this.snackBar.open('Please select a field', 'Close', { duration: 3000 });
        return;
      }
      request.fieldId = this.selectedField()!.id;
    }

    this.roleService.createPermission(request).subscribe({
      next: () => {
        this.snackBar.open('Permission added successfully', 'Close', { duration: 3000 });
        this.showPermissionForm.set(false);
        this.loadPermissions(role.id);
      },
      error: (error) => {
        console.error('Error adding permission:', error);
        this.snackBar.open('Error adding permission', 'Close', { duration: 3000 });
      }
    });
  }

  deletePermission(permission: PermissionDTO): void {
    if (!confirm('Remove this permission?')) return;

    this.roleService.deletePermission(permission.id).subscribe({
      next: () => {
        this.snackBar.open('Permission removed', 'Close', { duration: 3000 });
        const role = this.selectedRole();
        if (role) this.loadPermissions(role.id);
      },
      error: (error) => {
        console.error('Error deleting permission:', error);
        this.snackBar.open('Error removing permission', 'Close', { duration: 3000 });
      }
    });
  }

  getPermissionScope(permission: PermissionDTO): string {
    if (permission.level === 0) return 'All Sources';
    if (permission.level === 1) return permission.sourceName || 'Unknown';
    if (permission.level === 2) return `${permission.sourceName} / ${permission.domainName}`;
    if (permission.level === 3) return `${permission.sourceName} / ${permission.domainName} / ${permission.entityName}`;
    return `${permission.sourceName} / ${permission.domainName} / ${permission.entityName} / ${permission.fieldName}`;
  }

  getPermissionLevelLabel(level: number): string {
    const labels = ['Super Admin', 'Source', 'Schema', 'Table', 'Field'];
    return labels[level] || 'Unknown';
  }

  getPermissionLevelIcon(level: number): string {
    const icons = ['admin_panel_settings', 'storage', 'schema', 'table_chart', 'view_column'];
    return icons[level] || 'help';
  }

  getLevelColor(level: number): string {
    const colors = ['warn', 'primary', 'accent', '', ''];
    return colors[level] || '';
  }

  cancelPermissionForm(): void {
    this.showPermissionForm.set(false);
    this.resetPermissionSelections();
  }

  // Resizable panel methods
  startResize(event: MouseEvent): void {
    event.preventDefault();
    this.isResizing.set(true);
    this.startX = event.clientX;
    this.startWidth = this.rolePanelWidth();

    const onMouseMove = (e: MouseEvent) => this.onResize(e);
    const onMouseUp = () => this.stopResize(onMouseMove, onMouseUp);

    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  }

  private onResize(event: MouseEvent): void {
    if (!this.isResizing()) return;

    const container = document.querySelector('.role-container');
    if (!container) return;

    const containerWidth = container.clientWidth;
    const delta = event.clientX - this.startX;
    const deltaPercent = (delta / containerWidth) * 100;
    const newWidth = this.startWidth + deltaPercent;

    const constrainedWidth = Math.max(20, Math.min(50, newWidth));
    this.rolePanelWidth.set(constrainedWidth);
  }

  private stopResize(onMouseMove: (e: MouseEvent) => void, onMouseUp: () => void): void {
    this.isResizing.set(false);
    document.removeEventListener('mousemove', onMouseMove);
    document.removeEventListener('mouseup', onMouseUp);
  }
}
