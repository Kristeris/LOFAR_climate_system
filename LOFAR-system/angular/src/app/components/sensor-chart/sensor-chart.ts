import { Component, OnInit, OnDestroy, signal, effect, AfterViewInit, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import * as echarts from 'echarts';
import { ClimateSensorData } from '../../models/climate-sensor-data';
import { ClimateSensorDataService } from '../../services/climate-sensor-data';

@Component({
  selector: 'app-sensor-chart',
  imports: [CommonModule],
  templateUrl: './sensor-chart.html',
  styleUrl: './sensor-chart.css',
})
export class SensorChart implements OnInit, OnDestroy {
  sensors = signal<ClimateSensorData[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);
  
  private temperatureChart: echarts.ECharts | null = null;
  private humidityChart: echarts.ECharts | null = null;
  private combinedChart: echarts.ECharts | null = null;
  private platformId = inject(PLATFORM_ID);
  private isBrowser: boolean;
  private chartsInitialized = false;

  constructor(private sensorService: ClimateSensorDataService) {
    this.isBrowser = isPlatformBrowser(this.platformId);
    
    // Effect to update charts when sensors data changes
    effect(() => {
      const data = this.sensors();
      const isLoading = this.loading();
      
      if (data.length > 0 && !isLoading && this.isBrowser) {
        // Wait for DOM to be ready and then update charts
        setTimeout(() => {
          this.ensureChartsInitialized();
          this.updateCharts(data);
        }, 150);
      }
    });
  }

  ngOnInit(): void {
    // Load all sensors automatically when component initializes
    this.loadSensors();
  }

  ngOnDestroy(): void {
    // Dispose charts to prevent memory leaks
    this.disposeCharts();
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

    // Initialize Temperature Chart
    const tempChartDom = document.getElementById('temperatureChart');
    if (tempChartDom && !this.temperatureChart) {
      this.temperatureChart = echarts.init(tempChartDom);
    }

    // Initialize Humidity Chart
    const humidChartDom = document.getElementById('humidityChart');
    if (humidChartDom && !this.humidityChart) {
      this.humidityChart = echarts.init(humidChartDom);
    }

    // Initialize Combined Chart
    const combinedChartDom = document.getElementById('combinedChart');
    if (combinedChartDom && !this.combinedChart) {
      this.combinedChart = echarts.init(combinedChartDom);
    }

    // Mark as initialized if all charts are created
    if (this.temperatureChart && this.humidityChart && this.combinedChart) {
      this.chartsInitialized = true;
      console.log('Charts initialized successfully');
    }
  }

  private updateCharts(data: ClimateSensorData[]): void {
    if (!this.isBrowser || !this.chartsInitialized) {
      console.log('Charts not ready for update');
      return;
    }

    console.log('Updating charts with data:', data);

    // Sort data by date
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

    // Temperature Chart
    if (this.temperatureChart) {
      this.temperatureChart.setOption({
        title: {
          text: 'Temperature Over Time',
          left: 'center',
          textStyle: {
            color: '#333',
            fontSize: 18
          }
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}<br/>Temperature: {c}°C'
        },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: {
            rotate: 45,
            interval: 'auto'
          }
        },
        yAxis: {
          type: 'value',
          name: 'Temperature (°C)',
          axisLabel: {
            formatter: '{value}°C'
          }
        },
        series: [{
          name: 'Temperature',
          type: 'line',
          data: temperatures,
          smooth: true,
          lineStyle: {
            color: '#ff6b6b',
            width: 3
          },
          itemStyle: {
            color: '#ff6b6b'
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(255, 107, 107, 0.5)' },
              { offset: 1, color: 'rgba(255, 107, 107, 0.1)' }
            ])
          }
        }],
        grid: {
          left: '10%',
          right: '10%',
          bottom: '20%',
          containLabel: true
        }
      });
      this.temperatureChart.resize();
    }

    // Humidity Chart
    if (this.humidityChart) {
      this.humidityChart.setOption({
        title: {
          text: 'Humidity Over Time',
          left: 'center',
          textStyle: {
            color: '#333',
            fontSize: 18
          }
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}<br/>Humidity: {c}%'
        },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: {
            rotate: 45,
            interval: 'auto'
          }
        },
        yAxis: {
          type: 'value',
          name: 'Humidity (%)',
          axisLabel: {
            formatter: '{value}%'
          }
        },
        series: [{
          name: 'Humidity',
          type: 'line',
          data: humidity,
          smooth: true,
          lineStyle: {
            color: '#4dabf7',
            width: 3
          },
          itemStyle: {
            color: '#4dabf7'
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(77, 171, 247, 0.5)' },
              { offset: 1, color: 'rgba(77, 171, 247, 0.1)' }
            ])
          }
        }],
        grid: {
          left: '10%',
          right: '10%',
          bottom: '20%',
          containLabel: true
        }
      });
      this.humidityChart.resize();
    }

    // Combined Chart
    if (this.combinedChart) {
      this.combinedChart.setOption({
        title: {
          text: 'Temperature & Humidity',
          left: 'center',
          textStyle: {
            color: '#333',
            fontSize: 18
          }
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'cross'
          }
        },
        legend: {
          data: ['Temperature', 'Humidity'],
          top: 30
        },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: {
            rotate: 45,
            interval: 'auto'
          }
        },
        yAxis: [
          {
            type: 'value',
            name: 'Temperature (°C)',
            position: 'left',
            axisLabel: {
              formatter: '{value}°C'
            }
          },
          {
            type: 'value',
            name: 'Humidity (%)',
            position: 'right',
            axisLabel: {
              formatter: '{value}%'
            }
          }
        ],
        series: [
          {
            name: 'Temperature',
            type: 'line',
            yAxisIndex: 0,
            data: temperatures,
            smooth: true,
            lineStyle: {
              color: '#ff6b6b',
              width: 2
            },
            itemStyle: {
              color: '#ff6b6b'
            }
          },
          {
            name: 'Humidity',
            type: 'line',
            yAxisIndex: 1,
            data: humidity,
            smooth: true,
            lineStyle: {
              color: '#4dabf7',
              width: 2
            },
            itemStyle: {
              color: '#4dabf7'
            }
          }
        ],
        grid: {
          left: '10%',
          right: '10%',
          bottom: '20%',
          containLabel: true
        }
      });
      this.combinedChart.resize();
    }
  }

  loadSensors(): void {
    this.loading.set(true);
    this.error.set(null);
    
    // Dispose existing charts before loading new data
    if (this.chartsInitialized) {
      this.disposeCharts();
    }
    
    this.sensorService.getAll().subscribe({
      next: (data) => {
        console.log('Loaded sensor data:', data);
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
    
    // Dispose existing charts before loading new data
    if (this.chartsInitialized) {
      this.disposeCharts();
    }
    
    this.sensorService.getLatest10().subscribe({
      next: (data) => {
        console.log('Loaded latest 10 sensor data:', data);
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

  downloadChart(chartType: 'temperature' | 'humidity' | 'combined', format: 'png' | 'jpeg'): void {
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

    // Get the chart as base64 image
    const imageUrl = chart.getDataURL({
      type: format,
      pixelRatio: 2,
      backgroundColor: '#fff'
    });

    // Create a temporary link and trigger download
    const link = document.createElement('a');
    link.href = imageUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  downloadAllCharts(format: 'png' | 'jpeg'): void {
    if (!this.isBrowser || !this.chartsInitialized) {
      console.error('Charts not initialized');
      return;
    }

    // Download all charts with a small delay between each
    this.downloadChart('temperature', format);
    setTimeout(() => this.downloadChart('humidity', format), 300);
    setTimeout(() => this.downloadChart('combined', format), 600);
  }
}