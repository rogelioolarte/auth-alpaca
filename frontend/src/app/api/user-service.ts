import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { User } from '../models/user';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { getParams, Page, Pageable } from '../models/pageable';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private readonly http = inject(HttpClient)

  createUser(name: string): Observable<User> {
    return this.http.post<User>(`${environment.API_URL}/api/users`, { name })
  }

  updateUserById(id: string, name: string): Observable<User> {
    return this.http.put<User>(`${environment.API_URL}/api/users/${id}`, { name })
  }

  deleteUserById(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.API_URL}/api/users/${id}`)
  }

  getUserById(id: string): Observable<User> {
    return this.http.get<User>(`${environment.API_URL}/api/users/${id}`)
  }

  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${environment.API_URL}/api/users`)
  }

  getAllPageUsers(pageable: Pageable): Observable<Page<User>> {
    return this.http.get<Page<User>>(`${environment.API_URL}/api/users/page?${getParams(pageable)}`)
  }
}
