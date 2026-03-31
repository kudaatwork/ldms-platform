package projectlx.user.management.service.business.logic.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.auditable.api.UserGroupServiceAuditable;
import projectlx.user.management.service.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.service.business.logic.api.UserGroupService;
import projectlx.user.management.service.business.validator.api.UserGroupServiceValidator;
import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.User;
import projectlx.user.management.service.model.UserGroup;
import projectlx.user.management.service.model.UserRole;
import projectlx.user.management.service.repository.UserGroupRepository;
import projectlx.user.management.service.repository.UserRepository;
import projectlx.user.management.service.repository.UserRoleRepository;
import projectlx.user.management.service.repository.specification.UserGroupSpecification;
import projectlx.user.management.service.utils.dtos.UserGroupDto;
import projectlx.user.management.service.utils.dtos.UserRoleDto;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.AddUserToUserGroupRequest;
import projectlx.user.management.service.utils.requests.AssignUserRoleToUserGroupRequest;
import projectlx.user.management.service.utils.requests.CreateUserGroupRequest;
import projectlx.user.management.service.utils.requests.EditUserGroupRequest;
import projectlx.user.management.service.utils.requests.RemoveUserRolesFromUserGroupRequest;
import projectlx.user.management.service.utils.requests.UserGroupMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserGroupResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import projectlx.user.management.service.utils.dtos.ImportSummary;

@RequiredArgsConstructor
public class UserGroupServiceImpl implements UserGroupService {

    private final UserGroupServiceValidator userGroupServiceValidator;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final UserGroupServiceAuditable userGroupServiceAuditable;
    private final UserRoleRepository userRoleRepository;
    private final UserServiceAuditable userServiceAuditable;

    private static final String[] HEADERS = {
            "ID", "NAME", "DESCRIPTION", "USER ROLES"
    };

    private static final String[] CSV_HEADERS = {
            "NAME", "DESCRIPTION"
    };

    @Override
    public UserGroupResponse create(CreateUserGroupRequest createUserGroupRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isCreateUserGroupRequestValid(createUserGroupRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_GROUP_INVALID_REQUEST.getCode(), new String[]{},
                        locale);

            return buildUserGroupResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<UserGroup> userGroupRetrieved =
                    userGroupRepository.findByNameAndEntityStatusNot(createUserGroupRequest.getName(), EntityStatus.DELETED);

        if (userGroupRetrieved.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_ALREADY_EXISTS.getCode(), new String[]{},
                        locale);

            return buildUserGroupResponse(400, false, message, null,
                        null, null);
        }

        createUserGroupRequest.setName(createUserGroupRequest.getName().toUpperCase());
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserGroup userGroupToBeSaved = modelMapper.map(createUserGroupRequest, UserGroup.class);

        UserGroup userGroupSaved = userGroupServiceAuditable.create(userGroupToBeSaved, locale, username);

        UserGroupDto useGroupDtoReturned = modelMapper.map(userGroupSaved, UserGroupDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                    locale);

