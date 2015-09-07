package com.taobao.rigel.rap.api.service;

import com.taobao.rigel.rap.project.bo.Action;

public interface OpenAPIMgr {

	/**
	 * get model JSON text
	 * 
	 * @param projectId
     * @param ver optional
	 * @return
	 * @throws Exception
	 */
	Object getModel(int projectId, String ver) throws Exception;

	/**
	 * get JSON Schema text of action
	 * 
	 * @param actionId
	 * @param type
     * @param ver optional
	 * @return
	 */
	//Object getSchema(int actionId, Action.TYPE type, String ver, int projectId);
	
	//redefined by liweiguang 2015-9-6
	/**
	 * 
	 * 功能描述：通过actionId获取对应的jsonschema的内容
	 * @param actionId
	 * @return jsonschema的内容
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-9-6
	 */
	String getSchema(int actionId);
	/**
	 * modify mock rules
	 *
	 * @param rules mock rules
	 * @param actionId action id
	 * @return JSON, contains isOk/msg properties
	 */
	String modifyMockRules(String rules, int actionId);

	/**
	 * reset(delete) mock rules
	 *
	 * @param actionId action id
	 * @return JSON, contains isOk/msg properties
	 */
	String resetMockRules(int actionId);
}
