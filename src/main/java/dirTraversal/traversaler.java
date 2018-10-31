package dirTraversal;

import dirTraversal.model.Dir;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.BasicConfigurator;


import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;




/**
 * Created by zhou1 on 2018/10/26.
 * 从/开始，每遇到一个Dir就生成一个traversaler
 * 里面会记录父文件夹，子文件夹和所有的目录
 */
public class traversaler {
    private final static Configuration conf = new Configuration();
    private static String DestHdfs;
    private static FileSystem fs;
    private static taskQueue tq;
    private static sqlQueue sq;
    public FileStatus fStatus;
    // /节点的FileStatus是nil
    private traversaler parentNode;

    public static void initDestFs(String dest, taskQueue taskQueue, sqlQueue sqlQueue) throws Exception{
        DestHdfs = dest;
        fs = FileSystem.get(new URI(DestHdfs),conf);

        tq = taskQueue;
        sq = sqlQueue;
    }

    public traversaler(FileStatus fStatus, traversaler parentNode){
        this.fStatus = fStatus;
        this.parentNode = parentNode;
    }

//    public static traversaler generateTraversal(FileStatus fStatus, traversaler parentNode){
//        traversaler t = new traversaler(fStatus,parentNode);
//        if (!t.fStatus.isDirectory()){
//            return t;
//        }
//        try{
//            while (tq.size()>=80){
//                System.out.println(tq.size()+"this fails:" + t.getfStatus().getPath().toString());
//                Thread.yield();
//                TimeUnit.MILLISECONDS.sleep(500);
//            }
//            tq.put(t);
////            tq.put(new traversalTask(t));
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return t;
//    }

    private traversaler(Path selfPath){
        try{
            this.fStatus = fs.getFileStatus(selfPath);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static traversaler generateInitTraversal(Path selfPath){
        traversaler t = new traversaler(selfPath);
        try{
            while (tq.size()>=80){
                System.out.println(tq.size()+"this fails:" + t.getfStatus().getPath().toString());
                Thread.yield();
                TimeUnit.MILLISECONDS.sleep(500);
            }
            tq.put(t);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return t;
    }

    @Override
    public String toString() {
        return this.fStatus.getPath().toString();
    }

    public FileStatus getfStatus(){
        return this.fStatus;
    }

}


class testMain{
    public static void main(String[] args) throws Exception {

//        BasicConfigurator.configure(); //自动快速地使用缺省Log4j环境。

        ExecutorService exec = Executors.newCachedThreadPool();
        taskQueue tq = new taskQueue(100);
        sqlQueue sq = new sqlQueue(10);
        endNotifyQueue endQ = new endNotifyQueue(10);
        taskQueueRunner.setDestHdfs(args[0]);
        try{
            traversaler.initDestFs(args[0],tq,sq);
//            traversalTask.initDestFs(args[0],tq);
        }catch (Exception e){
            e.printStackTrace();
        }


        traversaler rt = traversaler.generateInitTraversal(new Path(args[1]));

        batisWrite.setQueue(sq,endQ);
        exec.execute(new batisWrite());
        for (int i =0;i<Integer.parseInt(args[2]);i++){
            exec.execute(new taskQueueRunner(tq,sq));
        }


//        System.out.println(endQ.isEmpty());
        TimeUnit.SECONDS.sleep(30);

        exec.shutdownNow();
    }
}