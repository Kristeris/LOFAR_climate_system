import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { WebSocketService } from '../../services/websocket.service';
import { ClimateSensorData } from '../../models/climate-sensor-data';

@Component({
  selector: 'app-ws-test',
  imports: [CommonModule, FormsModule],
  templateUrl: './ws-test.html',
  styleUrl: './ws-test.css'
})
export class WsTestComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  connected = signal<boolean>(false);
  lastData = signal<ClimateSensorData | null>(null);
  dataHistory = signal<ClimateSensorData[]>([]);
  log = signal<string[]>([]);
  updateCount = signal<number>(0);
  lastUpdateTime = signal<string>('Never');

  constructor(private ws: WebSocketService) {}

  ngOnInit(): void {
    this.addLog('SYSTEM', '🚀 WebSocket Test Component Initialized');
    this.addLog('SYSTEM', '⏳ Waiting for automatic updates from backend...');
    
    // Subscribe to connection status
    this.ws.getConnectionStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe(isConnected => {
        this.connected.set(isConnected);
        this.addLog('STATUS', isConnected ? 'CONNECTED ✅' : 'DISCONNECTED ❌');
      });

    // Subscribe to sensor data updates - AUTOMATIC REAL-TIME UPDATES
    this.ws.getSensorDataUpdates()
      .pipe(takeUntil(this.destroy$))
      .subscribe(data => {
        // Update latest data
        this.lastData.set(data);
        
        // Add to history (keep last 10)
        this.dataHistory.update(history => [data, ...history].slice(0, 10));
        
        // Increment counter
        this.updateCount.update(count => count + 1);
        
        // Update timestamp
        this.lastUpdateTime.set(new Date().toLocaleTimeString());
        
        // Log the update
        this.addLog('📥 RECEIVED', `New sensor data automatically pushed from backend!`);
        this.addLog('DATA', data);
      });
  }

  requestLatest(): void {
    this.addLog('OUT /app/sensor/request', 'Manually requesting latest sensor data...');
    this.ws.requestLatestData();
  }

  requestHistory(): void {
    this.addLog('OUT /app/sensor/history', 'Manually requesting historical data...');
    this.ws.requestHistoricalData();
  }

  clearLog(): void {
    this.log.set([]);
    this.addLog('SYSTEM', 'Log cleared');
  }

  clearHistory(): void {
    this.dataHistory.set([]);
    this.updateCount.set(0);
    this.addLog('SYSTEM', 'Data history cleared');
  }

  private addLog(tag: string, data: any): void {
    const timestamp = new Date().toLocaleTimeString();
    const line = `[${timestamp}] ${tag}: ${typeof data === 'string' ? data : JSON.stringify(data, null, 2)}`;
    this.log.update(logs => [line, ...logs].slice(0, 200));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}