import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdvertiserPage } from './advertisers';

describe('Advertiser', () => {
  let component: AdvertiserPage;
  let fixture: ComponentFixture<AdvertiserPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdvertiserPage],
    }).compileComponents();

    fixture = TestBed.createComponent(AdvertiserPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
