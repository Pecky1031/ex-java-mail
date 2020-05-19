package com.lpq.mail.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * mail_info
 * @author 
 */
@Data
public class UserInfo implements Serializable {
    /**
     * 邮件id
     */
    private Integer id;

    /**
     * 邮件所属的用户
     */
    private Integer userId;

    /**
     * 主题
     */
    private String subject;

    /**
     * 发送方
     */
    private String from;

    /**
     * 接收方
     */
    private String to;

    /**
     * 邮件内容
     */
    private String content;

    private static final long serialVersionUID = 1L;
}