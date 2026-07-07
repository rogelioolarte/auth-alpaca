import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { ownerGuard } from './owner-guard';

describe('ownerGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => ownerGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
