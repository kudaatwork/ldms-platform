package projectlx.user.management.service.business.logic.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.config.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.auditable.api.UserAddressServiceAuditable;
import projectlx.user.management.service.business.validator.api.UserAddressServiceValidator;
import projectlx.user.management.service.model.Address;
import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.repository.UserAddressRepository;
import projectlx.user.management.service.repository.UserRepository;
import projectlx.user.management.service.utils.dtos.AddressDto;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.CreateAddressRequest;
import projectlx.user.management.service.utils.requests.EditAddressRequest;
import projectlx.user.management.service.utils.requests.AddressMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.AddressResponse;
import java.lang.reflect.Type;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressServiceImplTest {

    private UserAddressServiceImpl userAddressService;
    private UserAddressServiceValidator userAddressServiceValidator;
    private MessageService messageService;
    private ModelMapper modelMapper;
    private Configuration configurationMock;
    private UserAddressRepository userAddressRepository;
    private UserRepository userRepository;
    private UserAddressServiceAuditable userAddressServiceAuditable;
    private projectlx.user.management.service.clients.LocationsServiceClient locationsServiceClient;

    private final Locale locale = Locale.ENGLISH;
    private final String username = "SYSTEM";

    private CreateAddressRequest createAddressRequest;
    private EditAddressRequest editAddressRequest;
    private AddressMultipleFiltersRequest addressMultipleFiltersRequest;
    private Address address;
    private AddressDto addressDto;
    private List<Address> addressList;
    private List<AddressDto> addressDtoList;
    private Long userAddressId;
    private Page<Address> userAddressPage;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        userAddressServiceValidator = mock(UserAddressServiceValidator.class);
        messageService = mock(MessageService.class);
        modelMapper = mock(ModelMapper.class);
        configurationMock = mock(Configuration.class);
        userAddressRepository = mock(UserAddressRepository.class);
        userRepository = mock(UserRepository.class);
        userAddressServiceAuditable = mock(UserAddressServiceAuditable.class);
        locationsServiceClient = mock(projectlx.user.management.service.clients.LocationsServiceClient.class);

        // Instantiate service
        userAddressService = new UserAddressServiceImpl(
                userAddressServiceValidator,
                messageService,
                modelMapper,
                userAddressRepository,
                userRepository,
                userAddressServiceAuditable,
                locationsServiceClient
        );

        // Common ModelMapper configuration stub
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);

        // Test data
        userAddressId = 1L;
        address = new Address();
        address.setId(userAddressId);
        address.setLocationAddressId(100L);
        address.setEntityStatus(EntityStatus.ACTIVE);
        
        // Create AddressDto for the transient addressDetails field
        AddressDto details = new AddressDto();
        details.setId(100L); // Location address ID
        details.setLine1("123 Main St");
        details.setLine2("Apt 4B");
        details.setPostalCode("12345");
        details.setSuburbId(200L);
        details.setGeoCoordinatesId(300L);
        address.setAddressDetails(details);

        addressList = new ArrayList<>();
        addressList.add(address);

        addressDto = new AddressDto();
        addressDto.setId(userAddressId);
        addressDto.setLocationAddressId(100L);
        addressDto.setLine1("123 Main St");
        addressDto.setLine2("Apt 4B");
        addressDto.setPostalCode("12345");
        addressDto.setSuburbId(200L);
        addressDto.setGeoCoordinatesId(300L);
        addressDto.setEntityStatus(projectlx.co.zw.shared_library.utils.enums.EntityStatus.ACTIVE);
        addressDto.setCreatedAt(LocalDateTime.now());
        addressDto.setUpdatedAt(LocalDateTime.now());

        addressDtoList = new ArrayList<>();
        addressDtoList.add(addressDto);

        createAddressRequest = new CreateAddressRequest();
        createAddressRequest.setLine1("123 Main St");
        createAddressRequest.setLine2("Apt 4B");
        createAddressRequest.setPostalCode("12345");
        createAddressRequest.setSuburbId(200L);
        createAddressRequest.setGeoCoordinatesId(300L);

        editAddressRequest = new EditAddressRequest();
        editAddressRequest.setId(userAddressId);
        editAddressRequest.setLocationAddressId(100L);
        editAddressRequest.setLine1("456 Elm St");
        editAddressRequest.setLine2("Suite 101");
        editAddressRequest.setPostalCode("67890");
        editAddressRequest.setSuburbId(201L);
        editAddressRequest.setGeoCoordinatesId(301L);

        addressMultipleFiltersRequest = new AddressMultipleFiltersRequest();
        addressMultipleFiltersRequest.setPage(0);
        addressMultipleFiltersRequest.setSize(10);
        addressMultipleFiltersRequest.setLine1("123 Main St");
        addressMultipleFiltersRequest.setLine2("Apt 4B");
        addressMultipleFiltersRequest.setPostalCode("12345");
        addressMultipleFiltersRequest.setEntityStatus(projectlx.co.zw.shared_library.utils.enums.EntityStatus.ACTIVE);
        addressMultipleFiltersRequest.setSearchValue("Test");

        // Page data
        Pageable pageable = PageRequest.of(0, 10);
        userAddressPage = new PageImpl<>(addressList, pageable, addressList.size());
    }

    @Test
    public void create_shouldReturnFalseAnd400IfRequestIsInvalid() {
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Error message");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);

        when(userAddressServiceValidator.isCreateAddressRequestValid(any(CreateAddressRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_CREATE_USER_ADDRESS_INVALID_REQUEST.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid request");

        AddressResponse response = userAddressService.create(createAddressRequest, locale, username);

        verify(userAddressServiceValidator, times(1))
                .isCreateAddressRequestValid(any(CreateAddressRequest.class), eq(locale));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_CREATE_USER_ADDRESS_INVALID_REQUEST.getCode()), any(String[].class), eq(locale));
        // Verify that locationsServiceClient.create() was NOT called when request is invalid
        verify(locationsServiceClient, times(0))
                .create(any(CreateAddressRequest.class), any(Locale.class));
        verify(userAddressServiceAuditable, times(0))
                .create(any(Address.class), any(Locale.class), anyString());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getErrorMessages());
        assertEquals(errorMessages, response.getErrorMessages());
    }

    @Test
    public void create_shouldReturnFalseAnd500IfLocationServiceFails() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAddressServiceValidator.isCreateAddressRequestValid(any(CreateAddressRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        
        // Mock LocationsServiceClient response for address existence check
        AddressResponse locationServiceResponse = new AddressResponse();
        locationServiceResponse.setSuccess(false);
        locationServiceResponse.setStatusCode(400); // Status code from location service
        locationServiceResponse.setMessage("Address already exists");
        locationServiceResponse.setAddressDto(null); // Ensure addressDto is null
        when(locationsServiceClient.create(eq(createAddressRequest), eq(locale)))
                .thenReturn(locationServiceResponse);
                
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_LOCATION_SERVICE_ERROR.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Address already exists");

        AddressResponse response = userAddressService.create(createAddressRequest, locale, username);

        verify(userAddressServiceValidator, times(1))
                .isCreateAddressRequestValid(any(CreateAddressRequest.class), eq(locale));
        verify(locationsServiceClient, times(1))
                .create(eq(createAddressRequest), eq(locale));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_LOCATION_SERVICE_ERROR.getCode()), any(String[].class), eq(locale));
        verify(userAddressServiceAuditable, times(0))
                .create(any(Address.class), any(Locale.class), anyString());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(500, response.getStatusCode()); // UserAddressServiceImpl returns 500 when location service fails
    }

    @Test
    public void create_shouldReturnTrueAnd201ForValidRequest() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAddressServiceValidator.isCreateAddressRequestValid(any(CreateAddressRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        
        // Mock successful LocationsServiceClient response
        AddressResponse locationServiceResponse = new AddressResponse();
        locationServiceResponse.setSuccess(true);
        locationServiceResponse.setStatusCode(201);
        locationServiceResponse.setAddressDto(addressDto);
        when(locationsServiceClient.create(eq(createAddressRequest), eq(locale)))
                .thenReturn(locationServiceResponse);
                
        // Mock modelMapper to return a valid AddressDto
        when(modelMapper.map(any(AddressDto.class), eq(AddressDto.class)))
                .thenReturn(addressDto);

        // Stub ModelMapper configuration
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);
        when(configurationMock.setMatchingStrategy(any())).thenReturn(configurationMock);

        // Mock Address creation
        when(userAddressServiceAuditable.create(any(Address.class), any(Locale.class), anyString()))
                .thenReturn(address);
        
        // Mock address details population
        when(locationsServiceClient.findById(anyLong(), any(Locale.class)))
                .thenReturn(locationServiceResponse);
                
        when(modelMapper.map(any(Address.class), eq(AddressDto.class)))
                .thenReturn(addressDto);
        AddressResponse response = userAddressService.create(createAddressRequest, locale, username);

        verify(userAddressServiceValidator, times(1))
                .isCreateAddressRequestValid(any(CreateAddressRequest.class), eq(locale));
        verify(locationsServiceClient, times(1))
                .create(eq(createAddressRequest), eq(locale));
        verify(userAddressServiceAuditable, times(1))
                .create(any(Address.class), any(Locale.class), anyString());
        verify(messageService, times(0))
                .getMessage(eq(I18Code.MESSAGE_ADDRESS_CREATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getAddressDto());
    }

    @Test
    public void findById_shouldReturnFalseAnd400IfIdIsInvalid() {
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid ID error");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);

        when(userAddressServiceValidator.isIdValid(anyLong(), eq(locale)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_ADDRESS_ID_INVALID.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid ID");

        AddressResponse response = userAddressService.findById(userAddressId, locale, username);

        verify(userAddressServiceValidator, times(1))
                .isIdValid(anyLong(), eq(locale));
        verify(userAddressRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_ADDRESS_ID_INVALID.getCode()), any(String[].class), eq(locale));
        // No need to verify LocationsServiceClient as the method returns early if ID is invalid

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
        // The response should have a message but might not have errorMessages list
        // since we're using buildAddressResponse not buildAddressResponseWithErrors
        assertEquals("Invalid ID", response.getMessage());
    }

    @Test
    public void findById_shouldReturnFalseAnd404IfNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAddressServiceValidator.isIdValid(anyLong(), eq(locale)))
                .thenReturn(validatorDto);
        when(userAddressRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Not found");

        AddressResponse response = userAddressService.findById(userAddressId, locale, username);

        verify(userAddressServiceValidator, times(1))
                .isIdValid(anyLong(), eq(locale));
        verify(userAddressRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findById_shouldReturnTrueAnd200ForValidId() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAddressServiceValidator.isIdValid(anyLong(), eq(locale)))
                .thenReturn(validatorDto);
        when(userAddressRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(address));
                
        // Mock LocationsServiceClient.findById response
        AddressResponse locationServiceResponse = new AddressResponse();
        locationServiceResponse.setSuccess(true);
        locationServiceResponse.setStatusCode(200);
        locationServiceResponse.setAddressDto(addressDto);
        when(locationsServiceClient.findById(anyLong(), any(Locale.class)))
                .thenReturn(locationServiceResponse);
                
        // The implementation uses convertToAddressDto which creates a new AddressDto instead of using modelMapper directly
        // So we don't need to mock modelMapper.map here
                
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ADDRESS_RETRIEVED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Retrieved");

        AddressResponse response = userAddressService.findById(userAddressId, locale, username);

        verify(userAddressServiceValidator, times(1))
                .isIdValid(anyLong(), eq(locale));
        verify(userAddressRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(locationsServiceClient, times(1))
                .findById(anyLong(), any(Locale.class));
        // The implementation uses convertToAddressDto which creates a new AddressDto instead of using modelMapper directly
        // So we don't verify modelMapper.map here
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ADDRESS_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAddressDto());
    }

    @Test
    public void findAllAsList_shouldReturnFalseAnd404IfEmpty() {
        when(userAddressRepository.findByEntityStatusNot(any(EntityStatus.class)))
                .thenReturn(Collections.emptyList());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("No addresses");

        AddressResponse response = userAddressService.findAllAsList(username, locale);

        verify(userAddressRepository, times(1))
                .findByEntityStatusNot(EntityStatus.DELETED);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findAllAsList_shouldReturnTrueAnd200WithList() {
        Type listType = new TypeToken<List<AddressDto>>() {}.getType();

        when(userAddressRepository.findByEntityStatusNot(any(EntityStatus.class)))
                .thenReturn(addressList);
                
        // Mock LocationsServiceClient.findById response
        AddressResponse locationServiceResponse = new AddressResponse();
        locationServiceResponse.setSuccess(true);
        locationServiceResponse.setStatusCode(200);
        locationServiceResponse.setAddressDto(addressDto);
        when(locationsServiceClient.findById(anyLong(), any(Locale.class)))
                .thenReturn(locationServiceResponse);
                
        // Mock modelMapper for individual Address to AddressDto conversion
        when(modelMapper.map(any(Address.class), eq(AddressDto.class)))
                .thenReturn(addressDto);
                
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ADDRESS_RETRIEVED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("List retrieved");

        AddressResponse response = userAddressService.findAllAsList(username, locale);

        verify(userAddressRepository, times(1))
                .findByEntityStatusNot(EntityStatus.DELETED);
        verify(locationsServiceClient, times(1))
                .findById(anyLong(), any(Locale.class));
        // The implementation uses convertToAddressDto which creates a new AddressDto instead of using modelMapper directly
        // So we don't verify modelMapper.map here
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ADDRESS_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAddressDtoList());
        assertFalse(response.getAddressDtoList().isEmpty());
        assertEquals(1, response.getAddressDtoList().size());
    }

    @Test
    public void update_shouldReturnFalseAnd400IfRequestIsInvalid() {
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid update error");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);

        when(userAddressServiceValidator.isRequestValidForEditing(any(EditAddressRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_UPDATE_USER_ADDRESS_INVALID_REQUEST.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid update");

        AddressResponse response = userAddressService.update(editAddressRequest, username, locale);

        verify(userAddressServiceValidator, times(1))
                .isRequestValidForEditing(any(EditAddressRequest.class), eq(locale));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_UPDATE_USER_ADDRESS_INVALID_REQUEST.getCode()), any(String[].class), eq(locale));
        verify(userAddressRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userAddressServiceAuditable, times(0))
                .update(any(Address.class), any(Locale.class), anyString());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getErrorMessages());
        assertEquals(errorMessages, response.getErrorMessages());
    }

    @Test
    public void update_shouldReturnFalseAnd400IfNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userAddressServiceValidator.isRequestValidForEditing(any(EditAddressRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userAddressRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Not found");

        AddressResponse response = userAddressService.update(editAddressRequest, username, locale);

        verify(userAddressServiceValidator, times(1))
                .isRequestValidForEditing(any(EditAddressRequest.class), any(Locale.class));
        verify(userAddressRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode()), any(String[].class), eq(locale));
        verify(userAddressServiceAuditable, times(0))
                .update(any(Address.class), any(Locale.class), anyString());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void update_shouldReturnTrueAnd201WhenSharedByOne() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userAddressServiceValidator.isRequestValidForEditing(any(EditAddressRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userAddressRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(address));
                
        // Mock LocationsServiceClient.update response
        AddressResponse locationServiceResponse = new AddressResponse();
        locationServiceResponse.setSuccess(true);
        locationServiceResponse.setStatusCode(200);
        locationServiceResponse.setAddressDto(addressDto);
        when(locationsServiceClient.update(any(EditAddressRequest.class), any(Locale.class)))
                .thenReturn(locationServiceResponse);
                
        // Mock LocationsServiceClient.findById response for populateAddressDetails
        AddressResponse findByIdResponse = new AddressResponse();
        findByIdResponse.setSuccess(true);
        findByIdResponse.setStatusCode(200);
        findByIdResponse.setAddressDto(addressDto);
        when(locationsServiceClient.findById(anyLong(), any(Locale.class)))
                .thenReturn(findByIdResponse);

        // Stub applyUpdates: modelMapper mapping happens after auditable.update
        when(userAddressServiceAuditable.update(any(Address.class), any(Locale.class), anyString()))
                .thenReturn(address);
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);
        when(configurationMock.setMatchingStrategy(any())).thenReturn(configurationMock);
        when(modelMapper.map(any(Address.class), eq(AddressDto.class)))
                .thenReturn(addressDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ADDRESS_UPDATED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Updated");

        AddressResponse response = userAddressService.update(editAddressRequest, username, locale);

        verify(userAddressServiceValidator, times(1))
                .isRequestValidForEditing(any(EditAddressRequest.class), any(Locale.class));
        verify(userAddressRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        // The implementation now uses locationsServiceClient.update instead of countByIdAndEntityStatusNot
        verify(locationsServiceClient, times(1))
                .update(any(EditAddressRequest.class), any(Locale.class));
        verify(userAddressServiceAuditable, times(1))
                .update(any(Address.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ADDRESS_UPDATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getAddressDto());
    }

    @Test
    public void delete_shouldReturnFalseAnd400IfIdIsInvalid() {
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid ID");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);
        when(userAddressServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_ADDRESS_ID_INVALID.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid ID");

        AddressResponse response = userAddressService.delete(userAddressId, locale, username);

        verify(userAddressServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userAddressRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userAddressServiceAuditable, times(0))
                .delete(any(Address.class), any(Locale.class));

        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_ADDRESS_ID_INVALID.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void delete_shouldReturnFalseAnd404IfNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userAddressServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userAddressRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Not found");

        AddressResponse response = userAddressService.delete(userAddressId, locale, username);

        verify(userAddressServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userAddressRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode()), any(String[].class), eq(locale));
        verify(userAddressServiceAuditable, times(0))
                .delete(any(Address.class), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void delete_shouldReturnTrueAnd200ForValidRequest() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userAddressServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userAddressRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(address));
                
        // Mock LocationsServiceClient.delete response
        AddressResponse locationServiceResponse = new AddressResponse();
        locationServiceResponse.setSuccess(true);
        locationServiceResponse.setStatusCode(200);
        locationServiceResponse.setAddressDto(addressDto);
        when(locationsServiceClient.delete(anyLong(), any(Locale.class)))
                .thenReturn(locationServiceResponse);
                
        when(userAddressServiceAuditable.delete(any(Address.class), any(Locale.class)))
                .thenReturn(address);
        when(modelMapper.map(any(Address.class), eq(AddressDto.class)))
                .thenReturn(addressDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ADDRESS_DELETED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Deleted");

        AddressResponse response = userAddressService.delete(userAddressId, locale, username);

        verify(userAddressServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userAddressRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(locationsServiceClient, times(1))
                .delete(anyLong(), any(Locale.class));
        verify(userAddressServiceAuditable, times(1))
                .delete(any(Address.class), any(Locale.class));
        // The implementation uses convertToAddressDto which creates a new AddressDto instead of using modelMapper directly
        // So we don't verify modelMapper.map here
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ADDRESS_DELETED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAddressDto());
    }

    @Test
    public void findByMultipleFilters_shouldReturnFalseAnd400IfRequestIsInvalid() {
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid filters");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);
        when(userAddressServiceValidator.isRequestValidToRetrieveAddressesByMultipleFilters(any(AddressMultipleFiltersRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ADDRESS_INVALID_MULTIPLE_FILTERS_REQUEST.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid filters");

        AddressResponse response = userAddressService.findByMultipleFilters(
                addressMultipleFiltersRequest, username, locale
        );

        verify(userAddressServiceValidator, times(1))
                .isRequestValidToRetrieveAddressesByMultipleFilters(any(AddressMultipleFiltersRequest.class), any(Locale.class));
        verify(userAddressRepository, times(0))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ADDRESS_INVALID_MULTIPLE_FILTERS_REQUEST.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void findByMultipleFilters_shouldReturnFalseAnd404IfNoResults() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userAddressServiceValidator.isRequestValidToRetrieveAddressesByMultipleFilters(any(AddressMultipleFiltersRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        
        // Mock LocationsServiceClient.findByMultipleFilters response
        AddressResponse locationServiceResponse = new AddressResponse();
        locationServiceResponse.setSuccess(false);
        locationServiceResponse.setStatusCode(404);
        locationServiceResponse.setMessage("No addresses found");
        locationServiceResponse.setAddressDtoList(null);
        when(locationsServiceClient.findByMultipleFilters(any(AddressMultipleFiltersRequest.class), any(Locale.class)))
                .thenReturn(locationServiceResponse);
                
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("No addresses");

        AddressResponse response = userAddressService.findByMultipleFilters(
                addressMultipleFiltersRequest, username, locale
        );

        verify(userAddressServiceValidator, times(1))
                .isRequestValidToRetrieveAddressesByMultipleFilters(any(AddressMultipleFiltersRequest.class), any(Locale.class));
        // The implementation now uses LocationsServiceClient instead of repository
        verify(locationsServiceClient, times(1))
                .findByMultipleFilters(any(AddressMultipleFiltersRequest.class), any(Locale.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findByMultipleFilters_shouldReturnTrueAnd200WithPage() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userAddressServiceValidator.isRequestValidToRetrieveAddressesByMultipleFilters(any(AddressMultipleFiltersRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
                
        // Mock LocationsServiceClient.findByMultipleFilters response
        AddressResponse locationServiceResponse = new AddressResponse();
        locationServiceResponse.setSuccess(true);
        locationServiceResponse.setStatusCode(200);
        locationServiceResponse.setAddressDtoList(addressDtoList);
        when(locationsServiceClient.findByMultipleFilters(any(AddressMultipleFiltersRequest.class), any(Locale.class)))
                .thenReturn(locationServiceResponse);
                
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ADDRESS_RETRIEVED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Page retrieved");

        AddressResponse response = userAddressService.findByMultipleFilters(
                addressMultipleFiltersRequest, username, locale
        );

        verify(userAddressServiceValidator, times(1))
                .isRequestValidToRetrieveAddressesByMultipleFilters(any(AddressMultipleFiltersRequest.class), any(Locale.class));
        // The implementation now uses LocationsServiceClient instead of repository
        verify(locationsServiceClient, times(1))
                .findByMultipleFilters(any(AddressMultipleFiltersRequest.class), any(Locale.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ADDRESS_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getAddressDtoList());
        assertEquals(addressDtoList, response.getAddressDtoList());
    }

    @Test
    public void exportToCsv_shouldReturnCorrectByteArray() {
        // Given
        String expectedCsvStart = "ID,LINE 1,LINE 2,POSTAL CODE,SUBURB ID,GEO COORDINATES ID,CREATED AT,UPDATED AT,ENTITY STATUS\n";
        String expectedCsvDataStart = "1,123 Main St,Apt 4B,12345,200,300,";
        
        // When
        byte[] result = userAddressService.exportToCsv(addressDtoList);

        // Then
        assertNotNull(result);
        String resultContent = new String(result);
        assertTrue(resultContent.startsWith(expectedCsvStart), "CSV should start with expected headers");
        // Check that the data row contains the expected values
        assertTrue(resultContent.contains("1,123 Main St,Apt 4B,12345,200,300,"), "CSV should contain expected data");
        assertTrue(resultContent.contains("ACTIVE"), "CSV should contain ACTIVE status");
    }

    @Test
    public void exportToExcel_shouldReturnNonEmptyByteArray() throws IOException {
        // Mock LocationsServiceClient.exportAddresses response for Excel
        byte[] excelBytes = new byte[100]; // Dummy Excel data
        org.springframework.http.ResponseEntity<byte[]> responseEntity = 
            new org.springframework.http.ResponseEntity<>(excelBytes, org.springframework.http.HttpStatus.OK);
        
        when(locationsServiceClient.exportAddresses(any(), eq("excel"), any(Locale.class)))
            .thenReturn(responseEntity);
            
        // When
        byte[] result = userAddressService.exportToExcel(addressDtoList);

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void exportToPdf_shouldReturnNonEmptyByteArray() throws Exception {
        // Mock LocationsServiceClient.exportAddresses response for PDF
        // Create a simple PDF-like content that starts with %PDF
        String pdfContent = "%PDF-1.4\nSome dummy PDF content";
        byte[] pdfBytes = pdfContent.getBytes();
        org.springframework.http.ResponseEntity<byte[]> responseEntity = 
            new org.springframework.http.ResponseEntity<>(pdfBytes, org.springframework.http.HttpStatus.OK);
        
        when(locationsServiceClient.exportAddresses(any(), eq("pdf"), any(Locale.class)))
            .thenReturn(responseEntity);
            
        // When
        byte[] result = userAddressService.exportToPdf(addressDtoList);

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
        // PDF files start with %PDF
        assertTrue(new String(result).startsWith("%PDF"));
    }
}
