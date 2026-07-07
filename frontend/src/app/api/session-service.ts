import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { getParams, Page, Pageable } from '../models/pageable';
import { Session } from '@app/models/session';

@Injectable({
  providedIn: 'root',
})
export class SessionService {

  private readonly http = inject(HttpClient);

  getSessions(pageable: Pageable): Observable<Page<Session>> {
    return this.http.get<Page<Session>>(
      `${environment.API_URL}/api/sessions/page?${getParams(pageable)}`,
    );
  }

  revokeSession(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.API_URL}/api/sessions/${id}`);
  }

  revokeAllSessions(): Observable<void> {
    return this.http.delete<void>(`${environment.API_URL}/api/sessions/all`);
  }
}
