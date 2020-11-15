CREATE TABLE IF NOT EXISTS url_record (
    extension CHAR(8) PRIMARY KEY,
    url VARCHAR(250) UNIQUE,
    hits INT
);