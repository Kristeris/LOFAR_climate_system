import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClimateSensorData } from '../../models/climate-sensor-data';
import { ClimateSensorDataService } from '../../services/climate-sensor-data';

@Component({
  selector: 'app-sensor-list',
  imports: [CommonModule],
  templateUrl: './sensor-list.html',
  styleUrl: './sensor-list.css',
})
export class SensorList implements OnInit {
  sensors = signal<ClimateSensorData[]>([]);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);

  constructor(private sensorService: ClimateSensorDataService) {}

  ngOnInit(): void {
    this.loadSensors();
  }

  loadSensors(): void {
    this.loading.set(true);
    this.sensorService.getAll().subscribe({
      next: (data) => {
        this.sensors.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load sensor data');
        this.loading.set(false);
        console.error('Error loading sensors:', err);
      },
    });
  }

  loadLatest10(): void {
    this.loading.set(true);
    this.sensorService.getLatest10().subscribe({
      next: (data) => {
        this.sensors.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load sensor data');
        this.loading.set(false);
        console.error('Error loading sensors:', err);
      },
    });
  }
}