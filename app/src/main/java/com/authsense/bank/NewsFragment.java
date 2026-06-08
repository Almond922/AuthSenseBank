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
                "In a major push toward financial inclusion, AuthSense Bank announced a fully digital zero-fee savings account exclusively designed for students.",
                "AuthSense Bank has officially launched its 'Gen-Z Savings Account', a zero-balance digital account designed specifically for students aged 18-25.\n\n" +
                        "Key Benefits:\n" +
                        "• No minimum balance requirement.\n" +
                        "• Instant KYC via Aadhaar.\n" +
                        "• Free Virtual Debit Card for online shopping.\n" +
                        "• Exclusive discounts on educational platforms and food delivery apps.\n\n" +
                        "The initiative aims to bring banking closer to the youth and encourage early financial planning."
        ));

        news.add(new NewsAdapter.NewsItem(
                "CREDIT CARD", "Apr 15, 2026",
                "New AuthSense Signature Card Offers 10X Rewards on Luxury Spends",
                "The newly launched Signature card targets high-net-worth individuals with exclusive perks and golf access.",
                "We are proud to introduce the AuthSense Signature Credit Card, our most premium offering yet.\n\n" +
                        "Highlights include:\n" +
                        "• 10X rewards points on luxury dining and travel.\n" +
                        "• 4 Complimentary rounds of golf per year at selected clubs.\n" +
                        "• Unlimited international lounge access.\n" +
                        "• 24/7 dedicated personal concierge service.\n\n" +
                        "Apply now through the 'Cards' section of the app to elevate your lifestyle."
        ));

        news.add(new NewsAdapter.NewsItem(
                "INVESTMENT", "Apr 12, 2026",
                "AuthSense Mutual Fund SIP Now Available Inside the App",
                "Customers can now start a SIP directly from the app with as little as ₹500/month.",
                "Investing has never been easier. With the latest update to the AuthSense mobile app, users can now start Systematic Investment Plans (SIPs) in over 5,000 mutual funds.\n\n" +
                        "New Features:\n" +
                        "• 'Smart Recommendation' engine based on your risk appetite.\n" +
                        "• Paperless setup in under 2 minutes.\n" +
                        "• Real-time tracking of your portfolio.\n\n" +
                        "Start building your wealth today with the 'Investment' tab."
        ));

        news.add(new NewsAdapter.NewsItem(
                "SECURITY", "Apr 10, 2026",
                "AuthSense Rolls Out AI Fraud Detection — 99.7% Accuracy",
                "The bank's newly deployed real-time fraud detection engine powered by machine learning is now live.",
                "Security is our top priority. AuthSense Bank has deployed 'Guardian AI', a state-of-the-art fraud detection system.\n\n" +
                        "Guardian AI analyzes millions of data points in real-time, including:\n" +
                        "• Keystroke dynamics and device motion patterns.\n" +
                        "• Unusual transaction locations.\n" +
                        "• Login attempts from unrecognized hardware.\n\n" +
                        "In its first week, the system successfully blocked over 12,000 unauthorized access attempts, protecting customer funds without interrupting genuine users."
        ));

        news.add(new NewsAdapter.NewsItem(
                "AWARDS", "Apr 5, 2026",
                "AuthSense Bank Wins 'Best Digital Bank of the Year 2026'",
                "AuthSense Bank was recognised for its outstanding innovation in digital banking and cybersecurity.",
                "We are thrilled to announce that AuthSense Bank has been named the 'Best Digital Bank of the Year 2026' at the India Banking Excellence Awards.\n\n" +
                        "The jury praised our seamless app interface, our focus on user privacy, and the innovative implementation of behavioral biometrics.\n\n" +
                        "We dedicate this award to our loyal customers who have been part of our journey since day one."
        ));

        return news;
    }
}