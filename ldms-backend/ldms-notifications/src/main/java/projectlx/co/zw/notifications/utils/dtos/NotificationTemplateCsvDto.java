package projectlx.co.zw.notifications.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

/**
 * DTO for mapping CSV data to NotificationTemplate
 */
@Data
public class NotificationTemplateCsvDto {
    
    @CsvBindByName(column = "TEMPLATE KEY")
    private String templateKey;
    
    @CsvBindByName(column = "DESCRIPTION")
    private String description;
    
    @CsvBindByName(column = "CHANNELS")
    private String channels;
    
    @CsvBindByName(column = "EMAIL SUBJECT")
    private String emailSubject;
    
    @CsvBindByName(column = "EMAIL BODY HTML")
    private String emailBodyHtml;
    
    @CsvBindByName(column = "SMS BODY")
    private String smsBody;
    
    @CsvBindByName(column = "IN-APP TITLE")
    private String inAppTitle;
    
    @CsvBindByName(column = "IN-APP BODY")
    private String inAppBody;
    
    @CsvBindByName(column = "WHATSAPP TEMPLATE NAME")
    private String whatsappTemplateName;
    
    @CsvBindByName(column = "ACTIVE")
    private String active;
}