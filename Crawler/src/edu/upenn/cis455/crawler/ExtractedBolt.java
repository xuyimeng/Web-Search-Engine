package edu.upenn.cis455.crawler;

import java.util.Map;
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
import edu.upenn.cis455.crawler.XPathCrawler;
import edu.upenn.cis455.crawler.info.FrontierQueue;
import edu.upenn.cis455.crawler.info.URLInfo;

public class ExtractedBolt implements IRichBolt{
	
	Fields fields = new Fields();
	String id = UUID.randomUUID().toString();
	
	private OutputCollector collector;
	private FrontierQueue frontierQueue;
	
	public ExtractedBolt(){
		XPathCrawler xPathCrawler = XPathCrawler.getInstance();
		//this.frontierQueue = FrontierQueue.getInstance();
		frontierQueue = xPathCrawler.frontierQueue;
	}
	
	@Override
	public String getExecutorId() {
		// TODO Auto-generated method stub
		return id;
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// TODO Auto-generated method stub
		declarer.declare(fields);
	}

	@Override
	public void cleanup() {
		
	}

	@Override
	public void execute(Tuple input) {

		String links = input.getStringByField("extractedLink");
		URLInfo urlInfo = new URLInfo(links);
		String urlString = "http://" + urlInfo.getHostName();
		FileGetter fileGetter = FileGetter.getInstance();
		
		System.out.println("extraced links added to frontier queue");
		if (fileGetter.isAllowed(links, urlString)){
			frontierQueue.addToFrontierQueue(urlString);
		}
		
		
	}

	@Override
	public void prepare(Map<String, String> stormConf, TopologyContext context,
			OutputCollector collector) {
		// TODO Auto-generated method stub
		this.collector = collector;
	}

	@Override
	public void setRouter(IStreamRouter router) {
		// TODO Auto-generated method stub
		this.collector.setRouter(router);
	}

	@Override
	public Fields getSchema() {
		// TODO Auto-generated method stub
		return fields;
	}
}
