package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.Webpage;


/*
 * file getter is responsible for getting webpages from Queue and sends requests and interacts with DB
 */


public class FileGetter {
	
	private int maxSize;
	private URLInfo urlInfo;
	private int port;
	private String rootDir;
	private String contentType;
	private final String userAgent = "cis455crawler";
	
	private final String dateFormat1 = "EEE, dd MMM yyyy HH:mm:ss z";
	private final String dateFormat2 = "EEEE, dd-MMM-yy HH:mm:ss z";
	private final String dateFormat3 = "EEE MMM dd HH:mm:ss yyyy";
	
	private HashMap<String, String> headHeaders;
	private HashMap<String, String> getHeaders;
	private StringBuffer sb;
	public FrontierQueueRuntime frontierQueueRuntime;
	
	private final String TEXTHTML = "text/html";
	private final String TEXTXML = "text/xml";
	private final String APPXML = "application/xml";
	private final String XML = "+xml";
	private static FileGetter fileGetter = null;
	
	private URL url = null;
	
	private DBWrapper dbWrapper;
	
	/**
	 * @param maxSize
	 */
	private FileGetter(int maxSize, FrontierQueue urlToBeCrawled, String root){
		this.maxSize = maxSize * 1024 * 1024;
		
		sb = new StringBuffer();
		headHeaders = new HashMap<>();
		getHeaders = new HashMap<>();		///
		this.urlToBeCrawled = urlToBeCrawled;
		dbWrapper = DBWrapper.getInstance(root);
	}
	
	public static synchronized FileGetter getInstance(){
		return fileGetter;
	}
	
	public static synchronized FileGetter getInstance(int maxSize, FrontierQueue urlToBeCrawled, String root){
		if (fileGetter == null){
			fileGetter = new FileGetter(maxSize, urlToBeCrawled, root);
		}
		return fileGetter;
	}
	
	/**
	 * @param urlString
	 * @return a webpage object corresponding to url
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 */
	public Webpage getWebpage(String urlString) throws UnknownHostException, IOException, NoSuchAlgorithmException{
		
		
		
		if (urlString.startsWith("https")){
			//System.out.println(urlString + " is a safe url");
			isSafe = true;
			url = new URL(urlString);
			rootDir = "https://" + url.getHost();
			//System.out.println("HTTP root dir is " + rootDir);
			port = url.getPort() == -1 ? 80 : url.getPort();
			
		} else if (urlString.startsWith("http")){
			//System.out.println(urlString + " is NOT a safe url");
			isSafe = false;
			urlInfo = new URLInfo(urlString);
			rootDir = "http://" + urlInfo.getHostName();
			//System.out.println("HTTPS rootdir is " + rootDir);
			port = urlInfo.getPortNo();
			
		}
		
		if (!disallowMap.containsKey(rootDir)){		// if we have NOT had the robots txt 
			
			if (isSafe){
				//System.out.println("a safe client");
				MyHttpsClient client = new MyHttpsClient(rootDir);
				
				client.getRobotsTxt(false, "");
				client.parseRobotsTxt();
				
				disallowMap.put(rootDir, client.getDisallowed());			// store crawl info
				allowMap.put(rootDir, client.getAllowed());
				delayMap.put(rootDir, client.getDelay());
				lastCrawled.put(rootDir, System.currentTimeMillis());
				
				
			} else {
				//System.out.println("a unsafe client");
				MyHttpClient client = new MyHttpClient(rootDir, urlToBeCrawled);
				client.getRobotsTxt(false, "");
				client.parseRobotsTxt();
				
				disallowMap.put(rootDir, client.getDisallowed());			// store crawl info
				allowMap.put(rootDir, client.getAllowed());
				delayMap.put(rootDir, client.getDelay());
				lastCrawled.put(rootDir, System.currentTimeMillis());							/// 
			}
				
		}
		
			
		//checkLastAccess(rootDir);			// check last access time, wait if needs be
		
		if (checkLastAccess(rootDir)){
			lastCrawled.put(rootDir, System.currentTimeMillis());
		} else {
			//System.out.println("push this to the end of queue");
			urlToBeCrawled.addToFrontierQueue(urlString);
			return null;
		}
		
		if (!isAllowed(urlString, rootDir)){		// if this is forbidden
			System.out.println(urlString + ": Disallowed");
			return null;
		} 
		// ISALLOWED
		
		
		try {
			Thread.sleep(delayMap.get(rootDir) * 1000);
		} catch (InterruptedException e) {
			
		}
		if (!isSafe){
			//System.out.println("not safe");
			sendHeadReq(urlString, urlInfo);	
		} else {
			sendHeadReq(urlString, url);
		}
		
		if (headHeaders.size() == 0){
			
			System.out.println(urlString + ": Disallowed");
			return null;
		}
		// HEAD REQUEST
		
		XPathCrawler.left--;
		System.out.println(XPathCrawler.left);
		
		if (!hasCorrectType() || !hasRightSize()){				//check size and type
			if (!hasCorrectType()){
				System.out.println("unsupported type");
			} else {
				System.out.println("file is too large.");
			}
			return null;
		}
		contentType = headHeaders.get("content-type");
		// SIZE AND EXTENSION
		
		
		Webpage webpage = dbWrapper.getWebpage(urlString);
		
		if (webpage == null){
			//System.out.println("NO THIS FILE IN DB.");
			
			try {
				Thread.sleep(delayMap.get(rootDir) * 1000);
			} catch (InterruptedException e) {
				
			}
			
			if (!isSafe){
				sendGetReq(urlString, rootDir);
			} else {
				sendGetReq(urlString, url);
			}
			
			
			webpage = new Webpage(new Date(), urlString, contentType, sb.toString());

		} else {
																				// checking last modified date and last crawled date
			Date lastModified = getLastModifiedDate();
			Date lastCrawled = webpage.getLastModifiedTimeDate();
			
			if (lastModified == null || lastCrawled == null || lastModified != null && lastCrawled != null && lastCrawled.before(lastModified)){
				System.out.println("MODIFIED AFTER LAST CRAWL");
				
				try {
					Thread.sleep(delayMap.get(rootDir) * 1000);
				} catch (InterruptedException e) {
					
				}
				if (!isSafe){
					sendGetReq(urlString, rootDir);
				} else {
					sendGetReq(urlString, url);
				}
				
				webpage = new Webpage(new Date(), urlString, contentType, sb.toString());

			} else {
				System.out.println(urlString + ": Not modified");			// THIS SHOULD BE PRESERVED!!!
				return dbWrapper.getWebpage(urlString);
			}
		}
		
		if (hasRightSize()){
			dbWrapper.putWebpage(webpage);
			//XPathCrawler.maxNum--;
			return webpage;
		} else{
			return null;
		}
	}
	
