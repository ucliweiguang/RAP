alter table tb_parameter add column required tinyint(1) default '0' comment '该字段值是否必须，0:否,1:是';
alter table tb_action add column jsonschema text comment '该接口的数据校验jsonschema内容';