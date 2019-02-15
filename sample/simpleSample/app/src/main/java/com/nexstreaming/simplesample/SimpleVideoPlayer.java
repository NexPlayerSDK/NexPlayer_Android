package com.nexstreaming.simplesample;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;


import android.widget.ImageButton;
import android.widget.SeekBar;


import com.nexstreaming.nexplayerengine.NexALFactory;
import com.nexstreaming.nexplayerengine.NexCaptionPainter;
import com.nexstreaming.nexplayerengine.NexClosedCaption;
import com.nexstreaming.nexplayerengine.NexContentInformation;
import com.nexstreaming.nexplayerengine.NexEventReceiver;
import com.nexstreaming.nexplayerengine.NexPlayer;
import com.nexstreaming.nexplayerengine.NexVideoRenderer;
import com.nexstreaming.nexplayerengine.NexVideoViewFactory;
import com.nexstreaming.nexplayerengine.NexWVDRM;

import java.io.File;

public class SimpleVideoPlayer extends AppCompatActivity {
    private static final String LOG_TAG = "NexPlayerSample";

    // Player Objects
    protected NexPlayer			mNexPlayer;
    protected NexALFactory mNexALFactory;
    private NexVideoViewFactory.INexVideoView mVideoView = null;
    private NexEventReceiver mEventReceiver;
    private NexContentInformation mContentInfo = null;

    // Widevine
    private NexWVDRM mNexWVDRM = null;

    // Subtitle View
    private NexCaptionPainter mCaptionPainter = null;

    public static final Handler mHandler = new Handler();
    private static final int SCALE_FIT_TO_SCREEN = 0;
    private static final int SCALE_ORIGINAL = 1;
    private static final int SCALE_STRETCH_TO_SCREEN = 2;

    private int mScaleMode = SCALE_FIT_TO_SCREEN;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    private String mContentURL = "";
    private int mDrmType = 0;
    private String mLicenseServer = "";

