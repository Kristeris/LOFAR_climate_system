import { Component, signal } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { Navigation } from './components/navigation/navigation';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navigation, CommonModule],
  template: `
    @if (showNav()) {
      <app-navigation />
    }
    <router-outlet />
  `,
  styleUrl: './app.css'
})
export class App {
  showNav = signal(true);

  constructor(private router: Router) {
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe((e: NavigationEnd) => {
      this.showNav.set(!e.urlAfterRedirects.startsWith('/login'));
    });
  }
}