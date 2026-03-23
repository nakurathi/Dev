import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule }      from '@angular/common';
import { RouterModule }      from '@angular/router';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatTableModule }    from '@angular/material/table';
import { MatSortModule }     from '@angular/material/sort';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatInputModule }    from '@angular/material/input';
import { MatSelectModule }   from '@angular/material/select';
import { MatButtonModule }   from '@angular/material/button';
import { MatIconModule }     from '@angular/material/icon';
import { MatChipsModule }    from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { AppointmentService } from '../../core/services/appointment.service';
import { Appointment, AppointmentStatus } from '../../core/models/models';

@Component({
  selector: 'app-appointment-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatTableModule, MatSortModule, MatPaginatorModule,
    MatInputModule, MatSelectModule, MatButtonModule,
    MatIconModule, MatChipsModule, MatProgressSpinnerModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Appointments</h1>
        <button mat-raised-button color="primary" routerLink="/appointments/new">
          <mat-icon>add</mat-icon> New Appointment
        </button>
      </div>

      <!-- Filters -->
      <div class="filters-row">
        <mat-form-field appearance="outline">
          <mat-label>Search by Patient ID</mat-label>
          <input matInput [formControl]="searchControl" placeholder="patient-001" />
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Status</mat-label>
          <mat-select [formControl]="statusControl">
            <mat-option value="">All</mat-option>
            @for (s of statuses; track s) {
              <mat-option [value]="s">{{ s }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </div>

      @if (loading()) {
        <div class="loader-center"><mat-spinner diameter="40"></mat-spinner></div>
      } @else {
        <table mat-table [dataSource]="filtered()" class="full-width" matSort>

          <ng-container matColumnDef="appointmentId">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>ID</th>
            <td mat-cell *matCellDef="let a">{{ a.appointmentId | slice:0:8 }}...</td>
          </ng-container>

          <ng-container matColumnDef="patientId">
            <th mat-header-cell *matHeaderCellDef>Patient</th>
            <td mat-cell *matCellDef="let a">{{ a.patientId }}</td>
          </ng-container>

          <ng-container matColumnDef="doctorId">
            <th mat-header-cell *matHeaderCellDef>Doctor</th>
            <td mat-cell *matCellDef="let a">{{ a.doctorId }}</td>
          </ng-container>

          <ng-container matColumnDef="scheduledAt">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Scheduled</th>
            <td mat-cell *matCellDef="let a">{{ a.scheduledAt | date:'medium' }}</td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let a">
              <mat-chip [class]="'chip-' + a.status.toLowerCase()">{{ a.status }}</mat-chip>
            </td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let a">
              <button mat-icon-button color="warn"
                      (click)="cancel(a)"
                      [disabled]="a.status === 'CANCELLED' || a.status === 'COMPLETED'">
                <mat-icon>cancel</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>

        <mat-paginator [pageSizeOptions]="[10, 25, 50]" showFirstLastButtons></mat-paginator>
      }
    </div>
  `,
  styles: [`
    .page-container  { padding: 24px; }
    .page-header     { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .filters-row     { display: flex; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
    .full-width      { width: 100%; }
    .loader-center   { display: flex; justify-content: center; padding: 40px; }
    .chip-scheduled  { background: #e3f2fd !important; color: #1565c0 !important; }
    .chip-confirmed  { background: #e8f5e9 !important; color: #2e7d32 !important; }
    .chip-cancelled  { background: #ffebee !important; color: #b71c1c !important; }
    .chip-completed  { background: #f3e5f5 !important; color: #6a1b9a !important; }
  `]
})
export class AppointmentListComponent implements OnInit {
  private readonly svc = inject(AppointmentService);

  displayedColumns = ['appointmentId', 'patientId', 'doctorId', 'scheduledAt', 'status', 'actions'];
  statuses: AppointmentStatus[] = ['SCHEDULED', 'CONFIRMED', 'CANCELLED', 'COMPLETED'];

  appointments = signal<Appointment[]>([]);
  loading      = signal(true);
  filtered     = signal<Appointment[]>([]);

  searchControl = new FormControl('');
  statusControl = new FormControl('');

  ngOnInit(): void {
    // Reactive filter pipeline
    this.searchControl.valueChanges.pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => this.applyFilters());
    this.statusControl.valueChanges.subscribe(() => this.applyFilters());

    const patientId = new URLSearchParams(window.location.search).get('patientId') ?? '';
    if (patientId) {
      this.svc.getByPatient(patientId).subscribe({
        next:  data => { this.appointments.set(data); this.filtered.set(data); this.loading.set(false); },
        error: ()   => this.loading.set(false),
      });
    } else {
      this.svc.appointments$.subscribe(data => {
        this.appointments.set(data);
        this.applyFilters();
        this.loading.set(false);
      });
    }
  }

  applyFilters(): void {
    const q      = (this.searchControl.value ?? '').toLowerCase();
    const status = this.statusControl.value ?? '';
    this.filtered.set(
      this.appointments().filter(a =>
        (!q || a.patientId.toLowerCase().includes(q)) &&
        (!status || a.status === status)
      )
    );
  }

  cancel(appt: Appointment): void {
    this.svc.cancel(appt.appointmentId).subscribe();
  }
}
