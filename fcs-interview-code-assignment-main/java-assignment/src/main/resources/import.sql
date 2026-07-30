INSERT INTO store(id, name, quantityProductsInStock) VALUES (1, 'TONSTAD', 10) ON CONFLICT DO NOTHING;
INSERT INTO store(id, name, quantityProductsInStock) VALUES (2, 'KALLAX', 5) ON CONFLICT DO NOTHING;
INSERT INTO store(id, name, quantityProductsInStock) VALUES (3, 'BESTÅ', 3) ON CONFLICT DO NOTHING;
ALTER SEQUENCE store_seq RESTART WITH 4;

INSERT INTO product(id, name, stock) VALUES (1, 'TONSTAD', 10) ON CONFLICT DO NOTHING;
INSERT INTO product(id, name, stock) VALUES (2, 'KALLAX', 5) ON CONFLICT DO NOTHING;
INSERT INTO product(id, name, stock) VALUES (3, 'BESTÅ', 3) ON CONFLICT DO NOTHING;
ALTER SEQUENCE product_seq RESTART WITH 4;

INSERT INTO warehouse(id, businessUnitCode, location, capacity, stock, createdAt, archivedAt)
VALUES (1, 'MWH.001', 'ZWOLLE-001', 100, 10, '2024-07-01', null) ON CONFLICT DO NOTHING;
INSERT INTO warehouse(id, businessUnitCode, location, capacity, stock, createdAt, archivedAt)
VALUES (2, 'MWH.012', 'AMSTERDAM-001', 50, 5, '2023-07-01', null) ON CONFLICT DO NOTHING;
INSERT INTO warehouse(id, businessUnitCode, location, capacity, stock, createdAt, archivedAt)
VALUES (3, 'MWH.023', 'TILBURG-001', 30, 27, '2021-02-01', null) ON CONFLICT DO NOTHING;
ALTER SEQUENCE warehouse_seq RESTART WITH 4;

-- fulfilment_association sequence for auto-generated IDs
ALTER SEQUENCE IF EXISTS fulfilment_association_seq RESTART WITH 1;
