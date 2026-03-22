import { Component, Injectable } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../../core/services/auth.service';
import { DataExplorer } from '../data-explorer/data-explorer';
import { QueryList } from '../query-list/query-list';
import { QueryBuilder } from '../query-builder/query-builder';
import { QueryExecutor } from '../query-executor/query-executor';

@Injectable()
export class ClientNavigationService {
  private dashboard!: Dashboard;

  setDashboard(dashboard: Dashboard): void {
    this.dashboard = dashboard;
  }

  navigateToBuilder(): void {
    if (this.dashboard) {
      this.dashboard.navigateToBuilder();
    }
  }

  navigateToQueries(): void {
    if (this.dashboard) {
      this.dashboard.navigateToQueries();
    }
  }

  navigateToExplorer(): void {
    if (this.dashboard) {
      this.dashboard.navigateToExplorer();
    }
  }

  navigateToStudio(): void {
    if (this.dashboard) {
      this.dashboard.navigateToStudio();
    }
  }
}

@Component({
  selector: 'app-client-dashboard',
  standalone: true,
  imports: [
    RouterModule,
    MatToolbarModule,
    MatSidenavModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    MatCardModule,
    DataExplorer,
    QueryList,
    QueryBuilder,
    QueryExecutor
  ],
  providers: [ClientNavigationService],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  selectedTab = 'home';

  constructor(
    public authService: AuthService,
    private router: Router,
    private navigationService: ClientNavigationService
  ) {
    this.navigationService.setDashboard(this);
  }

  currentUser() {
    return this.authService.currentUserSignal();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  navigateToExplorer(): void {
    this.selectedTab = 'explorer';
  }

  navigateToQueries(): void {
    this.selectedTab = 'queries';
  }

  navigateToBuilder(): void {
    this.selectedTab = 'builder';
  }

  navigateToStudio(): void {
    this.selectedTab = 'studio';
  }
}
