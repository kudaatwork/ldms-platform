-- Assistant (user guide) vs Agent (platform architecture + future actions).
ALTER TABLE bot_session
    ADD COLUMN assistant_mode VARCHAR(50) NOT NULL DEFAULT 'ASSISTANT' AFTER topic;
