import { isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class PlatformService {
  private platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);

  isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  public actualRoute() {
    return this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    );
  }
}
