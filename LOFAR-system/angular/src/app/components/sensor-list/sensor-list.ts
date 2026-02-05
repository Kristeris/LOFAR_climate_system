import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClimateSensorData } from '../../models/climate-sensor-data';
import { ClimateSensorDataService } from '../../services/climate-sensor-data';
import { DateFilter, DateFilterCriteria } from '../date-filter/date-filter';

@Component({
  selector: 'app-sensor-list',
  imports: [CommonModule, DateFilter],
  templateUrl: './sensor-list.html',
  styleUrl: './sensor-list.css',
})
export class SensorList implements OnInit {
  sensors = signal<ClimateSensorData[]>([]);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);
  filterCriteria = signal<DateFilterCriteria>({
    startDate: null,
    endDate: null,
    startTime: null,
    endTime: null,
  });

  filteredSensors = computed(() => {
    const data = this.sensors();
    const criteria = this.filterCriteria();

    if (!criteria.startDate && !criteria.endDate && !criteria.startTime && !criteria.endTime) {
      return data;
    }

    return data.filter((sensor) => {
      const sensorDateTime = new Date(sensor.sensorDateTime);
      
      // Build start datetime for comparison
      let startDateTime: Date | null = null;
      if (criteria.startDate) {
        const timeStr = criteria.startTime || '00:00';
        startDateTime = new Date(`${criteria.startDate}T${timeStr}:00`);
      }

      // Build end datetime for comparison
      let endDateTime: Date | null = null;
      if (criteria.endDate) {
        const timeStr = criteria.endTime || '23:59';
        endDateTime = new Date(`${criteria.endDate}T${timeStr}:59`);
      }

      // Apply filters
      if (startDateTime && sensorDateTime < startDateTime) {
        return false;
      }

      if (endDateTime && sensorDateTime > endDateTime) {
        return false;
      }

      return true;
    });
  });

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

  onFilterChange(criteria: DateFilterCriteria): void {
    this.filterCriteria.set(criteria);
  }
}