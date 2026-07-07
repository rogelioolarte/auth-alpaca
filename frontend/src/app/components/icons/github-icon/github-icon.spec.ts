import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GithubIcon } from './github-icon';

describe('GithubIcon', () => {
  let component: GithubIcon;
  let fixture: ComponentFixture<GithubIcon>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GithubIcon],
    }).compileComponents();

    fixture = TestBed.createComponent(GithubIcon);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
