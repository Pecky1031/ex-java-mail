package com.lpq.mail.service.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.lpq.mail.dao.MailAccountInfoDao;
import com.lpq.mail.dao.MailInfoDao;
import com.lpq.mail.dao.UserInfoDao;
import com.lpq.mail.dao.UserRoleInfoDao;
import com.lpq.mail.entity.*;
import com.lpq.mail.exception.GlobalException;
import com.lpq.mail.result.CodeMessage;
import com.lpq.mail.service.MailService;
import com.lpq.mail.service.UserService;
import com.lpq.mail.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Wei yuyaung
 * @date 2020.05.20 0:03
 */
@Service
public class UserServiceImpl implements UserService {

    //普通用户
    public static final int ROLE_USER = 0;
    //管理员
    public static final int ROLE_ADMIN = 1;

    private UserInfoDao userInfoDao;
    private MailAccountInfoDao mailAccountInfoDao;
    private UserRoleInfoDao userRoleInfoDao;

    private MailService mailService;

    //用户锁定状态，state = 1
    public static final int USER_LOCKED = 1;

    @Autowired
    public UserServiceImpl(UserInfoDao userInfoDao, MailAccountInfoDao mailAccountInfoDao, UserRoleInfoDao userRoleInfoDao, MailService mailService) {
        this.userInfoDao = userInfoDao;
        this.mailAccountInfoDao = mailAccountInfoDao;
        this.userRoleInfoDao = userRoleInfoDao;
        this.mailService = mailService;
    }

    @Override
    public String login(LoginVO loginVO) throws GlobalException {
        UserInfoExample example = new UserInfoExample();
        example.createCriteria().andUsernameEqualTo(loginVO.getUsername());
        List<UserInfo> userInfos = userInfoDao.selectByExample(example);
        if (userInfos.size() > 1) {
            throw new GlobalException(CodeMessage.DATABSE_ERROR);
        } else if (userInfos.size() == 0) {
            throw new GlobalException(CodeMessage.ERROR_USERNAME);
        }

        if (userInfos.get(0).getPassword().equals(loginVO.getPassword())) {
            //jwt生成token
            return getToken(userInfos.get(0));
        } else {
            throw new GlobalException(CodeMessage.ERROR_PASSWORD);
        }
    }


    @Override
    public boolean changePassword(Integer userId, ChangePasswordVO changePasswordVO) throws GlobalException {
        UserInfo userInfo = userInfoDao.selectByPrimaryKey(userId);
        if (userInfo == null) {
            //用户不存在，并发操作可能删除了用户
            throw new GlobalException(CodeMessage.USER_NOT_EXIST);
        }

        if (userInfo.getState() == USER_LOCKED) {
            //账户被锁定
            throw new GlobalException(CodeMessage.USER_LOCKED);
        }
        if (userInfo.getPassword().equals(changePasswordVO.getOldPassword())) {
            userInfo.setPassword(changePasswordVO.getNewPassword());
            int i = userInfoDao.updateByPrimaryKeySelective(userInfo);
            return i > 0;
        } else {
            throw new GlobalException(CodeMessage.ERROR_PASSWORD);
        }
    }

    @Override
    public boolean changeNickName(Integer userId, ModifyUserInfoVO modifyUserInfoVO) throws GlobalException {
        UserInfo userInfo = userInfoDao.selectByPrimaryKey(userId);
        if (userInfo == null) {
            throw new GlobalException(CodeMessage.USER_NOT_EXIST);
        }
        userInfo.setNickName(modifyUserInfoVO.getNick_name());
        int i = userInfoDao.updateByPrimaryKeySelective(userInfo);
        return i > 0;
    }

    @Override
    public UserInfo info(Integer userId) {
        return userInfoDao.selectByPrimaryKey(userId);
    }

    @Override
    public UserInfo info(String username) throws GlobalException {
        UserInfoExample example = new UserInfoExample();
        example.createCriteria().andUsernameEqualTo(username);
        List<UserInfo> userInfos = userInfoDao.selectByExample(example);
        if(userInfos.size() != 1){
            throw new GlobalException(new CodeMessage(500,"查询用户错误"));
        }

        return userInfos.get(0);
    }

    public String getToken(UserInfo user) {
        String token = "";
        token = JWT.create().withAudience(String.valueOf(user.getId()))
                .sign(Algorithm.HMAC256(user.getPassword()));
        return token;
    }

    @Override
    public List<MailAccountInfo> mailInfo(Integer userId) throws GlobalException {
        MailAccountInfoExample example = new MailAccountInfoExample();
        example.createCriteria().andUserIdEqualTo(userId);
        List<MailAccountInfo> mailInfos = mailAccountInfoDao.selectByExample(example);
        if (mailInfos.size() < 1) {
            throw new GlobalException(CodeMessage.EMPTY_USER_MAIL_LIST);
        }
        return mailInfos;
    }

    @Override
    public boolean addMailAccount(Integer userId, AddMailAccountVO addMailAccountVO) throws GlobalException {
        MailAccountInfo mailAccountInfo = new MailAccountInfo();
        mailAccountInfo.setUserId(userId);
        mailAccountInfo.setMailAccount(addMailAccountVO.getMail_account());
        mailAccountInfo.setMailPassword(addMailAccountVO.getMail_password());
        mailAccountInfo.setMailPopAddress(addMailAccountVO.getMail_pop_address());
        mailAccountInfo.setMailPopPort(addMailAccountVO.getMail_pop_port());
        mailAccountInfo.setMailSmtpAddress(addMailAccountVO.getMail_smtp_address());
        mailAccountInfo.setMailSmtpPort(addMailAccountVO.getMail_smtp_port());
        int success = mailAccountInfoDao.insert(mailAccountInfo);
        if (success != 0) {
            return true;
        } else {
            throw new GlobalException(CodeMessage.INSERT_MAIL_ACCOUNT_ERROR);
        }
    }


