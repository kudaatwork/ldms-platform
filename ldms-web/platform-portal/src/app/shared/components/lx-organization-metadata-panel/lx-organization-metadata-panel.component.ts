import { Component, Input } from '@angular/core';
import type {
  OrganizationMetadataItem,
  OrganizationMetadataSection,
  OrganizationPartnerMetadata,
} from '../../models/organization-metadata.model';

@Component({
  selector: 'lx-organization-metadata-panel',
  templateUrl: './lx-organization-metadata-panel.component.html',
  styleUrl: './lx-organization-metadata-panel.component.scss',
  standalone: false,
})
export class LxOrganizationMetadataPanelComponent {
  @Input() metadata: OrganizationPartnerMetadata | null = null;
  @Input() loading = false;
  @Input() error = '';
  @Input() emptyMessage = 'No metadata available for this record.';

  expandedSections = new Set<string>(['profile', 'contact', 'contact-person', 'registration', 'contract', 'kyc']);

  trackSection(_index: number, section: OrganizationMetadataSection): string {
    return section.id;
  }

  trackItem(_index: number, item: OrganizationMetadataItem): string {
    return `${item.label}:${item.value}`;
  }

  isExpanded(sectionId: string): boolean {
    return this.expandedSections.has(sectionId);
  }

  toggleSection(sectionId: string): void {
    if (this.expandedSections.has(sectionId)) {
      this.expandedSections.delete(sectionId);
      return;
    }
    this.expandedSections.add(sectionId);
  }

  badgeClass(tone: OrganizationMetadataItem['tone']): string {
    const key = tone ?? 'default';
    return `lx-org-meta__badge--${key}`;
  }
}
