import { Routes } from '@angular/router';
import { AuthBalloonsComponent } from './features/auth-balloons/auth-balloons.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { AdminDashboardComponent } from './features/admin-dashboard/admin-dashboard.component';
import { authGuard } from './core/guards/auth.guard';
import { VirementComponent } from './features/virement/virement.component';
import { CardDetailsComponent } from './features/card-details/card-details.component';
import { RendezVousComponent } from './features/rendez-vous/rendez-vous.component';
import { OperationsComponent } from './features/operations/operations.component';
import { InvestmentsComponent } from './features/investments/investments/investments.component';
import { InvestmentsHistoryComponent } from './features/investments/investments-history/investments-history.component';
import { SavingsPageComponent } from './features/savings/savings-page/savings-page.component';

export const routes: Routes = [
  { path: '', redirectTo: 'auth', pathMatch: 'full' },

  { path: 'auth', component: AuthBalloonsComponent },

  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard]
  },
  {
    path: 'admin',
    component: AdminDashboardComponent,
    canActivate: [authGuard]
  },
  {
    path: 'virement',
    component: VirementComponent,
    canActivate: [authGuard]
  },
  {
    path: 'card-details',
    component: CardDetailsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'rendez-vous',
    component: RendezVousComponent,
    canActivate: [authGuard]
  },
  {
    path: 'operations',
    component: OperationsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'investissements',
    component: InvestmentsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'investissements/historique',
    component: InvestmentsHistoryComponent,
    canActivate: [authGuard]
  },
  {
    path: 'epargne',
    component: SavingsPageComponent,
    canActivate: [authGuard]
  },

  { path: '**', redirectTo: 'auth' }
];
