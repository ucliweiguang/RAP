package com.taobao.rigel.rap.api.web.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.taobao.rigel.rap.api.service.OpenAPIMgr;
import com.taobao.rigel.rap.common.ActionBase;
import com.taobao.rigel.rap.mock.service.MockMgr;
import com.taobao.rigel.rap.project.bo.Action;
import com.taobao.rigel.rap.project.bo.Project;
import com.taobao.rigel.rap.project.service.ProjectMgr;
import com.taobao.rigel.rap.validation.service.ValidationMgr;

public class OpenAPIAction extends ActionBase {

    private static final long serialVersionUID = -1786553279434025468L;
	private ValidationMgr validationMgr;

	public ValidationMgr getValidationMgr() {
		return validationMgr;
	}

	public void setValidationMgr(ValidationMgr validationMgr) {
		this.validationMgr = validationMgr;
	}
    private OpenAPIMgr openAPIMgr;

    public void setOpenAPIMgr(OpenAPIMgr openAPIMgr) {
        this.openAPIMgr = openAPIMgr;
    }

    private ProjectMgr projectMgr;

    public void setProjectMgr(ProjectMgr projectMgr) {
        this.projectMgr = projectMgr;
    }

    private MockMgr mockMgr;

    public void setMockMgr(MockMgr mockMgr) {
        this.mockMgr = mockMgr;
    }

    private int projectId;

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    private int actionId;

    public int getActionId() {
        return this.actionId;
    }

    public void setActionId(int actionId) {
        this.actionId = actionId;
    }

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private String ver;

    public String getVer() {
        return ver;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }

    private String _c;

    private String callback;

    public String get_c() {
        return _c;
    }

