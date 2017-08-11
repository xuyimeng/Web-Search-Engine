package test;

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
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import org.apache.log4j.Logger;



public class AWSAccessor {
	static Logger log = Logger.getLogger(AWSAccessor.class);
	
	static void uploadFileToS3(Webpage webpage){
		
		//RobotCache.updateHostLastAccess(webpage.url);
		BasicAWSCredentials credentials = null;
        try {
//            credentials = new BasicAWSCredentials("AKIAIYEQC25NRZUS36DA", "URbRAsXQumJJjVt9eGvf/e9yHBxNfo7yXa2cirEy");
            //credentials = new BasicAWSCredentials("AKIAJ2ZWOURBZH3NHCXQ", "Ap/4xPjx5DIt/MamNLD7nN1KZsrGhlikq3tskHgb");
			credentials = new BasicAWSCredentials("AKIAJ6NFWWLQ3J6C47PA", "YNY4cU8hXZr1NKB6QtJL6k/6mSABPGf28q4e/lMU"); // mazhiyu key
            
        
        }
        catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (/Users/Felix/.aws/credentials), and is in valid format.",
                    e);
        }

        @SuppressWarnings("deprecation")
		AmazonS3 s3 = new AmazonS3Client(credentials);
//        Region usWest2 = Region.getRegion(Regions.US_EAST_2);
//        s3.setRegion(usWest2);
		
        // String bucketName = "cis455.indexer.output";
//        String bucketName = "testsunao1";
        String bucketName = "testsunao2";

		try{
			ByteArrayInputStream bstream = new ByteArrayInputStream(webpage.content);
			ObjectMetadata meta = new ObjectMetadata();
			meta.setContentLength(webpage.contentLength);
			meta.setContentType(webpage.contentType);
			meta.setLastModified(webpage.lastModifiedDate);
			//meta.setContentLanguage(webpage.language);
			
			String key = webpage.url;
			
			s3.putObject(new PutObjectRequest(bucketName, key, bstream, meta));

			//System.out.println("visited page is " + XPathCrawler.visited++);
			
		}
		catch (AmazonServiceException ase) {
            log.error("Caught an AmazonServiceException, which " +
            		"means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            log.error("Error Message:    " + ase.getMessage());
            log.error("HTTP Status Code: " + ase.getStatusCode());
            log.error("AWS Error Code:   " + ase.getErrorCode());
            log.error("Error Type:       " + ase.getErrorType());
            log.error("Request ID:       " + ase.getRequestId());
        }
        catch (AmazonClientException ace) {
            log.error("Caught an AmazonClientException, which " +
            		"means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            log.error("Error Message: " + ace.getMessage());
        }

		System.out.println(webpage.url + " has been successfully uploaded to S3");
		
	}
	
	@SuppressWarnings("deprecation")
	static Webpage downloadFileFromS3(String url) throws IOException{
		BasicAWSCredentials credentials = null;
        try {
        	credentials = new BasicAWSCredentials("AKIAIYEQC25NRZUS36DA", "URbRAsXQumJJjVt9eGvf/e9yHBxNfo7yXa2cirEy");
        	//credentials = new BasicAWSCredentials("AKIAJ2ZWOURBZH3NHCXQ", "Ap/4xPjx5DIt/MamNLD7nN1KZsrGhlikq3tskHgb");
            
        }
        catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (/Users/Felix/.aws/credentials), and is in valid format.",
                    e);
        }

        AmazonS3 s3 = new AmazonS3Client(credentials);
        Region usWest2 = Region.getRegion(Regions.US_EAST_2);
        s3.setRegion(usWest2);
        // String bucketName = "cis455.indexer.output";
        // String bucketName = "400badrequest";
        String bucketName = "testsunao1";

		try {
			String key = url;
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
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		catch (AmazonServiceException ase) {
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
        }
        catch (AmazonClientException ace) {
            log.error("Caught an AmazonClientException, which " +
            		"means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            log.error("Error Message: " + ace.getMessage());
        }
		return null;
		
	}
	
	@SuppressWarnings("unused")
	public static void s3iterator(){
		BasicAWSCredentials credentials = null;
        try {
        	credentials = new BasicAWSCredentials("AKIAIYEQC25NRZUS36DA", "URbRAsXQumJJjVt9eGvf/e9yHBxNfo7yXa2cirEy");
        	//credentials = new BasicAWSCredentials("AKIAJ2ZWOURBZH3NHCXQ", "Ap/4xPjx5DIt/MamNLD7nN1KZsrGhlikq3tskHgb");
            
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
        // String bucketName = "404notfound";
        String bucketName = "testsunao1";

        
        int i = 0;
        
        for (S3ObjectSummary summary : S3Objects.withPrefix(s3, bucketName, "") ) {
        	i++;
            //System.out.println("Object with key " + summary.getKey());
        }
        System.out.println(i);
	}
	
	@SuppressWarnings({ "deprecation", "unused" })
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException{
		
		//AWSAccessor.s3iterator();
		
		
		BasicAWSCredentials credentials = null;
        try {
//        	credentials = new BasicAWSCredentials("AKIAIYEQC25NRZUS36DA", "URbRAsXQumJJjVt9eGvf/e9yHBxNfo7yXa2cirEy");
			credentials = new BasicAWSCredentials("AKIAJ6NFWWLQ3J6C47PA", "YNY4cU8hXZr1NKB6QtJL6k/6mSABPGf28q4e/lMU"); // mazhiyu key
        }
        catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (/Users/Felix/.aws/credentials), and is in valid format.",
                    e);
        }
        
        //System.out.println(Regions.US_EAST_2.toString());
		
		AmazonS3Client s3 = new AmazonS3Client(credentials);
		
		Webpage webpage = new Webpage(new Date(), "http://www.google.com", "text/html", "<html> this is google </html>");
		Webpage webpage1 = new Webpage(new Date(), "http://www.google1.com", "text/html", "<html> this is google1 </html>");

		AWSAccessor.uploadFileToS3(webpage);
		AWSAccessor.uploadFileToS3(webpage1);
		
		//Webpage webpage1 = AWSAccessor.downloadFileFromS3("http://www.philly.com/philly/comics_games");
		//System.out.println(new String(webpage1.content, "UTF-8"));
		
//		Webpage webpage2 = AWSAccessor.downloadFileFromS3("http://www.google.com");
		
//		System.out.println(new String(webpage2.content, "UTF-8"));
		
//		try{
//		    PrintWriter writer = new PrintWriter("web.html", "UTF-8");
//		    writer.println(new String(webpage2.content));
//		    //writer.println("The second line");
//		    writer.close();
//		}
//		catch (IOException e) {
//		   // do something
//		}
		
		
		//System.out.println(new String(webpage2.content, "UTF-8"));
		
	}
}
