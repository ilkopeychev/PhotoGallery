package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {
private MenuItem mSearchItem;

    private static final String TAG="PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems=new ArrayList<>();

    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private ProgressBar mProgressBar;


    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateItems();
       PollService.setServiceAlarm(getActivity(),true);

        Handler responseHadler=new Handler();

        mThumbnailDownloader=new ThumbnailDownloader<>(responseHadler);
        mThumbnailDownloader.setTThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThummbnailDownloaded(PhotoHolder target, Bitmap bitmap) {
                Drawable drawable=new BitmapDrawable(getResources(),bitmap);
                target.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Backgound thread started");
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.fragment_photo_gallery,container,false);

        mPhotoRecyclerView=(RecyclerView)v.findViewById(R.id.fragmet_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));

        mProgressBar=(ProgressBar)v.findViewById(R.id.fragment_progress_bar);
        showProgressBar(true);
        setupAdapter();
        return v;
    }

    public void showProgressBar(boolean isShow){
        if(isShow){
            mProgressBar.setVisibility(View.VISIBLE);
            mPhotoRecyclerView.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mPhotoRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueque();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG,"Background thread destroyed");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery,menu);

        mSearchItem=menu.findItem(R.id.menu_item_search);
        final SearchView searchView=(SearchView)mSearchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "Query text submit "+s);
                QueryPreferences.setStoredQuery(getActivity(), s);

                collapseSearchView();
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "QueryTextChange" + s);
                return false;
            }

        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query=QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query,false );
            }
        });
        MenuItem toogleItem=menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())){
            toogleItem.setTitle(R.string.stop_polling);
        } else {
            toogleItem.setTitle(R.string.start_polling);
        }
    }

    private void collapseSearchView(){
        mSearchItem.collapseActionView();
        View view=getActivity().getCurrentFocus();
        if (view!=null){
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(),null  );
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm=!PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(),shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
                default:
                    return super.onOptionsItemSelected(item);
        }
    }

    private void setupAdapter() {
        if (isAdded()){
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }
    private void updateItems() {
        String query=QueryPreferences.getStoredQuery(getActivity());

        if (mPhotoRecyclerView!=null){
            mPhotoRecyclerView.setAdapter(null);
        }


        new FetchItemsTask(query,this).execute();
    }
    private class FetchItemsTask extends AsyncTask<Void,Void, List<GalleryItem>>{
        private String mQuery;
        private PhotoGalleryFragment mGalleryFragment;


        public FetchItemsTask(String query, PhotoGalleryFragment fragment){
            mQuery=query;
            mGalleryFragment=fragment;
        }
        @Override
        protected List<GalleryItem> doInBackground(Void... voids) {
         String query="robot";
         if (mQuery==null){
             return new FlickrFetchr().fetchRecentPhotos();
         }else {
             return new FlickrFetchr().searchPhotos(mQuery);
         }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mGalleryFragment.isResumed()){
                mGalleryFragment.showProgressBar(true);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mItems=galleryItems;
            setupAdapter();
            mGalleryFragment.showProgressBar(false);
        }
    }

    private class   PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView){
            super(itemView);

           mItemImageView=(ImageView)itemView
                   .findViewById(R.id.fragmet_photo_gallery_image_view);
                 itemView.setOnClickListener(this);

        }
        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }
        public void bindGalleryItem(GalleryItem galleryItem){
            mGalleryItem=galleryItem;
        }

        @Override
        public void onClick(View v) {
            Intent i=PhotoPageActivity.newIntent(getActivity(),mGalleryItem.getPhotoPageUri());
            startActivity(i);
        }
    }
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems=galleryItems;
        }

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater=LayoutInflater.from(getActivity());
            View view=inflater.inflate(R.layout.gallery_item,parent,false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
            GalleryItem galleryItem=mGalleryItems.get(position);
            holder.bindGalleryItem(galleryItem);
           Drawable placeholder=getResources().getDrawable(R.drawable.bill_up_close);
           holder.bindDrawable(placeholder);
           mThumbnailDownloader.queueThumbnail(holder,galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
           return mGalleryItems.size();
        }
    }
}
