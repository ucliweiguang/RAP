package com.taobao.rigel.rap.workspace.web.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.gson.Gson;
import com.taobao.rigel.rap.common.SystemConstant;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.taobao.rigel.rap.account.bo.Notification;
import com.taobao.rigel.rap.account.bo.User;
import com.taobao.rigel.rap.common.ActionBase;
import com.taobao.rigel.rap.common.ContextManager;
import com.taobao.rigel.rap.common.MapUtils;
import com.taobao.rigel.rap.project.bo.Module;
import com.taobao.rigel.rap.project.bo.Project;
import com.taobao.rigel.rap.project.service.ProjectMgr;
import com.taobao.rigel.rap.validation.service.ValidationMgr;
import com.taobao.rigel.rap.workspace.bo.CheckIn;
import com.taobao.rigel.rap.workspace.bo.Save;
import com.taobao.rigel.rap.workspace.bo.Workspace;
import com.taobao.rigel.rap.workspace.service.WorkspaceMgr;

public class WorkspaceAction extends ActionBase {

	private static final long serialVersionUID = 1L;
	//是否可访问该项目
	private boolean accessable;
	//是否可管理（写文档）
	private boolean manageable;
	
	public boolean isManageable() {
		return manageable;
	}

	public void setManageable(boolean manageable) {
		this.manageable = manageable;
	}

	public boolean isAccessable() {
		return accessable;
	}

	public void setAccessable(boolean accessable) {
		this.accessable = accessable;
	}

	private boolean mock;

	private int actionId;

	public int getActionId() {
		return actionId;
	}

	public void setActionId(int actionId) {
		this.actionId = actionId;
	}

	public boolean isMock() {
		return mock;
	}

	public void setMock(boolean mock) {
		this.mock = mock;
	}

	private int id;

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private String workspaceJsonString;

	public String getWorkspaceJsonString() {
		return this.workspaceJsonString;
	}

	public void setWorkspaceJsonString(String workspaceJsonString) {
		this.workspaceJsonString = workspaceJsonString;
	}

	private Workspace workspace;

	public Workspace getWorkspace() {
		return this.workspace;
	}

	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

	public Module module;

	public Module getModule() {
		return this.module;
	}

	public void setModule(Module module) {
		this.module = module;
	}

	private VelocityEngine velocityEngine;

	public VelocityEngine getVelocityEngine() {
		return velocityEngine;
	}

	public void setVelocityEngine(VelocityEngine velocityEngine) {
		this.velocityEngine = velocityEngine;
	}

	private int projectId;

	private Project project;

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public int getProjectId() {
		return this.projectId;
	}

	public void setProjectId(int projectId) {
		this.projectId = projectId;
	}

	private String projectData;

	public String getProjectData() {
		return projectData;
	}

	public void setProjectData(String projectData) {
		this.projectData = projectData;
	}

	private String projectDataOriginal;

	public String getProjectDataOriginal() {
		return projectDataOriginal;
	}

	public void setProjectDataOriginal(String projectDataOriginal) {
		this.projectDataOriginal = projectDataOriginal;
	}

	private String saveListJson;

	public String getSaveListJson() {
		return saveListJson;
	}

	public void setSaveListJson(String saveListJson) {
		this.saveListJson = saveListJson;
	}

	private int saveId = -1;

	public int getSaveId() {
		return saveId;
	}

	public void setSaveId(int saveId) {
		this.saveId = saveId;
	}

	private Save save;

	public Save getSave() {
		return save;
	}

	public void setSave(Save save) {
		this.save = save;
	}

	private int versionId;

	public int getVersionId() {
		return versionId;
	}

	public void setVersionId(int versionId) {
		this.versionId = versionId;
	}

	private String deletedObjectListData;

	public String getDeletedObjectListData() {
		return deletedObjectListData;
	}

	public void setDeletedObjectListData(String deletedObjectListData) {
		this.deletedObjectListData = deletedObjectListData;
	}

	private String tag;

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * from 1 to 4, version position
	 */
	private int versionPosition;

	public int getVersionPosition() {
		return versionPosition;
	}

	public void setVersionPosition(int versionPosition) {
		this.versionPosition = versionPosition;
	}

	private String description;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private boolean isLocked;

	public boolean getIsLocked() {
		return isLocked;
	}

	public void setIsLocked(boolean isLocked) {
		this.isLocked = isLocked;
	}

	private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager
			.getFormatterLogger(WorkspaceAction.class.getName());

