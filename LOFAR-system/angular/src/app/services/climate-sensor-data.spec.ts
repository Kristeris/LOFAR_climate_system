import { TestBed } from '@angular/core/testing';

import { ClimateSensorData } from './climate-sensor-data';

describe('ClimateSensorData', () => {
  let service: ClimateSensorData;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ClimateSensorData);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
