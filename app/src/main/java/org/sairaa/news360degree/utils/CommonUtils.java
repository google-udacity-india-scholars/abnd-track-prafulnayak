package org.sairaa.news360degree.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import org.sairaa.news360degree.MainActivity;
import org.sairaa.news360degree.R;
import org.sairaa.news360degree.api.ApiUtils;
import org.sairaa.news360degree.api.NewsApi;
import org.sairaa.news360degree.db.News;
import org.sairaa.news360degree.db.NewsDatabase;
import org.sairaa.news360degree.model.NewsData;
import org.sairaa.news360degree.model.NewsList;
import org.sairaa.news360degree.service.BackgroundService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommonUtils {
    private Context context;


    public CommonUtils(Context context) {
        this.context = context;
    }

    public static String getDate(String dateString) {

        try {
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
            Date date = format1.parse(dateString);
            DateFormat sdf = new SimpleDateFormat("MMM d yyyy");
            return sdf.format(date);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "xx";
        }
    }

    public static String getTime(String dateString) {

        try {
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
            Date date = format1.parse(dateString);
            DateFormat sdf = new SimpleDateFormat("h:mm a");
            Date netDate = (date);
            return sdf.format(netDate);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "xx";
        }
    }

    public static void showNotification(Context myService, String s) {
        int uniqueInteger = 0;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(myService, s);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.news360);
            builder.setLargeIcon(BitmapFactory.decodeResource(myService.getResources(), R.drawable.news360));
            builder.setColor(myService.getResources().getColor(R.color.colorAccent));
        } else {
            builder.setSmallIcon(R.drawable.news360);
        }
        builder.setContentTitle(myService.getString(R.string.app_name));
        builder.setContentText(s);
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(uri);
        builder.setAutoCancel(true);
        Intent intent = new Intent(myService, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(myService);
        stackBuilder.addNextIntent(intent);
        uniqueInteger = (int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) myService.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(uniqueInteger, builder.build());

    }

    //Retrieve top headline news and insert it into room and notifies user on new news arrival
    public void fetchTopHeadlineAndInsertToDb(final Executor executor, final String apiKey) {
        //get local country name
        String countryName = context.getResources().getConfiguration().locale.getDisplayCountry();

        //get the country code for retrival of news in respective country
        String countryCode = getCountryCode(countryName);

        final NewsDatabase mDb = NewsDatabase.getsInstance(context);
        NewsApi newsApi = ApiUtils.getNewsApi();

        //make retrofit call to retrieve category news of the respective country
        newsApi.getTopHeadLine(countryCode, apiKey).enqueue(new Callback<NewsList>() {
            @Override
            public void onResponse(Call<NewsList> call, Response<NewsList> response) {
                //Retrieve snapshot of response from News API to newsList
                final NewsList newsList = response.body();
                //get the News object in "newsListData" that need to be used for our app from
                //the response we got.
                final List<NewsData> newsListData = newsList.getNewsDataList();
                //Loop the newsListData and check whether the news object exist in Room or not
                for (int i = 0; i < newsList.getNewsDataList().size(); i++) {
                    final int position = i;
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {

                            //check whether same news object available or not
                            //if not available insert that news object to room database

                            // The @param newL will have a news object, if the title from any object of newsListData matches.
                            List<News> newsL = mDb.newsDao().getSingleNews(newsList.getNewsDataList().get(position).getTitle());
                            //if the @param newL does not contain any object, that means empty
                            //then the news object is not exist in Db. Then prepare the News object like Entity to insert in Room
                            if (newsL.isEmpty()) {
                                //Preparing News Entity to insert in Room
                                News news = new News(newsListData.get(position).getAuthor() == null ? context.getString(R.string.newsApi) : newsListData.get(position).getAuthor(),
                                        newsListData.get(position).getTitle() == null ? "" : newsListData.get(position).getTitle(),
                                        newsListData.get(position).getDescription() == null ? "" : newsListData.get(position).getDescription(),
                                        newsListData.get(position).getUrl() == null ? "" : newsListData.get(position).getUrl(),
                                        newsListData.get(position).getUrlToImage() == null ? "" : newsListData.get(position).getUrlToImage(),
                                        newsListData.get(position).getPublishedAt() == null ? "" : newsListData.get(position).getPublishedAt(),
                                        1);//1 for Top Headline
                                try {
                                    //insert news object to room
                                    insertNewsToDbLocal(news, mDb);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<NewsList> call, Throwable t) {

            }
        });
    }
    //insert new news object to Room and display notification if the operation ran in background service
    private void insertNewsToDbLocal(final News news, final NewsDatabase mDb) throws IOException {
        //format the date and time and set it to news object
        String dateTime = CommonUtils.getDate(news.getPublishedAt()).concat(" ").concat(CommonUtils.getTime(news.getPublishedAt()));
        news.setPublishedAt(dateTime);
        //inserting news in separate thread
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                mDb.newsDao().insert(news);
                //if data is inserted from background service, notify user
                if (context.getClass().getName().equals(context.getString(R.string.backgroundServiceName))) {
                    if (!news.getTitle().isEmpty())
                        CommonUtils.showNotification(context, news.getTitle());

                }
            }
        });
    }

    public String getCountryCode(String countryName) {
        if (countryName.equals(context.getString(R.string.india))) {
            return context.getString(R.string.india_code);
        }else if(countryName.equals(context.getString(R.string.usa))){
            return context.getString(R.string.us_code);
        }else if(countryName.equals(context.getString(R.string.south_africa))){
            return context.getString(R.string.sa_code);
        }else if(countryName.equals(context.getString(R.string.united_kingdom))){
            return context.getString(R.string.uk_code);
        }
        return context.getString(R.string.india_code);
    }

    public int getBookMark(String category) {
        if (category.equals(context.getString(R.string.business_cat))) {
            return 2; // 2 in bookmark coulmn in db is to retrieve business
        } else if (category.equals(context.getString(R.string.entertainment_cat))) {
            return 3; // 3 in bookmark coulmn in db is to retrieve entettainment
        } else if (category.equals(context.getString(R.string.health_cat))) {
            return 4; // 4 in bookmark coulmn in db is to retrieve health
        } else if (category.equals(context.getString(R.string.science_cat))) {
            return 5;  // 5 in bookmark coulmn in db is to retrieve science
        } else if (category.equals(context.getString(R.string.sports_cat))) {
            return 6; // 6 in bookmark coulmn in db is to retrieve sports
        } else if (category.equals(context.getString(R.string.technology_cat))) {
            return 7; // 7 in bookmark coulmn in db is to retrieve Technology
        } else
            return 1; // 1 in bookmark coulmn in db is to retrieve Top Headline News
    }
}
