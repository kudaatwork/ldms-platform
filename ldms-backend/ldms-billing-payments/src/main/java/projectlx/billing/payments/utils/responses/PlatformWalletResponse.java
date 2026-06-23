package projectlx.billing.payments.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.billing.payments.utils.dtos.OrganizationBillingSettingDto;
import projectlx.billing.payments.utils.dtos.PlatformActionChargeDto;
import projectlx.billing.payments.utils.dtos.PlatformWalletSummaryDto;
import projectlx.billing.payments.utils.dtos.RecordPlatformUsageChargeResultDto;
import projectlx.billing.payments.utils.dtos.SubscriptionPackageDto;
import projectlx.billing.payments.utils.dtos.UsageChargeReportDto;
import projectlx.billing.payments.utils.dtos.UsageChargeRecordDto;
import projectlx.billing.payments.utils.dtos.WalletDepositDto;
import projectlx.billing.payments.utils.dtos.WalletTransactionDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformWalletResponse extends CommonResponse {
    private PlatformWalletSummaryDto platformWalletSummaryDto;
    private OrganizationBillingSettingDto organizationBillingSettingDto;
    private PlatformActionChargeDto platformActionChargeDto;
    private List<PlatformActionChargeDto> platformActionChargeDtoList;
    private SubscriptionPackageDto subscriptionPackageDto;
    private List<SubscriptionPackageDto> subscriptionPackageDtoList;
    private WalletDepositDto walletDepositDto;
    private List<WalletDepositDto> walletDepositDtoList;
    private WalletTransactionDto walletTransactionDto;
    private List<WalletTransactionDto> walletTransactionDtoList;
    private UsageChargeRecordDto usageChargeRecordDto;
    private List<UsageChargeRecordDto> usageChargeRecordDtoList;
    private UsageChargeReportDto usageChargeReportDto;
    private RecordPlatformUsageChargeResultDto recordPlatformUsageChargeResultDto;
    private Boolean fuelConsumptionAvailable;
    private String receiptHtml;
}
