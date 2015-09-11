package com.taobao.rigel.rap.validation.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
	//当出现以下错误时，要检查附加规则才能判断最终是否有问题
	//常见排查错误提示：instance type (null)|missing required properties|
	@Override
	public Map validateAPIData(int projectId, String requestUrl, String jsonData)
			throws IOException, ProcessingException {		
		Map<String, String> result = new HashMap<String, String>();
		StringBuilder message = new StringBuilder();
		if (jsonData == null || "".equals(jsonData)) {
			result.put("code", "400");
			result.put("message", "测试数据为空.");			
			return result;
		}
		//long start = System.currentTimeMillis();
		//List<Action> actions = getProjectDao().getMatchedActionList(projectId, requestUrl);//1256ms
		Action action = projectDao.getActionByUrlAndProjectid(projectId, requestUrl);
		
		//System.out.println(action.getId());
		final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		//for (Action action : actions) {
			if (action.getJsonschema() == null || "".equals(action.getJsonschema())) {
				//message.append("接口的jsonSchema数据为空.");
				result.put("code", "401");
				result.put("message", "接口的jsonSchema数据为空.");
				//break;
				return result;
			}
			JsonNode jsonschema = JsonLoader.fromString(action.getJsonschema());
			JsonNode data = JsonLoader.fromString(jsonData);
			final JsonSchema schema = factory.getJsonSchema(jsonschema);
			ProcessingReport report = schema.validate(data, true);
			Iterator<ProcessingMessage> iter = report.iterator();
			StringBuilder tmpResult = new StringBuilder();
			while (iter.hasNext()) {//封装校验错误信息
				ProcessingMessage pm = (ProcessingMessage) iter.next();
				String field = pm.asJson().get("instance").findValue("pointer").asText();

				//获取参数对应的附加规则
				int error = 0;
				String[] rules = null;
				//遍历规则，看是否可处理默认的错误信息	
				//System.out.println("pm.getMessage():" + pm.getMessage());
				if (pm.getMessage().indexOf("instance type (null)") != -1) {
					rules = getRules(action.getJsonschema(), field);
					if (rules != null){
						for (int i = 0; i < rules.length; i++) {
							if ("NOT NULL".equals(rules[i])) {//只有找到不可为空的规则才能标记为错的
								error = 1;
							}
						}	
					}			
					if (error == 0)
						continue;
				} else if (pm.getMessage().indexOf("missing required properties") != -1){  //必须（是否返回显示）字段
					//处理depend类的规则。即返回数据的部分结构由某些字段值决定
					error = validateRequiredProperties(pm.getMessage(), field, action.getJsonschema(), jsonData);				
					
					if (error == 0)
						continue;
				} else if (pm.getMessage().indexOf("instance type") != -1){//任何类型不匹配的情况，如果出现，则可忽略(这种情况只用于处理变态的不定类型)
					error = 1;
					rules = getRules(action.getJsonschema(), field);
					if (rules != null){
						for (int i = 0; i < rules.length; i++) {
							if ("ANYTYPE".equals(rules[i])) {//只有找到不可为空的规则才能标记为错的
								error = 0;
							}
						}
					}			
					if (error == 0)
						continue;
				}
				
				tmpResult.append("字段'");
				tmpResult.append(field);
				tmpResult.append("'的数据问题：");
				tmpResult.append(pm.getMessage().replace('"', '\''));
				tmpResult.append("|");
			}
			if ("".equals(tmpResult.toString())) {//校验的数据没有错误
				result.put("code", "200");
				result.put("message", "本接口的数据未发现错误。");
				return result;
				//message.append("本接口的数据未发现错误。");
			} else {//校验数据有错误		
				result.put("code", "500");
				message.append(tmpResult.toString());
			}
		//}//结束循环
		result.put("message", message.toString());
		
		return result;
	}
	
	private int validateRequiredProperties(String message,String field,String jsonSchema,String jsonData) throws IOException{
		int error = 0;
		String requiredField = message.substring(42);
		requiredField = requiredField.substring(0, requiredField.length()-3);					;
		String[] rules = getRules(jsonSchema, field + "/"+ requiredField);
		int required =0;
		int existdepend = 0;//是否出现过depend规则
		if (rules != null){
			for (int i = 0; i < rules.length; i++) {
				if (rules[i].indexOf("depend") !=-1){
					existdepend = 1;
					String dependField = rules[i].substring(7, rules[i].indexOf("="));
					String dependValue = rules[i].substring(rules[i].indexOf("=")+1, rules[i].length());
					//找到对应dependFiled真实的值，以便作比较
					//System.out.println("depend path:" + field+ "/"+ dependField);
					String realValue = getRealdata( jsonData, field+ "/"+ dependField);
					//如果比较dependValue和真实的值匹配，则该字段需要校验(error=1)，否则可不校验该字段（error=0）
					if (dependValue.equals(realValue)){
						error = 1;
					}
				}
				if ("NOT NULL".equals(rules[i])) {//只有找到不可为空的规则才能标记为错的
					required = 1;
				}
			}
			//如果是符合字段出现的条件，并且是必填的
			// 该字段依赖于某字段的值并且是非空 || 该字段只是单纯非空
			if( (error ==1 && required ==1) || (existdepend == 0 && required==1)){
				error =1 ;
			}else{
				error =0;
			}
		}
		return error;
	}
	//获取字段的附加校验规则
	private String[] getRules(String jsonSchema, String field) throws IOException {
		//System.out.println("orginal fields:" + field);
		//处理有属性刚好也叫items
		field = field.replaceAll("items", "tempi");					
					
		JsonNode jsonNode = JsonLoader.fromString(jsonSchema);
		String[] tmps = field.substring(1).split("/");
		for(int i=0;i<tmps.length;i++){
			if (isNumeric(tmps[i])){
				tmps[i] = "items";
			}			
		}
		StringBuilder newField = new StringBuilder();
		for(int i=0;i<tmps.length;i++){
			if(i==tmps.length-1 && "items".equals(tmps[tmps.length-1])){
				break;
			}
			newField.append("/");
			newField.append(tmps[i]);
			
		}
		//System.out.println("newField:" + newField.toString());
		field = newField.toString().substring(0, newField.toString().length()).replaceAll("/", "/properties/").replaceAll("properties/items", "items").substring(1);
		
		//还原被处理过的特殊字段：items
		field = field.replaceAll("tempi", "items");
		
		String[] fields = field.split("/");
		//System.out.println("field:"+ field);
		for (int i = 0; i < fields.length; i++) {
			//System.out.println("fields[i]:"+ fields[i]);
			jsonNode = jsonNode.get(fields[i]);
			//System.out.println("jsonNode:"+ jsonNode);
		}
		if (jsonNode.get("description")==null){
			return null;
		}				
		//字段对应的附加规则从jsonschem的对应字段的description获取
		String description = jsonNode.get("description").toString();
		if (description.indexOf("@v")==-1){
			return null;
		}
		String ruleStr = description.substring(description.indexOf("@v"), description.length()-1)
				.substring(3);
		return ruleStr.split(";");

	}
	
	//获取指定路径下字段的值
	private String getRealdata(String jsonData,String field) throws IOException{
		JsonNode jsonNode = JsonLoader.fromString(jsonData);
		String[] fields = field.substring(1).split("/");
		for (int i=0;i<fields.length;i++){
			if (isNumeric(fields[i])){
				jsonNode = jsonNode.get(Integer.parseInt(fields[i]));
			}else{
				jsonNode = jsonNode.get(fields[i]);
			}
		}
		return jsonNode.toString();
	}
	
	private  boolean isNumeric(String str) {
		for (int i = str.length(); --i >= 0;) {
			if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
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
	private String generateFieldsSchema(Set<Parameter> parameters) {
		StringBuilder result = new StringBuilder("\"properties\": {");
		Iterator<Parameter> iter = parameters.iterator();
		// 必填字段
		StringBuilder requiredStr = new StringBuilder();
		;
		while (iter.hasNext()) {
			Parameter p = iter.next();
			Set<Parameter> subparameters = p.getParameterList();
			result.append("\"");
			if (subparameters.size() > 0) {//如果当前参数下还有附属参数，递归构造schema
				if ("object".equals(p.getDataType())) {//对象类型字段					
					result.append(removeMockForField(p.getIdentifier()));
					result.append("\":{\"type\":\"object\",");
					result.append(generateFieldsSchema(subparameters));
					result.append("}");
				} else {//数组类型的字段
					result.append(removeMockForField(p.getIdentifier()));
					//result.append("\":{\"type\":\"array\",\"items\":{");
					result.append("\":{\"type\":\"array\",\"items\":{\"type\": \"object\",");
					result.append(generateFieldsSchema(subparameters));
					result.append("}}");
				}
				if (iter.hasNext()) {
					result.append(",");
				}
			} else {//简单字段,description字段用于保存可能存在的附加规则
				//result.append("\"");
				result.append(removeMockForField(p.getIdentifier()));
				result.append("\":{\"type\":\"");
				result.append(convertDataType(p.getDataType()));
				result.append("\",\"description\":\"");
				result.append(p.getName());
				if (iter.hasNext()) {
					result.append("\"},");
				} else {
					result.append("\"}");
				}
			}
			//构造required字段列表串
			if (p.getName().indexOf("NOT NULL") != -1) {
				if ("".equals(requiredStr.toString())) {//第一次的时候才构造开始
					requiredStr.append(", \"required\": [");
				}
				requiredStr.append("\"");
				requiredStr.append(removeMockForField(p.getIdentifier()));
				requiredStr.append("\",");
			}
		}//字段循环遍历结束

		result.append("}");//required是和properties同级的
		if (!requiredStr.toString().equals("")) {
			result.append(requiredStr.substring(0, requiredStr.length() - 1));//去掉最后的逗号
			result.append("]");
		}

		return result.toString();
	}

	//由于json schema的数据类似于json接口的数据数据类型有一定差异，需转换
	private String convertDataType(String dataType) {
		if (dataType.contains("array")) {
			return "array";
		} else {
			return dataType;
		}
	}

	//将字段名中"|"以及后面的内容移除
	private String removeMockForField(String field) {
		if (field.contains("|")) {
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
		try {
			schema = JsonLoader.fromString(jsonSchema);
		} catch (JsonParseException je) {
			result.append("json基本格式有错误，请检查。建议排查花括号、中括号、双引号、逗号等的匹配性.");
			return result.toString();
		}

		ProcessingReport report = syntaxValidator.validateSchema(schema);
		//System.out.println("report:" + report);
		Iterator<ProcessingMessage> iter = report.iterator();
		while (iter.hasNext()) {
			ProcessingMessage pm = (ProcessingMessage) iter.next();
			result.append(pm.getMessage() + " 字段路径：" + pm.asJson().get("schema").get("pointer"));
			result.append("|");
		}
		return result.toString();
	}
}