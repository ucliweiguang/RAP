package com.taobao.rigel.rap.project.dao;

import java.util.List;
import java.util.Map;

import com.taobao.rigel.rap.account.bo.User;
import com.taobao.rigel.rap.project.bo.Action;
import com.taobao.rigel.rap.project.bo.Module;
import com.taobao.rigel.rap.project.bo.Page;
import com.taobao.rigel.rap.project.bo.Project;
import com.taobao.rigel.rap.project.bo.ProjectUser;

public interface ProjectDao {

	/**
	 * get project list
	 * @param user
	 * @param curPageNum
	 * @param pageSize
	 * @return
	 */
	List<Project> getProjectList(User user, int curPageNum, int pageSize);

	/**
	 * get new project
	 * @param project
	 * @return always 0
	 */
	int addProject(Project project);

	/**
	 * update project
	 * @param id
	 * @param projectData
	 * @param deletedObjectListData
	 * @return
	 */
	String updateProject(int id, String projectData,
			String deletedObjectListData, Map<Long, Long> actionIdMap);

	/**
	 * update project
	 * @param project
	 * @return
	 */
	int updateProject(Project project);

	/**
	 * remove project
	 * @param id
	 * @return
	 */
	int removeProject(int id);

	/**
	 * get project
	 * @param id
	 * @return
	 */
	Project getProject(int id);

	/**
	 * get module
	 * @param id
	 * @return
	 */
	Module getModule(int id);

	/**
	 * get page
	 * @param id
	 * @return
	 */
	Page getPage(int id);
	
	
	/**
	 * get action
	 * @param id
	 * @return
	 */
	Action getAction(long id);

	/**
	 * save project
	 * @param project
	 * @return
	 */
	int saveProject(Project project);

	/**
	 * get project list number
	 * @param user
	 * @return
	 */
	long getProjectListNum(User user);

	/**
	 * get matched action list based on URL pattern
	 * 
	 * @param projectId
	 * @param pattern
	 * @return
	 */
	List<Action> getMatchedActionList(int projectId, String pattern);

		/**
	 * clear all mock data of objects in specified project
	 * @param projectId project id
	 * @return affected rows num
	 */
	public int resetMockData(int projectId);

	/**
	 * get project list by group
	 * @param id
	 * @return
	 */
	List<Project> getProjectListByGroup(int id);

	/**
	 * project search
	 * 
	 * @param key
	 * @return
	 */
	List<Project> search(String key);

    List<Project> getProjectList();

    long getProjectListNum();

    long getModuleNum();

    long getPageNum();

    long getActionNum();

    long getParametertNum();

    long getCheckInNum();

    long getMockNumInTotal();

    List<Project> selectMockNumTopNProjectList(int limit);
    
    public Action getActionByUrlAndProjectid(int projectId,String requestUrl);
    /**
     * 
     * 功能描述：获取项目中所有action的id列表
     * @param projectId
     * @return 
     * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
     * created on: 2016-1-15
     */    
	public List<Integer> getActionIdsByProjectId(int projectId);
	/**
	 * 获取该项目的所有用户，包括读写、只读用户
	 * 功能描述：
	 * @param projectId
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2016-1-15
	 */
	public List<ProjectUser> getAllProjectUser(int projectId);
	/**
	 * 
	 * 功能描述：获取该项目的只读用户
	 * @param projectId
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2016-1-15
	 */
	public List<ProjectUser> getReadOnlyProjectUser(int projectId);
	/**
	 * 
	 * 功能描述：获取该项目读写用户
	 * @param projectId
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2016-1-15
	 */
	public List<ProjectUser> getRWProjectUser(int projectId);
	/**
	 * 
	 * 功能描述：删除指定项目中指定accesslevel的记录
	 * @param projecctId
	 * @param accesslevel 1-读写，2-只读
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2016-1-18
	 */
	public void deleteProjectUser(int projectId,int accesslevel);
	/**
	 * 
	 * 功能描述：创建指定项目中指定accesslevel的记录
	 * @param projecctId
	 * @param accesslevel   1-读写，2-只读
	 * @param userId 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2016-1-18
	 */
	public void createProjectUser(int projectId,int accesslevel,long userId);
	/**
	 * 
	 * 功能描述：更新项目API的通用信息
	 * @param projectId
	 * @param commonDesc 项目API的通用信息
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2016-3-23
	 */
	public int updateCommonDesc(int projectId,String commonDesc);
	/**
	 * 
	 * 功能描述：获取项目API的通用信息
	 * @param projectId
	 * @return 
	 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
	 * created on: 2016-3-23
	 */
	public String getCommonDesc(int projectId);
}
