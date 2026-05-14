import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ForumPost } from '../../models/forumpost';

interface OutcomeData {
  status: string;
  summary: string;
  totalGb: number;
  missedPercent: number;
  droppedByKernel: number;
}

@Component({
  selector: 'app-home-page',
  imports: [CommonModule],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css'
})
export class HomePage implements OnInit {
  posts = signal<ForumPost[]>([]);
  loading = signal(false);

  currentPosts = computed(() => this.posts().filter(p => p.status === 'CURRENT'));
  futurePosts  = computed(() => this.posts().filter(p => p.status === 'FUTURE'));
  pastPosts    = computed(() => this.posts().filter(p => p.status === 'PAST'));

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
    if (!seconds) return '—';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0 && m > 0) return `${h}h ${m}m`;
    if (h > 0) return `${h}h`;
    return `${m}m`;
  }

  progressPercent(post: ForumPost): number {
    if (!post.scheduledDateTime || !post.durationSeconds) return 0;
    const start = new Date(post.scheduledDateTime).getTime();
    const end = start + post.durationSeconds * 1000;
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

  formatEndDate(post: ForumPost): string {
    if (!post.scheduledDateTime || !post.durationSeconds) return '—';
    const end = new Date(new Date(post.scheduledDateTime).getTime() + post.durationSeconds * 1000);
    return end.toLocaleString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