    public void set_c(String _c) {
        this._c = _c;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public String getCallback() {
        return callback;
    }

    public String queryModel() throws Exception {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Gson g = new Gson();
        resultMap.put("model", openAPIMgr.getModel(projectId, ver));
        resultMap.put("code", 200);
        resultMap.put("msg", "");
        String resultJson = g.toJson(resultMap);

        // JSONP SUPPORTED
        if (callback != null && !callback.isEmpty()) {
            resultJson = (callback + "(" + resultJson + ")");
        } else if (_c != null && !_c.isEmpty()) {
            resultJson = (_c + "(" + resultJson + ")");
        }

        setJson(resultJson);
        return SUCCESS;
    }

/*    public String querySchema() {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Gson g = new Gson();
        resultMap.put("schema",
                openAPIMgr.getSchema(actionId, (type != null && type.equals("request") ? Action.TYPE.REQUEST : Action.TYPE.RESPONSE), ver, projectId));
        resultMap.put("code",
                200);
        resultMap.put("msg", "");
        String resultJson = g.toJson(resultMap);

        // JSONP SUPPORTED
        if (callback != null && !callback.isEmpty()) {
            resultJson = (callback + "(" + resultJson + ")");
        } else if (_c != null && !_c.isEmpty()) {
            resultJson = (_c + "(" + resultJson + ")");
        }

        setJson(resultJson);
        return SUCCESS;
    }*/
    
    //overrided  by liweiguang 2015-09-06 
    public String querySchema() {
        setJson(openAPIMgr.getSchema(actionId));
        return SUCCESS;
    }

    public String queryRAPModel() throws UnsupportedEncodingException {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Gson g = new Gson();
        Project p = projectMgr.getProject(projectId);
        List<Action> aList = p.getAllAction();
        Map<Integer, Object> mockDataMap = new HashMap<Integer, Object>();

        for (Action a : aList) {
            mockDataMap.put(Integer.parseInt(new Long(a.getId()).toString()), mockMgr.generateRule(Integer.parseInt(new Long(a.getId()).toString()), null, null));
        }

        resultMap.put("modelJSON", p.toString(Project.TO_STRING_TYPE.TO_PARAMETER));
        resultMap.put("mockjsMap", mockDataMap);
        resultMap.put("code", 200);
        resultMap.put("msg", 0);
        String resultJson = g.toJson(resultMap);

        // JSONP SUPPORTED
        if (callback != null && !callback.isEmpty()) {
            resultJson = (callback + "(" + resultJson + ")");
        } else if (_c != null && !_c.isEmpty()) {
            resultJson = (_c + "(" + resultJson + ")");
        }

        setJson(resultJson);
        return SUCCESS;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    private String rules;

    public String modifyMockRules() {
        String json = openAPIMgr.modifyMockRules(rules, actionId);
        setJson(json);
        return SUCCESS;
    }

    public String resetMockRules() {
        String json = openAPIMgr.resetMockRules(actionId);
        setJson(json);
        return SUCCESS;
    }
    

	private String requestUrl;

	public String getRequestUrl() {
		return requestUrl;
	}

	public void setRequestUrl(String requestUrl) {
		this.requestUrl = requestUrl;
	}
	private String jsonData;
	
	public String getJsonData() {
		return jsonData;
	}

	public void setJsonData(String jsonData) {
		this.jsonData = jsonData;
	}

	public String validateAPIData()
			throws IOException, ProcessingException {
		Map<String,String> result = validationMgr.validateAPIData(projectId, requestUrl, jsonData);
		Gson g = new Gson();
		setJson(g.toJson(result));
		return SUCCESS;
	}
	
	public String validateRequestData()throws IOException, ProcessingException {
		String queryString = "";
		String requestPath = "";
		Map queryMap = new HashMap();
		//System.out.println("projectId:"+projectId);
		//System.out.println("requestUrl:"+requestUrl);
		//如果requestUrl有问号，则从问号后的都是queryString；否则queryString = ""
		if (requestUrl.indexOf("?")!=-1){
			queryString = requestUrl.substring(requestUrl.indexOf("?")+1);
			queryMap = constructQueryMap(queryString);
			requestPath = requestUrl.substring(0, requestUrl.indexOf("?"));
		} else{
			requestPath =requestUrl;
		}
		//System.out.println("queryString:"+queryString);
		//System.out.println("requestPath:"+requestPath);
		
		//构造jsonData,jsonData = queryString + requestbody
		HttpServletRequest request = ServletActionContext.getRequest();
		Map requestMap = request.getParameterMap();  //获取formData的数据
		Enumeration names =  request.getParameterNames();
		while (names.hasMoreElements()){  //将原formdata中的数组型元素转为普通类型
			String key = (String)names.nextElement();
			String[] values = (String[]) requestMap.get(key);
			//System.out.println("value"+value[0]);
			if (isInteger(values[0])){
				requestMap.put(key, Integer.parseInt(values[0]));
			} else if (isDouble(values[0])){
				requestMap.put(key, Double.parseDouble(values[0]));
			} else {  //字符串
				requestMap.put(key, values[0]);
			}			
		}		
			
		if (queryMap !=null){
			requestMap.putAll(queryMap);
		}		
		requestMap.remove("projectId");
		requestMap.remove("requestUrl");
		
		//这里开始获取body的请求内容(raw)
		InputStreamReader inputReader = new InputStreamReader( 
		request.getInputStream(), "UTF-8"); 
		BufferedReader bufferReader = new BufferedReader(inputReader); 
		StringBuilder bodyStr = new StringBuilder(); 
		String line = null; 
		while ((line = bufferReader.readLine()) != null) { 
			bodyStr.append(line); 
		} 
		//System.out.println("body:"+sb.toString());
		Map bodyMap = new HashMap();
		bodyMap = constructBodyMap(bodyStr.toString());
		if (bodyMap != null){
			requestMap.putAll(bodyMap);	
		}		
		
		Gson data = new Gson();
		String jsonData = data.toJson(requestMap);
		System.out.println("jsonData:"+jsonData);
		
		Map<String,String> result = validationMgr.validateRequestData(projectId, requestPath, jsonData);
		Gson g = new Gson();
		setJson(g.toJson(result));
		return SUCCESS;
	}

	//将body的内容转化成Map
	private Map constructBodyMap(String bodyString){
		Map result = new HashMap();
		GsonBuilder gb = new GsonBuilder();
        Gson g = gb.create();
        Map<String,String> map = g.fromJson(bodyString, new TypeToken<Map<String,String>>() {}.getType());
        if (map ==null) return result;
        
        for (Map.Entry<String,String> entry : map.entrySet()) {        	  
            //System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());  
        	if (isInteger(entry.getValue())){
        		result.put(entry.getKey(), Integer.parseInt(entry.getValue()));
			} else if (isDouble(entry.getValue())){
				result.put(entry.getKey(), Double.parseDouble(entry.getValue()));
			} else {  //字符串
				result.put(entry.getKey(), entry.getValue());
			}
        }  
        return result;
	}
	//将queryString的内容转换成Map
	private Map constructQueryMap(String queryString){
		Map result = new HashMap();
		String[] data = queryString.split("&");
		for (String s : data){
			String  value = s.substring(s.indexOf("=")+1); 
			if (isInteger(value)){
				result.put(s.substring(0,s.indexOf("=")), Integer.parseInt(value));
			} else if (isDouble(value)){
				result.put(s.substring(0,s.indexOf("=")), Double.parseDouble(value));
			} else {  //字符串
				result.put(s.substring(0,s.indexOf("=")), value);
			}
			//result.put(s.substring(0,s.indexOf("=")),s.substring(s.indexOf("=")+1) );
		}		
		return result;
	}
	/* 
	  * 判断是否为整数  
	  * @param str 传入的字符串  
	  * @return 是整数返回true,否则返回false  
	*/  
	private boolean isInteger(String str) {    
		Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");    
		return pattern.matcher(str).matches();    
	} 
	/*  
	  * 判断是否为浮点数，包括double和float  
	  * @param str 传入的字符串  
	  * @return 是浮点数返回true,否则返回false  
	*/    
	private boolean isDouble(String str) {    
	  Pattern pattern = Pattern.compile("^[-\\+]?[.\\d]*$");    
	  return pattern.matcher(str).matches();    
	}  
	private String modelCode;	
	public String getModelCode() {
		return modelCode;
	}
	public void setModelCode(String modelCode) {
		this.modelCode = modelCode;
	}

	public String getModelJsonStr(){
		//System.out.println("projectId:"+projectId);
		//System.out.println("modelCode:"+modelCode);
        String json = openAPIMgr.getModelJsonStr(projectId, modelCode);
        setJson(json);
        return SUCCESS;
	}
}
