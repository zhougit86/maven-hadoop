package pqtRead;

import static java.lang.Thread.sleep;

import java.io.IOException;
import java.net.URI;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import compressFile.compressFile;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputCommitter;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import  org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.Mapper.Context;

import org.apache.parquet.Log;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.hadoop.ParquetInputFormat;
import org.apache.parquet.hadoop.ParquetOutputFormat;
import org.apache.parquet.hadoop.codec.CodecConfig;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.parquet.hadoop.example.GroupWriteSupport;
import org.apache.parquet.hadoop.example.ExampleInputFormat;
import org.apache.parquet.hadoop.example.ExampleOutputFormat;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.hadoop.util.ContextUtil;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import org.apache.parquet.schema.Type;

public class TestReadWriteParquet  extends Configured implements Tool {
    private static final Log LOG = Log.getLog(TestReadWriteParquet.class);
    private static final ConcurrentHashMap<Integer,String> map = new ConcurrentHashMap();

    public static class MyfileOutputFormat<T> extends ParquetOutputFormat<T> {
        private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance();

        private CompressionCodecName getCodec(TaskAttemptContext taskAttemptContext) {
            return CodecConfig.from(taskAttemptContext).getCodec();
        }

        public RecordWriter<Void, T> getRecordWriter(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
            Configuration conf = ContextUtil.getConfiguration(taskAttemptContext);
            CompressionCodecName codec = this.getCodec(taskAttemptContext);
            String extension = codec.getExtension() + ".parquet";
            Path file = this.getDefaultWorkFile(taskAttemptContext, extension);
            return this.getRecordWriter(conf, file, codec);
        }

        public static synchronized String getUniqueFile(TaskAttemptContext context, String name, String extension){
            TaskID taskId = context.getTaskAttemptID().getTaskID();
            int partition = taskId.getId();
//            String fileName =map.get(partition);
//            LOG.info(context)

            StringBuilder result = new StringBuilder();
            result.append(name);
            result.append("#$#");
            result.append(TaskID.getRepresentingCharacter(taskId.getTaskType()));
            result.append('$');
            result.append(NUMBER_FORMAT.format((long)partition));
            result.append(extension);
            return result.toString();
        }

        public Path getDefaultWorkFile(TaskAttemptContext context, String extension) throws IOException {
            FileOutputCommitter committer = (FileOutputCommitter)this.getOutputCommitter(context);
            return new Path(committer.getWorkPath(), getUniqueFile(context, getOutputName(context), extension));
        }
    }

//    public static class filePartitioner extends Partitioner<Text,Text> {
//        private static int fileNumber = 0;
//        public static HashMap<String,Integer> fileDist = new HashMap();
//        public static int initMap(FileStatus[] listStatus){
//            LOG.info("contains files:" + listStatus.length);
//
//            for (FileStatus f: listStatus){
////                System.out.println("partxiaoxiao"+f.getLen()+f);
//                if (f.getLen()==0){
//                    continue;
//                }
//                String TaskId = String.valueOf(fileNumber);
//
//                String[] sliceList = f.getPath().toString().split("/");
//                String lastString = sliceList[sliceList.length-1];
//                LOG.info(TaskId +"____"+lastString);
////                map.putIfAbsent(TaskId,lastString);
//
//                fileDist.put(f.getPath().toString(),fileNumber++);
//            }
//            return fileNumber;
//        }
//        @Override
//        public int getPartition(Text key,Text value,int numP){
//            Integer fileId = fileDist.get(key.toString());
//            return fileId==null?fileNumber:fileId;
//        }
//    }

    /*
     * Read a Parquet record, write a Parquet record
     */
    public static class ReadRequestMap extends Mapper<LongWritable, Group, Text, GroupWithFileName> {
        @Override
        protected void setup(Context context) throws IOException, InterruptedException{
            InputSplit inputSplit = context.getInputSplit();
            String fileName = ((FileSplit) inputSplit).getPath().toString();
            String[] sliceList = fileName.split("/");
            String lastString = sliceList[sliceList.length-1];
            int TaskId= context.getTaskAttemptID().getTaskID().getId();
            LOG.info(TaskId +"____"+lastString);
            map.putIfAbsent(TaskId,lastString);
        }

