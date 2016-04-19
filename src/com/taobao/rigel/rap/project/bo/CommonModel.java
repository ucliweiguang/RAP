package com.taobao.rigel.rap.project.bo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * 
 * 功能描述：接口的公用模型
 * <p> 版权所有：阿里UC移动事业群--浏览器应用业务部
 * <p> 未经本公司许可，不得以任何方式复制或使用本程序任何部分 <p>
 * 
 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
 * created on: 2016-4-6
 */
public class CommonModel implements Serializable {
	private static final long serialVersionUID = 2887584424136088928L;

	private int id;
	private String code;
	private String name;
	private int projectId;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getProjectId() {
		return projectId;
	}
	public void setProjectId(int projectId) {
		this.projectId = projectId;
	}
	private Project project;

	public Project getProject() {
		return this.project;
	}

	public void setProject(Project project) {
		this.project = project;
	}
		
	public void addCommonModelField(CommonModelField commonModelField) {
		getCommonModelFieldList().add(commonModelField);
		commonModelField.setCommonModel(this);
	}

	private Set<CommonModelField> commonModelFieldList = new HashSet<CommonModelField>();
	public Set<CommonModelField> getCommonModelFieldList() {
		return commonModelFieldList;
	}
	public void setCommonModelFieldList(Set<CommonModelField> commonModelFieldList) {
		this.commonModelFieldList = commonModelFieldList;
	}
	
	//更新CommonModel基本属性
	public void update(CommonModel commonModel) {
		setCode(commonModel.getCode());
		setName(commonModel.getName());
	}
	
	//回显公共模型字段时，按照sort字段进行排序
	public List<CommonModelField> getCommonModelFieldListOrdered() {
		Set<CommonModelField> commonFieldList = this.getCommonModelFieldList();
		List<CommonModelField> commonModelFieldListOrdered = new ArrayList<CommonModelField>();
		commonModelFieldListOrdered.addAll(commonFieldList);
		Collections.sort(commonModelFieldListOrdered, new CommonModelFieldComparator());
		return commonModelFieldListOrdered;
	}

}
