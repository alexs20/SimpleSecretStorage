/*
    Copyright 2016 Alexander Shulgin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package com.wolandsoft.sss.storage;

/**
 * Secret entry entity table definition.
 *
 * @author Alexander Shulgin
 */
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
