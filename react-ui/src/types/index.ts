/**
 * Shared TypeScript types — mirrors Spring Boot DTOs.
 * Shared with Angular via a monorepo libs/ package in production setups.
 */

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

// ── Patient ──────────────────────────────────────────────────────────────────
export type PatientStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';

export interface Patient {
  patientId:   string;
  firstName:   string;
  lastName:    string;
  email:       string;
  dateOfBirth: string;
  status:      PatientStatus;
}

// ── Appointment ───────────────────────────────────────────────────────────────
export type AppointmentStatus = 'SCHEDULED' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';

export interface Appointment {
  appointmentId: string;
  patientId:     string;
  doctorId:      string;
  scheduledAt:   string;
  status:        AppointmentStatus;
  notes?:        string;
  createdAt:     string;
}

export interface AppointmentRequest {
  patientId:   string;
  doctorId:    string;
  scheduledAt: string;
  notes?:      string;
}

// ── Billing ───────────────────────────────────────────────────────────────────
export type InvoiceStatus = 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELLED';

export interface Invoice {
  invoiceId:      string;
  patientId:      string;
  appointmentId?: string;
  amount:         number;
  status:         InvoiceStatus;
  dueDate:        string;
  paidAt?:        string;
  createdAt:      string;
}

export interface InvoiceRequest {
  patientId:      string;
  appointmentId?: string;
  amount:         number;
  dueDate:        string;
}

export interface PaymentRequest {
  invoiceId:  string;
  amountPaid: number;
}

// ── Dashboard ─────────────────────────────────────────────────────────────────
export interface DashboardStats {
  totalPatients:    number;
  activePatients:   number;
  todayAppointments: number;
  pendingInvoices:  number;
  overdueInvoices:  number;
  totalRevenue:     number;
}
