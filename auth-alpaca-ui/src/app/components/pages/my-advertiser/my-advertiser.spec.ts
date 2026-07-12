import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MyAdvertiser } from './my-advertiser';

describe('AdvertiserCreate', () => {
  let component: MyAdvertiser;
  let fixture: ComponentFixture<MyAdvertiser>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyAdvertiser],
    }).compileComponents();

    fixture = TestBed.createComponent(MyAdvertiser);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
