package com.taobao.rigel.rap.project.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.gson.Gson;
import com.taobao.rigel.rap.account.bo.Notification;
import com.taobao.rigel.rap.account.bo.User;
import com.taobao.rigel.rap.account.dao.AccountDao;
import com.taobao.rigel.rap.account.service.AccountMgr;
import com.taobao.rigel.rap.common.ArrayUtils;
import com.taobao.rigel.rap.common.HTTPUtils;
import com.taobao.rigel.rap.common.SystemConstant;
import com.taobao.rigel.rap.organization.bo.Group;
import com.taobao.rigel.rap.organization.dao.OrganizationDao;
import com.taobao.rigel.rap.project.bo.Action;
import com.taobao.rigel.rap.project.bo.CommonModel;
import com.taobao.rigel.rap.project.bo.CommonModelField;
import com.taobao.rigel.rap.project.bo.Module;
import com.taobao.rigel.rap.project.bo.Page;
import com.taobao.rigel.rap.project.bo.Parameter;
import com.taobao.rigel.rap.project.bo.Project;
import com.taobao.rigel.rap.project.bo.ProjectUser;
import com.taobao.rigel.rap.project.dao.ProjectDao;
import com.taobao.rigel.rap.project.service.ProjectMgr;
import com.taobao.rigel.rap.workspace.bo.CheckIn;
import com.taobao.rigel.rap.workspace.dao.WorkspaceDao;

public class ProjectMgrImpl implements ProjectMgr {

	private ProjectDao projectDao;
	private OrganizationDao organizationDao;
	private AccountMgr accountMgr;
    private WorkspaceDao workspaceDao;

    public void setWorkspaceDao(WorkspaceDao workspaceDao) {
        this.workspaceDao = workspaceDao;
    }

    public AccountMgr getAccountMgr() {
		return accountMgr;
	}

	public void setAccountMgr(AccountMgr accountMgr) {
		this.accountMgr = accountMgr;
	}

	public OrganizationDao getOrganizationDao() {
		return organizationDao;
	}

	public void setOrganizationDao(OrganizationDao organizationDao) {
		this.organizationDao = organizationDao;
	}

	public ProjectDao getProjectDao() {
		return this.projectDao;
	}

	public void setProjectDao(ProjectDao projectDao) {
		this.projectDao = projectDao;
	}

	private AccountDao accountDao;

	public AccountDao getAccountDao() {
		return accountDao;
	}

	public void setAccountDao(AccountDao accountDao) {
		this.accountDao = accountDao;
	}

	@Override
	public List<Project> getProjectList(User user, int curPageNum, int pageSize) {
		List<Project> projectList = projectDao.getProjectList(user, curPageNum,
				pageSize);
		for (Project p : projectList) {
			if (user.isUserInRole("admin")
					|| p.getUser().getId() == user.getId())
				p.setIsManagable(true);
		}
		return projectList;
	}

	@Override
	public int addProject(Project project) {
		project.setUpdateTime(new Date());
		project.setCreateDate(new Date());
		List<User> usersInformed = new ArrayList<User>();
		for (String account : project.getMemberAccountList()) {
			User user = accountDao.getUser(account);
			if (user != null) {
				boolean addSuccess = project.addMember(user);
				if (addSuccess) {
					usersInformed.add(user);
				}
			}
		}
		int result = projectDao.addProject(project);
		for (User u : usersInformed) {
			Notification o = new Notification();
			o.setTypeId((short)2);
			o.setTargetUser(project.getUser());
			o.setUser(u);
			o.setParam1(new Integer(result).toString());
			o.setParam2(project.getName());
			accountMgr.addNotification(o);
		}

		Group g = organizationDao.getGroup(project.getGroupId());
		if (g.getProductionLineId() > 0) {
			organizationDao.updateCountersInProductionLine(g
					.getProductionLineId());
		}

		return result;
	}

	@Override
	public int removeProject(int id) {
		Project p = getProject(id);
		Group g = organizationDao.getGroup(p.getGroupId());
		int result = projectDao.removeProject(id);
		if (g != null) {
			int pId = g.getProductionLineId();
			if (pId > 0) {
				organizationDao.updateCountersInProductionLine(pId);
			}
		}
		return result;
	}

