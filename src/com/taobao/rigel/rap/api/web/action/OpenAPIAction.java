package com.taobao.rigel.rap.api.web.action;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.gson.Gson;
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
