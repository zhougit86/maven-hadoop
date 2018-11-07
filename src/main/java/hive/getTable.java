package hive;

import hive.TBLS.persistence.dao.sdsMapper;
import hive.TBLS.persistence.dao.tableMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import pqtRead.TestReadWriteParquet;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

public class getTable{
    public static void main(String[] args) throws IOException{
        SqlSessionFactory sqlSessionFactory;
        Reader reader;
        SqlSession session ;
        tableMapper tableM;
        sdsMapper sDsM;

        reader = Resources.getResourceAsReader("mybatis-config.xml");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader,"tbds");
        session = sqlSessionFactory.openSession();
        tableM = session.getMapper(tableMapper.class);
        sDsM = session.getMapper(sdsMapper.class);
        System.out.println("sql init ok");

        Long sID = tableM.selectByTableName("inv_onway_dly_fct").getSdId();
        String wholeLocation = sDsM.selectByPrimaryKey(sID).getLocation();
        String location = wholeLocation.substring(wholeLocation.indexOf("hdfsCluster")+"hdfsCluster".length());
        System.out.println(location);

//        location+="/sdt=20160101";
        String[] compressArg = new String[]{"hdfs://10.1.53.205:8020", location, "/tmp/mrzip"+location ,"gzip" };

        Configuration conf= new Configuration();
        conf.set("mapred.textoutputformat.ignoreseparator", "true");
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        conf.set("mapreduce.framework.name","yarn");
//        conf.set("yarn.resourcemanager.address", "10.1.53.205:8032");
        conf.set("fs.defaultFS", "hdfs://10.1.53.205:8020");

        try {
            int res = ToolRunner.run(conf, new TestReadWriteParquet(), compressArg);
            System.out.println("the runner end:"+res);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("the runner exception");
        }
        for(Map.Entry<String, String> entry: TestReadWriteParquet.map.entrySet()) {
            System.out.println(entry);
        }


    }
}