        @Override
        public void map(LongWritable key, Group value, Context context) throws IOException, InterruptedException {

            InputSplit inputSplit = context.getInputSplit();
            String fileName = ((FileSplit) inputSplit).getPath().toString();
            try{
//                context.write(new Text("aaa"), value);
                context.write(null, new GroupWithFileName(fileName,value));
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    public int run(String[] args) throws Exception {
        if(args.length < 3) {
            LOG.error("Usage: " + getClass().getName() + " INPUTFILE OUTPUTFILE [compression]");
            return 1;
        }
        String DestHdfs = args[0];
        String inputFile = args[1];
        String outputFile = args[2];
        String compression = (args.length > 3) ? args[3] : "none";

        //读取avro的metadata
        Path parquetFilePath = null;
        // Find a file in case a directory was passed
        FileSystem fileSys= FileSystem.get(new URI(DestHdfs),getConf());
        RemoteIterator<LocatedFileStatus> it = fileSys.listFiles(new Path(inputFile), true);

        ParquetMetadata readFooter;
        while(it.hasNext()) {
            FileStatus fs = it.next();
            if(fs.isFile()) {
                parquetFilePath = fs.getPath();
                LOG.info("Getting schema from " + parquetFilePath);

                try{
                    readFooter= ParquetFileReader.readFooter(getConf(), parquetFilePath);
                    MessageType schema = readFooter.getFileMetaData().getSchema();
                    LOG.info(schema);
                    GroupWriteSupport.setSchema(schema, getConf());
                    break;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        if(parquetFilePath == null) {
            LOG.error("No file found for " + inputFile);
            return 1;
        }




        //设置map的类和设置任务
        Job job = Job.getInstance(getConf(), "file Compress");
        job.setJarByClass(getClass());
        job.setJobName(getClass().getName());
        job.setMapperClass(ReadRequestMap.class);
        job.setNumReduceTasks(0);

        //设置partition
        FileSystem fs = FileSystem.get(new URI(DestHdfs),getConf());
        FileStatus[] listStatus = fs.listStatus( new Path(args[1]));
//        filePartitioner.initMap(listStatus);
//      设置有多少个reduce任务  job.setNumReduceTasks(filePartitioner.initMap(listStatus));
//        job.setPartitionerClass(filePartitioner.class);
        //设置输入输出格式
        ParquetInputFormat.setReadSupportClass(job, GroupReadSupport.class);
        job.setInputFormatClass(ParquetInputFormat.class);
//        GroupWriteSupport.setSchema(schema, getConf());
//        ParquetOutputFormat.setValidation(getConf(),false);
//        ParquetOutputFormat.setEnableDictionary(job,false);
        MyfileOutputFormat.setWriteSupportClass(job, MySupport.class);
        job.setOutputFormatClass(MyfileOutputFormat.class);

        //设置压缩格式
        CompressionCodecName codec = CompressionCodecName.UNCOMPRESSED;
        if(compression.equalsIgnoreCase("snappy")) {
            codec = CompressionCodecName.SNAPPY;
        } else if(compression.equalsIgnoreCase("gzip")) {
            codec = CompressionCodecName.GZIP;
        }
        LOG.info("Output compression: " + codec);
        MyfileOutputFormat.setCompression(job, codec);

        //设置输入输出的路径
        FileInputFormat.setInputPaths(job, new Path(DestHdfs+inputFile));
        if (fileSys.exists(new Path(DestHdfs+outputFile))){
            LOG.info("the output already Exists");
            fileSys.delete(new Path(DestHdfs+outputFile),true);
        }
        FileOutputFormat.setOutputPath(job, new Path(DestHdfs+outputFile));

        job.waitForCompletion(true);

        return 0;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf= new Configuration();
        conf.set("mapred.textoutputformat.ignoreseparator", "true");
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        try {
            int res = ToolRunner.run(conf, new TestReadWriteParquet(), args);
            System.exit(res);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(255);
        }
    }
}

