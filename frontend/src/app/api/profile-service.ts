import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Profile } from '../models/profile';
import { environment } from '../../environments/environment';
import { getParams, Page, Pageable } from '../models/pageable';

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private readonly http = inject(HttpClient)

  createProfile(name: string): Observable<Profile> {
    return this.http.post<Profile>(`${environment.API_URL}/api/profiles`, { name })
  }

  updateProfileById(id: string, name: string): Observable<Profile> {
    return this.http.put<Profile>(`${environment.API_URL}/api/profiles/${id}`, { name })
  }

  deleteProfileById(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.API_URL}/api/profiles/${id}`)
  }

  getProfileById(id: string): Observable<Profile> {
    return this.http.get<Profile>(`${environment.API_URL}/api/profiles/${id}`)
  }

  getAllProfiles(): Observable<Profile[]> {
    return this.http.get<Profile[]>(`${environment.API_URL}/api/profiles`)
  }

  getAllPageProfiles(pageable: Pageable): Observable<Page<Profile>> {
    return this.http.get<Page<Profile>>(`${environment.API_URL}/api/profiles/page?${getParams(pageable)}`)
  }
}
