CREATE TABLE party (id BINARY(16) NOT NULL PRIMARY KEY, identity VARCHAR(20) NOT NULL, type VARCHAR(20) NOT NULL, name VARCHAR(50) NOT NULL, email VARCHAR(50) NULL, enabled BIT(1) NOT NULL, creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, modification_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP);

-- CREATE TRIGGER TRIG BEFORE INSERT ON party REFERENCING NEW ROW AS NEW FOR EACH ROW SET NEW.uuid = UUID();
CREATE UNIQUE INDEX unique_type_identity_idx ON party (type, identity);
CREATE INDEX identity_idx ON party (identity);
CREATE INDEX name_idx ON party (name);
CREATE INDEX type_idx ON party (type);
CREATE INDEX enabled_idx ON party (enabled);

CREATE TABLE party_rel (parent BINARY(16) NOT NULL, children BINARY(16) NOT NULL, CONSTRAINT PK_party_rel PRIMARY KEY (parent, children), CONSTRAINT FK__party_rel_parent FOREIGN KEY (parent) REFERENCES party (id), CONSTRAINT FK__party_rel_children FOREIGN KEY (children) REFERENCES party (id));

CREATE INDEX parent_idx ON party_rel (parent);
CREATE INDEX children_idx ON party_rel (children);