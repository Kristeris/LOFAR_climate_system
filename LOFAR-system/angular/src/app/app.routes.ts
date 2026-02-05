import { Routes } from '@angular/router';
import { SensorList } from './components/sensor-list/sensor-list';
import { SensorChart } from './components/sensor-chart/sensor-chart';

export const routes: Routes = [
    { path: '', redirectTo: '/sensors', pathMatch: 'full' },
    { path: 'sensors', component: SensorList },
    { path: 'charts', component: SensorChart }
];
