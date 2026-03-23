import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  Appointment, AppointmentRequest, AppointmentStatusUpdate, ApiResponse
} from '../models/models';
import { environment } from '../../../environments/environment';

/**
 * Angular 17 service for Appointment API calls.
 *
 * Uses inject() instead of constructor injection (Angular 17 best practice).
 * Maintains a local BehaviorSubject cache to avoid redundant HTTP calls.
 */
@Injectable({ providedIn: 'root' })
export class AppointmentService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/appointments`;

  // Local state — components can subscribe to this directly
  private readonly _appointments$ = new BehaviorSubject<Appointment[]>([]);
  readonly appointments$ = this._appointments$.asObservable();

  schedule(request: AppointmentRequest): Observable<Appointment> {
    return this.http
      .post<ApiResponse<Appointment>>(this.baseUrl, request)
      .pipe(
        map(res => res.data),
        tap(appt => {
          const current = this._appointments$.getValue();
          this._appointments$.next([...current, appt]);
        })
      );
  }

  getById(id: string): Observable<Appointment> {
    return this.http
      .get<ApiResponse<Appointment>>(`${this.baseUrl}/${id}`)
      .pipe(map(res => res.data));
  }

  getByPatient(patientId: string): Observable<Appointment[]> {
    const params = new HttpParams().set('patientId', patientId);
    return this.http
      .get<ApiResponse<Appointment[]>>(this.baseUrl, { params })
      .pipe(
        map(res => res.data),
        tap(appointments => this._appointments$.next(appointments))
      );
  }

  updateStatus(id: string, update: AppointmentStatusUpdate): Observable<Appointment> {
    return this.http
      .patch<ApiResponse<Appointment>>(`${this.baseUrl}/${id}/status`, update)
      .pipe(
        map(res => res.data),
        tap(updated => {
          const current = this._appointments$.getValue();
          this._appointments$.next(
            current.map(a => a.appointmentId === id ? updated : a)
          );
        })
      );
  }

  cancel(id: string): Observable<Appointment> {
    return this.updateStatus(id, { status: 'CANCELLED', reason: 'Cancelled by user' });
  }
}
