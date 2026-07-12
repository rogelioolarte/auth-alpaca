import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Base } from './base';

describe('Base', () => {
  let component: Base;
  let fixture: ComponentFixture<Base>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Base],
    }).compileComponents();

    fixture = TestBed.createComponent(Base);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
