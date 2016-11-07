package com.wolandsoft.sss.storage;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public abstract class ATableDefinition {

    public abstract String [] getCreateSQL();

    public abstract String [] getUpdateSQLs(int oneStepToVersion);
}
