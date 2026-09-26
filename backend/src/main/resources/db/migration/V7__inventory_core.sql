-- Inventory area: one logical location, stockable items, current balances,
-- and the append-only transaction/movement ledger.
-- The MAIN location is seeded as fixed reference data like roles were.

CREATE TABLE stock_location (
    id         UUID PRIMARY KEY,
    code       VARCHAR(50) NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO stock_location (id, code, name) VALUES
    ('66666666-6666-6666-6666-666666666666', 'MAIN', 'Main Warehouse');

CREATE TABLE inventory_item (
    id                UUID PRIMARY KEY,
    stock_location_id UUID NOT NULL REFERENCES stock_location (id),
    item_kind         VARCHAR(20) NOT NULL,
    material_id       UUID REFERENCES material (id),
    product_id        UUID REFERENCES product (id),
    unit              VARCHAR(20) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT inventory_item_kind_check CHECK (item_kind IN ('MATERIAL', 'PRODUCT')),
    CONSTRAINT inventory_item_shape_check CHECK (
        (item_kind = 'MATERIAL' AND material_id IS NOT NULL AND product_id IS NULL)
        OR
        (item_kind = 'PRODUCT' AND product_id IS NOT NULL AND material_id IS NULL)
    )
);

CREATE UNIQUE INDEX uq_inventory_item_material
    ON inventory_item (stock_location_id, material_id) WHERE material_id IS NOT NULL;
CREATE UNIQUE INDEX uq_inventory_item_product
    ON inventory_item (stock_location_id, product_id) WHERE product_id IS NOT NULL;

CREATE TABLE stock_balance (
    id                UUID PRIMARY KEY,
    inventory_item_id UUID NOT NULL UNIQUE REFERENCES inventory_item (id),
    on_hand_quantity  NUMERIC(19, 6) NOT NULL DEFAULT 0,
    reserved_quantity NUMERIC(19, 6) NOT NULL DEFAULT 0,
    version           BIGINT NOT NULL DEFAULT 0,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT stock_balance_on_hand_check CHECK (on_hand_quantity >= 0),
    CONSTRAINT stock_balance_reserved_check CHECK (reserved_quantity >= 0)
);

CREATE TABLE inventory_transaction (
    id              UUID PRIMARY KEY,
    transaction_type VARCHAR(30) NOT NULL,
    reference_type  VARCHAR(50),
    reference_id    UUID,
    reason          VARCHAR(500) NOT NULL,
    actor_user_id   UUID REFERENCES app_user (id),
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT inventory_transaction_type_check CHECK (
        transaction_type IN ('RECEIPT', 'ADJUSTMENT', 'RESERVATION', 'RELEASE', 'CONSUMPTION', 'PRODUCTION_RECEIPT')
    )
);

CREATE INDEX idx_inventory_transaction_reference
    ON inventory_transaction (reference_type, reference_id);

CREATE TABLE inventory_movement (
    id                       UUID PRIMARY KEY,
    inventory_transaction_id UUID NOT NULL REFERENCES inventory_transaction (id),
    inventory_item_id        UUID NOT NULL REFERENCES inventory_item (id),
    on_hand_delta            NUMERIC(19, 6) NOT NULL,
    reserved_delta           NUMERIC(19, 6) NOT NULL
);

CREATE INDEX idx_inventory_movement_item ON inventory_movement (inventory_item_id);
