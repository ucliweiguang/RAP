package com.taobao.rigel.rap.validation.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
import com.taobao.rigel.rap.common.HTTPUtils;
import com.taobao.rigel.rap.mock.service.MockMgr;
import com.taobao.rigel.rap.project.dao.ProjectDao;
import com.taobao.rigel.rap.project.service.ProjectMgr;
import com.taobao.rigel.rap.validation.service.ValidationMgr;
import com.taobao.rigel.rap.project.bo.Action;
import com.taobao.rigel.rap.project.bo.CommonModel;
import com.taobao.rigel.rap.project.bo.CommonModelField;
import com.taobao.rigel.rap.project.bo.Parameter;
import com.taobao.rigel.rap.project.bo.Project;

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
	//@param url参数  
	private static final String TAG_PARAM = "@param";
	//@json body参数
	private static final String TAG_JSON = "@json";

	//@header header参数
	private static final String TAG_HEADER = "@header";
	//@form form-data参数
	private static final String TAG_FORMDATA = "@form";
			
	private ProjectDao projectDao;
	private ProjectMgr projectMgr;
	private MockMgr mockMgr;
	private String rapdomain;

	public String getRapdomain() {
		return rapdomain;
	}

	public void setRapdomain(String rapdomain) {
		this.rapdomain = rapdomain;
	}

	public ProjectDao getProjectDao() {
		return projectDao;
	}

	public void setProjectDao(ProjectDao projectDao) {
		this.projectDao = projectDao;
	}

	public ProjectMgr getProjectMgr() {
		return projectMgr;
	}
	public void setProjectMgr(ProjectMgr projectMgr) {
		this.projectMgr = projectMgr;
	}
	public void setMockMgr(MockMgr mockMgr) {
		this.mockMgr = mockMgr;
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
		
		//System.out.println("actionid:"+action.getId());
		final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		//for (Action action : actions) {
			if (action.getJsonschema() == null || "".equals(action.getJsonschema())) {
				//message.append("接口的jsonSchema数据为空.");
				result.put("code", "401");
				result.put("message", "接口的jsonSchema数据为空.");
				//break;
				return result;
			}
			
			JsonNode jsonschema = JsonLoader.fromString(action.getJsonschema().replaceAll("\\\\", "\\\\\\\\"));
			//System.out.println("getJsonschema:" + action.getJsonschema());
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
				result.put("message", "本接口数据结构和字段规则校验完毕，未发现问题。");
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
	
	//将name含有"@model(package)"的字段转换成公共模型的字段
	private Set<Parameter> unionModelParameters(Set<Parameter> parameters, int projectId){
		Set<Parameter> result = new HashSet<Parameter>();
		Set<Parameter> temp = parameters;
		
		Iterator<Parameter> iter = temp.iterator();
		while (iter.hasNext()) {
			Parameter p = iter.next();
			
			if (p.getName().contains("@model")){
				String tmp = p.getName().substring(p.getName().indexOf("@model("));
				String modelCode = tmp.substring(7, tmp.indexOf(")"));
				CommonModel commonModel = projectMgr.getCommonModelByCode(projectId, modelCode);
				List<CommonModelField> fields = commonModel.getCommonModelFieldListOrdered();
				Set<Parameter> paralist = new HashSet<Parameter>();  //用于保存模型字段
				for(CommonModelField field : fields){
					Parameter para = new Parameter();
					para.setIdentifier(field.getIdentifier());
					if (field.getDescription() == null){
						para.setName(field.getIdentifier());
					} else {
						para.setName(field.getDescription().replaceAll("\n", ""));
					}
					
					if (field.getDatatype().equals("int")||field.getDatatype().equals("long")||
							field.getDatatype().equals("double")||field.getDatatype().equals("float")){
						para.setDataType("number");
					} else if (field.getDatatype().equals("string")){
						para.setDataType("string");
					} else if (field.getDatatype().equals("boolean")){
						para.setDataType("boolean");
					} else {
						para.setDataType("string");
					}
					paralist.add(para);
				}
				p.setParameterList(paralist);
				result.add(p);
				continue;
			}			
			
			if(p.getParameterList().size()>0){
				p.setParameterList(unionModelParameters(p.getParameterList(), projectId));				
			} 
			result.add(p);
			
		}
		return result;
	}
	
	@Override
	public String generateJsonSchema(long actionId, int projectId) {
		Action action = getProjectDao().getAction(actionId);
		Set<Parameter> parameters = action.getResponseParameterList();
		//System.out.println("actionid:"+actionId);
		Set<Parameter> newParameters = unionModelParameters(parameters, projectId);
		StringBuilder schema = new StringBuilder("{");
		schema.append("\"title\":\"");
		schema.append(action.getName());
		schema.append("\", \"type\": \"object\",");
		//生成字段Schema部分
		schema.append(generateFieldsSchema(newParameters));
		schema.append("}");

		return schema.toString();
	}

	//构造API字段的schema
	private String generateFieldsSchema(Set<Parameter> parameters) {
		StringBuilder result = new StringBuilder("\"properties\": {");
		Iterator<Parameter> iter = parameters.iterator();
		// 必填字段
		StringBuilder requiredStr = new StringBuilder();
		
		while (iter.hasNext()) {
			Parameter p = iter.next();
			Set<Parameter> subparameters = p.getParameterList();
			result.append("\"");
			if (subparameters.size() > 0) {//如果当前参数下还有附属参数，递归构造schema
				if ("object".equals(p.getDataType())) {//对象类型字段					
					result.append(removeMockForField(p.getIdentifier()));
					result.append("\":{\"type\":\"object\",");
					result.append("\"description\":\"");
					result.append(p.getName());
					result.append("\",");
					result.append(generateFieldsSchema(subparameters));
					result.append("}");
				} else {//数组类型的字段
					result.append(removeMockForField(p.getIdentifier()));
					//result.append("\":{\"type\":\"array\",\"items\":{");
					result.append("\":{\"type\":\"array\",");
					
					//插入校验表达式，如"minimum": 18,"maximum": 60"
					String description = p.getName();
					String extra = getExtraRuleExpression(description);
					if (!"".equals(extra)){				
						result.append(extra);
						result.append(",");
						//将extra##的内容去掉
						description = description.substring(0,description.indexOf("extra#")-1);
						//如果description没有其他自定义规则，将最右边的@v也去掉
						if ("@v".equals(description.substring(description.length()-2))){
							description = description.substring(0, description.length()-2);
						}					
					}
					//插入校验表达式结束					
					result.append("\"description\":\"");
					result.append(description);
					result.append("\",");
					
					result.append("\"items\":{\"type\": \"object\",");					
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
				result.append("\",");
				//插入校验表达式，如"minimum": 18,"maximum": 60"
				String description = p.getName();
				String extra = getExtraRuleExpression(description);
				if (!"".equals(extra)){				
					result.append(extra);
					result.append(",");
					//将extra##的内容去掉
					description = description.substring(0,description.indexOf("extra#")-1);
					//如果description没有其他自定义规则，将最右边的@v也去掉
					if ("@v".equals(description.substring(description.length()-2))){
						description = description.substring(0, description.length()-2);
					}					
				}
				//插入校验表达式结束
				result.append("\"description\":\"");
				result.append(description);
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
	//获取额外的校验表达式，比如"minimum": 18,"maximum": 60,"exclusiveMinimum": true"
	//方便用户在友好的界面定义校验表达式，该内容将写入jsonschema对应的字段中
	private String getExtraRuleExpression(String description){		
		String result = "";
		if(description.indexOf("extra#")==-1){
			return result;
		}
		String rule = description.substring(description.indexOf("@v"), description.length()).substring(3);
		String[] rules = rule.split(";");		
		for(int i=0;i<rules.length;i++){
			if(rules[i].indexOf("extra#")==0){
				result = rules[i].substring(5,rules[i].length()).replace("#", "");				
				//System.out.println(result);
				break;
			}
		}
		return result;
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
		
		//自定义数据类型校验
		result.append(validateDIYDataType(jsonSchema));
		
		return result.toString();
	}
	
	//自定义数据类型校验开始
	private String validateDIYDataType(String jsonSchema) throws IOException{		
		//System.out.println(jsonSchema);
		if(jsonSchema.contains("@datatype")==false){//如果校验规则不包含自定义类型，则马上返回
			return "";
		}
		JsonNode schema = JsonLoader.fromString(jsonSchema);
		JsonNode properties =  schema.get("properties");
		Iterator<String> fields = properties.fieldNames();
		
		String msg = getValidateMsg(properties,fields);
		//System.out.println("msg:"+msg);
		return msg;
	}
	
	private String getValidateMsg(JsonNode jsonNode,Iterator<String> fields){
		StringBuilder msg = new StringBuilder();
		Iterator<JsonNode> iter = jsonNode.elements();
		while(iter.hasNext()){
			JsonNode node = iter.next();
			String field = fields.next();			
			//System.out.println(field +":"+node);
			String type = node.get("type").textValue();
			if("array".equals(type)){//如果当前node还有子对象
				JsonNode subNode = node.get("items").get("properties");				
				Iterator<String> subfields = subNode.fieldNames();
				msg.append(getValidateMsg(subNode,subfields));
			}
			String description = node.get("description").textValue();
			//System.out.println(type+":"+description);
			//检查description字段是否含有自定义数据类型，如果有，提取自定义数据类型，并与datatype对比
			if (description.contains("@datatype")){
				String datatype = description.substring(10, description.length()-1);//自定义数据类型
				//System.out.println("datatype:"+datatype);
				if (isIntType(datatype)){//如果自定义的数据类型是数字类型，则检查type是否number
					if(!"number".equals(type)){//如果不是
						msg.append("字段:");
						msg.append(field);
						msg.append("的数据类型与自定义声明的类型");
						msg.append(datatype);
						msg.append("不匹配。|");
					}
				}
			}
		}
		return msg.toString();
	}
	
	//判断自定义数据类型是否数字类型.如果是，返回true;否，返回false
	private boolean isIntType(String datatype){
		boolean result = false;
		for (String type : INT_TYPES){
			if (type.equals(datatype)){
				return true;
			}
		}
		return result;
	}
	//自定义数据类型：数字类型
	private static final String[] INT_TYPES = {"uint8_t","uint16_t","uint32_t"}; 	
	//////////////////////自定义数据类型校验结束////////////////////////
	
	public void savePB(long actionId,String pbtxt,int reflag){
		Action action = getProjectDao().getAction(actionId);
		if(reflag==1){
			action.setPbrequest(pbtxt);
		}else{
			action.setPbresponse(pbtxt);
		}
		action.update(action);
	}
	
	public  Map<String,String> getPB(long actionId){
		Map<String,String> result = new HashMap<String,String>();
		Action action = getProjectDao().getAction(actionId);
		result.put("pbrequest", action.getPbrequest());
		result.put("pbresponse", action.getPbresponse());
		return result;
	}
	
	public void updatePB(long actionId,String pbrequest,String pbresponse){
		Action action = getProjectDao().getAction(actionId);
		action.setPbrequest(pbrequest);
		action.setPbresponse(pbresponse);
		action.update(action);
	}

	/////////////////////////////////////////////////////////
	@Override	
	public String generateCURL(long actionId) {
		Action action = getProjectDao().getAction(actionId);
		Set<Parameter> parameters = action.getRequestParameterList();
		String path = action.getRequestUrl(); //接口请求路径
		//接口类型：1-GET,2-POST,3-PUT,4-DELETE,5-PATCH,6-COPY
		String requestType = action.getRequestType();
		
		StringBuilder cURL = new StringBuilder("curl 'http://yourdomain.com/");
		if (path.startsWith("/")){
    		path = path.substring(1);
    	} 
		cURL.append(path);
		//遍历请求参数（递归），解析那几个自定义标签，以便组合curl内容串
		cURL.append(constructCURL(requestType,parameters));
		cURL.append(" --compressed");
		return cURL.toString();
	}

	//构造curl内容串	
	private String constructCURL(String requestType,Set<Parameter> parameters){
		if("1".equals(requestType)|| "6".equals(requestType)){ //GET和PATCH方法
			return constructCURLForGET(parameters);
		} else {
			return constructCURLForPOST(parameters,requestType);
		}
	}
	
	//适合于GET,COPY类型的请求
	//@param url参数 @json body参数 @header header参数 @form form-data参数
	private String constructCURLForGET(Set<Parameter> parameters){
		StringBuilder params = new StringBuilder("?");
		StringBuilder headers = new StringBuilder();
		Iterator<Parameter> iter = parameters.iterator();
		int i = 0;
		while (iter.hasNext()) {
			Parameter p = iter.next();
			if(p.getName().contains(TAG_HEADER)){
				headers.append(constructHeaderStr(p));				
			} else {  //构造其他常规的param参数
				if (i > 0){
					params.append("&");
				}
				params.append(p.getIdentifier());
				params.append("=");
				params.append(getValueFromParamName(p.getName(),p.getName().indexOf(TAG_PARAM)+6));
				i++;
			}						
		}
		params.append("'");
		return params.toString() + headers.toString();
	}
	
	//从参数的名称字段获取对应的值，如从字段Accept-Language获取"@header{zh-CN,zh;q=0.8}"
	private String getValueFromParamName(String name, int beginIndex){
		if (!name.contains(TAG_JSON)&&!name.contains(TAG_PARAM)&&!name.contains(TAG_HEADER)){
			return "mockvalue";
		}
		StringBuilder value =new StringBuilder(); 
		String tmp = name.substring(beginIndex);
		if (tmp.startsWith("{")){
			char[] cs = tmp.toCharArray();
			for (char c : cs ){
				if (c =='{') continue;
				if (c == '}') break;
				value.append(c);
			}
		} else {
			return "mockvalue";
		}
		return value.toString();
	}
	//构造带有@header标签参数的Header字符串
	private String constructHeaderStr(Parameter p){
		StringBuilder header = new StringBuilder();
		header.append(" -H '");
		header.append(p.getIdentifier());
		header.append(":");
		header.append(getValueFromParamName(p.getName(),p.getName().indexOf(TAG_HEADER)+7));
		header.append("'");
		return header.toString();
	}
	
	//构造带有@param标签参数的附加在url参数字符串
	private String constructParamStr(Parameter p){
		StringBuilder param = new StringBuilder();
		param.append(p.getIdentifier());
		param.append("=");
		param.append(getValueFromParamName(p.getName(),p.getName().indexOf(TAG_PARAM)+6));
		return param.toString();
	}
	
	//适合与POST,PUT,PATCH，DELETE类型的请求
	private String constructCURLForPOST(Set<Parameter> parameters,String requestType){
		Iterator<Parameter> iter = parameters.iterator();
		StringBuilder cURL = new StringBuilder();
		StringBuilder headers = new StringBuilder();
		StringBuilder params = new StringBuilder("?");
		StringBuilder datas = new StringBuilder(" --data-binary $'{");
		int i = 0;
		//boolean first = true;
		while (iter.hasNext()) {
			Parameter p = iter.next();
			//Set<Parameter> subparameters = p.getParameterList();
			if(p.getName().contains(TAG_HEADER)){
				headers.append(constructHeaderStr(p));				
			} else if(p.getName().contains(TAG_PARAM)){
				if (i > 0){
					params.append("&");
				}
				params.append(constructParamStr(p));
				i++;				
			} else {  //body(json) param参数
				//datas.append(constructBodyJsonStr(p,first));
				//first = false;
			}
		}
		//独立循环递归构建json的串
		datas.append(constructBodyJsonStr(parameters));
		
		params.append("'");
		datas.append("}'");
		
		cURL.append(params.toString()); //url参数
		//插入方法标识，如 -X DELETE 
		//接口类型：2-POST,3-PUT,4-DELETE,5-PATCH
		if ("3".equals(requestType)){
			cURL.append(" -X PUT ");
		} else if ("4".equals(requestType)){
			cURL.append(" -X DELETE ");
		} else if ("5".equals(requestType)){
			cURL.append(" -X PATCH ");
		}
		cURL.append(headers.toString());//header参数
		cURL.append(datas.toString());//body 参数
		return cURL.toString();
	}
	
	//构造带有@json标签参数的参数结构体
	//@first 是否第一个参数，也就是循环中的第一个元素
	private String constructBodyJsonStr(Set<Parameter> parameters){
		Iterator<Parameter> iter = parameters.iterator();
		StringBuilder datas = new StringBuilder();
		while (iter.hasNext()) {
			Parameter p = iter.next();
			String paraName = p.getName();
			if (paraName.contains(TAG_HEADER) || paraName.contains(TAG_PARAM) || paraName.contains(TAG_FORMDATA)){
				continue;  //如果参数不是json类型的就跳过
			}
			Set<Parameter> subparameters = p.getParameterList();
			if (subparameters.size() > 0) {//如果当前参数下还有子参数，递归构造
				datas.append("\"");
				datas.append(p.getIdentifier());
				datas.append("\":{");
				datas.append(constructBodyJsonStr(subparameters));
				datas.append("},");
			} else {
				datas.append("\"");
				datas.append(p.getIdentifier());
				datas.append("\":");
				if ("string".equals(p.getDataType())){
					datas.append("\"");
				}
				datas.append(getValueFromParamName(paraName,paraName.indexOf(TAG_JSON)+5));
				if ("string".equals(p.getDataType())){
					datas.append("\"");
				}
				datas.append(",");
			}	
		}
		//考虑去掉最后一个逗号
		String result = datas.toString();
		if ("".equals(result)) return "";
		
		return result.substring(0, result.length()-1);
	}
	
	@Override
	public void saveCURL(long actionId, String cURL) {
		//System.out.println("cURL:" + cURL);
		Action action = getProjectDao().getAction(actionId);
		action.setcURL(cURL);
		action.update(action);		
	}

	@Override
	public String getCURLByActionId(long actionId) {
		Action action = getProjectDao().getAction(actionId);
		return action.getcURL();
	}

	@Override
	public void generateJsonSchemaByProject(int projectId) {
		//通过项目id获取批量的action ids		
		//System.out.println("projectId:"+projectId);
		List<Integer> ids = getProjectDao().getActionIdsByProjectId(projectId);
		//System.out.println("ids.size:"+ids.size());
		for (Integer id : ids){
			//System.out.println("id:"+id);
			String jsonSchema = generateJsonSchema(Long.parseLong(id.toString()), projectId);
			saveJsonSchema(id, jsonSchema);
		}
		System.out.println("jsonschema update ok.");
	}

	@Override
	public void saveMockdata(long actionId, String mockdata) {
		Action action = getProjectDao().getAction(actionId);
		action.setMockdata(mockdata);
		action.update(action);			
	}

	@Override
	public void generateMockdataByProject(int projectId) {
		//通过项目id获取批量的action ids		
		List<Integer> ids = getProjectDao().getActionIdsByProjectId(projectId);
		//System.out.println("ids.size:"+ids.size());
		for (Integer id : ids){
			String path = getProjectDao().getAction(id).getRequestUrl();
			String requesttype = getProjectDao().getAction(id).getRequestType();
			String mockdata = generateMockdata(Long.parseLong(id.toString()),projectId,path,getRequesttypeById(Integer.parseInt(requesttype)));
			//System.out.println("mockdata2:"+mockdata);
			if(mockdata ==null || "".equals(mockdata)){//避免因前面的错误导致破坏原有的数据
				continue;
			}
			//插入公共模型的测试数据
			Action action = getProjectDao().getAction(id);
			List<Parameter> parameters = action.getResponseParameterListOrdered();
			mockdata = recursiveParameter(parameters,mockdata,projectId);
			//插入公共模型的测试数据end
			
			saveMockdata(id, com.taobao.rigel.rap.common.StringUtils.formatJSON(mockdata));
		}
		System.out.println("mockdata update ok.");
	}
	//遍历参数，将有公共模型的参数增加插入测试数据
	private String recursiveParameter(List<Parameter> parameters,String mockdata,int projectId){
		for(Parameter p : parameters){
			if (p.getParameterListOrdered()==null ||p.getParameterListOrdered().size()==0){
				//System.out.println("parameter:"+p.getName());
				if (p.getName().contains("@model")){ //找到有引用公共模型的参数，准备插入测试数据
					//System.out.println("p:"+p.getName());
					String modelCode = p.getName().substring(p.getName().indexOf("@model")+7, p.getName().length()-1);
					//System.out.println("modelCode:"+modelCode);
					//System.out.println("p.type:"+p.getDataType());
					if (p.getDataType().contains("array")){  //对象数据
						String modeldata = "\""+p.getIdentifier()+"\":["+ constructModelMockData( projectId, modelCode);	
						String target = "\""+p.getIdentifier()+"\":[";
						mockdata = mockdata.replace(target, modeldata);
						//System.out.println("mockdata:"+mockdata);
					} else {  //单纯的对象
						String modeldata = "\""+p.getIdentifier()+"\":{"+ constructModelMockData( projectId, modelCode);	
						String target = "\""+p.getIdentifier()+"\":{";
						mockdata = mockdata.replace(target, modeldata);
						//System.out.println("mockdata:"+mockdata);
					}					 
				}
			} else {
				mockdata = recursiveParameter(p.getParameterListOrdered(),mockdata,projectId);
			}			
		}
		return mockdata;
	}
	
	//获取公共模型的mock数据，随机生成
	private String constructModelMockData(int projectId,String modelCode){
		StringBuilder mockdata = new StringBuilder("{");
		//通过model的name和projectId获取model的id，并将model的所有字段获取
		CommonModel model = this.projectMgr.getCommonModelByCode(projectId, modelCode);
		List<CommonModelField> fields = model.getCommonModelFieldListOrdered();
		//通过model的所有字段的集合，构造mock数据
		for(CommonModelField field : fields){
			mockdata.append("\"");
			mockdata.append(field.getIdentifier());
			mockdata.append("\":");
			if("string".equals(field.getDatatype())){//字符串
				mockdata.append("\"测试内容");
				mockdata.append(getRandomNumbers());
				mockdata.append("\",");
			} else if("number".equals(field.getDatatype())||
					"int".equals(field.getDatatype())|| 
					"float".equals(field.getDatatype())||
					"double".equals(field.getDatatype())||
					"long".equals(field.getDatatype())){//数字型
				mockdata.append(getRandomNumbers());
				mockdata.append(",");
			} else if("a".equals(field.getDatatype().substring(0, 1))||
					"A".equals(field.getDatatype().substring(0, 1))){ //数组
				if(field.getDatatype().contains("tring>")){ //字符串数组
					mockdata.append("[\"测试内容");
					mockdata.append(getRandomNumbers());
					mockdata.append("\"],");
				} else if(field.getDatatype().contains("rray<")){//具体类型的数组,Array<App>
					String objCode = field.getDatatype().substring(6, field.getDatatype().length()-1);
					mockdata.append("[");
					mockdata.append(constructModelMockData(projectId,objCode));
					mockdata.append("],");
				} else {//模糊数组(数字数组)
					mockdata.append("[");
					mockdata.append(getRandomNumbers());
					mockdata.append("],");
				}
			} else {//未知类型，暂时为字符串
				mockdata.append("\"未知类型的测试内容");
				mockdata.append(getRandomNumbers());
				mockdata.append("\",");
			}
				
		}
		//去掉最后一个逗号
		String result = mockdata.toString();
		if ("".equals(result)) return "";		
		return result.substring(0, result.length()-1)+"}";			
	}
	//随机生成一个1000-9999的整数
	private int getRandomNumbers(){
		Random r = new Random();
		return r.nextInt(9999-1000+1)+1000;//为变量赋随机值1000-9999;
	}
	
	private String generateMockdata(long actionId,int projectId,String path,String method){
		String mockdata = null;
		//http://fn.uctest.local:8080/mockjs/1/project.php
    	if (path.startsWith("/")){
    		path = path.substring(1);
    	} 
		//String url = rapdomain + "newmockjsdata/" + projectId +"/" + method + "/" + removeSpecialPart(path);
		//System.out.println("url:" + url);
		try {
			//mockdata = HTTPUtils.sendGet(url,"");
			
			Map<String, Object> options = new HashMap<String, Object>();
			options.put("method", method); 
			mockdata = mockMgr.generateRuleDataForAPI(projectId, removeSpecialPart(path), options);
			//System.out.println("mockdata:" + new String(mockdata.getBytes(), "utf8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mockdata;
	}	
	
	//处理路径部分带冒号的path
	private String removeSpecialPart(String path){
		String[] paths = path.split("/");
    	//将冒号开头的路径部分替换成100
    	for (int i=0;i<paths.length;i++){
    		if(paths[i].startsWith(":")){
    			paths[i] = "100";
    		}
    	}
    	//再将原路径拼回去
        String newPath = "";
        for (int i=0;i<paths.length;i++){
        	newPath += paths[i] + "/";
        }
        newPath = newPath.substring(0, newPath.length()-1);
        return newPath;
	}
	
	private String getRequesttypeById(int id){
		String result = "GET";
		////接口类型：1-GET,2-POST,3-PUT,4-DELETE,5-PATCH,6-COPY
		if (id == 1){
			result = "GET";
		} else if (id == 2){
			result = "POST";
		} else if (id == 3){
			result = "PUT";
		} else if (id == 4){
			result = "DELETE";
		} else if (id == 5){
			result = "PATCH";
		} else if (id == 6){
			result = "COPY";
		}			
		return result;
	}

	@Override
	public String generateRequestJsonSchema(long actionId, int projectId) {
		Action action = getProjectDao().getAction(actionId);
		Set<Parameter> parameters = action.getRequestParameterList();		
		StringBuilder schema = new StringBuilder("{");
		schema.append("\"title\":\"");
		schema.append(action.getName());
		schema.append("\", \"type\": \"object\",");
		//生成字段Schema部分
		schema.append(generateFieldsSchema(parameters));
		schema.append("}");

		return schema.toString();
	}

	@Override
	public void saveRequestJsonSchema(long actionId, String jsonSchema) {
		Action action = getProjectDao().getAction(actionId);
		action.setRequestschema(jsonSchema);
		action.update(action);		
	}

	@Override
	//(projectId, path, method, queryString, jsonData)
	public Map<String, String> validateRequestData(int projectId, String requestUrl, String jsonData)
			throws IOException, ProcessingException {
		Map<String, String> result = new HashMap<String, String>();
		StringBuilder message = new StringBuilder();
		if (jsonData == null || "".equals(jsonData)) {
			result.put("code", "400");
			result.put("message", "请求数据为空.");			
			return result;
		}
		Action action = projectDao.getActionByUrlAndProjectid(projectId, requestUrl);	
		String requestSchema = action.getRequestschema();
		//System.out.println(action.getId());
		final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
			if (requestSchema == null || "".equals(requestSchema)) {
				result.put("code", "401");
				result.put("message", "接口的RequestSchema数据为空.");
				return result;
			}
			
			JsonNode jsonschema = JsonLoader.fromString(requestSchema.replaceAll("\\\\", "\\\\\\\\"));
			//System.out.println("getJsonschema:" + action.getJsonschema());
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
					rules = getRules(requestSchema, field);
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
					error = validateRequiredProperties(pm.getMessage(), field, requestSchema, jsonData);				
					
					if (error == 0)
						continue;
				} else if (pm.getMessage().indexOf("instance type") != -1){//任何类型不匹配的情况，如果出现，则可忽略(这种情况只用于处理变态的不定类型)
					error = 1;
					rules = getRules(requestSchema, field);
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
				result.put("message", "本接口请求数据字段规则校验完毕，未发现问题。");
				return result;
			} else {//校验数据有错误		
				result.put("code", "500");
				message.append(tmpResult.toString());
			}
		//}//结束循环
		result.put("message", message.toString());
		
		return result;
	}

	@Override
	public void generateRequestSchemaByProject(int projectId) {
		//通过项目id获取批量的action ids		
		//System.out.println("projectId:"+projectId);
		List<Integer> ids = getProjectDao().getActionIdsByProjectId(projectId);
		//System.out.println("ids.size:"+ids.size());
		for (Integer id : ids){
			String jsonSchema = generateRequestJsonSchema(Long.parseLong(id.toString()), projectId);
			saveRequestJsonSchema(id, jsonSchema);
		}
		System.out.println("request schema update ok.");
		
	}
}