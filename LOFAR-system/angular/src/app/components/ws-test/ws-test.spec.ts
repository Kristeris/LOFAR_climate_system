import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WsTest } from './ws-test';

describe('WsTest', () => {
  let component: WsTest;
  let fixture: ComponentFixture<WsTest>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WsTest]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WsTest);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
