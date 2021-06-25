
--Beginning of PL/SQL code block

DECLARE
message varchar2(20):='Hello, world!';
--declaring custom subtypes, there are several predef for PLSQL
SUBTYPE name IS char(20);
SUBTYPE surname IS varchar2(30);
lastname surname;

BEGIN
/*
*  PL/SQL executable statement
*/
dbms_output.put_line(message);
name := 'John';
lastname := 'Doe';
dbms_output.put_line('Hello' || name || lastname);

--null; //puedo usar un null como bloque si no hará nada!
EXCEPTION
--exception handling
END
/


---------------------------- NOTA:
EXCEPTION WHEN OTHERS THEN
dbms_output.put_line(SQLCODE||' - '||SUBSTR(SQLERRM,1,200));
O
insert into audit_table (error_number, error_message) values (SQLCODE, SUBSTR(SQLERRM,1,200));

---------------------------- NOTA:

EXCEPTION WHEN NO_DATA_FOUND
funciona es cuando usamos la estructura

BEGIN
  SELECT ...
  INTO xvariable;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    ...
END;

NO USANDO:
for rset in (select ...)
loop
  begin
    ...
  exception when ...  
  end;
end loop;  

---------------------------- NOTA:
SELECT column_name FROM all_tab_cols
WHERE table_name = 'nombreTabla'
AND owner = 'GIROS_OWN'
AND column_name NOT IN ('PASSWORD','VERSION','ID')



--*******************************--
--*********** EJEMPLOS:
--*******************************--

/* Formatted on 31/05/2019 12:36:36 (QP5 v5.227.12220.39754) */
-- Opción: dar permisos a tablas/vistas


--seleccionar de un string array
--select column_value TELEFONO from table(sys.dbms_debug_vc2coll('965184141','979115525','976852949'))


DECLARE
   type USUARIOARRAY IS VARRAY(3) OF VARCHAR2(20);

   v_nombres    USUARIOARRAY;
   --v_owner VARCHAR2(10) := 'GIROS_OWN';
    v_total     INTEGER;
BEGIN
   v_nombres := USUARIOARRAY('GIROS_HPSA', 'GIROS_APP');
   v_total := v_nombres.COUNT;
   
   DBMS_OUTPUT.PUT_LINE ('INICIO DEL PROCESO DE GRANTS');

   FOR r_objeto
      IN (SELECT object_name
            FROM all_objects
           WHERE     object_type IN ('PACKAGE', 'PROCEDURE', 'FUNCTION', 'TYPE')
                 AND owner = 'GIROS_OWN'
                 AND created > TO_DATE ('25/05/2019', 'dd/mm/yyyy'))
   LOOP
      BEGIN
         FOR i IN 1 .. v_total
         LOOP
            BEGIN
               EXECUTE IMMEDIATE 'GRANT EXECUTE ON GIROS_OWN.' || r_objeto.object_name || ' TO ' || v_nombres (i);
            END;
         END LOOP;
      EXCEPTION
         WHEN OTHERS
         THEN
            DBMS_OUTPUT.PUT_LINE ('Fallo el objeto ' || r_objeto.object_name);
      END;
   END LOOP;
   
   FOR r_tablas
      IN (SELECT object_name
            FROM all_objects
           WHERE     object_type IN ('TABLE','VIEW')
                 AND owner = 'GIROS_OWN'
                 AND created > TO_DATE ('25/05/2019', 'dd/mm/yyyy'))
   LOOP
      BEGIN
         FOR i IN 1 .. v_total
         LOOP
            BEGIN
               EXECUTE IMMEDIATE 'GRANT DELETE, INSERT, SELECT, UPDATE ON GIROS_OWN.' || r_tablas.object_name || ' TO ' || v_nombres (i);
            END;
         END LOOP;
      EXCEPTION
         WHEN OTHERS
         THEN
            DBMS_OUTPUT.PUT_LINE ('Fallo el objeto ' || r_tablas.object_name);
      END;
   END LOOP;
   
    FOR r_secuencias
      IN (SELECT object_name
            FROM all_objects
           WHERE     object_type IN ('SEQUENCE')
                 AND owner = 'GIROS_OWN'
                 AND created > TO_DATE ('25/05/2019', 'dd/mm/yyyy'))
   LOOP
      BEGIN
         FOR i IN 1 .. v_total
         LOOP
            BEGIN
               EXECUTE IMMEDIATE 'GRANT SELECT ON GIROS_OWN.' || r_secuencias.object_name || ' TO ' || v_nombres (i);
            END;
         END LOOP;
      EXCEPTION
         WHEN OTHERS
         THEN
            DBMS_OUTPUT.PUT_LINE ('Fallo el objeto ' || r_secuencias.object_name);
      END;
   END LOOP;
   
   COMMIT;
   DBMS_OUTPUT.PUT_LINE ('FIN DEL PROCESO DE GRANTS');
END;



--*****************************************


* INSERTAR EN UNA TABLA DATOS DESDE OTRA TABLA REMOTA
 (USANDO PL/SQL PARA RECORRER CADA REGISTRO Y EVALUAR SI EXISTE O NO USANDO UN SELECT CASE)

DECLARE 
existe number(1);
BEGIN
FOR obj
      IN (
        (SELECT VENDOR_ONT.VENDOR, MODEL_ONT.MODELNE, MF.ONU_TYPE,  MF.ONU_PORT, DECODE(MF.INTEGRADO, 1,'SI',0,'NO') as INTEGRADO
        FROM PHYS_MODELNE@PHYSIS_DBLINK MODEL_ONT
        INNER JOIN PHYS_VENDOR@PHYSIS_DBLINK VENDOR_ONT ON (MODEL_ONT.PHYS_VENDOR_ID_FK = VENDOR_ONT.PHYS_VENDOR_ID)
        INNER JOIN PHYS_MODELFTTH@PHYSIS_DBLINK MF ON (MODEL_ONT.PHYS_MODELNE_ID  = MF.PHYS_MODELNE_ID_FK)
        ))
   LOOP
      BEGIN
               
            select case 
                    when exists(
                            select * from FTTH_ONT_MODELO FT
                           where FT.RVENDOR = obj.VENDOR AND FT.MODELNE = obj.MODELNE AND FT.ONU_TYPE = obj.ONU_TYPE AND FT.ONU_PORT = obj.ONU_PORT AND FT.INTEGRADO = obj.INTEGRADO
                        )
                    then 1
                    else 0
            end into existe
            from dual;
               
               IF (existe = 1)
               
               THEN
                --DBMS_OUTPUT.PUT_LINE('EXISTE');
                null;
               ELSE
                --DBMS_OUTPUT.PUT_LINE('NO EXISTE');
                DBMS_OUTPUT.PUT_LINE(obj.VENDOR ||' '|| obj.MODELNE ||' '|| obj.ONU_TYPE ||' '|| obj.ONU_PORT ||' '|| obj.INTEGRADO);
                
               END IF;
               
      EXCEPTION
         WHEN OTHERS
         THEN
            DBMS_OUTPUT.PUT_LINE ('Fallo');
      END;
   END LOOP;

END;



