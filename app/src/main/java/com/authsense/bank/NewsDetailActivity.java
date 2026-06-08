package com.authsense.bank;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class NewsDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TAG = "extra_tag";
    public static final String EXTRA_DATE = "extra_date";
    public static final String EXTRA_HEADLINE = "extra_headline";
    public static final String EXTRA_FULL_ARTICLE = "extra_full_article";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);

        Toolbar toolbar = findViewById(R.id.toolbar_news_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        String tag         = getIntent().getStringExtra(EXTRA_TAG);
        String date        = getIntent().getStringExtra(EXTRA_DATE);
        String headline    = getIntent().getStringExtra(EXTRA_HEADLINE);
        String fullArticle = getIntent().getStringExtra(EXTRA_FULL_ARTICLE);

        ((TextView) findViewById(R.id.tv_detail_tag)).setText(tag);
        ((TextView) findViewById(R.id.tv_detail_date)).setText(date);
        ((TextView) findViewById(R.id.tv_detail_headline)).setText(headline);
        ((TextView) findViewById(R.id.tv_detail_body)).setText(fullArticle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
