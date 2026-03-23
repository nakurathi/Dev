import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule }  from '@angular/common';
import { RouterModule }  from '@angular/router';
import { MatTableModule }  from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule }   from '@angular/material/icon';
import { MatBadgeModule }  from '@angular/material/badge';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PatientService } from '../../core/services/api.services';
import { Patient } from '../../core/models/models';

// ── Patient List ──────────────────────────────────────────────────────────────

@Component({
  selector: 'app-patient-list',
  standalone: true,
  imports: [CommonModule, RouterModule, MatTableModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Patients</h1>
      </div>

      @if (loading()) {
        <div class="loader-center"><mat-spinner diameter="40"></mat-spinner></div>
      } @else {
        <table mat-table [dataSource]="patients()" class="full-width">

          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef>Name</th>
            <td mat-cell *matCellDef="let p">{{ p.firstName }} {{ p.lastName }}</td>
          </ng-container>

          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Email</th>
            <td mat-cell *matCellDef="let p">{{ p.email }}</td>
          </ng-container>

          <ng-container matColumnDef="dob">
            <th mat-header-cell *matHeaderCellDef>Date of Birth</th>
            <td mat-cell *matCellDef="let p">{{ p.dateOfBirth | date:'mediumDate' }}</td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let p">
              <span [class]="'status-badge status-' + p.status.toLowerCase()">{{ p.status }}</span>
            </td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let p">
              <button mat-icon-button [routerLink]="['/patients', p.patientId]">
                <mat-icon>visibility</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let row; columns: cols;"></tr>
        </table>
      }
    </div>
  `,
  styles: [`
    .page-container  { padding: 24px; }
    .page-header     { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .full-width      { width: 100%; }
    .loader-center   { display:flex; justify-content:center; padding:40px; }
    .status-badge    { padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
    .status-active   { background: #e8f5e9; color: #2e7d32; }
    .status-inactive { background: #fff3e0; color: #e65100; }
    .status-archived { background: #f5f5f5; color: #616161; }
  `]
})
export class PatientListComponent implements OnInit {
  private readonly svc = inject(PatientService);
  cols     = ['name', 'email', 'dob', 'status', 'actions'];
  patients = signal<Patient[]>([]);
  loading  = signal(true);

  ngOnInit(): void {
    this.svc.getAll().subscribe({
      next:  data => { this.patients.set(data); this.loading.set(false); },
      error: ()   => this.loading.set(false),
    });
  }
}

// ── Patient Detail ────────────────────────────────────────────────────────────

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="page-container">
      <button mat-button routerLink="/patients">
        <mat-icon>arrow_back</mat-icon> Back to Patients
      </button>

      @if (loading()) {
        <div class="loader-center"><mat-spinner diameter="40"></mat-spinner></div>
      } @else if (patient()) {
        <div class="detail-card">
          <h1>{{ patient()!.firstName }} {{ patient()!.lastName }}</h1>
          <p><strong>Email:</strong> {{ patient()!.email }}</p>
          <p><strong>Date of Birth:</strong> {{ patient()!.dateOfBirth | date:'longDate' }}</p>
          <p><strong>Status:</strong>
            <span [class]="'status-badge status-' + patient()!.status.toLowerCase()">
              {{ patient()!.status }}
            </span>
          </p>
          <p><strong>Patient ID:</strong> <code>{{ patient()!.patientId }}</code></p>

          <div class="actions">
            <button mat-raised-button color="primary"
                    [routerLink]="['/appointments']"
                    [queryParams]="{ patientId: patient()!.patientId }">
              <mat-icon>calendar_today</mat-icon> View Appointments
            </button>
            <button mat-raised-button
                    [routerLink]="['/billing/invoices']"
                    [queryParams]="{ patientId: patient()!.patientId }">
              <mat-icon>receipt</mat-icon> View Invoices
            </button>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; }
    .detail-card    { background: #fff; padding: 24px; border-radius: 8px; margin-top: 16px; max-width: 600px; }
    .loader-center  { display:flex; justify-content:center; padding:40px; }
    .actions        { display: flex; gap: 12px; margin-top: 24px; }
    .status-badge   { padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
    .status-active  { background: #e8f5e9; color: #2e7d32; }
    code            { background: #f5f5f5; padding: 2px 6px; border-radius: 4px; font-size: 13px; }
  `]
})
export class PatientDetailComponent implements OnInit {
  private readonly svc    = inject(PatientService);
  private readonly router = inject(RouterModule);
  patient = signal<Patient | null>(null);
  loading = signal(true);

  ngOnInit(): void {
    const id = window.location.pathname.split('/').pop() ?? '';
    this.svc.getById(id).subscribe({
      next:  p  => { this.patient.set(p); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
