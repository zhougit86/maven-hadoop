package dirTraversal;

import org.apache.hadoop.fs.FileStatus;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import dirTraversal.model.User;
import dirTraversal.dao.UserDao;

import dirTraversal.model.Dir;
import dirTraversal.dao.DirDao;

import java.io.IOException;
import java.io.Reader;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * Created by zhou1 on 2018/10/30.
 */


public class batisWrite implements Runnable{
    private static SqlSessionFactory sqlSessionFactory;
    private static Reader reader;
    private static sqlQueue sq;
    private static SqlSession session ;
    private static DirDao dirMapper;

    static {
        try {
            reader = Resources.getResourceAsReader("mybatis-config.xml");
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        session = sqlSessionFactory.openSession();
        dirMapper = session.getMapper(DirDao.class);
    }

    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }
    public static void setQueue(sqlQueue sq1){
        sq = sq1;
    }
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Dir t = sq.take();
                try {
                    dirMapper.insertDir(t);
                    session.commit();
                } catch (Exception e) {
                    session.rollback();
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
//            System.out.println("Runner off");
//            e.printStackTrace();
            session.close();
        }
    }

    public static void main(String[] args){
//        System.out.println("hello world");
        //alias demo
//        Configuration con = sqlSessionFactory.getConfiguration();
//        Map<String, Class<?>> typeMap = con.getTypeAliasRegistry().getTypeAliases();
//        for(Map.Entry<String, Class<?>> entry: typeMap.entrySet()) {
//            System.out.println(entry.getKey() + " ================> " + entry.getValue().getName());
//        }


        try {

            Dir u = new Dir();
            u.setDir(false);
            u.setPath("13131");
            u.setMod_Time(new Timestamp(1539687654516L));
            dirMapper.insertDir(u);
            session.commit();

//            User user = userMapper.findUserById(2);
////            User user = (User)session.selectOne("selectUserByID", 2);
////            session.commit();
//            System.out.println(user.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.close();
        }

    }
}
