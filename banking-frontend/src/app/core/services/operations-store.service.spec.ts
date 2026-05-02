import { TestBed } from '@angular/core/testing';

import { OperationsStoreService } from './operations-store.service';

describe('OperationsStoreService', () => {
  let service: OperationsStoreService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(OperationsStoreService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
