package dirTraversal;

import dirTraversal.model.Dir;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;

import java.net.URI;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

class taskQueueRunner implements Runnable{
    private taskQueue finishedQueue;
    private sqlQueue sQueue;
    private int counter = 0;
    private static int classId = 1;
    private int RunnerId = classId++;
    private Queue<traversaler> localCache = new LinkedList<traversaler>();
    private traversaler currentHandlingT;

    private final static Configuration conf = new Configuration();
    private static String DestHdfs;
    private static FileSystem fs;

    public static void setDestHdfs(String dest)throws Exception{
        DestHdfs = dest;
        fs = FileSystem.get(new URI(DestHdfs),conf);
    }

    public taskQueueRunner(taskQueue tq,sqlQueue sq) {
        finishedQueue = tq;
        sQueue = sq;
    }
    public void run(){
        System.out.println("i am :"+ this);
        try{
            while (!Thread.interrupted()) {
                TimeUnit.SECONDS.sleep(1);
                if (currentHandlingT!=null){
                    try{
                        FileStatus[] listStatus = fs.listStatus(this.currentHandlingT.fStatus.getPath());
                        for (FileStatus f: listStatus){
                            traversaler tTemp = finishedQueue.poll();
                            System.out.println("poll from queue" + this + tTemp);
                            if (tTemp !=null){
                                localCache.offer(tTemp);
                            }
                            String Path = f.getPath().toString();
//                            System.out.println("running"+Path);
                            Dir ddd = Dir.newDirFromFileStatus(f);
                            while (sQueue.size()>=5){
                                System.out.println("put sql fails");
                                Thread.yield();
                                TimeUnit.MILLISECONDS.sleep(500);
                            }
                            sQueue.put(ddd);



                            traversaler t = new traversaler(f,currentHandlingT);
                            if (!t.fStatus.isDirectory()){
                                continue;
                            }
                            try{
                                while (finishedQueue.size()>=80){
                                    System.out.println(finishedQueue.size()+"this fails:" + t.getfStatus().getPath().toString());
                                    Thread.yield();
                                    TimeUnit.MILLISECONDS.sleep(500);
                                }
                                finishedQueue.put(t);
//            tq.put(new traversalTask(t));
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else {
                    currentHandlingT = localCache.poll();
                    System.out.println("take from cache" + this);
                    if (currentHandlingT==null){
                        traversaler t = finishedQueue.take();
                        System.out.println("take from queue" + this + t);
                        localCache.offer(t);
                    }
                }
            }
        }catch (InterruptedException e){
//            System.out.println("Runner off");
            e.printStackTrace();
        }

    }
}