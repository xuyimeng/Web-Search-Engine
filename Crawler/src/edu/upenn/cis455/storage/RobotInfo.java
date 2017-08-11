package edu.upenn.cis455.storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

import edu.upenn.cis455.crawler.info.URLInfo;

@Entity
public class RobotInfo {
	
	@PrimaryKey
	String hostName;
	
	boolean isHTTPS;
	ArrayList<String> disallow = new ArrayList<>();
	ArrayList<String> allow = new ArrayList<>();
	
	int delay = 5;
	long lastVisitTime = 0;
	
	public RobotInfo(){
		
	}
	
	public RobotInfo(String url){
		//System.out.println("a");
		isHTTPS = isHTTPS(url.toLowerCase().trim());
		//System.out.println("b");
		hostName = setHostName(url.toLowerCase().trim());
		//System.out.println("c");
		getRobotsTxt();
		//System.out.println("d");
	}
	
	public String setHostName(String urlString){
		
		if (isHTTPS){
			URL url = null;
			try {
				url = new URL(urlString);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			
			return url.getHost();
		} else {												
			
			URLInfo urlInfo = new URLInfo(urlString);
			String hostName = urlInfo.getHostName();
			return hostName;
		}
	}
	
	public String getHostName(){
		return hostName;
	}
	
	public boolean isHTTPS(String url){
		return url.startsWith("https");
	}
	
	public void getRobotsTxt(){
		if (isHTTPS){
			URL url = null;
			URLConnection urlConnection = null;
			try {
				url = new URL("https://" + hostName + "/robots.txt");
				urlConnection = (URLConnection)url.openConnection();
				
				System.out.println("https://" + hostName + "/robots.txt: Downloading robots txt");
				
				InputStreamReader in = new InputStreamReader(urlConnection.getInputStream());
				BufferedReader br = new BufferedReader(in);
				
				parseRobotsTxt(br);
				
				
			} catch (Exception e) {
				e.printStackTrace();
			} 
			
		} else {
			String url = "http://" + hostName + "/robots.txt";
			System.out.println(url);
			URLInfo urlInfo = new URLInfo(url);
			try {
				//System.out.println(urlInfo.getHostName());
				//System.out.println(urlInfo.getPortNo());
				@SuppressWarnings("resource")
				Socket socket = new Socket(urlInfo.getHostName(), urlInfo.getPortNo());
				
				
				PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);			// get outputstream
				StringBuilder sb = new StringBuilder();	
				sb.append("GET " + url + " HTTP/1.1\r\n");
				sb.append("Host: localhost:8080\r\n");
				sb.append("User-Agent: cis455crawler\r\n");
				sb.append("Connection: close\r\n\r\n");
				printWriter.append(sb.toString());
				printWriter.flush();
				
				
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				//System.out.println("e");
				parseRobotsTxt(br);
				//System.out.println("f");
				
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	
		}
		
		lastVisitTime = System.currentTimeMillis();
	}
	
	public void parseRobotsTxt(BufferedReader br) throws IOException{
		String line = "";
		try {
			while ((line = br.readLine()) != null){
				//System.out.println("===" + line);
				if (line.toLowerCase().startsWith("user-agent: *")){
					
					while ((line = br.readLine()) != null){
						String lineLower = line.trim().toLowerCase();
						if (lineLower.startsWith("crawl-delay")){
							delay = Integer.valueOf(lineLower.substring(12).trim());
						} else if (lineLower.startsWith("allow")){
							String tmp = lineLower.substring(6).trim();
							int index = tmp.indexOf(" ");
							if (index < 0) {
								allow.add(tmp);
							} else {
								tmp = tmp.substring(0, index).trim();
								allow.add(tmp);
							}
							
							//allow.add(lineLower.substring(6).trim());
						} else if (lineLower.startsWith("disallow")){
							String tmp = lineLower.substring(9).trim();
							int index = tmp.indexOf(" ");
							if (index < 0) {
								disallow.add(tmp);
							} else {
								tmp = tmp.substring(0, index).trim();
								disallow.add(tmp);
							}
							
							
							//disallow.add(lineLower.substring(9).trim());
						} else if (lineLower.equals("") || lineLower.startsWith("user-agent:")){
							break;
						}
					}
					
				} else if (line.toLowerCase().startsWith("user-agent: cis455crawler")){
					allow = new ArrayList<>();
					disallow = new ArrayList<>();
					delay = 5;
					
					while ((line = br.readLine()) != null){
						String lineLower = line.trim().toLowerCase();
						if (lineLower.startsWith("crawl-delay")){
							delay = Integer.valueOf(lineLower.substring(12).trim());
						} else if (lineLower.startsWith("allow")){
							
							String tmp = lineLower.substring(6).trim();
							int index = tmp.indexOf(" ");
							if (index < 0) {
								allow.add(tmp);
							} else {
								tmp = tmp.substring(0, index).trim();
								allow.add(tmp);
							}
							
							//allow.add(lineLower.substring(6).trim());
						} else if (lineLower.startsWith("disallow")){
							
							String tmp = lineLower.substring(9).trim();
							int index = tmp.indexOf(" ");
							if (index < 0) {
								disallow.add(tmp);
							} else {
								tmp = tmp.substring(0, index).trim();
								disallow.add(tmp);
							}
							//disallow.add(lineLower.substring(9).trim());
						} else if (lineLower.equals("") || lineLower.startsWith("user-agent:")){
							break;
						}
					}
					break;
				}
				
				if (line == null) break;
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isAllowed(String urlString) throws MalformedURLException{
		
		URL url = new URL(urlString);
		String path = url.getFile();
		System.out.println("path is " + path);
		
		for (String tmpString : allow){
			if ("/".equals(tmpString)){
				continue;
				// return true;
			}
			
			
			String tmp1 = tmpString.endsWith("/") ? tmpString : tmpString + "/";			// address slash problem
			String tmp2 = path.endsWith("/")? path : path + "/";
			
			if (tmp2.contains(tmp1)){
				return true;
			}	
		}
		
		for (String tmpString : disallow){
			
			//if ("/".equals(tmpString)){
				//return false;
			//}
			String tmp1 = tmpString.endsWith("/") ? tmpString : tmpString + "/";			// address slash problem
			String tmp2 = path.endsWith("/") ? path : path + "/";
			
			if (tmp2.contains(tmp1)){
				return false;
			}
		}
		
		return true;
	}
	
	public long getLastVisited(){
		return lastVisitTime;
	}
	
	public int getDelay(){
		return delay;
	}
	
	public static void main(String[] args) throws MalformedURLException{
		RobotInfo robotInfo = new RobotInfo("http://hbo.com/utils");
		
		System.out.println(robotInfo.hostName);
		System.out.println("=============");
		for (String tmpString : robotInfo.allow){
			System.out.println(tmpString);
		}
		
		System.out.println("=============");
		for (String tmpString : robotInfo.disallow){
			System.out.println(tmpString);
		}
		
		System.out.println("=============");
		System.out.println(robotInfo.delay);
		
		System.out.println(robotInfo.isAllowed("http://hbo.com/utils"));
		
	}
	
}
