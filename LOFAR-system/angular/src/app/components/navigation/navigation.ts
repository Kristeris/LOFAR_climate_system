import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navigation',
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="navigation">
      <div class="nav-container">
        <h1 class="logo">LOFAR Climate System</h1>
        <ul class="nav-links">
          <li>
            <a routerLink="/sensors" routerLinkActive="active">
              Table View
            </a>
          </li>
          <li>
            <a routerLink="/charts" routerLinkActive="active">
              Charts
            </a>
          </li>
          <li>
            <a routerLink="/ws-test" routerLinkActive="active">
              🔌 WebSocket Test
            </a>
          </li>
        </ul>
      </div>
    </nav>
  `,
  styles: [`
    .navigation {
      background-color: #2c3e50;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      position: sticky;
      top: 0;
      z-index: 1000;
    }

    .nav-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 0 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      min-height: 60px;
    }

    .logo {
      color: white;
      font-size: 24px;
      font-weight: 700;
      margin: 0;
    }

    .nav-links {
      list-style: none;
      display: flex;
      gap: 20px;
      margin: 0;
      padding: 0;
    }

    .nav-links a {
      color: #ecf0f1;
      text-decoration: none;
      padding: 10px 20px;
      border-radius: 6px;
      font-weight: 500;
      transition: all 0.3s ease;
    }

    .nav-links a:hover {
      background-color: #34495e;
      color: white;
    }

    .nav-links a.active {
      background-color: #4dabf7;
      color: white;
    }

    @media (max-width: 768px) {
      .nav-container {
        flex-direction: column;
        padding: 15px 20px;
      }

      .logo {
        font-size: 20px;
        margin-bottom: 10px;
      }

      .nav-links {
        gap: 10px;
      }

      .nav-links a {
        padding: 8px 16px;
        font-size: 14px;
      }
    }
  `]
})
export class Navigation {}