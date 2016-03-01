package com.taobao.rigel.rap.validation.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.taobao.rigel.rap.api.service.OpenAPIMgr;
import com.taobao.rigel.rap.project.bo.Action;
import com.taobao.rigel.rap.project.dao.ProjectDao;
import com.taobao.rigel.rap.validation.service.ValidationMgr;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.google.gson.Gson;

import junit.framework.TestCase;

public class ValidationMgrTest extends TestCase {
	ApplicationContext context;
	//@param url参数  
	private static final String TAG_PARAM = "@param";
	//@json body参数
	private static final String TAG_JSON = "@json";
	//@header header参数
	private static final String TAG_HEADER = "@header";
	//@form form-data参数
	private static final String TAG_FORMDATA = "@form";
	@Before
	protected void setUp() throws Exception {
		context = new FileSystemXmlApplicationContext(
				new String[] { "src\\applicationContext.xml" });
	}

	@After
	protected void tearDown() throws Exception { // TODO
		context = null;
	}

	@Test
	public void testValidationData() throws IOException, ProcessingException {
		ValidationMgr valMgr = (ValidationMgr) context.getBean("validationMgr");
		//action:3 url: /mobile/page/channel_short/1.1.1  短视频列表接口
		/*for(int i=1 ;i<=70;i++){
			if (i==28 || i==34 || i==35||i==46||i==51||i==62||i==63||i==67||i==68) continue;
			String result = valMgr.generateJsonSchema(i);
			System.out.println("result:" + i);
			valMgr.saveJsonSchema(i, result);
		}*/

		/*long i = 35;
		String result = valMgr.generateJsonSchema(i);
		System.out.println("result:" + result);
		valMgr.saveJsonSchema(i, result);
		*/

		/*String param ="id=1&preview_nomc=1";
		String requestUrl = "mobile/page/short/1.1.3";
		String responseData=HttpRequest.sendGet("http://rvibll.test2.uae.uc.cn/" + requestUrl, param);		
		System.out.println("responseData:"+ responseData);
		
		Map result2 = valMgr.validateAPIData(3, requestUrl, responseData);
		System.out.println("result2:" + result2.get("message"));
*/		//novel/i.php?do=qs_getjingsp
		//String responseData = "{\"data\":[{\"name\":\"测试内容b7z6\",\"project_id\":68402,\"status\":1}],\"msg\":\"测试内容j85i\",\"result\":44877}";
		String responseData = "{\"status\": 1, \"info\": { \"page\": 1, \"size\": 10, \"count\": 1260, \"total\": 12598, \"visit_time\": 4821, \"spnum\": 81191}, \"data\": [{\"mid\": \"NOVELb022b4e4f55311e4a32c002590ab3e8c\", \"author\": \"天蚕土豆\",    \"title\": \"大主宰\", \"sm_uid\": \"5585662\", \"sm_name\": \"管理员小七\", \"sm_photo\": \"2\", \"uc_uid\": \"743862218\", \"is_top\": \"1\", \"is_jing\": \"0\", \"auto_jing\": \"1\", \"score\": 0, \"zan\": \"5464\",\"zan_time\": \"1456194325\", \"pub_time\": \"1431070051\", \"replynum\": \"1685\", \"replytime\": \"1456194331\", \"text_title\": \"\", \"text\": \"ss\"}], \"spend\": {\"total\": 0.0886 }}";
		Map result2 = valMgr.validateAPIData(9, "novel/i.php?do=qs_getjingsp", responseData);
		System.out.println("result2:" + result2.get("message"));
		
		//System.out.println(removeMockForField("field"));
		//String requiredStr = "abced,";
		//System.out.println(requiredStr.substring(0, requiredStr.length()-1));
	}

	private String removeMockForField(String field) {
		if (field.contains("|")) {
			return field.substring(0, field.indexOf("|"));
		} else {
			return field;
		}
	}

	//@Test
	/*	public void testValidateSchema() throws ProcessingException, IOException{
			ValidationMgr valMgr =(ValidationMgr)context.getBean("validationMgr");
			String jsonSchema = valMgr.getJsonSchemaByActionId(5);//热词接口
			String result = valMgr.validateJsonSchema(jsonSchema);
			System.out.println("result:" + result);
		}*/
	/*@Test
	public void testGetSchema(){
		OpenAPIMgr openAPIMgr =(OpenAPIMgr)context.getBean("openAPIMgr");
		System.out.println(openAPIMgr.getSchema(2));
	}*/
	//@Test
	public void String() {
		String str = "depend:type=short";
		String dependField = str.substring(7, str.indexOf("="));
		System.out.println("dependField:"+ dependField);
		String dependValue = str.substring(str.indexOf("=")+1, str.length());
		System.out.println("dependValue:"+ dependValue);
	}

