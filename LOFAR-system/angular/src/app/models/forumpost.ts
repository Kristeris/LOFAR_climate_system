export interface ForumPost {
  id: number;
  title: string;
  content: string;
  createdAt: string;
  authorUsername: string;
  googleCalendarEventId: string | null;
}