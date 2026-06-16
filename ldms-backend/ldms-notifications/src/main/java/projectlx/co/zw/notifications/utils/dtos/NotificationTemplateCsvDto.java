package projectlx.co.zw.notifications.utils.dtos;

import com.opencsv.bean.CsvBindByName;

/**
 * DTO for mapping CSV data to NotificationTemplate
 */
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

    @CsvBindByName(column = "WHATSAPP BODY")
    private String whatsappBody;

    @CsvBindByName(column = "ACTIVE")
    private String active;

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getChannels() { return channels; }
    public void setChannels(String channels) { this.channels = channels; }

    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject(String emailSubject) { this.emailSubject = emailSubject; }

    public String getEmailBodyHtml() { return emailBodyHtml; }
    public void setEmailBodyHtml(String emailBodyHtml) { this.emailBodyHtml = emailBodyHtml; }

    public String getSmsBody() { return smsBody; }
    public void setSmsBody(String smsBody) { this.smsBody = smsBody; }

    public String getInAppTitle() { return inAppTitle; }
    public void setInAppTitle(String inAppTitle) { this.inAppTitle = inAppTitle; }

    public String getInAppBody() { return inAppBody; }
    public void setInAppBody(String inAppBody) { this.inAppBody = inAppBody; }

    public String getWhatsappTemplateName() { return whatsappTemplateName; }
    public void setWhatsappTemplateName(String whatsappTemplateName) { this.whatsappTemplateName = whatsappTemplateName; }

    public String getWhatsappBody() { return whatsappBody; }
    public void setWhatsappBody(String whatsappBody) { this.whatsappBody = whatsappBody; }

    public String getActive() { return active; }
    public void setActive(String active) { this.active = active; }

    @Override
    public String toString() {
        return "NotificationTemplateCsvDto{templateKey='" + templateKey + "'}";
    }
}
