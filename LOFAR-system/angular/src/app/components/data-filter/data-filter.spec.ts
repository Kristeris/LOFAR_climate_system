import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DataFilter } from './data-filter';

describe('DataFilter', () => {
  let component: DataFilter;
  let fixture: ComponentFixture<DataFilter>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DataFilter]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DataFilter);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
