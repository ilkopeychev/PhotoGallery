package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

public class VisibleFragment extends android.support.v4.app.Fragment {
    private static final String TAG="VisibleFragment";

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter=new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        getActivity().registerReceiver(mOnShowNotification,filter, PollService.PERM_PRIVATE,null);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mOnShowNotification);
    }

    private BroadcastReceiver mOnShowNotification=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getActivity(),"God a broadcast" + intent.getAction(), Toast.LENGTH_LONG)
                    .show();
            setResultCode(Activity.RESULT_CANCELED);
        }
    };
}
