import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ForumPost } from '../../models/forumpost';
import { SensorList } from '../sensor-list/sensor-list';
import { SensorChart } from '../sensor-chart/sensor-chart';

interface OutcomeData {
  status: string;
  summary: string;
  totalGb: number;
  missedPercent: number;
  droppedByKernel: number;
}

@Component({
  selector: 'app-home-page',
  imports: [CommonModule, SensorList, SensorChart],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css'
})
export class HomePage implements OnInit {
  posts = signal<ForumPost[]>([]);
  loading = signal(false);

  currentPosts = computed(() => this.posts().filter(p => p.status === 'CURRENT'));
  futurePosts  = computed(() => this.posts().filter(p => p.status === 'FUTURE'));
  pastPosts    = computed(() => this.posts().filter(p => p.status === 'PAST'));

  pastPageSize = signal<number>(10);
  pastCurrentPage = signal<number>(1);
  pastStatusFilter = signal<string>('all');
  pastTimeFilter = signal<string>('all');
  pastSortOrder = signal<string>('desc');

  filteredPastPosts = computed(() => {
    let result = this.pastPosts();

    const status = this.pastStatusFilter();
    if (status === 'success') {
      result = result.filter(p => p.outcomeStatus === 'SUCCESS');
    } else if (status === 'failure') {
      result = result.filter(p => p.outcomeStatus !== 'SUCCESS');
    }

    const period = this.pastTimeFilter();
    if (period !== 'all') {
      const now = Date.now();
      const cutoffs: Record<string, number> = {
        '7days': 7 * 86400000,
        '30days': 30 * 86400000,
        '90days': 90 * 86400000,
        'year': 365 * 86400000,
      };
      const cutoff = cutoffs[period];
      result = result.filter(p => {
        const date = p.scheduledDateTime
          ? new Date(p.scheduledDateTime).getTime()
          : new Date(p.createdAt).getTime();
        return (now - date) <= cutoff;
      });
    }

    result = [...result].sort((a, b) => {
      const dateA = a.scheduledDateTime
        ? new Date(a.scheduledDateTime).getTime()
        : new Date(a.createdAt).getTime();
      const dateB = b.scheduledDateTime
        ? new Date(b.scheduledDateTime).getTime()
        : new Date(b.createdAt).getTime();
      return this.pastSortOrder() === 'desc' ? dateB - dateA : dateA - dateB;
    });

    return result;
  });

  totalPastPages = computed(() => Math.max(1, Math.ceil(this.filteredPastPosts().length / this.pastPageSize())));
  paginatedPastPosts = computed(() => {
    const start = (this.pastCurrentPage() - 1) * this.pastPageSize();
    return this.filteredPastPosts().slice(start, start + this.pastPageSize());
  });

  showSensorTable = signal<boolean>(false);
  showSensorChart = signal<boolean>(false);

  toggleSensorTable(): void {
    this.showSensorTable.update(v => !v);
  }

  toggleSensorChart(): void {
    this.showSensorChart.update(v => !v);
  }

  /** Modal state */
  selectedPost = signal<ForumPost | null>(null);
  selectedOutcome = signal<OutcomeData | null>(null);
  modalLoading = signal(false);

  private readonly apiBase = 'http://localhost:8080/api/forum';

  constructor(
    private http: HttpClient,
    public auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadPosts();
  }

  loadPosts(): void {
    this.loading.set(true);
    this.http.get<ForumPost[]>(this.apiBase).subscribe({
      next: (data) => {
        this.posts.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  openModal(post: ForumPost): void {
    this.selectedPost.set(post);
    this.selectedOutcome.set(null);
    if (post.status === 'PAST') {
      this.modalLoading.set(true);
      this.http.get<OutcomeData>(`${this.apiBase}/${post.id}/outcome`).subscribe({
        next: (data) => {
          this.selectedOutcome.set(data);
          this.modalLoading.set(false);
        },
        error: () => this.modalLoading.set(false)
      });
    }
  }

  closeModal(): void {
    this.selectedPost.set(null);
    this.selectedOutcome.set(null);
  }

  goToDetail(postId: number): void {
    this.closeModal();
    this.router.navigate(['/home/post', postId]);
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

  progressPercent(post: ForumPost): number {
    const startDateStr = post.scheduledDateTime || post.createdAt;
    if (!startDateStr) return 0;
    const durationSec = post.durationSeconds || 3600;
    const start = new Date(startDateStr).getTime();
    const end = start + durationSec * 1000;
    const now = Date.now();
    if (now <= start) return 0;
    if (now >= end) return 100;
    return Math.round(((now - start) / (end - start)) * 100);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  onPastPageSizeChange(event: Event): void {
    const value = parseInt((event.target as HTMLSelectElement).value, 10);
    this.pastPageSize.set(value);
    this.pastCurrentPage.set(1);
  }

  goToPastPage(page: number): void {
    this.pastCurrentPage.set(page);
  }

  prevPastPage(): void {
    if (this.pastCurrentPage() > 1) this.pastCurrentPage.update(p => p - 1);
  }

  nextPastPage(): void {
    if (this.pastCurrentPage() < this.totalPastPages()) this.pastCurrentPage.update(p => p + 1);
  }

  onPastStatusFilterChange(event: Event): void {
    this.pastStatusFilter.set((event.target as HTMLSelectElement).value);
    this.pastCurrentPage.set(1);
  }

  onPastTimeFilterChange(event: Event): void {
    this.pastTimeFilter.set((event.target as HTMLSelectElement).value);
    this.pastCurrentPage.set(1);
  }

  onPastSortOrderChange(event: Event): void {
    this.pastSortOrder.set((event.target as HTMLSelectElement).value);
    this.pastCurrentPage.set(1);
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
}
