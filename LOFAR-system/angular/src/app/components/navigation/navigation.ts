import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navigation',
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="navigation">
      <div class="nav-container">
        <a routerLink="/sensors" class="logo">
          <span class="logo-icon">📡</span>
          LOFAR Climate
        </a>
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
        </ul>
      </div>
    </nav>
  `,
  styles: [`
    .navigation {
      background-color: #1a252f;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
      position: sticky;
      top: 0;
      z-index: 1000;
      border-bottom: 1px solid rgba(255,255,255,0.06);
    }

    .nav-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 0 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      min-height: 62px;
    }

    .logo {
      color: white;
      font-size: 18px;
      font-weight: 700;
      text-decoration: none;
      display: flex;
      align-items: center;
      gap: 8px;
      letter-spacing: -0.01em;
      transition: opacity 0.2s;
    }

    .logo:hover { opacity: 0.85; }

    .logo-icon {
      font-size: 20px;
    }

    .nav-links {
      list-style: none;
      display: flex;
      gap: 4px;
      margin: 0;
      padding: 0;
    }

    .nav-links a {
      color: #9fb3c8;
      text-decoration: none;
      padding: 8px 16px;
      border-radius: 7px;
      font-weight: 500;
      font-size: 14px;
      transition: all 0.2s ease;
      display: block;
    }

    .nav-links a:hover {
      background-color: rgba(255,255,255,0.07);
      color: white;
    }

    .nav-links a.active {
      background-color: rgba(77, 171, 247, 0.15);
      color: #74c0fc;
    }

    .nav-links a.admin-link {
      color: #f8b400;
    }

    .nav-links a.admin-link:hover {
      background-color: rgba(248, 180, 0, 0.1);
      color: #ffd43b;
    }

    .nav-links a.admin-link.active {
      background-color: rgba(248, 180, 0, 0.15);
      color: #ffd43b;
    }

    @media (max-width: 768px) {
      .nav-container {
        flex-direction: column;
        padding: 12px 16px;
        gap: 10px;
      }

      .nav-links {
        gap: 2px;
        flex-wrap: wrap;
        justify-content: center;
      }

      .nav-links a {
        padding: 7px 12px;
        font-size: 13px;
      }
    }
  `]
})
export class Navigation {}