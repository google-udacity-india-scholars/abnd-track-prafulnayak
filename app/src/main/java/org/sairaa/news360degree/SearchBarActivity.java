package org.sairaa.news360degree;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.sairaa.news360degree.db.News;
import org.sairaa.news360degree.utils.CommonUtils;

public class SearchBarActivity extends AppCompatActivity {
    private SearchView searchView;

    CommonUtils commonUtils;
    FloatingActionButton floatingActionButton;
    private RecyclerView recyclerView;
    private NewsAdapter adapter;
    private NewsViewModel viewModel;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_bar);

        commonUtils = new CommonUtils(this);
        recyclerView = findViewById(R.id.search_material_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        viewModel = ViewModelProviders.of(this).get(NewsViewModel.class);
        adapter = new NewsAdapter(this);

        toolbar = findViewById(R.id.material_search_toolbar);
        searchView = findViewById(R.id.search_material);//new SearchView(this);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (getSupportActionBar() != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
        }

        //set focus to search view
        searchView.setFocusable(true);
        searchView.setIconified(false);
        searchView.requestFocusFromTouch();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                subscribeUi(adapter,query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                subscribeUi(adapter,newText);
                return true;
            }
        });

        floatingActionButton = findViewById(R.id.search_material_fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recyclerView.smoothScrollToPosition(0);

            }
        });

    }

    private void subscribeUi(final NewsAdapter adapter, final String queryString) {
        viewModel.getSearchNewsListLiveData(queryString).observe(this, new Observer<PagedList<News>>() {
            @Override
            public void onChanged(@Nullable PagedList<News> news) {
                if(!news.isEmpty()){
                    adapter.submitList(news);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    recyclerView.smoothScrollToPosition(0);
                }else
                    Toast.makeText(SearchBarActivity.this,getString(R.string.noInfo)+queryString,Toast.LENGTH_SHORT).show();


            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