	public String myWorkspace() {
		if (!isUserLogined()) {
			plsLogin();
			setRelativeReturnUrl("/workspace/myWorkspace.action?projectId=" + projectId);
			return LOGIN;
		}
		Project p = projectMgr.getProject(getProjectId());
		if (p == null || p.getId() <= 0) {
			setErrMsg("该项目不存在或已被删除，会不会是亲这个链接保存的太久了呢？0  .0");
			logger.error("Unexpected project id=%d", getProjectId());
			return ERROR;
		}
		Workspace workspace = new Workspace();
		workspace.setProject(p);
		setWorkspaceJsonString(workspace.toString());
		setWorkspace(workspace);
		//modified by liweiguang 2016-1-18
		setAccessable(getAccountMgr().canUserAccessProject(getCurUserId(), getProjectId()));
		setManageable(getAccountMgr().canUserManageProject(getCurUserId(), getProjectId()));
		//modified by liweiguang 2016-1-18 end
		return SUCCESS;
	}

	private InputStream fileInputStream;

	/**
	 * save workspace if this.saveId == -1(default), it's a new save and needs
	 * projectId and id(workspaceId). else, just need saveId only to cover the
	 * existed one. all conditions parameters: projectData
	 * 
	 * @return
	 */
	/*
	 * public String updateSave() { int id = getSaveId(); Save save = null; if
	 * (id == -1) { save = new Save(); save.setProjectId(getProjectId());
	 * save.setWorkspaceId(getId()); } else { save =
	 * workspaceMgr.getSave(getSaveId()); }
	 * save.setProjectData(getProjectData()); id =
	 * workspaceMgr.updateSave(save);
	 * 
	 * // after update the save, return saveList json string
	 * setupSaveListJson(); return SUCCESS; }
	 */

	/*
	 * private void setupSaveListJson() { Set<Save> saveList =
	 * workspaceMgr.getSaveList(getId()); StringBuilder stringBuilder = new
	 * StringBuilder(); stringBuilder.append("["); Iterator<Save> iterator =
	 * saveList.iterator(); while (iterator.hasNext()) {
	 * stringBuilder.append(iterator.next()); if (iterator.hasNext()) {
	 * stringBuilder.append(","); } } stringBuilder.append("]");
	 * setSaveListJson(stringBuilder.toString()); }
	 */

	/**
	 * delete save
	 * 
	 * @return
	 */
	/*
	 * public String removeSave() { workspaceMgr.removeSave(getSaveId());
	 * setupSaveListJson(); return SUCCESS; }
	 */

	/**
	 * load save, saveId must be significant, or operation failed
	 * 
	 * @return the save object loaded
	 */
	/*
	 * public String querySave() {
	 * setJson(workspaceMgr.getSave(getSaveId()).getProjectData()); return
	 * SUCCESS; }
	 */

	public InputStream getFileInputStream() {
		return fileInputStream;
	}

	/**
	 * ` save the current workspace
	 * 
	 * @return
	 */
	/*
	 * public String updateCurrentSave() { Workspace workspace =
	 * workspaceMgr.getWorkspace(getId());
	 * workspace.setProjectData(getProjectData());
	 * workspaceMgr.updateWorkspace(workspace); return SUCCESS; }
	 */

	private WorkspaceMgr workspaceMgr;

	public WorkspaceMgr getworkspaceMgr() {
		return workspaceMgr;
	}

	public void setWorkspaceMgr(WorkspaceMgr workspaceMgr) {
		this.workspaceMgr = workspaceMgr;
	}

	private ProjectMgr projectMgr;

	public ProjectMgr projectMgr() {
		return this.projectMgr;
	}

	public void setProjectMgr(ProjectMgr projectMgr) {
		this.projectMgr = projectMgr;
	}

	private ValidationMgr validationMgr;

	public ValidationMgr getValidationMgr() {
		return validationMgr;
	}

	public void setValidationMgr(ValidationMgr validationMgr) {
		this.validationMgr = validationMgr;
	}

	public String ping() {
		setJson("{\"isOk\":true}");
		return SUCCESS;
	}

	public String queryVersion() {
		setJson(workspaceMgr.getVersion(getVersionId()).toString(CheckIn.ToStringType.COMPLETED));
		return SUCCESS;
	}

