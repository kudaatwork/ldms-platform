import { Injectable, NgZone } from '@angular/core';
import { gsap } from 'gsap';

@Injectable({ providedIn: 'root' })
export class LandingMotionService {
  constructor(private readonly ngZone: NgZone) {}

  /** Attach scroll progress, nav chrome, and section reveal observers. Returns cleanup. */
  bindPageMotion(host: HTMLElement): () => void {
    const scrollBarEl = host.querySelector('.ldms-scroll-bar') as HTMLElement | null;
    let navScrolled = false;
    let navScrollRaf = 0;

    const updateScrollChrome = (): void => {
      const y = window.scrollY || document.documentElement.scrollTop || 0;
      const doc = document.documentElement;
      const max = Math.max(1, doc.scrollHeight - doc.clientHeight);
      if (scrollBarEl) {
        scrollBarEl.style.transform = `scaleX(${Math.min(1, y / max)})`;
      }
      const shell = host.querySelector('.ldms-landing__nav-shell');
      const nextNavScrolled = navScrolled ? y > 48 : y > 96;
      if (shell && nextNavScrolled !== navScrolled) {
        navScrolled = nextNavScrolled;
        shell.classList.toggle('ldms-landing__nav-shell--scrolled', nextNavScrolled);
      }
    };

    const onWindowScroll = (): void => {
      if (navScrollRaf) {
        return;
      }
      navScrollRaf = requestAnimationFrame(() => {
        navScrollRaf = 0;
        updateScrollChrome();
      });
    };

    window.addEventListener('scroll', onWindowScroll, { passive: true });
    updateScrollChrome();
    this.initSectionReveals(host);

    return () => {
      window.removeEventListener('scroll', onWindowScroll);
      if (navScrollRaf) {
        cancelAnimationFrame(navScrollRaf);
      }
    };
  }

  initHeroReveal(host: HTMLElement, selector = '.ldms-page-hero__eyebrow, .ldms-page-hero__title, .ldms-page-hero__lead, .ldms-page-hero__cta'): void {
    this.ngZone.runOutsideAngular(() => {
      const heroEls = host.querySelectorAll(selector);
      if (heroEls.length) {
        gsap.fromTo(
          heroEls,
          { y: 36, opacity: 0 },
          { y: 0, opacity: 1, duration: 0.85, stagger: 0.1, ease: 'power3.out', delay: 0.12 },
        );
      }
    });
  }

  initPrepaidDemoMotion(host: HTMLElement): void {
    this.ngZone.runOutsideAngular(() => {
      const demo = host.querySelector('.ldms-prepaid-demo');
      if (!demo) {
        return;
      }
      gsap.fromTo(
        demo,
        { opacity: 0, y: 28, scale: 0.98 },
        { opacity: 1, y: 0, scale: 1, duration: 0.75, ease: 'power3.out' },
      );
    });
  }

  private initSectionReveals(host: HTMLElement): void {
    const reveals = host.querySelectorAll('.ldms-reveal');
    const revealIn = (target: Element) => target.classList.add('ldms-reveal--in');

    const io = new IntersectionObserver(
      (entries) => {
        requestAnimationFrame(() => {
          entries.forEach((e) => {
            if (e.isIntersecting) {
              revealIn(e.target);
              io.unobserve(e.target);
            }
          });
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -8% 0px' },
    );

    reveals.forEach((element) => {
      const rect = element.getBoundingClientRect();
      const vh = window.innerHeight || document.documentElement.clientHeight || 0;
      if (rect.top < vh * 0.94 && rect.bottom > vh * 0.06) {
        revealIn(element);
      } else {
        io.observe(element);
      }
    });
  }
}
