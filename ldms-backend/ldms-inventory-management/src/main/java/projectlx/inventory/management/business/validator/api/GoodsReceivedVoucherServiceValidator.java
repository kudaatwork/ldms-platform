package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.ReceiveGoodsRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface GoodsReceivedVoucherServiceValidator {
    ValidatorDto isReceiveGoodsRequestValid(ReceiveGoodsRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
}
