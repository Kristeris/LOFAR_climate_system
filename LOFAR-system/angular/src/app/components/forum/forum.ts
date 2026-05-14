import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { ForumPost, ForumComment } from '../../models/forumpost';
import { calculateBeamletsFromForum } from '../../services/sub-band.js';

export type PostPreset = 'simple' | 'lofar';

export interface LuMPRecorderConfig {
  enabled: boolean;
  port: string;
  clockSpeed: string;
  beamletsPerLane: string;
  datadir: string;
  dataType: string;
  stationName: string;
  writerType: string;
  physicalBeamletArray: string;
  rcuMode: string;
  epoch: string;
  duration: string;
  subbandArray: string;
  filenameBase: string;
  sourcename: string;
  rightAscension: string;
  declination: string;
  startDate: string;
  recorderNumCores: string;
}

export interface LofarObsFields {
  targetName: string;
  swlevel: string;
  rspMode: string;
  rspSelect: string;
  bitmode: string;
  antennaset: string;
  rcus: string;
  band: string;
  subbands: string;
  clock: string;
  beamlets: string;
  anadir: string;
  anadirX: string;
  anadirY: string;
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
  lumpRecorders: LuMPRecorderConfig[];
}

function defaultLuMPRecorder(port: string, beamletArray: string, subbandArray: string, filenameBase: string): LuMPRecorderConfig {
  return {
    enabled: false,
    port,
    clockSpeed: '200',
    beamletsPerLane: '122',
    datadir: './',
    dataType: 'L_intComplex16_t',
    stationName: 'LV614',
    writerType: 'LuMP1',
    physicalBeamletArray: beamletArray,
    rcuMode: '5',
    epoch: 'J2000',
    duration: '600',
    subbandArray,
    filenameBase,
    sourcename: 'J0332+5434',
    rightAscension: '0.92934187',
    declination: '0.95257923',
    startDate: '2025-08-27T11:05:00Z',
    recorderNumCores: '2',
  };
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

  simbadLoading = signal(false);
lofar: LofarObsFields = {
      targetName:  'JUPITER',
      swlevel:     '3',
      rspMode:     '3',
      rspSelect:   '0:23,26:127,130:191',
      bitmode:     '8',
      antennaset:  'LBA_OUTER',
      rcus:        '0:23,26:127,130:191',
      band:        '10',
      subbands:    '40:283',
      beamlets:    '0:243',
      clock:       '200',
      anadir:      '0,0,JUPITER',
      anadirX:      '0',
      anadirY:      '0',
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
      lumpRecorders: [
        defaultLuMPRecorder('16140', '[0:122]', '[23:145]', 'J0332+5434_1'),
        defaultLuMPRecorder('16141', '[122:244]', '[145:267]', 'J0332+5434_2'),
        defaultLuMPRecorder('16142', '[244:366]', '[267:389]', 'J0332+5434_3'),
        defaultLuMPRecorder('16143', '[366:488]', '[389:511]', 'J0332+5434_4'),
      ],
    };

  filterUser   = signal<string>('all');
  filterShowCurrent = signal(true);
  filterShowFuture  = signal(true);
  filterShowPast    = signal(true);
  filterShowSuccess = signal(true);
  filterShowFailure = signal(true);
  filterShowUnknown = signal(true);

  uniqueUsers  = computed(() => [...new Set(this.posts().map(p => p.authorUsername))].sort());

  filteredPosts = computed(() => {
    return this.posts().filter(p => {
      const userMatch = this.filterUser() === 'all' || p.authorUsername === this.filterUser();
      const statusMatch =
        (p.status === 'CURRENT' && this.filterShowCurrent()) ||
        (p.status === 'FUTURE'  && this.filterShowFuture()) ||
        (p.status === 'PAST'    && this.filterShowPast());
      if (!userMatch || !statusMatch) return false;
      if (p.status === 'PAST') {
        const outcome = p.outcomeStatus;
        if (outcome === 'SUCCESS' && !this.filterShowSuccess()) return false;
        if (outcome === 'FAILURE' && !this.filterShowFailure()) return false;
        if ((!outcome || outcome === 'UNKNOWN') && !this.filterShowUnknown()) return false;
      }
      return true;
    });
  });

  currentPosts = computed(() => this.filteredPosts().filter(p => p.status === 'CURRENT'));
  futurePosts  = computed(() => this.filteredPosts().filter(p => p.status === 'FUTURE'));
  pastPosts    = computed(() => this.filteredPosts().filter(p => p.status === 'PAST'));

  /** Comments */
  expandedPostId = signal<number | null>(null);
  commentInput = signal('');

  private readonly apiBase = 'http://localhost:8080/api/forum';

  constructor(private http: HttpClient, public auth: AuthService) {}

  ngOnInit(): void {
    this.loadPosts();
    this.recalculateBeamlets();
  }

  setPreset(preset: PostPreset): void {
    this.activePreset.set(preset);
    if (preset === 'lofar' && !this.newTitle.trim()) {
      this.newTitle = `LOFAR observation – ${this.lofar.targetName}`;
    }
    if (preset === 'lofar') {
      this.recalculateBeamlets();
    }
  }

  private recalculateBeamlets(): void {
    const beamlets = calculateBeamletsFromForum(
      this.lofar.rspMode,
      this.lofar.subbands,
      this.lofar.clock
    );
    if (beamlets) {
      this.lofar.beamlets = beamlets;
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
    this.recalculateBeamlets();
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
    this.lofar.subbands = this.getSubbandsDefault();
    this.recalculateBeamlets();
  }

  onClockChange(): void {
    this.recalculateBeamlets();
  }

  onSubbandsChange(): void {
    this.recalculateBeamlets();
  }

  private readonly simbadApi = 'http://localhost:8080/api/simbad';

  /** Auto-update title when target name changes in LOFAR mode */
onTargetNameChange(): void {
    if (this.activePreset() === 'lofar') {
      this.newTitle = `LOFAR observation – ${this.lofar.targetName}`;
      this.lofar.anadirX = '0';
      this.lofar.anadirY = '0';
      this.lofar.anadirSystem = this.lofar.targetName;
      this.lookupSimbad(this.lofar.targetName);
    }
  }

  lookupSimbad(target: string): void {
    const name = target?.trim();
    if (!name) return;
    const planets = ['MERCURY', 'VENUS', 'JUPITER', 'SATURN', 'URANUS', 'NEPTUNE', 'PLUTO', 'SUN', 'MOON'];
    if (planets.includes(name.toUpperCase())) return;

    this.simbadLoading.set(true);
    this.http.get<any>(`${this.simbadApi}/coordinates`, { params: { object: name } }).subscribe({
      next: (data) => {
        if (data && data.raRad != null && data.decRad != null) {
          this.lofar.anadirX = data.raRad.toString();
          this.lofar.anadirY = data.decRad.toString();
          this.lofar.anadirSystem = 'J2000';
        }
        this.simbadLoading.set(false);
      },
      error: () => this.simbadLoading.set(false)
    });
  }

  isPlanetSystem(): boolean {
    const sys = this.lofar.anadirSystem;
    return ['MERCURY', 'VENUS', 'JUPITER', 'SATURN', 'URANUS', 'NEPTUNE', 'PLUTO', 'SUN', 'MOON'].includes(sys);
  }

  onCoordsChange(): void {
    const xStr = this.lofar.anadirX?.trim() || '';
    const yStr = this.lofar.anadirY?.trim() || '';
    
    if (!xStr || !yStr) return;
    
    let ra = parseFloat(xStr);
    let dec = parseFloat(yStr);
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
      this.lofar.anadirX = ra.toString();
      this.lofar.anadirY = dec.toString();
    }
    this.lofar.digdir = `${this.lofar.anadirX},${this.lofar.anadirY},${this.lofar.anadirSystem}`;
  }

  isCoordinateSystem(): boolean {
    const sys = this.lofar.anadirSystem;
    return ['J2000', 'ITRF', 'B1950', 'AZELGEO', 'GALACTIC', 'ECLIPTIC'].includes(sys);
  }

  validateAnadirCoords(): string | null {
    if (this.isPlanetSystem()) return null;
    const x = this.lofar.anadirX?.trim() || '';
    const y = this.lofar.anadirY?.trim() || '';
    const ra = parseFloat(x);
    const dec = parseFloat(y);
    if (isNaN(ra) || isNaN(dec)) return 'Invalid coordinates';
    if (ra < 0 || ra > 3.141593) return 'X must be 0 to 180 degrees (0 to 3.141593 rad)';
    if (dec < -1.570796 || dec > 1.570796) return 'Y must be -90 to 90 degrees (-1.570796 to 1.570796 rad)';
    return null;
  }

  onAnadirSystemChange(): void {
    if (this.isPlanetSystem()) {
      this.lofar.anadirX = '0';
      this.lofar.anadirY = '0';
    }
    this.lofar.digdir = `${this.lofar.anadirX},${this.lofar.anadirY},${this.lofar.anadirSystem}`;
  }

  buildLofarContent(): string {
    const f = this.lofar;
    let content = `LCU
swlevel ${f.swlevel}
rspctl --mode=${f.rspMode} --select=${f.rspSelect}
rspctl --rcu
rspctl --bitmode=${f.bitmode}
nohup beamctl --antennaset=${f.antennaset} --rcus=${f.rcus} --band=${f.band} --beamlets=${f.beamlets} --subbands=${f.subbands} --anadir=${f.anadirX},${f.anadirY},${f.anadirSystem} digdir=${f.digdir}&`;

    if (this.druExpanded) {
      content += `

DRU screen
cd ${f.druPath}
nohup dump_udp_ow_17 --compress --duration ${f.duration} --ports ${f.port1} --out ${f.outName} --check --Maxfilesize ${f.maxFilesize} --timeout ${f.timeout} --dropped_kernel --bufsize ${f.bufsize} --sock_bufsize ${f.sockBufsize} --skip ${f.skip} --verbose &
nohup dump_udp_ow_17 --compress --duration ${f.duration} --ports ${f.port2} --out ${f.outName} --check --Maxfilesize ${f.maxFilesize} --timeout ${f.timeout} --dropped_kernel --bufsize ${f.bufsize} --sock_bufsize ${f.sockBufsize} --skip ${f.skip} --verbose &`;
    }

    const enabledRecorders = f.lumpRecorders.filter(r => r.enabled);
    if (enabledRecorders.length > 0) {
      content += `\n\nLuMP recorders\n`;
      for (const rec of enabledRecorders) {
        const rcuModeArray = `[${rec.rcuMode}]*${rec.beamletsPerLane}`;
        const epochArray = `[${rec.epoch}]*${rec.beamletsPerLane}`;
        const sourcenameArray = `[${rec.sourcename}]*${rec.beamletsPerLane}`;
        const raArray = `[${rec.rightAscension}]*${rec.beamletsPerLane}`;
        const decArray = `[${rec.declination}]*${rec.beamletsPerLane}`;
        content += `Basic_LuMP_Recorder.py --port=${rec.port} --clock_speed=${rec.clockSpeed} --beamlets_per_lane=${rec.beamletsPerLane} --datadir=${rec.datadir} --data_type_in=${rec.dataType} --station_name=${rec.stationName} --writer_type=${rec.writerType} --physical_beamlet_array=${rec.physicalBeamletArray} --rcumode_array=${rcuModeArray} --epoch_array=${epochArray} --verbose --duration=${rec.duration} --subband_array=${rec.subbandArray} --filename_base=${rec.filenameBase} --sourcename_array=${sourcenameArray} --rightascension_array=${raArray} --declination_array=${decArray} --start_date=${rec.startDate} --recorder_num_cores=${rec.recorderNumCores} &\n`;
      }
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
    const reason = prompt('Delete reason (optional - will be logged):');
    if (reason === null) return;
    this.http.delete(`${this.apiBase}/${id}`, { params: { reason: reason || '' } }).subscribe({
      next: () => this.loadPosts(),
      error: () => this.error.set('Failed to delete post.')
    });
  }

  // ── Comments ─────────────────────────────────────────────────

  toggleComments(post: ForumPost): void {
    if (this.expandedPostId() === post.id) {
      this.expandedPostId.set(null);
      return;
    }
    this.expandedPostId.set(post.id);
    this.commentInput.set('');
    this.loadComments(post);
  }

  loadComments(post: ForumPost): void {
    this.http.get<ForumComment[]>(`${this.apiBase}/${post.id}/comments`).subscribe({
      next: (data) => {
        this.posts.update(posts =>
          posts.map(p => p.id === post.id ? { ...p, comments: data, commentCount: data.length } : p)
        );
      }
    });
  }

  addComment(postId: number): void {
    const text = this.commentInput().trim();
    if (!text) return;
    this.commentInput.set('');
    this.http.post<ForumComment>(`${this.apiBase}/${postId}/comments`, { content: text }).subscribe({
      next: () => {
        const post = this.posts().find(p => p.id === postId);
        if (post) this.loadComments(post);
      },
      error: (err) => this.error.set(err.error?.error ?? 'Failed to add comment.')
    });
  }

  deleteComment(postId: number, commentId: number): void {
    this.http.delete(`${this.apiBase}/${postId}/comments/${commentId}`).subscribe({
      next: () => {
        const post = this.posts().find(p => p.id === postId);
        if (post) this.loadComments(post);
      },
      error: (err) => this.error.set(err.error?.error ?? 'Failed to delete comment.')
    });
  }

  editingTimeId: number | null = null;
  editingTimeDate = '';
  editingTimeHour = '';

  startEditTime(post: ForumPost): void {
    this.editingTimeId = post.id;
    if (post.scheduledDateTime) {
      const dt = new Date(post.scheduledDateTime);
      this.editingTimeDate = dt.toISOString().split('T')[0];
      this.editingTimeHour = dt.toTimeString().slice(0, 5);
    } else {
      this.editingTimeDate = '';
      this.editingTimeHour = '';
    }
  }

  cancelEditTime(): void {
    this.editingTimeId = null;
    this.editingTimeDate = '';
    this.editingTimeHour = '';
  }

  saveTime(id: number): void {
    if (!this.editingTimeDate || !this.editingTimeHour) {
      this.error.set('Date and time are required.');
      return;
    }
    const scheduledDateTime = `${this.editingTimeDate}T${this.editingTimeHour}:00`;
    this.http.put<ForumPost>(`${this.apiBase}/${id}/time`, { scheduledDateTime }).subscribe({
      next: () => {
        this.successMsg.set('Scheduled time updated successfully.');
        this.cancelEditTime();
        this.loadPosts();
        setTimeout(() => this.successMsg.set(null), 5000);
      },
      error: (err) => this.error.set(err.error?.error ?? 'Failed to update time.')
    });
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  canAdminEdit(post: ForumPost): boolean {
    if (!post.scheduledDateTime) return false;
    return new Date(post.scheduledDateTime).getTime() - Date.now() > 60000;
  }

  formatEndDate(post: ForumPost): string {
    if (!post.scheduledDateTime || !post.durationSeconds) return '—';
    const end = new Date(new Date(post.scheduledDateTime).getTime() + post.durationSeconds * 1000);
    return end.toLocaleString('en-GB', {
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
      subbands:    '40:283',
      beamlets:    '0:243',
      clock:       '200',
      anadir:      '0,0,JUPITER',
      anadirX:     '0',
      anadirY:     '0',
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
      lumpRecorders: [
        defaultLuMPRecorder('16140', '[0:122]', '[23:145]', 'J0332+5434_1'),
        defaultLuMPRecorder('16141', '[122:244]', '[145:267]', 'J0332+5434_2'),
        defaultLuMPRecorder('16142', '[244:366]', '[267:389]', 'J0332+5434_3'),
        defaultLuMPRecorder('16143', '[366:488]', '[389:511]', 'J0332+5434_4'),
      ],
    };
    this.recalculateBeamlets();
  }
}