        return buildUserGroupResponse(201, true, message, useGroupDtoReturned, null,
                    null);
    }

    @Override
    public UserGroupResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Optional<UserGroup> userGroupRetrieved = userGroupRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userGroupRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(404, false, message, null, null,
                    null);
        }

        UserGroup userGroupReturned = userGroupRetrieved.get();
        UserGroupDto userGroupDto = modelMapper.map(userGroupReturned, UserGroupDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserGroupResponse(200, true, message, userGroupDto, null,
                null);
    }

    @Override
    public UserGroupResponse findAllAsList(String username, Locale locale) {

        String message = "";

        List<UserGroup> userGroupList = userGroupRepository.findByEntityStatusNot(EntityStatus.DELETED);

        if(userGroupList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildUserGroupResponse(404, false, message, null,
                    null, null);
        }

        List<UserGroupDto> userGroupDtoList = modelMapper.map(userGroupList, new TypeToken<List<UserGroupDto>>(){}.getType());

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserGroupResponse(200, true, message, null, userGroupDtoList,
                null);
    }

    @Override
    public UserGroupResponse update(EditUserGroupRequest editUserGroupRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isRequestValidForEditing(editUserGroupRequest, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_GROUP_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Optional<UserGroup> userGroupRetrieved = userGroupRepository.findByIdAndEntityStatusNot(editUserGroupRequest.getId(),
                EntityStatus.DELETED);

        if (userGroupRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        UserGroup userGroupToBeEdited = userGroupRetrieved.get();

        UserGroup userGroupEdited = userGroupServiceAuditable.update(userGroupToBeEdited, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserGroupDto userGroupDtoReturned = modelMapper.map(userGroupEdited, UserGroupDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserGroupResponse(201, true, message, userGroupDtoReturned, null,
                null);
    }

    @Override
    public UserGroupResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Optional<UserGroup> userGroupRetrieved = userGroupRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userGroupRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(404, false, message, null, null,
                    null);
        }

        UserGroup userGroupToBeDeleted = userGroupRetrieved.get();
        userGroupToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        UserGroup userGroupDeleted = userGroupServiceAuditable.delete(userGroupToBeDeleted, locale, username);

        UserGroupDto useGroupDtoReturned = modelMapper.map(userGroupDeleted, UserGroupDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserGroupResponse(200, true, message, useGroupDtoReturned, null,
                null);
    }

    @Override
    public UserGroupResponse findByMultipleFilters(UserGroupMultipleFiltersRequest userGroupMultipleFiltersRequest, String username, Locale locale) {

        String message = "";

        Specification<UserGroup> spec = null;
        spec = addToSpec(spec, UserGroupSpecification::deleted);

        ValidatorDto validatorDto = userGroupServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(
                userGroupMultipleFiltersRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Pageable pageable = PageRequest.of(userGroupMultipleFiltersRequest.getPage(),
                userGroupMultipleFiltersRequest.getSize());

        ValidatorDto nameValidatorDto = userGroupServiceValidator.isStringValid(
                userGroupMultipleFiltersRequest.getName(), locale);

        if (nameValidatorDto.getSuccess()) {

            spec = addToSpec(userGroupMultipleFiltersRequest.getName(), spec,
                    UserGroupSpecification::nameLike);
        }

        ValidatorDto descriptionValidatorDto = userGroupServiceValidator.isStringValid(
                userGroupMultipleFiltersRequest.getDescription(), locale);

        if (descriptionValidatorDto.getSuccess()) {

            spec = addToSpec(userGroupMultipleFiltersRequest.getDescription(), spec,
                    UserGroupSpecification::descriptionLike);
        }

        ValidatorDto searchValueValidatorDto = userGroupServiceValidator.isStringValid(
                userGroupMultipleFiltersRequest.getSearchValue(), locale);

        if (searchValueValidatorDto.getSuccess()) {

            spec = addToSpec(userGroupMultipleFiltersRequest.getSearchValue(), spec, UserGroupSpecification::any);
        }

        Page<UserGroup> result = userGroupRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponse(404, false, message,null, null,
                    null);
        }

        Page<UserGroupDto> userGroupDtoPage = convertUserGroupEntityToUserGroupDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserGroupResponse(200, true, message,null, null,
                userGroupDtoPage);
    }

    @Override
    public UserGroupResponse assignUserRoleToUserGroup(AssignUserRoleToUserGroupRequest assignUserRoleToUserGroupRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(assignUserRoleToUserGroupRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVALID_ASSIGN_USER_ROLE_TO_USER_GROUP_REQUEST.getCode(),
                    new String[]{}, locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Optional<UserGroup> userGroup =
                userGroupRepository.findByIdAndEntityStatusNot(assignUserRoleToUserGroupRequest.getUserGroupId(),
                        EntityStatus.DELETED);

        if (userGroup.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        UserGroup userGroupToBeUpdated = userGroup.get();

        Set<UserRole> userRoleSet = userRoleRepository.findByIdInAndEntityStatusNot(
                assignUserRoleToUserGroupRequest.getUserRoleIds(), EntityStatus.DELETED);

        if (userRoleSet.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        if (userGroupToBeUpdated.getUserRoles().containsAll(userRoleSet)) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLES_ALREADY_ASSIGNED.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        userRoleSet.addAll(userGroupToBeUpdated.getUserRoles());
        userGroupToBeUpdated.setUserRoles(userRoleSet);

        UserGroup userGroupUpdated = userGroupServiceAuditable.update(userGroupToBeUpdated, locale, username);

        UserGroupDto userGroupDto = modelMapper.map(userGroupUpdated, UserGroupDto.class);

        List<UserRoleDto> userRoleDtoList = modelMapper.map(userGroupUpdated.getUserRoles(),
                new TypeToken<List<UserRoleDto>>() {}.getType());

        userGroupDto.setUserRoleDtoSet(userRoleDtoList);

        message = messageService.getMessage(
                I18Code.MESSAGE_USER_ROLE_ASSIGNED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserGroupResponse(201, true, message, userGroupDto, null,
                null);
    }

    @Override
    public UserGroupResponse removeUserRolesFromUserGroup(RemoveUserRolesFromUserGroupRequest removeUserRolesFromUserGroupRequest, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVALID_REMOVE_USER_ROLES_FROM_USER_GROUP_REQUEST.getCode(),
                    new String[]{}, locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Optional<UserGroup> userGroup =
                userGroupRepository.findByIdAndEntityStatusNot(removeUserRolesFromUserGroupRequest.getUserGroupId(),
                        EntityStatus.DELETED);

        if (userGroup.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        UserGroup userGroupToBeUpdated = userGroup.get();

        Set<UserRole> userRoleSet = userRoleRepository.findByIdInAndEntityStatusNot(
                removeUserRolesFromUserGroupRequest.getUserRoleIds(), EntityStatus.DELETED);

        if (userRoleSet.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        Set<UserRole> remainingUserRoles = userGroupToBeUpdated.getUserRoles();

        boolean removed = remainingUserRoles.removeAll(userRoleSet);

        if(!removed) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_NOT_ASSIGNED.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        userGroupToBeUpdated.setUserRoles(remainingUserRoles);

        UserGroup userGroupReturned = userGroupServiceAuditable.update(userGroupToBeUpdated, locale, username);

        UserGroupDto userGroupDto = modelMapper.map(userGroupReturned, UserGroupDto.class);

        List<UserRoleDto> userRoleDtoList = modelMapper.map(userGroupReturned.getUserRoles(),
                new TypeToken<List<UserRoleDto>>() {}.getType());

        userGroupDto.setUserRoleDtoSet(userRoleDtoList);

        message = messageService.getMessage(I18Code.MESSAGE_USER_ROLES_REMOVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserGroupResponse(201, true, message, userGroupDto,
                null, null);
    }

    @Override
    public UserGroupResponse addUserGroupToUser(AddUserToUserGroupRequest addUserToUserGroupRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isRequestValidToAddUserToUserGroup(addUserToUserGroupRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVALID_ADD_USER_GROUP_TO_USER_REQUEST.getCode(),
                    new String[]{}, locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Optional<UserGroup> userGroup =
                userGroupRepository.findByIdAndEntityStatusNot(addUserToUserGroupRequest.getUserGroupId(),
                        EntityStatus.DELETED);

        if (userGroup.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        Optional<User> userRetrieved = userRepository.findByIdAndEntityStatusNot(
                addUserToUserGroupRequest.getUserId(), EntityStatus.DELETED);

        if (userRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        User userToBeUpdated = userRetrieved.get();

        userToBeUpdated.setUserGroup(userGroup.get());

        User userUpdated = userServiceAuditable.update(userToBeUpdated, locale, username);

        UserGroupDto userGroupDto = modelMapper.map(userGroup.get(), UserGroupDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_UPDATED_SUCCESSFULLY.getCode(),
                new String[]{userUpdated.getUsername()}, locale);

        return buildUserGroupResponse(200, true, message, userGroupDto, null,
                null);
    }

    private Specification<UserGroup> addToSpec(Specification<UserGroup> spec,
                                                 Function<EntityStatus, Specification<UserGroup>> predicateMethod) {
        Specification<UserGroup> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<UserGroup> addToSpec(final String aString, Specification<UserGroup> spec, Function<String,
            Specification<UserGroup>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<UserGroup> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

        private Page<UserGroupDto> convertUserGroupEntityToUserGroupDto(Page<UserGroup> userGroupPage) {

        List<UserGroup> userGroupList = userGroupPage.getContent();
        List<UserGroupDto> userGroupDtoList = new ArrayList<>();

        for (UserGroup userGroup : userGroupPage) {
            UserGroupDto userGroupDto = modelMapper.map(userGroup, UserGroupDto.class);
            userGroupDtoList.add(userGroupDto);
        }

        int page = userGroupPage.getNumber();
        int size = userGroupPage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableUserGroups = PageRequest.of(page, size);

        return new PageImpl<UserGroupDto>(userGroupDtoList, pageableUserGroups, userGroupPage.getTotalElements());
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<UserGroupDto> userGroups) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (UserGroupDto userGroup : userGroups) {
            String userRoles = userGroup.getUserRoleDtoSet() != null ? 
                userGroup.getUserRoleDtoSet().stream()
                    .map(role -> role.getRole())
                    .collect(Collectors.joining("; ")) : "";

            sb.append(userGroup.getId()).append(",")
                    .append(safe(userGroup.getName())).append(",")
                    .append(safe(userGroup.getDescription())).append(",")
                    .append(safe(userRoles)).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<UserGroupDto> userGroups) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("User Groups");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (UserGroupDto userGroup : userGroups) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(userGroup.getId());
            row.createCell(1).setCellValue(safe(userGroup.getName()));
            row.createCell(2).setCellValue(safe(userGroup.getDescription()));

            String userRoles = userGroup.getUserRoleDtoSet() != null ? 
                userGroup.getUserRoleDtoSet().stream()
                    .map(role -> role.getRole())
                    .collect(Collectors.joining("; ")) : "";
            row.createCell(3).setCellValue(safe(userRoles));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<UserGroupDto> userGroups) throws DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("USER GROUP EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (UserGroupDto userGroup : userGroups) {
            table.addCell(String.valueOf(userGroup.getId()));
            table.addCell(safe(userGroup.getName()));
            table.addCell(safe(userGroup.getDescription()));

            String userRoles = userGroup.getUserRoleDtoSet() != null ? 
                userGroup.getUserRoleDtoSet().stream()
                    .map(role -> role.getRole())
                    .collect(Collectors.joining("; ")) : "";
            table.addCell(safe(userRoles));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public ImportSummary importUserGroupsFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            List<CSVRecord> records = csvParser.getRecords(); // Get records first
            total = records.size();

            for (CSVRecord record : records) {
                try {
                    CreateUserGroupRequest request = new CreateUserGroupRequest();
                    request.setName(record.get("NAME"));
                    request.setDescription(record.get("DESCRIPTION"));

                    UserGroupResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        errors.add("Row " + record.getRecordNumber() + ": " + response.getMessage());
                    }

                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + record.getRecordNumber() + ": Unexpected error - " + e.getMessage());
                }
            }
        }

        // Determine status code and success flag based on import results
        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = success > 0
                ? "Import completed successfully. " + success + " out of " + total + " user groups imported."
                : "Import failed. No user groups were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private UserGroupResponse buildUserGroupResponse(int statusCode, boolean isSuccess, String message,
                                                     UserGroupDto userGroupDto, List<UserGroupDto> userGroupDtoList,
                                                     Page<UserGroupDto> userGroupDtoPage) {

        UserGroupResponse userGroupResponse = new UserGroupResponse();

        userGroupResponse.setStatusCode(statusCode);
        userGroupResponse.setSuccess(isSuccess);
        userGroupResponse.setMessage(message);
        userGroupResponse.setUserGroupDto(userGroupDto);
        userGroupResponse.setUserGroupDtoList(userGroupDtoList);
        userGroupResponse.setUserGroupDtoPage(userGroupDtoPage);

        return userGroupResponse;
    }

    private UserGroupResponse buildUserGroupResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                     UserGroupDto userGroupDto, List<UserGroupDto> userGroupDtoList,
                                                     List<String> errorMessages) {

        UserGroupResponse userGroupResponse = new UserGroupResponse();

        userGroupResponse.setStatusCode(statusCode);
        userGroupResponse.setSuccess(isSuccess);
        userGroupResponse.setMessage(message);
        userGroupResponse.setUserGroupDto(userGroupDto);
        userGroupResponse.setUserGroupDtoList(userGroupDtoList);
        userGroupResponse.setErrorMessages(errorMessages);

        return userGroupResponse;
    }
}
