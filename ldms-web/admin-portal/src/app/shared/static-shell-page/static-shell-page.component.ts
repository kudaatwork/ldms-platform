import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-static-shell-page',
  templateUrl: './static-shell-page.component.html',
  styleUrls: ['./static-shell-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class StaticShellPageComponent implements OnInit {
  title = '';
  lead = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const d = this.route.snapshot.data;
    this.title = (d['title'] as string) ?? 'Page';
    this.lead =
      (d['lead'] as string) ??
      'This section is scaffolded. Connect your services to show profile, preferences, and support tools here.';
    this.cdr.markForCheck();
  }
}
