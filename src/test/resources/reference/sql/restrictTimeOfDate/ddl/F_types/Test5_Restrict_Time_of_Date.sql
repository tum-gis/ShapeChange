CREATE TABLE T5_FEATURETYPE1 (

   _ID INTEGER NOT NULL PRIMARY KEY,
   PROP1 DATE NOT NULL
);

CREATE TABLE T5_FEATURETYPE1_PROP2 (

   T5_FEATURETYPE1_ID INTEGER NOT NULL,
   PROP2 DATE NOT NULL,
   PRIMARY KEY (T5_FEATURETYPE1_ID, PROP2)
);


ALTER TABLE T5_FEATURETYPE1 ADD CONSTRAINT T5_FEATURETYP_PROP1_CK CHECK (TO_CHAR(PROP1, 'HH24:MI:SS') = '00:00:00');
ALTER TABLE T5_FEATURETYPE1_PROP2 ADD CONSTRAINT FK_T5_FEATURETYPE1_PROP2_T5_FE FOREIGN KEY (T5_FEATURETYPE1_ID) REFERENCES T5_FEATURETYPE1;
ALTER TABLE T5_FEATURETYPE1_PROP2 ADD CONSTRAINT T5_FEATURETYP_PROP2_CK CHECK (TO_CHAR(PROP2, 'HH24:MI:SS') = '00:00:00');
