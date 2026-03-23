import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

/**
 * Application routes — all feature modules are lazy-loaded.
 * Guards protect routes based on authentication and role.
 */
export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard],
    title: 'Dashboard | Healthcare Platform',
  },
  {
    path: 'patients',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/patients/patient-list.component').then(m => m.PatientListComponent),
        title: 'Patients | Healthcare Platform',
      },
      {
        path: ':id',
        loadComponent: () =>
          import('./features/patients/patient-detail.component').then(m => m.PatientDetailComponent),
        title: 'Patient Detail | Healthcare Platform',
      },
    ],
  },
  {
    path: 'appointments',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/appointments/appointment-list.component').then(m => m.AppointmentListComponent),
        title: 'Appointments | Healthcare Platform',
      },
      {
        path: 'new',
        loadComponent: () =>
          import('./features/appointments/appointment-form.component').then(m => m.AppointmentFormComponent),
        canActivate: [roleGuard],
        data: { roles: ['CLINICIAN', 'ADMIN'] },
        title: 'New Appointment | Healthcare Platform',
      },
    ],
  },
  {
    path: 'billing',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['BILLING', 'ADMIN'] },
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/billing/invoice-list.component').then(m => m.InvoiceListComponent),
        title: 'Billing | Healthcare Platform',
      },
    ],
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./core/guards/login.component').then(m => m.LoginComponent),
    title: 'Login | Healthcare Platform',
  },
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
