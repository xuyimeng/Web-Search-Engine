package test.testAo;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * Created by gpzpati on 4/17/14.
 */
public class S3MapReduce  extends Configured implements Tool {

    Logger logger = LoggerFactory.getLogger(S3Mapper.class);

    public static class S3Mapper extends Mapper<LongWritable, Text, Text, IntWritable>{
        Logger logger = LoggerFactory.getLogger(S3Mapper.class);

        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();

        @Override
        protected void map(LongWritable key, Text value,
                           Context context)
                throws IOException, InterruptedException {
            logger.debug("Entering S3Mapper.map() " + this);
            String line = value.toString();
            StringTokenizer st = new StringTokenizer(line," ");
            while(st.hasMoreTokens()){
                word.set(st.nextToken());
                context.write(word,one);
            }
            logger.debug("Exiting S3Mapper.map()");
        }
    }

    public static class S3Reducer extends Reducer<Text, IntWritable, Text, IntWritable>{
        Logger logger = LoggerFactory.getLogger(S3Reducer.class);

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values,
                              Context context)
                throws IOException, InterruptedException {
            logger.debug("Entering S3Reducer.reduce() " + this);

            int sum = 0;
            Iterator<IntWritable> valuesIt = values.iterator();
            while(valuesIt.hasNext()){
                sum = sum + valuesIt.next().get();
            }
            logger.debug(key + " -> " + sum);
            context.write(key, new IntWritable(sum));
            logger.debug("Exiting S3Reducer.reduce()");
        }
    }

    public static void main(String[] argv)throws Exception{
        int exitCode = ToolRunner.run(new S3MapReduce(), argv);
        System.exit(exitCode);
    }

    @Override
    public int run(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.printf("Usage: %s [generic options] <input> <output>\n",
                    getClass().getSimpleName());
            ToolRunner.printGenericCommandUsage(System.err);
            return -1;
        }

        Job job = new org.apache.hadoop.mapreduce.Job();
        job.setJarByClass(S3MapReduce.class);
        job.setJobName("S3MapReduce");
        job.getConfiguration().set("fs.s3n.awsAccessKeyId", "AKIAJ6NFWWLQ3J6C47PA");
        job.getConfiguration().set("fs.s3n.awsSecretAccessKey","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");
        job.getConfiguration().set("fs.default.name","s3n://testtemplate1");

        System.out.println("Input path " + args[0]);
        System.out.println("Oupput path " + args[1]);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        job.setMapperClass(S3Mapper.class);
        job.setReducerClass(S3Reducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        job.setOutputFormatClass(MapFileOutputFormat.class);
        int returnValue = job.waitForCompletion(true) ? 0:1;
        System.out.println("job.isSuccessful " + job.isSuccessful());
        return returnValue;
    }

}