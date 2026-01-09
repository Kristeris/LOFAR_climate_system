import { Component, OnInit, OnDestroy, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  loading = signal<boolean>(true);
  error = signal<string | null>(null);
  
  private temperatureChart: echarts.ECharts | null = null;
  private humidityChart: echarts.ECharts | null = null;
  private combinedChart: echarts.ECharts | null = null;

  constructor(private sensorService: ClimateSensorDataService) {
    // Effect to update charts when sensors data changes
    effect(() => {
      const data = this.sensors();
      if (data.length > 0) {
        this.updateCharts(data);
      }
    });
  }

  ngOnInit(): void {
    this.loadSensors();
    this.initializeCharts();
  }

  ngOnDestroy(): void {
    // Dispose charts to prevent memory leaks
    if (this.temperatureChart) {
      this.temperatureChart.dispose();
    }
    if (this.humidityChart) {
      this.humidityChart.dispose();
    }
    if (this.combinedChart) {
      this.combinedChart.dispose();
    }
  }

  private initializeCharts(): void {
    // Initialize Temperature Chart
    const tempChartDom = document.getElementById('temperatureChart');
    if (tempChartDom) {
      this.temperatureChart = echarts.init(tempChartDom);
    }

    // Initialize Humidity Chart
    const humidChartDom = document.getElementById('humidityChart');
    if (humidChartDom) {
      this.humidityChart = echarts.init(humidChartDom);
    }

    // Initialize Combined Chart
    const combinedChartDom = document.getElementById('combinedChart');
    if (combinedChartDom) {
      this.combinedChart = echarts.init(combinedChartDom);
    }
  }

  private updateCharts(data: ClimateSensorData[]): void {
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
            interval: 0
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
          bottom: '15%',
          containLabel: true
        }
      });
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
            interval: 0
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
          bottom: '15%',
          containLabel: true
        }
      });
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
            interval: 0
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
          bottom: '15%',
          containLabel: true
        }
      });
    }
  }

  loadSensors(): void {
    this.loading.set(true);
    this.error.set(null);
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
}