    @Override
    @Transactional(readOnly = false)
    public boolean addUser(RegisterVO registerVO) throws GlobalException {
        UserInfoExample example = new UserInfoExample();
        example.createCriteria().andUsernameEqualTo(registerVO.getUsername());
        List<UserInfo> userInfos = userInfoDao.selectByExample(example);
        if (userInfos.size() > 0) {
            throw new GlobalException(CodeMessage.User_DOES_EXIST);
        }
        UserInfo userInfo = initUserInfo(registerVO.getUsername(), registerVO.getPassword());
        example.clear();
        int insertSelective = userInfoDao.insertSelective(userInfo);
        if (!(insertSelective > 0)) {
            throw new GlobalException(CodeMessage.ADD_NEW_USER_FAIL);
        }
        example.createCriteria().andUsernameEqualTo(registerVO.getUsername());
        List<UserInfo> infos = userInfoDao.selectByExample(example);
        return insertUserInfo(infos.get(0));
    }

    private UserInfo initUserInfo(String username, String password) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(username);
        userInfo.setPassword(password);
        userInfo.setState(0);
        userInfo.setNickName("请输入您的昵称");
        return userInfo;
    }

    private boolean insertUserInfo(UserInfo userInfo) {
        MailAccountInfoExample mailAccountInfoExample = new MailAccountInfoExample();
        UserRoleInfoExample roleInfoExample = new UserRoleInfoExample();

        //插入本地邮箱
        MailAccountInfo mailAccountInfo = new MailAccountInfo();
        mailAccountInfo.setUserId(userInfo.getId());
        mailAccountInfo.setMailAccount(userInfo.getUsername() + "@sbss");
        mailAccountInfo.setMailPassword(userInfo.getPassword());
        mailAccountInfo.setMailPopAddress("127.0.0.1");
        mailAccountInfo.setMailPopPort("1524");
        mailAccountInfo.setMailSmtpAddress("127.0.0.1");
        mailAccountInfo.setMailSmtpPort("4251");
        UserRoleInfo userRoleInfo = new UserRoleInfo();
        userRoleInfo.setUserId(userInfo.getId());
        //默认为普通用户
        userRoleInfo.setUserType(1);
        mailAccountInfoDao.insertSelective(mailAccountInfo);
        userRoleInfoDao.insertSelective(userRoleInfo);
        return true;
    }


    @Override
    @Transactional(readOnly = false)
    public boolean deleteUser(Integer userId) {
        boolean b = mailService.deleteMail(userId);
        MailAccountInfoExample mailAccountInfoExample = new MailAccountInfoExample();
        mailAccountInfoExample.createCriteria().andUserIdEqualTo(userId);
        int i = mailAccountInfoDao.deleteByExample(mailAccountInfoExample);
        int i1 = userInfoDao.deleteByPrimaryKey(userId);
        return true;
    }

    @Override
    public boolean changeRole(Integer userId) throws GlobalException {
        UserRoleInfoExample userRoleInfoExample = new UserRoleInfoExample();
        userRoleInfoExample.createCriteria().andUserIdEqualTo(userId);
        List<UserRoleInfo> userRoleInfos = userRoleInfoDao.selectByExample(userRoleInfoExample);
        if(userRoleInfos.size() != 1){
            throw new GlobalException(new CodeMessage(500,"用户不存在"));
        }
        userRoleInfoExample.clear();
        if(userRoleInfos.get(0).getUserType() == ROLE_USER){
            userRoleInfos.get(0).setUserType(ROLE_ADMIN);
        }else if(userRoleInfos.get(0).getUserType() == ROLE_ADMIN) {
            userRoleInfos.get(0).setUserType(ROLE_USER);
        }

        int i = userRoleInfoDao.updateByPrimaryKey(userRoleInfos.get(0));
        return i > 0;
    }

    @Override
    public boolean modifyUserMessage(AdminModifyInfo adminModifyInfo) {
        UserInfo userInfo = userInfoDao.selectByPrimaryKey(adminModifyInfo.getUserId());
        userInfo.setNickName(adminModifyInfo.getNickName());
        int i = userInfoDao.updateByPrimaryKey(userInfo);

        UserRoleInfoExample example = new UserRoleInfoExample();
        example.createCriteria().andUserIdEqualTo(adminModifyInfo.getUserId());
        List<UserRoleInfo> userRoleInfos = userRoleInfoDao.selectByExample(example);
        UserRoleInfo userRoleInfo = userRoleInfos.get(0);
        userRoleInfo.setUserType(adminModifyInfo.getRoleType());
        int i1 = userRoleInfoDao.updateByPrimaryKey(userRoleInfo);
        return i > 0 && i1 > 0;
    }

    @Override
    public Integer getUserRoleInfo(Integer userId) throws GlobalException {
        UserRoleInfoExample example = new UserRoleInfoExample();
        example.createCriteria().andUserIdEqualTo(userId);
        List<UserRoleInfo> userRoleInfos = userRoleInfoDao.selectByExample(example);
        if(userRoleInfos.size() != 1){
            throw new GlobalException(new CodeMessage(500,"数据库错误"));
        }
        return userRoleInfos.get(0).getUserType();
    }
}
