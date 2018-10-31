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

class taskQueue extends LinkedBlockingQueue<traversalTask> {
    public taskQueue(int i){
        super(i);
    }
    public taskQueue(){
        super();
    }
}


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

    private FileStatus fStatus;
    // /节点的FileStatus是nil
    private traversaler parentNode;
    private volatile ArrayList<traversaler> kidNodes;

    public static void initDestFs(String dest, taskQueue taskQueue, sqlQueue sqlQueue) throws Exception{
        DestHdfs = dest;
        fs = FileSystem.get(new URI(DestHdfs),conf);

        tq = taskQueue;
        sq = sqlQueue;
    }

    public void generateChild(){
        try{
            FileStatus[] listStatus = fs.listStatus(this.fStatus.getPath());
            for (FileStatus f: listStatus){
                String Path = f.getPath().toString();
                System.out.println("running"+Path);
                Dir ddd = Dir.newDirFromFileStatus(f);
                while (sq.size()>=5){
                    System.out.println("put sql fails");
                    Thread.yield();
                    TimeUnit.MILLISECONDS.sleep(500);
                }
                sq.put(ddd);
                generateTraversal(f,this);
//                try{
//                    sq.put(ddd);
//                }catch (InterruptedException e){
//                    System.out.println("put dir fails");
//                    TimeUnit.SECONDS.sleep(3);
//                    sq.put(ddd);
//                }

//                System.out.println(ddd);
//                System.out.println(Path.substring(Path.indexOf(DestHdfs)+DestHdfs.length()));
//                System.out.println( new Date(f.getModificationTime()) );
//                System.out.println(f.getModificationTime());
//                System.out.println(f.getPath().getParent()+"---"+f.getPath().getName());
//                System.out.println(this.kidNodes);
//                synchronized (this.kidNodes){
//                    this.kidNodes.add(generateTraversal(f,this));
//                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private traversaler(){
        this.kidNodes = new ArrayList();
    }

    private traversaler(FileStatus fStatus, traversaler parentNode){
//        this(fStatus,parentNode);
        this();
        this.fStatus = fStatus;
        this.parentNode = parentNode;
    }

    public static traversaler generateTraversal(FileStatus fStatus, traversaler parentNode){
        traversaler t = new traversaler(fStatus,parentNode);
        if (!t.fStatus.isDirectory()){
            return t;
        }
        try{
            while (tq.size()>=900){
                System.out.println(tq.size()+"this fails:" + t.getfStatus().getPath().toString());
                Thread.yield();
                TimeUnit.MILLISECONDS.sleep(500);
            }
            tq.put(new traversalTask(t));
//            tq.put(new traversalTask(t));
        }catch (Exception e){
            e.printStackTrace();
        }
        return t;
    }

    private traversaler(Path selfPath){
        this();
        try{
            this.fStatus = fs.getFileStatus(selfPath);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public static traversaler generateTraversal(Path selfPath){
        traversaler t = new traversaler(selfPath);
        try{
            while (tq.size()>=900){
                System.out.println(tq.size()+"this fails:" + t.getfStatus().getPath().toString());
                Thread.yield();
                TimeUnit.MILLISECONDS.sleep(500);
            }
            tq.put(new traversalTask(t));
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

class traversalTask{
    private traversaler currentT;


    public traversalTask(traversaler t){
        currentT = t;
    }
    public FileStatus run(){
        currentT.generateChild();
        return currentT.getfStatus();
    }
}

class taskQueueRunner implements Runnable{
    private taskQueue finishedQueue;
    private int counter = 0;
    private static int classId = 1;
    private int RunnerId = classId++;

    private static String destHdfs;

    public static void setDestHdfs(String dest){
        destHdfs = dest;
    }

    public taskQueueRunner(taskQueue tq) {
        finishedQueue = tq;
    }
    public void run(){
        System.out.println("i am :"+ this);
        try{
            while (!Thread.interrupted()) {
                traversalTask t = finishedQueue.take();
                counter++;
                System.out.println(this.toString()+":" + counter);
                t.run();
//                System.out.printf("counter: %d __",counter);
//                System.out.printf("# by Id: %d __",RunnerId);
//                System.out.printf("the time is %s __", Time.now());
//                System.out.println(dir);
            }
        }catch (InterruptedException e){
//            System.out.println("Runner off");
            e.printStackTrace();
        }

    }
}


class testMain{
    public static void main(String[] args) throws Exception {

//        BasicConfigurator.configure(); //自动快速地使用缺省Log4j环境。

        ExecutorService exec = Executors.newCachedThreadPool();
        taskQueue tq = new taskQueue(1000);
        sqlQueue sq = new sqlQueue(10);
        endNotifyQueue endQ = new endNotifyQueue(10);
        taskQueueRunner.setDestHdfs(args[0]);
        try{
            traversaler.initDestFs(args[0],tq,sq);
//            traversalTask.initDestFs(args[0],tq);
        }catch (Exception e){
            e.printStackTrace();
        }
        batisWrite.setQueue(sq,endQ);

        traversaler rt = traversaler.generateTraversal(new Path(args[1]));
        exec.execute(new batisWrite());

        for (int i =0;i<Integer.parseInt(args[2]);i++){
            exec.execute(new taskQueueRunner(tq));
        }


//        System.out.println(endQ.isEmpty());
        TimeUnit.SECONDS.sleep(30);

        exec.shutdownNow();
    }
}