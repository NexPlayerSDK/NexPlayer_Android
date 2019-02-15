package com.nexstreaming.simplesample;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.nexstreaming.nexplayerengine.NexSystemInfo;

public class MainActivity extends AppCompatActivity {

    public static class ContentInfo {
        public String contentUrl;
        public boolean widevineDrm;
        public String proxyServer;

        public ContentInfo(String url, boolean drm, String proxyUrl) {
            contentUrl = url;
            widevineDrm = drm;
            proxyServer = proxyUrl;
        }
    }

    public static final ContentInfo[] mContentList =  {
            new ContentInfo(
                    "http://amssamples.streaming.mediaservices.windows.net/683f7e47-bd83-4427-b0a3-26a6c4547782/BigBuckBunny.ism/manifest(format=mpd-time-csf)",
                    false,
                    ""),
            new ContentInfo(
                    "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd",
                    true,
                    "https://proxy.uat.widevine.com/proxy"),
    };

    private PermissionManager mPermissionManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(NexSystemInfo.getPlatformInfo() >= NexSystemInfo.NEX_SUPPORT_PLATFORM_MARSHMALLOW) {
            mPermissionManager = new PermissionManager(this);
            mPermissionManager.setPermissionFlags(
                    PermissionManager.REQUEST_STORAGE |
                    PermissionManager.REQUEST_LOCATION|
                    PermissionManager.REQUEST_PHONE_STATE);
            mPermissionManager.requestPermissions();
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        EditText editURL = (EditText) findViewById(R.id.editNormal);
        editURL.setText(mContentList[0].contentUrl);
        EditText editDRM = (EditText) findViewById(R.id.editDRM);
        editDRM.setText(mContentList[1].contentUrl);


        Button normalPlay = (Button) findViewById(R.id.btnNormalPlay);
        normalPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getBaseContext(), SimpleVideoPlayer.class);
                EditText editURL = (EditText) findViewById(R.id.editNormal);
                String normalUrl = editURL.getText().toString();
                intent.putExtra("ContentUrl", normalUrl);
                intent.putExtra("DrmType", 0);
                startActivity(intent);
            }
        });

        Button drmPlay = (Button) findViewById(R.id.btnDRMPlay);
        drmPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getBaseContext(), SimpleVideoPlayer.class);
                EditText editURL = (EditText) findViewById(R.id.editDRM);
                String drmlUrl = editURL.getText().toString();
                intent.putExtra("ContentUrl", drmlUrl);
                intent.putExtra("DrmType", 1);
                intent.putExtra("proxyUrl", mContentList[1].proxyServer);

                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if( mPermissionManager != null )
            mPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
