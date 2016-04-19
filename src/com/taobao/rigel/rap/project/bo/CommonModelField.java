package com.taobao.rigel.rap.project.bo;

import java.io.Serializable;
/**
 * 
 * 功能描述：接口的公用模型下的字段
 * <p> 版权所有：阿里UC移动事业群--浏览器应用业务部
 * <p> 未经本公司许可，不得以任何方式复制或使用本程序任何部分 <p>
 * 
 * @author <a href="mailto:weiguang.lwg@alibaba-inc.com">李伟光 </a>
 * created on: 2016-4-6
 */
public class CommonModelField implements Serializable {
	private static final long serialVersionUID = -8354082114000600760L;

	private int id;
	private int modelId;
	private String identifier;
	private String datatype;
	private int needed;
	private String description;
	
	private int sort;//字段排序顺序	
	
	public int getSort() {
		return sort;
	}
	public void setSort(int sort) {
		this.sort = sort;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getModelId() {
		return modelId;
	}
	public void setModelId(int modelId) {
		this.modelId = modelId;
	}
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public String getDatatype() {
		return datatype;
	}
	public void setDatatype(String datatype) {
		this.datatype = datatype;
	}
	public int getNeeded() {
		return needed;
	}
	public void setNeeded(int needed) {
		this.needed = needed;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	private CommonModel commonModel;
	public CommonModel getCommonModel() {
		return commonModel;
	}
	public void setCommonModel(CommonModel commonModel) {
		this.commonModel = commonModel;
	}
	
}
