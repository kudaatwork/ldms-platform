CREATE TABLE organization_contracted_clearing_agents (
    supplier_id BIGINT NOT NULL,
    clearing_agent_id BIGINT NOT NULL,
    PRIMARY KEY (supplier_id, clearing_agent_id),
    KEY idx_occa_clearing_agent (clearing_agent_id),
    CONSTRAINT fk_occa_supplier FOREIGN KEY (supplier_id) REFERENCES organization (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_occa_clearing_agent FOREIGN KEY (clearing_agent_id) REFERENCES organization (id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
