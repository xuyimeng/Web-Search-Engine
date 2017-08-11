package main.java.edu.upenn.cis555;

/**
 * Created by marcusma on 4/16/17.
 */

import com.google.common.collect.Iterables;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.*;
import org.apache.spark.storage.StorageLevel;
import scala.Serializable;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

public class PageRank{

	private static SparkConf conf;
	private static JavaSparkContext sc;

	public static class DoubleSum implements Function2<Double, Double, Double>{

		@Override
		public Double call(Double aDouble, Double aDouble2) throws Exception{
//			System.out.println("sum: " + (aDouble + aDouble2));
			return aDouble + aDouble2;
		}

	}

	public static class DoubleComparable implements Comparator<Tuple2<String, Double>>, Serializable{

		@Override
		public int compare(Tuple2<String, Double> o1, Tuple2<String, Double> o2){
			return Double.compare(o1._2(), o2._2());
		}
	}

	public PageRank(){

		conf = new SparkConf().setAppName("PageRank").setMaster("local");
		sc = new JavaSparkContext(conf);
		sc.hadoopConfiguration().set("fs.s3n.awsAccessKeyId", "AKIAJ6NFWWLQ3J6C47PA");
		sc.hadoopConfiguration().set("fs.s3n.awsSecretAccessKey", "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");

	}

	public static void main(String[] args){

		PageRank pr = new PageRank();

//		JavaRDD<String> lines = sc.textFile("/Users/marcusma/IdeaProjects/CIS555_Final_Project/new_page_test.txt");
		JavaRDD<String> lines = sc.textFile("/Users/marcusma/IdeaProjects/CIS555_Final_Project/pagerank_data.txt");
//		JavaRDD<String> lines = sc.textFile("s3n://mazhiyutest/new_page_test.txt");

		JavaRDD<String> nodesWithOutgoingLinks = lines.map(new Function<String, String>(){

			@Override
			public String call(String s) throws Exception{
				String returnedString = s.split("\\s+")[0];
				return returnedString;
			}

		}).distinct();

//		System.out.println("nodeSet content: " + nodesWithOutgoingLinks.takeOrdered((int)nodesWithOutgoingLinks.count()));

		JavaRDD<String> totalNodes = lines.flatMap(new FlatMapFunction<String, String>(){

			@Override
			public Iterator<String> call(String s) throws Exception{
				String[] nodes = s.split("\\s+");
				return Arrays.asList(nodes).iterator();
			}

		}).distinct().cache();

//		System.out.println("totalNode content: " + totalNodes.takeOrdered((int)totalNodes.count()));

		JavaRDD<String> nodesWithNoOutgoingLinks = totalNodes.subtract(nodesWithOutgoingLinks);

//		System.out.println("nodesWithoutOutgoingLinks content: " + nodesWithoutOutgoingLinks.takeOrdered((int)nodesWithoutOutgoingLinks.count()));

		JavaPairRDD<String, String> complementaryTuples = nodesWithNoOutgoingLinks.cartesian
				(totalNodes);

//		System.out.println("complementaryTuples: ");

//		complementaryTuples.foreach(new VoidFunction<Tuple2<String, String>>(){
//			@Override
//			public void call(Tuple2<String, String> stringStringTuple2) throws Exception{
//				System.out.println(stringStringTuple2._1() + stringStringTuple2._2());
//			}
//		});

		// inputLinkTuple, <1,2> means 1 points to 2;
		JavaPairRDD<String, String> inputLinkTuple = lines.flatMapToPair(new PairFlatMapFunction<String, String, String>(){
			@Override
			public Iterator<Tuple2<String, String>> call(String s) throws Exception{

				ArrayList<Tuple2<String, String>> returnTuples = new ArrayList<Tuple2<String, String>>();

				String[] tokens = s.split("\\s+");
				int count = 0;

				for(String temp : tokens){
//					if(count != 0){
						returnTuples.add(new Tuple2(tokens[0], temp));
//					}
//					else{
//						count++;
//					}
				}

				return returnTuples.iterator();
			}
		});

		JavaPairRDD<String, Iterable<String>> totalPair = inputLinkTuple.union(complementaryTuples).groupByKey().persist(StorageLevel.MEMORY_ONLY_SER());

		JavaPairRDD<String, Double> currentScores = totalPair.mapValues(new Function<Iterable<String>, Double>(){
			@Override
			public Double call(Iterable<String> strings) throws Exception{
				return 1.0;
			}
		}).cache();
		JavaPairRDD<String, Double> previousScores = currentScores.cache();

		int iterationTime = 0;

		while(iterationTime++ < 50){

			System.out.println("Iteration time: " + iterationTime);

			JavaPairRDD<String, Double> newAddedValue = totalPair.join(currentScores).values().flatMapToPair(new PairFlatMapFunction<Tuple2<Iterable<String>, Double>, String, Double>(){
				@Override
				public Iterator<Tuple2<String, Double>> call(Tuple2<Iterable<String>, Double> iterableDoubleTuple2) throws Exception{

					ArrayList<Tuple2<String, Double>> returnList = new ArrayList<Tuple2<String, Double>>();

					int countForLink = Iterables.size(iterableDoubleTuple2._1);

					for(String temp : iterableDoubleTuple2._1){
						returnList.add(new Tuple2(temp, iterableDoubleTuple2._2 / countForLink));
					}
					return returnList.iterator();
				}
			}).cache();
			previousScores = currentScores;
			currentScores = newAddedValue.reduceByKey(new DoubleSum()).mapValues(new Function<Double, Double>(){

				@Override
				public Double call(Double aDouble) throws Exception{
					return 0.15 + 0.85 * aDouble;
				}
			}).cache();

//			System.out.println("current_score: " + currentScores);

		}

//		System.out.println(previousScores.collect().toString());
		System.out.println(currentScores.collect().toString());

//		JavaRDD<Double> onlyScores = currentScores.map(new Function<Tuple2<String, Double>, Double>(){
//			@Override
//			public Double call(Tuple2<String, Double> stringDoubleTuple2) throws Exception{
//				return stringDoubleTuple2._2();
//			}
//		});

		Double maxScore = currentScores.max(new DoubleComparable())._2();
		System.out.println("Max: " + maxScore);

		JavaPairRDD<String, Double> pairwithMaxScore = totalNodes.mapToPair(new PairFunction<String, String, Double>(){
			@Override
			public Tuple2<String, Double> call(String s) throws Exception{
				return new Tuple2(s, maxScore);
			}
		}).distinct().cache();

		JavaPairRDD<String, Double> finalScore = currentScores.union(pairwithMaxScore).reduceByKey(new Function2<Double, Double, Double>(){
			@Override
			public Double call(Double aDouble, Double aDouble2) throws Exception{
				if(aDouble >= aDouble2){
					return aDouble2/aDouble;
				}
				else{
					return aDouble/aDouble2;
				}
			}
		});

		System.out.println("final score: " + finalScore.collect().toString());

//		finalScore.coalesce(1).saveAsTextFile("s3n://mazhiyutest/testoutput");

	}
}

//[(4,0.8048679294513235), (6,0.7061264004307011), (2,0.8048679294513235), (5,0.8048679294513235), (3,0.8048679294513235), (1,2.0744018817640053)]


//final score: [(6,0.31557353976073194), (3,0.6052076002814919), (4,0.6052076002814919), (1,1.0), (5,0.34799437016185786), (2,0.485573539760732)]

//		[(4,1.0808704777824556), (6,0.5635985445953333), (2,0.8672100344998773), (5,0.621500524724912), (3,1.0808704777824556), (1,1.7859499406149644)]
