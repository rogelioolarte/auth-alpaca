import {Injectable} from "@angular/core";
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {catchError, Observable, throwError} from "rxjs";
import {ACCESS_TOKEN, API_BASE_URL} from "../model/constants";
import {UserProfile} from "../model/model";

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  constructor(private http: HttpClient) { }

  private request(options: any): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
    });

    if (localStorage.getItem(ACCESS_TOKEN)) {
      headers.append('Authorization', 'Bearer ' + localStorage.getItem(ACCESS_TOKEN));
    }

    const defaults = { headers: headers };
    options = Object.assign({}, defaults, options);

    return this.http.request(options.method, options.url, options)
      .pipe(
        catchError(error => {
          return throwError(() => error);
        })
      );
  }

  getCurrentUser(): Observable<any> {
    if (!localStorage.getItem(ACCESS_TOKEN)) {
      return throwError(() => new Error('No access token set.'));
    }

    return this.request({
      url: `${API_BASE_URL}/user/me`,
      method: 'GET'
    });
  }

  login(loginRequest: any): Observable<any> {
    return this.request({
      url: `${API_BASE_URL}/auth/login`,
      method: 'POST',
      body: JSON.stringify(loginRequest)
    });
  }

  getUserInfo(): Observable<UserProfile> {
    return this.request({
      url: `${API_BASE_URL}/auth/me`,
      method: 'GET'
    });
  }

}
