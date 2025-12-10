// // src/app/components/sensor-list/sensor-list.component.ts
// import { Component, OnInit } from '@angular/core';
// import { CommonModule } from '@angular/common';
// import { ClimateSensorDataService } from '../services/climate-sensor-service';
// import { ClimateSensorData } from '../models/climate-sensor-data';


// @Component({
//   selector: 'app-sensor-list',
//   standalone: true,
//   imports: [CommonModule],
//   templateUrl: './sensor-list.component.html',
//   styleUrls: ['./sensor-list.component.css']
// })
// export class SensorListComponent implements OnInit {
//   sensorData: ClimateSensorData[] = [];
//   loading = true;
//   error: string | null = null;

//   constructor(private sensorService: ClimateSensorDataService) { }

//   ngOnInit(): void {
//     this.loadSensorData();
//   }

//   loadSensorData(): void {
//     this.loading = true;
//     this.error = null;
//     this.sensorService.getAllSensorData().subscribe({
//       next: (data: ClimateSensorData[]) => {
//         this.sensorData = data;
//         this.loading = false;
//       },
//       error: (err: unknown) => {
//         this.error = 'Failed to load sensor data';
//         this.loading = false;
//         console.error('Error loading data:', err);
//       }
//     });
//   }

//   refresh(): void {
//     this.loadSensorData();
//   }
// }