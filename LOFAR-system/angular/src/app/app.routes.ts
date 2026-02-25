import { Routes } from '@angular/router';
import { SensorList } from './components/sensor-list/sensor-list';
import { SensorChart } from './components/sensor-chart/sensor-chart';
import { WsTestComponent } from './components/ws-test/ws-test';
import { AdminPanel } from './components/admin-panel/admin-panel';
import { HomePage } from './components/home-page/home-page';
import { LoginComponent } from './components/login/login';
import { AuthGuard } from './guards/auth.guard';
import { Role } from './models/role';

export const routes: Routes = [
  // Public — no guard, no roles needed
  { path: 'login', component: LoginComponent, data: { title: 'Login' } },

  // Root redirect
  { path: '', redirectTo: '/sensors', pathMatch: 'full' },

  // Accessible to both USER and ADMIN
  {
    path: 'home',
    component: HomePage,
    canActivate: [AuthGuard],
    data: { roles: [Role.Admin, Role.User], title: 'Home' }
  },
  {
    path: 'sensors',
    component: SensorList,
    canActivate: [AuthGuard],
    data: { roles: [Role.Admin, Role.User], title: 'Sensor Data' }
  },
  {
    path: 'charts',
    component: SensorChart,
    canActivate: [AuthGuard],
    data: { roles: [Role.Admin, Role.User], title: 'Charts' }
  },
  {
    path: 'ws-test',
    component: WsTestComponent,
    canActivate: [AuthGuard],
    data: { roles: [Role.Admin, Role.User], title: 'WebSocket Monitor' }
  },

  // ADMIN only
  {
    path: 'admin',
    component: AdminPanel,
    canActivate: [AuthGuard],
    data: { roles: [Role.Admin], title: 'Admin Panel' }
  },

  // Catch-all
  { path: '**', redirectTo: '/sensors' }
];