	public static boolean isNumeric(String str) {
		for (int i = str.length(); --i >= 0;) {
			if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	//@Test
	public void GetRule() throws IOException{
		ProjectDao projectdao =(ProjectDao)context.getBean("projectDao");
		Action action = projectdao.getAction(35);
		//System.out.println(action.getJsonschema());
		JsonNode jsonNode = JsonLoader.fromString(action.getJsonschema());
				
		String field = "/data/subject/3/video_list/6/play_url";
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
		System.out.println("new Field:" + newField.toString());
		//field = field.replaceAll("/", "/properties/").replaceAll("properties/0", "items").substring(1);
		field = newField.toString().substring(0, newField.toString().length()).replaceAll("/", "/properties/").replaceAll("properties/items", "items").substring(1);
		System.out.println("field:" + field);
		String[] fields = field.split("/");		
		System.out.println("field.length:" + fields.length);
		for (int i =0 ;i < fields.length;i++){
			jsonNode = jsonNode.get(fields[i]);
			System.out.println(fields[i] + "====jsonNode[i]:" + jsonNode);
		}
		System.out.println("jsonNode:" + jsonNode);
		String description = jsonNode.get("description").toString();
		String ruleStr = description.substring(description.indexOf("@v"), description.length()-1).substring(3);
		//System.out.println("ruleStr:" + ruleStr.replaceAll("||", ";"));
		String[] rules = ruleStr.split(";");
		//System.out.println("rules.size" + rules.length);
		for(int i=0;i<rules.length;i++){
			System.out.println("rule[i]" + rules[i]);
		}
		
/*		String fieldName = field.substring(field.lastIndexOf("/")+1, field.length());
		System.out.println("fieldName:" + fieldName);*/

	}
	
	public void estGetJsonData() throws IOException{
		String param ="subject_tag=index&preview_nomc=1";
		String requestUrl = "mobile/page/index/1.1.1";
		String responseData=HttpRequest.sendPost("http://rvibll.test2.uae.uc.cn/" + requestUrl, param);
		//System.out.println("responseData:"+ responseData);
		JsonNode jsonNode = JsonLoader.fromString(responseData);
		String path = "/data/subject/3/video_list/6";
		System.out.println("jsonNode:" + jsonNode.get("data").get("subject").get(3).get("video_list").get(6).get("type"));
		
	}
	
	public void estMapToJson(){
		Map<String, String> m = new HashMap<String, String>();
		m.put("code", "200");
		m.put("message", "this is a message！");
		Gson g = new Gson();
		System.out.println(g.toJson(m));
		
	}
	
	public void estGetAction(){
		long start = System.currentTimeMillis();
		ProjectDao projectDao = (ProjectDao) context.getBean("projectDao");
		Action a = projectDao.getActionByUrlAndProjectid(3, "mobile/page/index/1.1.1");
		System.out.println("time:" + (System.currentTimeMillis()- start));
		System.out.println("Action:" + a.getJsonschema());
	}
	
	public void estFormatRules(){		
		String raw = readFileByLines("D:\\RAP\\test\\testdata");
		String result = "";
		
		String tmp = raw.substring(0,raw.indexOf("extra#")-1);
		//System.out.println(tmp);
		String description = "否包含边界@v";
		System.out.println(description.substring(0, description.length()-2));
				
		String rule = raw.substring(raw.indexOf("@v"), raw.length()).substring(3);		
		//System.out.println("rule:" + rule);
		String[] rules = rule.split(";");		
		for(int i=0;i<rules.length;i++){
			//System.out.println("rule:" + rules[i]);
			if(rules[i].indexOf("extra#")==0){
				//System.out.println("rule:" + rules[i]);
				result = rules[i].substring(5,rules[i].length()).replace("#", "");
				//System.out.println(result);
			}
		}
	}
	
	/**
     * 以行为单位读取文件，常用于读面向行的格式化文件
     */
    static String readFileByLines(String fileName) {
    	String result = "";
        File file = new File(fileName);
        BufferedReader reader = null;
        try {
            //System.out.println("以行为单位读取文件内容，一次读一整行：");
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            //int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                //System.out.println("line " + line + ": " + tempString);
            	//System.out.println("line : " + tempString);
            	result+=tempString;
                //line++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return result;
    }
    
	//从参数的名称字段获取对应的值，如从字段Accept-Language获取"@header{zh-CN,zh;q=0.8}"
	public void estgetValueFromParamName(){
		String name = "测试字段@header{zh-CN,zh;q=0.8}@v=NOT NULL;extra#abcd#";
		StringBuilder value =new StringBuilder(); 
		String tmp = name.substring(name.indexOf(TAG_HEADER)+7);
		if (tmp.startsWith("{")){
			char[] cs = tmp.toCharArray();
			for (char c : cs ){
				if (c =='{') continue;
				if (c == '}') break;
				value.append(c);
			}
		}

		System.out.println("values:" + value.toString());
	}
	
	public void estGenerateCURL(){
		long actionId = 288;  //288 //1
		ValidationMgr valMgr = (ValidationMgr) context.getBean("validationMgr");
		System.out.println("cURL:" + valMgr.generateCURL(actionId));
	}
}
