import { TestBed } from '@angular/core/testing';

import { AdvertiserService } from './advertiser-service';

describe('AdvertiserService', () => {
  let service: AdvertiserService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AdvertiserService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
