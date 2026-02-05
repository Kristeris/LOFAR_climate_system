import { Component, EventEmitter, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface DateFilterCriteria {
  startDate: string | null;
  endDate: string | null;
  startTime: string | null;
  endTime: string | null;
}

@Component({
  selector: 'app-date-filter',
  imports: [CommonModule, FormsModule],
  templateUrl: './date-filter.html',
  styleUrl: './date-filter.css',
})
export class DateFilter {
  @Output() filterChange = new EventEmitter<DateFilterCriteria>();

  startDate = signal<string>('');
  endDate = signal<string>('');
  startTime = signal<string>('');
  endTime = signal<string>('');

  isExpanded = signal<boolean>(true);

  applyFilter(): void {
    const criteria: DateFilterCriteria = {
      startDate: this.startDate() || null,
      endDate: this.endDate() || null,
      startTime: this.startTime() || null,
      endTime: this.endTime() || null,
    };
    this.filterChange.emit(criteria);
  }

  clearFilter(): void {
    this.startDate.set('');
    this.endDate.set('');
    this.startTime.set('');
    this.endTime.set('');
    this.filterChange.emit({
      startDate: null,
      endDate: null,
      startTime: null,
      endTime: null,
    });
  }

  toggleExpanded(): void {
    this.isExpanded.set(!this.isExpanded());
  }
}