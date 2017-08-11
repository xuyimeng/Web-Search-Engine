package edu.upenn.cis455.crawler;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import edu.upenn.cis455.crawler.XPathCrawler;

public class URLSpout implements IRichSpout
{
	static Logger log = Logger.getLogger(URLSpout.class);
	FrontierQueueRuntime frontierQueueRuntime;
	public SpoutOutputCollector collector;
	public String id = UUID.randomUUID().toString();
	
	public URLSpout(){
		log.debug("Starting Spout");
		this.frontierQueueRuntime = XPathCrawler.frontierQueueRuntime;
	}
	
	@Override
	public String getExecutorId() {
		return id;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("URL"));
	}

	@Override
	public void open(Map<String, String> config, TopologyContext topo,
			SpoutOutputCollector collector) {

		this.collector = collector;
	}

	@Override
	public void close() {
		
	}

	@Override
	public void nextTuple() {
		
		try {
			if (!frontierQueueRuntime.isEmpty()){
				String urlString = frontierQueueRuntime.poll();
				
				if (!RobotCache.checkDelay(urlString)){
					frontierQueueRuntime.offer(urlString);
				} else {
					this.collector.emit(new Values<Object>(urlString));
					log.info(urlString + " is spouted");
				}
			} 
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			log.info(sw.toString());
		}
		
		Thread.yield();
	}
	

	@Override
	public void setRouter(IStreamRouter router) {
		this.collector.setRouter(router);
	}

}