	@Override
	public int updateProject(Project outerProject) {
		Project project = getProject(outerProject.getId());
		project.setName(outerProject.getName());
		project.setIntroduction(outerProject.getIntroduction());
		project.setUpdateTime(new Date());

		if (outerProject.getMemberAccountList() != null) {
			// adding new ones
			for (String account : outerProject.getMemberAccountList()) {
				User user = accountDao.getUser(account);
				if (user != null) {
					boolean addSuccess = project.addMember(user);
					if (addSuccess) {
						Notification o = new Notification();
						o.setTypeId((short)2);
						o.setTargetUser(outerProject.getUser());
						o.setUser(user);
						o.setParam1(new Integer(outerProject.getId()).toString());
						o.setParam2(outerProject.getName());
						accountMgr.addNotification(o);
					}
				}
			}

			if (project.getUserList() != null) {
				// remove old ones
				List<User> userListToBeRemoved = new ArrayList<User>();
				for (User user : project.getUserList()) {
					if (!outerProject.getMemberAccountList().contains(
							user.getAccount())) {
						userListToBeRemoved.add(user);
					}
				}

				for (User user : userListToBeRemoved) {
					project.removeMember(user);
				}
			}
		}
		
		//added by liweiguang 2016-1-18		
		if (outerProject.getReadonlyMemberAccountList() != null){
			//先批量删除当前项目中access_level=2的旧记录
			projectDao.deleteProjectUser(outerProject.getId(), 2);
			//然后再逐个增加			
			for (String account : outerProject.getReadonlyMemberAccountList()) {
				User user = accountDao.getUser(account);
				if (user != null) {
					projectDao.createProjectUser(outerProject.getId(), 2, user.getId());
				}
			}
		}
		//added by liweiguang 2016-1-18 end 

		return projectDao.updateProject(project);
	}

	@Override
	public Project getProject(int id) {
		return projectDao.getProject(id);
	}

    @Override
    public Project getProject(int id, String ver) {
        CheckIn check = workspaceDao.getVersion(id, ver);
        String projectData = check.getProjectData();

        Gson gson = new Gson();
        Project p = gson.fromJson(projectData, Project.class);
        p.setVersion(check.getVersion());
        return p;
    }

    @Override
	public Module getModule(int id) {
		return projectDao.getModule(id);
	}

	@Override
	public Page getPage(int id) {
		return projectDao.getPage(id);
	}

	@Override
	public String updateProject(int id, String projectData,
			String deletedObjectListData, Map<Long, Long> actionIdMap) {
		return projectDao.updateProject(id, projectData, deletedObjectListData, actionIdMap);
	}

	@Override
	public long getProjectListNum(User user) {
		if (user != null && user.isUserInRole("admin")) {
			user = null;
		}
		return projectDao.getProjectListNum(user);
	}

	@Override
	public void loadParamIdListForAction(Action action) {
		List<String> paramIdList = new ArrayList<String>();
		recursivelyLoadParamIdList(paramIdList,
				action.getResponseParameterList());
		action.setRemarks(ArrayUtils.join(paramIdList, ","));
	}

	@Override
	public void loadParamIdListForPage(Page page) {
		for (Action action : page.getActionList()) {
			loadParamIdListForAction(action);
		}
	}

	/**
	 * sub method of loadParamIdListForAction for recursively load paramIdList
	 * for complex parameters
	 * 
	 * @param paramIdList
	 * @param paramList
	 */
	private void recursivelyLoadParamIdList(List<String> paramIdList,
			Set<Parameter> paramList) {
		for (Parameter p : paramList) {
			if (p.getIdentifier() != null || !p.getIdentifier().isEmpty()) {
				paramIdList.add(p.getIdentifier());
			}
			if (p.getParameterList() != null && p.getParameterList().size() > 0) {
				recursivelyLoadParamIdList(paramIdList, p.getParameterList());
			}
		}
	}

	@Override
	public List<Action> getMatchedActionList(int projectId, String pattern) {
		List<Action> actionList = projectDao.getMatchedActionList(projectId,
				pattern);
		if (actionList == null || actionList.size() == 0) {
			Project project = projectDao.getProject(projectId);
			if (project != null) {
				String ids = project.getRelatedIds();
				if (ids != null && !ids.isEmpty()) {
					String[] arr = ids.split(",");
					for (String id : arr) {
						actionList = projectDao.getMatchedActionList(
								Integer.parseInt(id), pattern);
						if (actionList != null && actionList.size() != 0) {
							return actionList;
						}
					}
				}
			}
		}
		return actionList;
	}

	@Override
	public List<Project> getProjectListByGroup(int id) {
		return projectDao.getProjectListByGroup(id);
	}
	
	@Override
	public List<Project> search(String key) {
		return projectDao.search(key);
	}

	@Override
	public Action getAction(long id) {
		return projectDao.getAction(id);
	}

