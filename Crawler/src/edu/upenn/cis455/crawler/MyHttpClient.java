package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.upenn.cis455.crawler.info.URLInfo;



public class MyHttpClient {
	
	public URLInfo url;
	//public URL url;
	private final String userAgent = "cis455crawler";
	
	//public HashMap<String, String> info;  //keys are URLSTRING, HOST, ROOTPATH, RELATIVEPATH
	public HashMap<String, String> headers; // keys are headers' names
	
	public int port;					
	public StringBuffer robotsContent;// robots txt content
	private boolean isValid = true;
	@SuppressWarnings("unused")
	private String contentType = null;
	private long delay = 10;
	private ArrayList<String> disallowed;
	private ArrayList<String> allowed;
	private FrontierQueue urlToBeCrawled;
	
	public MyHttpClient(String urlString, FrontierQueue urlToBeCrawled){
		//info = new HashMap<>();
		headers = new HashMap<>();
		disallowed = new ArrayList<>();
		allowed = new ArrayList<>();
		
		robotsContent = new StringBuffer();
	
		url = new URLInfo(urlString);
		
		port = url.getPortNo();
		this.urlToBeCrawled = urlToBeCrawled;
		
		/*
		info.put("URLSTRING", urlString);
		info.put("HOST", url.getHostName());
		info.put("ROOTPATH", "http://" + url.getHostName());
		info.put("RELATIVEPATH", url.getFilePath());
		
		System.out.println("PORT: " + port);
		for (String key : info.keySet()){
			System.out.println(key + ": " + info.get(key));
		}
		*/
	}
	
	/**
	 * @return
	 */
	public boolean isValid(){
		return isValid;
	}
	
