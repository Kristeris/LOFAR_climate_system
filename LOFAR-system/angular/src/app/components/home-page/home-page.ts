import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-home-page',
  imports: [CommonModule, RouterLink],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css'
})
export class HomePage implements OnInit {
  currentTime = signal<string>('');
  currentDate = signal<string>('');

  stats = signal<{ label: string; value: string; icon: string; color: string }[]>([
    { label: 'Active Sensors', value: '—', icon: '📡', color: '#4dabf7' },
    { label: 'Avg Temperature', value: '—', icon: '🌡️', color: '#ff6b6b' },
    { label: 'Avg Humidity', value: '—', icon: '💧', color: '#51cf66' },
    { label: 'Last Update', value: '—', icon: '🕐', color: '#fab005' },
  ]);

  features = [
    {
      icon: '📊',
      title: 'Table View',
      description: 'Browse all sensor readings in a structured, filterable table with real-time updates.',
      route: '/sensors',
      color: '#4dabf7'
    },
    {
      icon: '📈',
      title: 'Charts',
      description: 'Visualize temperature and humidity trends over time with interactive ECharts.',
      route: '/charts',
      color: '#ff6b6b'
    },
    {
      icon: '🔌',
      title: 'WebSocket Monitor',
      description: 'Watch live data streaming in real-time from the backend WebSocket connection.',
      route: '/ws-test',
      color: '#51cf66'
    },
    {
      icon: '⚙️',
      title: 'Admin Panel',
      description: 'Manage sensor configurations, trigger manual updates, and monitor system health.',
      route: '/admin',
      color: '#fab005'
    }
  ];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.updateClock();
    setInterval(() => this.updateClock(), 1000);
    this.loadStats();
  }

  private updateClock(): void {
    const now = new Date();
    this.currentTime.set(now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' }));
    this.currentDate.set(now.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }));
  }

  private loadStats(): void {
    this.http.get<any[]>('http://localhost:8080/api/sensors').subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          const avgTemp = (data.reduce((s, d) => s + d.temperature, 0) / data.length).toFixed(1);
          const avgHum = (data.reduce((s, d) => s + d.humidity, 0) / data.length).toFixed(1);
          const latest = data.sort((a, b) =>
            new Date(b.sensorDateTime).getTime() - new Date(a.sensorDateTime).getTime()
          )[0];
          const lastUpdate = new Date(latest.sensorDateTime).toLocaleString();

          this.stats.set([
            { label: 'Total Records', value: data.length.toString(), icon: '📡', color: '#4dabf7' },
            { label: 'Avg Temperature', value: `${avgTemp}°C`, icon: '🌡️', color: '#ff6b6b' },
            { label: 'Avg Humidity', value: `${avgHum}%`, icon: '💧', color: '#51cf66' },
            { label: 'Last Update', value: lastUpdate, icon: '🕐', color: '#fab005' },
          ]);
        }
      },
      error: () => {
        // Stats remain as dashes if backend is not available
      }
    });
  }
}