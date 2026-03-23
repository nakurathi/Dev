import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule }         from '@angular/material/card';
import { MatIconModule }         from '@angular/material/icon';
import { MatButtonModule }       from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DashboardService } from '../../core/services/api.services';
import { DashboardStats }   from '../../core/models/models';

/**
 * Dashboard component — Angular 17 Signals-based state management.
 * Uses signal() and computed() instead of BehaviorSubject for local state.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatCardModule,
    MatIconModule, MatButtonModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="dashboard">
      <h1 class="page-title">Dashboard</h1>

      @if (loading()) {
        <div class="loader-center">
          <mat-spinner diameter="48"></mat-spinner>
        </div>
      } @else if (stats()) {
        <!-- KPI Cards -->
        <div class="stats-grid">
          @for (card of statCards(); track card.label) {
            <mat-card class="stat-card" [class]="card.colorClass">
              <mat-card-content>
                <div class="stat-row">
                  <div>
                    <p class="stat-label">{{ card.label }}</p>
                    <p class="stat-value">{{ card.value }}</p>
                  </div>
                  <mat-icon class="stat-icon">{{ card.icon }}</mat-icon>
                </div>
              </mat-card-content>
            </mat-card>
          }
        </div>

        <!-- Quick Actions -->
        <mat-card class="quick-actions">
          <mat-card-header>
            <mat-card-title>Quick Actions</mat-card-title>
          </mat-card-header>
          <mat-card-content class="actions-row">
            <button mat-raised-button color="primary" routerLink="/appointments/new">
              <mat-icon>add</mat-icon> New Appointment
            </button>
            <button mat-raised-button color="accent" routerLink="/patients">
              <mat-icon>person_add</mat-icon> View Patients
            </button>
            <button mat-raised-button routerLink="/billing">
              <mat-icon>receipt</mat-icon> Billing
            </button>
          </mat-card-content>
        </mat-card>
      }

      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </div>
  `,
  styles: [`
    .dashboard    { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .page-title   { font-size: 28px; font-weight: 600; margin-bottom: 24px; color: #1a237e; }
    .stats-grid   { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .stat-card    { cursor: default; }
    .stat-row     { display: flex; justify-content: space-between; align-items: center; }
    .stat-label   { font-size: 13px; color: #666; margin: 0; }
    .stat-value   { font-size: 32px; font-weight: 700; margin: 4px 0 0; }
    .stat-icon    { font-size: 40px; width: 40px; height: 40px; opacity: 0.6; }
    .blue  .stat-value  { color: #1565c0; }
    .green .stat-value  { color: #2e7d32; }
    .orange .stat-value { color: #e65100; }
    .red .stat-value    { color: #b71c1c; }
    .quick-actions      { margin-top: 16px; }
    .actions-row        { display: flex; gap: 12px; flex-wrap: wrap; padding-top: 8px; }
    .loader-center      { display: flex; justify-content: center; padding: 60px; }
    .error              { color: #f44336; }
  `]
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  // Angular 17 Signals
  stats   = signal<DashboardStats | null>(null);
  loading = signal(true);
  error   = signal('');

  statCards = computed(() => {
    const s = this.stats();
    if (!s) return [];
    return [
      { label: 'Total Patients',      value: s.totalPatients,      icon: 'people',        colorClass: 'blue'   },
      { label: "Today's Appointments", value: s.todayAppointments,  icon: 'calendar_today', colorClass: 'green'  },
      { label: 'Pending Invoices',    value: s.pendingInvoices,     icon: 'pending_actions', colorClass: 'orange' },
      { label: 'Overdue Invoices',    value: s.overdueInvoices,     icon: 'warning',        colorClass: 'red'    },
    ];
  });

  ngOnInit(): void {
    this.dashboardService.getStats().subscribe({
      next:  stats  => { this.stats.set(stats);    this.loading.set(false); },
      error: err    => { this.error.set(err.message); this.loading.set(false); },
    });
  }
}
