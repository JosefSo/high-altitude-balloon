package com.ariel.balloonspy.utility;


import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

public class TextNow {

    /**
     * an Api class i created for app called "TEXTNOW"
     */

    public static final String textNowUserName = "roey.sdomi";
    private static final String textNowTestUserName = "roeytest";
    private static final String textNowURL = "https://www.textnow.com/api/users/" + textNowTestUserName + "/messages";

    private static final String Cookie ="unsupported_browsers_notif=true; _ga=GA1.2.301685588.1553533088; _gid=GA1.2.1326335517.1553533088; _fbp=fb.1.1553533090112.1366222464; language=he; V3_enable_authentication=true; connect.sid=s%3AGypbxDDPnDjxOOcfXtX0ZUx_FGxfC4VE.82wpW5Rvt7iCnlE2r9CB%2BfU8y3dEW54LTciagjG3MFs; G_ENABLED_IDPS=google; stc117823=tsa:1553533092589.31932248.18515587.8952130298295797.5:20190325172813|env:1%7C20190425165812%7C20190325172813%7C2%7C1073241:20200324165813|uid:1553533092588.980690211.0253282.117823.1600572832.:20200324165813|srchist:1073241%3A1%3A20190425165812:20200324165813; UserDidVisitApp=true; FirehoseSession-messaging=true; sm_dapi_session=1; puntCookie=true; __rtgt_sid=jtolhw0y15pbzk; d7s_uid=jtolhw0y15pbzk; d7s_spc=1; __gads=ID=0c4c98495b61892f:T=1553533102:S=ALNI_MaDezTL_SQ2b1wZIBvk-c8xHY849Q; PermissionPriming=1; XSRF-TOKEN=6UXIXUmu-r0ch8ZhVqzUP5fPQHDqD0iVdxNM; _gat=1" ;
    public static String res = null;


   public static void main(String []args)
   {
       TextNow t = new TextNow();
       try {
           t.sendMessage("rony.ronen","This is a Test");
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

    public String [] readLastMessage() throws IOException
    {
        /**
         * read last messege and user name
         */

        String url="https://www.textnow.com/api/users/" + textNowTestUserName + "/messages?start_message_id=0&direction=future&page_size=0";
        getRequest(url);
        int s=res.lastIndexOf("message\":\"");
        int t2=res.indexOf("\"",s+11);
        String f=res.substring(s+10,t2);
        int s2=res.lastIndexOf("contact_value\":\"");
        int t3=res.indexOf("@",s2+11);
        String f2=res.substring(s2+16,t3);
        String []fin= {f2,f};
        return fin;
    }

    /**
     * send messges to user by name and content
     */

    public void sendMessage(String to, String content) throws IOException
    {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {

                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.ms'Z'").format(new Date());
                    String body;
                    JSONObject json = new JSONObject();
                    json.put("contact_value", to);
                    json.put("contact_type", "2");
                    json.put("message", "[" + timeStamp + "]" + content);
                    json.put("read", "1");
                    json.put("message_direction", "2");
                    json.put("from_name", "");
                    json.put("has_video", "false");
                    json.put("new", "true");
                    json.put("date", timeStamp);

                    body = "json=" + json.toString();

                    /*String url="https://www.textnow.com/api/users/roeytest/messages";
                    String body ="json={\"contact_value\":\"roey.sdomi\",\"contact_type\":2,\"message\":\"efggflon\",\"read\":1,\"message_direction\":2,\"message_type\":1,\"from_name\":\"\",\"has_video\":false,\"new\":true,\"date\":\"2018-11-09T02:36:32.533Z\"}";
                    body=body.replace("roey.sdomi", to).replaceAll("efggflon", content);
                    postRequest(url, body);*/

                    postRequest(textNowURL, body);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        t.start();
    }

    ///-----------http headers commands--------------
    private void getRequest(String aURL) throws IOException
    {

        HttpsURLConnection connection = null;
        URL url = new URL(aURL);
        connection = (HttpsURLConnection)url.openConnection();
        headers(connection, aURL,"GET");
        connection.getContent();
        //-----cookies

        //-----getresponse------
        res = respondMaker(connection);
        //------------------
    }


    private void headers(HttpsURLConnection connection,String https_url,String type)
    {
        try {
            connection.setRequestMethod(type);
        } catch (ProtocolException e) {
            // TODO Auto-generated catch block
            System.out.println("problem in the headers");
        }
        connection.addRequestProperty(" origin", https_url);
        System.setProperty("http.KeepAlive", "true");
        //  connection.addRequestProperty("Upgrade-Insecure-Requests", "1");
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 11_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.0 Mobile/15E148 Safari/604.1");
        connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        connection.addRequestProperty("Referer", https_url);
        connection.addRequestProperty("Accept-Encoding", "");
        connection.addRequestProperty("Accept-Language", "q=0.9,en-US;q=0.8,en;q=0.7");

        connection.addRequestProperty("Cookie",Cookie);


        // connection.addRequestProperty("Connection","keep-alive");
        connection.setConnectTimeout(1000);

    }

    private String respondMaker(HttpsURLConnection connection) throws IOException
    {
        InputStream output = connection.getInputStream();
        Scanner s = new Scanner(output).useDelimiter("\\A");
        String result = ".";
        while (s.hasNext())
        {
            result = s.next();
        }
        return result;
    }

    private void postRequest(String aURL, String body) throws IOException
    {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(aURL);
            connection = (HttpsURLConnection)url.openConnection();
            //-----headers------
            headers(connection, aURL,"POST");
            //-----headers------
            //-----body------
            body(connection, body);
            //-----body--------
            connection.getContent();
            //-----cookies
            res = respondMaker(connection);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void body(HttpsURLConnection connection,String body) throws IOException
    {
        connection.setDoOutput(true);
        String str = body;
        byte[] outputInBytes = str.getBytes("UTF-8");
        OutputStream os = connection.getOutputStream();
        os.write(outputInBytes);
        os.close();
    }

}
