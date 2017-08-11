package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class MyHttpsClient {
	//public URLInfo url;
	//public URL url;
	private final String userAgent = "cis455crawler";
	
	//public HashMap<String, String> info;  //keys are URLSTRING, HOST, ROOTPATH, RELATIVEPATH
	public HashMap<String, String> headers; // keys are headers' names
	
	//public HashMap<String, String> disallowed;	// disallowed files
	//public HashMap<String, Long> delayed;	// delayed
	
	public int port;
	//public StringBuffer sb;					
	public StringBuffer robotsContent;// robots txt content
	private boolean isValid = true;
	@SuppressWarnings("unused")
	private String contentType = null;
	private long delay = 10;
	private ArrayList<String> disallowed;
	private ArrayList<String> allowed;
	private URL url;
	@SuppressWarnings("unused")
	private URLConnection urlConnection;
	
	
	public MyHttpsClient(String urlString) throws IOException{
		//info = new HashMap<>();
		headers = new HashMap<>();
		disallowed = new ArrayList<>();
		allowed = new ArrayList<>();
		
		robotsContent = new StringBuffer();
	
		url = new URL(urlString);
    	//HttpsURLConnection https = (HttpsURLConnection)url.openConnection();
    	urlConnection = (URLConnection)url.openConnection();
		
		port = url.getPort() == -1 ? 80 : url.getPort();
		
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
	public void getRobotsTxt(boolean isDirect, String redirect) throws UnknownHostException, IOException{ // get robots content and populate stringbuffer robotsContent
		String robotURL;
		if (isDirect){
			robotURL = redirect;
		} else {
			robotURL = "https://" + url.getHost() + "/robots.txt";
		}
		
		//String robotContent = null;
		
		if (!isValid()) return;
		
		System.out.println(robotURL + ": Downloading robots txt");
		
		URL url1 = new URL(robotURL);
    	//HttpsURLConnection https = (HttpsURLConnection)url.openConnection();
    	URLConnection urlConnection1 = (URLConnection)url1.openConnection();
    	
    	InputStreamReader in;
    	try {
    		in = new InputStreamReader(urlConnection1.getInputStream());
		} catch (FileNotFoundException e) {
			System.out.println(e.toString());
			return;
		}
    	BufferedReader br = new BufferedReader(in);
    	

		String line = "";
		/*
		while (true){						// second while loop reads in all the robots txt content
			try {
				if (br.ready() && line != null){// && line.length() != 0){
					line = br.readLine(); 
					//System.out.println("line is " + line);
					robotsContent.append(line + "\n");
				} else {
					break;
				}
			} catch (IOException e) {
				break;
			}
		}
		*/
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
		
		
		
		//System.out.println("the robots txt is: ");
		//System.out.println(robotsContent.toString());

		br.close();
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
					System.out.println("we have defaults now.");					// but overwrite it if we have more specific
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
					
					System.out.println("more specific one overwrite defaults.");
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
	
	/*
	public static void main(String args[]) throws IOException{
		MyHttpsClient client = new MyHttpsClient("https://www.github.com");
		client.getRobotsTxt(false, "");
		client.parseRobotsTxt();
		
		
		for (String string : client.getAllowed()){
			System.out.println(string);
			
		}
		System.out.println();
		for (String string : client.getDisallowed()){
			System.out.println(string);
			
		}
		
		
		//Queue<String> queue = new LinkedList<String>();
		//FileGetter fileGetter = new FileGetter(5, queue);
		//URL url = new URL("https://www.google.com");
		//fileGetter.sendHeadReq("https://www.google.com", url);
		//fileGetter.sendGetReq("https://www.google.com", url);
		//fileGetter.getWebpage("https://www.google.com");
	}
	*/
	
}
