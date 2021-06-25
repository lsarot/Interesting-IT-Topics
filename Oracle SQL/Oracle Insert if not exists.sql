--INSERT IF NOT EXISTS ORACLE 11G

insert into OPT (email, campaign_id) select 'mom@cox.net',100 from dual where not exists(select * from OPT where (email ="mom@cox.net" and campaign_id =100))
