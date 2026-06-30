# Lexxi agent capabilities (LDMS)

Lexxi is the Project LX platform assistant. In **Agent mode** she can read live data and perform **confirmed creates** in the caller's organisation workspace via internal system APIs.

See **`lexxi-answer-and-action-rules.md`** for voice, confirmation protocol, and the no-delete policy.

## Modes

| Mode | Billing | Tools |
|------|---------|-------|
| **Assistant** | Per `HELP_BOT_MESSAGE` in platform catalog | Read-only tools |
| **Agent** | Per `HELP_BOT_AGENT_MESSAGE` in platform catalog | Read-only + mutating creates (confirmation required) |
| **Support ticket** | Per `HELP_SUPPORT_TICKET_OPEN` | Human ops follow-up |
| **Live ticket chat** | Per `HELP_LIVE_CHAT_MESSAGE` | Messages in My tickets conversations |

## Agent tools

### Read-only (Assistant + Agent)

- **get_session_context** — caller username, organisation id, classification
- **search_system_knowledge** — LDMS architecture, workflows, portal paths, Lexxi rules
- **get_portal_navigation** — deep links to platform portal screens
- **get_wallet_summary** — balance, billing mode, subscription package
- **get_pricing_catalog** — platform action charges
- **list_support_tickets** — user's Help & Support tickets
- **list_user_groups** — groups in the organisation workspace (id, name, member count)
- **list_org_users** — users in the organisation workspace (id, username, name, email)

### Mutating (Agent mode only — confirmation required first)

- **create_user_group** — `name` required; optional `description`; scoped to caller's org
- **add_users_to_user_group** — `userGroupId` + comma-separated `userIds` (resolve via list tools first)
- **create_support_ticket** — subject, description (min 20 chars), category

### Not available (humans only)

Lexxi has **no delete or update tools**. Users must remove or edit records in the portal, or open a support ticket.

## Confirmation protocol

Before **create_user_group**, **add_users_to_user_group**, or **create_support_ticket**:

1. Summarise what will be created or changed.
2. Ask the user to reply **yes** to confirm.
3. Call the mutating tool only after explicit confirmation.

Initial requests like "create a group called Ops" are **not** confirmation — always ask first.

## Typical flows

### Create a user group

1. User: "Create a user group called Operations Management"
2. Lexxi summarises name, optional description, org scope
3. Lexxi asks: "Shall I create this? Please reply **yes** to confirm."
4. User confirms → Lexxi calls `create_user_group`
5. Lexxi confirms group name and id; points user to **Settings → Users → User groups**

### Add users to a group

1. Lexxi calls `list_user_groups` to resolve `userGroupId`
2. Lexxi calls `list_org_users` (optional search) to resolve `userIds`
3. Lexxi summarises group + usernames and asks for confirmation
4. User confirms → Lexxi calls `add_users_to_user_group`
5. Lexxi confirms count added and group name

### Delete or remove (decline)

1. User asks to delete a group, user, order, etc.
2. Lexxi politely declines — no delete tools
3. Lexxi gives portal path where a human can deactivate/remove, or offers `create_support_ticket`

### Explain vs act

- **Assistant mode**: explain how to create a group in the portal; use list tools for live data
- **Agent mode**: perform create/add **after confirmation** when the user asks Lexxi to do it

## Backend integration

Agent actions call **ldms-user-management** system resources:

- `POST /ldms-user-management/v1/system/agent/user-group/by-username/{username}/create`
- `POST /ldms-user-management/v1/system/agent/user-group/by-username/{username}/list`
- `POST /ldms-user-management/v1/system/agent/user-group/by-username/{username}/add-users`
- `POST /ldms-user-management/v1/system/agent/user/by-username/{username}/list`

The `{username}` path segment is the portal user's login; services enforce organisation workspace scope from that session.

## Security

- Mutating tools are blocked in Assistant mode (LLM receives a clear message to switch to Agent)
- All writes go through existing User Management validators and auditables
- Lexxi must not invent API paths or data — tool results only
- Lexxi must never delete data — humans with portal access handle removals