	/**
	 * @param rootDir
	 * check last time visiting this website
	 */
	private boolean checkLastAccess(String rootDir){
		long delay = delayMap.get(rootDir) * 1000;
		long wait = delay - (System.currentTimeMillis() - lastCrawled.get(rootDir));
		
		return wait <= 0;
	}
	
	
	/**
	 * @param url
	 * @param rootDir
	 * @return a boolean indicating if it is allowed
	 */
	public boolean isAllowed(String url, String rootDir){
		
		if (url.equalsIgnoreCase(rootDir)){
			return true;
		}
		
		ArrayList<String> disallowed = disallowMap.get(rootDir);
		ArrayList<String> allowed = allowMap.get(rootDir);
		
		
		for (String dis : disallowed){
			if ("/".equals(dis)){
				
				for (String all : allowed){
					if ("/".equals(all)){
						return true;
					}
					
					String tmp1 = all.endsWith("/")? all : all + "/";			// address slash problem
					String tmp2 = url.endsWith("/")? url : url + "/";
					
					if (tmp2.contains(tmp1)){
						return true;
					}	
				}
				
				return false;
			}
			
			String tmp1 = dis.endsWith("/")? dis : dis + "/";			// address slash problem
			String tmp2 = url.endsWith("/")? url : url + "/";
			
			if (tmp2.contains(tmp1)){
				return false;
			}
		}
		
		return true;
	}
	
	
	/**
	 * @return a boolean indicating if it has correct type
	 */
	public boolean hasCorrectType(){
		String typeString;
		if (headHeaders.containsKey("content-type")){
			typeString = headHeaders.get("content-type");
			headHeaders.put("content-type", typeString);
		} else if (headHeaders.containsKey("Content-Type")){
			typeString = headHeaders.get("Content-Type");
			headHeaders.put("content-type", typeString);
		} else{
			System.out.println("no content type header found.");
			return false;
		}
		
		return typeString.equalsIgnoreCase(TEXTHTML) ||
			   typeString.equalsIgnoreCase(TEXTXML) ||
			   typeString.equalsIgnoreCase(APPXML) ||
			   typeString.endsWith(XML) || 
			   typeString.contains(TEXTHTML) ||
			   typeString.contains(TEXTXML) ||
			   typeString.contains(APPXML) ||
			   typeString.contains(XML);
	}
	
