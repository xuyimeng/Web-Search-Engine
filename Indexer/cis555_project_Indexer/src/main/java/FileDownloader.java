import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;


public class FileDownloader {
	
	private static String outputDir;
	public static List<String> urls;
	
	public static void main(String[] args){
		if(args.length < 1){
			System.out.println("FileDownloader need output directory as input argument");
		}
		outputDir = args[0];
		File outputFile = new File(outputDir);
		if(!outputFile.exists()){
			outputFile.mkdirs();
		}
		// hardcode url list first, later download from s3
		urls = Arrays.asList(new String[]{"www.google.com"});
		// Access S3 and write file into output directory 
		for(String url : urls){
			downloadFileFromS3(url);
		}
		
	}
	public static void writeOutputFile(InputStream in){
		//Read file from input stream
		InputStreamReader streamReader = new InputStreamReader(in);
		BufferedReader bufferedReader = new BufferedReader(streamReader);
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			while( (line = bufferedReader.readLine()) != null ) {
				sb.append(line + "\n");
			}
			//write content to file
			FileWriter fw = new FileWriter(outputDir + "/output.txt",true);
			BufferedWriter bw = new BufferedWriter(fw);
			System.out.println(sb.toString());
			bw.write(sb.toString());
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void downloadFileFromS3(String url){
		BasicAWSCredentials credentials = null;
        try {
            credentials = new BasicAWSCredentials("", "");
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
			
			writeOutputFile(in);
			
			ObjectMetadata meta = s3Object.getObjectMetadata();
			
		}catch(Exception e){
			
		}
	}
	
}
