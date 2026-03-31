export interface KycApplicationRow {
  id: string;
  applicant: string;
  submitted: string;
  status: string;
  statusLabel: string;
}

export interface KycApplicationDocument {
  id: string;
  fileName: string;
  category: string;
  fileType: string;
  uploadedAt: string;
}

export interface KycApplicationDetail extends KycApplicationRow {
  tradingName: string;
  legalForm: string;
  registrationNumber: string;
  taxVatNumber: string;
  industrySector: string;
  primaryContactName: string;
  primaryContactEmail: string;
  primaryContactPhone: string;
  registeredAddress: string;
  principalPlaceOfBusiness: string;
  bankName: string;
  bankAccountMasked: string;
  applicantNotes: string;
  documents: KycApplicationDocument[];
}

export interface KycApplicationDecisionResult {
  decision: 'approve' | 'reject';
  reason: string;
}
