import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../../core/services/auth.service';
import { UserManagement } from '../user-management/user-management';
import { RoleManagement } from '../role-management/role-management';
import { DatasourceManagement } from '../datasource-management/datasource-management';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    RouterModule,
    MatToolbarModule,
    MatSidenavModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    UserManagement,
    RoleManagement,
    DatasourceManagement
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  selectedTab = 'datasources';

  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  currentUser() {
    return this.authService.currentUserSignal();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