	/**
	 * @param isDirect
	 * @param redirect
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	public void getRobotsTxt(boolean isDirect, String redirect) throws UnknownHostException, IOException{ // get robots content and populate stringbuffer robotsContent
		String robotURL;
		if (isDirect){
			robotURL = redirect;
		} else {
			robotURL = "http://" + url.getHostName() + "/robots.txt";
		}
		
		//String robotContent = null;
		
		if (!isValid()) return;
		
		System.out.println(robotURL + ": Downloading robots txt");
		//System.out.println("http://" + url.getHostName());
		//System.out.println(port);
		@SuppressWarnings("resource")	
		Socket socket = new Socket(url.getHostName(), url.getPortNo());
		//Socket socket = new Socket("www.google.com", port);
		
		PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);			// get outputstream
		StringBuilder sb = new StringBuilder();	
		sb.append("GET " + robotURL + " HTTP/1.1\r\n");
		sb.append("Host: " + url.getHostName() + ":8080\r\n");
		sb.append("User-Agent: " + userAgent + "\r\n");
		sb.append("Connection: close\r\n\r\n");
		printWriter.append(sb.toString());
		
		printWriter.flush();						// send get request to download robots txt from host
		
		
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		String statusLine = br.readLine();
		if (statusLine.indexOf("200") != -1){			// robots txt is available now
			
			//System.out.println("200 OK");
			String line;
			
			while (true){					// first while loop reads in all the headers
				try {
					line = br.readLine(); 
					//System.out.println("THIS IS HEADER: " + line);
					
					String[] strs = line.split(":", 2);
					if ("Content-Type".equalsIgnoreCase(strs[0].trim())){
						contentType = strs[1].trim().toLowerCase();
						//System.out.println("content type is: " + contentType);
					}
					
				} catch (Exception e) {
					break;
				}
					
				if (line == null) break;
				if ("".equals(line)) break;
			}
			
			line = "a";
			
			while (true){						// second while loop reads in all the robots txt content
				try {
					if (line != null){// && line.length() != 0){
						line = br.readLine(); 
						robotsContent.append(line + "\n");
					} else {
						break;
					}
				} catch (IOException e) {
					break;
				}
			}
			
			System.out.println("the robots txt is: ");
			System.out.println(robotsContent.toString());
			
			
			
			
		} else if (statusLine.indexOf("301") != -1 || statusLine.indexOf("302") != -1){			// if the robots txt has been moved to some places else
			System.out.println("301/302 move permanently/found");
			
			String line = null;
			String location = null;
			while (true){		
				try {
					line = br.readLine(); 
					//System.out.println("THIS IS HEADER: " + line);
					
					String[] strs = line.split(":", 2);
					if ("Location".equalsIgnoreCase(strs[0].trim())){			// get the new location of robots txt
						location = strs[1].trim().toLowerCase();
						System.out.println("location is: " + location);
						break;
					}
					
				} catch (Exception e) {
					break;
				}
					
				if (line == null) break;
				if ("".equals(line)) break;
			}
			
			if (location != null){			// if we have new location for robots txt
				if (location.equalsIgnoreCase(robotURL)){
					return;
				}
				if (!location.startsWith("https")){
					if (location.startsWith("http")){
						getRobotsTxt(true,  "http://" + url.getHostName() + "/" + location);
					} else {
						getRobotsTxt(true, location);
					}
					
				} else {
					
				
					urlToBeCrawled.addToFrontierQueue(location);

				}
			} else {
				//return;
			}
			
		}
		br.close();
		printWriter.close();
		
		return;
	}
	
	/**
	 * parse robots txt and populate robots content
	 */
	public void parseRobotsTxt(){		// parse the robots txt content and save contents into delayed and disallowed
		if (robotsContent.length() == 0){
			System.out.println("robots txt contains nothing.");
			return;
		}
		
		String[] tmp = robotsContent.toString().split("\n");
		
		int i = 0;
		
		while (i < tmp.length){
			
			String[] pair = tmp[i].split(":");
			if ("user-agent".equalsIgnoreCase(pair[0].trim())){				// WAITING FOR PIAZZA CLARIFICATION
				
				if ("*".equals(pair[1].trim()) && disallowed.size() == 0){			// if we have defaults, keep it in records 
					//System.out.println("we have defaults now.");					// but overwrite it if we have more specific
					i++;
					while (i < tmp.length && tmp[i].indexOf(":") != -1){
						String[] restrictions = tmp[i].split(":");
						
						if ("Disallow".equalsIgnoreCase(restrictions[0].trim())){
							if (restrictions[1].trim().length() > 0){		// make sure it has specified some files
								disallowed.add(restrictions[1].trim());
							}
						} else if ("Crawl-delay".equalsIgnoreCase(restrictions[0].trim())){
							delay = Long.parseLong(restrictions[1].trim());
						} else if ("Allow".equalsIgnoreCase(restrictions[0].trim())){
							if (restrictions[1].trim().length() > 0){		// make sure it has specified some files
								allowed.add(restrictions[1].trim());
							}
						}
						i++;					
					}
					
				} else if (userAgent.equalsIgnoreCase(pair[1].trim())){
					
					//System.out.println("more specific one overwrite defaults.");
					disallowed = new ArrayList<>();
					delay = 5;
					
					i++;
					while (i < tmp.length && tmp[i].indexOf(":") != -1){
						String[] restrictions = tmp[i].split(":");
						
						if ("Disallow".equalsIgnoreCase(restrictions[0].trim())){
							if (restrictions[1].trim().length() > 0){		// make sure it has specified some files
								disallowed.add(restrictions[1].trim());
							}
						} else if ("Crawl-delay".equalsIgnoreCase(restrictions[0].trim())){
							delay = Long.parseLong(restrictions[1].trim());
						} else if ("Allow".equalsIgnoreCase(restrictions[0].trim())){
							if (restrictions[1].trim().length() > 0){		// make sure it has specified some files
								allowed.add(restrictions[1].trim());
							}
						}	
						i++;					
					}
					
					break;
				}
			}
			
			i++;
		}
		
		/*
		System.out.println("the following is robots for cis455crawler");
		for (String tmpString : disallowed){
			System.out.println(tmpString);
		}
		System.out.println("the delay is: " + delay);
		*/
		
		
	}
	
	/**
	 * @return delay as a long
	 */
	public long getDelay(){
		return delay;
	}
	
	/**
	 * @return disallowed arraylist
	 */
	public ArrayList<String> getDisallowed(){
		return disallowed;
	}
	
	public ArrayList<String> getAllowed(){
		return allowed;
	}
	
}
