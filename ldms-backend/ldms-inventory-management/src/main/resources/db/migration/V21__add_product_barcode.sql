-- Optional product barcode for stock scanning and CSV import (EAN/UPC/custom).

ALTER TABLE product
    ADD COLUMN barcode VARCHAR(100) NULL AFTER product_code;

CREATE UNIQUE INDEX ux_product_barcode ON product (barcode);
