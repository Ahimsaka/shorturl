CREATE TABLE users_links (
    username VARCHAR(50) REFERENCES users (username),
    extension CHAR(8) REFERENCES url_record (extension)
);