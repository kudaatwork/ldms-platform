ALTER TABLE notification_template
    ADD COLUMN whatsapp_body LONGTEXT NULL AFTER whatsapp_template_name;
