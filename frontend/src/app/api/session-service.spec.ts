import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { SessionService } from './session-service';
import { environment } from '../../environments/environment';
import { Page, Pageable } from '../models/pageable';

describe('SessionService', () => {
  let service: SessionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(SessionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getSessions', () => {
    it('should call the correct endpoint with pageable parameters', () => {
      const mockPageable: Pageable = { page: 0, size: 10 };
      const mockResponse: Page<any> = { 
        content: [], 
        page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } 
      };

      service.getSessions(mockPageable).subscribe(response => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(req => req.url.includes('/api/sessions/page'));
      expect(req.request.method).toBe('GET');
      expect(req.request.url).toContain('page=0');
      expect(req.request.url).toContain('size=10');
      req.flush(mockResponse);
    });
  });

  describe('revokeSession', () => {
    it('should call the correct endpoint to revoke a session by ID', () => {
      const sessionId = 'test-session-id';

      service.revokeSession(sessionId).subscribe();

      const req = httpMock.expectOne(`${environment.API_URL}/api/sessions/${sessionId}`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('revokeAllOtherSessions', () => {
    it('should call the correct endpoint to revoke all sessions', () => {
      service.revokeAllOtherSessions().subscribe();

      const req = httpMock.expectOne(`${environment.API_URL}/api/sessions/all`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