	public String switchVersion() {
		CheckIn check = workspaceMgr.getVersion(getVersionId());
		workspaceMgr.prepareForVersionSwitch(check);
		projectMgr.updateProject(	check.getProject().getId(), check.getProjectData(), "[]",
									new HashMap<Long, Long>());
		Project project = projectMgr.getProject(check.getProject().getId());
		String projectData = project.toString(Project.TO_STRING_TYPE.TO_PARAMETER);
		setJson("{\"projectData\":" + projectData + ", \"isOk\":true}");
		project.setProjectData(projectData);
		projectMgr.updateProject(project);
		return SUCCESS;
	}

	public String checkIn() throws Exception {
		User curUser = getCurUser();
		if (curUser == null) {
			setErrMsg(LOGIN_WARN_MSG);
			setIsOk(false);
			logger.error("Unlogined user trying to checkin and failed.");
			return JSON_ERROR;
		}

		if (!getAccountMgr().canUserManageProject(getCurUserId(), getId())) {
			setErrMsg("access deny");
			setIsOk(false);
			logger.error(	"User %s trying to checkedin project(id=$d) and denied.",
							getCurAccount(),
							getId());
			return JSON_ERROR;
		}
		/*//save pb content
		String requestPBParameters = getRequestPBParameters();
		String responsePBParameters = getResponsePBParameters();
		System.out.println("requestPBParameters:"+ requestPBParameters);
		System.out.println("responsePBParameters:"+ responsePBParameters);*/

		// update project
		Map<Long, Long> actionIdMap = new HashMap<Long, Long>();
		projectMgr
				.updateProject(getId(), getProjectData(), getDeletedObjectListData(), actionIdMap);

		project = projectMgr.getProject(getId());

		// generate one check-in of VSS mode submit
		CheckIn checkIn = new CheckIn();
		checkIn.setCreateDate(new Date());
		checkIn.setDescription(getDescription());
		checkIn.setProject(project);
		checkIn.setProjectData(project.toString(Project.TO_STRING_TYPE.TO_PARAMETER));
		checkIn.setTag(getTag());
		checkIn.setUser(curUser);
		checkIn.setVersion(project.getVersion());
		checkIn.versionUpgrade(getVersionPosition());

		// after version upgrade, set back to project
		project.setVersion(checkIn.getVersion());
		checkIn.setWorkspaceMode(Workspace.ModeType.VSS);
		workspaceMgr.addCheckIn(checkIn);

		// calculate JSON string for client
		project = projectMgr.getProject(getId());
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("{\"projectData\":" + checkIn.getProjectData());
		stringBuilder.append(",\"checkList\":[");
		Iterator<CheckIn> iterator = project.getCheckInListOrdered().iterator();
		while (iterator.hasNext()) {
			stringBuilder.append(iterator.next());
			if (iterator.hasNext()) {
				stringBuilder.append(",");
			}
		}
		Gson g = new Gson();
		stringBuilder.append("],\"actionIdMap\":").append(g.toJson(actionIdMap))
				.append(",\"isOk\":true}");
		setJson(stringBuilder.toString());

		// update project data
		project.setProjectData(checkIn.getProjectData());
		projectMgr.updateProject(project);

		// unlock the workspace
		unlock();

		// notification for doc change
		/*
		for (User user : project.getUserList()) {
			Notification notification = new Notification();
			notification.setParam1(new Integer(id).toString());
			notification.setParam2(project.getName());
			notification.setTypeId((short) 1);
			notification.setTargetUser(getCurUser());
			notification.setUser(user);
			if (notification.getUser().getId() != getCurUserId())
				getAccountMgr().addNotification(notification);
		}

		Notification notification = new Notification();
		notification.setParam1(new Integer(id).toString());
		notification.setParam2(project.getName());
		notification.setTypeId((short) 1);
		notification.setTargetUser(getCurUser());
		notification.setUser(project.getUser());
		if (notification.getUser().getId() != getCurUserId())
			getAccountMgr().addNotification(notification);
		*/
		// unfinished

		Callable<String> taskSub = new Callable<String>() {

			@Override
			public String call() throws Exception {
				try {
					// async update doc
					// projectMgr.updateDoc(id);
					// async update disableCache
					projectMgr.updateCache(id);
					// async update batch jsonschem  added by liweiguang 2016-1-15
					//System.out.println("in call....id:"+id);
					validationMgr.generateJsonSchemaByProject(id);
					//add by liweiguang 2016-03-01
					validationMgr.generateMockdataByProject(id);
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				return null;
			}
		};

		FutureTask<String> futureTask = new FutureTask<String>(taskSub);
		Thread asyncThread = new Thread(futureTask);
		asyncThread.start();
		logger.info("Future task CHECK_IN running...");

		return SUCCESS;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String lock() {
		long curUserId = getCurUserId();
		if (curUserId <= 0) {
			setIsOk(false);
			setErrMsg(LOGIN_WARN_MSG);
			return JSON_ERROR;
		}

		boolean isOk = false;
		if (isLocked(getId())) {
			// if the project is locked, find the locker
			User user = getLocker(getId());
			if (!user.getAccount().equals(getCurAccount())) {
				setJson("{\"isOk\":false, \"errMsg\":\"该项目目前正被" + user.getName() + "锁定.\"}");
			} else {
				// user request lock a locked project
				// which is locked by himself, so let him go
				isOk = true;
			}

		} else {
			// else, lock the project, than let him go.
			Map app = ContextManager.getApplication();
			if (app.get(ContextManager.KEY_PROJECT_LOCK_LIST) == null) {
				app.put(ContextManager.KEY_PROJECT_LOCK_LIST, new HashMap());
			}
			Map projectLockList = (Map) app.get(ContextManager.KEY_PROJECT_LOCK_LIST);
			if (projectLockList.get(curUserId) == null) {
				projectLockList.put(curUserId, getId());
				// System.out.println("user[" + curUserId + "] locked project["+
				// getId() + "]");
			}
			isOk = true;
		}
		if (isOk) {
			setJson("{\"isOk\":true, \"projectData\":"
					+ projectMgr.getProject(getId()).getProjectData() + "}");
		}
		return SUCCESS;
	}

	@SuppressWarnings({ "rawtypes" })
	public String unlock() {
		if (isLocked(getId())) {
			Map app = ContextManager.getApplication();
			Map projectLockList = (Map) app.get(ContextManager.KEY_PROJECT_LOCK_LIST);
			if (projectLockList == null)
				return SUCCESS;
			long userId = super.getCurUserId();
			int projectId = (Integer) projectLockList.get(userId);
			projectLockList.remove(userId);
			logger.info("user[%d] unlock project[%d]", userId, projectId);
		}
		return SUCCESS;
	}

	/**
	 * caution: no authentication so far
	 * 
	 * @return
	 * @throws Exception
	 */
	public String export() throws Exception {
		project = projectMgr.getProject(projectId);
		velocityEngine.init();
		VelocityContext context = new VelocityContext();
		context.put("project", project);
		Template template = null;
		try {
			template = velocityEngine.getTemplate("resource/export.vm", "UTF8");
		} catch (ResourceNotFoundException rnfe) {
			rnfe.printStackTrace();
		} catch (ParseErrorException pee) {
			pee.printStackTrace();
		} catch (MethodInvocationException mie) {
			mie.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		StringWriter sw = new StringWriter();
		template.merge(context, sw);
		fileInputStream = new ByteArrayInputStream(sw.toString().getBytes("UTF8"));
		return SUCCESS;
	}

	public String onlineDocs() throws Exception {
		project = projectMgr.getProject(projectId);
		velocityEngine.init();
		VelocityContext context = new VelocityContext();
		context.put("project", project);
		Template template = null;
		try {
			template = velocityEngine.getTemplate("resource/export.vm", "UTF8");
		} catch (ResourceNotFoundException rnfe) {
			rnfe.printStackTrace();
		} catch (ParseErrorException pee) {
			pee.printStackTrace();
		} catch (MethodInvocationException mie) {
			mie.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		StringWriter sw = new StringWriter();
		template.merge(context, sw);

		return SUCCESS;
	}

	@SuppressWarnings("rawtypes")
	private boolean isLocked(int projectId) {
		Map app = ContextManager.getApplication();
		Map projectLockList = (Map) app.get(ContextManager.KEY_PROJECT_LOCK_LIST);
		return projectLockList != null && projectLockList.containsValue(projectId) ? true : false;
	}

	@SuppressWarnings("rawtypes")
	private User getLocker(int projectId) {
		Map app = ContextManager.getApplication();
		Map projectLockList = (Map) app.get(ContextManager.KEY_PROJECT_LOCK_LIST);
		if (projectLockList != null) {
			long userId = (Long) MapUtils.getKeyByValue(projectLockList, projectId);
			User user = getAccountMgr().getUser(userId);
			return user;
		}
		return null;
	}

	public String __init__() {
		// prevent repeated intialization of servcie

		if (SystemConstant.serviceInitialized) {
			return SUCCESS;
		}

		SystemConstant.serviceInitialized = true;

		List<Project> list = projectMgr.getProjectList();
		for (Project p : list) {
			projectMgr.updateDoc(p.getId());
		}
		return SUCCESS;
	}

	////////////////////////CURL///////////
	/**
	 * 
	 * 功能描述：generate curl for API
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2016-1-11
	 */
	public String generateCURL() {
		long curUserId = getCurUserId();
		if (curUserId <= 0) {
			setIsOk(false);
			setErrMsg(LOGIN_WARN_MSG);
			return JSON_ERROR;
		}

		boolean isOk = false;
		//System.out.println("ActionId():" + this.actionId);		
		validationMgr.saveCURL(this.actionId, validationMgr.generateCURL(this.actionId));
		isOk = true;

		if (isOk) {
			setJson("{\"isOk\":true}");
		}
		return SUCCESS;
	}

	/**
	 * 
	 * 功能描述：保存修改好的cURL内容
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2016-1-11
	 */
	public String getCURL() {
		long curUserId = getCurUserId();
		if (curUserId <= 0) {
			setIsOk(false);
			setErrMsg(LOGIN_WARN_MSG);
			return JSON_ERROR;
		}

		boolean isOk = false;
		//先生成最新的CURL
		validationMgr.saveCURL(this.actionId, validationMgr.generateCURL(this.actionId));
		
		String cURL = validationMgr.getCURLByActionId(actionId);
		isOk = true;

		if (isOk) {
			setJson("{\"isOk\":true,\"cURL\":\"" + string2Json(cURL) + "\"}");
		}
		return SUCCESS;
	}

	private String string2Json(String s) {
		if(s==null) return "";
    	StringBuffer sb = new StringBuffer ();       
	    for (int i=0; i<s.length(); i++) {    	     
	        char c = s.charAt(i);       
	        switch (c) {       
	        case '\"':       
	            sb.append("\\\"");       
	            break;       
	        case '\\':       
	            sb.append("\\\\");       
	            break;       
	        case '/':       
	            sb.append("\\/");       
	            break;       
	        case '\b':       
	            sb.append("\\b");       
	            break;       
	        case '\f':       
	            sb.append("\\f");       
	            break;       
	        case '\n':       
	            sb.append("\\n");       
	            break;       
	        case '\r':       
	            sb.append("\\r");       
	            break;       
	        case '\t':       
	            sb.append("\\t");       
	            break;       
	        default:       
	            sb.append(c);       
	        }    	
	    }
	    return sb.toString();
	}

	private String newCURL;

	public String getNewCURL() {
		return newCURL;
	}

	public void setNewCURL(String newCURL) {
		this.newCURL = newCURL;
	}

	/**
	 * 
	 * 功能描述：保存修改好的curl内容
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2016-1-11
	 */
	public String saveCURL() {
		long curUserId = getCurUserId();
		if (curUserId <= 0) {
			setIsOk(false);
			setErrMsg(LOGIN_WARN_MSG);
			return JSON_ERROR;
		}

		boolean isOk = false;
		//System.out.println("actionId:" + actionId);
		//System.out.println("newCURL:" + newCURL);
		validationMgr.saveCURL(actionId, newCURL);
		isOk = true;

		if (isOk) {
			setJson("{\"isOk\":true}");
		}
		return SUCCESS;
	}

	////////////////////////JsonSchema///////////
	/**
	 * 
	 * 功能描述：generate JsonSchema for every API
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-8-31
	 */
	public String generateJsonSchema() {
		long curUserId = getCurUserId();
		if (curUserId <= 0) {
			setIsOk(false);
			setErrMsg(LOGIN_WARN_MSG);
			return JSON_ERROR;
		}

		boolean isOk = false;
		//System.out.println("ActionId():" + this.actionId);
		//System.out.println("projectId:"+ this.projectId);
		validationMgr
				.saveJsonSchema(this.actionId, validationMgr.generateJsonSchema(this.actionId,this.projectId));
		isOk = true;

		if (isOk) {
			setJson("{\"isOk\":true}");
		}
		return SUCCESS;
	}

	/**
	 * 
	 * 功能描述：获取jsonschema内容
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-9-1
	 */
	public String getJsonSchema() {
		//System.out.println("getJsonSchema()this.actionId:" + actionId);
		long curUserId = getCurUserId();
		if (curUserId <= 0) {
			setIsOk(false);
			setErrMsg(LOGIN_WARN_MSG);
			return JSON_ERROR;
		}

		boolean isOk = false;
		String jsonSchema = validationMgr.getJsonSchemaByActionId(actionId);
		isOk = true;

		if (isOk) {
			setJson("{\"isOk\":true,\"jsonSchema\":" + jsonSchema + "}");
		}
		return SUCCESS;
	}

	private String newJsonSchema;

	public String getNewJsonSchema() {
		return newJsonSchema;
	}

	public void setNewJsonSchema(String newJsonSchema) {
		this.newJsonSchema = newJsonSchema;
	}

	/**
	 * 
	 * 功能描述：保存修改好的jsonschema内容
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-9-1
	 */
	public String saveJsonSchema() {
		long curUserId = getCurUserId();
		if (curUserId <= 0) {
			setIsOk(false);
			setErrMsg(LOGIN_WARN_MSG);
			return JSON_ERROR;
		}

		boolean isOk = false;
		//System.out.println("actionId:" + actionId);
		//System.out.println("newJsonSchema:" + newJsonSchema);
		validationMgr.saveJsonSchema(actionId, newJsonSchema);
		isOk = true;

		if (isOk) {
			setJson("{\"isOk\":true}");
		}
		return SUCCESS;
	}

	/**
	 * 
	 * 功能描述：校验改动过的jsonschema
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-9-2
	 * @throws IOException 
	 * @throws ProcessingException 
	 */
	public String validateJsonSchema() throws ProcessingException, IOException {
		boolean isOk = false;
		//System.out.println("newJsonSchema:" + newJsonSchema);
		String result = validationMgr.validateJsonSchema(newJsonSchema);
		if ("".equals(result)) {
			result = "JsonSchema内容格式没有错误，可放心保存.";
		}
		isOk = true;
		//System.out.println("result:" + result.replaceAll("\"", "'"));
		if (isOk) {
			setJson("{\"isOk\":true,\"result\":\"" + result.replaceAll("\"", "'") + "\"}");
		}
		return SUCCESS;
	}

	private String pbtxt;
	private int reflag;

	public String getPbtxt() {
		return pbtxt;
	}

	public void setPbtxt(String pbtxt) {
		this.pbtxt = pbtxt;
	}

	public int getReflag() {
		return reflag;
	}

	public void setReflag(int reflag) {
		this.reflag = reflag;
	}

	private String pbrequest;
	private String pbresponse;

	public String getPbrequest() {
		return pbrequest;
	}

	public void setPbrequest(String pbrequest) {
		this.pbrequest = pbrequest;
	}

	public String getPbresponse() {
		return pbresponse;
	}

	public void setPbresponse(String pbresponse) {
		this.pbresponse = pbresponse;
	}

	/**
	 * 
	 * 功能描述：保存用户提交的PB协议的文本内容(用户导入pb的时候)
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2015-10-20
	 */
	public String savePB() {
		long curUserId = getCurUserId();
		if (curUserId <= 0) {
			setIsOk(false);
			setErrMsg(LOGIN_WARN_MSG);
			return JSON_ERROR;
		}

		boolean isOk = false;
		validationMgr.savePB(actionId, pbtxt, reflag);
		isOk = true;

		if (isOk) {
			setJson("{\"isOk\":true}");
		}
		return SUCCESS;
	}

	//查看PB协议的内容
	public String getPB() {
		boolean isOk = false;
		Map<String, String> result = validationMgr.getPB(actionId);
		String pbrequest = result.get("pbrequest");
		String pbresponse = result.get("pbresponse");
		if (pbrequest == null)
			pbrequest = "";
		if (pbresponse == null)
			pbresponse = "";

		isOk = true;
		//System.out.println("result:" + result.replaceAll("\"", "'"));
		//System.out.println("result:" + result);
		if (isOk) {
			setJson("{\"isOk\":true,\"pbrequest\":\"" + pbrequest.replaceAll("\\n", "\\\\n")
					+ "\"," + "\"pbresponse\":\"" + pbresponse.replaceAll("\\n", "\\\\n") + "\"}");
		}
		return SUCCESS;
	}

	//在查看PB协议内容后执行更新内容操作
	public String updatePB() {
		long curUserId = getCurUserId();
		if (curUserId <= 0) {
			setIsOk(false);
			setErrMsg(LOGIN_WARN_MSG);
			return JSON_ERROR;
		}

		boolean isOk = false;
		validationMgr.updatePB(actionId, pbrequest, pbresponse);
		isOk = true;

		if (isOk) {
			setJson("{\"isOk\":true}");
		}
		return SUCCESS;
	}
}
