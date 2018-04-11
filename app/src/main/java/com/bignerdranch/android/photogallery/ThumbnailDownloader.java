package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.session.MediaSession;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T>extends HandlerThread {
    private static final String TAG="ThumbDownloader";
    private static final int MESSAGE_DOWNLOAD=0;
 //   private static final int MESSAGE_PRELOAD=1;
  //  private static final int CACHE_SIZE=400;

    private Handler mRequestHandler;  //handler


    private ConcurrentMap<T, String> mRequestMap=new ConcurrentHashMap<>();


    private ThumbnailDownloader<T> mThumbnailDownloader;

    private Handler mResponseHandler;
    // Listener
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

// LruCache<String, Bitmap> mCache;



    public interface ThumbnailDownloadListener<T> {
        void onThummbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setTThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
mThumbnailDownloadListener=listener;
    }

    public ThumbnailDownloader(Handler responseHandler){
        super(TAG);
        mResponseHandler=responseHandler;
//  mCache=new LruCache<String, Bitmap>(CACHE_SIZE);
    }

    public void queueThumbnail(T target, String url){
        Log.i(TAG, "Got a URL"+url);
        if (url==null){
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target,url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target).sendToTarget();
        }
    }

    public void clearQueque(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    private void handleRequest(final T target){

        try {
            final String url=mRequestMap.get(target);

            if (url==null){
                return;
            }
            byte[] bitmapBytes=new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap= BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target)!=url){
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThummbnailDownloaded(target,bitmap);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler=new Handler(){
            @Override
            public void handleMessage(Message msg) {

                if(msg.what==MESSAGE_DOWNLOAD){
                    T target=(T)msg.obj;
                    handleRequest(target);
                }
            }
        };
    }

}
