import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import * as SockJS from 'sockjs-client';
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
  private connectionStatus = new Subject<boolean>();

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
      this.connectionStatus.next(true);

      // Subscribe to sensor data updates
      this.stompClient?.subscribe('/topic/sensor-data', (message: IMessage) => {
        const sensorData: ClimateSensorData = JSON.parse(message.body);
        this.sensorDataSubject.next(sensorData);
      });
    };

    this.stompClient.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
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
      this.stompClient.publish({
        destination: '/app/sensor/request',
        body: ''
      });
    }
  }

  requestHistoricalData(): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination: '/app/sensor/history',
        body: ''
      });
    }
  }
}