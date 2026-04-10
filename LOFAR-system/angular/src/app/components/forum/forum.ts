import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { ForumPost } from '../../models/forumpost';

export type PostPreset = 'simple' | 'lofar';

export interface LofarObsFields {
  targetName: string;
  swlevel: string;
  rspMode: string;
  rspSelect: string;
  bitmode: string;
  antennaset: string;
  rcus: string;
  band: string;
  beamlets: string;
  subbands: string;
  anadir: string;
  digdir: string;
  druPath: string;
  duration: string;
  port1: string;
  port2: string;
  outName: string;
  maxFilesize: string;
  timeout: string;
  bufsize: string;
  sockBufsize: string;
  skip: string;
}

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

  // ── Preset switcher ────────────────────────────────────────────
  activePreset = signal<PostPreset>('simple');

  // ── LOFAR structured fields ────────────────────────────────────
  lofar: LofarObsFields = {
    targetName:  'JUPITER',
    swlevel:     '3',
    rspMode:     '3',
    rspSelect:   '0:23,26:127,130:191',
    bitmode:     '8',
    antennaset:  'LBA_OUTER',
    rcus:        '0:23,26:127,130:191',
    band:        '10_90',
    beamlets:    '0:243',
    subbands:    '40:283',
    anadir:      '0,0,JUPITER',
    digdir:      '0,0,JUPITER',
    druPath:     '/mnt/LOFAR0/pulsars/dump_udp_ow/jupiter',
    duration:    '7200',
    port1:       '16140',
    port2:       '16141',
    outName:     'jupiter',
    maxFilesize: '100e9',
    timeout:     '10',
    bufsize:     '1e9',
    sockBufsize: '1e7',
    skip:        '1',
  };

  private readonly apiBase = 'http://localhost:8080/api/forum';

  constructor(private http: HttpClient, public auth: AuthService) {}

  ngOnInit(): void {
    this.loadPosts();
  }

  setPreset(preset: PostPreset): void {
    this.activePreset.set(preset);
    if (preset === 'lofar' && !this.newTitle.trim()) {
      this.newTitle = `LOFAR observation – ${this.lofar.targetName}`;
    }
  }

  /** Auto-update title when target name changes in LOFAR mode */
  onTargetNameChange(): void {
    if (this.activePreset() === 'lofar') {
      this.newTitle = `LOFAR observation – ${this.lofar.targetName}`;
      this.lofar.anadir = `0,0,${this.lofar.targetName}`;
      this.lofar.digdir = `0,0,${this.lofar.targetName}`;
    }
  }

  buildLofarContent(): string {
    const f = this.lofar;
    return `LCU
swlevel ${f.swlevel}
rspctl --mode=${f.rspMode} --select=${f.rspSelect}
rspctl --rcu
rspctl --bitmode=${f.bitmode}
nohup beamctl --antennaset=${f.antennaset} --rcus=${f.rcus} --band=${f.band} --beamlets=${f.beamlets} --subbands=${f.subbands} --anadir=${f.anadir} --digdir=${f.digdir}&

DRU screen
cd ${f.druPath}
source ~/.profile
nohup dump_udp_ow_17 --compress --duration ${f.duration} --ports ${f.port1} --out ${f.outName} --check --Maxfilesize ${f.maxFilesize} --timeout ${f.timeout} --dropped_kernel --bufsize ${f.bufsize} --sock_bufsize ${f.sockBufsize} --skip ${f.skip} --verbose &
nohup dump_udp_ow_17 --compress --duration ${f.duration} --ports ${f.port2} --out ${f.outName} --check --Maxfilesize ${f.maxFilesize} --timeout ${f.timeout} --dropped_kernel --bufsize ${f.bufsize} --sock_bufsize ${f.sockBufsize} --skip ${f.skip} --verbose &`;
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
    const contentToSend = this.activePreset() === 'lofar'
      ? this.buildLofarContent()
      : this.newContent;

    if (!this.newTitle.trim() || !contentToSend.trim()) {
      this.error.set('Title and content are required.');
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    this.successMsg.set(null);

    let scheduledDateTime: string | null = null;
    if (this.scheduledDate && this.scheduledTime) {
      scheduledDateTime = `${this.scheduledDate}T${this.scheduledTime}:00`;
    }

    this.http.post<ForumPost>(this.apiBase, {
      title: this.newTitle.trim(),
      content: contentToSend.trim(),
      scheduledDateTime
    }).subscribe({
      next: (created) => {
        this.submitting.set(false);
        this.successMsg.set(
          `Post published! Google Calendar event created (ID: ${created.googleCalendarEventId}). A confirmation e-mail has been sent.`
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
    this.activePreset.set('simple');
    this.lofar = {
      targetName:  'JUPITER',
      swlevel:     '3',
      rspMode:     '3',
      rspSelect:   '0:23,26:127,130:191',
      bitmode:     '8',
      antennaset:  'LBA_OUTER',
      rcus:        '0:23,26:127,130:191',
      band:        '10_90',
      beamlets:    '0:243',
      subbands:    '40:283',
      anadir:      '0,0,JUPITER',
      digdir:      '0,0,JUPITER',
      druPath:     '/mnt/LOFAR0/pulsars/dump_udp_ow/jupiter',
      duration:    '7200',
      port1:       '16140',
      port2:       '16141',
      outName:     'jupiter',
      maxFilesize: '100e9',
      timeout:     '10',
      bufsize:     '1e9',
      sockBufsize: '1e7',
      skip:        '1',
    };
  }
}