import { Component, OnInit, OnDestroy, signal, effect, PLATFORM_ID, inject, computed } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Subscription } from 'rxjs';
import * as echarts from 'echarts';
import { ClimateSensorData } from '../../models/climate-sensor-data';
import { ClimateSensorDataService } from '../../services/climate-sensor-data';
import { WebSocketService } from '../../services/websocket.service';
import { DateFilter, DateFilterCriteria } from '../date-filter/date-filter';


@Component({
  selector: 'app-sensor-chart',
  imports: [CommonModule, DateFilter],
  templateUrl: './sensor-chart.html',
  styleUrl: './sensor-chart.css',
})
export class SensorChart implements OnInit, OnDestroy {
  private webSocketSubscription: Subscription | null = null;
  sensors = signal<ClimateSensorData[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);
  activeDropdown = signal<string | null>(null);
  wsConnected = signal<boolean>(false);
  lastUpdateTime = signal<string>('Never');
  filterCriteria = signal<DateFilterCriteria>({
    startDate: null,
    endDate: null,
    startTime: null,
    endTime: null,
  });
  
  private temperatureChart: echarts.ECharts | null = null;
  private humidityChart: echarts.ECharts | null = null;
  private combinedChart: echarts.ECharts | null = null;
  private platformId = inject(PLATFORM_ID);
  private isBrowser: boolean;
  private chartsInitialized = false;

  filteredSensors = computed(() => {
    const data = this.sensors();
    const criteria = this.filterCriteria();

    if (!criteria.startDate && !criteria.endDate && !criteria.startTime && !criteria.endTime) {
      return data;
    }

    return data.filter((sensor) => {
      const sensorDateTime = new Date(sensor.sensorDateTime);
      
      let startDateTime: Date | null = null;
      if (criteria.startDate) {
        const timeStr = criteria.startTime || '00:00';
        startDateTime = new Date(`${criteria.startDate}T${timeStr}:00`);
      }

      let endDateTime: Date | null = null;
      if (criteria.endDate) {
        const timeStr = criteria.endTime || '23:59';
        endDateTime = new Date(`${criteria.endDate}T${timeStr}:59`);
      }

      if (startDateTime && sensorDateTime < startDateTime) {
        return false;
      }

      if (endDateTime && sensorDateTime > endDateTime) {
        return false;
      }

      return true;
    });
  });

  constructor(private sensorService: ClimateSensorDataService, private webSocketService: WebSocketService) {
    this.isBrowser = isPlatformBrowser(this.platformId);
    
    effect(() => {
      const data = this.filteredSensors();
      const isLoading = this.loading();
      
      if (data.length > 0 && !isLoading && this.isBrowser) {
        setTimeout(() => {
          this.ensureChartsInitialized();
          this.updateCharts(data);
        }, 150);
      }
    });

    if (this.isBrowser) {
      document.addEventListener('click', this.handleClickOutside.bind(this));
    }
  }

  ngOnInit(): void {
    this.loadSensors();
    this.subscribeToWebSocketUpdates();
    this.subscribeToConnectionStatus();
  }

  ngOnDestroy(): void {
    if (this.webSocketSubscription) {
      this.webSocketSubscription.unsubscribe();
    }
    this.disposeCharts();
    if (this.isBrowser) {
      document.removeEventListener('click', this.handleClickOutside.bind(this));
    }
  }

