package com.taobao.rigel.rap.account.web.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;

import com.alibaba.buc.sso.client.util.SimpleUserUtil;
import com.alibaba.buc.sso.client.vo.BucSSOUser;
import com.google.gson.Gson;
import com.taobao.rigel.rap.account.bo.Notification;
import com.taobao.rigel.rap.account.bo.User;
import com.taobao.rigel.rap.common.ActionBase;
import com.taobao.rigel.rap.common.ContextManager;
import com.taobao.rigel.rap.common.Pinyin4jUtil;
import com.taobao.rigel.rap.common.SystemVisitorLog;

/**
 * account action
 * 
 * @author Bosn
 * 
 */
public class AccountAction extends ActionBase {

	private static final long serialVersionUID = 1L;
	private long userId;
	private int roleId;
	private String account;
	private String password;
	private String newPassword;
	private String name;
	private String email;
	private String SSO_TOKEN;
	private String BACK_URL;

	public String test() throws AddressException, InterruptedException {


		return SUCCESS;
	}
	
	public String getNotificationList() {
		if (!isUserLogined()) {
			plsLogin();
			return LOGIN;
		}
		List<Notification> list = getAccountMgr().getNotificationList(getCurUserId());
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (Notification o : list) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("id", o.getId());
			m.put("param1", o.getParam1());
			m.put("param2", o.getParam1());
			m.put("param3", o.getParam1());
			Map<String, Object> user = new HashMap<String, Object>();
			user.put("name", o.getUser().getName());
			user.put("id", o.getUser().getId());
			m.put("user", user);
			m.put("createTime", o.getCreateTime().getTime());
			m.put("typeId", o.getTypeId());
			result.add(m);
		}
		Gson gson = new Gson();
		String json = gson.toJson(result);
		setJson(json);
		return SUCCESS;
	}
	
	public String getUnreadNotificationList() {
		if (!isUserLogined()) {
			plsLogin();
			return LOGIN;
		}
		List<Notification> list = getAccountMgr().getUnreadNotificationList(getCurUserId());
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (Notification o : list) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("id", o.getId());
			m.put("param1", o.getParam1());
			m.put("param2", o.getParam2());
			m.put("param3", o.getParam3());
			
			Map<String, Object> user = new HashMap<String, Object>();
			user.put("name", o.getUser().getName());
			user.put("id", o.getUser().getId());
			m.put("user", user);
			
			Map<String, Object> targetUser = new HashMap<String, Object>();
			targetUser.put("name", o.getTargetUser().getName());
			targetUser.put("id", o.getTargetUser().getId());
			
			m.put("targetUser", targetUser);
			
			m.put("createTime", o.getCreateTime().getTime());
			m.put("createTimeStr", o.getCreateTimeStr());
			m.put("typeId", o.getTypeId());
			result.add(m);
		}
		Gson gson = new Gson();
		String json = gson.toJson(result);
		setJson(json);
		return SUCCESS;
	}
	
	public String readAllNotification() {
		if (!isUserLogined()) {
			plsLogin();
			return LOGIN;
		}
		getAccountMgr().readNotificationList(getCurUserId());
		return SUCCESS;
	}
	
	public String getSSO_TOKEN() {
		return SSO_TOKEN;
	}

	public void setSSO_TOKEN(String sSO_TOKEN) {
		SSO_TOKEN = sSO_TOKEN;
	}

	public String getBACK_URL() {
		return BACK_URL;
	}

	public void setBACK_URL(String bACK_URL) {
		BACK_URL = bACK_URL;
	}

	public String all() {
		Gson gson = new Gson();
		List<User> users = super.getAccountMgr().getUserList();
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (User user : users) {
			Map<String, Object> o = new HashMap<String, Object>();
			o.put("id", user.getId());
			o.put("name", user.getName());
			o.put("role", user.getRoleListStr());
			o.put("account", user.getAccount());
			o.put("realName", user.getRealname());
            o.put("empId", user.getEmpId());
            o.put("namePinyin", Pinyin4jUtil.calculatePinyinArrStr(user.getName()));
            o.put("realNamePinyin", Pinyin4jUtil.calculatePinyinArrStr(user.getRealname()));
			result.add(o);
		}
		setJson("{\"users\":" + gson.toJson(result) + "}");
		return SUCCESS;
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public int getRoleId() {
		return roleId;
	}

	public void setRoleId(int roleId) {
		this.roleId = roleId;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account.trim();
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password.trim();
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword.trim();
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name.trim();
	}

	public String getEmail() {
		return this.email.trim();
	}

	public void setEmail(String email) {
		this.email = email.trim().toLowerCase();
	}

	private String profileProperty;

	public String getProfileProperty() {
		return profileProperty;
	}

	public void setProfileProperty(String profileProperty) {
		this.profileProperty = profileProperty;
	}

	private String profileValue;

	public String getProfileValue() {
		return profileValue;
	}

	public void setProfileValue(String profileValue) {
		this.profileValue = profileValue;
	}

	private boolean isEditMode = false;

	public boolean getIsEditMode() {
		return isEditMode;
	}

	public void setIsEditMode(boolean isEditMode) {
		this.isEditMode = isEditMode;
	}

	public String login() {
		// if logged in, log out automatically
		doLogout();
		return SUCCESS;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public String doLogin() {
		if (super.getAccountMgr().validate(getAccount(), getPassword())) {
			Map session = ContextManager.getSession();
			User user = getAccountMgr().getUser(getAccount());
            if (user != null && user.getId() > 0) {
                session.put(ContextManager.KEY_ACCOUNT, user.getAccount());
                session.put(ContextManager.KEY_USER_ID, user.getId());
                session.put(ContextManager.KEY_NAME, user.getName());
            } else {
                setErrMsg("用户不存在或密码错误");
                return ERROR;
            }
			if (getReturnUrl() != null && !getReturnUrl().trim().equals("")) {
				return "redirect";
			}
			return SUCCESS;
		} else {
			setErrMsg("用户不存在或密码错误");
			return ERROR;
		}
	}

	public String doLogout() {
		String key = ContextManager.KEY_ACCOUNT;
		if (ContextManager.getSession().get(key) != null) {
			ContextManager.getSession().remove(key);
		}
		return SUCCESS;
	}

	// public String register() {
	// doLogout();
	// return SUCCESS;
	// }

	public String doRegister() {
		User user = new User();
		user.setAccount(getAccount());
		user.setPassword(getPassword());
		user.setName(getName());
		user.setEmail(getEmail());
		if (super.getAccountMgr().addUser(user)) {
			return doLogin();
		} else {
			return ERROR;
		}
	}

	public String myAccount() {
		if (!isUserLogined()) {
			plsLogin();
			setRelativeReturnUrl("/account/myAccount.action");
			return LOGIN;
		}
		return SUCCESS;
	}
	
	public String mySetting() {
		if (!isUserLogined()) {
			plsLogin();
			setRelativeReturnUrl("/account/mySetting.action");
			return LOGIN;
		}
		return SUCCESS;
	}

	public String doChangeProfile() {
		super.getAccountMgr().changeProfile(
				super.getAccountMgr().getUser(super.getCurAccount()).getId(),
				getProfileProperty(), getProfileValue());
		return SUCCESS;
	}

	/**
	 * public String updateProfile() { setIsEditMode(true); return SUCCESS; }
	 * 
	 * public String doUpdateProfile() { if
	 * (!super.getAccountMgr().updateProfile(getCurUserId(), getName(),
	 * getEmail(), getPassword(), getNewPassword())) { setIsEditMode(true);
	 * setErrMsg("旧密码输入错误"); } return SUCCESS; }
	 */
	
	//private HttpServletRequest request;
	public String sendBucSSOToken() {
		/*this.request = ServletActionContext.getRequest();
		try {
			BucSSOUser user = SimpleUserUtil.getBucSSOUser(this.request);
			System.out.println("sendBucSSOToken user:" + user);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServletException e) {
			e.printStackTrace();
		}*/
		return SUCCESS;
		//return "redirect";
	}

	public String logData() {
		Map<String, Object> obj = new HashMap<String, Object>();
		obj.put("online", this.getCountOfOnlineUserList());
		obj.put("mockNumToday", SystemVisitorLog.getMockNumToday());
		Gson gson = new Gson();
		setJson(gson.toJson(obj));
		return SUCCESS;
	}
}
