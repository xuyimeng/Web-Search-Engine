package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import edu.upenn.cis455.storage.Webpage;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;



public class AWSAccessor {
	static Logger log = Logger.getLogger(AWSAccessor.class);
	
	static void uploadFileToS3(Webpage webpage){
		
		BasicAWSCredentials credentials = null;
        try {
            credentials = new BasicAWSCredentials("key", "secret key");
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (/Users/Felix/.aws/credentials), and is in valid format.",
                    e);
        }

        @SuppressWarnings("deprecation")
		AmazonS3 s3 = new AmazonS3Client(credentials);
        Region usWest2 = Region.getRegion(Regions.US_EAST_2);
        s3.setRegion(usWest2);
		
        String bucketName = "404notfound";
		
		try{
			ByteArrayInputStream bstream = new ByteArrayInputStream(webpage.content);
			ObjectMetadata meta = new ObjectMetadata();
			meta.setContentLength(webpage.contentLength);
			meta.setContentType(webpage.contentType);
			meta.setLastModified(webpage.lastModifiedDate);
			meta.setContentLanguage(webpage.language);
			
			String key = DigestUtils.sha1Hex(webpage.url);
			
			s3.putObject(new PutObjectRequest(bucketName, key, bstream, meta));
			
			
		} catch (AmazonServiceException ase) {
            log.error("Caught an AmazonServiceException, which " +
            		"means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            log.error("Error Message:    " + ase.getMessage());
            log.error("HTTP Status Code: " + ase.getStatusCode());
            log.error("AWS Error Code:   " + ase.getErrorCode());
            log.error("Error Type:       " + ase.getErrorType());
            log.error("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            log.error("Caught an AmazonClientException, which " +
            		"means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            log.error("Error Message: " + ace.getMessage());
        }
		
	}
	
	@SuppressWarnings("deprecation")
	static Webpage downloadFileFromS3(String url) throws IOException{
		BasicAWSCredentials credentials = null;
        try {
            credentials = new BasicAWSCredentials("key", "secret key");
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (/Users/Felix/.aws/credentials), and is in valid format.",
                    e);
        }

        AmazonS3 s3 = new AmazonS3Client(credentials);
        Region usWest2 = Region.getRegion(Regions.US_EAST_2);
        s3.setRegion(usWest2);
        String bucketName = "404notfound";
        
		try {
			String key = DigestUtils.sha1Hex(url);
			S3Object s3Object = s3.getObject(new GetObjectRequest(bucketName, key));
			InputStream in = s3Object.getObjectContent();
			
			ObjectMetadata meta = s3Object.getObjectMetadata();
			
			
			
			InputStreamReader streamReader = new InputStreamReader(in);
			BufferedReader bufferedReader = new BufferedReader(streamReader);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while( (line = bufferedReader.readLine()) != null ) {
				sb.append(line + "\n");
			}
			
			
			long contentLength = meta.getContentLength();
			String contentType = meta.getContentType();
			String language = meta.getContentLanguage();
			Date date = meta.getLastModified();
			
			//System.out.println(contentLength);
			//System.out.println(contentType);
			//System.out.println(language);
			//System.out.println(date.toGMTString());
			
			try {
				Webpage webpage = new Webpage(date, url, contentType, sb.toString());
				webpage.contentLength = contentLength;
				webpage.language = language;
				return webpage;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (AmazonServiceException ase) {
			log.error("URL: "+url);
            log.error("Caught an AmazonServiceException, which " +
            		"means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            log.error("Error Message:    " + ase.getMessage());
            log.error("HTTP Status Code: " + ase.getStatusCode());
            log.error("AWS Error Code:   " + ase.getErrorCode());
            log.error("Error Type:       " + ase.getErrorType());
            log.error("Request ID:       " + ase.getRequestId());
            return null;
        } catch (AmazonClientException ace) {
            log.error("Caught an AmazonClientException, which " +
            		"means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            log.error("Error Message: " + ace.getMessage());
        }
		return null;
		
	}
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException{
		Date date = new Date();
		Webpage webpage = new Webpage(date, "www.google.com", "txt/html", "<html>this is google.</html>");
		webpage.language = "English";
		System.out.println(webpage.contentLength);
		System.out.println(webpage.contentType);
		System.out.println(webpage.language);
		System.out.println(new String(webpage.content, "UTF-8"));
		System.out.println(webpage.lastModifiedDate.toGMTString());
		
		System.out.println("+++++++++++");
		
		
		AWSAccessor.uploadFileToS3(webpage);
		Webpage webpage2 = AWSAccessor.downloadFileFromS3("www.google.com");
		System.out.println(webpage2.contentLength);
		System.out.println(webpage2.contentType);
		System.out.println(webpage2.language);
		System.out.println(webpage2.lastModifiedDate.toGMTString());
		System.out.println(new String(webpage2.content, "UTF-8"));
		
	}
}
