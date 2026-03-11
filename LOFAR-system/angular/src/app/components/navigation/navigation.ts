import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navigation',
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="navigation">
      <div class="nav-container">
        <!-- Brand -->
        <a routerLink="/sensors" class="logo">
          <span class="logo-icon">📡</span>
          LOFAR Climate
        </a>

        <!-- Nav links - shown to all authenticated users -->
        @if (auth.isLoggedIn()) {
          <ul class="nav-links">
            <li>
              <a routerLink="/sensors" routerLinkActive="active">
                📋 Table View
              </a>
            </li>
            <li>
              <a routerLink="/charts" routerLinkActive="active">
                📈 Charts
              </a>
            </li>
            <!-- Admin-only links -->
            @if (auth.isAdmin()) {
              <li>
                <a routerLink="/ws-test" routerLinkActive="active">
                  🔌 WebSocket
                </a>
              </li>
              <li>
                <a routerLink="/admin" routerLinkActive="active" class="admin-link">
                  ⚙️ Admin
                </a>
              </li>
            }
          </ul>

          <!-- User info + logout -->
          <div class="nav-user">
            <div class="user-badge" [class.badge-admin]="auth.isAdmin()">
              <span class="user-role-dot"></span>
              <span class="user-name">{{ auth.currentUser()?.username }}</span>
              <span class="user-role">{{ auth.isAdmin() ? 'ADMIN' : 'USER' }}</span>
            </div>
            <button class="logout-btn" (click)="auth.logout()">
              <span>⏻</span>
              Logout
            </button>
          </div>
        }
      </div>
    </nav>
  `,
  styles: [`
    .navigation {
      background-color: #0d1520;
      box-shadow: 0 1px 0 rgba(0, 180, 255, 0.12), 0 2px 16px rgba(0,0,0,0.4);
      position: sticky;
      top: 0;
      z-index: 1000;
    }

    .nav-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 0 24px;
      display: flex;
      align-items: center;
      min-height: 62px;
      gap: 8px;
    }

    .logo {
      color: #00c8ff;
      font-size: 17px;
      font-weight: 700;
      text-decoration: none;
      display: flex;
      align-items: center;
      gap: 8px;
      letter-spacing: 0.01em;
      transition: opacity 0.2s;
      flex-shrink: 0;
      margin-right: 12px;
      font-family: 'Share Tech Mono', 'Courier New', monospace;
    }

    .logo:hover { opacity: 0.8; }

    .logo-icon { font-size: 18px; }

    /* Nav links */
    .nav-links {
      list-style: none;
      display: flex;
      gap: 2px;
      margin: 0;
      padding: 0;
      flex: 1;
    }

    .nav-links a {
      color: #6b8499;
      text-decoration: none;
      padding: 8px 14px;
      border-radius: 4px;
      font-weight: 500;
      font-size: 13.5px;
      transition: all 0.2s ease;
      display: block;
      white-space: nowrap;
    }

    .nav-links a:hover {
      background-color: rgba(0, 200, 255, 0.07);
      color: #c8dde8;
    }

    .nav-links a.active {
      background-color: rgba(0, 200, 255, 0.12);
      color: #00c8ff;
    }

    .nav-links a.admin-link { color: #f8b400; }

    .nav-links a.admin-link:hover {
      background-color: rgba(248, 180, 0, 0.1);
      color: #ffd43b;
    }

    .nav-links a.admin-link.active {
      background-color: rgba(248, 180, 0, 0.14);
      color: #ffd43b;
    }

    /* User section */
    .nav-user {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-left: auto;
      flex-shrink: 0;
    }

    .user-badge {
      display: flex;
      align-items: center;
      gap: 8px;
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 100px;
      padding: 5px 12px 5px 8px;
    }

    .user-badge.badge-admin {
      border-color: rgba(248, 180, 0, 0.25);
      background: rgba(248, 180, 0, 0.06);
    }

    .user-role-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #51cf66;
      flex-shrink: 0;
      animation: dotPulse 2.5s ease-in-out infinite;
    }

    .user-badge.badge-admin .user-role-dot {
      background: #f8b400;
    }

    @keyframes dotPulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.6; transform: scale(1.2); }
    }

    .user-name {
      color: #c8dde8;
      font-size: 13px;
      font-weight: 600;
    }

    .user-role {
      font-size: 10px;
      font-weight: 700;
      letter-spacing: 0.1em;
      color: #51cf66;
      background: rgba(81, 207, 102, 0.12);
      padding: 2px 7px;
      border-radius: 100px;
    }

    .user-badge.badge-admin .user-role {
      color: #f8b400;
      background: rgba(248, 180, 0, 0.12);
    }

    .logout-btn {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 7px 14px;
      background: rgba(255, 56, 96, 0.1);
      border: 1px solid rgba(255, 56, 96, 0.25);
      border-radius: 4px;
      color: #ff6b8a;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      white-space: nowrap;
    }

    .logout-btn:hover {
      background: rgba(255, 56, 96, 0.2);
      border-color: rgba(255, 56, 96, 0.5);
      color: #ff4d6d;
      transform: translateY(-1px);
    }

    @media (max-width: 900px) {
      .nav-container {
        flex-wrap: wrap;
        padding: 10px 16px;
        gap: 8px;
        min-height: auto;
      }

      .logo { margin-right: 0; }

      .nav-links {
        order: 3;
        flex: 0 0 100%;
        gap: 2px;
        flex-wrap: wrap;
      }

      .nav-links a { font-size: 12px; padding: 6px 10px; }

      .nav-user { margin-left: auto; }

      .user-name { display: none; }
    }

    @media (max-width: 480px) {
      .user-badge { display: none; }
    }
  `]
})
export class Navigation {
  auth = inject(AuthService);
}