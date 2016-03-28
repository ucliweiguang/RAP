package com.taobao.rigel.rap.project.dao.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.taobao.rigel.rap.common.CacheUtils;
import com.taobao.rigel.rap.common.Param;
import com.taobao.rigel.rap.common.URLUtils;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import com.google.gson.Gson;
import com.taobao.rigel.rap.account.bo.User;
import com.taobao.rigel.rap.common.ArrayUtils;
import com.taobao.rigel.rap.common.StringUtils;
import com.taobao.rigel.rap.project.bo.Action;
import com.taobao.rigel.rap.project.bo.Module;
import com.taobao.rigel.rap.project.bo.ObjectItem;
import com.taobao.rigel.rap.project.bo.Page;
import com.taobao.rigel.rap.project.bo.Parameter;
import com.taobao.rigel.rap.project.bo.Project;
import com.taobao.rigel.rap.project.bo.ProjectUser;
import com.taobao.rigel.rap.project.dao.ProjectDao;

public class ProjectDaoImpl extends HibernateDaoSupport implements ProjectDao {

    @SuppressWarnings("unchecked")
	@Override
	public List<Project> getProjectList(User user, int curPageNum, int pageSize) {
		StringBuilder sql = new StringBuilder();
        sql
        .append("SELECT project_id ")
        .append("FROM tb_project_and_user ")
        .append("WHERE user_id = :userId ")
        .append("UNION ")
        .append("SELECT id ")
        .append("FROM tb_project ")
        .append("WHERE user_id = :userId ");
		Query query = getSession().createSQLQuery(sql.toString()).setLong("userId",
				user.getId());

        List<Integer> list = query.list();
        List<Project> resultList = new ArrayList<Project>();
	    for (Integer id : list) {
            Project p = this.getProject(id);
            if (p != null && p.getId() > 0) {
                resultList.add(p);
            }
        }
		return resultList;
	}
    @Override
    public List<Project> getProjectList() {
        String hqlByUser = "from Project";
        Query query = getSession().createQuery(hqlByUser);
        return query.list();
    }

	@Override
	public int addProject(Project project) {
		Session session = getSession();
		project.getUser().addCreatedProject(project);
		project.setVersion("0.0.0.1"); // default version
		int modeInt = project.getWorkspaceModeInt();
		if (modeInt <= 0 || modeInt >= 3) {
			project.setWorkspaceModeInt(1);
		}
		session.save(project);
		project = (Project) session.load(Project.class, project.getId());
		project.setProjectData(project
				.toString(Project.TO_STRING_TYPE.TO_PARAMETER));
		return project.getId();
	}

	@Override
	public int removeProject(int id) {
		Session session = getSession();
		Object project = session.get(Project.class, id);
		if (project != null) {
			session.delete((Project) project);
			return 0;
		} else {
			return -1;
		}

	}

	@Override
	public int updateProject(Project project) {
		Session session = getSession();
		session.update(project);
		return 0;
	}

