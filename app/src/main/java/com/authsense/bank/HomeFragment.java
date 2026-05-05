package com.authsense.bank;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.authsense.bank.adapters.CarouselAdapter;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private ViewPager2 carousel;
    private LinearLayout dotsLayout;
    private Handler autoSlideHandler = new Handler();
    private int currentPage = 0;

    private TextView tvWelcome, tvLastLogin, tvTotalBalance, tvSavingsBalance, tvCurrentBalance, tvInsights;
    private LinearLayout llTransactions;
    private SharedPreferences prefs;
    private String userEmail;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        prefs = requireActivity().getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("user_email", "");

        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvLastLogin = view.findViewById(R.id.tv_last_login);
        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvSavingsBalance = view.findViewById(R.id.tv_savings_balance);
        tvCurrentBalance = view.findViewById(R.id.tv_current_balance);
        tvInsights = view.findViewById(R.id.tv_insights);
        llTransactions = view.findViewById(R.id.ll_transactions);

        carousel = view.findViewById(R.id.carousel);
        dotsLayout = view.findViewById(R.id.dots_layout);

        loadUserData();
        loadTransactions();

        // Carousel data from resources
        String[] titlesRes = {
                getString(R.string.carousel_title1), getString(R.string.carousel_title2),
                getString(R.string.carousel_title3), getString(R.string.carousel_title4)
        };
        String[] subtitlesRes = {
                getString(R.string.carousel_subtitle1), getString(R.string.carousel_subtitle2),
                getString(R.string.carousel_subtitle3), getString(R.string.carousel_subtitle4)
        };

        // Setup carousel
        CarouselAdapter adapter = new CarouselAdapter(requireContext(), titlesRes, subtitlesRes);
        carousel.setAdapter(adapter);
        setupDots(titlesRes.length);

        carousel.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPage = position;
                updateDots(position);
            }
        });

        // Auto-slide every 3 seconds
        Runnable autoSlide = new Runnable() {
            @Override
            public void run() {
                int next = (currentPage + 1) % titlesRes.length;
                carousel.setCurrentItem(next, true);
                autoSlideHandler.postDelayed(this, 3000);
            }
        };
        autoSlideHandler.postDelayed(autoSlide, 3000);

        // Click listeners for Quick Actions
        view.findViewById(R.id.btn_send_money).setOnClickListener(v -> navigateTo(new SendMoneyFragment()));
        view.findViewById(R.id.btn_pay_bills).setOnClickListener(v -> navigateTo(new PayBillsFragment()));
        view.findViewById(R.id.btn_recharge).setOnClickListener(v -> navigateTo(new RechargeFragment()));
        view.findViewById(R.id.btn_scan_pay).setOnClickListener(v -> navigateTo(new ScanPayFragment()));
        view.findViewById(R.id.btn_request).setOnClickListener(v -> navigateTo(new RequestFragment()));

        setupQuickAssist(view);

        return view;
    }

    private void setupQuickAssist(View view) {
        EditText etQuery = view.findViewById(R.id.et_user_query);
        etQuery.setOnEditorActionListener((v, actionId, event) -> {
            String query = etQuery.getText().toString().toLowerCase();
            if (query.contains("send") || query.contains("pay") || query.contains("transfer")) {
                navigateTo(new SendMoneyFragment());
            } else if (query.contains("bill") || query.contains("electricity") || query.contains("water")) {
                navigateTo(new PayBillsFragment());
            } else if (query.contains("recharge") || query.contains("mobile") || query.contains("phone")) {
                navigateTo(new RechargeFragment());
            } else if (query.contains("scan") || query.contains("qr")) {
                navigateTo(new ScanPayFragment());
            } else if (query.contains("card")) {
                navigateTo(new CreditCardFragment());
            } else if (query.contains("news")) {
                navigateTo(new NewsFragment());
            } else {
                Toast.makeText(getContext(), "I'm not sure how to help with that yet.", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private void loadUserData() {
        String name = prefs.getString("name_" + userEmail, "Guest");
        String lastLogin = prefs.getString("last_login_" + userEmail, "Never");
        
        // If balance is missing or zero, initialize with demo funds
        if (!prefs.contains("savings_" + userEmail) || prefs.getString("savings_" + userEmail, "0.00").equals("0.00")) {
            prefs.edit()
                .putString("savings_" + userEmail, "124560.75")
                .putString("current_" + userEmail, "26360.75")
                .apply();
        }

        String savingsStr = prefs.getString("savings_" + userEmail, "124560.75");
        String currentStr = prefs.getString("current_" + userEmail, "26360.75");

        double savings = 0, current = 0;
        try {
            savings = Double.parseDouble(savingsStr);
            current = Double.parseDouble(currentStr);
        } catch (NumberFormatException e) {
            // Default to 0
        }

        tvWelcome.setText(getString(R.string.welcome_back, name));
        tvLastLogin.setText(getString(R.string.last_login, lastLogin));
        tvSavingsBalance.setText(getString(R.string.currency_symbol, String.format("%.2f", savings)));
        tvCurrentBalance.setText(getString(R.string.currency_symbol, String.format("%.2f", current)));

        double total = savings + current;
        tvTotalBalance.setText(getString(R.string.currency_symbol, String.format("%.2f", total)));
        
        generateInsights(total, name);

        // Update last login to now for next time
        String now = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
        prefs.edit().putString("last_login_" + userEmail, now).apply();
    }

    private void generateInsights(double total, String name) {
        StringBuilder sb = new StringBuilder();
        if (total > 100000) {
            sb.append("Great job, ").append(name).append("! Your balance is strong. Consider moving some funds to a High-Yield Savings account or exploring our investment options.\n\n");
        } else if (total > 10000) {
            sb.append("Your financial health is stable. To grow your wealth, try setting a monthly savings goal of 10%.\n\n");
        } else {
            sb.append("Welcome! Start your journey by setting up a small monthly SIP or recurring deposit to build your emergency fund.\n\n");
        }

        sb.append("Based on your recent activity, you are eligible for personalized credit card offers. Check the 'Credit Cards' tab for more details.\n\n");
        sb.append("Security Tip: Always ensure you are using the official AuthSense Bank app and never share your OTP with anyone.");
        
        if (tvInsights != null) {
            tvInsights.setText(sb.toString());
        }
    }

    private void loadTransactions() {
        llTransactions.removeAllViews();
        String history = prefs.getString("history_" + userEmail, "");
        if (history.isEmpty()) {
            addTransactionToUI("No transactions yet", false);
            return;
        }

        String[] logs = history.split("\\|");
        for (int i = logs.length - 1; i >= 0; i--) { // Show latest first
            if (!logs[i].isEmpty()) {
                boolean isCredit = logs[i].contains("Credit") || logs[i].contains("Refund");
                addTransactionToUI(logs[i], isCredit);
            }
        }
    }

    private void addTransactionToUI(String text, boolean isCredit) {
        if (getContext() == null) return;
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(0, 40, 0, 40);
        tv.setTextColor(isCredit ? ContextCompat.getColor(getContext(), R.color.success) : ContextCompat.getColor(getContext(), R.color.text_primary));
        if (isCredit) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        
        View divider = new View(getContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.divider));

        llTransactions.addView(tv);
        llTransactions.addView(divider);
    }

    private void navigateTo(Fragment fragment) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(fragment);
        }
    }

    private void setupDots(int count) {
        ImageView[] dots = new ImageView[count];
        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(requireContext());
            dots[i].setImageDrawable(ContextCompat.getDrawable(requireContext(),
                    i == 0 ? R.drawable.dot_active : R.drawable.dot_inactive));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
            params.setMargins(6, 0, 6, 0);
            dotsLayout.addView(dots[i], params);
        }
    }

    private void updateDots(int selected) {
        for (int i = 0; i < dotsLayout.getChildCount(); i++) {
            ImageView dot = (ImageView) dotsLayout.getChildAt(i);
            dot.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                    i == selected ? R.drawable.dot_active : R.drawable.dot_inactive));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        autoSlideHandler.removeCallbacksAndMessages(null);
    }
}
