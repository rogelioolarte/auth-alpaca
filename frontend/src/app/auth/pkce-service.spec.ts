import { TestBed } from '@angular/core/testing';

import { PkceService } from './pkce-service';

describe('PkceService', () => {
  let service: PkceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PkceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
