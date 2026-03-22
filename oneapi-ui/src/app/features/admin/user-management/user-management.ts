import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTabsModule } from '@angular/material/tabs';
import { MatExpansionModule } from '@angular/material/expansion';
import { UserService, UserDTO, CreateUserRequest, UpdateUserRequest, UserPreferencesDTO, UserRoleDTO } from '../../../core/services/user.service';
import { RoleService, RoleDTO } from '../../../core/services/role.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-user-management',
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
    MatDialogModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatCheckboxModule,
    MatTabsModule,
    MatExpansionModule
  ],
  templateUrl: './user-management.html',
  styleUrl: './user-management.scss',
})
export class UserManagement implements OnInit {
  users = signal<UserDTO[]>([]);
  allRoles = signal<RoleDTO[]>([]);
  loading = signal(false);
  displayedColumns = ['login', 'email', 'firstName', 'activated', 'actions'];
  showCreateForm = signal(false);
  editingUser = signal<UserDTO | null>(null);

  newUser: CreateUserRequest = {
    login: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    activated: true,
    langKey: 'en'
  };

  updateRequest: UpdateUserRequest = {
    email: '',
    firstName: '',
    lastName: '',
    activated: true,
    langKey: 'en'
  };

  userPreferences: UserPreferencesDTO = {
    userId: '',
    pageSize: 20,
    ttlMinutes: 30,
    maxConcurrentSessions: 5
  };

  selectedRoles: number[] = [];
  userRoles: UserRoleDTO[] = [];
  preferencesLoading = signal(false);
  preferencesLoaded = signal(false);

  get currentUserEmail(): string {
    return this.editingUser() ? (this.updateRequest.email || '') : (this.newUser.email || '');
  }

  set currentUserEmail(value: string) {
    if (this.editingUser()) {
      this.updateRequest.email = value;
    } else {
      this.newUser.email = value;
    }
  }

  get currentUserFirstName(): string {
    return this.editingUser() ? this.updateRequest.firstName || '' : this.newUser.firstName || '';
  }

  set currentUserFirstName(value: string) {
    if (this.editingUser()) {
      this.updateRequest.firstName = value;
    } else {
      this.newUser.firstName = value;
    }
  }

  get currentUserLastName(): string {
    return this.editingUser() ? this.updateRequest.lastName || '' : this.newUser.lastName || '';
  }

  set currentUserLastName(value: string) {
    if (this.editingUser()) {
      this.updateRequest.lastName = value;
    } else {
      this.newUser.lastName = value;
    }
  }

  get currentUserLangKey(): string {
    return this.editingUser() ? this.updateRequest.langKey || 'en' : this.newUser.langKey || 'en';
  }

  set currentUserLangKey(value: string) {
    if (this.editingUser()) {
      this.updateRequest.langKey = value;
    } else {
      this.newUser.langKey = value;
    }
  }

  get currentUserActivated(): boolean {
    return this.editingUser() ? this.updateRequest.activated ?? true : this.newUser.activated ?? true;
  }

  set currentUserActivated(value: boolean) {
    if (this.editingUser()) {
      this.updateRequest.activated = value;
    } else {
      this.newUser.activated = value;
    }
  }

