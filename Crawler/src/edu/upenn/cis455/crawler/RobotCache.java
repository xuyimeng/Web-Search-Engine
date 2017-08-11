package edu.upenn.cis455.crawler;

import java.util.Date;

import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.storage.DBWrapper;

public class RobotCache {
	private static DBWrapper db = DBWrapper.getInstance(XPathCrawler.root);
	
	public static void addRobotsTxt(String url){
		URLInfo urlInfo = new URLInfo(url);
		String hostName = urlInfo.getHostName();
		if (hostName == null) return;
		if (!db.containsRobots(url)){
			db.addRobotMap(hostName, url);
		}
	}
	
	public static boolean isValid(String url){
		URLInfo urlInfo = new URLInfo(url);
		String hostName = urlInfo.getHostName();
		return db.isValid(hostName, url);
	}
	
	public static boolean checkDelay(String url){
		URLInfo urlinfo = new URLInfo(url);
		String hostName = urlinfo.getHostName();
		
		if (db.containsRobots(hostName)){
			long delay = db.getDelay(hostName) * 1000;
			long now = new Date().getTime();
			long lastVisitTime = db.getRobotLastVisitTime(hostName);
			
			if ((lastVisitTime + delay) <= now) {
				db.setRobotLastVisitTime(hostName);
				return true;
			} else {
				return false;
			}
		} else {
			addRobotsTxt(url);
			return true;
		}	
	}

}