	/**
	 * @return a boolean indicating if it has right size
	 */
	public boolean hasRightSize(){
		long len;
		if (headHeaders.containsKey("content-length")){
			len = Long.parseLong(headHeaders.get("content-length"));
		} else if (headHeaders.containsKey("Content-Length")){
			len = Long.parseLong(headHeaders.get("Content-Length"));
		} else {
			System.out.println("no content length header found.");
			return true;				// without content length, get it first then decide 
		}
		headHeaders.put("content-length", "" + len);
		return len <= this.maxSize;
	}
	
	/**
	 * @return a date object 
	 */
	public Date getLastModifiedDate(){
		
		String dateString;
		if (headHeaders.containsKey("last-modified")){
			dateString = headHeaders.get("last-modified");
			Date date = getDate(dateString);
			return date;
		} else if (headHeaders.containsKey("Last-Modified")){
			dateString = headHeaders.get("Last-Modified");
			Date date = getDate(dateString);
			return date;
		} else {
			return null;			
		}
		
	}
	
	/*
	public boolean contentSeen(Webpage webpage){
		
		for (String string : dbWrapper.accessor.webpageIndex.keys()){
			Webpage webpage2 = dbWrapper.getWebpage(string);
			if (webpage.sampleString.toString().equals(webpage2.sampleString.toString())){
				System.out.println(webpage.getURL() + " has been seen at the following URL:");
				System.out.println(webpage2.getURL());
				return true;
			}
		}
		
		
		return false;
	}
	*/
	
	
	/**
	 * @param url
	 * @param urlInfo
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	public void sendHeadReq(String url, URLInfo urlInfo) throws UnknownHostException, IOException{
		headHeaders = new HashMap<>();
		System.out.println(url + ": Sending HEAD request");
		@SuppressWarnings("resource")
		Socket socket = new Socket(urlInfo.getHostName(), port);
		PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);			// get outputstream
		
		
		StringBuilder sb = new StringBuilder();	
		sb.append("HEAD " + url + " HTTP/1.1\r\n");
		sb.append("Host: " + this.rootDir + ":" + this.port + "\r\n");		/// 
		sb.append("User-Agent: " + userAgent + "\r\n");
		sb.append("Connection: close\r\n\r\n");
		printWriter.append(sb.toString());
		
		printWriter.flush();						// send get request to download robots txt from host
		
		
		BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		String statusLine = br.readLine();
		if (statusLine.indexOf("200") != -1){			// robots txt is available now
			
			String line;
			
			while (true){					// first while loop reads in all the headers
				try {
					line = br.readLine(); 
					
					String[] strs = line.split(":", 2);
					headHeaders.put(strs[0].trim().toLowerCase(), strs[1].trim());
						
				} catch (Exception e) {
					break;
				}
					
				if (line == null) break;
				if ("".equals(line)) break;
			}
			
			
		} else if (statusLine.indexOf("301") != -1 || statusLine.indexOf("302") != -1){			// if the file has been moved to some places else
			System.out.println("301/302 move permanently/found");
			
			String line = null;
			String location = null;
			while (true){		
				try {
					line = br.readLine();
					
					String[] strs = line.split(":", 2);
					if ("Location".equalsIgnoreCase(strs[0].trim())){			// get the new location of robots txt

						location = strs[1].trim().toLowerCase();
						//System.out.println("get a location: " + location);
						System.out.println("location is: " + location);
						break;
					}
					
				} catch (Exception e) {
					break;
				}
					
				if (line == null) break;
				if ("".equals(line)) break;
			}
			
			if (location != null){			// if we have new location for file
				System.out.println("location is " + location);
				if (!location.startsWith("https") && !location.startsWith("http")){
					
					if (isSafe){
						urlToBeCrawled.addToFrontierQueue("https://" + location);
					} else {
						urlToBeCrawled.addToFrontierQueue("http://" + location);
					}
					return;
				} else {
					urlToBeCrawled.addToFrontierQueue(location);
					return;
				}
			} else {
				
			}
			
		}
		
		
		br.close();
		printWriter.close();
		
		return;
	}
	
	
	public void sendHeadReq(String urlString, URL url) throws UnknownHostException, IOException{
		headHeaders = new HashMap<>();
		System.out.println(urlString + ": Sending HEAD request");
		
		URLConnection urlConnection = (URLConnection)url.openConnection();
    	
		
		for (String string : urlConnection.getHeaderFields().keySet()){
			if (string == null) continue;
			if (string.equalsIgnoreCase("Location")){
				headHeaders.clear();
				urlToBeCrawled.addToFrontierQueue(urlConnection.getHeaderField(string));
				return;
			}
			StringBuffer sb = new StringBuffer();
			for (String string2 : urlConnection.getHeaderFields().get(string)){
				sb.append(string2);
			}
			
			headHeaders.put(string, sb.toString());

		}
    	
	}
	
	/**
	 * @param url
	 * @param rootDir
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	public void sendGetReq(String url, String rootDir) throws UnknownHostException, IOException{
		
		getHeaders = new HashMap<>();
		System.out.println(url + ": Downloading");				// THIS PRINT SHOULD BE PRESERVED!!!
		@SuppressWarnings("resource")
		Socket socket = new Socket(urlInfo.getHostName(), port);
		PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);			// get outputstream
		
		StringBuilder sb = new StringBuilder();	
		sb.append("GET " + url + " HTTP/1.1\n");
		sb.append("Host: " + this.rootDir + ":" + this.port + "\n");		/// which number is this????
		sb.append("User-Agent: " + userAgent + "\n");
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
					getHeaders.put(strs[0].trim().toLowerCase(), strs[1].trim());
						
				} catch (Exception e) {
					break;
				}
					
				if (line == null) break;
				if ("".equals(line)) break;
			}
			
			line = "a";
			this.sb = new StringBuffer();
			while (true){						// second while loop reads in all the robots txt content
				try {
					if (br.ready() && line != null){// && line.length() != 0){
						line = br.readLine(); 
						this.sb.append(line + "\n");
					} else {
						break;
					}
				} catch (IOException e) {
					break;
				}
			}
			
			
		} else if (statusLine.indexOf("301") != -1 || statusLine.indexOf("302") != -1){			// if the robots txt has been moved to some places else
			System.out.println("301/302 move permanently/found");
			
			String line = null;
			String location = null;
			while (true){		
				try {
					line = br.readLine();
					
					String[] strs = line.split(":", 2);
					if ("Location".equalsIgnoreCase(strs[0].trim())){			// get the new location of robots txt

						location = strs[1].trim();
						//System.out.println("get a location: " + location);
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

				sendGetReq(location, url);
				return;

			} else {
				
			}
			
		}
			
		br.close();
		printWriter.close();
	
		return;
	}
	
	

	public void sendGetReq(String urlString, URL url) throws UnknownHostException, IOException{
		sb = new StringBuffer();
		headHeaders = new HashMap<>();
		System.out.println(urlString + ": Sending GET request");
		
		URLConnection urlConnection = (URLConnection)url.openConnection();
    	
		InputStreamReader in = new InputStreamReader(urlConnection.getInputStream());
    	
    	BufferedReader br = new BufferedReader(in);
    	
    	String inputString;
    	while ((inputString = br.readLine()) != null){
    		//System.out.println(inputString);
    		sb.append(inputString);
    	}
    	
    	if (!headHeaders.containsKey("Content-Length")){
    		headHeaders.put("Content-Length", "" + sb.length());
    	}
		
		
    	
	}
	

	/**
	 * @param dateString
	 * @return a date object
	 */
	public Date getDate(String dateString){
		
		Date date = null;
		
		SimpleDateFormat sdf1 = new SimpleDateFormat(dateFormat1);
		SimpleDateFormat sdf2 = new SimpleDateFormat(dateFormat2);
		SimpleDateFormat sdf3 = new SimpleDateFormat(dateFormat3);
		
		try {
			date = sdf1.parse(dateString);
		} catch (ParseException e) {
			try {
				date = sdf2.parse(dateString);
			} catch (ParseException e1) {
				try {
					date = sdf3.parse(dateString);
				} catch (ParseException e2) {
					// TODO Auto-generated catch block
					System.out.println("parse last modified header fails! in file getter");				
				}
			}
		}
		return date;
	}
	
}
