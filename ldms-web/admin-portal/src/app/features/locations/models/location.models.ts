import type { LocationType } from './location.enums';

/** Mirrors backend CommonResponse fields used by the portal */
export interface CommonApiFields {
  statusCode: number;
  isSuccess: boolean;
  message?: string;
  errorMessages?: string[];
}

export type EntityStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

export interface Country {
  id: number;
  name: string;
  isoAlpha2Code: string;
  isoAlpha3Code: string;
  dialCode: string;
  timezone: string;
  currencyCode?: string | null;
  geoCoordinatesId?: number | null;
  localizedNameIds?: number[] | null;
  administrativeLevelIds?: number[] | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  entityStatus?: EntityStatus | null;
}

export interface Province {
  id: number;
  name: string;
  code?: string | null;
  countryId: number;
  countryName?: string | null;
  administrativeLevelId?: number | null;
  administrativeLevelName?: string | null;
  localizedNameIds?: number[] | null;
  geoCoordinatesId?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  entityStatus?: EntityStatus | null;
}

export interface District {
  id: number;
  name: string;
  code?: string | null;
  provinceId: number;
  provinceName?: string | null;
  countryId?: number | null;
  countryName?: string | null;
  administrativeLevelId?: number | null;
  administrativeLevelName?: string | null;
  localizedNameIds?: number[] | null;
  geoCoordinatesId?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  entityStatus?: EntityStatus | null;
}

export interface Suburb {
  id: number;
  name: string;
  code?: string | null;
  districtId: number;
  districtName?: string | null;
  /** First-class city (Country → Province → District → City → Suburb). */
  cityId?: number | null;
  cityName?: string | null;
  provinceId?: number | null;
  provinceName?: string | null;
  countryId?: number | null;
  countryName?: string | null;
  geoCoordinatesId?: number | null;
  postalCode?: string | null;
  administrativeLevelId?: number | null;
  administrativeLevelName?: string | null;
  localizedNameIds?: number[] | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  entityStatus?: EntityStatus | null;
}

/** Settlement under a district (parallel tier to {@link Village}). */
export interface City {
  id: number;
  name: string;
  code?: string | null;
  districtId: number;
  districtName?: string | null;
  provinceId?: number | null;
  provinceName?: string | null;
  countryId?: number | null;
  countryName?: string | null;
  latitude?: string | null;
  longitude?: string | null;
  timezone?: string | null;
  postalCode?: string | null;
  entityStatus?: EntityStatus | null;
  createdAt?: string | null;
  createdBy?: string | null;
  modifiedAt?: string | null;
  modifiedBy?: string | null;
}

/** Settlement under a city (same tier as suburb; {@link SettlementType} on address). */
export interface Village {
  id: number;
  name: string;
  code?: string | null;
  cityId: number;
  cityName?: string | null;
  districtId: number;
  districtName?: string | null;
  provinceId?: number | null;
  provinceName?: string | null;
  countryId?: number | null;
  countryName?: string | null;
  suburbId?: number | null;
  suburbName?: string | null;
  latitude?: string | null;
  longitude?: string | null;
  timezone?: string | null;
  postalCode?: string | null;
  entityStatus?: EntityStatus | null;
  createdAt?: string | null;
  createdBy?: string | null;
  modifiedAt?: string | null;
  modifiedBy?: string | null;
}

export interface AdministrativeLevel {
  id: number;
  name: string;
  code?: string | null;
  level?: number | null;
  description?: string | null;
  countryId?: number | null;
  /** Populated by the API from the linked country (for display). */
  countryName?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  entityStatus?: EntityStatus | null;
}

export type SettlementType = 'SUBURB' | 'VILLAGE';

