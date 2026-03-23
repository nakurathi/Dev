/**
 * TypeScript domain models mirroring the Spring Boot backend DTOs.
 * Used across Angular components, services, and React app.
 */

// ── Shared ───────────────────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
}

// ── Patient ──────────────────────────────────────────────────────────────────

export interface Patient {
  patientId: string;
  firstName: string;
  lastName: string;
  email: string;
  dateOfBirth: string;  // ISO date
  status: PatientStatus;
}

export type PatientStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';

export interface PatientRequest {
  firstName: string;
  lastName: string;
  email: string;
  dateOfBirth: string;
  status?: PatientStatus;
}

// ── Appointment ───────────────────────────────────────────────────────────────

export interface Appointment {
  appointmentId: string;
  patientId: string;
  doctorId: string;
  scheduledAt: string;   // ISO timestamp
  status: AppointmentStatus;
  notes?: string;
  createdAt: string;
}

export type AppointmentStatus = 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';

export interface AppointmentRequest {
  patientId: string;
  doctorId: string;
  scheduledAt: string;
  notes?: string;
}

export interface AppointmentStatusUpdate {
  status: AppointmentStatus;
  reason?: string;
}

// ── Billing ───────────────────────────────────────────────────────────────────

export interface Invoice {
  invoiceId: string;
  patientId: string;
  appointmentId?: string;
  amount: number;
  status: InvoiceStatus;
  dueDate: string;
  paidAt?: string;
  createdAt: string;
}

export type InvoiceStatus = 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELLED';

export interface InvoiceRequest {
  patientId: string;
  appointmentId?: string;
  amount: number;
  dueDate: string;
}

export interface PaymentRequest {
  invoiceId: string;
  amountPaid: number;
}

// ── Dashboard ─────────────────────────────────────────────────────────────────

export interface DashboardStats {
  totalPatients: number;
  activePatients: number;
  todayAppointments: number;
  pendingInvoices: number;
  overdueInvoices: number;
  totalRevenue: number;
}
