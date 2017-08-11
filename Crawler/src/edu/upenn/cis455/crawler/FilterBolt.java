package edu.upenn.cis455.crawler;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

/*
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.bolt.IRichBolt;
import edu.upenn.cis.stormlite.bolt.OutputCollector;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
*/
import edu.upenn.cis455.crawler.FileGetter;
import edu.upenn.cis455.crawler.MyHttpClient;
import edu.upenn.cis455.crawler.XPathCrawler;
import edu.upenn.cis455.crawler.info.FrontierQueue;
import edu.upenn.cis455.crawler.info.URLInfo;

public class FilterBolt implements IRichBolt{
	
	Fields fields = new Fields();
	String id = UUID.randomUUID().toString();
	
	private OutputCollector collector;
	private FrontierQueue frontierQueue;
	
	public FilterBolt(){
		XPathCrawler xPathCrawler = XPathCrawler.getInstance();
		frontierQueue = xPathCrawler.frontierQueue;
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
		@SuppressWarnings("unchecked")
		Queue<String> links = (Queue<String>) input.getObjectByField("URLStream");
		//String urlString = input.getStringByField("URL");
		
		
		//System.out.println("Filter Bolt receives: " + urlString);
		
		FileGetter fileGetter = FileGetter.getInstance();
		
		while (!links.isEmpty()){							/// filter out disallowed urls
			String tmp = links.poll();
			URLInfo urlInfo = new URLInfo(tmp);
			String rootDir = "http://" + urlInfo.getHostName();
			
			if (!fileGetter.disallowMap.containsKey(rootDir)){
				//System.out.println("getting robot txt for " + rootDir);
				MyHttpClient client = new MyHttpClient(rootDir, frontierQueue);
				try {
					client.getRobotsTxt(false, "");
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				client.parseRobotsTxt();
				
				fileGetter.disallowMap.put(rootDir, client.getDisallowed());			// store crawl info
				fileGetter.allowMap.put(rootDir, client.getAllowed());
				fileGetter.delayMap.put(rootDir, client.getDelay());
				fileGetter.lastCrawled.put(rootDir, System.currentTimeMillis());	
				
				links.offer(tmp);
				continue;
			} else {
				//if (!checkLastAccess(fileGetter, rootDir)){
					//System.out.println("not modified");
					//continue;
				if (!isAllowed(tmp, rootDir) || !checkLastAccess(fileGetter, rootDir)){
					//System.out.println(tmp + " is NOT ALLOWED.");
					continue;
				} else {
					//System.out.println("add a link to queue");
					//System.out.println("frontier queue size is " + frontierQueue.size());
					frontierQueue.addToFrontierQueue(tmp);
				}
			}
		}
		
		//System.out.println("final frontier queue size is " + frontierQueue.size());
		
	}
	
	
	private boolean checkLastAccess(FileGetter fileGetter, String rootDir){
		
		long delay = fileGetter.delayMap.get(rootDir) * 1000;
		long wait = delay - (System.currentTimeMillis() - fileGetter.lastCrawled.get(rootDir));
		
		return wait <= 0;
	}
	
	
	public boolean isAllowed(String url, String rootDir){
		
		if (url.equalsIgnoreCase(rootDir)){
			return true;
		}
		
		FileGetter fileGetter = FileGetter.getInstance();
		
		ArrayList<String> disallowed = fileGetter.disallowMap.get(rootDir);
		ArrayList<String> allowed = fileGetter.allowMap.get(rootDir);
		
		
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
