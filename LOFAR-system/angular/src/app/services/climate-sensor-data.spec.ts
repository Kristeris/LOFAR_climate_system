import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ClimateSensorDataService } from './climate-sensor-data';

describe('ClimateSensorDataService', () => {
  let service: ClimateSensorDataService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ClimateSensorDataService]
    });
    service = TestBed.inject(ClimateSensorDataService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
