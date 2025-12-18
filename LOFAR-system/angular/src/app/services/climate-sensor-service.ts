import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {Observable} from 'rxjs';
import {ClimateSensorData} from '../models/climate-sensor-data';
import { environment } from '../../env/enviroment';
import { map } from 'rxjs/operators';



@Injectable({
    providedIn: 'root'
})
export class ClimateSensorDataService {
    


    constructor(private http: HttpClient) { }

    // getAllSensorData(): Observable<ClimateSensorData[]> {
    //     var apiUrl = environment.serverUrl + 'api/sensors';
    //     return this.http.get<ClimateSensorData[]>(this.apiUrl);
    // }

    // getSensorDataById(id: number): Observable<ClimateSensorData> {
    // return this.http.get<ClimateSensorData>(`${this.apiUrl}/${id}`);
    // }

    
    // getAllSensorData(): Observable<ClimateSensorData[]> {
    //   const url = environment.serverUrl + 'users/all';
    //   const config = {
    //     headers: {
    //       'Content-Type': 'application/json'
    //     }
    //   };

    //   return this.http.get(url, config).pipe(
    //     map(res => res)
    //   );
    // }
    
    getAllSensorData() {
        const url = environment.serverUrl + 'users/all';
        const config = {
        headers: {
            'Content-Type': 'application/json'
        }
        };

        return this.http.get(url, config).pipe(
        map(res => res)
        );
    }

    
    

    
}