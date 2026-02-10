import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ClimateSensorData } from '../../models/climate-sensor-data';
import { ClimateSensorDataService } from '../../services/climate-sensor-data';
import { WebSocketService } from '../../services/websocket.service';
import { DateFilter, DateFilterCriteria } from '../date-filter/date-filter';

@Component({
  selector: 'app-sensor-list',
  imports: [CommonModule, DateFilter],
  templateUrl: './sensor-list.html',
  styleUrl: './sensor-list.css',
})
export class SensorList implements OnInit, OnDestroy {
  private webSocketSubscription: Subscription | null = null;
  private connectionSubscription: Subscription | null = null;
  private newRowIds = new Set<number>();
  
  sensors = signal<ClimateSensorData[]>([]);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);
  wsConnected = signal<boolean>(false);
  lastUpdateTime = signal<string>('Never');
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

  constructor(
    private sensorService: ClimateSensorDataService,
    private webSocketService: WebSocketService
  ) {}

  ngOnInit(): void {
    this.loadSensors();
    this.subscribeToWebSocketUpdates();
    this.subscribeToConnectionStatus();
  }

  ngOnDestroy(): void {
    if (this.webSocketSubscription) {
      this.webSocketSubscription.unsubscribe();
    }
    if (this.connectionSubscription) {
      this.connectionSubscription.unsubscribe();
    }
  }

  private subscribeToConnectionStatus(): void {
    this.connectionSubscription = this.webSocketService.getConnectionStatus().subscribe({
      next: (isConnected) => {
        this.wsConnected.set(isConnected);
        console.log('WebSocket connection status:', isConnected ? 'CONNECTED' : 'DISCONNECTED');
      }
    });
  }

  private subscribeToWebSocketUpdates(): void {
    this.webSocketSubscription = this.webSocketService.getSensorDataUpdates().subscribe({
      next: (newSensorData: ClimateSensorData) => {
        const currentSensors = this.sensors();
        // Check if this sensor already exists (avoid duplicates)
        const exists = currentSensors.some(s => 
          s.sensorId === newSensorData.sensorId && 
          new Date(s.sensorDateTime).getTime() === new Date(newSensorData.sensorDateTime).getTime()
        );
        
        if (!exists) {
          // Add new data to the beginning of the list
          const updatedSensors = [newSensorData, ...currentSensors];
          this.sensors.set(updatedSensors);
          this.lastUpdateTime.set(new Date().toLocaleTimeString());
          
          // Mark as new row for highlighting
          this.newRowIds.add(newSensorData.sensorId);
          
          // Remove highlight after 3 seconds
          setTimeout(() => {
            this.newRowIds.delete(newSensorData.sensorId);
          }, 3000);
          
          console.log('📥 New sensor data received and added to list:', newSensorData);
        }
      },
      error: (err) => {
        console.error('Error receiving WebSocket updates:', err);
      }
    });
  }

  isNewRow(sensor: ClimateSensorData): boolean {
    return this.newRowIds.has(sensor.sensorId);
  }

  loadSensors(): void {
    this.loading.set(true);
    this.newRowIds.clear(); // Clear highlights when loading new data
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
    this.newRowIds.clear(); // Clear highlights when loading new data
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