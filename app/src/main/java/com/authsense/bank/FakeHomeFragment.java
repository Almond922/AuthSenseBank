package com.authsense.bank;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.authsense.bank.adapters.CarouselAdapter;
import java.util.Locale;

public class FakeHomeFragment extends Fragment {

    private ViewPager2 carousel;
    private LinearLayout dotsLayout;
    private Handler autoSlideHandler = new Handler();
    private int currentPage = 0;
    private SharedPreferences prefs;
    private String userEmail;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        prefs = requireActivity().getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("user_email", "");

        TextView tvWelcome = view.findViewById(R.id.tv_welcome);
        TextView tvLastLogin = view.findViewById(R.id.tv_last_login);
        TextView tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        TextView tvSavingsBalance = view.findViewById(R.id.tv_savings_balance);
        TextView tvCurrentBalance = view.findViewById(R.id.tv_current_balance);
        TextView tvInsights = view.findViewById(R.id.tv_insights);
        LinearLayout llTransactions = view.findViewById(R.id.ll_transactions);

        carousel = view.findViewById(R.id.carousel);
        dotsLayout = view.findViewById(R.id.dots_layout);

        // Load REAL data to deceive the attacker
        String name = prefs.getString("name_" + userEmail, "User");
        String lastLogin = prefs.getString("last_login_" + userEmail, "Recently");

        boolean isHoneypot = prefs.getBoolean("is_honeypot", false);
        String savingsKey = (isHoneypot ? "fake_savings_" : "savings_") + userEmail;
        String currentKey = (isHoneypot ? "fake_current_" : "current_") + userEmail;

        String savingsStr = prefs.getString(savingsKey, "");
        if (savingsStr.isEmpty() && isHoneypot) {
            savingsStr = prefs.getString("savings_" + userEmail, "124560.75");
        }

        String currentStr = prefs.getString(currentKey, "");
        if (currentStr.isEmpty() && isHoneypot) {
            currentStr = prefs.getString("current_" + userEmail, "26360.75");
        }

        double savings = 0, current = 0;
        try {
            savings = Double.parseDouble(savingsStr);
            current = Double.parseDouble(currentStr);
        } catch (NumberFormatException e) {
            savings = 124560.75;
            current = 26360.75;
        }

        tvWelcome.setText(getString(R.string.welcome_back, name));
        tvLastLogin.setText(getString(R.string.last_login, lastLogin));
        tvSavingsBalance.setText(String.format(Locale.US, "$ %.2f", savings));
        tvCurrentBalance.setText(String.format(Locale.US, "$ %.2f", current));
        tvTotalBalance.setText(String.format(Locale.US, "$ %.2f", savings + current));
        
        tvInsights.setText("Your account security status: High. All systems operational.\n\nSecurity Tip: Regularly update your password for better safety.");

        // Load real-looking transactions from history
        loadTransactions(llTransactions);

        // Setup carousel (same as real home)
        String[] titlesRes = { "Secure Banking", "Instant Transfers", "Investments", "Safety First" };
        String[] subtitlesRes = { "Protected by AI", "Send money anywhere", "Grow your wealth", "Continuous Monitoring" };

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

        // Click listeners - redirecting to real fragments which are now honeypot-aware
        view.findViewById(R.id.btn_send_money).setOnClickListener(v -> {
            if (getActivity() instanceof FakeMainActivity) {
                ((FakeMainActivity) getActivity()).loadFragment(new SendMoneyFragment());
            }
        });
        view.findViewById(R.id.btn_pay_bills).setOnClickListener(v -> {
            if (getActivity() instanceof FakeMainActivity) {
                ((FakeMainActivity) getActivity()).loadFragment(new PayBillsFragment());
            }
        });
        view.findViewById(R.id.btn_recharge).setOnClickListener(v -> {
            if (getActivity() instanceof FakeMainActivity) {
                ((FakeMainActivity) getActivity()).loadFragment(new RechargeFragment());
            }
        });

        // Other actions still show deceptive success messages if fragments aren't built yet
        View.OnClickListener deceptiveAction = v -> {
            Toast.makeText(getContext(), "Processing request...", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(() -> {
                Toast.makeText(getContext(), "Transaction Successful!", Toast.LENGTH_LONG).show();
                recordDeceptiveAction(v.getId());
            }, 1500);
        };

        view.findViewById(R.id.btn_scan_pay).setOnClickListener(deceptiveAction);
        view.findViewById(R.id.btn_request).setOnClickListener(deceptiveAction);

        return view;
    }

    private void recordDeceptiveAction(int viewId) {
        // Here we would send this data to SensorService or log it
        android.util.Log.d("AUTH_SENSE_HONEYPOT", "Attacker attempted action on view ID: " + viewId);
    }

    private void loadTransactions(LinearLayout llTransactions) {
        boolean isHoneypot = prefs.getBoolean("is_honeypot", false);
        String historyKey = (isHoneypot ? "fake_history_" : "history_") + userEmail;
        String history = prefs.getString(historyKey, "");
        
        if (history.isEmpty() && isHoneypot) {
            history = prefs.getString("history_" + userEmail, "");
        }

        if (history.isEmpty()) {
            addFakeTransaction(llTransactions, "Initial Deposit", "$ 10,000.00", true);
            addFakeTransaction(llTransactions, "ATM Withdrawal", "$ 200.00", false);
            return;
        }

        String[] logs = history.split("\\|");
        for (int i = logs.length - 1; i >= Math.max(0, logs.length - 10); i--) {
            if (!logs[i].isEmpty()) {
                boolean isCredit = logs[i].contains("Credit") || logs[i].contains("Refund") || logs[i].contains("Deposit");
                addFakeTransaction(llTransactions, logs[i], "", isCredit);
            }
        }
    }

    private void addFakeTransaction(LinearLayout container, String title, String amount, boolean isCredit) {
        if (getContext() == null) return;
        TextView tv = new TextView(getContext());
        tv.setText(String.format("%s %s", title, amount));
        tv.setPadding(0, 40, 0, 40);
        tv.setTextColor(isCredit ? ContextCompat.getColor(getContext(), R.color.success) : ContextCompat.getColor(getContext(), R.color.text_primary));
        
        View divider = new View(getContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.divider));

        container.addView(tv);
        container.addView(divider);
    }

    private void setupDots(int count) {
        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(requireContext());
            dot.setImageDrawable(ContextCompat.getDrawable(requireContext(), i == 0 ? R.drawable.dot_active : R.drawable.dot_inactive));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
            params.setMargins(6, 0, 6, 0);
            dotsLayout.addView(dot, params);
        }
    }

    private void updateDots(int selected) {
        for (int i = 0; i < dotsLayout.getChildCount(); i++) {
            ImageView dot = (ImageView) dotsLayout.getChildAt(i);
            dot.setImageDrawable(ContextCompat.getDrawable(requireContext(), i == selected ? R.drawable.dot_active : R.drawable.dot_inactive));
        }
    }
}
