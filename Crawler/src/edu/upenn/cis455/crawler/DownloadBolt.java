package edu.upenn.cis455.crawler;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;


import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
import edu.upenn.cis455.crawler.MyHTMLParser;
import edu.upenn.cis455.crawler.XPathCrawler;
import edu.upenn.cis455.crawler.info.FrontierQueue;

import edu.upenn.cis455.storage.Webpage;

public class DownloadBolt implements IRichBolt{
	
	Fields fields = new Fields("URL", "TYPE", "WEBPAGE", "URLStream");
	String idString = UUID.randomUUID().toString();
	private OutputCollector collector;
	private FrontierQueue urlToBeCrawled;
	
	//private StringBuffer sb;		// hold the possible file downloaded
	//private HashMap<String, String> getHeaders;
	
	public DownloadBolt(){
		XPathCrawler xPathCrawler = XPathCrawler.getInstance();
		this.urlToBeCrawled = xPathCrawler.frontierQueue;
	}
	
	@Override
	public String getExecutorId() {
		return idString;
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
		
		Webpage webpage = (Webpage)input.getObjectByField("WEBPAGE");
		String urlString = input.getStringByField("URL");
		//System.out.println("Download Bolt receives: " + urlString + " and webpage is " + ((webpage == null) ? " null " : " not null."));
		if (webpage == null) return;
		
		
		long time = System.currentTimeMillis();
		FileGetter fileGetter = FileGetter.getInstance();
		fileGetter.lastCrawled.put(urlString, time);
		
		MyHTMLParser parser = new MyHTMLParser(urlToBeCrawled);
		
		Queue<String> links = new LinkedList<String>();
		
		Elements elements = parser.parseForHtml(urlString);
		
		//System.out.println("Downloading and extracting links!");
		
		for (Element elem : elements){
			links.add(elem.attr("abs:href"));
		}
		//System.out.println("the size of links is: " + links.size());
		//System.out.println("Download Bolt emitting: " + urlString);
		collector.emit(new Values<Object>(urlString, webpage.getContentType(), webpage, links));
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
