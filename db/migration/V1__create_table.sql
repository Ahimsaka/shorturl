CREATE TABLE IF NOT EXISTS url_record (
    url VARCHAR(250) PRIMARY KEY,
    extension CHAR(8) UNIQUE,
    hits INT
);