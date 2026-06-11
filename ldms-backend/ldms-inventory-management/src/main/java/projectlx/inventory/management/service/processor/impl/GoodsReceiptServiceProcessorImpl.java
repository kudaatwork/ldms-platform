package projectlx.inventory.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.GoodsReceiptProcessor;
import projectlx.inventory.management.business.logic.api.PurchaseOrderStatusManager;
import projectlx.inventory.management.model.PurchaseOrder;
import projectlx.inventory.management.model.PurchaseOrderStatus;
import projectlx.inventory.management.repository.PurchaseOrderRepository;
import projectlx.inventory.management.service.processor.api.GoodsReceiptServiceProcessor;
import projectlx.inventory.management.utils.dtos.GoodsReceiptResult;
import projectlx.inventory.management.utils.dtos.PurchaseOrderDto;
import projectlx.inventory.management.utils.dtos.ReceiptValidationError;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.ReceiveGoodsRequest;
import projectlx.inventory.management.utils.responses.PurchaseOrderResponse;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsReceiptServiceProcessorImpl implements GoodsReceiptServiceProcessor {

    private final GoodsReceiptProcessor goodsReceiptProcessor;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderStatusManager purchaseOrderStatusManager;
    private final ModelMapper modelMapper;
    private final MessageService messageService;

    @Override
    public PurchaseOrderResponse receiveGoods(ReceiveGoodsRequest request, Locale locale, String username) {
        log.info("Incoming request to receive goods for PO {} by user {}", request.getPurchaseOrderId(), username);
        GoodsReceiptResult result = goodsReceiptProcessor.processGoodsReceipt(request, username, locale);

        if (!result.isSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_RECEIVE_GOODS_INVALID_REQUEST.getCode(), new String[]{}, locale);
            List<String> errors = result.getErrors() == null ? null : result.getErrors().stream()
                    .map(ReceiptValidationError::getErrorMessage)
                    .collect(Collectors.toList());
            return buildResponse(400, false, message, null, null, errors);
        }

        Optional<PurchaseOrder> poOpt = purchaseOrderRepository
                .findByIdAndEntityStatusNot(request.getPurchaseOrderId(), EntityStatus.DELETED);

        if (poOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_PURCHASE_ORDER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildResponse(404, false, message, null, null, null);
        }

        PurchaseOrderDto dto = mapToDto(poOpt.get());
        String successMsg = messageService.getMessage(I18Code.MESSAGE_RECEIVE_GOODS_SUCCESSFUL.getCode(), new String[]{}, locale);
        return buildResponse(200, true, successMsg, dto, null, null);
    }

    private PurchaseOrderDto mapToDto(PurchaseOrder purchaseOrder) {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper.map(purchaseOrder, PurchaseOrderDto.class);
    }

    private PurchaseOrderResponse buildResponse(int statusCode, boolean isSuccess, String message,
                                                PurchaseOrderDto dto, List<PurchaseOrderDto> dtoList,
                                                List<String> errorMessages) {
        PurchaseOrderResponse response = new PurchaseOrderResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(isSuccess);
        response.setMessage(message);
        response.setPurchaseOrderDto(dto);
        response.setPurchaseOrderDtoList(dtoList);
        response.setErrorMessages(errorMessages);
        return response;
    }
}
