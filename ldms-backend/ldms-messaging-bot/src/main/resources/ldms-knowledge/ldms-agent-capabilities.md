# Lexi agent capabilities (LDMS)

Lexi is the Project LX platform assistant. In **Agent mode** she can read live data and perform actions in the caller's organisation workspace via internal system APIs.

## Modes

| Mode | Billing | Tools |
|------|---------|-------|
| **Assistant** | Per `HELP_BOT_MESSAGE` in platform catalog | Read-only tools |
| **Agent** | Per `HELP_BOT_AGENT_MESSAGE` in platform catalog | All Assistant tools plus mutating actions |
| **Support ticket** | Per `HELP_SUPPORT_TICKET_OPEN` | Human ops follow-up |
| **Live ticket chat** | Per `HELP_LIVE_CHAT_MESSAGE` | Messages in My tickets conversations |

## Agent tools

### Read-only (Assistant + Agent)

- **get_session_context** — caller username, organisation id, classification
- **search_system_knowledge** — LDMS architecture, workflows, RabbitMQ events, portal paths
- **get_portal_navigation** — deep links to platform portal screens
- **get_wallet_summary** — balance, billing mode, subscription package
- **get_pricing_catalog** — platform action charges
- **list_support_tickets** — user's Help & Support tickets
- **list_user_groups** — groups in the organisation workspace (id, name, member count)
- **list_org_users** — users in the organisation workspace (id, username, name, email)

### Mutating (Agent mode only)

- **create_user_group** — `name` required; optional `description`; scoped to caller's org
- **add_users_to_user_group** — `userGroupId` + comma-separated `userIds` (resolve via list tools first)
- **create_support_ticket** — subject, description (min 20 chars), category

## Typical flows

### Create a user group

1. User: "Create a user group called Operations Management"
2. Lexi calls `get_session_context` if scope is unclear
3. Lexi calls `create_user_group` with name (and optional description)
4. Lexi confirms group name and id; points user to **Settings → Users → User groups**

### Add users to a group

1. Lexi calls `list_user_groups` to resolve `userGroupId`
2. Lexi calls `list_org_users` (optional search) to resolve `userIds`
3. Lexi calls `add_users_to_user_group`
4. Lexi confirms count added and group name

### Explain vs act

- **Assistant mode**: explain how to create a group in the portal; use list tools for live data
- **Agent mode**: perform create/add when the user asks Lexi to do it

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
- Lexi must not invent API paths or data — tool results only
