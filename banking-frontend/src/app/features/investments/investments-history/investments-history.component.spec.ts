import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InvestmentsHistoryComponent } from './investments-history.component';

describe('InvestmentsHistoryComponent', () => {
  let component: InvestmentsHistoryComponent;
  let fixture: ComponentFixture<InvestmentsHistoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InvestmentsHistoryComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InvestmentsHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
