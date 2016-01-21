package com.taobao.rigel.rap.organization.web.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.taobao.rigel.rap.common.ActionBase;
import com.taobao.rigel.rap.common.ContextManager;
import com.taobao.rigel.rap.organization.bo.Corporation;
import com.taobao.rigel.rap.organization.bo.ProductionLine;
import com.taobao.rigel.rap.organization.service.OrganizationMgr;
import com.taobao.rigel.rap.project.bo.Project;
import com.taobao.rigel.rap.project.service.ProjectMgr;

public class OrganizationAction extends ActionBase {
	private static final long serialVersionUID = -7254075166703993812L;
	private OrganizationMgr organizationMgr;
	private ProjectMgr projectMgr;
	private int plid;
	private int id;
	/*private String atadomain;

	public String getAtadomain() {
		return atadomain;
	}

	public void setAtadomain(String atadomain) {
		this.atadomain = atadomain;
	}*/

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getPlid() {
		return plid;
	}

	public void setPlid(int plid) {
		this.plid = plid;
	}

	public ProductionLine getProductLine() {
		return organizationMgr.getProductionLine(plid);
	}

	public ProjectMgr getProjectMgr() {
		return projectMgr;
	}

	public void setProjectMgr(ProjectMgr projectMgr) {
		this.projectMgr = projectMgr;
	}

	public OrganizationMgr getOrganizationMgr() {
		return organizationMgr;
	}

	public void setOrganizationMgr(OrganizationMgr organizationMgr) {
		this.organizationMgr = organizationMgr;
	}

	public String myHome() {
		return SUCCESS;
	}

	public String index() {
		if (isUserLogined()) {

			return SUCCESS;
		} else {
			return "public";
		}

	}

	public String group() {
		return SUCCESS;
	}

	@SuppressWarnings("unchecked")
	public String productline() {
		Corporation c = organizationMgr.getCorporation(id);
		if (c != null) {
			ContextManager.getSession().put(ContextManager.KEY_CORP_NAME,
					c.getName());
		}
		return SUCCESS;
	}

	public String projects() {
		if (!isUserLogined())
			return LOGIN;
		Gson gson = new Gson();
		List<Map<String, Object>> projects = new ArrayList<Map<String, Object>>();
		// long totalRecNum = projectMgr.getProjectListNum(getCurUser());
		List<Project> projectList = projectMgr.getProjectList(getCurUser(), 1,
				Integer.MAX_VALUE);
		String atadomain = organizationMgr.getAtadomain();
		for (Project p : projectList) {
			if (getCurUser().isUserInRole("admin")
					|| getAccountMgr().canUserManageProject(
							getCurUser().getId(), p.getId())) {
				p.setIsManagable(true);
			}
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("id", p.getId());
			map.put("name", p.getName());
			map.put("desc", p.getIntroduction());
			map.put("status", p.getLastUpdateStr());
			map.put("accounts", p.getMemberAccountListStr());
			map.put("readonlyaccounts", p.getReadonlyMemberAccountListStr());//added by liweiguang 2016-1-18
			map.put("isManagable", p.getIsManagable());
			map.put("creator", p.getUser().getUserBaseInfo());
			map.put("related", p.getUser().getId() != getCurUserId());
			
			//System.out.println("atadomain:"+atadomain);
			map.put("atadomain", atadomain);
			projects.add(map);
		}
		StringBuilder json = new StringBuilder();
		json.append("{");
		json.append("	\"groups\" : [{");
		json.append("		\"type\" : \"user\",");
		json.append("		\"projects\" :");
		json.append(gson.toJson(projects));
		json.append("	}]");
		json.append("}");
		setJson(json.toString());
		return SUCCESS;
	}

	public String corporationList() {
		Gson gson = new Gson();
		setJson(gson.toJson(organizationMgr.getCorporationList()));
		return SUCCESS;
	}

}
