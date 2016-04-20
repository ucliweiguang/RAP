package com.taobao.rigel.rap.api.service.impl;

import java.util.*;

import com.google.gson.Gson;
import com.taobao.rigel.rap.api.service.OpenAPIMgr;
import com.taobao.rigel.rap.mock.bo.Rule;
import com.taobao.rigel.rap.mock.dao.MockDao;
import com.taobao.rigel.rap.mock.service.MockMgr;
import com.taobao.rigel.rap.project.bo.Action;
import com.taobao.rigel.rap.project.bo.Module;
import com.taobao.rigel.rap.project.bo.Page;
import com.taobao.rigel.rap.project.bo.Parameter;
import com.taobao.rigel.rap.project.bo.Project;
import com.taobao.rigel.rap.project.service.ProjectMgr;

public class OpenAPIMgrImpl implements OpenAPIMgr {

	ProjectMgr projectMgr;

	public ProjectMgr getProjectMgr() {
		return projectMgr;
	}

	public void setProjectMgr(ProjectMgr projectMgr) {
		this.projectMgr = projectMgr;
	}

	public MockDao getMockDao() {
		return mockDao;
	}

	public void setMockDao(MockDao mockDao) {
		this.mockDao = mockDao;
	}

	MockDao mockDao;


	@Override
	public Object getModel(int projectId, String ver) throws Exception {
        Project project;
        if (ver != null && !ver.isEmpty()) {
            project = projectMgr.getProject(projectId, ver);
        } else {
            project = projectMgr.getProject(projectId);
        }
		if (project == null || project.getId() == 0) {
			throw new Exception("Illegal project id");
		}

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("id", project.getId());
		model.put("name", project.getName());
		model.put("ver", project.getVersion());
		model.put("intro", project.getIntroduction());

		List<Map<String, Object>> moduleList = new ArrayList<Map<String, Object>>();

		for (Module m : project.getModuleList()) {

			Map<String, Object> module = new HashMap<String, Object>();
			module.put("id", m.getId());
			module.put("name", m.getName());
			module.put("intro", m.getIntroduction());

			List<Map<String, Object>> pageList = new ArrayList<Map<String, Object>>();

			for (Page p : m.getPageList()) {
				Map<String, Object> page = new HashMap<String, Object>();
				page.put("id", p.getId());
				page.put("name", p.getName());
				page.put("intro", p.getIntroduction());

				List<Map<String, Object>> interfaceList = new ArrayList<Map<String, Object>>();

				for (Action a : p.getActionList()) {
					Map<String, Object> action = new HashMap<String, Object>();
					action.put("id", a.getId());
					action.put("name", a.getName());
					action.put("desc", a.getDescription());
					action.put("reqType", a.getRequestType());
					action.put("reqUrl", a.getRequestUrl());
					action.put("pbrequest", a.getPbrequest());
					action.put("pbresponse", a.getPbresponse());
					interfaceList.add(action);
				}

				page.put("interfaceList", interfaceList);
				pageList.add(page);
			}

			module.put("pageList", pageList);
			moduleList.add(module);
		}

		model.put("moduleList", moduleList);
		return model;
	}

	/*@Override
	public Object getSchema(int actionId, Action.TYPE type, String ver, int projectId) {
		Action action;
        if (ver != null && !ver.isEmpty() && projectId > 0) {
            action = projectMgr.getAction(actionId, ver, projectId);
        } else {
            action = projectMgr.getAction(actionId);
        }
		Map<String, Object> schema = new HashMap<String, Object>();

		Set<Parameter> pSet = type == Action.TYPE.REQUEST ? action
				.getRequestParameterList() : action.getResponseParameterList();

		schema.put("type", "object");
		schema.put("$schema", "http://json-schema.org/draft-04/schema");
        schema.put("type", "object");
		schema.put("entity_id", actionId);

		Map<String, Object> properties = new HashMap<String, Object>();

		for (Parameter p : pSet) {
			properties.put(p.getIdentifierWithoutMockjsRule(), generateJSONSchema(p));
		}

		schema.put("properties", properties);

		return schema;
	}*/

	@Override
	public String modifyMockRules(String rules, int actionId) {
        Rule oldRule = mockDao.getRule(actionId);
        int code;
        if (oldRule == null) {
            Rule rule = new Rule();
            rule.setActionId(actionId);
            rule.setRules(rules);
            rule.setUpdateTime(new Date());
            code = mockDao.addRule(rule);
        } else {
            Rule rule = oldRule;
            rule.setActionId(actionId);
            rule.setRules(rules);
            rule.setUpdateTime(new Date());
            code = mockDao.updateRule(rule);
        }
		Map<String, Object> jsonObj = new HashMap<String, Object>();
		boolean isOk = code == 0;
		String msg = "";
		if (code == -1) {
			msg = "Update rules failed.";
		}
		jsonObj.put("isOk", isOk);
		jsonObj.put("msg", msg);
		Gson gson = new Gson();
		return gson.toJson(jsonObj);
	}

	@Override
	public String resetMockRules(int actionId) {
		int code = mockDao.removeRule(actionId);
		Map<String, Object> jsonObj = new HashMap<String, Object>();
		boolean isOk = code == 0;
		String msg = "";
		if (code == -1) {
			msg = "Reset rules failed.";
		}
		jsonObj.put("isOk", isOk);
		jsonObj.put("msg", msg);
		Gson gson = new Gson();
		return gson.toJson(jsonObj);
	}

	private Object generateJSONSchema(Parameter p) {
		Map<String, Object> pMap = new HashMap<String, Object>();
		pMap.put("entity_id", p.getId());
		pMap.put("type", p.getJSONSchemaDataType());
        pMap.put("title", p.getName());
        pMap.put("description", p.getRemarkWithoutMockjsRule());
        String remark = p.getRemark();
        String format = "";
        if (remark != null && remark.contains("@mock=function")) {
            format = "";
        }
        String identifier = p.getIdentifier();
        if (identifier != null && identifier.contains("|") && identifier.indexOf("|") != identifier.length() - 1) {
            format += "" + identifier.substring(identifier.indexOf("|") + 1);
        } else {
            format += "";
        }
        if (remark != null && remark.contains("@mock=")) {
            format += "|" + remark.substring(remark.indexOf("@mock=") + 6);
        } else {
            format += "|";
        }
        pMap.put("iftest", format);
		Set<Parameter> children = p.getParameterList();
		if (children != null && children.size() > 0) {
			Map<String, Object> properties = new HashMap<String, Object>();
			for (Parameter child : children) {
				properties
						.put(child.getIdentifierWithoutMockjsRule(), generateJSONSchema(child));
			}
			pMap.put("properties", properties);
		}
		return pMap;
	}

	@Override
	public String getSchema(int actionId) {		
		Action action = projectMgr.getAction(actionId);;
		return action.getJsonschema();
	}

	@Override
	public String getModelJsonStr(int projectId, String modelCode) {		
		return projectMgr.getModelJsonStr(projectId, modelCode);
	}

}
