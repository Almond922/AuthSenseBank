package com.authsense.bank;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.authsense.bank.adapters.NewsAdapter;
import java.util.ArrayList;
import java.util.List;

public class NewsFragment extends Fragment {

    private NewsAdapter adapter;
    private List<NewsAdapter.NewsItem> allNews;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);

        RecyclerView rv = view.findViewById(R.id.rv_news);
        EditText etSearch = view.findViewById(R.id.et_search_news);

        allNews = getNewsList();
        adapter = new NewsAdapter(requireContext(), new ArrayList<>(allNews));

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNews(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void filterNews(String query) {
        List<NewsAdapter.NewsItem> filtered = new ArrayList<>();
        for (NewsAdapter.NewsItem item : allNews) {
            if (item.headline.toLowerCase().contains(query.toLowerCase()) ||
                item.summary.toLowerCase().contains(query.toLowerCase()) ||
                item.tag.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(item);
            }
        }
        adapter.updateList(filtered);
    }

    private List<NewsAdapter.NewsItem> getNewsList() {
        List<NewsAdapter.NewsItem> news = new ArrayList<>();

        news.add(new NewsAdapter.NewsItem(
                "BANKING", "Apr 17, 2026",
                "AuthSense Bank Launches Zero-Fee Student Savings Account",
                "In a major push toward financial inclusion, AuthSense Bank announced a fully digital zero-fee savings account exclusively designed for students aged 18–25, with instant KYC via Aadhaar."
        ));

        news.add(new NewsAdapter.NewsItem(
                "CREDIT CARD", "Apr 15, 2026",
                "New AuthSense Signature Card Offers 10X Rewards on Luxury Spends",
                "The newly launched Signature card targets high-net-worth individuals with exclusive perks including dedicated concierge service, golf access, and premium hotel benefits worldwide."
        ));

        news.add(new NewsAdapter.NewsItem(
                "INVESTMENT", "Apr 12, 2026",
                "AuthSense Mutual Fund SIP Now Available Inside the App",
                "Customers can now start a SIP directly from the AuthSense mobile app with as little as ₹500/month, with AI-powered fund recommendations based on their spending profile."
        ));

        news.add(new NewsAdapter.NewsItem(
                "SECURITY", "Apr 10, 2026",
                "AuthSense Rolls Out AI Fraud Detection — 99.7% Accuracy",
                "The bank's newly deployed real-time fraud detection engine powered by machine learning blocked over 12,000 fraudulent transactions in its first week of operation."
        ));

        news.add(new NewsAdapter.NewsItem(
                "AWARDS", "Apr 5, 2026",
                "AuthSense Bank Wins 'Best Digital Bank of the Year 2026'",
                "At the India Banking Excellence Awards 2026, AuthSense Bank was recognised for its outstanding innovation in digital banking, customer experience, and cybersecurity infrastructure."
        ));

        return news;
    }
}