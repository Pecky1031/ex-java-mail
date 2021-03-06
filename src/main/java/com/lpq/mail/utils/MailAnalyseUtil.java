package com.lpq.mail.utils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Wei yuyaung
 * @date 2020.05.19 17:54
 */
public class MailAnalyseUtil {

    public String from(String context) {
        String pattern = "(From: ).*(<)(.*)(?=>)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(context);
        if (m.find( )) {
            return m.group(3);
        } else {
            return null ;
        }
    }

    public String to(String context) {
        String pattern = "(To: ).*(<)(.*)(?=>)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(context);
        if (m.find( )) {
            return m.group(3);
        } else {
            return null ;
        }
    }

    public Timestamp date(String context) throws ParseException {
        String pattern = "(Date: )(.*?)\\n";
        Pattern r = Pattern.compile(pattern) ;
        Matcher m = r.matcher(context) ;
        if (m.find()) {
            String time = m.group(2);
            time = time.replaceAll("," , "") ;
            time = time.substring(0, 24) ;
            SimpleDateFormat sdf1 = new SimpleDateFormat ("EEE dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf1.parse(time) ;
            time = sdf2.format(date) ;
            Timestamp ts ;
            try {
                ts = Timestamp.valueOf(time);
                return ts ;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            return null;
        }
        return null ;
    }

    public String subjectCharset(String context) {
        String pattern = "(Subject: )(.*?)\\n" ;
        Pattern r = Pattern.compile(pattern) ;
        Matcher m = r.matcher(context) ;
        String[] subject = null ;
        if (m.find( )) {
            String x = m.group(2);
            String str = null ;
            if(x.contains("?")){
                subject = x.split("\\?");
                str =  subject[1] ;
                if(str.contains("\"")){
                    str=str.replace("\"","");
                }
            }else{
                return  null ;
            }
            return str ;
        } else {
            return null;
        }
    }

    public String subjectText(String context) {
        String pattern = "(Subject: )(.*?)\\n" ;
        Pattern r = Pattern.compile(pattern) ;
        Matcher m = r.matcher(context) ;
        String[] subject = null ;
        if (m.find( )) {
            String x = m.group(2);
            String str = null ;
            if(x.contains("?")){
                subject = x.split("\\?");
                str =  subject[3] ;
            }else{
                str = m.group(2);
            }
            return str ;
        } else {
            return null;
        }
    }

    public String bodyCharset(String context) {
        String text = init(context) ;
        String pattern = "(?=Content-Type: text/plain).*(charset=)(.*)(Content-Transfer-Encoding: base64)" ;
        Pattern r = Pattern.compile(pattern) ;
        Matcher m = r.matcher(text) ;
        if (m.find( )) {
            String s = m.group(2);
            if(s.contains("\"")){
                s=s.replace("\"","");
            }
            return s;
        } else {
            return null ;
        }
    }

    public String bodyText(String context) {
        String text = init(context) ;
        String pattern = "Content-Transfer-Encoding: base64(.*?)------" ;
        Pattern r = Pattern.compile(pattern) ;
        Matcher m = r.matcher(text) ;
        if (m.find( )) {
            return m.group(1);
        } else {
            return null ;
        }
    }

    private String init(String line){
        String result =line.replaceAll("\t|\n", "");
        return result ;
    }
}
