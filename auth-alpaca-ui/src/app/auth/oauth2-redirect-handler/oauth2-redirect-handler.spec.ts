import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Oauth2RedirectHandler } from './oauth2-redirect-handler';

describe('Oauth2RedirectHandler', () => {
  let component: Oauth2RedirectHandler;
  let fixture: ComponentFixture<Oauth2RedirectHandler>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Oauth2RedirectHandler],
    }).compileComponents();

    fixture = TestBed.createComponent(Oauth2RedirectHandler);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
