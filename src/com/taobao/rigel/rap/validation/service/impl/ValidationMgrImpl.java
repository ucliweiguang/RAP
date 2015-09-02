package com.taobao.rigel.rap.validation.service.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.taobao.rigel.rap.project.dao.ProjectDao;
import com.taobao.rigel.rap.validation.service.ValidationMgr;
import com.taobao.rigel.rap.project.bo.Action;
import com.taobao.rigel.rap.project.bo.Parameter;
/**
 * 
 * 功能描述：API数据校验服务的实现。服务于后端API自动化测试
 * <p> 版权所有：阿里UC移动事业群--浏览器应用业务部
 * <p> 未经本公司许可，不得以任何方式复制或使用本程序任何部分 <p>
 * 
 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
 * created on: 2015-8-20
 */
public class ValidationMgrImpl implements ValidationMgr {
	private ProjectDao projectDao;
	
	public ProjectDao getProjectDao() {
		return projectDao;
	}

	public void setProjectDao(ProjectDao projectDao) {
		this.projectDao = projectDao;
	}

	//返回数据格式：{"requestUrl:":requestUrl,"Result":[
	//{"API":action.getName(),"code":code,"resultStr":result.toString()}
	//]}
	@Override
	public String validateAPIData(int projectId, String requestUrl, String jsonData) throws IOException, ProcessingException {
		StringBuilder result = new StringBuilder();		
		if (jsonData ==null || "".equals(jsonData)) {
			result.append("测试数据为空.");
			return result.toString();
		}
		List<Action> actions = getProjectDao().getMatchedActionList(projectId, requestUrl);
		final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		for (Action action : actions){			
			if (action.getJsonschema() ==null || "".equals(action.getJsonschema())) {
				result.append("接口的jsonSchema数据为空.");
				break;
			}
			JsonNode jsonschema =JsonLoader.fromString(action.getJsonschema());
			JsonNode data = JsonLoader.fromString(jsonData);			
	        final JsonSchema schema = factory.getJsonSchema(jsonschema);
	        ProcessingReport report = schema.validate(data,true);
	        Iterator<ProcessingMessage> iter = report.iterator();
	        StringBuilder tmpResult = new StringBuilder();
	  		while (iter.hasNext()) {//封装校验错误信息
				ProcessingMessage pm = (ProcessingMessage)iter.next();
				tmpResult.append("字段'");
				tmpResult.append(pm.asJson().get("instance").findValue("pointer").asText().replace("/", ""));
				tmpResult.append("'的数据问题：");
				tmpResult.append(pm.getMessage().replace('"', '\''));
				tmpResult.append("\n");
			}
			if ("".equals(tmpResult.toString())){//校验的数据没有错误
				result.append("本接口数据没有错误。");
			} else {//校验数据有错误				
				result.append(tmpResult.toString());				
			}			
		}//结束循环
		
		return result.toString();
	}

	@Override
	public String generateJsonSchema(long actionId) {
		Action action = getProjectDao().getAction(actionId);
		Set<Parameter> parameters = action.getResponseParameterList();			
		StringBuilder schema = new StringBuilder("{");
		schema.append("\"title\":\"");
		schema.append(action.getName());
		schema.append("\", \"type\": \"object\",");
		//生成字段Schema部分
		schema.append(generateFieldsSchema(parameters));
		schema.append("}");
		
		return schema.toString();
	}
	
	//构造API字段的schema
	private String generateFieldsSchema(Set<Parameter> parameters){
		StringBuilder result = new StringBuilder("\"properties\": {");
		Iterator<Parameter> iter = parameters.iterator();
		// 必填字段
		StringBuilder requiredStr = new StringBuilder();;
		while(iter.hasNext()){
			Parameter p = iter.next();
			Set<Parameter> subparameters = p.getParameterList();
			result.append("\"");
			if (subparameters.size() > 0){//如果当前参数下还有附属参数，递归构造schema
				if ("object".equals(p.getDataType())){//对象类型字段					
					result.append(removeMockForField(p.getIdentifier()));
					result.append("\":{\"type\":\"object\",");
					result.append(generateFieldsSchema(subparameters));
					result.append("}");
				} else{//数组类型的字段
					result.append(removeMockForField(p.getIdentifier()));
					//result.append("\":{\"type\":\"array\",\"items\":{");
					result.append("\":{\"type\":\"array\",\"items\":{\"type\": \"object\",");
					result.append(generateFieldsSchema(subparameters));
					result.append("}}");
				}
				if(iter.hasNext()){
					result.append(",");	
				}
			} else {//简单字段
				//result.append("\"");
				result.append(removeMockForField(p.getIdentifier()));
				result.append("\":{\"type\":\"");
				result.append(convertDataType(p.getDataType()));
				if(iter.hasNext()){
					result.append("\"},");	
				} else {
					result.append("\"}");
				}				
			}
			//构造required字段列表串
			if(p.getRequired() > 0){
				if ("".equals(requiredStr.toString())){//第一次的时候才构造开始
					requiredStr.append(", \"required\": [");
				}
				requiredStr.append("\"");
				requiredStr.append(removeMockForField(p.getIdentifier()));
				requiredStr.append("\",");
			}			
		}//字段循环遍历结束
		
		result.append("}");//required是和properties同级的
		if (!requiredStr.toString().equals("")){			
			result.append(requiredStr.substring(0, requiredStr.length()-1));//去掉最后的逗号
			result.append("]");
		}
		
		return result.toString();
	}
	
	//由于json schema的数据类似于json接口的数据数据类型有一定差异，需转换
	private String convertDataType(String dataType){
		if (dataType.contains("array")){
			return "array";
		}else{
			return dataType;
		}		
	}
	//将字段名中"|"以及后面的内容移除
	private String removeMockForField(String field){
		if (field.contains("|")){
			return field.substring(0, field.indexOf("|"));
		} else {
			return field;
		}
	}

	@Override
	public void saveJsonSchema(long actionId, String jsonSchema) {
		Action action = getProjectDao().getAction(actionId);
		action.setJsonschema(jsonSchema);
		action.update(action);
	}

	@Override
	public String getJsonSchemaByActionId(long actionId) {
		Action action = getProjectDao().getAction(actionId);
		return action.getJsonschema();
	}

	@Override
	public String validateJsonSchema(String jsonSchema) throws ProcessingException, IOException {
		StringBuilder result = new StringBuilder();
		ValidationConfiguration vc = ValidationConfiguration.byDefault();		
		SyntaxValidator syntaxValidator = new SyntaxValidator(vc);
		JsonNode schema;
		try{
			schema = JsonLoader.fromString(jsonSchema);	
		}catch(JsonParseException je){
			result.append("json基本格式有错误，请检查。建议排查花括号、中括号、双引号、逗号等的匹配性.");
			return result.toString();
		}
		
		ProcessingReport report = syntaxValidator.validateSchema(schema);		
		System.out.println("report:" + report);
		Iterator<ProcessingMessage> iter = report.iterator();
		while (iter.hasNext()) {
			ProcessingMessage pm = (ProcessingMessage)iter.next();			
			result.append(pm.getMessage());	
			result.append("|");
		}       
		return result.toString();
	}	
}