import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ThemeService } from '../../../core/services/theme.service';

/** Full-screen offline state with animated glass panel. */
@Component({
  selector: 'app-offline-page',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  templateUrl: './offline-page.component.html',
  styleUrl: './offline-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OfflinePageComponent {
  constructor(
    readonly theme: ThemeService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  toggleTheme(): void {
    this.theme.toggle();
    this.cdr.markForCheck();
  }
}