  private subscribeToConnectionStatus(): void {
    this.webSocketService.getConnectionStatus().subscribe({
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
        const exists = currentSensors.some(s => s.sensorId === newSensorData.sensorId && 
          new Date(s.sensorDateTime).getTime() === new Date(newSensorData.sensorDateTime).getTime());
        
        if (!exists) {
          const updatedSensors = [newSensorData, ...currentSensors];
          this.sensors.set(updatedSensors);
          this.lastUpdateTime.set(new Date().toLocaleTimeString());
          console.log('📥 New sensor data received - charts will auto-update:', newSensorData);
        }
      },
      error: (err) => {
        console.error('Error receiving WebSocket updates:', err);
      }
    });
  }

  private handleClickOutside(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.download-dropdown')) {
      this.activeDropdown.set(null);
    }
  }

  toggleDropdown(chartType: string): void {
    if (this.activeDropdown() === chartType) {
      this.activeDropdown.set(null);
    } else {
      this.activeDropdown.set(chartType);
    }
  }

  private disposeCharts(): void {
    if (this.temperatureChart) {
      this.temperatureChart.dispose();
      this.temperatureChart = null;
    }
    if (this.humidityChart) {
      this.humidityChart.dispose();
      this.humidityChart = null;
    }
    if (this.combinedChart) {
      this.combinedChart.dispose();
      this.combinedChart = null;
    }
    this.chartsInitialized = false;
  }

  private ensureChartsInitialized(): void {
    if (!this.isBrowser || this.chartsInitialized) return;

    const tempChartDom = document.getElementById('temperatureChart');
    if (tempChartDom && !this.temperatureChart) {
      this.temperatureChart = echarts.init(tempChartDom);
    }

    const humidChartDom = document.getElementById('humidityChart');
    if (humidChartDom && !this.humidityChart) {
      this.humidityChart = echarts.init(humidChartDom);
    }

    const combinedChartDom = document.getElementById('combinedChart');
    if (combinedChartDom && !this.combinedChart) {
      this.combinedChart = echarts.init(combinedChartDom);
    }

    if (this.temperatureChart && this.humidityChart && this.combinedChart) {
      this.chartsInitialized = true;
    }
  }

  private updateCharts(data: ClimateSensorData[]): void {
    if (!this.isBrowser || !this.chartsInitialized) {
      return;
    }

    const sortedData = [...data].sort((a, b) => 
      new Date(a.sensorDateTime).getTime() - new Date(b.sensorDateTime).getTime()
    );

    const dates = sortedData.map(s => 
      new Date(s.sensorDateTime).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    );
    const temperatures = sortedData.map(s => s.temperature);
    const humidity = sortedData.map(s => s.humidity);

    if (this.temperatureChart) {
      this.temperatureChart.setOption({
        title: {
          text: 'Temperature Over Time',
          left: 'center',
          textStyle: { color: '#333', fontSize: 18 }
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}<br/>Temperature: {c}°C'
        },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: { rotate: 45, interval: 'auto' }
        },
        yAxis: {
          type: 'value',
          name: 'Temperature (°C)',
          axisLabel: { formatter: '{value}°C' }
        },
        series: [{
          name: 'Temperature',
          type: 'line',
          data: temperatures,
          smooth: true,
          lineStyle: { color: '#ff6b6b', width: 3 },
          itemStyle: { color: '#ff6b6b' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(255, 107, 107, 0.5)' },
              { offset: 1, color: 'rgba(255, 107, 107, 0.1)' }
            ])
          }
        }],
        grid: { left: '10%', right: '10%', bottom: '20%', containLabel: true }
      });
      this.temperatureChart.resize();
    }

    if (this.humidityChart) {
      this.humidityChart.setOption({
        title: {
          text: 'Humidity Over Time',
          left: 'center',
          textStyle: { color: '#333', fontSize: 18 }
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}<br/>Humidity: {c}%'
        },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: { rotate: 45, interval: 'auto' }
        },
        yAxis: {
          type: 'value',
          name: 'Humidity (%)',
          axisLabel: { formatter: '{value}%' }
        },
        series: [{
          name: 'Humidity',
          type: 'line',
          data: humidity,
          smooth: true,
          lineStyle: { color: '#4dabf7', width: 3 },
          itemStyle: { color: '#4dabf7' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(77, 171, 247, 0.5)' },
              { offset: 1, color: 'rgba(77, 171, 247, 0.1)' }
            ])
          }
        }],
        grid: { left: '10%', right: '10%', bottom: '20%', containLabel: true }
      });
      this.humidityChart.resize();
    }

    if (this.combinedChart) {
      this.combinedChart.setOption({
        title: {
          text: 'Temperature & Humidity',
          left: 'center',
          textStyle: { color: '#333', fontSize: 18 }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross' }
        },
        legend: {
          data: ['Temperature', 'Humidity'],
          top: 30
        },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: { rotate: 45, interval: 'auto' }
        },
        yAxis: [
          {
            type: 'value',
            name: 'Temperature (°C)',
            position: 'left',
            axisLabel: { formatter: '{value}°C' }
          },
          {
            type: 'value',
            name: 'Humidity (%)',
            position: 'right',
            axisLabel: { formatter: '{value}%' }
          }
        ],
        series: [
          {
            name: 'Temperature',
            type: 'line',
            yAxisIndex: 0,
            data: temperatures,
            smooth: true,
            lineStyle: { color: '#ff6b6b', width: 2 },
            itemStyle: { color: '#ff6b6b' }
          },
          {
            name: 'Humidity',
            type: 'line',
            yAxisIndex: 1,
            data: humidity,
            smooth: true,
            lineStyle: { color: '#4dabf7', width: 2 },
            itemStyle: { color: '#4dabf7' }
          }
        ],
        grid: { left: '10%', right: '10%', bottom: '20%', containLabel: true }
      });
      this.combinedChart.resize();
    }
  }

  loadSensors(): void {
    this.loading.set(true);
    this.error.set(null);
    
    if (this.chartsInitialized) {
      this.disposeCharts();
    }
    
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
    this.error.set(null);
    
    if (this.chartsInitialized) {
      this.disposeCharts();
    }
    
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

  downloadChart(chartType: 'temperature' | 'humidity' | 'combined', format: 'png' | 'jpeg' | 'csv'): void {
    this.activeDropdown.set(null);
    
    if (format === 'csv') {
      this.downloadCSV(chartType);
      return;
    }

    if (!this.isBrowser || !this.chartsInitialized) {
      console.error('Charts not initialized');
      return;
    }

    let chart: echarts.ECharts | null = null;
    let filename = '';

    switch(chartType) {
      case 'temperature':
        chart = this.temperatureChart;
        filename = `temperature-chart.${format}`;
        break;
      case 'humidity':
        chart = this.humidityChart;
        filename = `humidity-chart.${format}`;
        break;
      case 'combined':
        chart = this.combinedChart;
        filename = `combined-chart.${format}`;
        break;
    }

    if (!chart) {
      console.error('Chart not found');
      return;
    }

    const imageUrl = chart.getDataURL({
      type: format,
      pixelRatio: 2,
      backgroundColor: '#fff'
    });

    const link = document.createElement('a');
    link.href = imageUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  downloadCSV(chartType: 'temperature' | 'humidity' | 'combined'): void {
    const data = this.filteredSensors();
    if (data.length === 0) {
      console.error('No data to download');
      return;
    }

    const sortedData = [...data].sort((a, b) => 
      new Date(a.sensorDateTime).getTime() - new Date(b.sensorDateTime).getTime()
    );

    let csvContent = '';
    let filename = '';

    switch(chartType) {
      case 'temperature':
        csvContent = 'Sensor ID,Date Time,Temperature (°C),Humidity (%)\n';
        sortedData.forEach(sensor => {
          const dateTime = new Date(sensor.sensorDateTime).toLocaleString();
          csvContent += `${sensor.sensorId},"${dateTime}",${sensor.temperature},${sensor.humidity}\n`;
        });
        filename = 'temperature-data.csv';
        break;

      case 'humidity':
        csvContent = 'Sensor ID,Date Time,Temperature (°C),Humidity (%)\n';
        sortedData.forEach(sensor => {
          const dateTime = new Date(sensor.sensorDateTime).toLocaleString();
          csvContent += `${sensor.sensorId},"${dateTime}",${sensor.temperature},${sensor.humidity}\n`;
        });
        filename = 'humidity-data.csv';
        break;

      case 'combined':
        csvContent = 'Sensor ID,Date Time,Temperature (°C),Humidity (%)\n';
        sortedData.forEach(sensor => {
          const dateTime = new Date(sensor.sensorDateTime).toLocaleString();
          csvContent += `${sensor.sensorId},"${dateTime}",${sensor.temperature},${sensor.humidity}\n`;
        });
        filename = 'combined-data.csv';
        break;
    }

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }

  downloadAllCharts(format: 'png' | 'jpeg' | 'csv'): void {
    if (format === 'csv') {
      this.downloadChart('temperature', 'csv');
      setTimeout(() => this.downloadChart('humidity', 'csv'), 300);
      setTimeout(() => this.downloadChart('combined', 'csv'), 600);
      return;
    }

    if (!this.isBrowser || !this.chartsInitialized) {
      console.error('Charts not initialized');
      return;
    }

    this.downloadChart('temperature', format);
    setTimeout(() => this.downloadChart('humidity', format), 300);
    setTimeout(() => this.downloadChart('combined', format), 600);
  }

  downloadAllFormats(chartType: 'temperature' | 'humidity' | 'combined'): void {
    this.activeDropdown.set(null);
    
    this.downloadChart(chartType, 'png');
    setTimeout(() => this.downloadChart(chartType, 'jpeg'), 300);
    setTimeout(() => this.downloadChart(chartType, 'csv'), 600);
  }
}