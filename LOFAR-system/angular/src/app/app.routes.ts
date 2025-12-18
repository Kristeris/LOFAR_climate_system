import { Routes } from '@angular/router';
import { SensorList } from './components/sensor-list/sensor-list';

export const routes: Routes = [
    { path: '', redirectTo: '/sensors', pathMatch: 'full' },
    { path: 'sensors', component: SensorList }
];