  constructor(
    private userService: UserService,
    private roleService: RoleService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    forkJoin({
      users: this.userService.getAllUsers(),
      roles: this.roleService.getAllRoles()
    }).subscribe({
      next: (result) => {
        this.users.set(result.users);
        this.allRoles.set(result.roles);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading data:', error);
        this.snackBar.open('Error loading data', 'Close', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  openCreateDialog(): void {
    this.editingUser.set(null);
    this.newUser = {
      login: '',
      email: '',
      password: '',
      firstName: '',
      lastName: '',
      activated: true,
      langKey: 'en'
    };
    this.userPreferences = {
      userId: '',
      pageSize: 20,
      ttlMinutes: 30,
      maxConcurrentSessions: 5
    };
    this.selectedRoles = [];
    this.showCreateForm.set(true);
  }

  openEditDialog(user: UserDTO): void {
    this.editingUser.set(user);
    this.updateRequest = {
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      activated: user.activated,
      langKey: user.langKey || 'en'
    };

    // Reset preferences state
    this.preferencesLoaded.set(false);
    this.userPreferences = {
      userId: user.login,
      pageSize: 20,
      ttlMinutes: 30,
      maxConcurrentSessions: 5
    };

    // Load user roles
    this.userService.getUserRoles(user.id).subscribe({
      next: (roles) => {
        console.log('Loaded user roles:', roles);
        this.userRoles = roles;
        this.selectedRoles = roles.map(r => r.roleId);
        console.log('Selected roles:', this.selectedRoles);
      },
      error: (error) => {
        console.error('Error loading user roles:', error);
      }
    });

    // Load user preferences immediately (don't wait for tab change)
    this.loadUserPreferences();

    this.showCreateForm.set(true);
  }

  cancelForm(): void {
    this.showCreateForm.set(false);
    this.editingUser.set(null);
  }

  saveUser(): void {
    const editing = this.editingUser();
    console.log('=== saveUser called ===');
    console.log('Editing user:', editing);

    if (editing) {
      console.log('Update request:', this.updateRequest);
      console.log('Selected roles:', this.selectedRoles);
      console.log('User preferences:', this.userPreferences);

      // Update existing user - save user details, roles, and preferences
      this.userService.updateUser(editing.id, this.updateRequest).subscribe({
        next: () => {
          console.log('User details updated successfully');
          this.snackBar.open('User updated successfully', 'Close', { duration: 3000 });
          // Update roles
          this.updateUserRoles(editing.id);
          // Update preferences
          this.saveUserPreferences(editing.login);
        },
        error: (error) => {
          console.error('Error updating user:', error);
          this.snackBar.open('Error updating user', 'Close', { duration: 3000 });
        }
      });
    } else {
      // Create new user
      console.log('Creating new user:', this.newUser);
      console.log('Selected roles:', this.selectedRoles);

      this.userService.createUser(this.newUser).subscribe({
        next: (createdUser) => {
          console.log('User created:', createdUser);
          this.snackBar.open('User created successfully', 'Close', { duration: 3000 });
          this.assignUserRoles(createdUser.id);
        },
        error: (error) => {
          console.error('Error creating user:', error);
          this.snackBar.open('Error creating user', 'Close', { duration: 3000 });
        }
      });
    }
  }

  updateUserRoles(userId: number): void {
    console.log('=== updateUserRoles called ===');
    console.log('userId:', userId);
    console.log('userRoles:', this.userRoles);
    console.log('selectedRoles:', this.selectedRoles);

    // Get current roles
    const currentRoleIds = this.userRoles.map(r => r.roleId);
    console.log('currentRoleIds:', currentRoleIds);

    // Find roles to add
    const rolesToAdd = this.selectedRoles.filter(roleId => !currentRoleIds.includes(roleId));
    console.log('rolesToAdd:', rolesToAdd);

    // Find roles to remove
    const rolesToRemove = this.userRoles.filter(ur => !this.selectedRoles.includes(ur.roleId));
    console.log('rolesToRemove:', rolesToRemove);

    const operations = [];

    // Add new roles
    for (const roleId of rolesToAdd) {
      console.log('Adding role:', userId, roleId);
      operations.push(this.userService.assignRole({ userId, roleId }));
    }

    // Remove old roles
    for (const userRole of rolesToRemove) {
      console.log('Removing role:', userRole.id);
      operations.push(this.userService.revokeUserRole(userRole.id));
    }

    console.log('Total operations:', operations.length);

    if (operations.length > 0) {
      forkJoin(operations).subscribe({
        next: () => {
          console.log('Role operations completed successfully');
          this.snackBar.open('User roles updated successfully', 'Close', { duration: 3000 });
          // Reload user roles to show updated list
          this.loadUserRoles(userId);
        },
        error: (error) => {
          console.error('Error updating user roles:', error);
          this.snackBar.open('Error updating user roles', 'Close', { duration: 3000 });
        }
      });
    } else {
      console.log('No role changes detected');
      this.snackBar.open('No role changes to save', 'Close', { duration: 2000 });
    }
  }

  assignUserRoles(userId: number): void {
    if (this.selectedRoles.length === 0) {
      this.snackBar.open('User created successfully without roles', 'Close', { duration: 3000 });
      this.showCreateForm.set(false);
      this.loadData();
      return;
    }

    const operations = this.selectedRoles.map(roleId =>
      this.userService.assignRole({ userId, roleId })
    );

    forkJoin(operations).subscribe({
      next: () => {
        this.snackBar.open('User created and roles assigned successfully', 'Close', { duration: 3000 });
        this.showCreateForm.set(false);
        this.loadData();
      },
      error: (error) => {
        console.error('Error assigning roles:', error);
        this.snackBar.open('User created but error assigning roles', 'Close', { duration: 3000 });
        this.showCreateForm.set(false);
        this.loadData();
      }
    });
  }

  deleteUser(user: UserDTO): void {
    if (confirm(`Are you sure you want to delete user: ${user.login}?`)) {
      this.userService.deleteUser(user.id).subscribe({
        next: () => {
          this.snackBar.open('User deleted successfully', 'Close', { duration: 3000 });
          this.loadData();
        },
        error: (error) => {
          console.error('Error deleting user:', error);
          this.snackBar.open('Error deleting user', 'Close', { duration: 3000 });
        }
      });
    }
  }

  loadUserRoles(userId: number): void {
    this.userService.getUserRoles(userId).subscribe({
      next: (roles) => {
        console.log('Loaded user roles:', roles);
        this.userRoles = roles;
        this.selectedRoles = roles.map(r => r.roleId);
      },
      error: (error) => {
        console.error('Error loading user roles:', error);
      }
    });
  }

  toggleRole(roleId: number): void {
    const index = this.selectedRoles.indexOf(roleId);
    if (index >= 0) {
      this.selectedRoles.splice(index, 1);
    } else {
      this.selectedRoles.push(roleId);
    }
    console.log('Selected roles after toggle:', this.selectedRoles);
  }

  isRoleSelected(roleId: number): boolean {
    return this.selectedRoles.includes(roleId);
  }

  getRoleName(roleId: number): string {
    const role = this.allRoles().find(r => r.id === roleId);
    return role?.name || '';
  }

  loadUserPreferences(): void {
    const user = this.editingUser();
    if (!user) return;

    this.preferencesLoading.set(true);
    console.log('Loading preferences for user:', user.login);

    this.userService.getUserPreferencesByUserId(user.login).subscribe({
      next: (prefs) => {
        console.log('Loaded preferences:', prefs);
        this.userPreferences = {
          userId: prefs.userId,
          pageSize: prefs.pageSize || 20,
          ttlMinutes: prefs.ttlMinutes || 30,
          maxConcurrentSessions: prefs.maxConcurrentSessions || 5
        };
        this.preferencesLoaded.set(true);
        this.preferencesLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading user preferences:', error);
        // Set defaults if no preferences exist
        this.userPreferences = {
          userId: user.login,
          pageSize: 20,
          ttlMinutes: 30,
          maxConcurrentSessions: 5
        };
        this.preferencesLoaded.set(true);
        this.preferencesLoading.set(false);
      }
    });
  }

  saveUserPreferences(userId: string): void {
    console.log('Saving preferences for user:', userId, this.userPreferences);

    this.userService.updateUserPreferencesByUserId(userId, this.userPreferences).subscribe({
      next: (updated) => {
        console.log('Preferences saved successfully:', updated);
        this.snackBar.open('User preferences updated successfully', 'Close', { duration: 3000 });
      },
      error: (error) => {
        console.error('Error saving preferences:', error);
        this.snackBar.open('Error updating user preferences', 'Close', { duration: 3000 });
      }
    });
  }

  onTabChange(index: number): void {
    // Load preferences when Preferences tab is selected (index 2)
    if (index === 2 && !this.preferencesLoaded() && this.editingUser()) {
      this.loadUserPreferences();
    }
  }
}
