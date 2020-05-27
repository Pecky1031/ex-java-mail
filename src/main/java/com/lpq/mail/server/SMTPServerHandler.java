package com.lpq.mail.server;

import com.lpq.mail.dao.LocalMailInfoDao;
import com.lpq.mail.dao.MailInfoDao;
import com.lpq.mail.dao.UserInfoDao;
import com.lpq.mail.entity.*;
import lombok.SneakyThrows;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.List;

/**
 * 创建人：肖易安
 * 创建时间：  2020/5/23
 * 注释：null
 **/
public class SMTPServerHandler implements Runnable {
    private Socket socket ;
    private BufferedWriter out ;
    private BufferedReader in  ;
    private SqlSessionFactory factory;
    private SqlSession sqlSession;
    private LocalMailInfoDao mailInfoDao ;
    private UserInfoDao userInfoDao;

    public SMTPServerHandler(Socket socket) throws IOException {
        this.socket = socket ;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out =  new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        factory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsStream("mybatis-config.xml"));
        sqlSession = factory.openSession(true);
        this.mailInfoDao = sqlSession.getMapper(LocalMailInfoDao.class);
    }
    @SneakyThrows
    @Override
    public void run() {
        try{
            LocalMailInfo mailInfo = new LocalMailInfo();
            String userID = getRequest(in);
            mailInfo.setFrom(getRequest(in));
            String to = getRequest(in);
            mailInfo.setTo(to);
            mailInfo.setSubject(getRequest(in));
            mailInfo.setContent(getRequest(in));
            mailInfo.setDate(new Date());
            UserInfoExample example = new UserInfoExample();
            example.createCriteria().andUsernameEqualTo(to);
            List<UserInfo> users = userInfoDao.selectByExample(example);
            int userId ;
            if(users.size()<1){
                userId = -1 ;
            }else{
                userId = users.get(0).getId();
            }
            mailInfo.setUserId(userId);
            mailInfoDao.insert(mailInfo);
            sendMessage("250 发送成功" , out);
        } catch (IOException e) {
            e.printStackTrace();
            sendMessage("554 发送失败" , out);
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
