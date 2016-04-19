package com.taobao.rigel.rap.project.bo;

import java.util.Comparator;

public class CommonModelFieldComparator implements Comparator<CommonModelField>{

	@Override
	public int compare(CommonModelField o1, CommonModelField o2) {
		CommonModelField l = (CommonModelField) o1;
		CommonModelField r = (CommonModelField) o2;
		//return l.getId() < r.getId() ? 0 : 1;
		return l.getSort() > r.getSort() ? 1 : -1;
	}

}
