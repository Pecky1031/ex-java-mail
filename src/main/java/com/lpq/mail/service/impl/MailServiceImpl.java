package com.lpq.mail.service.impl;

import com.lpq.mail.dao.LocalMailInfoDao;
import com.lpq.mail.dao.MailAccountInfoDao;
import com.lpq.mail.dao.MailInfoDao;
import com.lpq.mail.dao.MailSendInfoDao;
import com.lpq.mail.entity.*;
import com.lpq.mail.exception.GlobalException;
import com.lpq.mail.result.BaseResult;
import com.lpq.mail.result.CodeMessage;
import com.lpq.mail.service.MailService;
import com.lpq.mail.utils.MailAnalyseUtil;
import com.lpq.mail.utils.MailDecodeUtil;
import com.lpq.mail.utils.POPUtil;
import com.lpq.mail.utils.SMTPUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.beans.Transient;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Wei yuyaung
 * @date 2020.05.19 17:17
 */
@Service
public class MailServiceImpl implements MailService {

    private MailInfoDao mailInfoDao;
    private MailAccountInfoDao mailAccountInfoDao;
    private MailSendInfoDao mailSendInfoDao;
    private LocalMailInfoDao localMailInfoDao;

    @Autowired
    public MailServiceImpl(MailInfoDao mailInfoDao, MailAccountInfoDao mailAccountInfoDao, MailSendInfoDao mailSendInfoDao, LocalMailInfoDao localMailInfoDao) {
        this.mailInfoDao = mailInfoDao;
        this.mailAccountInfoDao = mailAccountInfoDao;
        this.mailSendInfoDao = mailSendInfoDao;
        this.localMailInfoDao = localMailInfoDao;
    }

    @Override
    public String send(MailSendInfo mailInfo) throws GlobalException {
        SMTPUtil smtp = new SMTPUtil();
        MailAccountInfoExample example = new MailAccountInfoExample();
        example.createCriteria().andMailAccountEqualTo(mailInfo.getFrom()).andUserIdEqualTo(mailInfo.getUserId());
        List<MailAccountInfo> mailAccountInfo = mailAccountInfoDao.selectByExample(example);
        mailInfo.setDate(new Date());
        example.clear();
        if (mailAccountInfo.size() != 1) {
            throw new GlobalException(CodeMessage.NO_MAILBOX);
        }
        String to = mailInfo.getTo();
        String success = null;
        if (to.contains(";")) {
            String[] tos = to.split(";");
            for (String t : tos) {
                mailInfo.setTo(t);
                if(mailInfo.getFrom().contains("@sbss")){
                    success = smtp.MySmtpServer(mailInfo,mailAccountInfo.get(0));
                }else{
                    success = smtp.SMTPserver(mailInfo, mailAccountInfo.get(0));
                }
            }
        } else {
            if(mailInfo.getFrom().contains("@sbss")){
                success = smtp.MySmtpServer(mailInfo,mailAccountInfo.get(0));
            }else{
                success = smtp.SMTPserver(mailInfo, mailAccountInfo.get(0));
            }
        }
        success = success.substring(0,3);
        if(success.equals("250")){
            mailSendInfoDao.insert(mailInfo);
        }
        return success;
    }

    @Override
    public boolean receiveMail(Integer userId) throws GlobalException, InterruptedException, ParseException, IOException, MessagingException {
        //先删后存，防止数据库里面邮件重复
        MailInfoExample example = new MailInfoExample();
        example.createCriteria().andUserIdEqualTo(userId);
        mailInfoDao.deleteByExample(example);
        example.clear();
        //获取用户所有邮箱账号
        MailAccountInfoExample accountInfoExample = new MailAccountInfoExample();
        accountInfoExample.createCriteria().andUserIdEqualTo(userId);
        List<MailAccountInfo> accountInfoList = null;
        accountInfoList = mailAccountInfoDao.selectByExample(accountInfoExample);
        accountInfoExample.clear();
        //没有邮箱账号直接返回
        if (accountInfoList.isEmpty()) {
            throw new GlobalException(CodeMessage.NO_MAILBOX);
        }
        //遍历邮箱账号
        for (MailAccountInfo mailaccount : accountInfoList) {
            POPUtil popUtil = new POPUtil();
            if(mailaccount.getMailAccount().contains("@sbss")){
                List<MailInfo> mails = popUtil.MyPopServer(mailaccount);
                if(mails.size()>0){
                    mailInfoDao.insertByList(mails);
                }
            }else{
                try {
                    List<MailInfo> mails = popUtil.POPServer(mailaccount);
                    if(mails.size()>0){
                        mailInfoDao.insertByList(mails);
                    }
                } catch (MessagingException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }
        return true;
    }

    @Override
    public List<MailInfo> takeMail(Integer userId) throws GlobalException {
        try {
            MailInfoExample example = new MailInfoExample();
            example.createCriteria().andUserIdEqualTo(userId);
            List<MailInfo> mails = mailInfoDao.selectByExample(example);
            return mails;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public List<MailSendInfo> takeSendMail(Integer userId) throws GlobalException {
        try {
            MailSendInfoExample example = new MailSendInfoExample();
            example.createCriteria().andUserIdEqualTo(userId);
            List<MailSendInfo> mails = mailSendInfoDao.selectByExample(example);
            return mails;
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public boolean deleteMail(Integer userId) {
        MailInfoExample example = new MailInfoExample();
        example.createCriteria().andUserIdEqualTo(userId);
        int i = mailInfoDao.deleteByExample(example);
        MailSendInfoExample mailSendInfoExample = new MailSendInfoExample();
        mailSendInfoExample.createCriteria().andUserIdEqualTo(userId);
        int i1 = mailSendInfoDao.deleteByExample(mailSendInfoExample);
        LocalMailInfoExample localMailInfoExample = new LocalMailInfoExample();
        localMailInfoExample.createCriteria().andUserIdEqualTo(userId);
        int i2 = localMailInfoDao.deleteByExample(localMailInfoExample);
        return i > 0 && i1 > 0 && i2 > 0;
    }

    @Override
    public MailInfo getMailDetails(Integer id) {
        return mailInfoDao.selectByPrimaryKey(id);
    }
}
