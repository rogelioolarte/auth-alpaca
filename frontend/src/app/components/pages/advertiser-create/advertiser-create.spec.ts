import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdvertiserCreate } from './advertiser-create';

describe('AdvertiserCreate', () => {
  let component: AdvertiserCreate;
  let fixture: ComponentFixture<AdvertiserCreate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdvertiserCreate],
    }).compileComponents();

    fixture = TestBed.createComponent(AdvertiserCreate);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
