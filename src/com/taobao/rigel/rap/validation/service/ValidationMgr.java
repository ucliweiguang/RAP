package com.taobao.rigel.rap.validation.service;

import java.io.IOException;
import java.util.Map;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

/**
 * 
 * 功能描述：API数据校验服务。服务于后端API自动化测试
 * <p> 版权所有：阿里UC移动事业群--浏览器应用业务部
 * <p> 未经本公司许可，不得以任何方式复制或使用本程序任何部分 <p>
 * 
 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
 * created on: 2015-8-20
 */
public interface ValidationMgr {
	/** 
	 * 功能描述：校验指定项目对应请求路径的API的数据的合法性。
	 * 根据登记在RAP中API的jsonschema和传入的jsonData进行规则校验，返回校验结果
	 * @param projectId   项目id
	 * @param requestUrl  API的请求路径
	 * @param jsonData    要校验的数据（json格式）
	 * @return 校验结果  由code和message构成的Map，方便前端转换成json
	 * code: 200--无错误；400--测试数据为空；401--接口的jsonSchema数据为空；500-接口数据有错误
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-8-20
	 */
	public Map<String, String> validateAPIData(int projectId,String requestUrl,String jsonData) throws IOException, ProcessingException;
	/**
	 * 
	 * 功能描述：为对应的API生成json schema(无校验规则)，仅根据action的response parameters来生产
	 * @param actionId API的id
	 * @return 接口对应的json schema
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-8-20
	 */
	public String generateJsonSchema(long actionId);
	/**
	 * 
	 * 功能描述：保存对应API的数据校验Schema
	 * @param actionId API的id
	 * @param jsonSchema schema的内容
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-8-21
	 */
	public void saveJsonSchema(long actionId,String jsonSchema);
	/**
	 * 
	 * 功能描述：获取action对应的jsonschema内容
	 * @param actionId
	 * @return jsonschema内容
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-9-1
	 */
	public String getJsonSchemaByActionId(long actionId);	
	/**
	 * 
	 * 功能描述：校验jsonschema的合法性
	 * @param jsonSchema 待校验的jsonSchema
	 * @return 校验的结果.如果返回空字符串则表示没有错误
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-9-2
	 */
	public String validateJsonSchema(String jsonSchema) throws ProcessingException, IOException;
	/**
	 * 
	 * 功能描述：保存用户提交的PB协议的文本内容
	 * @param actionId 对应的API的id
	 * @param pbtxt  PB协议的文本内容
	 * @param reflag 1--request;0--response
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-10-20
	 */
	public void savePB(long actionId,String pbtxt,int reflag);
	/**
	 * 
	 * 功能描述：获取action对应的PB协议的内容
	 * @param actionId
	 * @return PB协议的内容，包含请求参数和响应参数
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-10-20
	 */
	public Map<String,String> getPB(long actionId);	
	/**
	 * 
	 * 功能描述：保存更新pb协议的内容
	 * @param actionId
	 * @param pbrequest  pb协议请求参数
	 * @param pbresponse pb协议响应参数
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-10-20
	 */
	public void updatePB(long actionId,String pbrequest,String pbresponse);
}
