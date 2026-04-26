import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdvertiserId } from './advertiser-id';

describe('AdvertiserId', () => {
  let component: AdvertiserId;
  let fixture: ComponentFixture<AdvertiserId>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdvertiserId],
    }).compileComponents();

    fixture = TestBed.createComponent(AdvertiserId);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
