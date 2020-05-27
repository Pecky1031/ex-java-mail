package com.lpq.mail.server;

import com.lpq.mail.dao.LocalMailInfoDao;
import com.lpq.mail.dao.MailAccountInfoDao;
import com.lpq.mail.dao.MailInfoDao;
import com.lpq.mail.dao.UserInfoDao;
import com.lpq.mail.entity.*;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;


import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * 创建人：肖易安
 * 创建时间：  2020/5/23
 * 注释：null
 **/
public class POPServerHandler implements Runnable {
    private Socket socket ;
    private BufferedWriter out ;
    private BufferedReader in  ;
    private SqlSessionFactory factory;
    private SqlSession sqlSession;
    private LocalMailInfoDao mailInfoDao ;
    private MailAccountInfoDao mailAccountInfoDao;
    private int userId ;

    public POPServerHandler(Socket socket) throws IOException {
        this.socket = socket ;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        factory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsStream("mybatis-config.xml"));
        sqlSession = factory.openSession();
        this.mailInfoDao = sqlSession.getMapper(LocalMailInfoDao.class);
        this.mailAccountInfoDao = sqlSession.getMapper(MailAccountInfoDao.class);
    }

    @Override
    public void run() {
        try{
            String message = getRequest(in);
            if(message.contains("pop")) {
                System.out.println(message);
                String username = message.split(" ")[1];
                String password = message.split(" ")[2];
                MailAccountInfoExample example = new MailAccountInfoExample();
                example.createCriteria().andMailAccountEqualTo(username);
                List<MailAccountInfo> accounts = mailAccountInfoDao.selectByExample(example);
                example.clear();
                if (accounts.get(0).getMailPassword().equals(password)) {
                    sendMessage("250 登陆成功", out);
                } else {
                    sendMessage("290 登陆失败", out);
                    return;
                }
                int userId = accounts.get(0).getUserId();
                LocalMailInfoExample mailExample = new LocalMailInfoExample();
                mailExample.createCriteria().andUserIdEqualTo(userId);
                List<LocalMailInfo> mails = mailInfoDao.selectByExample(mailExample);
                if(mails.size()<1){
                    sendMessage("null",out);
                    this.socket = null ;
                    this.in = null ;
                    this.out = null ;
                    return;
                }
                mailExample.clear();
                for(int i = 0 ; i < mails.size() ; i++ ){
                    sendMessage(mails.get(i).getFrom() , out);
                    sendMessage(mails.get(i).getTo() , out);
                    sendMessage(mails.get(i).getSubject() , out);
                    sendMessage(mails.get(i).getContent() , out);
                    sendMessage(mails.get(i).getDate().toString() , out);
                    if(i==mails.size()-1){
                        sendMessage("..." , out);
                    }else{
                        sendMessage(".",out);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            this.socket = null ;
            this.in = null ;
            this.out = null ;
        }
    }

    //接收请求
    public String getRequest(BufferedReader in) {
        String line = null;
        try {
            line = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return line;
        }
    }
    //发送信息
    public void sendMessage(String str,BufferedWriter out) throws IOException {
        out.write(str);
        out.newLine();
        out.flush();
    }
}
