package com.wolandsoft.sss.storage;

public final class SecretEntryTable extends ATableDefinition {
    public final static String TBL_NAME = "secret_entry";
    public static final String FLD_ID = "id";
    public static final String FLD_CREATED = "created";
    public static final String FLD_UPDATED = "updated";

    @Override
    public String[] getCreateSQL() {
        return new String[]{"CREATE TABLE " + TBL_NAME + " (" +
                FLD_ID + " INTEGER PRIMARY KEY," +
                FLD_CREATED + " INTEGER," +
                FLD_UPDATED + " INTEGER);"};
    }

    @Override
    public String[] getUpdateSQLs(int oneStepToVersion) {
        switch (oneStepToVersion) {
            //case 2:
            //    return new String [] {"sql statement 1", "sql statement 2"};
            default:
                return new String[0];
        }
    }
}
