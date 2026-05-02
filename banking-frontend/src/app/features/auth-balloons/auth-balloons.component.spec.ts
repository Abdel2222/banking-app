import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AuthBalloonsComponent } from './auth-balloons.component';

describe('AuthBalloonsComponent', () => {
  let component: AuthBalloonsComponent;
  let fixture: ComponentFixture<AuthBalloonsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthBalloonsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AuthBalloonsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