export interface Address {
  id: number;
  line1: string;
  line2?: string | null;
  postalCode?: string | null;
  settlementType?: SettlementType | null;
  settlementId?: number | null;
  suburbId?: number | null;
  suburbName?: string | null;
  villageId?: number | null;
  villageName?: string | null;
  cityId?: number | null;
  cityName?: string | null;
  districtId?: number | null;
  districtName?: string | null;
  provinceId?: number | null;
  provinceName?: string | null;
  countryId?: number | null;
  countryName?: string | null;
  externalSource?: string | null;
  externalPlaceId?: string | null;
  formattedAddress?: string | null;
  geoCoordinatesId?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  entityStatus?: EntityStatus | null;
}

export interface Language {
  id: number;
  name: string;
  isoCode?: string | null;
  nativeName?: string | null;
  isDefault?: boolean | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  entityStatus?: EntityStatus | null;
}

export interface LocalizedName {
  id: number;
  value: string;
  languageId: number;
  referenceType: string;
  referenceId: number;
  createdAt?: string | null;
  updatedAt?: string | null;
  entityStatus?: EntityStatus | null;
}

export interface LocationNode {
  id: number;
  name: string;
  code?: string | null;
  locationType: LocationType;
  parentId?: number | null;
  parentName?: string | null;
  /** Present when the API denormalizes hierarchy (optional). */
  provinceId?: number | null;
  provinceName?: string | null;
  countryId?: number | null;
  countryName?: string | null;
  districtId?: number | null;
  districtName?: string | null;
  suburbId?: number | null;
  suburbName?: string | null;
  latitude?: string | null;
  longitude?: string | null;
  timezone?: string | null;
  postalCode?: string | null;
  aliases?: string[] | null;
  entityStatus?: EntityStatus | null;
  createdAt?: string | null;
  createdBy?: string | null;
  modifiedAt?: string | null;
  modifiedBy?: string | null;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface CountryListResponse extends CommonApiFields {
  countryDto?: Country;
  countryDtoList?: Country[];
  countryDtoPage?: SpringPage<Country>;
}

export interface ProvinceListResponse extends CommonApiFields {
  provinceDto?: Province;
  provinceDtoList?: Province[];
  provinceDtoPage?: SpringPage<Province>;
}

export interface DistrictListResponse extends CommonApiFields {
  districtDto?: District;
  districtDtoList?: District[];
  districtDtoPage?: SpringPage<District>;
}

export interface SuburbListResponse extends CommonApiFields {
  suburbDto?: Suburb;
  suburbDtoList?: Suburb[];
  suburbDtoPage?: SpringPage<Suburb>;
}

export interface AdministrativeLevelListResponse extends CommonApiFields {
  administrativeLevelDto?: AdministrativeLevel;
  administrativeLevelDtoList?: AdministrativeLevel[];
  administrativeLevelDtoPage?: SpringPage<AdministrativeLevel>;
}

export interface LocationNodeListResponse extends CommonApiFields {
  locationNodeDto?: LocationNode;
  locationNodeDtoList?: LocationNode[];
  locationNodeDtoPage?: SpringPage<LocationNode>;
}

export interface AddressListResponse extends CommonApiFields {
  addressDto?: Address;
  addressDtoList?: Address[];
  addressDtoPage?: SpringPage<Address>;
}

export interface LanguageListResponse extends CommonApiFields {
  languageDto?: Language;
  languageDtoList?: Language[];
  languageDtoPage?: SpringPage<Language>;
}

export interface LocalizedNameListResponse extends CommonApiFields {
  localizedNameDto?: LocalizedName;
  localizedNameDtoList?: LocalizedName[];
  localizedNameDtoPage?: SpringPage<LocalizedName>;
}

export interface ImportSummaryResponse {
  statusCode?: number;
  isSuccess?: boolean;
  message?: string;
  total?: number;
  /** Successful row count from import (backend `importedCount`). */
  importedCount?: number;
  /** @deprecated Prefer `importedCount`; older responses may still send this name */
  success?: number;
  failed?: number;
  errorMessages?: string[];
}

export type LocationEntityKind =
  | 'country'
  | 'province'
  | 'district'
  | 'city'
  | 'address'
  | 'suburb'
  | 'village'
  | 'admin-level'
  | 'language'
  | 'localized-name';