	@Override
	public Project getProject(int id) {
		Project p = null;
		try {
			Session session = getSession();
			p = (Project) session.get(Project.class, id);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return p;
	}

	@Override
	public Module getModule(int id) {
		Module m = null;
		try {
			Session session = getSession();
			m = (Module) session.get(Module.class, id);
		} catch (ObjectNotFoundException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return m;
	}

	@Override
	public Page getPage(int id) {
		return (Page) getSession().get(Page.class, id);
	}


	@Override
	public Action getAction(long id) {
		return (Action) getSession().get(Action.class, id);
	}

	private Parameter getParameter(int id) {
		return (Parameter) getSession().get(Parameter.class, id);
	}

	@Override
	public int saveProject(Project project) {
		Session session = getSession();
		session.saveOrUpdate(project);
		return 0;
	}

	@Override
	public String updateProject(int id, String projectData,
			String deletedObjectListData, Map<Long, Long> actionIdMap) {
		Session session = getSession();
		// StringBuilder log = new StringBuilder();
		Gson gson = new Gson();
		//System.out.println("projectData:"+projectData);
		Project projectClient = gson.fromJson(projectData, Project.class);		
		
		ObjectItem[] deletedObjectList = gson.fromJson(deletedObjectListData,
				ObjectItem[].class);

		Project projectServer = (Project) session.load(Project.class, id);
		projectServer.setUpdateTime(new Date());

		// performing deleting
		for (ObjectItem item : deletedObjectList) {
			if (item.getClassName().equals("Module")) {
				projectServer.removeModule(item.getId(), session);
			} else if (item.getClassName().equals("Page")) {
				projectServer.removePage(item.getId(), session);
			} else if (item.getClassName().equals("Action")) {
				projectServer.removeAction(item.getId(), session);
			} else if (item.getClassName().equals("Parameter")) {
				projectServer.removeParameter(item.getId(), session);
			}
		}

		// performing adding & updating
		for (Module module : projectClient.getModuleList()) {
			Module moduleServer = projectServer.findModule(module.getId());
			if (moduleServer == null) {
				addModule(session, projectServer, module);
				continue;
			}
			moduleServer.update(module);
			for (Page page : module.getPageList()) {
				Page pageServer = projectServer.findPage(page.getId());
				if (pageServer == null) {
					addPage(session, module, page);
					continue;
				}
				pageServer.update(page);
				for (Action action : page.getActionList()) {
					Action actionServer = projectServer.findAction(action
							.getId());
					if (actionServer == null) {
						long oldActionId = action.getId();
						long createdActionId = addAction(session, page, action);
						actionIdMap.put(oldActionId, createdActionId);
						continue;
					}
					actionServer.update(action);
                    CacheUtils.removeCacheByActionId(action.getId());
                    
                    //added by liweiguang
                    Set<Parameter> requestParameters = sortFields(action.getRequestParameterList());
                    //
					//for (Parameter parameter : action.getRequestParameterList()) {
					for (Parameter parameter : requestParameters) {
						Parameter parameterServer = projectServer
								.findParameter(parameter.getId(), true);
						if (parameterServer == null) {
							addParameter(session, action, parameter, true);
							continue;
						}
						parameterServer.update(parameter);
						
						//added by liweiguang
	                    Set<Parameter> childrequestParameters = sortFields(parameter.getParameterList());
	                    //
	                    //for (Parameter childParameter : parameter.getParameterList()) {
						for (Parameter childParameter :childrequestParameters) {
							processParameterRecursively(session, projectServer,
									parameter, childParameter);
						}
					}
					//added by liweiguang
                    Set<Parameter> responseParameters = sortFields(action.getResponseParameterList());
                    //
                    //for (Parameter parameter : action.getResponseParameterList()) {
					for (Parameter parameter : responseParameters) {
						Parameter parameterServer = projectServer
								.findParameter(parameter.getId(), false);
						if (parameterServer == null) {
							addParameter(session, action, parameter, false);
							continue;
						}
						parameterServer.update(parameter);
						//added by liweiguang
	                    Set<Parameter> childresponseParameters = sortFields(parameter.getParameterList());
	                    //
	                    //for (Parameter childParameter : parameter.getParameterList()) {
						for (Parameter childParameter : childresponseParameters) {
							processParameterRecursively(session, projectServer,
									parameter, childParameter);
						}
					}
				}
			}
		}
		return "";
	}

	private Set<Parameter> sortFields(Set<Parameter> fields){
		//List<Action> actions = projectClient.getAllAction();
		//Action a = actions.get(0);
		//Set<Parameter> params = a.getRequestParameterList();
		SortedSet<Parameter> result = new TreeSet<Parameter>(new Comparator<Parameter>() {
			@Override
			public int compare(Parameter o1, Parameter o2) {

				if (o1.getId() > o2.getId()){
					return 1;
				} else if (o1.getId() < o2.getId()){
					return -1;
				} else{
					return 0;
				}				
			}
			
		});
		result.addAll(fields);
		/*for (Parameter p : result){
			System.out.println("para:" + p.getId());
			System.out.println("para.getIdentifier:" + p.getIdentifier());
		}*/
		return result;		
	}
	
	private void processParameterRecursively(Session session,
			Project projectServer, Parameter parameter, Parameter childParameter) {
		Parameter childParameterServer = projectServer
				.findChildParameter(childParameter.getId());
		if (childParameterServer == null) {
			addParameterRecursively(session, parameter, childParameter);
		} else {
			childParameterServer.update(childParameter);
		}
		//added by liweiguang
        Set<Parameter> childrequestParameters = sortFields(childParameter.getParameterList());
        //
        //for (Parameter childOfChildParameter : childParameter.getParameterList()) {
		for (Parameter childOfChildParameter : childrequestParameters) {
			processParameterRecursively(session, projectServer, childParameter,
					childOfChildParameter);
		}
	}

	private void addModule(Session session, Project project, Module module) {
		project.addModule(module);
		session.save(module);
		for (Page page : module.getPageList()) {
			addPage(session, module, page);
		}
	}

	private void addPage(Session session, Module module, Page page) {
		module = (Module) session.load(Module.class, module.getId());
		module.addPage(page);
		session.save(page);
		for (Action action : page.getActionList()) {
			addAction(session, page, action);
		}
	}

	private long addAction(Session session, Page page, Action action) {
		page = (Page) session.load(Page.class, page.getId());
		page.addAction(action);
		long createdId = (Long)session.save(action);
		for (Parameter parameter : action.getRequestParameterList()) {
			addParameter(session, action, parameter, true);
		}
		for (Parameter parameter : action.getResponseParameterList()) {
			addParameter(session, action, parameter, false);
		}
		return createdId;
	}

	private void addParameter(Session session, Action action,
			Parameter parameter, boolean isRequest) {
		action = (Action) session.load(Action.class, action.getId());
		action.addParameter(parameter, isRequest);
		session.save(parameter);
		for (Parameter childParameter : parameter.getParameterList()) {
			addParameterRecursively(session, parameter, childParameter);
		}
	}

	/**
	 * add parameter recursively
	 * 
	 * @param session
	 *            session object
	 * @param parameter
	 *            parent parameter
	 * @param childParameter
	 *            child parameter
	 */
	private void addParameterRecursively(Session session, Parameter parameter,
			Parameter childParameter) {
		parameter = (Parameter) session
				.load(Parameter.class, parameter.getId());
		parameter.addChild(childParameter);
		session.save(childParameter);
		for (Parameter childOfChildParameter : childParameter
				.getParameterList()) {
			addParameterRecursively(session, childParameter,
					childOfChildParameter);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public long getProjectListNum(User user) {
		String hql = "select count(p) from Project as p order by p.id desc";
		String hqlByUser = "select count(p) from Project as p left join p.userList as u where p.user.id = :userId or u.id = :userId order by p.id desc";
		Query query = user == null ? getSession().createQuery(hql)
				: getSession().createQuery(hqlByUser).setLong("userId",
						user.getId());
		List<Long> list = query.list();
		return list.get(0);
	}

    @Override
    public long getProjectListNum() {
        String sql = "SELECT COUNT(*) FROM tb_project";
        Query query = getSession().createSQLQuery(sql);
        return Long.parseLong(query.uniqueResult().toString());
    }

    @Override
    public long getModuleNum() {
        String sql = "SELECT COUNT(*) FROM tb_module";
        Query query = getSession().createSQLQuery(sql);
        return Long.parseLong(query.uniqueResult().toString());
    }

    @Override
    public long getPageNum() {
        String sql = "SELECT COUNT(*) FROM tb_page";
        Query query = getSession().createSQLQuery(sql);
        return Long.parseLong(query.uniqueResult().toString());
    }

    @Override
    public long getActionNum() {
        String sql = "SELECT COUNT(*) FROM tb_action";
        Query query = getSession().createSQLQuery(sql);
        return Long.parseLong(query.uniqueResult().toString());
    }

    @Override
    public long getMockNumInTotal() {
        String sql = "SELECT SUM(mock_num) FROM tb_project";
        Query query = getSession().createSQLQuery(sql);
        Object queryResult = query.uniqueResult();
        return queryResult != null ? Long.parseLong(queryResult.toString()) : 0;
    }

    @Override
    public long getParametertNum() {
        String sql = "SELECT COUNT(*) FROM tb_parameter";
        Query query = getSession().createSQLQuery(sql);
        return Long.parseLong(query.uniqueResult().toString());
    }

    @Override
    public long getCheckInNum() {
        String sql = "SELECT COUNT(*) FROM tb_check_in";
        Query query = getSession().createSQLQuery(sql);
        return Long.parseLong(query.uniqueResult().toString());
    }

	@Override
	public List<Action> getMatchedActionList(int projectId, String pattern) {
		List<Action> list = getActionListOfProject(projectId);
		List<Action> result = new ArrayList<Action>();
		for (Action action : list) {
			String url = action.getRequestUrl();
			if (url != null && !url.isEmpty() && url.charAt(0) != '/'
					&& !url.startsWith("reg:")) {
				url = "/" + url;
			}
			if (url.startsWith("reg:")) { // regular pattern
				if (StringUtils.regMatch(url.substring(4), pattern)) {
					result.add(action);
				}
			} else if (url.contains(":")) {
				String urlParamRemoved = URLUtils.removeParamsInUrl(url);
				String realUrlParamRemoved = URLUtils
						.removeRealParamsInUrl(pattern);
				if (urlParamRemoved.contains(realUrlParamRemoved) ||
						realUrlParamRemoved.contains(urlParamRemoved)) {
					result.add(action);
				}
			} else { // normal pattern
				if (url.contains(pattern)) {
					result.add(action);
				}
			}
		}

		return result;

		// process /:id/ cases
		// boolean urlParalized = false;
		// String patternOrignial = pattern;
		// if (pattern.contains(":")) {
		// urlParalized = true;
		// pattern = pattern.substring(0, pattern.indexOf(":"));
		// }

		/**
		 * StringBuilder sb = new StringBuilder();
		 * sb.append("SELECT a.id FROM tb_action a ")
		 * .append("JOIN tb_action_and_page ap ON ap.action_id = a.id ")
		 * .append("JOIN tb_page p ON p.id = ap.page_id ")
		 * .append("JOIN tb_module m ON m.id = p.module_id ") .append(
		 * "WHERE LOCATE(:pattern, a.request_url) != 0 AND m.project_id = :projectId "
		 * );
		 * 
		 * String sql = sb.toString(); Query query =
		 * getSession().createSQLQuery(sql); query.setString("pattern",
		 * pattern); query.setInteger("projectId", projectId); List<Integer>
		 * list = query.list(); List<Action> actionList = new
		 * ArrayList<Action>(); for (int id : list) {
		 * actionList.add(getAction(id)); }
		 */

		// URL parameters filter
		/**
		 * if (urlParalized) { List<Action> filteredActionList = new
		 * ArrayList<Action>(); for (Action a : actionList) { String u =
		 * a.getRequestUrl(); if (u.contains("?")) { u = u.substring(0,
		 * u.indexOf("?")); } u = StringUtils.removeParamsInUrl(u);
		 * patternOrignial = StringUtils .removeParamsInUrl(patternOrignial); if
		 * (u != null && patternOrignial != null && u.equals(patternOrignial)) {
		 * filteredActionList.add(a); } } actionList = filteredActionList; }
		 */
		// return actionList;
	}

	@SuppressWarnings("unchecked")
	private List<Integer> getParameterIdList(int projectId) {
		StringBuilder sql = new StringBuilder();
		sql.append(" SELECT DISTINCT p.id")
				.append(" FROM tb_parameter p")
				.append(" JOIN tb_response_parameter_list_mapping rplm ON p.id = rplm.parameter_id")
				.append(" JOIN tb_action_and_page ap ON ap.action_id = rplm.action_id")
				.append(" JOIN tb_page p2 ON p2.id = ap.page_id")
				.append(" JOIN tb_module m ON m.id = p2.module_id")
				.append(" WHERE m.project_id = :projectId");
		Query query = getSession().createSQLQuery(sql.toString());
		query.setInteger("projectId", projectId);
		List<Integer> list = query.list();

		StringBuilder sql2 = new StringBuilder();
		sql2.append(" SELECT DISTINCT p.id")
				.append(" FROM tb_parameter p")
				.append(" JOIN tb_request_parameter_list_mapping rplm ON p.id = rplm.parameter_id")
				.append(" JOIN tb_action_and_page ap ON ap.action_id = rplm.action_id")
				.append(" JOIN tb_page p2 ON p2.id = ap.page_id")
				.append(" JOIN tb_module m ON m.id = p2.module_id")
				.append(" WHERE m.project_id = :projectId");
		Query query2 = getSession().createSQLQuery(sql2.toString());
		query2.setInteger("projectId", projectId);
		list.addAll(query2.list());
		List<Parameter> paramList = new ArrayList<Parameter>();
		for (Integer pId : list) {
			paramList.add(getParameter(pId));
		}
		for (Parameter p : paramList) {
			recursivelyAddSubParamList(list, p);
		}
		return list;
	}

	private void recursivelyAddSubParamList(List<Integer> list, Parameter p) {
		list.add(p.getId());
		if (p.getParameterList() == null)
			return;
		for (Parameter subP : p.getParameterList()) {
			recursivelyAddSubParamList(list, subP);
		}
	}

	public int resetMockData(int projectId) {
		List<Integer> pIdList = getParameterIdList(projectId);
		String sql = "UPDATE tb_parameter SET mock_data = NULL where id in ("
				+ ArrayUtils.join(pIdList, ",") + ")";
		Query query = getSession().createSQLQuery(sql);
		return query.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Project> getProjectListByGroup(int id) {
		String hql = "from Project where groupId = :id";
		Query query = getSession().createQuery(hql);
		query.setInteger("id", id);
		return query.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Project> search(String key) {
		String hql = "from Project where name LIKE :key";
		Query query = getSession().createQuery(hql);
		query.setString("key", "%" + key + "%");
		return query.list();
	}

    @SuppressWarnings({ "rawtypes" })
	private List<Action> getActionListOfProject(int projectId) {
		List<Action> list = new ArrayList<Action>();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT a.id ");
		sql.append("FROM tb_project p ");
		sql.append("JOIN tb_module m ON m.project_id = p.id ");
		sql.append("JOIN tb_page ON tb_page.module_id = m.id ");
		sql.append("JOIN tb_action_and_page anp ON anp.page_id = tb_page.id ");
		sql.append("JOIN tb_action a ON a.id = anp.action_id ");
		sql.append("WHERE p.id = :projectId ");
		Query query = getSession().createSQLQuery(sql.toString());
		query.setInteger("projectId", projectId);

		List result = query.list();
		List<Integer> ids = new ArrayList<Integer>();
		for (Object r : result) {
			ids.add((Integer) r);
		}
		for (Integer id : ids) {
			list.add(this.getAction(id));
		}
		return list;
	}

    @Override
    public List<Project> selectMockNumTopNProjectList(int limit) {
        String hqlByUser = "from Project order by mockNum desc";
        Query query = getSession().createQuery(hqlByUser);
        return query.setMaxResults(limit).list();
    }
    
    public Action getActionByUrlAndProjectid(int projectId,String requestUrl) {
		//Action result =
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT a.id ");
		sql.append("FROM tb_project p ");
		sql.append("JOIN tb_module m ON m.project_id = p.id ");
		sql.append("JOIN tb_page ON tb_page.module_id = m.id ");
		sql.append("JOIN tb_action_and_page anp ON anp.page_id = tb_page.id ");
		sql.append("JOIN tb_action a ON a.id = anp.action_id ");
		sql.append("WHERE p.id = :projectId ");
		sql.append("AND a.request_url = :requestUrl ");
		Query query = getSession().createSQLQuery(sql.toString());
		query.setInteger("projectId", projectId);
		query.setString("requestUrl", requestUrl);
		List result = query.list();
		if (result.size()>0){
			Integer id = (Integer)result.get(0);
			return this.getAction(id);
		}
		return null;
	}
    
	@Override
	public List<Integer> getActionIdsByProjectId(int projectId) {
		//List<Integer> ids = new ArrayList<Integer>();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT a.id ");
		sql.append("FROM tb_project p ");
		sql.append("JOIN tb_module m ON m.project_id = p.id ");
		sql.append("JOIN tb_page ON tb_page.module_id = m.id ");
		sql.append("JOIN tb_action_and_page anp ON anp.page_id = tb_page.id ");
		sql.append("JOIN tb_action a ON a.id = anp.action_id ");
		sql.append("WHERE p.id = :projectId ");
		Query query = getSession().createSQLQuery(sql.toString());
		query.setInteger("projectId", projectId);

		List<Integer> result = query.list();
		
		/*for (Object r : result) {
			ids.add((Integer) r);
		}*/
		return result;
	}

    //获取该项目的所有用户，包括读写、只读用户
    public List<ProjectUser> getAllProjectUser(int projectId){
    	List<ProjectUser> list = new ArrayList<ProjectUser>();
    	StringBuilder sql = new StringBuilder();
    	 sql.append("FROM ProjectUser t ")
         .append("WHERE t.projectId = :projectId ");
    	Query query = getSession().createQuery(sql.toString());
 		
		query.setInteger("projectId", projectId);
		list = query.list();
    	return list;
    }
	@Override
	//accesslevel=2,readonly user
	public List<ProjectUser> getReadOnlyProjectUser(int projectId) {
		List<ProjectUser> users = getAllProjectUser(projectId);
		List<ProjectUser> result = new ArrayList<ProjectUser>();
		for (ProjectUser pu : users){
			if (pu.getAccesslevel()==2){
				result.add(pu);
			}
		}
		return result;
	}
	@Override
	//accesslevel=1,read-write user
	public List<ProjectUser> getRWProjectUser(int projectId) {
		List<ProjectUser> users = getAllProjectUser(projectId);
		List<ProjectUser> result = new ArrayList<ProjectUser>();
		for (ProjectUser pu : users){
			if (pu.getAccesslevel()==1){
				result.add(pu);
			}
		}
		return result;
	}
	@Override
	public void deleteProjectUser(int projectId, int accesslevel) {
		StringBuilder sql = new StringBuilder();
		sql.append("DELETE");
		sql.append(" FROM tb_project_and_user");		
		sql.append(" WHERE project_id = :projectId AND access_level = :accesslevel");
		Query query = getSession().createSQLQuery(sql.toString());
		query.setInteger("projectId", projectId);
		query.setInteger("accesslevel", accesslevel);
		query.executeUpdate();
	}
	@Override
	public void createProjectUser(int projectId, int accesslevel,long userId) {
		Session session = getSession();
		ProjectUser pu = new ProjectUser();
		pu.setAccesslevel(accesslevel);
		pu.setProjectId(projectId);
		pu.setUserId(userId);
		session.save(pu);		
	}
	
	//更新项目API的通用信息
	public int updateCommonDesc(int projectId,String commonDesc) {		
		String sql = "UPDATE tb_project SET commondesc = :commondesc where id=:projectId";
		Query query = getSession().createSQLQuery(sql);
		query.setString("commondesc", commonDesc);
		query.setInteger("projectId", projectId);
		return query.executeUpdate();
	}
	//获取项目API的通用信息
	public String getCommonDesc(int projectId){    	
    	Project project = getProject(projectId);    	
    	return project.getCommondesc();
	}
}