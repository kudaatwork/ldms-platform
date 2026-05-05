package projectlx.co.zw.locationsmanagementservice.business.logic.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import projectlx.co.zw.locationsmanagementservice.business.auditable.api.AddressServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.AdministrativeLevelServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.CityServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.DistrictServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.LocationNodeServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.ProvinceServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.SuburbServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.VillageServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.Address;
import projectlx.co.zw.locationsmanagementservice.model.AdministrativeLevel;
import projectlx.co.zw.locationsmanagementservice.model.City;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.LocationNode;
import projectlx.co.zw.locationsmanagementservice.model.Province;
import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.locationsmanagementservice.model.Village;
import projectlx.co.zw.locationsmanagementservice.repository.AddressRepository;
import projectlx.co.zw.locationsmanagementservice.repository.AdministrativeLevelRepository;
import projectlx.co.zw.locationsmanagementservice.repository.CityRepository;
import projectlx.co.zw.locationsmanagementservice.repository.DistrictRepository;
import projectlx.co.zw.locationsmanagementservice.repository.LocationNodeRepository;
import projectlx.co.zw.locationsmanagementservice.repository.ProvinceRepository;
import projectlx.co.zw.locationsmanagementservice.repository.SuburbRepository;
import projectlx.co.zw.locationsmanagementservice.repository.VillageRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

/**
 * Marks active descendants deleted before a geography parent so the hierarchy behaves as a consistent tree after soft-delete.
 */
@Service
@RequiredArgsConstructor
public class LocationHierarchyCascadeSoftDeleteService {

    private static final EntityStatus DEL = EntityStatus.DELETED;

    private final AdministrativeLevelRepository administrativeLevelRepository;
    private final AdministrativeLevelServiceAuditable administrativeLevelServiceAuditable;
    private final ProvinceRepository provinceRepository;
    private final ProvinceServiceAuditable provinceServiceAuditable;
    private final DistrictRepository districtRepository;
    private final DistrictServiceAuditable districtServiceAuditable;
    private final SuburbRepository suburbRepository;
    private final SuburbServiceAuditable suburbServiceAuditable;
    private final CityRepository cityRepository;
    private final CityServiceAuditable cityServiceAuditable;
    private final VillageRepository villageRepository;
    private final VillageServiceAuditable villageServiceAuditable;
    private final LocationNodeRepository locationNodeRepository;
    private final LocationNodeServiceAuditable locationNodeServiceAuditable;
    private final AddressRepository addressRepository;
    private final AddressServiceAuditable addressServiceAuditable;

    public void cascadeBeforeDeletingCountry(Long countryId, Locale locale, String username) {
        List<Province> provinces = provinceRepository.findAllByCountry_IdAndEntityStatusNot(countryId, DEL);
        for (Province p : provinces) {
            cascadeBeforeDeletingProvince(p.getId(), locale, username);
            p.setEntityStatus(DEL);
            provinceServiceAuditable.delete(p, locale);
        }

        List<AdministrativeLevel> levels =
                administrativeLevelRepository.findAllByCountry_IdAndEntityStatusNot(countryId, DEL);
        for (AdministrativeLevel al : levels) {
            al.setEntityStatus(DEL);
            administrativeLevelServiceAuditable.delete(al, locale);
        }
    }

    public void cascadeBeforeDeletingProvince(Long provinceId, Locale locale, String username) {
        List<District> districts = districtRepository.findAllByProvince_IdAndEntityStatusNot(provinceId, DEL);
        for (District d : districts) {
            cascadeBeforeDeletingDistrict(d.getId(), locale, username);
            d.setEntityStatus(DEL);
            districtServiceAuditable.delete(d, locale);
        }
    }

    public void cascadeBeforeDeletingDistrict(Long districtId, Locale locale, String username) {
        List<Suburb> suburbs = suburbRepository.findAllByDistrict_IdAndEntityStatusNot(districtId, DEL);
        for (Suburb s : suburbs) {
            cascadeBeforeDeletingSuburb(s.getId(), locale, username);
            s.setEntityStatus(DEL);
            suburbServiceAuditable.delete(s, locale, username);
        }
        List<City> cities = cityRepository.findAllByDistrict_IdAndEntityStatusNot(districtId, DEL);
        for (City c : cities) {
            cascadeBeforeDeletingCity(c.getId(), locale);
            c.setEntityStatus(DEL);
            cityServiceAuditable.delete(c, locale);
        }
        purgeDistrictDirectLocationForest(districtId, locale, username);
    }

