package com.juniverse.babylistener;

import android.util.Log;

public class BLDebugger
{
    public static final boolean _DEBUG = false;
    private final static String filter = "BabyListener";
    
    public static void printLog(String msg)
    {
        if ( _DEBUG )
        {
            Log.d(filter, msg);
        }
    }
}
