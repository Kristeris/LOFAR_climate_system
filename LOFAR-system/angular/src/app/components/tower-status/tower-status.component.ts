import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as echarts from 'echarts';
import { Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-tower-status',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tower-status.component.html',
  styleUrls: ['./tower-status.component.css']
})
export class TowerStatusComponent implements OnInit, OnDestroy {
  chartInstance: any;
  updateSubscription?: Subscription;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.initChart();
    this.fetchData();
  }

  ngOnDestroy(): void {
    if (this.chartInstance) {
      this.chartInstance.dispose();
    }
    if (this.updateSubscription) {
      this.updateSubscription.unsubscribe();
    }
  }

  initChart() {
    const chartDom = document.getElementById('tower-chart');
    if (!chartDom) return;
    this.chartInstance = echarts.init(chartDom);
    
    window.addEventListener('resize', () => {
      this.chartInstance.resize();
    });
  }

  fetchData() {
    this.updateSubscription = this.http.get('assets/data.sql', { responseType: 'text' }).pipe(
      map(text => {
        const results = [];
        const regex = /\(\s*(\d+)\s*,\s*([01])\s*\)/g;
        let match;
        while ((match = regex.exec(text)) !== null) {
            results.push({
                slotNumber: parseInt(match[1], 10),
                value: parseInt(match[2], 10)
            });
        }
        return results;
      })
    ).subscribe({
      next: (data: any[]) => {
        this.renderChart(data);
      },
      error: (err: any) => console.error('Failed to fetch tower statuses', err)
    });
  }

  renderChart(apiData: any[]) {
    const points: any[] = [];
    const size = 10;
    const cols = 20;
    const rows = 20;
    const centerX = cols / 2;
    const centerY = rows / 2;

    for (let c = 0; c < cols; c++) {
      for (let r = 0; r < rows; r++) {
        const x = c * 1.5 * size;
        const y = (r + (c % 2 === 0 ? 0 : 0.5)) * Math.sqrt(3) * size;
        
        const cx = centerX * 1.5 * size;
        const cy = centerY * Math.sqrt(3) * size;
        
        const dist = Math.sqrt(Math.pow(x - cx, 2) + Math.pow(y - cy, 2));
        points.push({ c, r, x, y, dist });
      }
    }

    points.sort((a, b) => a.dist - b.dist);
    const selectedPoints = points.slice(1, 97);
    
    selectedPoints.sort((a, b) => {
      if (Math.abs(a.y - b.y) > size) return a.y - b.y;
      return a.x - b.x;
    });

    const seriesData = [];
    
    for (let i = 0; i < selectedPoints.length; i++) {
        const targetSlot = i + 1; 
        const statusRecord = apiData.find(d => d.slotNumber === targetSlot);
        const value = statusRecord ? statusRecord.value : 0; 
        
        const pt = selectedPoints[i];
        
        seriesData.push({
            name: `Tower ${i}`,
            value: [pt.x, -pt.y, value], 
            itemStyle: {
                color: value === 1 ? '#73A56D' : '#FF0000', // Matches screenshot greens and red
                borderColor: '#1E1E1E',
                borderWidth: 1
            },
            label: {
                show: true,
                formatter: `${i}`,
                color: '#1E1E1E',
                position: 'inside',
                fontSize: 12,
                fontWeight: 'bold'
            }
        });
    }

    const centerPoint = points[0];
    seriesData.push({
        name: 'Center',
        value: [centerPoint.x, -centerPoint.y, -1],
        symbol: 'path://M -10 -10 L 10 10 M -10 10 L 10 -10', 
        symbolSize: 20,
        itemStyle: { color: 'red', borderWidth: 3, borderColor: 'red' },
        label: { show: false }
    });

    const option = {
        title: {
            text: 'LOFAR Tower Status Array',
            left: 'center',
            top: 20,
            textStyle: { color: '#ffffff', fontSize: 24, fontWeight: 'normal' }
        },
        backgroundColor: '#FFFFFF',
        tooltip: {
            formatter: function (params: any) {
                if (params.data.name === 'Center') return 'Center Core';
                const status = params.data.value[2] === 1 ? 'Working' : 'Offline';
                return `<strong>${params.data.name}</strong><br/>Status: <span style="color:${params.data.value[2] === 1 ? '#5cb85c' : '#d9534f'}">${status}</span>`;
            }
        },
        xAxis: { show: false, scale: true },
        yAxis: { show: false, scale: true },
        series: [{
            type: 'scatter',
            symbolSize: 45,
            data: seriesData
        }]
    };

    this.chartInstance.setOption(option);
  }
}
