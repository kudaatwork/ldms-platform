package projectlx.co.zw.organizationmanagement.business.logic.support;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import projectlx.co.zw.organizationmanagement.business.auditable.api.AgentServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.BranchServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.auditable.api.IndustryServiceAuditable;
import projectlx.co.zw.organizationmanagement.business.validation.api.OrganizationServiceValidator;
import projectlx.co.zw.organizationmanagement.model.Agent;
import projectlx.co.zw.organizationmanagement.model.AgentKind;
import projectlx.co.zw.organizationmanagement.model.Branch;
import projectlx.co.zw.organizationmanagement.model.Industry;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.repository.AgentRepository;
import projectlx.co.zw.organizationmanagement.repository.BranchRepository;
import projectlx.co.zw.organizationmanagement.repository.IndustryRepository;
import projectlx.co.zw.organizationmanagement.repository.OrganizationRepository;
import projectlx.co.zw.organizationmanagement.repository.specification.AgentSpecifications;
import projectlx.co.zw.organizationmanagement.repository.specification.BranchSpecifications;
import projectlx.co.zw.organizationmanagement.repository.specification.IndustrySpecifications;
import projectlx.co.zw.organizationmanagement.utils.dtos.AgentCsvDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.BranchCsvDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.ImportSummary;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryCsvDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryDto;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryMapping;
import projectlx.co.zw.organizationmanagement.utils.dtos.OrganizationMapping;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;
import projectlx.co.zw.organizationmanagement.utils.requests.AgentMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.BranchMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateAgentRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateIndustryRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.IndustryMultipleFiltersRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateAgentRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateBranchRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateIndustryRequest;
import projectlx.co.zw.organizationmanagement.utils.responses.OrganizationManagementResponse;
import projectlx.co.zw.shared_library.utils.dtos.AgentDto;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrganizationDirectoryAdminService {

    private final OrganizationRepository organizationRepository;
    private final BranchRepository branchRepository;
    private final AgentRepository agentRepository;
    private final IndustryRepository industryRepository;
    private final BranchServiceAuditable branchServiceAuditable;
    private final AgentServiceAuditable agentServiceAuditable;
    private final IndustryServiceAuditable industryServiceAuditable;
    private final OrganizationServiceValidator organizationServiceValidator;
    private final MessageService messageService;

    @Transactional
    public OrganizationResponse createBranch(CreateBranchRequest request, Locale locale, String username) {
        ValidatorDto v = organizationServiceValidator.validateCreateBranch(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return errors(v.getErrorMessages());
        }
        Organization org = loadOrganization(request.getOrganizationId(), locale);
        Branch branch = new Branch();
        branch.setOrganization(org);
        applyBranchFields(branch, request.getBranchName(), request.getBranchCode(), request.getLocationId(),
                request.getPhoneNumber(), request.getEmail(), request.getLatitude(), request.getLongitude(),
                request.isHeadOffice(), request.getRegion(), request.getBusinessHours(),
                request.getActive() == null || Boolean.TRUE.equals(request.getActive()));
        branch.setEntityStatus(EntityStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        branch.setCreatedAt(now);
        branch.setCreatedBy(username);
        Branch saved = branchServiceAuditable.save(branch);
        return branchSuccess(saved, I18Code.BRANCH_CREATED, 201, locale);
    }

    @Transactional
    public OrganizationResponse updateBranch(Long id, UpdateBranchRequest request, Locale locale, String username) {
        ValidatorDto idV = organizationServiceValidator.validateBranchId(id, locale);
        if (Boolean.FALSE.equals(idV.getSuccess())) {
            return errors(idV.getErrorMessages());
        }
        ValidatorDto v = organizationServiceValidator.validateUpdateBranch(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return errors(v.getErrorMessages());
        }
        Branch branch = loadBranch(id, locale);
        if (request.getOrganizationId() != null) {
            branch.setOrganization(loadOrganization(request.getOrganizationId(), locale));
        }
        if (StringUtils.hasText(request.getBranchName())) {
            branch.setBranchName(request.getBranchName().trim());
        }
        if (request.getBranchCode() != null) {
            branch.setBranchCode(trimToNull(request.getBranchCode()));
        }
        if (request.getLocationId() != null) {
            branch.setLocationId(request.getLocationId());
        }
        if (request.getPhoneNumber() != null) {
            branch.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        }
        if (request.getEmail() != null) {
            branch.setEmail(trimToNull(request.getEmail()));
        }
        if (request.getLatitude() != null) {
            branch.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            branch.setLongitude(request.getLongitude());
        }
        if (request.getHeadOffice() != null) {
            branch.setHeadOffice(request.getHeadOffice());
        }
        if (request.getRegion() != null) {
            branch.setRegion(trimToNull(request.getRegion()));
        }
        if (request.getBusinessHours() != null) {
            branch.setBusinessHours(trimToNull(request.getBusinessHours()));
        }
        if (request.getActive() != null) {
            branch.setActive(request.getActive());
        }
        branch.setModifiedAt(LocalDateTime.now());
        branch.setModifiedBy(username);
        Branch saved = branchServiceAuditable.save(branch);
        return branchSuccess(saved, I18Code.BRANCH_UPDATED, 200, locale);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getBranchById(Long id, Locale locale) {
        ValidatorDto v = organizationServiceValidator.validateBranchId(id, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return errors(v.getErrorMessages());
        }
        Branch branch = loadBranch(id, locale);
        OrganizationManagementResponse res = ok();
        res.setBranchDto(OrganizationMapping.toBranchDto(branch));
        res.setMessage(messageService.getMessage(I18Code.BRANCH_RETRIEVED.getCode(), new String[] {}, locale));
        return res;
    }

    @Transactional
    public OrganizationResponse deleteBranch(Long id, Locale locale, String username) {
        ValidatorDto v = organizationServiceValidator.validateBranchId(id, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return errors(v.getErrorMessages());
        }
        Branch branch = loadBranch(id, locale);
        branch.setEntityStatus(EntityStatus.DELETED);
        branch.setModifiedAt(LocalDateTime.now());
        branch.setModifiedBy(username);
        branchServiceAuditable.save(branch);
        OrganizationManagementResponse res = ok();
        res.setMessage(messageService.getMessage(I18Code.BRANCH_DELETED.getCode(), new String[] {}, locale));
        return res;
    }

    @Transactional
    public OrganizationResponse createAgent(CreateAgentRequest request, Locale locale, String username) {
        ValidatorDto v = organizationServiceValidator.validateCreateAgent(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return errors(v.getErrorMessages());
        }
        Organization org = loadOrganization(request.getOrganizationId(), locale);
        Agent agent = new Agent();
        agent.setOrganization(org);
        applyAgentFields(agent, request.getAgentKind(), request.getFirstName(), request.getLastName(),
                request.getEmail(), request.getPhoneNumber(), request.getAgentType(), request.getRole(),
                request.getBranchId(), request.getRepresentedOrganizationId(), request.getLocationId(),
                request.getAssignedRegion(), request.getActive() == null || Boolean.TRUE.equals(request.getActive()));
        agent.setEntityStatus(EntityStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        agent.setCreatedAt(now);
        agent.setCreatedBy(username);
        Agent saved = agentServiceAuditable.save(agent);
        return agentSuccess(saved, I18Code.AGENT_CREATED, 201, locale);
    }

    @Transactional
    public OrganizationResponse updateAgent(Long id, UpdateAgentRequest request, Locale locale, String username) {
        ValidatorDto idV = organizationServiceValidator.validateAgentId(id, locale);
        if (Boolean.FALSE.equals(idV.getSuccess())) {
            return errors(idV.getErrorMessages());
        }
        ValidatorDto v = organizationServiceValidator.validateUpdateAgent(request, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return errors(v.getErrorMessages());
        }
        Agent agent = loadAgent(id, locale);
        if (request.getOrganizationId() != null) {
            agent.setOrganization(loadOrganization(request.getOrganizationId(), locale));
        }
        if (StringUtils.hasText(request.getAgentKind())) {
            agent.setAgentKind(AgentKind.valueOf(request.getAgentKind().trim()));
        }
        if (request.getFirstName() != null) {
            agent.setFirstName(trimToNull(request.getFirstName()));
        }
        if (request.getLastName() != null) {
            agent.setLastName(trimToNull(request.getLastName()));
        }
        if (request.getEmail() != null) {
            agent.setEmail(trimToNull(request.getEmail()));
        }
        if (request.getPhoneNumber() != null) {
            agent.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        }
        if (request.getAgentType() != null) {
            agent.setAgentType(trimToNull(request.getAgentType()));
        }
        if (request.getRole() != null) {
            agent.setRole(trimToNull(request.getRole()));
        }
        if (request.getBranchId() != null) {
            agent.setBranch(request.getBranchId() > 0 ? loadBranch(request.getBranchId(), locale) : null);
        }
        if (request.getRepresentedOrganizationId() != null) {
            agent.setRepresentedOrganization(
                    request.getRepresentedOrganizationId() > 0
                            ? loadOrganization(request.getRepresentedOrganizationId(), locale)
                            : null);
        }
        if (request.getLocationId() != null) {
            agent.setLocationId(request.getLocationId());
        }
        if (request.getAssignedRegion() != null) {
            agent.setAssignedRegion(trimToNull(request.getAssignedRegion()));
        }
        if (request.getActive() != null) {
            agent.setActive(request.getActive());
        }
        agent.setModifiedAt(LocalDateTime.now());
        agent.setModifiedBy(username);
        Agent saved = agentServiceAuditable.save(agent);
        return agentSuccess(saved, I18Code.AGENT_UPDATED, 200, locale);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getAgentById(Long id, Locale locale) {
        ValidatorDto v = organizationServiceValidator.validateAgentId(id, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return errors(v.getErrorMessages());
        }
        Agent agent = loadAgent(id, locale);
        OrganizationManagementResponse res = ok();
        res.setAgentDto(OrganizationMapping.toAgentDto(agent));
        res.setMessage(messageService.getMessage(I18Code.AGENT_RETRIEVED.getCode(), new String[] {}, locale));
        return res;
    }

    @Transactional
    public OrganizationResponse deleteAgent(Long id, Locale locale, String username) {
        ValidatorDto v = organizationServiceValidator.validateAgentId(id, locale);
        if (Boolean.FALSE.equals(v.getSuccess())) {
            return errors(v.getErrorMessages());
        }
        Agent agent = loadAgent(id, locale);
        agent.setEntityStatus(EntityStatus.DELETED);
        agent.setModifiedAt(LocalDateTime.now());
        agent.setModifiedBy(username);
        agentServiceAuditable.save(agent);
        OrganizationManagementResponse res = ok();
        res.setMessage(messageService.getMessage(I18Code.AGENT_DELETED.getCode(), new String[] {}, locale));
        return res;
    }

    @Transactional(readOnly = true)
    public List<BranchDto> listBranchesForExport(BranchMultipleFiltersRequest filters, Locale locale) {
        filters.setPage(0);
        filters.setSize(Integer.MAX_VALUE);
        return findBranchDtos(filters);
    }

    @Transactional(readOnly = true)
    public List<AgentDto> listAgentsForExport(AgentMultipleFiltersRequest filters, Locale locale) {
        filters.setPage(0);
        filters.setSize(Integer.MAX_VALUE);
        return findAgentDtos(filters);
    }

    @Transactional(readOnly = true)
    public List<IndustryDto> listIndustriesForExport(IndustryMultipleFiltersRequest filters, Locale locale) {
        filters.setPage(0);
        filters.setSize(Integer.MAX_VALUE);
        var spec = IndustrySpecifications.notDeleted();
        if (StringUtils.hasText(filters.getName())) {
            spec = spec.and(IndustrySpecifications.nameLike(filters.getName()));
        }
        if (StringUtils.hasText(filters.getIndustryCode())) {
            spec = spec.and(IndustrySpecifications.industryCodeLike(filters.getIndustryCode()));
        }
        if (StringUtils.hasText(filters.getSearchValue())) {
            spec = spec.and(IndustrySpecifications.searchValueLike(filters.getSearchValue()));
        }
        return industryRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(IndustryMapping::toDto)
                .toList();
    }

    @Transactional
    public ImportSummary importBranchesFromCsv(InputStream inputStream, Locale locale, String username) throws IOException {
        List<BranchCsvDto> rows = parseCsv(inputStream, BranchCsvDto.class);
        int imported = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            BranchCsvDto row = rows.get(i);
            try {
                CreateBranchRequest req = new CreateBranchRequest();
                req.setOrganizationId(Long.parseLong(row.getOrganizationId().trim()));
                req.setBranchName(row.getBranchName());
                req.setBranchCode(row.getBranchCode());
                req.setRegion(row.getRegion());
                req.setEmail(row.getEmail());
                req.setPhoneNumber(row.getPhoneNumber());
                req.setBusinessHours(row.getBusinessHours());
                if (StringUtils.hasText(row.getLocationId())) {
                    req.setLocationId(Long.parseLong(row.getLocationId().trim()));
                }
                req.setHeadOffice(parseBoolean(row.getHeadOffice()));
                req.setActive(parseBoolean(row.getActive()));
                OrganizationResponse resp = createBranch(req, locale, username);
                if (resp.isSuccess()) {
                    imported++;
                } else {
                    failed++;
                    errors.add("Row " + (i + 2) + ": " + String.join(" ", resp.getErrorMessages()));
                }
            } catch (RuntimeException ex) {
                failed++;
                errors.add("Row " + (i + 2) + ": " + ex.getMessage());
            }
        }
        return buildImportSummary(rows.size(), imported, failed, errors, locale, "branches");
    }

    @Transactional
    public ImportSummary importAgentsFromCsv(InputStream inputStream, Locale locale, String username) throws IOException {
        List<AgentCsvDto> rows = parseCsv(inputStream, AgentCsvDto.class);
        int imported = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            AgentCsvDto row = rows.get(i);
            try {
                CreateAgentRequest req = new CreateAgentRequest();
                req.setOrganizationId(Long.parseLong(row.getOrganizationId().trim()));
                req.setAgentKind(row.getAgentKind().trim().toUpperCase());
                req.setFirstName(row.getFirstName());
                req.setLastName(row.getLastName());
                req.setEmail(row.getEmail());
                req.setPhoneNumber(row.getPhoneNumber());
                req.setAgentType(row.getAgentType());
                req.setRole(row.getRole());
                if (StringUtils.hasText(row.getBranchId())) {
                    req.setBranchId(Long.parseLong(row.getBranchId().trim()));
                }
                req.setActive(parseBoolean(row.getActive()));
                OrganizationResponse resp = createAgent(req, locale, username);
                if (resp.isSuccess()) {
                    imported++;
                } else {
                    failed++;
                    errors.add("Row " + (i + 2) + ": " + String.join(" ", resp.getErrorMessages()));
                }
            } catch (RuntimeException ex) {
                failed++;
                errors.add("Row " + (i + 2) + ": " + ex.getMessage());
            }
        }
        return buildImportSummary(rows.size(), imported, failed, errors, locale, "agents");
    }

    @Transactional
    public ImportSummary importIndustriesFromCsv(InputStream inputStream, Locale locale, String username) throws IOException {
        List<IndustryCsvDto> rows = parseCsv(inputStream, IndustryCsvDto.class);
        int imported = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            IndustryCsvDto row = rows.get(i);
            try {
                CreateIndustryRequest req = new CreateIndustryRequest();
                req.setName(row.getName());
                req.setIndustryCode(row.getIndustryCode());
                req.setDescription(row.getDescription());
                req.setRegulatoryBodyName(row.getRegulatoryBodyName());
                req.setRegulatoryBodyContactInfo(row.getRegulatoryBodyContactInfo());
                req.setComplianceRequirements(row.getComplianceRequirements());
                req.setActive(parseBoolean(row.getActive()));
                if (!StringUtils.hasText(req.getName())) {
                    failed++;
                    errors.add("Row " + (i + 2) + ": name is required");
                    continue;
                }
                if (industryRepository.findByNameIgnoreCaseAndEntityStatusNot(req.getName().trim(), EntityStatus.DELETED).isPresent()) {
                    failed++;
                    errors.add("Row " + (i + 2) + ": industry name already exists");
                    continue;
                }
                Industry industry = new Industry();
                IndustryMapping.applyCreate(req, industry);
                industry.setEntityStatus(EntityStatus.ACTIVE);
                LocalDateTime now = LocalDateTime.now();
                industry.setCreatedAt(now);
                industry.setCreatedBy(username);
                industryServiceAuditable.save(industry);
                imported++;
            } catch (RuntimeException ex) {
                failed++;
                errors.add("Row " + (i + 2) + ": " + ex.getMessage());
            }
        }
        return buildImportSummary(rows.size(), imported, failed, errors, locale, "industries");
    }

    private List<BranchDto> findBranchDtos(BranchMultipleFiltersRequest request) {
        var spec = buildBranchFilterSpec(request);
        Sort sort = Sort.by(Sort.Direction.ASC, "branchName");
        if (isExportSize(request.getSize())) {
            return branchRepository.findAll(spec, sort).stream()
                    .map(OrganizationMapping::toBranchDto)
                    .toList();
        }
        Page<Branch> page = branchRepository.findAll(spec, PageRequest.of(request.getPage(), request.getSize(), sort));
        return page.map(OrganizationMapping::toBranchDto).getContent();
    }

    private List<AgentDto> findAgentDtos(AgentMultipleFiltersRequest request) {
        var spec = buildAgentFilterSpec(request);
        Sort sort = Sort.by(Sort.Direction.ASC, "lastName", "firstName");
        if (isExportSize(request.getSize())) {
            return agentRepository.findAll(spec, sort).stream()
                    .map(OrganizationMapping::toAgentDto)
                    .toList();
        }
        Page<Agent> page = agentRepository.findAll(spec, PageRequest.of(request.getPage(), request.getSize(), sort));
        return page.map(OrganizationMapping::toAgentDto).getContent();
    }

    private static boolean isExportSize(int size) {
        return size <= 0 || size >= 100_000;
    }

    private static Specification<Branch> buildBranchFilterSpec(BranchMultipleFiltersRequest request) {
        var spec = BranchSpecifications.notDeleted();
        if (StringUtils.hasText(request.getBranchName())) {
            spec = spec.and(BranchSpecifications.branchNameLike(request.getBranchName()));
        }
        if (request.getOrganizationId() != null) {
            spec = spec.and(BranchSpecifications.organizationIdEquals(request.getOrganizationId()));
        }
        if (StringUtils.hasText(request.getSearchValue())) {
            spec = spec.and(BranchSpecifications.searchValueLike(request.getSearchValue()));
        }
        return spec;
    }

    private static Specification<Agent> buildAgentFilterSpec(AgentMultipleFiltersRequest request) {
        var spec = AgentSpecifications.notDeleted();
        if (request.getOrganizationId() != null) {
            spec = spec.and(AgentSpecifications.organizationIdEquals(request.getOrganizationId()));
        }
        if (StringUtils.hasText(request.getAgentKind())) {
            spec = spec.and(AgentSpecifications.agentKindEquals(AgentKind.valueOf(request.getAgentKind().trim())));
        }
        if (StringUtils.hasText(request.getSearchValue())) {
            spec = spec.and(AgentSpecifications.searchValueLike(request.getSearchValue()));
        }
        return spec;
    }

    private void applyBranchFields(Branch branch, String branchName, String branchCode, Long locationId,
            String phone, String email, java.math.BigDecimal lat, java.math.BigDecimal lng,
            boolean headOffice, String region, String businessHours, boolean active) {
        branch.setBranchName(branchName.trim());
        branch.setBranchCode(trimToNull(branchCode));
        branch.setLocationId(locationId);
        branch.setPhoneNumber(trimToNull(phone));
        branch.setEmail(trimToNull(email));
        branch.setLatitude(lat);
        branch.setLongitude(lng);
        branch.setHeadOffice(headOffice);
        branch.setRegion(trimToNull(region));
        branch.setBusinessHours(trimToNull(businessHours));
        branch.setActive(active);
    }

    private void applyAgentFields(Agent agent, String agentKind, String firstName, String lastName,
            String email, String phone, String agentType, String role, Long branchId,
            Long representedOrgId, Long locationId, String assignedRegion, boolean active) {
        agent.setAgentKind(AgentKind.valueOf(agentKind.trim().toUpperCase()));
        agent.setFirstName(trimToNull(firstName));
        agent.setLastName(trimToNull(lastName));
        agent.setEmail(trimToNull(email));
        agent.setPhoneNumber(trimToNull(phone));
        agent.setAgentType(trimToNull(agentType));
        agent.setRole(trimToNull(role));
        if (branchId != null && branchId > 0) {
            agent.setBranch(branchRepository.findById(branchId).orElse(null));
        }
        if (representedOrgId != null && representedOrgId > 0) {
            agent.setRepresentedOrganization(organizationRepository.findById(representedOrgId).orElse(null));
        }
        agent.setLocationId(locationId);
        agent.setAssignedRegion(trimToNull(assignedRegion));
        agent.setActive(active);
    }

    private Organization loadOrganization(Long id, Locale locale) {
        return organizationRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getMessage(I18Code.ORG_NOT_FOUND.getCode(), new String[] {}, locale)));
    }

    private Branch loadBranch(Long id, Locale locale) {
        return branchRepository.findById(id)
                .filter(b -> b.getEntityStatus() != EntityStatus.DELETED)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getMessage(I18Code.BRANCH_NOT_FOUND.getCode(), new String[] {}, locale)));
    }

    private Agent loadAgent(Long id, Locale locale) {
        return agentRepository.findById(id)
                .filter(a -> a.getEntityStatus() != EntityStatus.DELETED)
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.getMessage(I18Code.AGENT_NOT_FOUND.getCode(), new String[] {}, locale)));
    }

    private OrganizationResponse branchSuccess(Branch saved, I18Code code, int status, Locale locale) {
        OrganizationManagementResponse res = ok();
        res.setStatusCode(status);
        res.setBranchDto(OrganizationMapping.toBranchDto(saved));
        res.setMessage(messageService.getMessage(code.getCode(), new String[] {}, locale));
        return res;
    }

    private OrganizationResponse agentSuccess(Agent saved, I18Code code, int status, Locale locale) {
        OrganizationManagementResponse res = ok();
        res.setStatusCode(status);
        res.setAgentDto(OrganizationMapping.toAgentDto(saved));
        res.setMessage(messageService.getMessage(code.getCode(), new String[] {}, locale));
        return res;
    }

    private OrganizationManagementResponse ok() {
        OrganizationManagementResponse res = new OrganizationManagementResponse();
        res.setSuccess(true);
        res.setStatusCode(200);
        return res;
    }

    private OrganizationResponse errors(List<String> errors) {
        OrganizationResponse r = new OrganizationResponse();
        r.setSuccess(false);
        r.setStatusCode(400);
        r.setErrorMessages(errors);
        return r;
    }

    private ImportSummary buildImportSummary(int total, int imported, int failed, List<String> errors,
            Locale locale, String entityLabel) {
        boolean success = failed == 0;
        String message = success
                ? "Imported " + imported + " " + entityLabel + " successfully."
                : "Imported " + imported + " of " + total + " " + entityLabel + "; " + failed + " failed.";
        return new ImportSummary(success ? 200 : 400, success, message, total, imported, failed, errors);
    }

    private <T> List<T> parseCsv(InputStream inputStream, Class<T> type) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(type);
            CsvToBean<T> csv = new CsvToBeanBuilder<T>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            return csv.parse();
        }
    }

    private boolean parseBoolean(String raw) {
        if (!StringUtils.hasText(raw)) {
            return true;
        }
        return !"false".equalsIgnoreCase(raw.trim()) && !"0".equals(raw.trim()) && !"no".equalsIgnoreCase(raw.trim());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
