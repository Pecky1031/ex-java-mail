package com.lpq.mail.utils;

import com.lpq.mail.dao.MailInfoDao;
import com.lpq.mail.entity.MailAccountInfo;
import com.lpq.mail.entity.MailInfo;
import com.lpq.mail.exception.GlobalException;
import com.lpq.mail.result.CodeMessage;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.swing.text.AbstractDocument;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 创建人：肖易安
 * 创建时间：  2020/5/19
 * 注释：null
 **/
public class POPUtil {
    public List<MailInfo> MyPopServer(MailAccountInfo mailAccountInfo) throws GlobalException, IOException, ParseException {
        List<MailInfo> mails = new ArrayList<MailInfo>() ;
        int port = Integer.valueOf(mailAccountInfo.getMailPopPort());
        String server = mailAccountInfo.getMailPopAddress();
        Socket socket = null ;
        try{
            socket = new Socket(server,port);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            sendServer("pop " + mailAccountInfo.getMailAccount()+" "+mailAccountInfo.getMailPassword(), out);
            String line = null ;
            line = getReturn(in);
            if(!line.substring(0,3).equals("250")){
                throw new GlobalException(CodeMessage.GET_MAIL_ERROR);
            }
            do{
                MailInfo mailInfo = new MailInfo();
                mailInfo.setUserId(mailAccountInfo.getUserId());
                line = getReturn(in);
                if(line.equals("null")){
                    break;
                }
                mailInfo.setFrom(line);
                line = getReturn(in);
                mailInfo.setTo(line);
                line = getReturn(in);
                mailInfo.setSubject(line);
                line = getReturn(in);
                mailInfo.setContent(line);
                line = getReturn(in);
                SimpleDateFormat sdf1 = new SimpleDateFormat ("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                Date date = sdf1.parse(line) ;
                mailInfo.setDate(date);
                if(!(mailInfo.getFrom() == null || mailInfo.getFrom().length()<1)){
                    mails.add(mailInfo);
                }
                line = getReturn(in);
            }
            while(line.equals("."));
            socket.close();
            return mails;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw e;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } catch (ParseException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public List<MailInfo> POPServer(MailAccountInfo mailAccountInfo) throws MessagingException {
        List<MailInfo> mails = new ArrayList<>();
        try{
            String address = mailAccountInfo.getMailAccount();
            String[] split = mailAccountInfo.getMailAccount().split("@");
            String user = split[0] ;
            String password = mailAccountInfo.getMailPassword();
            Properties props = new Properties();
            props.setProperty("mail.store.protocol" , "pop3");
            props.setProperty("mail.pop3.host" , mailAccountInfo.getMailPopAddress());
            props.setProperty("mail.pop3.port" , mailAccountInfo.getMailPopPort());
            Session session = Session.getInstance(props);
            Store store = session.getStore("pop3");
            store.connect(address , password);
            Folder folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            Message[] message = folder.getMessages();
            if(message==null||message.length<1){
                return null ;
            }
            for (int i=0 ; i<message.length ; i++) {
                MailInfo mailInfo = new MailInfo();
                mailInfo.setUserId(mailAccountInfo.getUserId());
                mailInfo.setTo(mailAccountInfo.getMailAccount());
                MimeMessage msg = (MimeMessage)message[i] ;
                mailInfo.setSubject(getSubject(msg));
                mailInfo.setFrom(getFrom(msg));
                StringBuffer content = new StringBuffer(30);
                getMailTextContent(msg, content);
                String contents = (content.length() > 100 ? content.substring(0,100) + "..." : String.valueOf(content));
                mailInfo.setContent(contents);
                mailInfo.setDate(msg.getSentDate());
                mails.add(mailInfo);
            }
            folder.close();
            store.close();
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
        return mails ;
    }
    public static void getMailTextContent(Part part, StringBuffer content) throws MessagingException, IOException {
        boolean isContainTextAttach = part.getContentType().indexOf("name") > 0;
        if (part.isMimeType("text/*") && !isContainTextAttach) {
            content.append(part.getContent().toString());
        } else if (part.isMimeType("message/rfc822")) {
            getMailTextContent((Part)part.getContent(),content);
        } else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            int partCount = multipart.getCount();
            for (int i = 0; i < partCount; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                getMailTextContent(bodyPart,content);
            }
        }
    }
    public static String getSubject(MimeMessage msg) throws UnsupportedEncodingException, MessagingException {
        return MimeUtility.decodeText(msg.getSubject());
    }
    public static String getFrom(MimeMessage msg) throws MessagingException, UnsupportedEncodingException {
        String from = "";
        Address[] froms = msg.getFrom();
        if (froms.length < 1) {
            throw new MessagingException("没有发件人!");
        }
        InternetAddress address = (InternetAddress) froms[0];
        String person = address.getPersonal();
        if (person != null) {
            person = MimeUtility.decodeText(person) + " ";
        } else {
            person = "";
        }
        from = person + "<" + address.getAddress() + ">";

        return from;
    }

    public String getReturn(BufferedReader in) {
        String line = null;
        try {
            line = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return line;
        }
    }

    public String getResult(String line) {
        StringTokenizer stringTokenizer = new StringTokenizer(line, " ");
        return stringTokenizer.nextToken();
    }

    public String sendServer(String str,BufferedReader in,BufferedWriter out) throws IOException {
        out.write(str);
        out.newLine();
        out.flush();
        return getReturn(in);
    }
    public void sendServer(String str , BufferedWriter out) throws IOException {
        out.write(str);
        out.newLine();
        out.flush();
    }

    public void user(String user,BufferedReader in,BufferedWriter out) throws IOException {
        try {
            String result = getResult(getReturn(in));
            if (!"+OK".equals(result)) {
                throw new IOException("连接服务器失败!");
            }
            result=getResult(sendServer("user "+user,in,out));
            if(!"+OK".equals(result)){
                throw new IOException("用户名错误!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pass(String pwd,BufferedReader in,BufferedWriter out) {
        try {
            String result = null ;
            result = getResult(sendServer("pass " + pwd,in,out));
            if (!"+OK".equals(result)) {
                throw new IOException("密码错误!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int stat(BufferedReader in,BufferedWriter out) throws IOException {
        String result = null;
        String line = null;
        int mailNum = 0;
        line=sendServer("stat",in,out);
        StringTokenizer st=new StringTokenizer(line," ");
        result=st.nextToken();
        if(st.hasMoreTokens()) {
            mailNum=Integer.parseInt(st.nextToken());
        } else{
            mailNum=0;
        }
        if(!"+OK".equals(result)){
            throw new IOException("查看邮箱状态出错!");
        }
        return mailNum;
    }
    public void list(BufferedReader in,BufferedWriter out) throws IOException{
        String message = "";
        String line = null;
        line=sendServer("list",in,out);
        while(!".".equalsIgnoreCase(line)){
            message=message+line+"\n";
            line=in.readLine().toString();
        }
    }
    public void list_one(int mailNumber ,BufferedReader in,BufferedWriter out) throws IOException{
        String result = null;
        result = getResult(sendServer("list "+mailNumber,in,out));
        if(!"+OK".equals(result)){
            throw new IOException("list错误!");
        }
    }
    public String retr(int mailNum,BufferedReader in,BufferedWriter out) throws IOException, InterruptedException{
        String result = null;
        result=getResult(sendServer("retr "+mailNum,in,out));
        if(!"+OK".equals(result)){
            throw new IOException("接收邮件出错!");
        }
        String message = "";
        String line = null;
        try{
            line= in.readLine();
            while(!".".equalsIgnoreCase(line)){
                message=message+line+"\n";
                line= in.readLine();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return message;
    }
    public void quit(BufferedReader in,BufferedWriter out) throws IOException{
        String result;
        result=getResult(sendServer("QUIT",in,out));
        if(!"+OK".equals(result)){
            throw new IOException("未能正确退出");
        }
    }
}
