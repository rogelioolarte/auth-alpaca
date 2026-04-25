import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { Permission } from '../models/permission';
import { Observable } from 'rxjs';
import { getParams, Page, Pageable } from '../models/pageable';

@Injectable({
  providedIn: 'root',
})
export class PermisionService {
  private readonly http = inject(HttpClient)

  createPermission(name: string): Observable<Permission> {
    return this.http.post<Permission>(`${environment.API_URL}/api/permissions`, { name })
  }

  updatePermissionById(id: string, name: string): Observable<Permission> {
    return this.http.put<Permission>(`${environment.API_URL}/api/permissions/${id}`, { name })
  }

  deletePermissionById(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.API_URL}/api/permissions/${id}`)
  }

  getPermissionById(id: string): Observable<Permission> {
    return this.http.get<Permission>(`${environment.API_URL}/api/permissions/${id}`)
  }

  getAllPermissions(): Observable<Permission[]> {
    return this.http.get<Permission[]>(`${environment.API_URL}/api/permissions`)
  }

  getAllPagePermissions(pageable: Pageable): Observable<Page<Permission>> {
    return this.http.get<Page<Permission>>(`${environment.API_URL}/api/permissions/page?${getParams(pageable)}`)
  }

}
