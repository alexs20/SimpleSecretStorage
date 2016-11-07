package com.wolandsoft.sss.storage;

public final class SecretEntryAttributeTable extends ATableDefinition {
    public final static String TBL_NAME = "secret_entry_attribute";
    public static final String FLD_ENTRY_UUID_MSB = "entry_uuid_msb";
    public static final String FLD_ENTRY_UUID_LSB = "entry_uuid_lsb";
    public static final String FLD_ORDER_ID = "order_id";
    public static final String FLD_KEY = "key";
    public static final String FLD_VALUE = "value";
    public static final String FLD_PROTECTED = "protected";

    @Override
    public String[] getCreateSQL() {
        return new String[]{"CREATE TABLE " + TBL_NAME + " (" +
                FLD_ENTRY_UUID_MSB + " INTEGER," +
                FLD_ENTRY_UUID_LSB + " INTEGER," +
                FLD_ORDER_ID + " INTEGER," +
                FLD_KEY + " TEXT," +
                FLD_VALUE + " TEXT," +
                FLD_PROTECTED + " INTEGER," +
                "PRIMARY KEY (" + FLD_ENTRY_UUID_MSB + ", " + FLD_ENTRY_UUID_LSB + ", " + FLD_KEY + "));",
                "CREATE INDEX search_idx ON " + TBL_NAME +
                        " (" + FLD_VALUE + "," + FLD_PROTECTED + ") WHERE " +
                        FLD_PROTECTED + "=0;"};
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
