package org.sairaa.news360degree.service;

import android.content.Context;

import org.sairaa.news360degree.utils.CommonUtils;
import org.sairaa.news360degree.R;
import org.sairaa.news360degree.api.ApiUtils;
import org.sairaa.news360degree.api.NewsApi;
import org.sairaa.news360degree.db.News;
import org.sairaa.news360degree.db.NewsDatabase;
import org.sairaa.news360degree.model.NewsList;

import java.util.List;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BackGroundUtils {
    private static final String APIKEY = "c19366b11c0440848041a33b1745e3d1";
    Context context;
    private CommonUtils commonUtils;

    public BackGroundUtils(Context context) {
        this.context = context;
        commonUtils = new CommonUtils(context);
    }

    //fetch latest news and insert it into room and notifies user on new news arrival
    public void fatchLatestNews() {
        commonUtils.fetchTopHeadlineAndInsertToDb(Executors.newSingleThreadExecutor(), APIKEY);

    }

}
