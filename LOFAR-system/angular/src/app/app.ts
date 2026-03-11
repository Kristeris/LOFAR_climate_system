import { Component, signal, OnInit, inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { Navigation } from './components/navigation/navigation';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { filter } from 'rxjs/operators';
import { BnNgIdleService } from 'bn-ng-idle';
import { AuthService } from './services/auth.service';

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
export class App implements OnInit {
  showNav = signal(true);
  private platformId = inject(PLATFORM_ID);
  private cdr = inject(ChangeDetectorRef);

  constructor(
    private router: Router,
    private bnIdle: BnNgIdleService,
    private auth: AuthService
  ) {
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe((e: NavigationEnd) => {
      const url = e.urlAfterRedirects;
      this.showNav.set(!url.startsWith('/login') && !url.startsWith('/register'));
    });
  }

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.bnIdle.startWatching(600).subscribe((isTimedOut: boolean) => {
        if (isTimedOut && this.auth.isLoggedIn()) {
          this.auth.logout();
          this.cdr.detectChanges();
        }
      });
    }
  }
}