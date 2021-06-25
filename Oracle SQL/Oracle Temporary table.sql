select instance_name from v$instance;
select * from v$session;
select * from all_db_links;


-- CREATE TEMPORARY TABLE ORACLE --

-- https://oracle-base.com/articles/misc/temporary-tables --

CREATE GLOBAL TEMPORARY TABLE TEMPORALT_123_1(
     EQUIPO VARCHAR2(100),
     CENTRAL VARCHAR2(100),
     MIGA VARCHAR2(30)
) ON COMMIT PRESERVE|DELETE ROWS;



select table_name
from all_tables
where TEMPORARY = 'Y'
and table_name like 'TEMPORALT_123_1';



select * from TEMPORALT_123_1;











