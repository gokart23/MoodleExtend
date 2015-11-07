package com.plianto.moodlews;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
*
* @author 
*/
public class MoodleTest {

	public static void main(String[] args) throws ProtocolException,
		IOException, ParserConfigurationException, SAXException {
	
		final String token = "ff5afe9ac851ef125be98d642e2d7ffe";
		final String domainName = "http://192.168.3.113/moodle";
		String functionName = "core_files_get_files";
		String urlParameters = "moodlewsrestformat=json" +
				"&contextid=62&component=assignsubmission_file&filearea=submission_files&itemid=4" +
				"&filepath=/&filename=big_21.jpg";
				
	    		/*"&users[0][firstname]=" + URLEncoder.encode("A", "UTF-8") +
	    		"&users[0][lastname]=" + URLEncoder.encode("jhansi", "UTF-8");*/
	    
	    String serverurl = domainName + "/webservice/rest/server.php"
		+ "?wstoken=" + token + "&wsfunction=" + functionName;
		
		HttpURLConnection con = (HttpURLConnection) new URL(serverurl).openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setRequestProperty("Content-Language", "en-US");
		con.setDoOutput(true);
		con.setUseCaches(false);
		con.setDoInput(true);
		
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
		
		// Get Response
		InputStream is = con.getInputStream();
	
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		String line;
		StringBuilder response = new StringBuilder();
		while ((line = rd.readLine()) != null) {
		response.append(line);
		response.append('\r');
		}
		rd.close();
		System.out.println(response.toString());
		
	}

}

