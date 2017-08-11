import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Locale;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/*
 * webpage is responsible for storing webpage info
 */

@Entity
public class Webpage {
	
	@PrimaryKey
	public String url;
	
	public long lastModifiedTime;
	public Date lastModifiedDate;
	
	public String contentType;
	//public String content;
	public byte[] content;
	
	public long contentLength;
	public Locale locale;
	public String language;
	
	public String SHA256CheckSum;
	
	
	public Webpage(){
		
	}
	
	/**
	 * @param lastAccessTime
	 * @param url
	 * @param contentType
	 * @param content
	 * @throws NoSuchAlgorithmException 
	 */
	public Webpage(long lastAccessTime, String url, String contentType, String content) throws NoSuchAlgorithmException{
		this.lastModifiedTime = lastAccessTime;
		this.lastModifiedDate = new Date(this.lastModifiedTime);
		this.url = url;
		this.contentType = contentType;
		this.content = content.getBytes();
		this.contentLength = content.length();
		
		this.SHA256CheckSum = Webpage.getCheckSum(content);
	}
	
	
	
	/**
	 * @param lastAccessDate
	 * @param url
	 * @param contentType
	 * @param content
	 * @throws NoSuchAlgorithmException 
	 */
	public Webpage(Date lastAccessDate, String url, String contentType, String content) throws NoSuchAlgorithmException{
		this.lastModifiedDate = lastAccessDate;
		this.lastModifiedTime = this.lastModifiedDate.getTime();
		this.url = url;
		this.contentType = contentType;
		this.content = content.getBytes();
		this.contentLength = content.length();
		this.SHA256CheckSum = Webpage.getCheckSum(content);
		
	}
	
	public static String getCheckSum(String content) throws NoSuchAlgorithmException{
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(content.getBytes());
		
		byte byteData[] = md.digest();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < byteData.length; i++){
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}
		
		return sb.toString();
	}
	
	/**
	 * @return last access time as a long
	 */
	public long getLastModifiedTimeLong(){
		return lastModifiedTime;
	}
	
	/**
	 * @return last access time as a date object
	 */
	public Date getLastModifiedTimeDate(){
		return lastModifiedDate;
	}
	
	/**
	 * @return url
	 */
	public String getURL(){
		return url;
	}
	
	/**
	 * @return content type
	 */
	public String getContentType(){
		return contentType;
	}
	
	/**
	 * @return content
	 */
	public String getContent(){
		return new String(content);
	}
	
	public long getContentLength(){
		return contentLength;
	}
	
	public static void main(String args[]) throws NoSuchAlgorithmException{
		String check = Webpage.getCheckSum("abcde");
		System.out.println(check);
	}
	
}
