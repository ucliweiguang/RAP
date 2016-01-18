package com.taobao.rigel.rap.project.bo;

public class ProjectUser implements java.io.Serializable {
	
	private static final long serialVersionUID = 3496892737935805325L;
	
	private long userId;
	private int projectId;
	private int accesslevel;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + accesslevel;
		result = prime * result + projectId;
		result = prime * result + (int) (userId ^ (userId >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProjectUser other = (ProjectUser) obj;
		if (accesslevel != other.accesslevel)
			return false;
		if (projectId != other.projectId)
			return false;
		if (userId != other.userId)
			return false;
		return true;
	}
	public long getUserId() {
		return userId;
	}
	public void setUserId(long userId) {
		this.userId = userId;
	}
	public int getProjectId() {
		return projectId;
	}
	public void setProjectId(int projectId) {
		this.projectId = projectId;
	}
	public int getAccesslevel() {
		return accesslevel;
	}
	public void setAccesslevel(int accesslevel) {
		this.accesslevel = accesslevel;
	}   
    
}
