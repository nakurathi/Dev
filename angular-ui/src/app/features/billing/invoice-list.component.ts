import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule }         from '@angular/common';
import { RouterModule }         from '@angular/router';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatTableModule }       from '@angular/material/table';
import { MatButtonModule }      from '@angular/material/button';
import { MatInputModule }       from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { BillingService }       from '../../core/services/api.services';
import { Invoice }              from '../../core/models/models';

@Component({
  selector: 'app-invoice-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatTableModule, MatButtonModule, MatInputModule,
    MatSnackBarModule, MatProgressSpinnerModule, MatDialogModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Invoices</h1>
      </div>

      <mat-form-field appearance="outline">
        <mat-label>Patient ID</mat-label>
        <input matInput [formControl]="patientIdControl"
               placeholder="Enter patient ID to load invoices" />
      </mat-form-field>
      <button mat-raised-button color="primary" (click)="loadInvoices()" style="margin-left:8px">
        Load
      </button>

      @if (loading()) {
        <div class="loader-center"><mat-spinner diameter="40"></mat-spinner></div>
      } @else {
        <table mat-table [dataSource]="invoices()" class="full-width" style="margin-top:16px">

          <ng-container matColumnDef="invoiceId">
            <th mat-header-cell *matHeaderCellDef>Invoice ID</th>
            <td mat-cell *matCellDef="let i">{{ i.invoiceId | slice:0:8 }}...</td>
          </ng-container>

          <ng-container matColumnDef="amount">
            <th mat-header-cell *matHeaderCellDef>Amount</th>
            <td mat-cell *matCellDef="let i">{{ i.amount | currency }}</td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let i">
              <span [class]="'status-badge status-' + i.status.toLowerCase()">{{ i.status }}</span>
            </td>
          </ng-container>

          <ng-container matColumnDef="dueDate">
            <th mat-header-cell *matHeaderCellDef>Due Date</th>
            <td mat-cell *matCellDef="let i">{{ i.dueDate | date:'mediumDate' }}</td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let i">
              <button mat-raised-button color="accent" [disabled]="i.status === 'PAID'"
                      (click)="pay(i)">Pay</button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let row; columns: cols;"></tr>
        </table>
      }
    </div>
  `,
  styles: [`
    .page-container    { padding: 24px; }
    .page-header       { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
    .full-width        { width: 100%; }
    .loader-center     { display:flex; justify-content:center; padding:40px; }
    .status-badge      { padding: 4px 10px; border-radius: 12px; font-size: 12px; font-weight: 500; }
    .status-pending    { background: #fff3e0; color: #e65100; }
    .status-paid       { background: #e8f5e9; color: #2e7d32; }
    .status-overdue    { background: #ffebee; color: #b71c1c; }
    .status-cancelled  { background: #f5f5f5; color: #616161; }
  `]
})
export class InvoiceListComponent implements OnInit {
  private readonly svc   = inject(BillingService);
  private readonly snack = inject(MatSnackBar);

  cols             = ['invoiceId', 'amount', 'status', 'dueDate', 'actions'];
  invoices         = signal<Invoice[]>([]);
  loading          = signal(false);
  patientIdControl = new FormControl('');

  ngOnInit(): void {
    const pid = new URLSearchParams(window.location.search).get('patientId');
    if (pid) { this.patientIdControl.setValue(pid); this.loadInvoices(); }
  }

  loadInvoices(): void {
    const pid = this.patientIdControl.value?.trim();
    if (!pid) return;
    this.loading.set(true);
    this.svc.getInvoicesByPatient(pid).subscribe({
      next:  data => { this.invoices.set(data); this.loading.set(false); },
      error: ()   => this.loading.set(false),
    });
  }

  pay(invoice: Invoice): void {
    this.svc.processPayment({ invoiceId: invoice.invoiceId, amountPaid: invoice.amount })
      .subscribe({
        next: updated => {
          this.invoices.update(list => list.map(i => i.invoiceId === invoice.invoiceId ? updated : i));
          this.snack.open('Payment processed!', 'OK', { duration: 3000 });
        },
        error: () => this.snack.open('Payment failed', 'Dismiss', { duration: 4000 }),
      });
  }
}
