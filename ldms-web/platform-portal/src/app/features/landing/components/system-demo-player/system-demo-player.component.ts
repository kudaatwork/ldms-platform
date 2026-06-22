import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  NgZone,
  OnDestroy,
  Output,
  signal,
} from '@angular/core';

@Component({
  selector: 'app-system-demo-player',
  templateUrl: './system-demo-player.component.html',
  styleUrls: ['./system-demo-player.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SystemDemoPlayerComponent implements AfterViewInit, OnDestroy {
  @Output() sceneChange = new EventEmitter<number>();

  readonly isPlaying = signal(false);
  readonly activeScene = signal(0);
  readonly sceneCount = 4;

  readonly scenes = [
    { label: 'Dispatch', icon: 'local_shipping' },
    { label: 'Live trip', icon: 'map' },
    { label: 'Wallet', icon: 'account_balance_wallet' },
    { label: 'Delivery', icon: 'task_alt' },
  ] as const;

  private timerId: ReturnType<typeof setInterval> | null = null;
  private observer: IntersectionObserver | null = null;
  private hasAutoStarted = false;
  private readonly sceneMs = 4500;

  constructor(
    private readonly el: ElementRef<HTMLElement>,
    private readonly ngZone: NgZone,
  ) {}

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => {
      this.observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting && !this.hasAutoStarted && !this.isPlaying()) {
              this.hasAutoStarted = true;
              this.ngZone.run(() => this.play());
            }
          });
        },
        { threshold: 0.45 },
      );
      this.observer.observe(this.el.nativeElement);
    });
  }

  ngOnDestroy(): void {
    this.stopTimer();
    this.observer?.disconnect();
  }

  togglePlay(): void {
    if (this.isPlaying()) {
      this.pause();
      return;
    }
    this.play();
  }

  play(): void {
    this.isPlaying.set(true);
    this.sceneChange.emit(this.activeScene());
    this.startTimer();
  }

  pause(): void {
    this.isPlaying.set(false);
    this.stopTimer();
  }

  goToScene(index: number): void {
    this.activeScene.set(index);
    this.sceneChange.emit(index);
    if (this.isPlaying()) {
      this.startTimer();
    }
  }

  private startTimer(): void {
    this.stopTimer();
    this.timerId = setInterval(() => {
      const next = (this.activeScene() + 1) % this.sceneCount;
      this.activeScene.set(next);
      this.sceneChange.emit(next);
    }, this.sceneMs);
  }

  private stopTimer(): void {
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }
}
