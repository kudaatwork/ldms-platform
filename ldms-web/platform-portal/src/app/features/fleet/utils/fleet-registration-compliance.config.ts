import type {
  FleetRegistrationComplianceType,
  FleetVehicleOwnershipType,
  FleetVehicleType,
} from '../models/fleet.model';

/** Backend {@code ComplianceType} keys submitted during asset registration. */
export type { FleetRegistrationComplianceType } from '../models/fleet.model';

export type FleetRegistrationComplianceCategory =
  | 'statutory'
  | 'operating_authority'
  | 'commodity'
  | 'contractual'
  | 'driver';

export interface FleetRegistrationDocDefinition {
  complianceType: FleetRegistrationComplianceType;
  category: FleetRegistrationComplianceCategory;
  label: string;
  hint: string;
  icon: string;
  /** When this document becomes mandatory during registration. */
  requiredWhen: 'always' | 'contracted' | 'tanker' | 'driver_assigned';
}

export interface FleetRegistrationDocContext {
  ownershipType: FleetVehicleOwnershipType;
  vehicleType: FleetVehicleType;
  driverAssigned: boolean;
}

export const FLEET_REGISTRATION_COMPLIANCE_CATEGORY_LABELS: Record<FleetRegistrationComplianceCategory, string> = {
  statutory: 'Primary statutory & registration',
  operating_authority: 'Commercial operating authority',
  commodity: 'Hazardous or specialised cargo',
  contractual: 'Contractual possession',
  driver: 'Assigned driver credentials',
};

/**
 * Generic commercial-vehicle compliance catalogue.
 * Hints reference Zimbabwe examples in parentheses; requirements apply internationally.
 */
export const FLEET_REGISTRATION_DOC_DEFINITIONS: FleetRegistrationDocDefinition[] = [
  {
    complianceType: 'VEHICLE_REGISTRATION',
    category: 'statutory',
    label: 'Vehicle registration book',
    hint:
      'Original registration book or certified copy proving lawful possession. For hired vehicles, include the lease or permission-of-use annex. (e.g. CVR logbook in Zimbabwe)',
    icon: 'menu_book',
    requiredWhen: 'always',
  },
  {
    complianceType: 'ROAD_LICENSE',
    category: 'statutory',
    label: 'Road licence disc',
    hint: 'Valid annual road-licence or road-tax disc for the registration plate. (e.g. ZINARA disc)',
    icon: 'confirmation_number',
    requiredWhen: 'always',
  },
  {
    complianceType: 'ROADWORTHINESS',
    category: 'statutory',
    label: 'Certificate of fitness',
    hint:
      'Roadworthiness / fitness certificate from the vehicle inspectorate — braking, suspension, and safety checks. Commercial haulage is typically renewed every 6–12 months. (e.g. VID certificate)',
    icon: 'verified',
    requiredWhen: 'always',
  },
  {
    complianceType: 'INSURANCE',
    category: 'statutory',
    label: 'Commercial vehicle insurance',
    hint:
      'Valid commercial cover note — full third-party or comprehensive — including public liability. Private motor policies do not cover contracted freight operations.',
    icon: 'policy',
    requiredWhen: 'always',
  },
  {
    complianceType: 'GOODS_OPERATOR_LICENCE',
    category: 'operating_authority',
    label: "Goods operator's licence",
    hint:
      'Freight / goods-operator licence authorising commercial carriage of cargo. Held by the truck owner or operating entity. (e.g. RMT Form 5 in Zimbabwe)',
    icon: 'local_shipping',
    requiredWhen: 'always',
  },
  {
    complianceType: 'PERMIT',
    category: 'operating_authority',
    label: 'Vehicle operator disc',
    hint:
      'Vehicle-specific operating or commutation disc tied to the registration, displayed with road-licence and fitness discs. (e.g. RMT operator disc)',
    icon: 'badge',
    requiredWhen: 'always',
  },
  {
    complianceType: 'HAZARDOUS_SUBSTANCES_PERMIT',
    category: 'commodity',
    label: 'Hazardous substances permit',
    hint:
      'Permit for transporting flammable gases, LPG, or other regulated hazardous cargo. (e.g. EMA hazardous substances licence)',
    icon: 'science',
    requiredWhen: 'tanker',
  },
  {
    complianceType: 'FIRE_SAFETY_CLEARANCE',
    category: 'commodity',
    label: 'Fire safety clearance',
    hint:
      'Fire-authority and energy-regulator clearance for dangerous-goods vehicles, including serviced extinguishers and hazchem signage. (e.g. ZERA / municipal fire clearance)',
    icon: 'local_fire_department',
    requiredWhen: 'tanker',
  },
  {
    complianceType: 'LEASE_HIRE_AGREEMENT',
    category: 'contractual',
    label: 'Lease / hire agreement',
    hint:
      'Signed contract between the vehicle owner and your organisation covering maintenance, insurance, driver employment, and liability — kept in the vehicle file.',
    icon: 'handshake',
    requiredWhen: 'contracted',
  },
  {
    complianceType: 'LICENSE',
    category: 'driver',
    label: "Driver's licence",
    hint:
      'Valid licence with the correct weight class for this vehicle. (e.g. SADC Class C/CE or legacy Class 2 in Zimbabwe)',
    icon: 'credit_card',
    requiredWhen: 'driver_assigned',
  },
  {
    complianceType: 'DEFENSIVE_DRIVING_CERTIFICATE',
    category: 'driver',
    label: 'Defensive driving certificate',
    hint:
      'Mandatory defensive-driving credential for public-service or commercial goods vehicles. (e.g. TSCZ DDC in Zimbabwe)',
    icon: 'school',
    requiredWhen: 'driver_assigned',
  },
  {
    complianceType: 'DRIVER_MEDICAL_CERTIFICATE',
    category: 'driver',
    label: 'Driver medical certificate',
    hint: 'Professional driver medical fitness certificate, usually renewed every 12 months.',
    icon: 'medical_services',
    requiredWhen: 'driver_assigned',
  },
];

export function isFleetRegistrationDocRequired(
  definition: FleetRegistrationDocDefinition,
  context: FleetRegistrationDocContext,
): boolean {
  switch (definition.requiredWhen) {
    case 'always':
      return true;
    case 'contracted':
      return context.ownershipType === 'contracted';
    case 'tanker':
      return context.vehicleType === 'tanker';
    case 'driver_assigned':
      return context.driverAssigned;
    default:
      return false;
  }
}

export function resolveFleetRegistrationDocs(context: FleetRegistrationDocContext): FleetRegistrationDocDefinition[] {
  return FLEET_REGISTRATION_DOC_DEFINITIONS.filter((definition) =>
    isFleetRegistrationDocRequired(definition, context),
  );
}

export function groupFleetRegistrationDocsByCategory(
  docs: FleetRegistrationDocDefinition[],
): { category: FleetRegistrationComplianceCategory; label: string; docs: FleetRegistrationDocDefinition[] }[] {
  const order: FleetRegistrationComplianceCategory[] = [
    'statutory',
    'operating_authority',
    'commodity',
    'contractual',
    'driver',
  ];
  return order
    .map((category) => ({
      category,
      label: FLEET_REGISTRATION_COMPLIANCE_CATEGORY_LABELS[category],
      docs: docs.filter((doc) => doc.category === category),
    }))
    .filter((group) => group.docs.length > 0);
}
