import { Routes } from '@angular/router';
import { WsTest } from './components/ws-test/ws-test';
import { AdminPanel } from './components/admin-panel/admin-panel';

import { Login } from './components/login/login';
import { Register } from './components/register/register';
import { Forum } from './components/forum/forum';
import { TowerStatusComponent } from './components/tower-status/tower-status.component';
import { AuthGuard } from './guards/auth.guard';
import { Role } from './models/role';
import { HomePage } from './components/home-page/home-page';
import { PostDetailComponent } from './components/post-detail/post-detail.component';

export const routes: Routes = [
  // Public
  { path: 'login',    component: Login,    data: { title: 'Login' } },
  { path: 'register', component: Register, data: { title: 'Register' } },

  // Root redirect
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  // Home
  {
    path: 'home',
    component: HomePage,
    canActivate: [AuthGuard],
    data: { roles: [Role.User], title: 'Home' }
  },
  {
    path: 'home/post/:id',
    component: PostDetailComponent,
    canActivate: [AuthGuard],
    data: { roles: [Role.User], title: 'Post Detail' }
  },

  // Authenticated (USER + ADMIN)
  {
    path: 'antenna-status',
    component: TowerStatusComponent,
    canActivate: [AuthGuard],
    data: { roles: [Role.Admin, Role.User], title: 'Antenna Status' }
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
  // Catch-all
  { path: '**', redirectTo: '/home' }
];