    @Override
    public Action getAction(long id, String ver, int projectId) {
        CheckIn check = workspaceDao.getVersion(projectId, ver);
        Gson gson = new Gson();
        Project p = gson.fromJson(check.getProjectData(), Project.class);
        return p.findAction(id);
    }

    @Override
    public void updateDoc(int projectId) {
        try {
            HTTPUtils.sendGet("http://" + SystemConstant.NODE_SERVER + "/api/generateDoc?projectId=" + projectId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Project> getProjectList() {
        return projectDao.getProjectList();
    }

    @Override
    public long getProjectNum() {
        return projectDao.getProjectListNum();
    }

    @Override
    public long getModuleNum() {
        return projectDao.getModuleNum();
    }

    @Override
    public long getPageNum() {
        return projectDao.getPageNum();
    }

    @Override
    public long getActionNum() {
        return projectDao.getActionNum();
    }

    @Override
    public long getParametertNum() {
        return projectDao.getParametertNum();
    }

    @Override
    public long getCheckInNum() {
        return projectDao.getCheckInNum();
    }

    @Override
    public long getMockNumInTotal() {return projectDao.getMockNumInTotal();}

    @Override
    public List<Project> selectMockNumTopNProjectList(int limit) {
        return projectDao.selectMockNumTopNProjectList(limit);
    }

    @Override
    public void updateCache(int projectId) {
        Project project = getProject(projectId);
        for (Module module : project.getModuleList()) {
            for (Page page : module.getPageList()) {
                for (Action action : page.getActionList()) {
                    updateActionCache(action);
                }
            }
        }
    }

    private void updateActionCache(Action action) {
        action.setDisableCache(0);
		for (Parameter param : action.getResponseParameterList()) {
			clearParameterCache(param, action);
		}
    }

	private void clearParameterCache(Parameter param, Action action) {
		String rules = param.getMockJsRules();
		if (rules != null && rules.contains("${") && rules.contains("}")) {
			action.setDisableCache(1);
			return; // over
		}
		Set<Parameter> children = param.getParameterList();
		if (children != null && children.size() != 0) {
			for (Parameter child : children) {
				clearParameterCache(child, action);
			}
		}
	}

	@Override
	public List<ProjectUser> getReadOnlyProjectUser(int projectId) {
		return projectDao.getReadOnlyProjectUser(projectId);
	}

	@Override
	public List<ProjectUser> getRWProjectUser(int projectId) {
		return projectDao.getRWProjectUser(projectId);
	}

	@Override
	public List<ProjectUser> getAllProjectUser(int projectId) {
		return projectDao.getAllProjectUser(projectId);
	}

	@Override
	public int updateCommonDesc(int projectId, String commonDesc) {
		return projectDao.updateCommonDesc(projectId, commonDesc);
	}

	@Override
	public String getCommonDesc(int projectId) {
		return projectDao.getCommonDesc(projectId);
	}

	@Override
	public void updateCommonModel(int projectId, List<CommonModel> commonModels) {
		//先清空旧数据
		projectDao.deleteCommonModels(projectId);
		//再逐个增加新的commonModel的数据
		for (CommonModel commonModel : commonModels){
			projectDao.addCommonModel(projectId, commonModel);
		}
	}

	@Override
	public List<CommonModel> getCommonModelList(int projectId) {
		List<CommonModel> list = projectDao.getCommonModels(projectId);
		for (CommonModel c : list){
			List<CommonModelField> fields = c.getCommonModelFieldListOrdered();
			//for(CommonModelField f : fields){
				/*System.out.println(f.getIdentifier());
				System.out.println(f.getSort());
				System.out.println("######################");*/
			//}
		}
		return list;
	}

	@Override
	public List<CommonModel> readCommonModelListFromExcel(InputStream is) throws IOException {
		XSSFWorkbook hssfWorkbook = new XSSFWorkbook(is);
		//HSSFWorkbook hssfWorkbook = new HSSFWorkbook(is);
		List<CommonModel> commonModellist = new ArrayList<CommonModel>();
		CommonModel commonModel;
		CommonModelField commonModelField;
		// 循环工作表Sheet
	    for (int numSheet = 0; numSheet < hssfWorkbook.getNumberOfSheets(); numSheet++) {
	      //HSSFSheet hssfSheet = hssfWorkbook.getSheetAt(numSheet);
	      XSSFSheet hssfSheet = hssfWorkbook.getSheetAt(numSheet);
	      if (hssfSheet == null) {
	        continue;
	      }
	      commonModel = new CommonModel();
	      String sheetName = hssfSheet.getSheetName();	  
	      //封装commonModel
	      commonModel.setCode(sheetName.substring(sheetName.indexOf("<")+1, sheetName.indexOf(">")));
	      commonModel.setName(sheetName.substring(0, sheetName.indexOf("<")));
	      
	      // 循环行Row
	      for (int rowNum = 1; rowNum <= hssfSheet.getLastRowNum(); rowNum++) {
	    	  commonModelField = new CommonModelField();
	    	  //HSSFRow hssfRow = hssfSheet.getRow(rowNum);
	    	  XSSFRow hssfRow = hssfSheet.getRow(rowNum);
	    	  if (hssfRow.getCell(1) ==null){ //如果某行的字段名为空，则结束该模型的构建
	    		  continue;
	    	  }
	    	  for (int i = 0; i < hssfRow.getLastCellNum(); i++) {
		          //HSSFCell hssfCell = hssfRow.getCell(i);
	    		  XSSFCell hssfCell = hssfRow.getCell(i);		          
		          if (i == 0) {//sort		        	  
		        	  commonModelField.setSort((int)(hssfCell.getNumericCellValue()));
		          } else if (i == 1) {//字段名
		        	  commonModelField.setIdentifier(hssfCell.getStringCellValue());
		          } else if (i == 2) {//类型,强制转成小写
		        	  commonModelField.setDatatype(hssfCell.getStringCellValue().toLowerCase());
		          } else if (i == 3) {//必填
		        	  String n = hssfCell.getStringCellValue();
		        	  if ("y".equals(n.toLowerCase())){
		        		  commonModelField.setNeeded(1);
		        	  } else if ("n".equals(n.toLowerCase())){
		        		  commonModelField.setNeeded(0);
		        	  }		        	  
		          } else if (i == 4) {//备注
		        	  commonModelField.setDescription(hssfCell.getStringCellValue());
		          }
	    	  }
	    	  commonModel.addCommonModelField(commonModelField);	        
	      }
	      commonModellist.add(commonModel);
	    }
		return commonModellist;
	}

	@Override
	public void updateModelFileName(int projectId, String modelFileName) {
		Project project = getProject(projectId);
		project.setModelfilename(modelFileName);
		projectDao.updateProject(project);
	}

	@Override
	public String getModelFileName(int projectId) {
		Project project = getProject(projectId);
		return project.getModelfilename();
	}

	@Override
	public String getModelJsonStr(int projectId, String modelCode) {
		String json = null;
		CommonModel model = getCommonModelByCode(projectId, modelCode);
		json = generateModelJson(model);
		return json;
	}
	//生成model的json内容
	/*
	 * 	id: 2210,
		identifier: "status",
		name: "",
		remark: "",
		parameterList: [ ],
		validator: "",
		dataType: "number"
	 */
	private String generateModelJson(CommonModel model){
		StringBuilder json = new StringBuilder("[");
		List<CommonModelField> fields = model.getCommonModelFieldListOrdered();
		for (CommonModelField field : fields){
			json.append("{");
			json.append("\"id\":");
			json.append(field.getId() * 1000000);//将id与RAP的字段的id区分
			json.append(",");
			
			json.append("\"identifier\":\"");
			json.append(field.getIdentifier());
			json.append("\",");
			
			json.append("\"name\":\"");
			json.append(field.getDescription().replaceAll("\n", ""));
			json.append("\",");
			
			json.append("\"remark\":\"\",");			
			json.append("\"parameterList\":[],");						
			json.append("\"validator\":\"\",");
			
			json.append("\"dataType\":\"");
			if (field.getDatatype().equals("int")||field.getDatatype().equals("long")||
					field.getDatatype().equals("double")||field.getDatatype().equals("float")){
				json.append("number\"");
			} else if (field.getDatatype().equals("string")){
				json.append("string\"");
			} else if (field.getDatatype().equals("boolean")){
				json.append("boolean\"");
			} else {
				json.append("string\"");
			}			
			json.append("},");
		}		
		//去掉最后一个逗号
		String result = json.toString();
		return result.substring(0, result.length() - 1)+"]";
	}
	
	public CommonModel getCommonModelByCode(int projectId, String modelCode){
		CommonModel result = null;
		Project project = getProject(projectId);
		Set<CommonModel> commonModelList = project.getCommonModelList();
		Iterator<CommonModel> iter = commonModelList.iterator();
		while (iter.hasNext()){
			CommonModel model = iter.next();
			if (model.getCode().equals(modelCode)){//找到对应的模型了
				result = model;
				break;
			}
		}
		return result;
	}
}
