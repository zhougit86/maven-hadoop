package jmulti;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhou1 on 2018/11/4.
 */

//第四章的核心就是要保持不变性（final）。当无法保证不变性的时候则需要加锁，和LocalThread等等方法。
//限制变量的作用域，比如私有变量，局部变量
//有些容器是线程安全的，比如：Vector, 而有些是不安全的，如：List。Collections.synchronizedList的作用是把本身不是线程安全的容器变成线程安全的


//Immutable是不可变，所有的基础变量都用final修饰起来。ConcurrentHashMap可以保护map所有操作都是原子的
//unmodifiableMap是不可变的Map


//委托只有在多个独立的变量，彼此之间没有关系的时候才能成立

//发布变量的条件！1，线程安全，2，没有不变性约束，3，不存在不允许转换的状态


//Syncmap的话是每个操作都加锁了，而concurrmap的话则是使用了一种lock strip的东西（类似于一种特殊的读写锁）
//尽量使用Concurentmap


//dequeue可以处理既是生产者又是消费者的方法
//抛出InterruptedException的方法都是会导致阻塞的方法，拿到这个异常一般两种处理throw,Thread.currentThread().interrupt()
//

public class localT extends Thread {
    private static int globalId;
    private static CountDownLatch cl;
    private static ThreadLocal<Integer> safeInt
        =new ThreadLocal<Integer>(){
            public Integer initialValue(){
                return new Integer(9);
            }
    };

    private int myId = globalId++;

    public static void setCDL(CountDownLatch cl1){
        cl = cl1;
    }

    @Override
    public String toString(){
        StringBuffer sBuffer = new StringBuffer();
        sBuffer.append(safeInt.get());
        sBuffer.append(" __ i am #");
        sBuffer.append(myId);

        return sBuffer.toString();
    }

    @Override
    public void run(){
        int count =0;
        safeInt.set(safeInt.get()+myId);
        while (!interrupted()){
            try{
                TimeUnit.SECONDS.sleep(1);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            if (count++==3){
                synchronized (cl){
                    if(myId<3){
                        System.out.printf("Thread#%d is done\n",myId);
                        cl.countDown();
                    }
                }
                return;
            }
            System.out.println(this);
        }
    }

    public static void main(String[] args){
        CountDownLatch latch = new CountDownLatch(3);
        ExecutorService exec = Executors.newCachedThreadPool();
        localT.setCDL(latch);

        for(int i =0;i<3;i++){
            exec.execute(new localT());
        }

        try {
            latch.await();
            exec.shutdownNow();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
