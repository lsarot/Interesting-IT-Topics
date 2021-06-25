
/*
SELECT val FROM   (

SELECT val, rownum AS rnum FROM   (
        
        SELECT val
        FROM   table
        ORDER BY val
                
  ) WHERE rownum <= 8
        
) WHERE  rnum > 4;
*/

-------------------------------------------------

-- USO DE LIMIT Y OFFSET EN ORACLE 11 O INFERIOR (en el 12 existe: OFFSET 4 ROWS FETCH NEXT 4 ROWS ONLY)
-- BUSCAMOS REGISTROS DONDE SE REPITA EN COD_ADMIN MÁS DE 1 VEZ...

SELECT TABLA1.COD_ADMIN, COUNT(*) CANT FROM  (

            SELECT T4.* FROM (
                    SELECT T3.*, rownum RNUM FROM (

                            --ORIGINAL QUERY
                            SELECT  'FTTH Masivo' SERVICIO,'FTTH Mutualizado Datos' ACCESO,'IP dinamicas' CATEGORIA, P.PRCL_ID_SERVICIO_CRM ID_SERVICIO ,P.ID_EXTERNO_CLIENTE ID_CLIENTE, P.PRCL_USUARIO,DECODE(P.PRCL_CODIGOCLIENTEUNI2,'0','')  CIF,C.ADMINISTRATIVO COD_ADMIN,P.PRCL_TELEFONO TELEFONO,P.PRCL_MODALIDADCONEXIONPRODUCT PERFIL, P.PRCL_REMOTE_ID REMOTE_ID
                            FROM CAI_CAI C, PRCL_PRODUCTOCLIENTE P, CAIPR_CAIPRODUCT CP , FTTC_FTTHMARCA FM
                            WHERE P.PRCL_MIGRADOPHYSIS='S' 
                            AND P.PRCL_TIPOSERVICIOSIAM IN ('FTTH_MM','FTTH_VOD') 
                            AND C.CAI_MARCA_COMERCIAL=FM.FTM_ID
                            AND UPPER(FM.FTM_NOMBRE)='JAZZTEL' 
                            AND CP.PRCL_PRODUCTID=P.PRCL_ID AND C.IDENTIFIER=CP.CAI_CAID
                            UNION
                            SELECT 'FTTH Masivo' SERVICIO,'FTTH Mutualizado Datos' ACCESO,'IP dinamicas' CATEGORIA, P.PRCL_ID_SERVICIO_CRM ID_SERVICIO ,P.ID_EXTERNO_CLIENTE ID_CLIENTE, P.PRCL_USUARIO,DECODE(P.PRCL_CODIGOCLIENTEUNI2,'0','') CIF ,F.FTTH_IUA_ORANGE COD_ADMIN,P.PRCL_TELEFONO TELEFONO,P.PRCL_MODALIDADCONEXIONPRODUCT PERFIL, P.PRCL_REMOTE_ID REMOTE_ID 
                            FROM FTTH_FTTH F, PRCL_PRODUCTOCLIENTE P, FTPR_FTTHPRODUCT FP , FTTC_FTTHMARCA FM, OPERADORES_RED OPE
                            WHERE P.PRCL_MIGRADOPHYSIS='S' 
                            AND P.PRCL_TIPOSERVICIOSIAM = 'FTTH' 
                            AND FP.PRCL_PRODUCTID =P.PRCL_ID 
                            AND F.FTTH_ID = FP.FTTH_FTTHID 
                            AND F.FTTH_OPERADOR_RED= OPE.OPE_ID
                            AND UPPER(OPE.OPE_NOMBRE)='ORANGE'
                            AND F.FTTH_MARCA_COMERCIAL=FM.FTM_ID
                            AND UPPER(FM.FTM_NOMBRE)='JAZZTEL' 

                    ) T3
                    WHERE rownum <= 1000000      --LIMIT
            ) T4 WHERE rnum >= 5        --OFFSET

)  TABLA1
GROUP BY COD_ADMIN		--GROUPING TO FIND ROWS WHERE COD_ADMIN IS REPEATED
HAVING COUNT(*) > 1