import {Injectable} from "@angular/core";
import {Router} from "@angular/router";
import {ACCESS_TOKEN} from "../model/constants";
import {jwtDecode} from "jwt-decode";

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  parsedToken: any;
  authenticated: boolean = false;
  currentUser: any;

  constructor(public router: Router) {
    const token = this.getToken();
    if (token !== null && this.parsedToken == null) {
      this.parsedToken = jwtDecode(token);
    }
  }

  getToken() {
    return localStorage.getItem(ACCESS_TOKEN);
  }

  setAuthentication(accessToken: string) {
    localStorage.setItem(ACCESS_TOKEN, accessToken);
    this.authenticated = true;
  }

  logout() {
    localStorage.removeItem(ACCESS_TOKEN);
    this.authenticated = false;
    this.currentUser = null;
    this.router.navigate(['/login']);
  }

}
