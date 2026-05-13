import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { ForumPost } from '../../models/forumpost';

@Component({
  selector: 'app-home-page',
  imports: [CommonModule],
  templateUrl: './home-page.html',
  styleUrl: './home-page.css'
})
export class HomePage implements OnInit {
  posts = signal<ForumPost[]>([]);
  loading = signal(false);

  private readonly apiBase = 'http://localhost:8080/api/forum';

  constructor(private http: HttpClient, public auth: AuthService) {}

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
