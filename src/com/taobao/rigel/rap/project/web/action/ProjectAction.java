package com.taobao.rigel.rap.project.web.action;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.struts2.ServletActionContext;
import org.apache.tomcat.util.http.fileupload.DiskFileUpload;

import com.google.gson.Gson;
import com.taobao.rigel.rap.account.bo.User;
import com.taobao.rigel.rap.auto.generate.bo.VelocityTemplateGenerator;
import com.taobao.rigel.rap.auto.generate.contract.Generator;
import com.taobao.rigel.rap.common.ActionBase;
import com.taobao.rigel.rap.common.RapError;
import com.taobao.rigel.rap.log.bo.RapLog;
import com.taobao.rigel.rap.log.service.LogMgr;
import com.taobao.rigel.rap.project.bo.CommonModel;
import com.taobao.rigel.rap.project.bo.Page;
import com.taobao.rigel.rap.project.bo.Project;
import com.taobao.rigel.rap.project.service.ProjectMgr;
import com.taobao.rigel.rap.workspace.service.WorkspaceMgr;

public class ProjectAction extends ActionBase {
	private LogMgr logMgr;
	
	public LogMgr getLogMgr() {
		return logMgr;
	}

	public void setLogMgr(LogMgr logMgr) {
		this.logMgr = logMgr;
	}
	private int id;

	private List<Project> searchResult;

	private String ids;

	public String getIds() {
		if (ids == null || ids.isEmpty()) {
			return "";
		}

		String[] idList = ids.split(",");
		String returnVal = "";
		Integer num = 0;
		int counter = 0;
		for (String id : idList) {
			try {
				num = Integer.parseInt(id);
				counter++;
				if (counter > 1) {
					returnVal += ",";
				}
				returnVal += num.toString();

			} catch (Exception ex) {

			}
		}
		return returnVal;
	}

	public void setIds(String ids) {
		this.ids = ids;
	}

	public List<Project> getSearchResult() {
		return searchResult;
	}

	private String key;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private String desc;

	private int groupId;

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	private String accounts;

	private String readonlyaccounts;

	public String getReadonlyaccounts() {
		if (readonlyaccounts == null)
			return "";
		return readonlyaccounts;
	}

