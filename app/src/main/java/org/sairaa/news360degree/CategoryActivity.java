package org.sairaa.news360degree;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.arch.persistence.room.Database;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.sairaa.news360degree.R;
import org.sairaa.news360degree.api.ApiUtils;
import org.sairaa.news360degree.api.NewsApi;
import org.sairaa.news360degree.db.News;
import org.sairaa.news360degree.db.NewsDatabase;
import org.sairaa.news360degree.model.NewsData;
import org.sairaa.news360degree.model.NewsList;
import org.sairaa.news360degree.utils.CheckConnection;
import org.sairaa.news360degree.utils.CommonUtils;
import org.sairaa.news360degree.utils.DialogAction;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryActivity extends AppCompatActivity {
    private static final String APIKEY = "c19366b11c0440848041a33b1745e3d1";
    static DialogAction dialogAction;
    CommonUtils commonUtils;
    FloatingActionButton floatingActionButton;
    private RecyclerView recyclerView;
    private NewsAdapter adapter;
    private NewsViewModel viewModel;
    private CheckConnection checkConnection;
    private TextView emptyTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        //Initialize the declared variable
        init();

        Intent intent = getIntent();
        //get the category details from intent
        String category = intent.getStringExtra(getString(R.string.category));

        Toolbar toolbar = findViewById(R.id.toolbar_cat);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (getSupportActionBar() != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setTitle(category);
        }

        //if network is connected retrieve category news and insert it to Room
        if (checkConnection.isConnected()) {
            progressBar.setVisibility(View.VISIBLE);
            emptyTextView.setText(getString(R.string.loadingUi));
            new fatchAndInsertToDbAsyncTask().execute(category);
//            subscribeUi(adapter,commonUtils.getBookMark(category));
        } else{
            emptyTextView.setText(getString(R.string.network));
            Toast.makeText(this, getString(R.string.network), Toast.LENGTH_LONG).show();
        }

        //SetUp UI
        subscribeUi(adapter, commonUtils.getBookMark(category));


        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recyclerView.smoothScrollToPosition(0);

            }
        });
    }

    private void init() {
        progressBar = findViewById(R.id.progressBarCat);
        emptyTextView = findViewById(R.id.emptyCatTextView);
        checkConnection = new CheckConnection(this);
        floatingActionButton = findViewById(R.id.fab_cat);
        dialogAction = new DialogAction(this);
        commonUtils = new CommonUtils(this);
        recyclerView = findViewById(R.id.recyclerview_cat);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        viewModel = ViewModelProviders.of(this).get(NewsViewModel.class);

        adapter = new NewsAdapter(this);
    }

    private void subscribeUi(final NewsAdapter adapter, int bookMark) {

        viewModel.getNewsListLiveData(bookMark).observe(this, new Observer<PagedList<News>>() {
            @Override
            public void onChanged(@Nullable PagedList<News> news) {
                if(!news.isEmpty()){
                    adapter.submitList(null);
                    adapter.submitList(news);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    recyclerView.smoothScrollToPosition(0);
                    progressBar.setVisibility(View.INVISIBLE);
                    emptyTextView.setVisibility(View.INVISIBLE);
                }else
                    emptyTextView.setVisibility(View.VISIBLE);

            }
        });

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    //Retrieve categories news and insert it into room and notifies user on new news arrival
    private void insertNewsToDbCatogery(final Executor executor, final String category) {
        //get local country name
        String countryName = getApplicationContext().getResources().getConfiguration().locale.getDisplayCountry();
        //get the country code required for retrival of respective country news
        String countryCode = commonUtils.getCountryCode(countryName);
        //get instance of room database
        final NewsDatabase mDb = NewsDatabase.getsInstance(this);
        NewsApi newsApi = ApiUtils.getNewsApi();

        //make retrofit call to retrieve category news of the respective country
        newsApi.getTopHeadLineCategory(countryCode, category, APIKEY).enqueue(new Callback<NewsList>() {

            @Override
            public void onResponse(Call<NewsList> call, Response<NewsList> response) {
                //Retrieve snapshot of response from NewsAPI to newsList
                final NewsList newsList = response.body();
                //get the News object in "newsListData" that need to be used for our app from
                //the response we got.
                final List<NewsData> newsListData = newsList.getNewsDataList();
                //Loop the newsListData and check whether the news object exist or not in Room
                for (int i = 0; i < newsListData.size(); i++) {

                    final int position = i;
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {

                            /*check whether same news object available in Room Database or not
                            if not available insert that news object to Room Database*/

                            // The @param newL will have a news object, if the title from any object of newsListData matches.
                            List<News> newsL = mDb.newsDao().getSingleNews(newsListData.get(position).getTitle());
                            //if the @param newL does not contain any object, that means empty
                            //then the news object is not exist in Db. Then prepare the News object like Entity to insert in Room
                            if (newsL.isEmpty()) {
                                //Preparing News Entity to insert in Room
                                News news = new News(newsListData.get(position).getAuthor() == null ? getString(R.string.newsApi) : newsListData.get(position).getAuthor(),
                                        newsListData.get(position).getTitle() == null ? "" : newsListData.get(position).getTitle(),
                                        newsListData.get(position).getDescription() == null ? "" : newsListData.get(position).getDescription(),
                                        newsListData.get(position).getUrl() == null ? "" : newsListData.get(position).getUrl(),
                                        newsListData.get(position).getUrlToImage() == null ? "" : newsListData.get(position).getUrlToImage(),
                                        newsListData.get(position).getPublishedAt() == null ? "" : newsListData.get(position).getPublishedAt(),
                                        commonUtils.getBookMark(category));
                                try {
                                    //insert to room database
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
                dialogAction.hideDialog();
            }
        });
    }

    //insert to room database
    private void insertNewsToDbLocal(final News news, final NewsDatabase mDb) throws IOException {
        //format the date and time and set it to news object
        String dateTime = CommonUtils.getDate(news.getPublishedAt()).concat(", ").concat(CommonUtils.getTime(news.getPublishedAt()));
        news.setPublishedAt(dateTime);
        //inserting news in separate thread
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                mDb.newsDao().insert(news);
            }
        });
    }

    private class fatchAndInsertToDbAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialogAction.showDialog(getString(R.string.app_name), getString(R.string.retrieve));
        }

        @Override
        protected String doInBackground(String... category) {
            insertNewsToDbCatogery(Executors.newSingleThreadExecutor(), category[0]);
            return category[0];
        }

        @Override
        protected void onPostExecute(String category) {
            super.onPostExecute(category);

            subscribeUi(adapter, commonUtils.getBookMark(category));
            dialogAction.hideDialog();
        }
    }


}
