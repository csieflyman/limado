DROP TABLE interval_tree;
DROP TABLE dag_edge;
DROP TABLE party_rel;
DROP TABLE party;
DROP TABLE hibernate_sequence;
DELETE FROM changelog where change_number in (1, 2, 3);
