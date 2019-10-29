package com.ashessin.cs441.hw2.dblp.mr;

import com.ashessin.cs441.hw2.dblp.utils.PublicationWritable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

public class SimpleCount extends Configured implements Tool {
    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new SimpleCount(), args);
        System.exit(res);
    }

    /**
     * Execute the command with the given arguments.
     *
     * @param args command specific arguments.
     * @return exit code.
     * @throws Exception
     */
    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = super.getConf();

        final String HDFS = "hdfs://localhost:9000";
        Path inputFile = new Path(new URI(HDFS + args[0]));
        Path outputPath = new Path(new URI(HDFS + args[1]));
        DblpMapper.setRequiredField(args[2].toLowerCase());

        FileSystem hdfs = FileSystem.get(URI.create(HDFS), conf);
        // delete existing directory
        if (hdfs.exists(outputPath)) {
            hdfs.delete(outputPath, true);
        }

        Job job = Job.getInstance(conf, "Dblp Simple Count");
        job.setJarByClass(SimpleCount.class);
        job.setMapperClass(DblpMapper.class);
        job.setReducerClass(IntSumReducer.class);
        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.setInputPaths(job, inputFile);
        FileOutputFormat.setOutputPath(job, outputPath);

        if (job.waitForCompletion(true)) {
            return 0;
        }
        return 1;
    }

    public static class DblpMapper extends Mapper<Text, PublicationWritable, Text, IntWritable> {

        private static final IntWritable ONE = new IntWritable(1);
        private static String requiredField;
        private Text tag = new Text();

        static void setRequiredField(String fieldKey) {
            requiredField = fieldKey;
        }

        static boolean getRequiredFieldType(String fieldKey) {
            return !fieldKey.matches("authors|editors|urls|ees|cites|schools");
        }

        /**
         * Called once for each key/value pair in the input split.
         *
         * @param key
         * @param publication
         * @param context
         */
        @Override
        protected void map(Text key, PublicationWritable publication, Context context) throws IOException, InterruptedException {
            String methodName = "get" + requiredField.substring(0, 1).toUpperCase() + requiredField.substring(1);
            if (getRequiredFieldType(requiredField)) {
                try {
                    String keyString = String.valueOf(getMethod(publication, methodName).invoke(publication));
                    tag.set(new Text(keyString));
                    context.write(tag, ONE);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    List<String> keyStrings = (List<String>) getMethod(publication, methodName).invoke(publication);
                    for (String keyString : keyStrings) {
                        tag.set(new Text(keyString));
                        context.write(tag, ONE);
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

        }

        Method getMethod(PublicationWritable pw, String methodName) throws NoSuchMethodException {
            return pw.getClass().getMethod(methodName);
        }
    }
}