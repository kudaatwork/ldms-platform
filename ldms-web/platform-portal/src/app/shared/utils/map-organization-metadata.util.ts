import type {
  OrganizationMetadataItem,
  OrganizationMetadataSection,
  OrganizationMetadataTone,
  OrganizationPartnerMetadata,
} from '../models/organization-metadata.model';

function toObj(value: unknown): Record<string, unknown> | null {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

function text(value: unknown): string {
  if (value == null) {
    return '';
  }
  const s = String(value).trim();
  return s;
}

function positiveId(value: unknown): number | undefined {
  const n = Number(value);
  return Number.isFinite(n) && n > 0 ? n : undefined;
}

function formatDate(value: unknown): string {
  if (value == null || value === '') {
    return '';
  }
  const d = new Date(String(value));
  if (Number.isNaN(d.getTime())) {
    return text(value);
  }
  return d.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatLabel(value: string): string {
  return value
    .split('_')
    .filter(Boolean)
    .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
    .join(' ');
}

function uploadItem(label: string, uploadId: unknown): OrganizationMetadataItem | null {
  const id = positiveId(uploadId);
  if (!id) {
    return null;
  }
  return { label, value: `Upload #${id}`, kind: 'upload' };
}

function item(label: string, value: unknown, tone?: OrganizationMetadataTone): OrganizationMetadataItem | null {
  const v = text(value);
  if (!v) {
    return null;
  }
  return { label, value: v, tone };
}

function linkItem(label: string, value: unknown): OrganizationMetadataItem | null {
  const v = text(value);
  if (!v) {
    return null;
  }
  return { label, value: v, kind: 'link' };
}

function badgeItem(label: string, value: unknown, tone?: OrganizationMetadataTone): OrganizationMetadataItem | null {
  const v = text(value);
  if (!v) {
    return null;
  }
  return { label, value: formatLabel(v), kind: 'badge', tone };
}

function boolItem(label: string, value: unknown): OrganizationMetadataItem | null {
  if (value == null) {
    return null;
  }
  const yes = value === true || value === 'true';
  return {
    label,
    value: yes ? 'Yes' : 'No',
    kind: 'badge',
    tone: yes ? 'success' : 'muted',
  };
}

function kycTone(status: string): OrganizationMetadataTone {
  const key = status.toUpperCase();
  if (key === 'APPROVED') {
    return 'success';
  }
  if (key === 'REJECTED') {
    return 'danger';
  }
  if (key === 'DRAFT') {
    return 'muted';
  }
  return 'warn';
}

function section(id: string, title: string, icon: string, items: (OrganizationMetadataItem | null)[]): OrganizationMetadataSection | null {
  const filtered = items.filter((entry): entry is OrganizationMetadataItem => entry != null);
  if (!filtered.length) {
    return null;
  }
  return { id, title, icon, items: filtered };
}

export function mapOrganizationPartnerMetadata(dto: Record<string, unknown>): OrganizationPartnerMetadata {
  const id = Number(dto['id'] ?? 0);
  const name = text(dto['name']) || 'Unnamed organisation';
  const kycStatus = text(dto['kycStatus']);
  const verified = Boolean(dto['isVerified'] ?? dto['verified']);

  const sections: OrganizationMetadataSection[] = [];

  const profile = section('profile', 'Profile', 'business', [
    item('Organisation ID', id),
    item('Legal name', dto['name']),
    badgeItem('Type', dto['organizationType']),
    badgeItem('Classification', dto['organizationClassification']),
    item('Industry', dto['industryName']),
    item('Industry code', dto['industryCode']),
    item('Description', dto['organizationDescription']),
    item('Website', dto['websiteUrl']),
    item('Location ID', dto['locationId']),
    item('Entity status', dto['entityStatus']),
  ]);
  if (profile) {
    sections.push(profile);
  }

  const contact = section('contact', 'Contact', 'contact_mail', [
    item('Email', dto['email']),
    item('Phone', dto['phoneNumber']),
    item('Data protection officer', dto['dataProtectionOfficerContact']),
    boolItem('Two-factor authentication', dto['twoFactorAuthenticationEnabled']),
  ]);
  if (contact) {
    sections.push(contact);
  }

  const contactPerson = section('contact-person', 'Primary contact', 'person', [
    item('First name', dto['contactPersonFirstName']),
    item('Last name', dto['contactPersonLastName']),
    item('Email', dto['contactPersonEmail']),
    item('Phone', dto['contactPersonPhoneNumber']),
    item('Position', dto['contactPersonPosition']),
    badgeItem('Gender', dto['contactPersonGender']),
    item('Date of birth', dto['contactPersonDateOfBirth']),
    item('National ID', dto['contactPersonNationalIdNumber']),
    item('Passport', dto['contactPersonPassportNumber']),
    item('Portal user ID', dto['contactPersonUserId']),
  ]);
  if (contactPerson) {
    sections.push(contactPerson);
  }

  const registration = section('registration', 'Registration & compliance', 'gavel', [
    item('Registration number', dto['registrationNumber']),
    item('Tax number', dto['taxNumber']),
    item('Representative national ID', dto['representativeNationalIdNumber']),
    item('Representative passport', dto['representativePassportNumber']),
    uploadItem('Registration certificate', dto['registrationCertificateUploadId']),
    uploadItem('Tax clearance certificate', dto['taxClearanceCertificateUploadId']),
    uploadItem('Business licence', dto['businessLicenseUploadId']),
    uploadItem('Proof of address', dto['proofOfAddressUploadId']),
    uploadItem('Industry-specific licence', dto['industrySpecificLicenseUploadId']),
    uploadItem('Contact national ID document', dto['contactPersonNationalIdUploadId']),
    uploadItem('Contact passport document', dto['contactPersonPassportUploadId']),
    uploadItem('Logo', dto['logoUploadId']),
  ]);
  if (registration) {
    sections.push(registration);
  }

  const business = section('business', 'Business profile', 'insights', [
    item('Incorporation date', formatDate(dto['incorporationDate'])),
    item('Business hours', dto['businessHours']),
    item('Employees', dto['numberOfEmployees']),
    item('Annual revenue estimate', dto['annualRevenueEstimate']),
    item('Regions served', dto['regionsServed']),
    item('Subscription plan ID', dto['subscriptionPlanId']),
    item('Latitude', dto['latitude']),
    item('Longitude', dto['longitude']),
    item('Account manager user ID', dto['assignedAccountManagerUserId']),
  ]);
  if (business) {
    sections.push(business);
  }

  const kyc = section('kyc', 'KYC & verification', 'verified_user', [
    badgeItem('KYC status', kycStatus, kycTone(kycStatus)),
    {
      label: 'Verified',
      value: verified ? 'Verified' : 'Unverified',
      kind: 'badge',
      tone: verified ? 'success' : 'muted',
    },
    boolItem('Created via signup', dto['createdViaSignup']),
    item('Submitted at', formatDate(dto['submittedAt'])),
    item('Resubmission cycle', dto['currentResubmissionCycle']),
    item('Resubmission count', dto['resubmissionCount']),
    item('Last rejection reason', dto['lastRejectionReason']),
    item('Stage 1 reviewed by', dto['stage1ReviewedBy']),
    item('Stage 1 reviewed at', formatDate(dto['stage1ReviewedAt'])),
    item('Stage 2 reviewed by', dto['stage2ReviewedBy']),
    item('Stage 2 reviewed at', formatDate(dto['stage2ReviewedAt'])),
    item('Required approval stages', dto['kycRequiredApprovalStages']),
    item('Effective approval stages', dto['effectiveKycRequiredApprovalStages']),
  ]);
  if (kyc) {
    sections.push(kyc);
  }

  const social = section('social', 'Social & web', 'share', [
    linkItem('LinkedIn', dto['linkedInUrl']),
    linkItem('Facebook', dto['facebookUrl']),
    linkItem('Twitter / X', dto['twitterUrl']),
    linkItem('Instagram', dto['instagramUrl']),
    linkItem('YouTube', dto['youtubeUrl']),
  ]);
  if (social) {
    sections.push(social);
  }

  const contract = section('contract', 'Contract terms', 'handshake', [
    item('Contract start', formatDate(dto['contractStartDate'])),
    item('Contract end', dto['contractEndDate'] ? formatDate(dto['contractEndDate']) : 'Open-ended'),
    item('Linked on platform', formatDate(dto['contractLinkedAt'])),
  ]);
  if (contract) {
    sections.push(contract);
  }

  const system = section('system', 'System metadata', 'schedule', [
    item('Created', formatDate(dto['createdAt'])),
    item('Last updated', formatDate(dto['updatedAt'])),
  ]);
  if (system) {
    sections.push(system);
  }

  return { id, name, sections };
}

export function extractOrganizationDto(response: unknown): Record<string, unknown> {
  const root = toObj(response);
  if (!root) {
    return {};
  }
  const envelope = toObj(root['data']) ?? toObj(root['body']) ?? toObj(root['payload']) ?? root;
  const list = envelope['organizationDtoList'];
  if (Array.isArray(list) && list.length) {
    return toObj(list[0]) ?? {};
  }
  return toObj(envelope['organizationDto']) ?? envelope;
}
