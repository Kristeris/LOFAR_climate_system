import { Component, OnInit, OnDestroy, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import * as echarts from 'echarts';
import { ForumPost } from '../../models/forumpost';
import { WebSocketService } from '../../services/websocket.service';
import { Subscription } from 'rxjs';

interface EventOutcomeData {
  id: number;
  postId: number;
  status: string;
  summary: string;
  logSnapshot: string;
  totalGb: number;
  missedPercent: number;
  droppedByKernel: number;
  recordedAt: string;
}

@Component({
  selector: 'app-post-detail',
  imports: [CommonModule, RouterLink],
  templateUrl: './post-detail.component.html',
  styleUrl: './post-detail.component.css'
})
export class PostDetailComponent implements OnInit, OnDestroy {
  post = signal<ForumPost | null>(null);
  outcome = signal<EventOutcomeData | null>(null);
  loading = signal(true);
  last5Logs = signal<string[]>([]);

  /** Live terminal lines (CURRENT posts) */
  liveTerminalLines = signal<string[]>([]);
  liveTotalGb = signal(0);
  liveMissedPercent = signal(0);
  liveDroppedByKernel = signal(0);
  progressPercent = signal(0);

  private postId: number | null = null;
  private volumeChart: echarts.ECharts | null = null;
  private missChart: echarts.ECharts | null = null;
  private nohupSub: Subscription | null = null;
  private progressTimer: ReturnType<typeof setInterval> | null = null;
  private statusTimer: ReturnType<typeof setInterval> | null = null;

  private readonly apiBase = 'http://localhost:8080/api/forum';

  constructor(
    private http: HttpClient,
    private route: ActivatedRoute,
    private wsService: WebSocketService
  ) {
    effect(() => {
      if (this.outcome() && this.post()?.status !== 'CURRENT') {
        setTimeout(() => this.initCharts(), 200);
      }
    });
  }

  ngOnInit(): void {
    this.postId = Number(this.route.snapshot.paramMap.get('id'));
    if (this.postId) {
      this.loadData(this.postId);
    }
  }

  ngOnDestroy(): void {
    this.stopLiveUpdates();
    if (this.statusTimer) clearInterval(this.statusTimer);
  }

  private stopLiveUpdates(): void {
    this.nohupSub?.unsubscribe();
    this.nohupSub = null;
    if (this.progressTimer) {
      clearInterval(this.progressTimer);
      this.progressTimer = null;
    }
    this.volumeChart?.dispose();
    this.volumeChart = null;
    this.missChart?.dispose();
    this.missChart = null;
  }

  private startLiveUpdates(): void {
    this.updateProgress();
    this.progressTimer = setInterval(() => this.updateProgress(), 1000);
    this.nohupSub = this.wsService.getNohupUpdates().subscribe(block => {
      if (block) {
        this.liveTerminalLines.update(lines => [...lines, block].slice(-100));
        this.parseLiveMetrics(block);
      }
    });
    setTimeout(() => {
      this.initCharts();
      this.wsService.requestFullNohupLog();
    }, 300);
  }

  private updateProgress(): void {
    const p = this.post();
    if (!p) return;
    const startDateStr = p.scheduledDateTime || p.createdAt;
    if (!startDateStr) return;
    const durationSec = p.durationSeconds || 3600;
    const start = new Date(startDateStr).getTime();
    const end = start + durationSec * 1000;
    const now = Date.now();
    if (now <= start) { this.progressPercent.set(0); return; }
    if (now >= end) { this.progressPercent.set(100); return; }
    this.progressPercent.set(Math.round(((now - start) / (end - start)) * 100));
  }

  private parseLiveMetrics(block: string): void {
    const gbMatch = block.match(/volume\s+([0-9.]+)\s+GB/i);
    if (gbMatch) this.liveTotalGb.set(parseFloat(gbMatch[1]));

    const missMatch = block.match(/missed packets\s+\d+\s+([0-9.]+)\s+%/i);
    if (missMatch) this.liveMissedPercent.set(parseFloat(missMatch[1]));

    const kernelMatch = block.match(/dropped by kernel\s+(\d+)/i);
    if (kernelMatch) this.liveDroppedByKernel.set(parseInt(kernelMatch[1], 10));

    this.liveUpdateCharts();
  }

  private liveUpdateCharts(): void {
    if (this.volumeChart) {
      this.volumeChart.setOption({
        series: [{
          data: [
            { value: this.liveTotalGb(), itemStyle: { color: '#51cf66' } },
            { value: this.liveDroppedByKernel(), itemStyle: { color: '#ff6b6b' } }
          ]
        }]
      });
    }
    if (this.missChart) {
      const missed = this.liveMissedPercent();
      const success = Math.max(0, 100 - missed);
      this.missChart.setOption({
        series: [{
          data: [
            { value: success, name: 'Success', itemStyle: { color: '#51cf66' } },
            { value: missed, name: 'Missed', itemStyle: { color: '#ff6b6b' } }
          ]
        }]
      });
    }
  }

  private loadData(id: number): void {
    this.http.get<ForumPost>(`${this.apiBase}/${id}`).subscribe({
      next: (post) => {
        this.post.set(post);
        this.loading.set(false);
        if (post.status === 'CURRENT') {
          this.startLiveUpdates();
        } else {
          this.loadOutcome(id);
        }
        this.startStatusPoller(id);
      },
      error: () => this.loading.set(false)
    });
  }

  private loadOutcome(id: number): void {
    this.http.get<EventOutcomeData>(`${this.apiBase}/${id}/outcome`).subscribe({
      next: (outcome) => {
        this.outcome.set(outcome);
        this.parseLast5Logs(outcome);
      },
      error: () => {}
    });
  }

  private startStatusPoller(id: number): void {
    this.statusTimer = setInterval(() => {
      this.http.get<ForumPost>(`${this.apiBase}/${id}`).subscribe(p => {
        const currentStatus = this.post()?.status;
        if (p.status === 'CURRENT' && currentStatus !== 'CURRENT') {
          this.post.set(p);
          this.startLiveUpdates();
        } else if (p.status === 'PAST' && currentStatus !== 'PAST') {
          this.stopLiveUpdates();
          this.post.set(p);
          this.loadOutcome(id);
        }
      });
    }, 10000);
  }

  private parseLast5Logs(outcome: EventOutcomeData): void {
    if (!outcome.logSnapshot) return;
    const lines = outcome.logSnapshot.split('\n').filter(l => l.trim());
    this.last5Logs.set(lines.slice(-5));
  }

  private initCharts(): void {
    if (!this.volumeChart) {
      const volEl = document.getElementById('volumeChart');
      if (volEl) this.volumeChart = echarts.init(volEl);
    }
    if (!this.missChart) {
      const missEl = document.getElementById('missChart');
      if (missEl) this.missChart = echarts.init(missEl);
    }
    this.renderVolumeChart();
    this.renderMissChart();
  }

  private renderVolumeChart(): void {
    if (!this.volumeChart) return;
    const outcome = this.outcome();
    const gb = outcome?.totalGb ?? 0;
    const dropped = outcome?.droppedByKernel ?? 0;
    this.volumeChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 30, bottom: 30 },
      xAxis: {
        type: 'category',
        data: ['Total GB', 'Dropped'],
        axisLabel: { color: '#6b8499' }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#6b8499' },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } }
      },
      series: [{
        type: 'bar',
        data: [
          { value: gb, itemStyle: { color: '#51cf66' } },
          { value: dropped, itemStyle: { color: '#ff6b6b' } }
        ],
        barWidth: 40
      }]
    });
  }

  private renderMissChart(): void {
    if (!this.missChart) return;
    const outcome = this.outcome();
    const missed = outcome?.missedPercent ?? 0;
    const success = Math.max(0, 100 - missed);
    this.missChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '50%'],
        data: [
          { value: success, name: 'Success', itemStyle: { color: '#51cf66' } },
          { value: missed, name: 'Missed', itemStyle: { color: '#ff6b6b' } }
        ],
        label: { color: '#c8dde8', fontSize: 12 },
        labelLine: { lineStyle: { color: '#6b8499' } }
      }]
    });
  }

  timeUntilStart(post: ForumPost): string {
    if (!post.scheduledDateTime) return '—';
    const diff = new Date(post.scheduledDateTime).getTime() - Date.now();
    if (diff <= 0) return 'Started already';
    const days = Math.floor(diff / 86400000);
    const hours = Math.floor((diff % 86400000) / 3600000);
    const minutes = Math.floor((diff % 3600000) / 60000);
    if (days > 0) return `Starts in ${days}d ${hours}h`;
    if (hours > 0) return `Starts in ${hours}h ${minutes}m`;
    return `Starts in ${minutes}m`;
  }

  formatDuration(seconds: number | undefined): string {
    const sec = seconds || 3600;
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    if (h > 0 && m > 0) return `${h}h ${m}m`;
    if (h > 0) return `${h}h`;
    return `${m}m`;
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatEndDate(post: ForumPost): string {
    const startDateStr = post.scheduledDateTime || post.createdAt;
    if (!startDateStr) return '—';
    const durationSec = post.durationSeconds || 3600;
    const end = new Date(new Date(startDateStr).getTime() + durationSec * 1000);
    return end.toLocaleString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  downloadLogs(): void {
    const postId = this.postId;
    if (!postId) return;
    const a = document.createElement('a');
    a.href = `${this.apiBase}/${postId}/outcome/download-log`;
    a.download = `observation_log_${postId}.txt`;
    a.click();
  }

  downloadGraphs(): void {
    const canvas1 = document.querySelector('#volumeChart canvas') as HTMLCanvasElement;
    const canvas2 = document.querySelector('#missChart canvas') as HTMLCanvasElement;
    const combined = document.createElement('canvas');
    combined.width = 800;
    combined.height = 500;
    const ctx = combined.getContext('2d');
    if (!ctx) return;
    ctx.fillStyle = '#0d1520';
    ctx.fillRect(0, 0, 800, 500);
    if (canvas1) ctx.drawImage(canvas1, 20, 20, 370, 220);
    if (canvas2) ctx.drawImage(canvas2, 410, 20, 370, 220);
    const a = document.createElement('a');
    a.href = combined.toDataURL('image/png');
    a.download = `graphs_post_${this.postId}.png`;
    a.click();
  }
}
