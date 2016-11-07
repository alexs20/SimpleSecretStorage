package com.wolandsoft.sss.storage.db;

public final class SecretEntryTable extends ATableDefinition {
    public final static String TBL_NAME = "secret_entry";
    public static final String FLD_UUID_MSB = "uuid_msb";
    public static final String FLD_UUID_LSB = "uuid_lsb";
    public static final String FLD_CREATED = "created";
    public static final String FLD_UPDATED = "updated";

    @Override
    public String[] getCreateSQL() {
        return new String[]{"CREATE TABLE " + TBL_NAME + " (" +
                FLD_UUID_MSB + " INTEGER," +
                FLD_UUID_LSB + " INTEGER," +
                FLD_CREATED + " INTEGER," +
                FLD_UPDATED + " INTEGER," +
                "PRIMARY KEY (" + FLD_UUID_MSB + ", " + FLD_UUID_LSB + "));"};
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
