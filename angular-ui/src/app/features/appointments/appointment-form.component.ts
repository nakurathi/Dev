import { Component, inject } from '@angular/core';
import { CommonModule }       from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule }     from '@angular/material/card';
import { MatInputModule }    from '@angular/material/input';
import { MatButtonModule }   from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AppointmentService } from '../../core/services/appointment.service';

/**
 * Appointment scheduling form using Angular Reactive Forms.
 * Validates required fields and future dates before submitting.
 */
@Component({
  selector: 'app-appointment-form',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatCardModule, MatInputModule, MatButtonModule,
    MatDatepickerModule, MatNativeDateModule, MatSnackBarModule,
  ],
  template: `
    <div class="form-page">
      <mat-card class="form-card">
        <mat-card-header>
          <mat-card-title>Schedule Appointment</mat-card-title>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Patient ID</mat-label>
              <input matInput formControlName="patientId" placeholder="patient-001" />
              @if (form.get('patientId')?.hasError('required') && form.get('patientId')?.touched) {
                <mat-error>Patient ID is required</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Doctor ID</mat-label>
              <input matInput formControlName="doctorId" placeholder="doctor-001" />
              @if (form.get('doctorId')?.hasError('required') && form.get('doctorId')?.touched) {
                <mat-error>Doctor ID is required</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Scheduled Date</mat-label>
              <input matInput [matDatepicker]="picker" formControlName="scheduledAt" />
              <mat-datepicker-toggle matIconSuffix [for]="picker"></mat-datepicker-toggle>
              <mat-datepicker #picker></mat-datepicker>
              @if (form.get('scheduledAt')?.hasError('required') && form.get('scheduledAt')?.touched) {
                <mat-error>Date is required</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Notes (optional)</mat-label>
              <textarea matInput formControlName="notes" rows="3"></textarea>
            </mat-form-field>

            <div class="form-actions">
              <button mat-button type="button" routerLink="/appointments">Cancel</button>
              <button mat-raised-button color="primary" type="submit"
                      [disabled]="form.invalid || submitting">
                {{ submitting ? 'Scheduling…' : 'Schedule Appointment' }}
              </button>
            </div>

          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .form-page    { display: flex; justify-content: center; padding: 32px 16px; }
    .form-card    { width: 100%; max-width: 520px; }
    .full-width   { width: 100%; margin-bottom: 16px; }
    .form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; }
  `]
})
export class AppointmentFormComponent {
  private readonly fb      = inject(FormBuilder);
  private readonly svc     = inject(AppointmentService);
  private readonly router  = inject(Router);
  private readonly snack   = inject(MatSnackBar);

  submitting = false;

  form = this.fb.group({
    patientId:   ['', Validators.required],
    doctorId:    ['', Validators.required],
    scheduledAt: [null as Date | null, Validators.required],
    notes:       [''],
  });

  onSubmit(): void {
    if (this.form.invalid) return;
    this.submitting = true;

    const { patientId, doctorId, scheduledAt, notes } = this.form.value;

    this.svc.schedule({
      patientId:   patientId!,
      doctorId:    doctorId!,
      scheduledAt: (scheduledAt as Date).toISOString(),
      notes:       notes ?? undefined,
    }).subscribe({
      next: () => {
        this.snack.open('Appointment scheduled!', 'OK', { duration: 3000 });
        this.router.navigate(['/appointments']);
      },
      error: err => {
        this.snack.open(err.error?.detail ?? 'Failed to schedule', 'Dismiss', { duration: 5000 });
        this.submitting = false;
      },
    });
  }
}
