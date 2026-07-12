/* eslint-disable @typescript-eslint/no-explicit-any */
import { TestBed } from '@angular/core/testing';
import { ManageSessions } from './manage-sessions';
import { SessionService } from '../../../api/session-service';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { MatDialog } from '@angular/material/dialog';

describe('ManageSessions', () => {
  let component: ManageSessions;
  let sessionServiceMock: any;
  let dialogMock: any;

  beforeEach(() => {
    sessionServiceMock = {
      getSessions: vi.fn().mockReturnValue(of({
        content: [],
        page: { size: 10, number: 0, totalElements: 0, totalPages: 0 }
      })),
      revokeSession: vi.fn().mockReturnValue(of(undefined)),
      revokeAllOtherSessions: vi.fn().mockReturnValue(of(undefined)),
    };

    dialogMock = {
      open: vi.fn().mockReturnValue({
        afterClosed: () => of(true),
      }),
    };

    TestBed.configureTestingModule({
      providers: [
        ManageSessions,
        { provide: SessionService, useValue: sessionServiceMock },
        { provide: MatDialog, useValue: dialogMock },
      ],
    });
    component = TestBed.inject(ManageSessions);
  });

  describe('truncateString', () => {
    it('should return the same string if length is <= 20', () => {
      const input = 'Short string';
      expect(component.truncateString(input)).toBe('Short string');
    });

    it('should return the same string if length is exactly 20', () => {
      const input = 'a'.repeat(20);
      expect(component.truncateString(input)).toBe('a'.repeat(20));
    });

    it('should truncate and add ellipsis if length is > 20', () => {
      const input = 'This string is definitely longer than twenty characters';
      expect(component.truncateString(input)).toBe('This string is defin...');
    });

    it('should return empty string if value is null or undefined', () => {
      expect(component.truncateString(null as any)).toBe('');
      expect(component.truncateString(undefined as any)).toBe('');
    });
  });

  describe('revokeSession', () => {
    it('should trigger dialog and call SessionService.revokeSession on confirmation', () => {
      const sessionId = 'test-id';
      component.revokeSession(sessionId);

      expect(dialogMock.open).toHaveBeenCalled();
      expect(sessionServiceMock.revokeSession).toHaveBeenCalledWith(sessionId);
    });
  });

  describe('revokeAllOtherSessions', () => {
    it('should trigger dialog and call SessionService.revokeAllOtherSessions on confirmation', () => {
      component.revokeAllOtherSessions();

      expect(dialogMock.open).toHaveBeenCalled();
      expect(sessionServiceMock.revokeAllOtherSessions).toHaveBeenCalled();
    });
  });
});
