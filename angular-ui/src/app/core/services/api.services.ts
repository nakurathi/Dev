import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  Patient, PatientRequest, Invoice, InvoiceRequest,
  PaymentRequest, ApiResponse, DashboardStats
} from '../models/models';
import { environment } from '../../../environments/environment';

// ── Patient Service ───────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http    = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/patients`;

  create(request: PatientRequest): Observable<Patient> {
    return this.http.post<Patient>(this.baseUrl, request);
  }

  getById(id: string): Observable<Patient> {
    return this.http.get<Patient>(`${this.baseUrl}/${id}`);
  }

  getAll(status = 'ACTIVE'): Observable<Patient[]> {
    const params = new HttpParams().set('status', status);
    return this.http.get<Patient[]>(this.baseUrl, { params });
  }

  updateStatus(id: string, status: string): Observable<Patient> {
    return this.http.patch<Patient>(`${this.baseUrl}/${id}/status`, null, {
      params: new HttpParams().set('status', status),
    });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  search(query: string): Observable<Patient[]> {
    const params = new HttpParams().set('q', query);
    return this.http.get<Patient[]>(`${this.baseUrl}/search`, { params });
  }
}

// ── Billing Service ───────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class BillingService {
  private readonly http    = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/billing`;

  createInvoice(request: InvoiceRequest): Observable<Invoice> {
    return this.http
      .post<ApiResponse<Invoice>>(`${this.baseUrl}/invoices`, request)
      .pipe(map(res => res.data));
  }

  getInvoiceById(id: string): Observable<Invoice> {
    return this.http
      .get<ApiResponse<Invoice>>(`${this.baseUrl}/invoices/${id}`)
      .pipe(map(res => res.data));
  }

  getInvoicesByPatient(patientId: string): Observable<Invoice[]> {
    const params = new HttpParams().set('patientId', patientId);
    return this.http
      .get<ApiResponse<Invoice[]>>(`${this.baseUrl}/invoices`, { params })
      .pipe(map(res => res.data));
  }

  processPayment(request: PaymentRequest): Observable<Invoice> {
    return this.http
      .post<ApiResponse<Invoice>>(`${this.baseUrl}/payments`, request)
      .pipe(map(res => res.data));
  }
}

// ── Dashboard Service ─────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http    = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/dashboard`;

  getStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.baseUrl}/stats`);
  }
}
