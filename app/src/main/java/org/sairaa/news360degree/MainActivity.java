package org.sairaa.news360degree;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.sairaa.news360degree.db.News;
import org.sairaa.news360degree.service.ServiceUtils;
import org.sairaa.news360degree.utils.CheckConnection;
import org.sairaa.news360degree.utils.CommonUtils;
import org.sairaa.news360degree.utils.DialogAction;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String APIKEY = "c19366b11c0440848041a33b1745e3d1";//"079dac74a5f94ebdb990ecf61c8854b7";
    FloatingActionButton floatingActionButton;
    private RecyclerView recyclerView;
    private NewsAdapter adapter;
    private NewsViewModel viewModel;
    private CheckConnection checkConnection;
    private CommonUtils commonUtils;
    private DrawerLayout mDrawerLayout;
    private DialogAction dialogAction;
    private TextView emptyTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        //if network is connected retrieve top headline news and insert it to room
        if (checkConnection.isConnected()) {
            progressBar.setVisibility(View.VISIBLE);
            emptyTextView.setText(getString(R.string.loadingUi));
            new fatchAndInsertToDbAsyncTask().execute();
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            emptyTextView.setText(getString(R.string.network));
            subscribeUi(adapter, 1);
        }

        //navigation drawer
        //start an intent to get category news details
        setUpNavigationDrawer();


        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recyclerView.smoothScrollToPosition(0);

            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    floatingActionButton.show();
                }
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 || dy < 0 && floatingActionButton.isShown())
                    floatingActionButton.hide();
            }
        });

    }

    private void init() {
        progressBar = findViewById(R.id.progressBarMain);
        emptyTextView  = findViewById(R.id.emptyViewText);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        dialogAction = new DialogAction(this);
        checkConnection = new CheckConnection(this);
        floatingActionButton = findViewById(R.id.floatingActionButton2);
        commonUtils = new CommonUtils(this);
        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        //viewmodel
        viewModel = ViewModelProviders.of(this).get(NewsViewModel.class);

        adapter = new NewsAdapter(this);
    }

    private class fatchAndInsertToDbAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialogAction.showDialog(getString(R.string.app_name), getString(R.string.retrieve));
        }

        @Override
        protected Void doInBackground(Void... voids) {
            commonUtils.fetchTopHeadlineAndInsertToDb(Executors.newSingleThreadExecutor(), APIKEY);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            subscribeUi(adapter, 1);
            dialogAction.hideDialog();
        }
    }

    //set UI
    private void subscribeUi(final NewsAdapter adapter, int bookMark) {
        //observe if the data gets changed and notify the UI
        viewModel.getNewsListLiveData(bookMark).observe(this, new Observer<PagedList<News>>() {
            @Override
            public void onChanged(@Nullable PagedList<News> news) {

                if(!news.isEmpty()){
                    //cleare the adapter
                    adapter.submitList(null);
                    //submit news list to adapter
                    adapter.submitList(news);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    //move to top of the recycler view
//                recyclerView.smoothScrollToPosition(0);
                    progressBar.setVisibility(View.INVISIBLE);
                    emptyTextView.setVisibility(View.INVISIBLE);

                }else
                    emptyTextView.setVisibility(View.VISIBLE);

            }
        });
    }

    private void setUpNavigationDrawer() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                // set item as selected to persist highlight
                menuItem.setChecked(true);
                // close drawer when item is tapped
                mDrawerLayout.closeDrawers();
                Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
                switch (menuItem.getItemId()) {
                    case R.id.nav_business:
                        intent.putExtra(getString(R.string.category), getString(R.string.business_cat));
                        startActivity(intent);
                        break;
                    case R.id.nav_entertainment:
                        intent.putExtra(getString(R.string.category), getString(R.string.entertainment_cat));
                        startActivity(intent);
                        break;
                    case R.id.nav_health:
                        intent.putExtra(getString(R.string.category), getString(R.string.health_cat));
                        startActivity(intent);
                        break;
                    case R.id.nav_science:
                        intent.putExtra(getString(R.string.category), getString(R.string.science_cat));
                        startActivity(intent);
                        break;
                    case R.id.nav_sports:
                        intent.putExtra(getString(R.string.category), getString(R.string.sports_cat));
                        startActivity(intent);
                        break;
                    case R.id.nav_technology:
                        intent.putExtra(getString(R.string.category), getString(R.string.technology_cat));
                        startActivity(intent);
                        break;
                }
                return true;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            // Respond to a click on the "Insert data" menu option
            case R.id.refresh_info:
                if (checkConnection.isConnected()) {
                    progressBar.setVisibility(View.VISIBLE);
                    emptyTextView.setText(getString(R.string.loadingUi));
                    new fatchAndInsertToDbAsyncTask().execute();

                } else
                    Toast.makeText(this, getString(R.string.network), Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_search:
                Intent intent = new Intent(MainActivity.this, SearchBarActivity.class);
                startActivity(intent);

                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ServiceUtils serviceUtils = new ServiceUtils();
        serviceUtils.scheduleTask(this);
    }

}
