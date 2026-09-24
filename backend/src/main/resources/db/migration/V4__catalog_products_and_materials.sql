-- Catalog area: products and materials.
-- Referenced records are archived, never deleted; the archive timestamp records
-- when the record became unusable for new business operations.

CREATE TABLE product (
    id              UUID PRIMARY KEY,
    code            VARCHAR(50) NOT NULL,
    code_normalized VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    category        VARCHAR(50),
    description     VARCHAR(500),
    output_unit     VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at     TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT product_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT product_output_unit_check CHECK (output_unit IN ('PIECE', 'METER', 'KILOGRAM'))
);

CREATE TABLE material (
    id              UUID PRIMARY KEY,
    code            VARCHAR(50) NOT NULL,
    code_normalized VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    material_type   VARCHAR(50),
    base_unit       VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at     TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT material_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT material_base_unit_check CHECK (base_unit IN ('PIECE', 'METER', 'KILOGRAM'))
);

CREATE INDEX idx_product_status ON product (status);
CREATE INDEX idx_material_status ON material (status);
