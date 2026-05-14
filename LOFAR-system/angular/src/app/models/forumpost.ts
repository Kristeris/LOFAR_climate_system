export interface ForumComment {
  id: number;
  postId: number;
  authorUsername: string;
  content: string;
  createdAt: string;
}

export interface ForumPost {
  id: number;
  title: string;
  content: string;
  createdAt: string;
  authorUsername: string;
  googleCalendarEventId: string | null;
  scheduledDateTime: string | null;
  durationSeconds?: number;
  status?: string;
  commentCount?: number;
  comments?: ForumComment[];
}