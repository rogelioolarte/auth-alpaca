import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Role } from '../models/role';
import { environment } from '../../environments/environment';
import { getParams, Page, Pageable } from '../models/pageable';

@Injectable({
  providedIn: 'root',
})
export class RoleService {
  private readonly http = inject(HttpClient)

  createRole(name: string): Observable<Role> {
    return this.http.post<Role>(`${environment.API_URL}/api/roles`, { name })
  }

  updateRoleById(id: string, name: string): Observable<Role> {
    return this.http.put<Role>(`${environment.API_URL}/api/roles/${id}`, { name })
  }

  deleteRoleById(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.API_URL}/api/roles/${id}`)
  }

  getRoleById(id: string): Observable<Role> {
    return this.http.get<Role>(`${environment.API_URL}/api/roles/${id}`)
  }

  getAllRoles(): Observable<Role[]> {
    return this.http.get<Role[]>(`${environment.API_URL}/api/roles`)
  }

  getAllPageRoles(pageable: Pageable): Observable<Page<Role>> {
    return this.http.get<Page<Role>>(`${environment.API_URL}/api/roles/page?${getParams(pageable)}`)
  }
}
