import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Advertiser, AdvertiserRequest } from '../models/advertiser';
import { getParams, Page, Pageable } from '../models/pageable';

@Injectable({
  providedIn: 'root',
})
export class AdvertiserService {
  private readonly http = inject(HttpClient);

  createAdvertiser(advertiser: AdvertiserRequest): Observable<Advertiser> {
    return this.http.post<Advertiser>(`${environment.API_URL}/api/advertisers`, advertiser);
  }

  updateAdvertiserById(id: string, advertiser: AdvertiserRequest): Observable<Advertiser> {
    return this.http.put<Advertiser>(`${environment.API_URL}/api/advertisers/${id}`, advertiser);
  }

  deleteAdvertiserById(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.API_URL}/api/advertisers/${id}`);
  }

  getAdvertiserById(id: string): Observable<Advertiser> {
    return this.http.get<Advertiser>(`${environment.API_URL}/api/advertisers/${id}`);
  }

  getAllAdvertisers(): Observable<Advertiser[]> {
    return this.http.get<Advertiser[]>(`${environment.API_URL}/api/advertisers`);
  }

  getAllPageAdvertisersForAdmin(pageable: Pageable): Observable<Page<Advertiser>> {
    return this.http.get<Page<Advertiser>>(
      `${environment.API_URL}/api/advertisers/page-admin?${getParams(pageable)}`,
    );
  }

  getAllPageAdvertisers(pageable: Pageable): Observable<Page<Advertiser>> {
    return this.http.get<Page<Advertiser>>(
      `${environment.API_URL}/api/advertisers/page?${getParams(pageable)}`,
    );
  }
}
