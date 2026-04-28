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
  administrativeLevelId?: number | null;
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
  administrativeLevelId?: number | null;
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
  geoCoordinatesId?: number | null;
  postalCode?: string | null;
  administrativeLevelId?: number | null;
  localizedNameIds?: number[] | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  entityStatus?: EntityStatus | null;
}

export interface AdministrativeLevel {
  id: number;
  name: string;
  code?: string | null;
  level?: number | null;
  description?: string | null;
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

export interface ImportSummaryResponse {
  statusCode?: number;
  isSuccess?: boolean;
  message?: string;
  total?: number;
  success?: number;
  failed?: number;
  errorMessages?: string[];
}

export type LocationEntityKind =
  | 'country'
  | 'province'
  | 'district'
  | 'city'
  | 'suburb'
  | 'village'
  | 'admin-level';
