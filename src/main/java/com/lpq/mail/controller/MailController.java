package com.lpq.mail.controller;

import com.auth0.jwt.JWT;
import com.lpq.mail.annotations.UserLoginToken;
import com.lpq.mail.entity.MailInfo;
import com.lpq.mail.entity.MailSendInfo;
import com.lpq.mail.exception.GlobalException;
import com.lpq.mail.result.BaseResult;
import com.lpq.mail.result.CodeMessage;
import com.lpq.mail.service.MailService;
import com.lpq.mail.vo.MailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Wei yuyaung
 * @date 2020.05.19 12:17
 */
@RestController
@RequestMapping("mail")
public class MailController {
    private MailService mailService;

    @Autowired
    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @UserLoginToken
    @PostMapping("/send")
    public BaseResult<Void> sendMail(MailVO mailVO, HttpServletRequest httpServletRequest) {
        MailSendInfo mailInfo = new MailSendInfo();
        int userId = Integer.parseInt(JWT.decode(httpServletRequest.getHeader("token")).getAudience().get(0));
        mailInfo.setUserId(userId);
        mailInfo.setFrom(mailVO.getFrom());
        mailInfo.setContent(mailVO.getContent());
        mailInfo.setTo(mailVO.getTo());
        mailInfo.setSubject(mailVO.getSubject());

        try {
            String success = mailService.send(mailInfo);
            if (success.equals("250")) {
                return BaseResult.success(null);
            } else if (success.equals("554")) {
                return BaseResult.fail(CodeMessage.JUNK_MAIL);
            } else {
                return BaseResult.fail(CodeMessage.SEND_MAIL_ERROR);
            }
        } catch (GlobalException e) {
            return BaseResult.fail(e.getCodeMessage());
        }
    }

    @UserLoginToken
    @GetMapping("/list")
    public BaseResult<List<MailInfo>> receiveMail(HttpServletRequest httpServletRequest) {
        try {
            int userId = Integer.parseInt(JWT.decode(httpServletRequest.getHeader("token")).getAudience().get(0));
            boolean success = mailService.receiveMail(userId);
            if (success) {
                List<MailInfo> mails = mailService.takeMail(userId);
                if (mails.size() < 1) {
                    mails = new ArrayList<>();
                    return BaseResult.success(mails);
                }
                return BaseResult.success(mails);
            } else {
                return BaseResult.fail(CodeMessage.GET_MAIL_ERROR);
            }
        } catch (GlobalException e) {
            return BaseResult.fail(e.getCodeMessage());
        } catch (InterruptedException | ParseException | IOException e) {
            e.printStackTrace();
            return BaseResult.fail(CodeMessage.GET_MAIL_ERROR);
        } catch (MessagingException e) {
            e.printStackTrace();
            return BaseResult.fail(CodeMessage.GET_MAIL_ERROR);
        }
    }

    @PostMapping("/details")
    public BaseResult<MailInfo> mailDetails(Integer id){
        MailInfo mailDetails = mailService.getMailDetails(id);
        if(mailDetails == null){
            return BaseResult.fail(new CodeMessage(500,"邮件不存在"));
        }else {
            return BaseResult.success(mailDetails);
        }
    }
}
