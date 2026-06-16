package projectlx.fleet.management.utils.enums;

/**
 * Compliance document categories for fleet assets and drivers.
 * Labels in the portal are jurisdiction-agnostic; local equivalents (e.g. ZINARA, VID, RMT) are shown as hints.
 */
public enum ComplianceType {
    /** Commercial vehicle insurance (third-party or comprehensive, incl. public liability). */
    INSURANCE,
    /** Driver licence with correct vehicle weight class. */
    LICENSE,
    MAINTENANCE,
    /** Certificate of fitness / roadworthiness from the vehicle inspectorate. */
    ROADWORTHINESS,
    /** Vehicle-specific operating disc or commutation permit. */
    PERMIT,
    /** Vehicle registration book or certified copy proving lawful possession. */
    VEHICLE_REGISTRATION,
    /** Annual road licence disc or equivalent road-tax credential. */
    ROAD_LICENSE,
    /** Goods / freight operator licence authorising commercial carriage. */
    GOODS_OPERATOR_LICENCE,
    /** Permit for transporting hazardous or flammable cargo. */
    HAZARDOUS_SUBSTANCES_PERMIT,
    /** Fire-safety and energy-regulator clearance for dangerous-goods vehicles. */
    FIRE_SAFETY_CLEARANCE,
    /** Lease or hire agreement when the vehicle is not owned by the operator. */
    LEASE_HIRE_AGREEMENT,
    /** Defensive driving certificate for commercial / PSV operations. */
    DEFENSIVE_DRIVING_CERTIFICATE,
    /** Professional driver medical fitness certificate. */
    DRIVER_MEDICAL_CERTIFICATE,
    OTHER
}
