import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit, AfterViewChecked, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as echarts from 'echarts';
import { WebSocketService } from '../../services/websocket.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-nohup-monitor',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './nohup-monitor.component.html',
  styleUrls: ['./nohup-monitor.component.css']
})
export class NohupMonitorComponent implements OnInit, OnDestroy, AfterViewInit, AfterViewChecked {
  @ViewChild('terminalBox') private terminalBox!: ElementRef;
  
  rawLogLines: string[] = [];
  
  // KPIs
  totalGB: number = 0;
  missedPercent: number = 0;
  bufferPercent: number = 0;
  isGoodStatus: boolean = true;
  packetsDropped: number = 0;
  
  // Charts
  chartInstance: any;
  missedHistory: number[] = [];
  timeHistory: string[] = [];
  
  private wsSubscription?: Subscription;
  public autoScroll = true;

  constructor(private wsService: WebSocketService, private cdr: ChangeDetectorRef) {}

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.initChart();
    }, 500);
  }

  ngOnInit(): void {

    // Request full log on page load
    this.wsService.requestFullNohupLog();

    this.wsSubscription = this.wsService.getNohupUpdates().subscribe(logBlock => {
      console.log('📥 Nohup log received, length:', logBlock?.length);
      if (!logBlock) return;
      
      const lines = logBlock.split('\n');
      this.rawLogLines.push(...lines);
      
      if (this.rawLogLines.length > 500) {
        this.rawLogLines = this.rawLogLines.slice(this.rawLogLines.length - 500);
      }
      
      this.parseLogForKPIs(logBlock);
      this.cdr.detectChanges();
    });
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  scrollToBottom(): void {
    if (this.autoScroll && this.terminalBox) {
      try {
        this.terminalBox.nativeElement.scrollTop = this.terminalBox.nativeElement.scrollHeight;
      } catch(err) { }
    }
  }

  onScroll(event: any) {
    const element = event.target;
    if (element.scrollHeight - element.scrollTop > element.clientHeight + 50) {
      this.autoScroll = false;
    } else {
      this.autoScroll = true;
    }
  }

  parseLogForKPIs(log: string) {
    let updatedChart = false;
    
    // e.g. "total  59.540 GB  max buff 336432/1000001536 (0.0 % full)"
    const gbMatch = /total\s+([0-9.]+)\s+GB/i.exec(log);
    if (gbMatch) {
      this.totalGB = parseFloat(gbMatch[1]);
    }

    const buffMatch = /\(\s*([0-9.]+)\s*%\s*full\s*\)/i.exec(log);
    if (buffMatch) {
      this.bufferPercent = parseFloat(buffMatch[1]);
    }
    
    const blocksMatch = log.match(/exp\s+([0-9.]+)\s+%\s+missed\s+([0-9.]+)\s+%\s+dropped/ig);
    if (blocksMatch && blocksMatch.length > 0) {
        const lastLine = blocksMatch[blocksMatch.length-1];
        const m = /exp\s+([0-9.]+)\s+%\s+missed\s+([0-9.]+)\s+%\s+dropped/i.exec(lastLine);
        if (m) {
            this.missedPercent = parseFloat(m[1]);
            const dropped = parseFloat(m[2]);
            this.packetsDropped = dropped;
            this.isGoodStatus = (dropped === 0);
            
            const now = new Date();
            this.timeHistory.push(now.getHours() + ':' + ('0'+now.getMinutes()).slice(-2) + ':' + ('0'+now.getSeconds()).slice(-2));
            this.missedHistory.push(this.missedPercent);
            
            if (this.timeHistory.length > 20) {
              this.timeHistory.shift();
              this.missedHistory.shift();
            }
            updatedChart = true;
        }
    }
    
    const kernelDropMatch = /dropped by kernel\s+([0-9]+)/i.exec(log);
    if (kernelDropMatch && parseInt(kernelDropMatch[1], 10) > 0) {
      this.isGoodStatus = false;
      this.packetsDropped = parseInt(kernelDropMatch[1], 10);
    }
    
    if (updatedChart) {
        this.updateChart();
    }
  }

  initChart() {
    const chartDom = document.getElementById('missed-chart');
    if (chartDom) {
      this.chartInstance = echarts.init(chartDom);
      this.updateChart();
      
      window.addEventListener('resize', () => {
        this.chartInstance?.resize();
      });
    }
  }

  updateChart() {
    if (!this.chartInstance) return;
    
    const option = {
      title: { text: '', textStyle: { color: '#ccc', fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      grid: { left: '40px', right: '10px', bottom: '25px', top: '25px' },
      xAxis: {
        type: 'category',
        data: this.timeHistory.length > 0 ? this.timeHistory : ['--'],
        axisLine: { lineStyle: { color: '#444' } },
        axisLabel: { color: '#888' }
      },
      yAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: '#444' } },
        splitLine: { lineStyle: { color: '#222' } },
        axisLabel: { color: '#888' },
        max: (value: any) => { return Math.max(30, Number((value.max * 1.2).toFixed(0))); }
      },
      series: [
        {
          data: this.missedHistory.length > 0 ? this.missedHistory : [0],
          type: 'line',
          smooth: true,
          lineStyle: { color: '#ff9ff3', width: 3 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(255, 159, 243, 0.4)' },
              { offset: 1, color: 'rgba(255, 159, 243, 0.0)' }
            ])
          },
          animation: false
        }
      ]
    };

    this.chartInstance.setOption(option);
  }

  ngOnDestroy(): void {
    if (this.wsSubscription) {
      this.wsSubscription.unsubscribe();
    }
    if (this.chartInstance) {
      this.chartInstance.dispose();
    }
  }
}
