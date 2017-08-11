
package edu.upenn.cis455.crawler;
import edu.upenn.cis455.crawler.XPathCrawler;

public class XPathCrawlerFactory {
	public XPathCrawler getCrawler(String url, String root, String maxSize) {
		return new XPathCrawler(url, root, maxSize);
	}
	
	public XPathCrawler getCrawler(String url, String root, String maxSize, String maxNum) {
		return new XPathCrawler(url, root, maxSize, maxNum);
	}
}
