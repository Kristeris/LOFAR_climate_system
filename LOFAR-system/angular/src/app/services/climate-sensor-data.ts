import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ClimateSensorData } from '../models/climate-sensor-data';

@Injectable({
  providedIn: 'root',
})
export class ClimateSensorDataService {
  private apiUrl = `${environment.apiUrl}/sensors`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ClimateSensorData[]> {
    return this.http.get<ClimateSensorData[]>(this.apiUrl);
  }

  getById(id: number): Observable<ClimateSensorData> {
    return this.http.get<ClimateSensorData>(`${this.apiUrl}/${id}`);
  }

  getLatest10(): Observable<ClimateSensorData[]> {
    return this.http.get<ClimateSensorData[]>(`${this.apiUrl}/latest`);
  }

  create(data: ClimateSensorData): Observable<ClimateSensorData> {
    return this.http.post<ClimateSensorData>(this.apiUrl, data);
  }

  update(id: number, data: ClimateSensorData): Observable<ClimateSensorData> {
    return this.http.put<ClimateSensorData>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}


