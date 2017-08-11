package edu.upenn.cis455.storage;

import java.util.LinkedList;
import java.util.Queue;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class FrontierQueueDisk {			// not necessary using concurrent bc each node has a queue

	@PrimaryKey
	String queueName;
	
	Queue<String> queue;
	
	public FrontierQueueDisk(){
		this.queue = new LinkedList<String>();
		this.queueName = "FrontierQueueDisk";
	}
	
	public boolean isEmpty(){
		return queue.isEmpty();
	}
	
	public int size(){
		return queue.size();
	}
	
	public String poll(){
		if (queue.isEmpty()) return null;
		return queue.poll();
	}
	
	public boolean offer(String url){
		boolean res = queue.offer(url);
		return res;
	}
	
	public String peek(){
		return queue.peek();
	}
	
}
