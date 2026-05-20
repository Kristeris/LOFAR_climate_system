import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Navigation } from './navigation';

describe('Navigation', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Navigation],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(Navigation);
    expect(fixture.componentInstance).toBeTruthy();
  });
});