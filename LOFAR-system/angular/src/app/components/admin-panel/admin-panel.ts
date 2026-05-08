import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { WebSocketService } from '../../services/websocket.service';

interface UserHourInfo {
  username: string;
  yearMonth: string;
  totalSeconds: number;
  totalHours: number;
}

@Component({
  selector: 'app-admin-panel',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-panel.html',
  styleUrl: './admin-panel.css',
})
export class AdminPanel implements OnInit {
  wsRunning       = signal<boolean>(true);
  wsConnected     = signal<boolean>(false);
  actionInProgress = signal<boolean>(false);
  statusMessage   = signal<string | null>(null);
  statusIsError   = signal<boolean>(false);
  schedulerEnabled = signal<boolean>(true);

  /** Current interval shown in the UI (minutes) */
  intervalMinutes = signal<number>(10);
  /** Value bound to the number input while the user is editing */
  intervalInput   = 10;
  intervalSaving  = signal<boolean>(false);

  /** User hours tracking */
  userHours = signal<UserHourInfo[]>([]);
  loadingUserHours = signal<boolean>(false);

  private readonly apiBase = 'http://localhost:8080/api/admin';

  constructor(
    private http: HttpClient,
    private wsService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.wsService.getConnectionStatus().subscribe(connected => {
      this.wsConnected.set(connected);
    });
    this.fetchSchedulerStatus();
    this.fetchUserHours();
  }

  fetchUserHours(): void {
    this.loadingUserHours.set(true);
    this.http.get<UserHourInfo[]>(`${this.apiBase}/stats/month/current`).subscribe({
      next: (data) => {
        this.userHours.set(data);
        this.loadingUserHours.set(false);
      },
      error: () => {
        this.loadingUserHours.set(false);
      }
    });
  }

  private fetchSchedulerStatus(): void {
    this.http.get<{ schedulerEnabled: boolean; intervalMinutes: number }>(
      `${this.apiBase}/scheduler/status`
    ).subscribe({
      next: (res) => {
        this.schedulerEnabled.set(res.schedulerEnabled);
        this.wsRunning.set(res.schedulerEnabled);
        this.intervalMinutes.set(res.intervalMinutes);
        this.intervalInput = res.intervalMinutes;
      },
      error: () => {}
    });
  }

  stopWebSocket(): void {
    this.actionInProgress.set(true);
    this.statusMessage.set(null);
    this.http.post(`${this.apiBase}/scheduler/stop`, {}).subscribe({
      next: () => {
        this.wsRunning.set(false);
        this.schedulerEnabled.set(false);
        this.wsService.disconnect();
        this.showStatus('✅ WebSocket scheduler stopped. No new data will be pushed.', false);
        this.actionInProgress.set(false);
      },
      error: (err) => {
        this.showStatus(`❌ Failed to stop scheduler: ${err.message}`, true);
        this.actionInProgress.set(false);
      }
    });
  }

  startWebSocket(): void {
    this.actionInProgress.set(true);
    this.statusMessage.set(null);
    this.http.post(`${this.apiBase}/scheduler/start`, {}).subscribe({
      next: () => {
        this.wsRunning.set(true);
        this.schedulerEnabled.set(true);
        this.wsService.connect();
        this.showStatus('✅ WebSocket scheduler started. Live data streaming resumed.', false);
        this.actionInProgress.set(false);
      },
      error: (err) => {
        this.showStatus(`❌ Failed to start scheduler: ${err.message}`, true);
        this.actionInProgress.set(false);
      }
    });
  }

  triggerManualUpdate(): void {
    this.actionInProgress.set(true);
    this.statusMessage.set(null);
    this.http.get(`${this.apiBase}/sensor/trigger`).subscribe({
      next: () => {
        this.showStatus('✅ Manual sensor update triggered successfully.', false);
        this.actionInProgress.set(false);
      },
      error: (err) => {
        this.showStatus(`❌ Manual trigger failed: ${err.message}`, true);
        this.actionInProgress.set(false);
      }
    });
  }

  saveInterval(): void {
    const minutes = this.intervalInput;
    if (!minutes || minutes < 1 || minutes > 1440) {
      this.showStatus('❌ Interval must be between 1 and 1440 minutes.', true);
      return;
    }
    this.intervalSaving.set(true);
    this.statusMessage.set(null);
    this.http.post(`${this.apiBase}/scheduler/interval`, { minutes }).subscribe({
      next: () => {
        this.intervalMinutes.set(minutes);
        this.showStatus(`✅ Scheduler interval updated to ${minutes} minute(s).`, false);
        this.intervalSaving.set(false);
      },
      error: (err) => {
        this.showStatus(`❌ Failed to update interval: ${err.message}`, true);
        this.intervalSaving.set(false);
      }
    });
  }

  private showStatus(msg: string, isError: boolean): void {
    this.statusMessage.set(msg);
    this.statusIsError.set(isError);
    setTimeout(() => this.statusMessage.set(null), 5000);
  }
}