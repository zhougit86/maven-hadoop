package hive.TBLS.persistence.dao;

import hive.TBLS.model.syncLog;

public interface syncLogMapper {
    int deleteByPrimaryKey(Integer syncId);

    int insert(syncLog record);

    int insertSelective(syncLog record);

    syncLog selectByPrimaryKey(Integer syncId);

    int updateByPrimaryKeySelective(syncLog record);

    int updateByPrimaryKey(syncLog record);
}