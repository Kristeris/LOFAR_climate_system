import { Routes } from '@angular/router';
import { SensorList } from './components/sensor-list/sensor-list';
import { SensorChart } from './components/sensor-chart/sensor-chart';
import { WsTest } from './components/ws-test/ws-test';
import { AdminPanel } from './components/admin-panel/admin-panel';
import { HomePage } from './components/home-page/home-page';
import { Login } from './components/login/login';
import { Register } from './components/register/register';
import { Forum } from './components/forum/forum';
import { TowerStatusComponent } from './components/tower-status/tower-status.component';
import { AuthGuard } from './guards/auth.guard';
import { Role } from './models/role';
import { NohupMonitorComponent } from './components/nohup-monitor/nohup-monitor.component';

export const routes: Routes = [
  // Public
  { path: 'login',    component: Login,    data: { title: 'Login' } },
  { path: 'register', component: Register, data: { title: 'Register' } },

  // Root redirect
  { path: '', redirectTo: '/sensors', pathMatch: 'full' },

  // Authenticated (USER + ADMIN)
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
    path: 'tower-status',
    component: TowerStatusComponent,
    canActivate: [AuthGuard],
    data: { roles: [Role.Admin, Role.User], title: 'Tower Status' }
  },
  {
    path: 'forum',
    component: Forum,
    canActivate: [AuthGuard],
    data: { roles: [Role.Admin, Role.User], title: 'Forum' }
  },

  // ADMIN only
  {
    path: 'admin',
    component: AdminPanel,
    canActivate: [AuthGuard],
    data: { roles: [Role.Admin], title: 'Admin Panel' }
  },
  {
    path: 'ws-test',
    component: WsTest,
    canActivate: [AuthGuard],
    data: { roles: [Role.Admin], title: 'WebSocket Monitor' }
  },
  {
    path: 'monitoring',
    component: NohupMonitorComponent,
    canActivate: [AuthGuard],
    data: { roles: [Role.Admin, Role.User], title: 'Nohup Monitor' }
  },

  // Catch-all
  { path: '**', redirectTo: '/sensors' }
];