    // UI
    private ImageButton			mPlayPauseButton;
    private SeekBar mSeekBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_player);

        mContentURL = getIntent().getExtras().getString("ContentUrl", "");
        mDrmType = getIntent().getExtras().getInt("DrmType");
        if(mDrmType > 0) {
            mLicenseServer = getIntent().getExtras().getString("proxyUrl", "");
        }

        // Video view setting
        setVideoRendererView();
        // Subtitle view setting
        setCaptionPainter();

        //UI setting
        setPlayPauseButton();
        setSeekBarLayout();

        // Nexplayer setting
        if( initNexPlayer() < 0 ) {
            return;
        } else {
            setNexPlayerProperties();
        }

        if(mDrmType > 0) {
            setWidevineDRM();
        }

        // Video play contents
        startPlay(mContentURL);
    }

    @Override
    protected void onDestroy() {
        stopPlayer();
        releasePlayer();
        mVideoView.release();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if( mNexPlayer != null && mNexPlayer.isInitialized() ) {
            mPlayPauseButton.setBackgroundResource(android.R.drawable.ic_media_play);

            if( mNexPlayer.getState() >= NexPlayer.NEXPLAYER_STATE_STOP ) {
                mNexPlayer.stop();
            }
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        if( mNexPlayer != null && mNexPlayer.isInitialized() ) {
            mPlayPauseButton.setBackgroundResource(android.R.drawable.ic_media_pause);
            if( mNexPlayer.getState() == NexPlayer.NEXPLAYER_STATE_STOP ) {
                mNexPlayer.start(mSeekBar.getProgress());
            }
        }
        super.onResume();
    }

    private int initNexPlayer() {

        int debugLogLevel = 0x00000000;

        mNexPlayer = new NexPlayer();
        mNexALFactory = new NexALFactory();

        if( mNexALFactory.init(this, android.os.Build.MODEL, NexPlayer.NEX_DEVICE_USE_AUTO, debugLogLevel, 1 ) == false ) {
            Log.e(LOG_TAG, "ALFactory initialization failed");
            return -2;
        }

        mNexPlayer.setNexALFactory(mNexALFactory);
        if( mNexPlayer.init(this, 0) == false ) {
            Log.e(LOG_TAG,"NexPlayer initialization failed");
            return -3;
        }

        addEventReceiver();
        mVideoView.init(mNexPlayer);
        mVideoView.setVisibility(View.VISIBLE);
        return 0;
    }

    protected void setWidevineDRM() {
        mNexPlayer.setNexMediaDrmKeyServerUri(mLicenseServer);
        mNexWVDRM = new NexWVDRM();
        File fileDir = this.getFilesDir();
        String strCertPath = fileDir.getAbsolutePath() + "/wvcert";

        //String keyServer = "";
        int offlineMode = 0;
        String nexenginePath = getBaseContext().getApplicationInfo().dataDir + "/lib/libnexplayerengine.so";
        if(mNexWVDRM.initDRMManagerMulti(mNexPlayer, nexenginePath, strCertPath, mLicenseServer, offlineMode) == 0) {
            mNexWVDRM.enableWVDRMLogs(true);
        }
        mNexPlayer.setProperties(215, 3);
    }

    protected void setNexPlayerProperties() {
        mNexPlayer.setProperty(NexPlayer.NexProperty.DATA_INACTIVITY_TIMEOUT, 30000);
        mNexPlayer.setProperty(NexPlayer.NexProperty.PREFER_AV, 1);

        mNexPlayer.setDebugLogs(-1, -1, -1);
    }

    private void setVideoRendererView() {
        mVideoView = (NexVideoRenderer)findViewById(R.id.videoview);
        mVideoView.setListener(new NexVideoRenderer.IListener()
        {

            @Override
            public void onVideoSizeChanged()
            {
                Point videoSize = new Point();
                mVideoView.getVideoSize(videoSize);

                mVideoWidth = videoSize.x;
                mVideoHeight = videoSize.y;

                if (null != mContentInfo) {
                    if (90 == mContentInfo.mRotationDegree || 270 == mContentInfo.mRotationDegree) {
                        int rotationWidth = mVideoWidth;
                        mVideoWidth = mVideoHeight;
                        mVideoHeight = rotationWidth;
                    }
                }

                setPlayerOutputPosition(mVideoWidth, mVideoHeight, mScaleMode);
            }

            @Override
            public void onSizeChanged()
            {
                setPlayerOutputPosition(mVideoWidth, mVideoHeight, mScaleMode);
            }

            @Override
            public void onFirstVideoRenderCreate()
            {
                /* Initialization of mScaleMode has been done at the Activity Instance Initialization
                 * Not every time a new playback begins: NPDS-2404
                 */
                setPlayerOutputPosition(mVideoWidth, mVideoHeight, mScaleMode);
            }

            @Override
            public void onDisplayedRectChanged()
            {
            }
        });
        mVideoView.setPostNexPlayerVideoRendererListener(mEventReceiver);
    }

    private void setCaptionPainter() {
        mCaptionPainter = (NexCaptionPainter) findViewById(R.id.NexCaptionPainter);
    }

    void setPlayerOutputPosition(int videoWidth, int videoHeight, int scaleMode) {
        int width, height, top, left;
        width = height = top = left = 0;
        final int screenWidth = mVideoView.getWidth();
        final int screenHeight = mVideoView.getHeight();

        if (mVideoWidth == 0 && mVideoHeight == 0) //(mVideoWidth == 0 && mVideoHeight == 0) means Video Off state
            scaleMode = SCALE_STRETCH_TO_SCREEN;

        float scale = 1f;

        switch (scaleMode) {
            case SCALE_FIT_TO_SCREEN:
                scale = Math.min((float) screenWidth / (float) videoWidth,
                        (float) screenHeight / (float) videoHeight);

                width = (int) (videoWidth * scale);
                height = (int) (videoHeight * scale);
                top = (screenHeight - height) / 2;
                left = (screenWidth - width) / 2;

                break;
            case SCALE_ORIGINAL:
                width = videoWidth;
                height = videoHeight;
                top = (screenHeight - videoHeight) / 2;
                left = (screenWidth - videoWidth) / 2;

                break;
            case SCALE_STRETCH_TO_SCREEN:
                width = screenWidth;
                height = screenHeight;

                if(videoWidth != 0 && videoHeight != 0) {
                    scale = Math.min((float) screenWidth / (float) videoWidth,
                            (float) screenHeight / (float) videoHeight);
                }

                break;
        }

        mCaptionPainter.setRenderingArea(new Rect(left, top, left + width, top + height), scale);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mCaptionPainter.invalidate();
            }
        });

        if (mContentInfo != null && mContentInfo.mCurrVideoStreamID != NexPlayer.MEDIA_STREAM_DISABLE_ID) {
            mVideoView.setOutputPos(left, top, width, height);
        }
    }

    protected void addEventReceiver() {
        mEventReceiver = new NexEventReceiver() {
            @Override
            public void onAsyncCmdComplete(NexPlayer mp, int command, int result, int param1, int param2) {
                Log.d(LOG_TAG, "onAsyncCmdComplete : Command(" + command + "), Result(" + result + ")");
                switch (command) {
                    case NexPlayer.NEXPLAYER_ASYNC_CMD_OPEN_LOCAL:
                    case NexPlayer.NEXPLAYER_ASYNC_CMD_OPEN_STREAMING:
                        Log.d(LOG_TAG, "NEXPLAYER_ASYNC_CMD_OPEN_STREAMING - App");
                        if(result == 0) {
                            mContentInfo = mNexPlayer.getContentInfo();
                            mCaptionPainter.setCaptionType(mContentInfo.mCaptionType);
                            mNexPlayer.start( 0 );
                        }
                        else {
                            onError(mp, NexPlayer.NexErrorCode.fromIntegerValue(result));
                        }
                        break;

                    case NexPlayer.NEXPLAYER_ASYNC_CMD_START_STREAMING:
                        Log.d(LOG_TAG, "NEXPLAYER_ASYNC_CMD_START_STREAMING - App");
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                int duration = mNexPlayer.getContentInfoInt(NexPlayer.CONTENT_INFO_INDEX_MEDIA_DURATION);
                                mSeekBar.setMax(duration);
                                //mContentDurView.setText( getTimeToString(duration) );
                                }
                        });
                        break;
                    case NexPlayer.NEXPLAYER_ASYNC_CMD_STOP:
                        Log.d(LOG_TAG, "NEXPLAYER_ASYNC_CMD_STOP - App");
                        break;

                    case NexPlayer.NEXPLAYER_ASYNC_CMD_SEEK:
                        Log.d(LOG_TAG, "NEXPLAYER_ASYNC_CMD_SEEK - App");

                        break;
                }
            }

            @Override
            public void onStatusReport(NexPlayer mp, int msg, int param1) {
                if (msg == NexPlayer.NEXPLAYER_STATUS_REPORT_CONTENT_INFO_UPDATED) {
                    mContentInfo = mNexPlayer.getContentInfo();
                    int contentDuration = mContentInfo.mMediaDuration;
                }
            }

            @Override
            public void onEndOfContent(NexPlayer mp) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mNexPlayer.stop();
                    }
                });
            }

            @Override
            public void onTime(NexPlayer mp, int millisec) {
                showProgressBar(millisec);
            }

            @Override
            public void onError(NexPlayer mp, NexPlayer.NexErrorCode errorcode) {
                Log.e(LOG_TAG, "onError: " + errorcode);

                String lineBreak = "\n";
                if ("".equals(mp.getDetailedError())) {
                    lineBreak = "";
                }

                if( errorcode == null ) {
                    showErrorStatus("onError : Unknown Error Occured with Invalid errorcode object");
                }
                else {
                    switch (errorcode.getCategory()) {
                        case API:
                        case BASE:
                        case NO_ERROR:
                        case INTERNAL:
                            showErrorStatus("An internal error occurred while attempting to open the media: "
                                    + errorcode.name());
                            break;

                        case AUTH:
                            showErrorStatus("You are not authorized to view this content, "
                                    + "or it was not possible to verify your authorization, "
                                    + "for the following reason:\n\n" + errorcode.getDesc());
                            break;

                        case CONTENT_ERROR:
                            showErrorStatus("The content cannot be played back, probably because of an error in "
                                    + "the format of the content (0x"
                                    + Integer.toHexString(errorcode.getIntegerCode())
                                    + ": " + errorcode.name() + ").");
                            break;

                        case NETWORK:
                            showErrorStatus("The content cannot be played back because of a "
                                    + "problem with the network.  This may be temporary, "
                                    + "and trying again later may resolve the problem.\n("
                                    + errorcode.getDesc() + lineBreak + mp.getDetailedError() + ")");
                            break;

                        case NOT_SUPPORT:
                            showErrorStatus("The content can not be played back because it uses a "
                                    + "feature which is not supported by NexPlayer.\n\n("
                                    + errorcode.getDesc() + ")");
                            break;

                        case GENERAL:
                            showErrorStatus("The content cannot be played back for the following reason:\n\n"
                                    + errorcode.getDesc() + lineBreak + mp.getDetailedError());
                            break;

                        case PROTOCOL:
                            showErrorStatus("The content cannot be played back because of a "
                                    + "protocol error.  This may be due to a problem with "
                                    + "the network or a problem with the server you are "
                                    + "trying to access.  Trying again later may resolve "
                                    + "the problem.\n(" + errorcode.name()  + lineBreak + mp.getDetailedError() + ")");
                            break;
                        case DOWNLOADER:
                            showErrorStatus("Download has the problem\n\n(" + errorcode.name() + ")" );
                            break;

                        case SYSTEM:
                            showErrorStatus("SYSTEM has the problem\n\n(" + errorcode.name() + ")" );
                            break;
                    }
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int state = mNexPlayer.getState();

                        if ( state == NexPlayer.NEXPLAYER_STATE_PLAY
                                || state == NexPlayer.NEXPLAYER_STATE_PAUSE ) {
                            mNexPlayer.stop();
                        }
                        else if( state == NexPlayer.NEXPLAYER_STATE_STOP ){
                            closePlayer();
                        }
                        else if( state == NexPlayer.NEXPLAYER_STATE_CLOSED ) {

                        }
                    }
                });
            }

            @Override
            public void onVideoRenderCreate(NexPlayer mp, int width, int height, Object rgbBuffer) {
                super.onVideoRenderCreate(mp, width, height, rgbBuffer);
            }

            @Override
            public void onTextRenderRender(NexPlayer mp, int trackIndex, NexClosedCaption textInfo) {
                mCaptionPainter.setDataSource(textInfo);
            }
        };
        mNexPlayer.addEventReceiver(mEventReceiver);
    }


    private void startPlay(String url) {
        if (mNexPlayer.getState() == NexPlayer.NEXPLAYER_STATE_STOP){
            mNexPlayer.close();
        }
        if ( url.length() == 0 ) {
            Log.e(LOG_TAG,"Media URL/path not set for playback");
            return;
        }

        if( mVideoView.getVisibility() == View.INVISIBLE ) {
            mVideoView.setVisibility(View.VISIBLE);
        }
        //mErrorView.setVisibility(View.INVISIBLE);

        int ret = mNexPlayer.open(url, null, null,
                NexPlayer.NEXPLAYER_SOURCE_TYPE_STREAMING, NexPlayer.NEXPLAYER_TRANSPORT_TYPE_TCP);

        if( ret != 0 ) {
            mEventReceiver.onError(mNexPlayer, NexPlayer.NexErrorCode.fromIntegerValue(ret));
        }
    }

    private void stopPlayer () {
        if (mNexPlayer.getState() == NexPlayer.NEXPLAYER_STATE_PAUSE ||
                mNexPlayer.getState() == NexPlayer.NEXPLAYER_STATE_PLAY ) {
            int ret = mNexPlayer.stop();

            if(ret == 0 ) {
                try {
                    while(mNexPlayer.getState() !=NexPlayer.NEXPLAYER_STATE_STOP) {
                        Thread.sleep(100);
                    }
                }
                catch (InterruptedException e) { }
            }
        }
    }

    private void closePlayer() {
        mNexPlayer.close();
    }

    private void releasePlayer() {
        try {
            if (mNexPlayer != null) {
                if (mNexPlayer.getState() > NexPlayer.NEXPLAYER_STATE_CLOSED) {
                    mNexPlayer.close();
                }
                mNexPlayer.release();
                mNexALFactory.release();
            }
        }
        catch (Exception e) { }
    }


    //UI
    private void setPlayPauseButton() {
        mPlayPauseButton = (ImageButton)findViewById(R.id.play_pause_button);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mNexPlayer != null && mNexPlayer.isInitialized()) {

                    if(mNexPlayer.getState() == NexPlayer.NEXPLAYER_STATE_PLAY) {
                        mPlayPauseButton.setBackgroundResource(android.R.drawable.ic_media_play);
                        mNexPlayer.pause();
                    }
                    else if( mNexPlayer.getState() == NexPlayer.NEXPLAYER_STATE_PAUSE ) {
                        mPlayPauseButton.setBackgroundResource(android.R.drawable.ic_media_pause);
                        mNexPlayer.resume();
                    }
                    else if( mNexPlayer.getState() == NexPlayer.NEXPLAYER_STATE_STOP ) {
                        //startPlay(mContentURL );
                        mNexPlayer.start(0);
                    }
                    else if( mNexPlayer.getState() == NexPlayer.NEXPLAYER_STATE_CLOSED ) {
                        startPlay(mContentURL );
                    }
                }
            }
        });
    }

    private void setSeekBarLayout() {

        //mCurrentTimeView = (TextView)findViewById(R.id.currenttime);
        //mContentDurView = (TextView)findViewById(R.id.duration);

        mSeekBar = (SeekBar)findViewById(R.id.seek_layout);
        mSeekBar.setMax(0);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mNexPlayer.seek( seekBar.getProgress() ) == 0 ) {

                };
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                //mCurrentTimeView.setText(getTimeToString(progress));
            }
        });
    }

    private void showProgressBar(int millisec) {
        int progress = Math.max(millisec , 0);
        mSeekBar.setProgress(progress);
    }

    private void showErrorStatus(final String status) {
        Log.e(LOG_TAG, "error:" + status);
    }
}
