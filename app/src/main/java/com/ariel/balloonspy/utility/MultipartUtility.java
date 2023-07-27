package com.ariel.balloonspy.utility;


import android.os.Environment;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * This utility class provides an abstraction layer for sending multipart HTTP
 * POST requests to a web server.
 * @author www.codejava.net
 *
 */
public class MultipartUtility {
    private static final String LINE_FEED = "\r\n";
    ////--------------------------
    public static String album="";
    public static String urlpic2="https://imgur.com/upload";
    public static String urlpic1="https://imgur.com/upload/checkcaptcha";
    public static String Cookie="";
    private final String boundary;
    public  CookieManager ckman = new CookieManager();
    public String charset= "UTF-8";
    private HttpURLConnection httpConn;
    private OutputStream outputStream;
    private PrintWriter writer;
    public MultipartUtility()
    {
        ckman  = new CookieManager();
        CookieHandler.setDefault(ckman);
        boundary = "----WebKitFormBoundarySXVRBXPvYANzdt9a";
		  /*
		  System.setProperty("http.proxyHost", "127.0.0.1");
	      System.setProperty("https.proxyHost", "127.0.0.1");
	      System.setProperty("http.proxyPort", "8888");
	      System.setProperty("https.proxyPort", "8888");
	      System.setProperty("javax.net.ssl.trustStore", "C:\\Users\\Roey\\Desktop\\FiddlerKeystore");
	      System.setProperty("javax.net.ssl.trustStorePassword", "12345678");
	      */
    }
    public MultipartUtility(String theurl)
            throws IOException
    {


        // creates a unique boundary based on time stamp
        boundary = "----WebKitFormBoundarySXVRBXPvYANzdt9a";

        URL url = new URL(theurl);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); // indicates POST method
        httpConn.setDoInput(true);
        if(!theurl.contains("check"))
        {
            httpConn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary);
        }

        httpConn.setRequestProperty("User-Agent", "CodeJava Agent");
        //  httpConn.setRequestProperty("Cookie",Cookie);
        httpConn.setRequestProperty("Referer","https://imgur.com/");
        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                true);
    }

    /**

     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data

     * @throws IOException
     */
    public static void main(String []args) throws IOException
    {

	      System.out.println(uplouadPic(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator+"roey"));

    }

    public static String uplouadPic(String pictureName) throws IOException
    {
        MultipartUtility utility = new MultipartUtility();
        utility.getcookie();
        String url="https://imgur.com/upload";

        //https://www.textnow.com/api/users/roeytest/messages
        MultipartUtility m=new MultipartUtility(urlpic2);
       // final String pathname = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator;
        File uploadFile1 = new File(pictureName+".jpg");

        m.addFormField("Content-Disposition: form-data; name=\"json\"",
                "{\"contact_value\":\"roey.sdomi\",\"contact_type\":3,\"read\":1,\"message_direction\":2,\"message_type\":2,\"from_name\":\"computer com\",\"has_video\":false,\"new\":true,\"date\":\"2018-11-12T16:02:20.765Z\"}");
        m.addFilePart("snapshot", uploadFile1);
        String v = m.finish().toString();
        int i = v.indexOf("\"hash\":\"");
        int z = v.indexOf("\"",i+12);

        String s = v.substring(i+8, z);
        s = "i.imgur.com/"+s+".jpg";

        return s;
    }

    public String getcookie() throws IOException
    {
        String url=urlpic1;
        //https://www.textnow.com/api/users/roeytest/messages

        MultipartUtility m=new MultipartUtility(urlpic1);
        //	m.addFormField("Content-Disposition: form-data; name=\"json\"", "{\"contact_value\":\"roey.sdomi\",\"contact_type\":3,\"read\":1,\"message_direction\":2,\"message_type\":2,\"from_name\":\"computer com\",\"has_video\":false,\"new\":true,\"date\":\"2018-11-12T16:02:20.765Z\"}");
        m.addFormField2("total_uploads=1&create_album=true", "1");
        String v=m.finish().toString();
        int i=v.indexOf("new_album_id\":\"");
        int z=v.indexOf("\"",i+17);

        String s=v.substring(i+15, z);
        album=s;

        return "";


    }

    /**
     * Adds a form field to the request
     * @param name field name
     * @param value field value
     */
    public void addFormField(String name, String value) {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"new_album_id\"")
                .append(LINE_FEED);
        // writer.append("Content-Type: text/plain; charset=" + charset).append(
        //       LINE_FEED);
        writer.append(LINE_FEED);

        writer.append(album).append(LINE_FEED);

        writer.flush();
    }

    public void addFormField2(String name, String value) {
        // writer.append("--" + boundary).append(LINE_FEED);
        writer.append(name)
                .append(LINE_FEED);
        // writer.append("Content-Type: text/plain; charset=" + charset).append(
        //       LINE_FEED);
        writer.append(LINE_FEED);
        // writer.append(value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a upload file section to the request
     * @param fieldName name attribute in <input type="file" name="..." />
     * @param uploadFile a File to be uploaded
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile)
            throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--").append(boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"Filedata\";").append(" filename=\"").append(fileName).append("\"")
                .append(LINE_FEED);
        writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(fileName))
                .append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a header field to the request.
     * @param name - name of the header field
     * @param value - value of the header field
     */
    public void addHeaderField(String name, String value) {
        writer.append(name + ": " + value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     * @return a list of Strings as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    public List<String> finish() throws IOException {
        List<String> response = new ArrayList<String>();

        writer.append(LINE_FEED).flush();
        writer.append("--" + boundary + "--").append(LINE_FEED);

        writer.close();

        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        } else {
            throw new IOException("Server returned non-OK status: " + status);
        }

        return response;
    }
}
