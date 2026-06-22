import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';
import { SharedModule } from './shared/shared.module';
import { StorageService } from './core/services/storage.service';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [AppComponent],
      providers: [
        {
          provide: StorageService,
          useValue: {
            clearSession: (): void => undefined,
            getRoles: (): string[] => ['ADMIN'],
            getUser: () => null,
            getToken: () => null,
          },
        },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('classifies flat nav entries as links', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const cmp = fixture.componentInstance;
    cmp.rebuildVisibleNav();
    const users = cmp.visibleNavItems.find((i) => i.route === '/users');
    expect(users?.children?.length).toBeGreaterThan(0);
    for (const entry of users!.children!) {
      expect(cmp.navEntryKind(entry)).toBe('link');
    }
  });

  it('renders submenu links when a nav group is expanded', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    cmp.rebuildVisibleNav();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.sb-subitem').length).toBe(0);

    cmp.toggleGroup('/users');
    fixture.detectChanges();

    const subitems = fixture.nativeElement.querySelectorAll('.sb-subitem');
    expect(subitems.length).toBe(4);
    expect(cmp.isGroupExpanded('/users')).toBe(true);

    cmp.toggleGroup('/users');
    fixture.detectChanges();

    expect(cmp.isGroupExpanded('/users')).toBe(false);
    expect(fixture.nativeElement.querySelectorAll('.sb-subitem').length).toBe(0);
  });
});