	public void setReadonlyaccounts(String readonlyaccounts) {
		this.readonlyaccounts = readonlyaccounts;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getAccounts() {
		if (accounts == null)
			return "";
		return accounts;
	}

	public void setAccounts(String accounts) {
		this.accounts = accounts;
	}

	private String commonDesc;

	public String getCommonDesc() {
		return commonDesc;
	}

	public void setCommonDesc(String commonDesc) {
		this.commonDesc = commonDesc;
	}

	public List<String> getMemberAccountList() {
		List<String> memberList = new ArrayList<String>();
		for (User user : super.getAccountMgr().getUserList()) {
			memberList.add(user.getAccount() + "(" + user.getName() + ")");
		}
		return memberList;
	}

	private String memberAccountListStr;

	public String getMemberAccountListStr() {
		return memberAccountListStr;
	}

	public void setMemberAccountListStr(String memberAccountListStr) {
		this.memberAccountListStr = memberAccountListStr;
	}

	private String name;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private Date createDate;

	public Date getCreateDate() {
		return this.createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	private String introduction;

	public String getIntroduction() {
		return this.introduction;
	}

	public void setIntroduction(String introduction) {
		this.introduction = introduction;
	}

	private List<Project> projectList;

	public List<Project> getProjectList() {
		return this.projectList;
	}

	public void setProjectList(List<Project> projectList) {
		this.projectList = projectList;
	}

	private Project project;

	public Project getProject() {
		return this.project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	private static final long serialVersionUID = 1L;

	private ProjectMgr projectMgr;

	/**
	 * get project manager
	 * 
	 * @return project manager
	 */
	public ProjectMgr getProjectMgr() {
		return projectMgr;
	}

	public void setProjectMgr(ProjectMgr projectMgr) {
		this.projectMgr = projectMgr;
	}

	private WorkspaceMgr workspaceMgr;

	public WorkspaceMgr getWorkspaceMgr() {
		return workspaceMgr;
	}

	public void setWorkspaceMgr(WorkspaceMgr workspaceMgr) {
		this.workspaceMgr = workspaceMgr;
	}

	private int pageId;

	public int getPageId() {
		return pageId;
	}

	public void setPageId(int pageId) {
		this.pageId = pageId;
	}

	private String projectData;

	public String getProjectData() {
		return projectData;
	}

	public void setProjectData(String projectData) {
		this.projectData = projectData;
	}

	public String delete() {
		if (!isUserLogined())
			return LOGIN;
		if (!getAccountMgr().canUserManageProject(getCurUserId(), getId())) {
			setErrMsg("您没有管理该项目的权限");
			return ERROR;
		}
		projectMgr.removeProject(getId());
		return SUCCESS;
	}

	public String create() {
		if (!isUserLogined())
			return LOGIN;
		Gson gson = new Gson();
		Project project = new Project();
		project.setCreateDate(new Date());
		project.setUser(getCurUser());
		project.setIntroduction(getDesc());
		project.setName(getName());
		project.setGroupId(groupId);
		List<String> memberAccountList = new ArrayList<String>();
		String[] list = getAccounts().split(",");
		// format: mashengbo(大灰狼堡森), linpanhui(林攀辉),
		for (String item : list) {
			String account = item.contains("(") ? item.substring(0, item.indexOf("(")).trim()
												: item.trim();
			if (!account.equals(""))
				memberAccountList.add(account);
		}
		project.setMemberAccountList(memberAccountList);
		int projectId = projectMgr.addProject(project);
		project = projectMgr.getProject(projectId);
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("id", project.getId());
		result.put("name", project.getName());
		result.put("desc", project.getIntroduction());
		result.put("accounts", project.getMemberAccountListStr());
		result.put("groupId", project.getGroupId());
		result.put("isManagable", "true");
		result.put("creator", project.getUser().getUserBaseInfo());
		setJson(new RapError(gson.toJson(result)).toString());
		return SUCCESS;
	}

	public String update() {
		if (!isUserLogined())
			return LOGIN;
		if (!getAccountMgr().canUserManageProject(getCurUserId(), getId())) {
			setErrMsg("您没有管理该项目的权限");
			return ERROR;
		}
		Project project = new Project();
		project.setId(getId());
		project.setIntroduction(getDesc());
		project.setName(getName());
		project.setUser(getCurUser());
		List<String> memberAccountList = new ArrayList<String>();
		String[] list = getAccounts().split(",");
		// format: mashengbo(大灰狼堡森), linpanhui(林攀辉),
		for (String item : list) {
			String account = item.contains("(") ? item.substring(0, item.indexOf("(")).trim()
												: item.trim();
			if (!account.equals(""))
				memberAccountList.add(account);
		}

		//added by liweiguang 2016-1-18
		List<String> readonlymemberAccountList = new ArrayList<String>();
		String[] readonlylist = getReadonlyaccounts().split(",");
		for (String item : readonlylist) {
			String account = item.contains("(") ? item.substring(0, item.indexOf("(")).trim()
												: item.trim();
			if (!account.equals(""))
				readonlymemberAccountList.add(account);
		}
		//added by liweiguang 2016-1-18 end

		Gson gson = new Gson();
		project.setMemberAccountList(memberAccountList);
		project.setReadonlyMemberAccountList(readonlymemberAccountList);//added by liweiguang 2016-1-18 end

		projectMgr.updateProject(project);
		project = projectMgr.getProject(project.getId());

		if (getCurUser().isUserInRole("admin") || getCurUser().getId() == project.getUser().getId()) {
			project.setIsManagable(true);
		}

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("id", project.getId());
		result.put("name", project.getName());
		result.put("desc", project.getIntroduction());
		result.put("accounts", project.getMemberAccountListStr());
		//readonlyaccounts
		result.put("readonlyaccounts", project.getReadonlyMemberAccountListStr());
		result.put("groupId", project.getGroupId());
		result.put("isManagable", project.getIsManagable());
		setJson(new RapError(gson.toJson(result)).toString());
		
		//记录操作日志
		RapLog log = new RapLog();		
		log.setIp(org.apache.struts2.ServletActionContext.getRequest().getRemoteAddr());
		log.setOperation("修改项目信息，含调整项目成员.项目名称："+project.getName());
		log.setUserName(getAccountMgr().getUser(getCurUserId()).getName());
		logMgr.createLog(log);
				
		return SUCCESS;
	}

	public String updateReleatedIds() {
		if (!isUserLogined())
			return LOGIN;
		if (!getAccountMgr().canUserManageProject(getCurUserId(), getId())) {
			setErrMsg("您没有管理该项目的权限");
			return ERROR;
		}

		Project project = projectMgr.getProject(getId());
		project.setRelatedIds(getIds());
		projectMgr.updateProject(project);

		return SUCCESS;
	}

	private String result;

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	private InputStream outputStream;

	public InputStream getOutputStream() {
		return outputStream;
	}

	public void setOutputStream(InputStream outputStream) {
		this.outputStream = outputStream;
	}

	public String autoGenerate() throws FileNotFoundException, UnsupportedEncodingException {
		Generator generator = new VelocityTemplateGenerator();
		Page page = projectMgr.getPage(getPageId());
		generator.setObject(page);
		String exportFileString = generator.doGenerate();
		outputStream = new ByteArrayInputStream(exportFileString.getBytes("UTF8"));
		return SUCCESS;
	}

	public String getGeneratedFileName() {
		return projectMgr.getPage(getPageId()).getTemplate();
	}

	public String search() {
		searchResult = projectMgr.search(key);
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		for (Project p : searchResult) {
			Map<String, Object> item = new HashMap<String, Object>();
			item.put("id", p.getId());
			item.put("name", p.getName());
			list.add(item);
		}
		Gson gson = new Gson();
		setJson(gson.toJson(list));
		return SUCCESS;
	}

	public String updateCommonDesc() {
		if (!isUserLogined())
			return LOGIN;
		if (!getAccountMgr().canUserManageProject(getCurUserId(), getId())) {
			setErrMsg("您没有管理该项目的权限");
			return ERROR;
		}
		//System.out.println("getId():"+getId()+"==>commonDesc:"+getCommonDesc());
		projectMgr.updateCommonDesc(getId(), getCommonDesc());
		
		//记录操作日志
		RapLog log = new RapLog();		
		log.setIp(org.apache.struts2.ServletActionContext.getRequest().getRemoteAddr());
		log.setOperation("编辑项目API通用信息.项目名称："+projectMgr.getProject(getId()).getName());
		log.setUserName(getAccountMgr().getUser(getCurUserId()).getName());
		logMgr.createLog(log);
		
		return SUCCESS;
	}

	public String showCommonDesc() {
		if (!isUserLogined())
			return LOGIN;
		if (!getAccountMgr().canUserManageProject(getCurUserId(), getId())) {
			setErrMsg("您没有管理该项目的权限");
			return ERROR;
		}
		String commonDesc = projectMgr.getCommonDesc(getId());
		if (commonDesc ==null) commonDesc ="";
		//System.out.println("commonDesc:"+commonDesc);
		//Gson gson = new Gson();
		//setJson(gson.toJson(commonDesc));
		/*String line = "";
		String commonModel = projectMgr.getCommonModelHTML(getId());
		if (commonModel == null || "".equals(commonModel)){
			line = "";
		} else {
			line = "<hr><h1>项目API公用模型</h1>";
		}*/		
		
		setText(commonDesc);//+line+commonModel
		return SUCCESS;
	}

	public String showCommonDescWithModel() {
		if (!isUserLogined())
			return LOGIN;
		if (!getAccountMgr().canUserManageProject(getCurUserId(), getId())) {
			setErrMsg("您没有管理该项目的权限");
			return ERROR;
		}
		String commonDesc = projectMgr.getCommonDesc(getId());
		if (commonDesc ==null) commonDesc ="";
		//System.out.println("commonDesc:"+commonDesc);
		//Gson gson = new Gson();
		//setJson(gson.toJson(commonDesc));
		String line = "";
		String commonModel = projectMgr.getCommonModelHTML(getId());
		//System.out.println("commonModel:"+commonModel);
		if (commonModel == null || "".equals(commonModel)){
			line = "";
		} else {
			line = "<hr><h1>项目API公用模型</h1>";
		}	
		
		setText(commonDesc+line+commonModel);//
		return SUCCESS;
	}
	//注意，file并不是指前端jsp上传过来的文件本身，而是文件上传过来存放在临时文件夹下面的文件
	private File file;
	//提交过来的file的名字
	private String fileFileName;
	private int projectId;
	private String fileContentType;

	public String getFileContentType() {
		return fileContentType;
	}

	public void setFileContentType(String fileContentType) {
		this.fileContentType = fileContentType;
	}

	public int getProjectId() {
		return projectId;
	}

	public void setProjectId(int projectId) {
		this.projectId = projectId;
	}

	public String getFileFileName() {
		return fileFileName;
	}

	public void setFileFileName(String fileFileName) {
		this.fileFileName = fileFileName;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String uploadcommonmodel() throws IOException {
		try {
			String fileDir = ServletActionContext.getServletContext().getRealPath("/upload");//上传文件存放目录
			File tempFile = new File(fileDir);
		    if (!tempFile.exists()) {
		      tempFile.mkdirs();
		    }
		    //写文件开始
		    InputStream originalIs = new FileInputStream(file);
		    OutputStream os = new FileOutputStream(new File(fileDir, fileFileName));
		    byte[] buffer = new byte[500];
			int length = 0;
			while (-1 != (length = originalIs.read(buffer, 0, buffer.length))) {
				os.write(buffer);
			}
			originalIs.close();
			os.close();
			//写文件结束
			
		    String excelFileName = fileDir + "/" + fileFileName;// 获取上传文件名,包括路径		    
			//System.out.println("fileDir:"+fileDir);
			//System.out.println("projectId:"+projectId);
			//System.out.println("fileContentType:"+fileContentType);
			//System.out.println("file:"+file);
			//System.out.println("excelFileName:"+excelFileName);
			File excelFile = new File(excelFileName);
			InputStream is = new FileInputStream(excelFile);			
	
			//System.out.println("fileFileName: " + fileFileName);//上传文件的文件名
			// 因为file是存放在临时文件夹的文件，我们可以将其文件名和文件路径打印出来，看和之前的fileFileName是否相同
			//System.out.println("fileName: " + file.getName());
			//System.out.println("filePath: " + file.getPath());			
			
			List<CommonModel> commonModels = projectMgr.readCommonModelListFromExcel(is);
			projectMgr.updateCommonModel(projectId, commonModels);
			projectMgr.updateModelFileName(projectId, fileFileName);
			is.close();			
	
			//记录操作日志
			RapLog log = new RapLog();		
			log.setIp(org.apache.struts2.ServletActionContext.getRequest().getRemoteAddr());
			log.setOperation("上传API公用模型.项目名称："+projectMgr.getProject(projectId).getName()+" 文件名："+fileFileName);
			log.setUserName(getAccountMgr().getUser(getCurUserId()).getName());
			logMgr.createLog(log);
			
			return SUCCESS;
		} catch (Exception e) {
			e.printStackTrace();
			return SUCCESS;
		}
	}
}
