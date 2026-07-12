import { TestBed } from '@angular/core/testing';

import { PermisionService } from './permision-service';

describe('PermisionService', () => {
  let service: PermisionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PermisionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
