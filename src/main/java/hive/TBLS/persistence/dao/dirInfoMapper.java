package hive.TBLS.persistence.dao;

import hive.TBLS.model.dirInfo;

public interface dirInfoMapper {
    int deleteByPrimaryKey(Long id);

    int insert(dirInfo record);

    int insertSelective(dirInfo record);

    dirInfo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(dirInfo record);

    int updateByPrimaryKey(dirInfo record);
}