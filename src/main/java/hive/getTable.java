package hive;

import hive.TBLS.persistence.dao.sdsMapper;
import hive.TBLS.persistence.dao.tableMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.Reader;

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
        System.out.println(sDsM.selectByPrimaryKey(sID).getLocation());


    }
}


