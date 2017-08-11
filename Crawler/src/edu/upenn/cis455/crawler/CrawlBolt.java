package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

/*
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.bolt.IRichBolt;
import edu.upenn.cis.stormlite.bolt.OutputCollector;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;
*/
import edu.upenn.cis455.crawler.FileGetter;
import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.storage.Webpage;

public class CrawlBolt implements IRichBolt{
	
	static Logger log = Logger.getLogger(CrawlBolt.class);
	String executorId = UUID.randomUUID().toString();

	public FrontierQueueRuntime frontierQueueRuntime;
	
	Fields fields = new Fields("URL", "WEBPAGE");
	String id = UUID.randomUUID().toString();
	
	private OutputCollector collector;

	
	public CrawlBolt(){
		log.debug("Starting CrawlBolt");
    	this.frontierQueueRuntime = XPathCrawler.frontierQueueRuntime;
	}
	
	@Override
	public String getExecutorId() {
		return id;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(fields);
	}

	@Override
	public void cleanup() {
		
	}

	@Override
	public void execute(Tuple input) {
		
		String urlString = input.getStringByField("URL");
		//System.out.println("CrawlerBolt receives: " + urlString);
		FileGetter fileGetter = FileGetter.getInstance();
		
		Webpage webpage = null;
		try {
			webpage = fileGetter.getWebpage(urlString);			// crawl it and put it in db if needs be
		} catch (Exception e) {
			
			
		}
		if (webpage == null) return;
		//System.out.println("CrawlerBolt emitting: " + urlString);
		
		collector.emit(new Values<Object>(urlString, webpage));					/// so far it returns NULL for files should not be retrieved
		
		/*
		try {							// either send it directly or use my own get request
			//connection = Jsoup.connect(urlString).userAgent("cis455crawler");
			
			//Response response = connection.header("User-Agent", "cis455crawler").execute();
			//Document document = response.parse();
			
			//String contentType = response.contentType();
			sendGetReq(urlString);
			
			
			collector.emit(new Values<Object>(urlString, sb.toString(), contentType));
			
		} catch (IOException e) {
			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			e.printStackTrace(printWriter);
		}
		*/
		
	}	
	
	
	@SuppressWarnings("unused")
	public void sendGetReq(String url) throws UnknownHostException, IOException{
		
		getHeaders = new HashMap<>();
		System.out.println(url + ": Downloading");				// THIS PRINT SHOULD BE PRESERVED!!!
		
		URLInfo urlInfo = new URLInfo(url);
		int port = urlInfo.getPortNo();
		String rootDir = "http://" + urlInfo.getHostName();
		
		@SuppressWarnings("resource")
		Socket socket = new Socket(urlInfo.getHostName(), port);
		PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);			// get outputstream
		
		StringBuilder sb = new StringBuilder();	
		sb.append("GET " + url + " HTTP/1.1\n");
		sb.append("Host: " + rootDir + ":" + port + "\n");		/// which number is this????
		sb.append("User-Agent: cis455crawler\n");
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
			
			contentType = "text/html";
			if (getHeaders.get("content-type").startsWith("text/html")){
				contentType = "text/html";
			} else if (getHeaders.get("content-type").startsWith("text/xml")){
				contentType = "text/xml";
			} else if (getHeaders.get("content-type").startsWith("application/xml")){
				contentType = "application/xml";
			} else if (getHeaders.get("content-type").endsWith("+xml")){
				contentType = getHeaders.get("content-type");
			} 													/// do we need to consider charset???
			
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

				sendGetReq(location);
				return;

			} else {
				
			}
			
		}
			
		br.close();
		printWriter.close();
	
		return;
	}

	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void setRouter(IStreamRouter router) {
		this.collector.setRouter(router);
	}

	@Override
	public Fields getSchema() {
		return fields;
	}

}
