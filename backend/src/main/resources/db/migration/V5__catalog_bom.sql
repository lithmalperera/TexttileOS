-- Catalog area: simple BOM. One BOM per product in the MVP; production copies
-- its lines into a snapshot, so the BOM itself can be edited freely.

CREATE TABLE bom (
    id         UUID PRIMARY KEY,
    product_id UUID NOT NULL UNIQUE REFERENCES product (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE bom_item (
    id                       UUID PRIMARY KEY,
    bom_id                   UUID NOT NULL REFERENCES bom (id),
    material_id              UUID NOT NULL REFERENCES material (id),
    quantity_per_product_unit NUMERIC(19, 6) NOT NULL,
    CONSTRAINT bom_item_quantity_check CHECK (quantity_per_product_unit > 0),
    CONSTRAINT bom_item_unique_material UNIQUE (bom_id, material_id)
);

CREATE INDEX idx_bom_item_bom_id ON bom_item (bom_id);
CREATE INDEX idx_bom_item_material_id ON bom_item (material_id);
