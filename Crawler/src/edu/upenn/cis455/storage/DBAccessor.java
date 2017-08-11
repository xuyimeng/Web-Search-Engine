package edu.upenn.cis455.storage;

import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;


public class DBAccessor {
	public PrimaryIndex<String, Webpage> webpageIndex;
	public PrimaryIndex<String, ExtractedLinks> linksIndex;
	public PrimaryIndex<String, FrontierQueueDisk> queueIndex;
	public PrimaryIndex<String, RobotInfo> robotIndex;
	public PrimaryIndex<String, VisitedURL> visitedURLIndex;
	
	public DBAccessor(EntityStore store){
		visitedURLIndex = store.getPrimaryIndex(String.class, VisitedURL.class);
		queueIndex = store.getPrimaryIndex(String.class, FrontierQueueDisk.class);
		webpageIndex = store.getPrimaryIndex(String.class, Webpage.class);
		robotIndex = store.getPrimaryIndex(String.class, RobotInfo.class);
		linksIndex = store.getPrimaryIndex(String.class, ExtractedLinks.class);
	}
}
