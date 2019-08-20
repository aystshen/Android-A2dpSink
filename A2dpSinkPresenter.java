package com.ayst.sample.items.a2dpsink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.util.Log;

public class A2dpSinkPresenter {
    private static final String TAG = "A2dpSinkPresenter";

    private static final String ACTION_TRACK_EVENT =
            "android.bluetooth.avrcp-controller.profile.action.TRACK_EVENT";
    public static final String EXTRA_METADATA =
            "android.bluetooth.avrcp-controller.profile.extra.METADATA";
    public static final String EXTRA_PLAYBACK =
            "android.bluetooth.avrcp-controller.profile.extra.PLAYBACK";

    private Context mContext;
    private IA2dpSinkView mA2dpSinkView;

    public A2dpSinkPresenter(Context context, IA2dpSinkView view) {
        mContext = context;
        mA2dpSinkView = view;
    }

    public void start() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TRACK_EVENT);
        mContext.registerReceiver(mBtReceiver, filter);
    }

    public void stop() {
        mContext.unregisterReceiver(mBtReceiver);
    }

    private BroadcastReceiver mBtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive intent=" + intent);
            String action = intent.getAction();

            if (ACTION_TRACK_EVENT.equals(action)) {
                PlaybackState pbb = intent.getParcelableExtra(EXTRA_PLAYBACK);
                MediaMetadata mmd = intent.getParcelableExtra(EXTRA_METADATA);
                if (null != mmd) {
                    mA2dpSinkView.updateA2dpSinkMediaInfo(mmd);
                }
            }
        }
    };
}
