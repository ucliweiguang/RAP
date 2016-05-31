package com.taobao.rigel.rap.api.web.action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		Map<String,String> queryMap = new HashMap<String,String>();
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
			String[] value = (String[]) requestMap.get(key);
			//System.out.println("value"+value[0]);
			requestMap.put(key, value[0]);
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
		Map<String,String> bodyMap = new HashMap<String,String>();
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
	private Map<String,String> constructBodyMap(String bodyString){
		GsonBuilder gb = new GsonBuilder();
        Gson g = gb.create();
        Map<String, String> map = g.fromJson(bodyString, new TypeToken<Map<String, String>>() {}.getType());
        return map;
	}
	//将queryString的内容转换成Map
	private Map<String,String> constructQueryMap(String queryString){
		Map<String,String> result = new HashMap<String,String>();
		String[] data = queryString.split("&");
		for (String s : data){
			result.put(s.substring(0,s.indexOf("=")), s.substring(s.indexOf("=")+1));
		}		
		return result;
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
