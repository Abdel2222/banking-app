import { Routes, provideRouter } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { BankerListComponent } from './pages/banker-list/banker-list.component';
import { LoanApplicationListComponent } from './pages/loan-application-list/loan-application-list.component';
import { CustomerListComponent } from './pages/customer-list/customer-list.component';
import { NewLoanFormComponent } from './pages/new-loan-form/new-loan-form.component';

export const appRoutes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: 'bankerlist',
    component: BankerListComponent,
  },
  {
    path: 'customerlist',
    component: CustomerListComponent,
  },
  {
    path: 'application-list',
    component: LoanApplicationListComponent,
  },
  {
    path: 'new-loan',
    component: NewLoanFormComponent,
  },
];

// 🔹 Active le système de routage pour Angular 19
export const appRouting = provideRouter(appRoutes);
