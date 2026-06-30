import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { LandingMotionService } from '../../services/landing-motion.service';
import { LexxiChatLauncherService, LexxiChatBrand } from '../../../../shared/services/lexxi-chat-launcher.service';
import { LEXXI_BOT_NAME } from '../../../../shared/constants/lexxi-bot.constants';

export type LandingSitePage = 'welcome' | 'about' | 'pricing' | 'demo' | 'contact' | 'support';

@Component({
  selector: 'app-landing-shell',
  templateUrl: './landing-shell.component.html',
  styleUrls: ['./landing-shell.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LandingShellComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() activePage: LandingSitePage = 'welcome';

  readonly adminUrl = environment.adminPortalOrigin;
  readonly appleAppStoreUrl = environment.mobileApps.appleAppStoreUrl;
  readonly googlePlayStoreUrl = environment.mobileApps.googlePlayStoreUrl;
  readonly lexxiName = LEXXI_BOT_NAME;
  readonly mobileNavOpen = signal(false);
  readonly chatOpen = signal(false);
  readonly chatBrand = signal<LexxiChatBrand>('live-chat');

  private cleanupMotion: (() => void) | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly router: Router,
    private readonly el: ElementRef<HTMLElement>,
    private readonly motion: LandingMotionService,
    private readonly chatLauncher: LexxiChatLauncherService,
  ) {}

  ngOnInit(): void {
    this.chatLauncher.isOpen$
      .pipe(takeUntil(this.destroy$))
      .subscribe((open) => this.chatOpen.set(open));
    this.chatLauncher.brand$
      .pipe(takeUntil(this.destroy$))
      .subscribe((brand) => this.chatBrand.set(brand));
  }

  ngAfterViewInit(): void {
    this.cleanupMotion = this.motion.bindPageMotion(this.el.nativeElement);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.cleanupMotion?.();
  }

  isActive(page: LandingSitePage): boolean {
    return this.activePage === page;
  }

  toggleMobileNav(): void {
    this.mobileNavOpen.update((open) => !open);
  }

  closeMobileNav(): void {
    this.mobileNavOpen.set(false);
  }

  goSignup(): void {
    void this.router.navigate(['/signup']);
  }

  selectChatBrand(brand: LexxiChatBrand): void {
    this.closeMobileNav();
    this.chatLauncher.setBrand(brand);
    if (!this.chatOpen()) {
      this.chatLauncher.openChat();
    }
  }

  toggleLiveChat(): void {
    this.closeMobileNav();
    this.chatLauncher.toggleChat();
  }

  openLiveChat(): void {
    this.closeMobileNav();
    this.chatLauncher.openChat();
  }
}
