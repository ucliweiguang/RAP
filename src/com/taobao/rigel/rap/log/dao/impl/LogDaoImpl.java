package com.taobao.rigel.rap.log.dao.impl;

import java.util.Date;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import com.taobao.rigel.rap.log.bo.RapLog;
import com.taobao.rigel.rap.log.dao.LogDao;

public class LogDaoImpl extends HibernateDaoSupport implements LogDao {

	@Override
	public void createLog(RapLog log) {
		log.setOperatetime(new Date());
		getSession().save(log);		
	}	

}
