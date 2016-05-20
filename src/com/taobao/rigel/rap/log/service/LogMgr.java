package com.taobao.rigel.rap.log.service;

import com.taobao.rigel.rap.log.bo.RapLog;

/**
 * 
 * 功能描述：用户操作日志服务，用于记录、查找操作日志
 * <p> 版权所有：阿里UC移动事业群--浏览器应用业务部
 * <p> 未经本公司许可，不得以任何方式复制或使用本程序任何部分 <p>
 * 
 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
 * created on: 2016-5-19
 */
public interface LogMgr {
	/**
	 * 
	 * 功能描述：记录用户操作日志
	 * @param log 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2016-5-19
	 */
	public void createLog(RapLog log);
}
