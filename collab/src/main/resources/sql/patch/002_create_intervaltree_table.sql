CREATE TABLE hibernate_sequence (next_val BIGINT);
INSERT INTO hibernate_sequence values (1);

CREATE TABLE interval_tree (id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, node_id BINARY(16) NOT NULL, low INT NOT NULL, high INT NOT NULL, tree_id VARCHAR(50) NOT NULL, tree_type VARCHAR(50) NOT NULL, PRIMARY KEY (id), INDEX node_id_idx(node_id), INDEX low_idx(low), INDEX high_idx(high), INDEX tree_id_idx(tree_id), INDEX tree_type_idx(tree_type), UNIQUE unique_node_type_idx (node_id, tree_type), CONSTRAINT FK_interval_tree_node_id FOREIGN KEY (node_id) REFERENCES party (id));