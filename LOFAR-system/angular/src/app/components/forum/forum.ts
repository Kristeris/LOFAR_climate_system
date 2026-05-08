import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { ForumPost } from '../../models/forumpost';
import { calculateBeamletsFromForum } from '../../services/sub-band.js';

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
  lumpEnabled: boolean;
  lumpPort: string;
  lumpClockSpeed: string;
  lumpBeamletsPerLane: string;
  lumpDatadir: string;
  lumpDataType: string;
  lumpStationName: string;
  lumpWriterType: string;
  lumpPhysicalBeamletArray: string;
  lumpRcuMode: string;
  lumpEpoch: string;
  lumpDuration: string;
  lumpSubbandArray: string;
  lumpFilenameBase: string;
  lumpSourcename: string;
  lumpRightAscension: string;
  lumpDeclination: string;
  lumpStartDate: string;
  lumpRecorderNumCores: string;
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
  lumpExpanded = true;

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
      lumpEnabled:   false,
      lumpPort:      '16140',
      lumpClockSpeed: '200',
      lumpBeamletsPerLane: '122',
      lumpDatadir:     './',
      lumpDataType:   'L_intComplex16_t',
      lumpStationName: 'LV614',
      lumpWriterType:  'LuMP1',
      lumpPhysicalBeamletArray: '[0:122]',
      lumpRcuMode:     '5',
      lumpEpoch:      'J2000',
      lumpDuration:    '600',
      lumpSubbandArray: '[23:145]',
      lumpFilenameBase: 'J0332+5434',
      lumpSourcename:  'J0332+5434',
      lumpRightAscension: '0.92934187',
      lumpDeclination: '0.95257923',
      lumpStartDate:   '2025-08-27T11:05:00Z',
      lumpRecorderNumCores: '2',
    };

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

  /** Auto-update title when target name changes in LOFAR mode */
onTargetNameChange(): void {
    if (this.activePreset() === 'lofar') {
      this.newTitle = `LOFAR observation – ${this.lofar.targetName}`;
      this.lofar.anadirX = '0';
      this.lofar.anadirY = '0';
      this.lofar.anadirSystem = this.lofar.targetName;
    }
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

    if (f.lumpEnabled) {
      const beamletStart = parseInt(f.lumpPhysicalBeamletArray.replace(/[\[\]:]/g, '').split(':')[0] || '0', 10);
      const subbandStart = parseInt(f.lumpSubbandArray.replace(/[\[\]:]/g, '').split(':')[0] || '0', 10);
      const beamletsPerLane = parseInt(f.lumpBeamletsPerLane, 10) || 122;
      const basePort = parseInt(f.lumpPort, 10) || 16140;

      const lane1Beamlet = `${beamletStart}:${beamletStart + beamletsPerLane}`;
      const lane1Subband = `${subbandStart}:${subbandStart + beamletsPerLane}`;
      const lane2Beamlet = `${beamletStart + beamletsPerLane}:${beamletStart + beamletsPerLane * 2}`;
      const lane2Subband = `${subbandStart + beamletsPerLane}:${subbandStart + beamletsPerLane * 2}`;
      const lane3Beamlet = `${beamletStart + beamletsPerLane * 2}:${beamletStart + beamletsPerLane * 3}`;
      const lane3Subband = `${subbandStart + beamletsPerLane * 2}:${subbandStart + beamletsPerLane * 3}`;
      const lane4Beamlet = `${beamletStart + beamletsPerLane * 3}:${beamletStart + beamletsPerLane * 4}`;
      const lane4Subband = `${subbandStart + beamletsPerLane * 3}:${subbandStart + beamletsPerLane * 4}`;

      const rcuModeArray = `[${f.lumpRcuMode}]*${beamletsPerLane}`;
      const epochArray = `[${f.lumpEpoch}]*${beamletsPerLane}`;
      const sourcenameArray = `[${f.lumpSourcename}]*${beamletsPerLane}`;
      const raArray = `[${f.lumpRightAscension}]*${beamletsPerLane}`;
      const decArray = `[${f.lumpDeclination}]*${beamletsPerLane}`;

      const baseFile = f.lumpFilenameBase;
      const lumpBase = `Basic_LuMP_Recorder.py --clock_speed=${f.lumpClockSpeed} --beamlets_per_lane=${f.lumpBeamletsPerLane} --datadir=${f.lumpDatadir} --data_type_in=${f.lumpDataType} --station_name=${f.lumpStationName} --writer_type=${f.lumpWriterType}`;

      content += `

LuMP recorders
${lumpBase} --port=${basePort} --physical_beamlet_array=[${lane1Beamlet}] --rcumode_array=${rcuModeArray} --epoch_array=${epochArray} --verbose --subband_array=[${lane1Subband}] --filename_base=${baseFile}_1 --sourcename_array=${sourcenameArray} --rightascension_array=${raArray} --declination_array=${decArray} --start_date=${f.lumpStartDate} --recorder_num_cores=${f.lumpRecorderNumCores} &
${lumpBase} --port=${basePort + 1} --physical_beamlet_array=[${lane2Beamlet}] --rcumode_array=${rcuModeArray} --epoch_array=${epochArray} --verbose --subband_array=[${lane2Subband}] --filename_base=${baseFile}_2 --sourcename_array=${sourcenameArray} --rightascension_array=${raArray} --declination_array=${decArray} --start_date=${f.lumpStartDate} --recorder_num_cores=${f.lumpRecorderNumCores} &
${lumpBase} --port=${basePort + 2} --physical_beamlet_array=[${lane3Beamlet}] --rcumode_array=${rcuModeArray} --epoch_array=${epochArray} --verbose --subband_array=[${lane3Subband}] --filename_base=${baseFile}_3 --sourcename_array=${sourcenameArray} --rightascension_array=${raArray} --declination_array=${decArray} --start_date=${f.lumpStartDate} --recorder_num_cores=${f.lumpRecorderNumCores} &
${lumpBase} --port=${basePort + 3} --physical_beamlet_array=[${lane4Beamlet}] --rcumode_array=${rcuModeArray} --epoch_array=${epochArray} --verbose --subband_array=[${lane4Subband}] --filename_base=${baseFile}_4 --sourcename_array=${sourcenameArray} --rightascension_array=${raArray} --declination_array=${decArray} --start_date=${f.lumpStartDate} --recorder_num_cores=${f.lumpRecorderNumCores} &`;
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
      lumpEnabled:   false,
      lumpPort:      '16140',
      lumpClockSpeed: '200',
      lumpBeamletsPerLane: '122',
      lumpDatadir:     './',
      lumpDataType:   'L_intComplex16_t',
      lumpStationName: 'LV614',
      lumpWriterType:  'LuMP1',
      lumpPhysicalBeamletArray: '[0:122]',
      lumpRcuMode:     '5',
      lumpEpoch:      'J2000',
      lumpDuration:    '600',
      lumpSubbandArray: '[23:145]',
      lumpFilenameBase: 'J0332+5434',
      lumpSourcename:  'J0332+5434',
      lumpRightAscension: '0.92934187',
      lumpDeclination: '0.95257923',
      lumpStartDate:   '2025-08-27T11:05:00Z',
      lumpRecorderNumCores: '2',
    };
    this.recalculateBeamlets();
  }
}
