import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed } from '@angular/core';
import { RouteProgressService } from '../../../core/services/route-progress.service';

/** Fixed top aurora beam — route changes and initial bootstrap. */
@Component({
  selector: 'lx-route-progress',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './lx-route-progress.component.html',
  styleUrl: './lx-route-progress.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LxRouteProgressComponent {
  readonly scale = computed(() => this.progress.progress() / 100);

  constructor(readonly progress: RouteProgressService) {}
}
