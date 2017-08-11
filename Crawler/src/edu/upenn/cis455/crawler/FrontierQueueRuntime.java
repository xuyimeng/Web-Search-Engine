package edu.upenn.cis455.crawler;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import edu.upenn.cis455.storage.DBWrapper;

public class FrontierQueueRuntime {
	public Queue<String> queue1 = null;
	public Queue<String> queue2 = null;
	public int maxSize = 400000;
	public volatile static int URLExecuted = 0;
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(FrontierQueueRuntime.class);
	private DBWrapper db = DBWrapper.getInstance(XPathCrawler.root);
	
	public FrontierQueueRuntime(){
		queue1 = new LinkedBlockingQueue<String>();
		queue2 = new LinkedBlockingQueue<String>();
	}
	
	public FrontierQueueRuntime(int size){
		this();
		maxSize = size;
	}
	
	public void getFrontierQueueFromDisk(){
		if (queue1.isEmpty()){
			db.pushIntoFrontierQueueDisk((LinkedBlockingQueue<String>)queue2);
			db.pollFromFrontierQueueDisk(1000, queue1);
		}
	}
	
	public void pushFrontierQueueIntoDisk(){
		db.pushIntoFrontierQueueDisk((LinkedBlockingQueue<String>)queue2);
	}
	
	public void offer(String url){
		queue2.offer(url);
		if (queue2.size() > 5000){
			pushFrontierQueueIntoDisk();
		}
	}
	
	public boolean isEmpty(){
		if (queue1.isEmpty()){
			if (db.isFrontierQueueDiskEmpty()){
				db.pushIntoFrontierQueueDisk(queue2);
			}
			db.pollFromFrontierQueueDisk(1000, queue1);
		}
		return queue1.isEmpty();
	}
	
	public String poll(){
		getFrontierQueueFromDisk();
		return queue1.poll();
	}
	
	
}
