package com.lpq.mail.service;

import com.lpq.mail.entity.ManagerInfo;
import com.lpq.mail.entity.UserInfo;
import com.lpq.mail.exception.GlobalException;
import com.lpq.mail.vo.LoginVO;

import java.util.List;

/**
 * 创建人：肖易安
 * 创建时间：  2020/5/20
 * 注释：null
 **/
public interface ManagerService {
    /**
     * 创建人：肖易安
     * 创建时间：  2020/5/20
     * 注释：登陆
     **/
    String login (LoginVO loginVO)throws GlobalException ;
    /**
     * 创建人：肖易安
     * 创建时间：  2020/5/20
     * 注释：查询用户信息
     **/
    List<UserInfo> loadAllUser() throws GlobalException ;
    /**
     * 创建人：肖易安
     * 创建时间：  2020/5/20
     * 注释：修改用户状态
     **/
    boolean changeState(Integer userId , Integer state) throws GlobalException ;
}