import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Oauth2RedirectHandlerComponent } from './oauth2-redirect-handler.component';

describe('Oauth2RedirectHandlerComponent', () => {
  let component: Oauth2RedirectHandlerComponent;
  let fixture: ComponentFixture<Oauth2RedirectHandlerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Oauth2RedirectHandlerComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(Oauth2RedirectHandlerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
