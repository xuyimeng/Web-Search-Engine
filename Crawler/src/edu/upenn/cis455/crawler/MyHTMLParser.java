package edu.upenn.cis455.crawler;

import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.upenn.cis455.crawler.info.FrontierQueue;


/*
 * parse the HTML webpage and get all hrefs
 */
public class MyHTMLParser {			// only parse the text/html
	
	
	private FrontierQueue urlToBeCrawled = null;
	private Document document;
	private Elements links;
	
	public MyHTMLParser(){
	
	}
	
	/**
	 * @param urlToBeCrawled
	 */
	public MyHTMLParser(FrontierQueue urlToBeCrawled){
		this.urlToBeCrawled = urlToBeCrawled;
	}
	
	/**
	 * @param urlString
	 * parse url and save links in queue
	 */
	public Elements parseForHtml(String urlString){
		if (urlString == null || urlToBeCrawled == null) return null;
		
		try {
			document = Jsoup.connect(urlString).get();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e1){
			return null;
		}
		
		links = document.select("a[href]");
		
		for (Element element : links){
			//System.out.println(element);
			urlToBeCrawled.addToFrontierQueue(element.attr("abs:href"));		// get the absolute path of links
			//System.out.println(element.attr("href"));
		}
		return links;
	}
	
}
