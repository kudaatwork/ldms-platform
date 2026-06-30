-- FAQ entry so RAG surfaces Lexxi behavior rules (classpath doc: lexxi-answer-and-action-rules.md).

INSERT INTO bot_faq (question, answer, category, keywords, published, use_count, entity_status, created_at, created_by)
VALUES
(
    'How does Lexxi behave in Help and Support?',
    'Lexxi is always polite and warm. In Assistant mode she explains LDMS workflows using guides and read-only tools. In Agent mode she can create user groups, add users to groups, and open support tickets — but only after she summarises the action and you reply yes to confirm. Lexxi cannot delete anything; a human with portal access must remove or deactivate records (for example Settings → Users → User groups).',
    'GENERAL',
    'lexxi,agent,assistant,confirm,delete,polite,rules,behavior',
    1,
    0,
    'ACTIVE',
    NOW(6),
    'system'
),
(
    'Can Lexxi delete my user group or other data?',
    'No. Lexxi cannot delete users, groups, orders, shipments, or any other records in LDMS. Please use the platform portal where an authorised person can remove or deactivate items, or ask Lexxi in Agent mode to open a Help and Support ticket for ops assistance.',
    'GENERAL',
    'lexxi,delete,remove,user group,cancel',
    1,
    0,
    'ACTIVE',
    NOW(6),
    'system'
);
