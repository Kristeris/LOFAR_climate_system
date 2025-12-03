import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {Observable} from 'rxjs';
import {ClimateSensorData} from '../models/climate-sensor-data';

@Injectable({
    providedIn: 'root'
})
export class ClimateSensorDataService {
    private apiUrl = 'http://localhost:8080/api/sensors';


    constructor(private http: HttpClient) { }

    getAllSensorData(): Observable<ClimateSensorData[]> {
    return this.http.get<ClimateSensorData[]>(this.apiUrl);
    }

    getSensorDataById(id: number): Observable<ClimateSensorData> {
    return this.http.get<ClimateSensorData>(`${this.apiUrl}/${id}`);
    }

    createSensorData(data: ClimateSensorData): Observable<ClimateSensorData> {
    return this.http.post<ClimateSensorData>(this.apiUrl, data);
    }

    
}