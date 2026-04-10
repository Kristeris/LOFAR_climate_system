import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { ForumPost } from '../../models/forumpost';

@Component({
  selector: 'app-forum',
  imports: [CommonModule, FormsModule],
  templateUrl: './forum.html',
  styleUrl: './forum.css'
})
export class Forum implements OnInit {
  posts = signal<ForumPost[]>([]);
  loading = signal(false);
  submitting = signal(false);
  error = signal<string | null>(null);
  successMsg = signal<string | null>(null);

  newTitle = '';
  newContent = '';
  showForm = signal(false);

  scheduledDate = '';
  scheduledTime = '';
  todayDate: string = new Date().toISOString().split('T')[0];

  private readonly apiBase = 'http://localhost:8080/api/forum';

  constructor(private http: HttpClient, public auth: AuthService) {}

  ngOnInit(): void {
    this.loadPosts();
  }

  loadPosts(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<ForumPost[]>(this.apiBase).subscribe({
      next: (data) => { this.posts.set(data); this.loading.set(false); },
      error: () => { this.error.set('Failed to load forum posts.'); this.loading.set(false); }
    });
  }

  toggleForm(): void {
    this.showForm.set(!this.showForm());
    this.error.set(null);
    this.successMsg.set(null);
    if (!this.showForm()) this.resetForm();
  }

  submitPost(): void {
    if (!this.newTitle.trim() || !this.newContent.trim()) {
      this.error.set('Title and content are required.');
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.successMsg.set(null);

    // If user picked a date and time, send it as ISO-8601 string.
    // Otherwise send null so the backend falls back to right now.
    let scheduledDateTime: string | null = null;
    if (this.scheduledDate && this.scheduledTime) {
      scheduledDateTime = `${this.scheduledDate}T${this.scheduledTime}:00`;
    }

    this.http.post<ForumPost>(this.apiBase, {
      title: this.newTitle.trim(),
      content: this.newContent.trim(),
      scheduledDateTime
    }).subscribe({
      next: (created) => {
        this.submitting.set(false);
        this.successMsg.set(
          `✅ Post published! Google Calendar event created (ID: ${created.googleCalendarEventId}). A confirmation e-mail has been sent.`
        );
        this.resetForm();
        this.showForm.set(false);
        this.loadPosts();
        setTimeout(() => this.successMsg.set(null), 8000);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err.error?.error ?? 'Failed to create post.');
      }
    });
  }

  deletePost(id: number): void {
    if (!confirm('Are you sure you want to delete this post?')) return;
    this.http.delete(`${this.apiBase}/${id}`).subscribe({
      next: () => this.loadPosts(),
      error: () => this.error.set('Failed to delete post.')
    });
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatScheduledDisplay(): string {
    if (!this.scheduledDate || !this.scheduledTime) return '';
    return new Date(`${this.scheduledDate}T${this.scheduledTime}:00`).toLocaleString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  private resetForm(): void {
    this.newTitle = '';
    this.newContent = '';
    this.scheduledDate = '';
    this.scheduledTime = '';
  }
}