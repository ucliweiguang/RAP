package com.taobao.rigel.rap.common;

import com.alibaba.buc.sso.client.vo.BucSSOUser;

public class SystemConstant {
	public static final int FIRST_PAGE_NUM = 1;
	public static final int DEFAULT_PAGE_NUM = 10;
	public static String README_PATH = "";
	public static String ROOT = "";	
	public static String DOMAIN_URL = "";
	
	//日常：login-test.alibaba-inc.com，线上：login.alibaba-inc.com
	//public static final String ALI_LOGIN_URL = "login-test.alibaba-inc.com";
	public static final String ALI_LOGIN_URL = "login.alibaba-inc.com";
    
	public static final String NODE_SERVER = "localhost:7429";
    public static boolean serviceInitialized = false;
	private static String domainURL = "";

	public static String getAliLoginUrl() {
		return ALI_LOGIN_URL;
	}

	public static String getDOMAIN_URL() {
		return domainURL;
	}
	public static void setDOMAIN_URL(String domainURL) {
		 SystemConstant.domainURL = domainURL;
	}
	
	public static BucSSOUser user = null;

}