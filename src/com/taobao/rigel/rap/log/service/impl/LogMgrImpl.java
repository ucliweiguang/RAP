package com.taobao.rigel.rap.log.service.impl;

import com.taobao.rigel.rap.log.bo.RapLog;
import com.taobao.rigel.rap.log.dao.LogDao;
import com.taobao.rigel.rap.log.service.LogMgr;

public class LogMgrImpl implements LogMgr {
	private LogDao logDao;
	
	public void setLogDao(LogDao logDao) {
		this.logDao = logDao;
	}

	@Override
	public void createLog(RapLog log) {
		// TODO Auto-generated method stub
		logDao.createLog(log);
	}

}
