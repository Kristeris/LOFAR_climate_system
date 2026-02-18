import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { WebSocketService } from '../../services/websocket.service';

@Component({
  selector: 'app-admin-panel',
  imports: [CommonModule],
  templateUrl: './admin-panel.html',
  styleUrl: './admin-panel.css',
})
export class AdminPanel implements OnInit {
  /** Current WebSocket running state (mirrors backend scheduler state) */
  wsRunning = signal<boolean>(true);
  wsConnected = signal<boolean>(false);
  actionInProgress = signal<boolean>(false);
  statusMessage = signal<string | null>(null);
  statusIsError = signal<boolean>(false);
  schedulerEnabled = signal<boolean>(true);

  private readonly apiBase = 'http://localhost:8080/api/admin';

  constructor(
    private http: HttpClient,
    private wsService: WebSocketService
  ) {}

  ngOnInit(): void {
    // Track live WebSocket connection state
    this.wsService.getConnectionStatus().subscribe(connected => {
      this.wsConnected.set(connected);
    });

    // Fetch initial scheduler state from backend
    this.fetchSchedulerStatus();
  }

  private fetchSchedulerStatus(): void {
    this.http.get<{ schedulerEnabled: boolean }>(`${this.apiBase}/scheduler/status`).subscribe({
      next: (res) => {
        this.schedulerEnabled.set(res.schedulerEnabled);
        this.wsRunning.set(res.schedulerEnabled);
      },
      error: () => {
        // Backend may not be running; keep defaults
      }
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

  private showStatus(msg: string, isError: boolean): void {
    this.statusMessage.set(msg);
    this.statusIsError.set(isError);
    setTimeout(() => this.statusMessage.set(null), 5000);
  }
}