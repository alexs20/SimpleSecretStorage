package com.wolandsoft.sss.storage;

public final class SecretEntryAttributeTable extends ATableDefinition {
    public final static String TBL_NAME = "secret_entry_attribute";
    public static final String FLD_ENTRY_ID = "entry_id";
    public static final String FLD_ORDER_ID = "order_id";
    public static final String FLD_KEY = "key";
    public static final String FLD_VALUE = "value";
    public static final String FLD_PROTECTED_VALUE = "protected_value";

    @Override
    public String[] getCreateSQL() {
        return new String[]{
                "CREATE TABLE " + TBL_NAME + " (" +
                        FLD_ENTRY_ID + " INTEGER," +
                        FLD_ORDER_ID + " INTEGER," +
                        FLD_KEY + " TEXT," +
                        FLD_VALUE + " TEXT," +
                        FLD_PROTECTED_VALUE + " TEXT," +
                        "PRIMARY KEY (" + FLD_ENTRY_ID + ", " + FLD_ORDER_ID + "));",
                "CREATE INDEX search_idx ON " + TBL_NAME +
                        " (" + FLD_ORDER_ID + "," + FLD_VALUE + ");",
                };
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
