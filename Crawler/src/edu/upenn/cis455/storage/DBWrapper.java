package edu.upenn.cis455.storage;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;


public class DBWrapper {
	
	public static String envDirectory = null;		// root directory
	public File rootFile;
	public Environment myEnv;
	public EntityStore store;
	public DBAccessor accessor;
	private static DBWrapper dbWrapper;
	
	/**
	 *  DBWrapper is a class where the crawler interacts with Berkeley DB
	 */

	
	private DBWrapper(String env){
		envDirectory = env;
		try {
			
			rootFile = new File(env);
			
			if (!rootFile.exists()){
				rootFile.mkdirs();
				rootFile.setReadable(true);
				rootFile.setWritable(true);
			}
			
			EnvironmentConfig envConfig = new EnvironmentConfig();
			envConfig.setLockTimeout(1000, TimeUnit.MILLISECONDS);
			
			StoreConfig storeConfig = new StoreConfig();
			
			envConfig.setAllowCreate(true);
			envConfig.setReadOnly(false);
			storeConfig.setAllowCreate(true);
			storeConfig.setReadOnly(false);
			
			envConfig.setTransactional(true);
			storeConfig.setTransactional(true);
			
			myEnv = new Environment(rootFile, envConfig);
			store = new EntityStore(myEnv, "EntityStore", storeConfig);
		} catch (Exception e) {
			e.printStackTrace();
		}
		accessor = new DBAccessor(store);
	}
	
	
	public synchronized static DBWrapper getInstance(String envDir){
		if (dbWrapper == null){
			dbWrapper = new DBWrapper(envDir);
		} 
		return dbWrapper;
	}
	
	public synchronized static DBWrapper getInstance(){
		return dbWrapper;
	}
	
	
	public Webpage getWebpage(String key){
		return accessor.webpageIndex.get(key);
	}
	
	public void putWebpage(Webpage webpage){
		accessor.webpageIndex.put(webpage);
	}
	
	public ArrayList<Webpage> getWebpages(){			/// refer to BDB tutorial PAGE 45
		ArrayList<Webpage> arrayList = new ArrayList<>();
		
		EntityCursor<Webpage> cursor = accessor.webpageIndex.entities();
		Iterator<Webpage> itr = cursor.iterator();
		
		while (itr.hasNext()){
			arrayList.add(itr.next());
		}
		cursor.close();
		
		return arrayList;
	}
	
	
	
	public ExtractedLinks getExtractedLinks(String url){
		return accessor.linksIndex.get(url);
	}
	
	public void putExtractedLinks(ExtractedLinks links){
		accessor.linksIndex.put(links);
	}
	
	public void addExtractedLinks(String url, String link){
		ExtractedLinks links = accessor.linksIndex.get(url);
		if (links == null){
			links = new ExtractedLinks(url);
		}
		links.addLink(link);
		putExtractedLinks(links);
		sync();
	}
	
	public ArrayList<ExtractedLinks> getAllExtractedLinks(){			/// refer to BDB tutorial PAGE 45
		ArrayList<ExtractedLinks> arrayList = new ArrayList<>();
		
		EntityCursor<ExtractedLinks> cursor = accessor.linksIndex.entities();
		Iterator<ExtractedLinks> itr = cursor.iterator();
		
		while (itr.hasNext()){
			arrayList.add(itr.next());
		}
		cursor.close();
		
		return arrayList;
	}
	
	public VisitedURL getVisitedURL(String key){
		return accessor.visitedURLIndex.get(key);
	}
	
	public void putVisitedURL(VisitedURL url){
		accessor.visitedURLIndex.put(url);
	}
	
	public boolean containsRobots(String hostName){
		if (hostName == null) return false;
		return accessor.robotIndex.contains(hostName);
	}
	
	
	public void addRobotMap(String hostName, String url){
		RobotInfo robotInfo = new RobotInfo(url);
		accessor.robotIndex.put(robotInfo);
	}
	
	public boolean isValid(String hostName, String url){
		RobotInfo robotInfo = accessor.robotIndex.get(hostName);
		
		if (robotInfo == null){
			try {
				addRobotMap(hostName, url);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		
		robotInfo = accessor.robotIndex.get(hostName);
		
		try {
			return robotInfo.isAllowed(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public long getDelay(String hostName) {
		return accessor.robotIndex.get(hostName).getDelay();
	}
	
	public long getRobotLastVisitTime(String hostName){
		return accessor.robotIndex.get(hostName).lastVisitTime;
	}
	
	public void setRobotLastVisitTime(String hostName){
		accessor.robotIndex.get(hostName).lastVisitTime = new Date().getTime();
	}
	
	public synchronized FrontierQueueDisk getFrontierQueueDisk(){
		FrontierQueueDisk queue = accessor.queueIndex.get("FrontierQueueDisk");
		if(queue == null) {
			queue = new FrontierQueueDisk();
			accessor.queueIndex.put(queue);
			sync();
		}
		return queue;
	}
	
	public synchronized void pushIntoFrontierQueueDisk(Queue<String> queue2){
		FrontierQueueDisk queue = getFrontierQueueDisk();
		while (!queue2.isEmpty()){
			queue.offer(queue2.poll());
		}		
		accessor.queueIndex.put(queue);
	}
	
	public synchronized void pollFromFrontierQueueDisk(int num, Queue<String> memoryQueue){
		FrontierQueueDisk queue = getFrontierQueueDisk();
		int count = 0;
		while(!queue.isEmpty() && count < num) {
			String url = queue.poll();
			memoryQueue.offer(url);
			count++;
		}
		accessor.queueIndex.put(queue);
	}
	
	public boolean isFrontierQueueDiskEmpty(){
		FrontierQueueDisk frontierQueueDisk = getFrontierQueueDisk();
		return frontierQueueDisk.isEmpty();
	}
	
	
	/**
	 * close Berkeley DB
	 */
	public synchronized void close(){
		if (store != null) store.close();
		if (myEnv != null) myEnv.close();
		dbWrapper = null;
	}
	
	public void sync(){
		store.sync();
		myEnv.sync();
	}
}