    public void cascadeBeforeDeletingSuburb(Long suburbId, Locale locale, String username) {
        for (Address a : addressRepository.findAllBySuburb_IdAndEntityStatusNot(suburbId, DEL)) {
            softDeleteAddress(a, locale);
        }
        List<Village> villages = villageRepository.findAllBySuburb_IdAndEntityStatusNot(suburbId, DEL);
        for (Village v : villages) {
            purgeAddressesForVillage(v.getId(), locale);
            softDeleteVillageEntity(v, locale);
        }

        softDeleteLocationNodeForest(
                locationNodeRepository.findAllBySuburb_IdAndEntityStatusNot(suburbId, DEL), locale, username);
    }

    public void cascadeBeforeDeletingCity(Long cityId, Locale locale) {
        List<Village> villages = villageRepository.findAllByCity_IdAndEntityStatusNot(cityId, DEL);
        for (Village v : villages) {
            purgeAddressesForVillage(v.getId(), locale);
            softDeleteVillageEntity(v, locale);
        }
        for (Address a : addressRepository.findAllByCity_IdAndEntityStatusNot(cityId, DEL)) {
            softDeleteAddress(a, locale);
        }
    }

    /** Clears addresses linked to the village — caller soft-deletes the village row afterward. */
    public void purgeAddressesLinkedToVillage(Long villageId, Locale locale) {
        purgeAddressesForVillage(villageId, locale);
    }

    /**
     * Recursively soft-deletes every active descendant ({@link LocationNode#getParent()}) leaving {@code rootId} for the caller to delete.
     */
    public void cascadeBeforeDeletingLocationNode(Long rootId, Locale locale, String username) {
        List<LocationNode> directChildren =
                new ArrayList<>(locationNodeRepository.findByParentIdAndEntityStatusNot(rootId, DEL));
        for (LocationNode child : directChildren) {
            cascadeBeforeDeletingLocationNode(child.getId(), locale, username);
            locationNodeRepository.findByIdAndEntityStatusNot(child.getId(), DEL)
                    .ifPresent(n -> softDeleteLocationNode(n, locale, username));
        }
    }

    private void purgeDistrictDirectLocationForest(Long districtId, Locale locale, String username) {
        List<LocationNode> nodes =
                locationNodeRepository.findAllByDistrict_IdAndSuburbIsNullAndEntityStatusNot(districtId, DEL);
        softDeleteLocationNodeForest(nodes, locale, username);
    }

    /**
     * Soft-delete a coherent forest encoded in {@code subgraph}: each node's parent appears in subgraph or is absent (root).
     */
    private void softDeleteLocationNodeForest(List<LocationNode> subgraph, Locale locale, String username) {
        if (subgraph == null || subgraph.isEmpty()) {
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (LocationNode n : subgraph) {
            ids.add(n.getId());
        }
        Map<Long, List<Long>> adj = new HashMap<>(16);
        for (LocationNode n : subgraph) {
            Long pid = null;
            if (n.getParent() != null) {
                pid = n.getParent().getId();
            }
            Long parentWithin = (pid != null && ids.contains(pid)) ? pid : null;
            if (parentWithin == null) {
                adj.computeIfAbsent(null, k -> new ArrayList<>()).add(n.getId());
            } else {
                adj.computeIfAbsent(parentWithin, k -> new ArrayList<>()).add(n.getId());
            }
        }
        List<Long> roots = Objects.requireNonNullElse(adj.remove(null), List.of());
        for (Long rid : roots) {
            subtreePostOrderFromParentMap(rid, adj, locale, username);
        }
    }

    private void subtreePostOrderFromParentMap(
            Long rootId,
            Map<Long, List<Long>> adj,
            Locale locale,
            String username) {
        List<Long> orderRootLast = new ArrayList<>();
        buildPostOrderRootLast(rootId, adj, orderRootLast);
        for (long id : orderRootLast) {
            locationNodeRepository.findByIdAndEntityStatusNot(id, DEL)
                    .ifPresent(n -> softDeleteLocationNode(n, locale, username));
        }
    }

    private static void buildPostOrderRootLast(
            Long id,
            Map<Long, List<Long>> adjByParentActive,
            List<Long> accumulator) {

        List<Long> ch = adjByParentActive.getOrDefault(id, List.of());
        for (Long c : ch) {
            buildPostOrderRootLast(c, adjByParentActive, accumulator);
        }
        accumulator.add(id);
    }

    private void purgeAddressesForVillage(Long villageId, Locale locale) {
        for (Address a : addressRepository.findAllByVillage_IdAndEntityStatusNot(villageId, DEL)) {
            softDeleteAddress(a, locale);
        }
    }

    private void softDeleteVillageEntity(Village entity, Locale locale) {
        entity.setEntityStatus(DEL);
        villageServiceAuditable.delete(entity, locale);
    }

    private void softDeleteLocationNode(LocationNode node, Locale locale, String username) {
        node.setEntityStatus(DEL);
        locationNodeServiceAuditable.delete(node, locale, username);
    }

    private void softDeleteAddress(Address a, Locale locale) {
        a.setEntityStatus(DEL);
        addressServiceAuditable.delete(a, locale);
    }
}
