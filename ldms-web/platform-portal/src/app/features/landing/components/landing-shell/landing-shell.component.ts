import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  Input,
  OnDestroy,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { environment } from '../../../../../environments/environment';
import { LandingMotionService } from '../../services/landing-motion.service';

export type LandingSitePage = 'welcome' | 'about' | 'pricing' | 'demo' | 'contact';

@Component({
  selector: 'app-landing-shell',
  templateUrl: './landing-shell.component.html',
  styleUrls: ['./landing-shell.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class LandingShellComponent implements AfterViewInit, OnDestroy {
  @Input() activePage: LandingSitePage = 'welcome';
  /** Show in-page section jump links (home only). */
  @Input() showSectionNav = false;

  readonly adminUrl = environment.adminPortalOrigin;
  readonly appleAppStoreUrl = environment.mobileApps.appleAppStoreUrl;
  readonly googlePlayStoreUrl = environment.mobileApps.googlePlayStoreUrl;
  readonly mobileNavOpen = signal(false);

  private cleanupMotion: (() => void) | null = null;

  constructor(
    private readonly router: Router,
    private readonly el: ElementRef<HTMLElement>,
    private readonly motion: LandingMotionService,
  ) {}

  ngAfterViewInit(): void {
    this.cleanupMotion = this.motion.bindPageMotion(this.el.nativeElement);
  }

  ngOnDestroy(): void {
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
}
