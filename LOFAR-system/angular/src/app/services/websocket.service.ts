import { Injectable } from '@angular/core';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import SockJS from 'sockjs-client';
import { Client, IMessage } from '@stomp/stompjs';
import { ClimateSensorData } from '../models/climate-sensor-data';
import { environment } from '../../env/enviroment';
//WebSocketService - Manages WebSocket connection

//Auto-reconnects on disconnect
//Subscribes to real-time sensor updates
//Handles connection status

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client | null = null;
  private sensorDataSubject = new Subject<ClimateSensorData>();
  // CHANGED: Use BehaviorSubject instead of Subject for connection status
  // This ensures components get the current status immediately when they subscribe
  private connectionStatus = new BehaviorSubject<boolean>(false);

  constructor() {
    this.connect();
  }

  connect(): void {
    const socket = new SockJS(environment.serverUrl + 'ws-sensor');
    
    this.stompClient = new Client({
      webSocketFactory: () => socket as any,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        console.log('STOMP: ' + str);
      }
    });

    this.stompClient.onConnect = (frame) => {
      console.log('Connected: ' + frame);
      // IMPORTANT: Set connection status to true
      this.connectionStatus.next(true);

      // Subscribe to sensor data updates
      this.stompClient?.subscribe('/topic/sensor-data', (message: IMessage) => {
        console.log('📥 Received sensor data:', message.body);
        const sensorData: ClimateSensorData = JSON.parse(message.body);
        this.sensorDataSubject.next(sensorData);
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
      this.connectionStatus.next(false);
    };

    this.stompClient.onDisconnect = () => {
      console.log('Disconnected from WebSocket');
      this.connectionStatus.next(false);
    };

    this.stompClient.activate();
  }

  disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.connectionStatus.next(false);
    }
  }

  getSensorDataUpdates(): Observable<ClimateSensorData> {
    return this.sensorDataSubject.asObservable();
  }

  getConnectionStatus(): Observable<boolean> {
    return this.connectionStatus.asObservable();
  }

  requestLatestData(): void {
    if (this.stompClient && this.stompClient.connected) {
      console.log('📤 Requesting latest data...');
      this.stompClient.publish({
        destination: '/app/sensor/request',
        body: ''
      });
    } else {
      console.warn('Cannot request data - not connected');
    }
  }

  requestHistoricalData(): void {
    if (this.stompClient && this.stompClient.connected) {
      console.log('📤 Requesting historical data...');
      this.stompClient.publish({
        destination: '/app/sensor/history',
        body: ''
      });
    } else {
      console.warn('Cannot request history - not connected');
    }
  }
}