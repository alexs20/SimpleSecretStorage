package com.wolandsoft.sss.storage.db;

public final class MasterPasswordTable extends ATableDefinition {
    public final static String TBL_NAME = "master_password";
    public static final String FLD_PASSWORD = "password";

    @Override
    public String[] getCreateSQL() {
        return new String[]{"CREATE TABLE " + TBL_NAME + " (" + FLD_PASSWORD + " TEXT PRIMARY KEY);"};
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
