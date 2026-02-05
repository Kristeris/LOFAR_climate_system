import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ClimateSensorDataService } from '../services/climate-sensor-service';
import { WebSocketService } from '../services/websocket.service';
import { ClimateSensorData } from '../models/climate-sensor-data';
//SensorListComponent - Displays real-time data

//Shows live updates as they arrive
//Maintains connection status indicator
//Limits stored data to last 50 readings

@Component({
  selector: 'app-sensor-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sensor-list.component.html',
  styleUrls: ['./sensor-list.component.css']
})
export class SensorListComponent implements OnInit, OnDestroy {
  sensorData: ClimateSensorData[] = [];
  latestSensorData: ClimateSensorData | null = null;
  loading = true;
  error: string | null = null;
  isConnected = false;

  private sensorSubscription: Subscription | null = null;
  private connectionSubscription: Subscription | null = null;

  constructor(
    private sensorService: ClimateSensorDataService,
    private webSocketService: WebSocketService
  ) { }

  ngOnInit(): void {
    this.loadInitialData();
    this.subscribeToWebSocket();
  }

  ngOnDestroy(): void {
    if (this.sensorSubscription) {
      this.sensorSubscription.unsubscribe();
    }
    if (this.connectionSubscription) {
      this.connectionSubscription.unsubscribe();
    }
  }

  loadInitialData(): void {
    this.loading = true;
    this.error = null;
    
    this.sensorService.getAllSensorData().subscribe({
      next: (data: any) => {
        this.sensorData = data;
        if (this.sensorData.length > 0) {
          this.latestSensorData = this.sensorData[0];
        }
        this.loading = false;
      },
      error: (err: unknown) => {
        this.error = 'Failed to load sensor data';
        this.loading = false;
        console.error('Error loading data:', err);
      }
    });
  }

  subscribeToWebSocket(): void {
    // Subscribe to connection status
    this.connectionSubscription = this.webSocketService.getConnectionStatus()
      .subscribe(status => {
        this.isConnected = status;
        console.log('WebSocket connection status:', status);
      });

    // Subscribe to real-time sensor data updates
    this.sensorSubscription = this.webSocketService.getSensorDataUpdates()
      .subscribe({
        next: (data: ClimateSensorData) => {
          console.log('Received new sensor data via WebSocket:', data);
          
          // Add new data to the beginning of the array
          this.sensorData.unshift(data);
          this.latestSensorData = data;
          
          // Keep only last 50 readings to avoid memory issues
          if (this.sensorData.length > 50) {
            this.sensorData = this.sensorData.slice(0, 50);
          }
        },
        error: (err: unknown) => {
          console.error('WebSocket error:', err);
          this.error = 'WebSocket connection error';
        }
      });
  }

  refresh(): void {
    this.loadInitialData();
    this.webSocketService.requestLatestData();
  }

  reconnectWebSocket(): void {
    this.webSocketService.disconnect();
    setTimeout(() => {
      this.webSocketService.connect();
    }, 1000);
  }
}