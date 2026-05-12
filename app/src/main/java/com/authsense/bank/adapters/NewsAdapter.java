package com.authsense.bank.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.authsense.bank.NewsDetailActivity;
import com.authsense.bank.R;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private final Context context;
    private List<NewsItem> newsList;

    public NewsAdapter(Context context, List<NewsItem> newsList) {
        this.context = context;
        this.newsList = newsList;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem item = newsList.get(position);
        holder.tag.setText(item.tag);
        holder.date.setText(item.date);
        holder.headline.setText(item.headline);
        holder.summary.setText(item.summary);
        
        holder.readMore.setOnClickListener(v -> showFullArticle(item));
        holder.itemView.setOnClickListener(v -> showFullArticle(item));
    }

    private void showFullArticle(NewsItem item) {
        Intent intent = new Intent(context, NewsDetailActivity.class);
        intent.putExtra(NewsDetailActivity.EXTRA_TAG, item.tag);
        intent.putExtra(NewsDetailActivity.EXTRA_DATE, item.date);
        intent.putExtra(NewsDetailActivity.EXTRA_HEADLINE, item.headline);
        intent.putExtra(NewsDetailActivity.EXTRA_FULL_ARTICLE, item.fullArticle);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() { return newsList.size(); }

    public void updateList(List<NewsItem> newList) {
        this.newsList = newList;
        notifyDataSetChanged();
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        TextView tag, date, headline, summary, readMore;
        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            tag = itemView.findViewById(R.id.news_tag);
            date = itemView.findViewById(R.id.news_date);
            headline = itemView.findViewById(R.id.news_headline);
            summary = itemView.findViewById(R.id.news_summary);
            readMore = itemView.findViewById(R.id.news_read_more);
        }
    }

    public static class NewsItem {
        public String tag, date, headline, summary, fullArticle;
        public NewsItem(String tag, String date, String headline, String summary) {
            this.tag = tag; this.date = date;
            this.headline = headline; this.summary = summary;
            this.fullArticle = summary; // Defaulting to summary if full article not provided
        }

        public NewsItem(String tag, String date, String headline, String summary, String fullArticle) {
            this.tag = tag; this.date = date;
            this.headline = headline; this.summary = summary;
            this.fullArticle = fullArticle;
        }
    }
}