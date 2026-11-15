
CREATE TABLE liste_noel.favoris
(
    id_favoris SERIAL NOT NULL,
    id_liste SERIAL NOT NULL,
    email character varying(320) NOT NULL,
    PRIMARY KEY (id_favoris),
    CONSTRAINT id_liste_fk FOREIGN KEY (id_liste) REFERENCES liste_noel.liste (id_liste),
    CONSTRAINT email_fk FOREIGN KEY (email) REFERENCES liste_noel.compte (email)
);

ALTER TABLE liste_noel.compte ADD COLUMN activation_key VARCHAR(255);
ALTER TABLE liste_noel.compte ADD COLUMN email_verified BOOLEAN DEFAULT FALSE;