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
  clock: string;
  anadir: string;
  anadirCoords: string;
  anadirSystem: string;
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
  druExpanded = true;

  lofar: LofarObsFields = {
    targetName:  'JUPITER',
    swlevel:     '3',
    rspMode:     '3',
    rspSelect:   '0:23,26:127,130:191',
    bitmode:     '8',
antennaset:  'LBA_OUTER',
      rcus:        '0:23,26:127,130:191',
      band:        '10',
      beamlets:    '0:243',
      subbands:    '40:283',
      clock:       '200',
      anadir:      '0,0,JUPITER',
      anadirCoords: '0,0',
      anadirSystem: 'JUPITER',
      digdir:      '0,0,JUPITER',
      druPath:     '/mnt/LOFAR0/pulsars/dump_udp_ow/jupiter',
    duration:    '3600',
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

  getBandMin(): number {
    const mode = parseInt(this.lofar.rspMode, 10);
    if (mode === 5) return 200;
    if (mode === 6) return 160;
    if (mode === 7) return 200;
    return 0;
  }

  getBandMax(): number {
    const mode = parseInt(this.lofar.rspMode, 10);
    if (mode === 5) return 100;
    if (mode === 6) return 240;
    if (mode === 7) return 300;
    return 100;
  }

  onModeChange(): void {
    const bandNum = parseInt(this.lofar.band, 10);
    if (isNaN(bandNum) || bandNum < this.getBandMin() || bandNum > this.getBandMax()) {
      this.lofar.band = this.getBandMin().toString();
    }
    const mode = parseInt(this.lofar.rspMode, 10);
    if (mode >= 1 && mode <= 2) this.lofar.antennaset = 'LBL_OUTER';
    if (mode >= 3 && mode <= 4) this.lofar.antennaset = 'LBH_OUTER';
    if (mode >= 5) this.lofar.antennaset = 'HBA_OUTER';
  }

  getBeamletsDefault(): string {
    if (this.lofar.bitmode === '8') return '0:487';
    return '0:243';
  }

  getBeamletsPlaceholder(): string {
    return this.getBeamletsDefault();
  }

  getSubbandsDefault(): string {
    if (this.lofar.bitmode === '8') return '40:527';
    return '40:283';
  }

  getSubbandsPlaceholder(): string {
    return this.getSubbandsDefault();
  }

  getAntennasetPlaceholder(): string {
    const mode = parseInt(this.lofar.rspMode, 10);
    if (mode >= 1 && mode <= 2) return 'LBL_OUTER';
    if (mode >= 3 && mode <= 4) return 'LBH_OUTER';
    return 'HBA_OUTER';
  }

  onBitmodeChange(): void {
    this.lofar.beamlets = this.getBeamletsDefault();
    this.lofar.subbands = this.getSubbandsDefault();
  }

  /** Auto-update title when target name changes in LOFAR mode */
onTargetNameChange(): void {
    if (this.activePreset() === 'lofar') {
      this.newTitle = `LOFAR observation – ${this.lofar.targetName}`;
      this.lofar.anadirCoords = `0,0`;
      this.lofar.anadirSystem = this.lofar.targetName;
    }
  }

  isPlanetSystem(): boolean {
    const sys = this.lofar.anadirSystem;
    return ['MERCURY', 'VENUS', 'JUPITER', 'SATURN', 'URANUS', 'NEPTUNE', 'PLUTO', 'SUN', 'MOON'].includes(sys);
  }

  onCoordsChange(): void {
    const coords = this.lofar.anadirCoords.trim();
    if (!coords) return;
    const parts = coords.split(',');
    if (parts.length !== 2) return;
    let ra = parseFloat(parts[0]);
    let dec = parseFloat(parts[1]);
    let changed = false;
    if (!isNaN(dec)) {
      if (dec < -1.570796) { dec = -1.570796; changed = true; }
      if (dec > 1.570796) { dec = 1.570796; changed = true; }
    }
    if (!isNaN(ra)) {
      if (ra < 0.0) { ra = 0.0; changed = true; }
      if (ra > 3.141593) { ra = 3.141593; changed = true; }
    }
    if (changed) {
      this.lofar.anadirCoords = `${ra},${dec}`;
    }
    this.lofar.digdir = `${this.lofar.anadirCoords},${this.lofar.anadirSystem}`;
  }

  isCoordinateSystem(): boolean {
    const sys = this.lofar.anadirSystem;
    return ['J2000', 'ITRF', 'B1950', 'AZELGEO', 'GALACTIC', 'ECLIPTIC'].includes(sys);
  }

  validateAnadirCoords(): string | null {
    if (this.isPlanetSystem()) return null;
    const coords = this.lofar.anadirCoords.trim();
    const parts = coords.split(',');
    if (parts.length !== 2) return 'Use format: RA,Dec (e.g., 0.5,-0.3)';
    const ra = parseFloat(parts[0]);
    const dec = parseFloat(parts[1]);
    if (isNaN(ra) || isNaN(dec)) return 'Invalid numbers';
    if (ra < 0 || ra > 3.141593) return 'RA must be 0 to 180 degrees (0 to 3.141593 rad)';
    if (dec < -1.570796 || dec > 1.570796) return 'Dec must be -90 to 90 degrees (-1.570796 to 1.570796 rad)';
    return null;
  }

  onAnadirSystemChange(): void {
    if (this.isPlanetSystem()) {
      this.lofar.anadirCoords = '0,0';
    }
    this.lofar.digdir = `${this.lofar.anadirCoords},${this.lofar.anadirSystem}`;
  }

  buildLofarContent(): string {
    const f = this.lofar;
    let content = `LCU
swlevel ${f.swlevel}
rspctl --mode=${f.rspMode} --select=${f.rspSelect}
rspctl --rcu
rspctl --bitmode=${f.bitmode}
nohup beamctl --antennaset=${f.antennaset} --rcus=${f.rcus} --band=${f.band} --beamlets=${f.beamlets} --subbands=${f.subbands} --anadir=${f.anadirCoords},${f.anadirSystem} digdir=${f.digdir}&`;

    if (this.druExpanded) {
      content += `

DRU screen
cd ${f.druPath}
nohup dump_udp_ow_17 --compress --duration ${f.duration} --ports ${f.port1} --out ${f.outName} --check --Maxfilesize ${f.maxFilesize} --timeout ${f.timeout} --dropped_kernel --bufsize ${f.bufsize} --sock_bufsize ${f.sockBufsize} --skip ${f.skip} --verbose &
nohup dump_udp_ow_17 --compress --duration ${f.duration} --ports ${f.port2} --out ${f.outName} --check --Maxfilesize ${f.maxFilesize} --timeout ${f.timeout} --dropped_kernel --bufsize ${f.bufsize} --sock_bufsize ${f.sockBufsize} --skip ${f.skip} --verbose &`;
    }

    return content;
  }

  loadPosts(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<ForumPost[]>(this.apiBase).subscribe({
      next: (data) => {
        this.posts.set(data);
        this.loading.set(false);
        if (data.length === 0 && !this.showForm()) {
          this.showForm.set(true);
        }
      },
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

    if (this.activePreset() === 'lofar') {
      const missingFields: string[] = [];

      if (!this.lofar.targetName?.trim()) missingFields.push('target name');
      if (!this.lofar.swlevel?.trim()) missingFields.push('swlevel');
      if (!this.lofar.rspMode?.trim()) missingFields.push('rspMode');
      if (!this.lofar.rspSelect?.trim()) missingFields.push('rspSelect');
      if (!this.lofar.bitmode?.trim()) missingFields.push('bitmode');
      if (!this.lofar.antennaset?.trim()) missingFields.push('antennaset');
      if (!this.lofar.band?.toString().trim()) missingFields.push('band');
      if (!this.lofar.rcus?.trim()) missingFields.push('rcus');
      if (!this.lofar.beamlets?.trim()) missingFields.push('beamlets');
      if (!this.lofar.subbands?.trim()) missingFields.push('subbands');

      if (missingFields.length > 0) {
        this.error.set(`Missing required fields: ${missingFields.join(', ')}`);
        return;
      }
    }

    const bandNum = parseInt(this.lofar.band, 10);
    const mode = parseInt(this.lofar.rspMode, 10);
    let minBand = 0, maxBand = 100;
    if (mode === 6) { minBand = 160; maxBand = 240; }
    else if (mode === 7) { minBand = 200; maxBand = 300; }
    if (isNaN(bandNum) || bandNum < minBand || bandNum > maxBand) {
      this.error.set(`Band must be between ${minBand} and ${maxBand} for mode ${mode}.`);
      return;
    }

    if (this.isCoordinateSystem()) {
      const coordError = this.validateAnadirCoords();
      if (coordError) {
        this.error.set(coordError);
        return;
      }
    }

    const sys = this.lofar.anadirSystem;
    const coordSystems = ['J2000', 'ITRF', 'B1950', 'AZELGEO', 'GALACTIC', 'ECLIPTIC'];
    if (coordSystems.includes(sys) && !this.validateAnadirCoords()) {
      this.error.set('--anadir must be in range -90° to 90° and 0° to 180°.');
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
band:        '10',
      beamlets:    '0:243',
      subbands:    '40:283',
      clock:       '200',
      anadir:      '0,0,JUPITER',
      anadirCoords: '0,0',
      anadirSystem: 'JUPITER',
      digdir:      '0,0,JUPITER',
      druPath:     '/mnt/LOFAR0/pulsars/dump_udp_ow/jupiter',
      duration:    '3600',
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