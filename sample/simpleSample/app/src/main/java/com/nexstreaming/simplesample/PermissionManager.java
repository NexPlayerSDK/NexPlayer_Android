package com.nexstreaming.simplesample;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.M)
public class PermissionManager {
    private static final String LOG_TAG = "PermissionManager";
    private Activity mActivity = null;

    public static final int REQUEST_STORAGE = 1;
    public static final int REQUEST_LOCATION = 2;
    public static final int REQUEST_PHONE_STATE = 4;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static String[] PERMISSIONS_PHONE_STATE = {Manifest.permission.READ_PHONE_STATE};
    private static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_COARSE_LOCATION};

    private int mPermissionFlag = 0;

    public PermissionManager(@NonNull Activity activity) {
        mActivity = activity;
    }

    public void setPermissionFlags(int flag) {
        mPermissionFlag = flag;
        Log.d(LOG_TAG, "setPermissionFlags flag : " + flag);
    }

    public synchronized void requestPermissions() {
        requestPermission(getNextRequestCode(mPermissionFlag));
    }

    private String[] getPermissionsFromRequestCode(int requestCode) {
        String[] permissions = null;
        switch (requestCode) {
            case REQUEST_STORAGE:
                permissions = PERMISSIONS_STORAGE;
                break;
            case REQUEST_PHONE_STATE:
                permissions = PERMISSIONS_PHONE_STATE;
                break;
            case REQUEST_LOCATION:
                permissions = PERMISSIONS_LOCATION;
                break;
        }

        return permissions;
    }

    private void requestPermission(int requestCode) {
        String[] permissions = getPermissionsFromRequestCode(requestCode);
        Log.d(LOG_TAG, "requestPermission permissions : "+ permissions);
        if( permissions != null )
            mActivity.requestPermissions(permissions, requestCode);
    }

    private int getNextRequestCode(int flag) {
        Log.d(LOG_TAG, "getNextRequestCode flag : "+ flag);
        if( flag > 0 ) {
            int requestCode = flag;

            if ( (flag - REQUEST_PHONE_STATE) >= 0 )
                requestCode = REQUEST_PHONE_STATE;
            else if( (flag - REQUEST_LOCATION) >= 0 )
                requestCode = REQUEST_LOCATION;
            else if( (flag - REQUEST_STORAGE) >= 0 )
                requestCode = REQUEST_STORAGE;
            Log.d(LOG_TAG, "getNextRequestCode requestCode : "+ requestCode);
            return requestCode;
        } else
            return flag;
    }

    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(LOG_TAG, "requestCode:"+requestCode);

        for(int i=0; i<permissions.length; i++) {
            Log.d(LOG_TAG, "    permission["+i+"] "+ permissions[i]);
        }

        boolean grant = true;
        for(int i=0; i<grantResults.length; i++) {
            Log.d(LOG_TAG, "    grantResults["+i+"] "+ grantResults[i]);
            if( grantResults[i] == PackageManager.PERMISSION_DENIED ) {
                grant = false;
            }
        }

        if( grant )
            mPermissionFlag -= requestCode;
        if( mPermissionFlag == 0 )
            return true;
        else {
            requestPermission(getNextRequestCode(mPermissionFlag));
            return false;
        }
    }
}
