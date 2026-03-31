# Add Notification Template modal – design parity with Add Organization modal

**Requirement:** The Add Notification Template modal MUST follow the **same design** as the Add Organization modal (layout, structure, stepper, and behaviour).

This document is the single reference for frontend implementation so both modals feel consistent.

---

## 1. Design parity checklist

Use the **Add Organization** modal as the visual and interaction reference. The Add Notification Template modal MUST:

| Aspect | Add Organization (reference) | Add Notification Template (must match) |
|--------|-----------------------------|----------------------------------------|
| **Modal** | Same width, max-height, overlay, close (X), title area | Same |
| **Layout** | Stepper / multi-step with step indicators (e.g. 1–N) | Same: use steps from metadata (Identity → Channels → channel-specific) |
| **Steps** | Sections: e.g. Basic info → Address → Contact → … | Sections: Identity → Channels → Email / SMS / In-app / WhatsApp (conditional) |
| **Step navigation** | Back / Next; Submit on last step; optional Cancel | Same |
| **Form fields** | Labels, hints, validation messages, required markers | Same |
| **Validation** | Per-step or on Next; errors shown inline | Same; backend returns `errorMessages[]` |
| **Submit** | Single create call; loading state; success/error handling | POST create; same pattern |
| **Styling** | Same component library, spacing, typography, colours | Reuse same modal/stepper components and CSS |

---

## 2. API usage

- **On opening the modal (Add Template):**
  - Call **GET** `.../api/v1/frontend/notification-template/add-template-metadata` (via API Gateway).
  - Use the response to build the stepper and sections (see below).

- **On submit (last step):**
  - Call **POST** `.../api/v1/frontend/notification-template/create` with a JSON body matching `CreateTemplateRequest`.

---

## 3. Response shape: Add Template metadata

**GET** `.../notification-template/add-template-metadata` returns (inside the standard API wrapper):

```json
{
  "statusCode": 200,
  "isSuccess": true,
  "addTemplateMetadata": {
    "sections": [
      { "sectionKey": "identity", "sectionLabel": "Template identity", "sectionDescription": "Basic identifier and description (like Organization name and type).", "order": 1, "fieldKeys": ["templateKey", "description"] },
      { "sectionKey": "channels", "sectionLabel": "Delivery channels", "sectionDescription": "Select where this template will be sent (Email, SMS, In-app, WhatsApp).", "order": 2, "fieldKeys": ["channels"] },
      { "sectionKey": "email", "sectionLabel": "Email content", "sectionDescription": "Subject and HTML body. Shown when Email is selected. Use {{placeholder}} for variables.", "order": 3, "fieldKeys": ["emailSubject", "emailBodyHtml"] },
      { "sectionKey": "sms", "sectionLabel": "SMS content", "sectionDescription": "Message body for SMS (max 320 characters). Shown when SMS is selected.", "order": 4, "fieldKeys": ["smsBody"] },
      { "sectionKey": "inApp", "sectionLabel": "In-app content", "sectionDescription": "Title and body for in-app notifications. Shown when In-app is selected.", "order": 5, "fieldKeys": ["inAppTitle", "inAppBody"] },
      { "sectionKey": "whatsapp", "sectionLabel": "WhatsApp", "sectionDescription": "Twilio/WhatsApp template name (Content SID). Shown when WhatsApp is selected.", "order": 6, "fieldKeys": ["whatsappTemplateName"] }
    ],
    "channelOptions": [
      { "value": "EMAIL", "label": "EMAIL", "description": "Send as email (subject + HTML body)." },
      { "value": "SMS", "label": "SMS", "description": "Send as SMS (max 320 chars)." },
      { "value": "IN_APP", "label": "IN APP", "description": "Show in-app notification (title + body)." },
      { "value": "WHATSAPP", "label": "WHATSAPP", "description": "Send via WhatsApp (Twilio template name required)." }
    ]
  }
}
```

---

## 4. Mapping metadata to the modal (same pattern as Add Organization)

- **Step 1 – Identity**  
  - Use section with `sectionKey === "identity"`.  
  - Render fields: `templateKey`, `description`.  
  - Use `sectionLabel` as step title and `sectionDescription` as hint.

- **Step 2 – Channels**  
  - Use section with `sectionKey === "channels"`.  
  - Render a multi-select from `channelOptions` (value, label, description).  
  - Bind selected values to `channels` in the create payload.

- **Steps 3–6 – Conditional channel sections**  
  - Only show a section if its channel is in the selected `channels`:  
    - `email` ↔ EMAIL  
    - `sms` ↔ SMS  
    - `inApp` ↔ IN_APP  
    - `whatsapp` ↔ WHATSAPP  
  - Use each section’s `sectionLabel`, `sectionDescription`, and `fieldKeys` for the step title, hint, and form fields.  
  - This gives a dynamic number of steps, like Add Organization’s conditional sections.

- **Navigation**  
  - Back / Next between steps; Submit on the last step.  
  - Reuse the same stepper component and button placement as Add Organization.

---

## 5. Create request body (POST create)

Send a single object per backend contract (same structure as in OpenAPI):

```json
{
  "templateKey": "MY_TEMPLATE_KEY",
  "description": "When this notification is sent.",
  "channels": ["EMAIL", "SMS"],
  "emailSubject": "Optional if EMAIL not in channels",
  "emailBodyHtml": "HTML with {{placeholders}}.",
  "smsBody": "Optional if SMS not in channels.",
  "inAppTitle": null,
  "inAppBody": null,
  "whatsappTemplateName": null
}
```

- Required: `templateKey`, `description`, `channels` (non-empty).  
- For each selected channel, the backend requires the corresponding fields (e.g. EMAIL → `emailSubject` + `emailBodyHtml`; SMS → `smsBody`).  
- Omit or null unused channel fields if that channel is not selected.

---

## 6. Implementation notes

- **Reuse the same modal and stepper components** used for Add Organization; only the steps and form fields change.  
- **Same design** = same CSS classes / design tokens, same modal size, same step indicator style, same button labels (e.g. “Back”, “Next”, “Create” / “Save”).  
- **Validation:** Show backend `errorMessages[]` in the same way as Add Organization (e.g. at top of modal or per field).  
- **Loading / success / error:** Use the same patterns as Add Organization (spinner on submit, success message or redirect, error message display).

When the Add Notification Template modal is implemented to this spec, it will follow the same design as the Add Organization modal.
