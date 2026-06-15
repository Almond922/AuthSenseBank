package com.authsense.bank;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import java.util.Locale;

public class ScanPayFragment extends Fragment {

    private LinearLayout llDetails;
    private TextView tvName, tvAcc, tvBalance;
    private EditText etAmount;
    private Button btnConfirm, btnSimulate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan_pay, container, false);

        llDetails = view.findViewById(R.id.ll_recipient_details);
        tvName = view.findViewById(R.id.tv_scanned_name);
        tvAcc = view.findViewById(R.id.tv_scanned_acc);
        tvBalance = view.findViewById(R.id.tv_scan_balance);
        etAmount = view.findViewById(R.id.et_scan_amount);
        btnConfirm = view.findViewById(R.id.btn_scan_pay_confirm);
        btnSimulate = view.findViewById(R.id.btn_simulate_scan);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", "");

        updateBalanceDisplay(tvBalance, prefs, userEmail);

        btnSimulate.setOnClickListener(v -> {
            // Simulate a successful scan
            tvName.setText("Recipient: Sarah Connor");
            tvAcc.setText("Acc: 9876543210");
            llDetails.setVisibility(View.VISIBLE);
            btnSimulate.setVisibility(View.GONE);
        });

        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String recipient = tvName.getText().toString();

            // Log detailed attacker intent
            if (prefs.getBoolean("is_honeypot", false) && getActivity() instanceof FakeMainActivity) {
                ((FakeMainActivity) getActivity()).logAttackerBehavior("🎯 QR PAY ATTEMPT: [" + recipient + ", Amount: " + amountStr + "]");
            }

            if (prefs.getBoolean("transaction_blocked", false)) {
                Toast.makeText(getContext(), "⚠️ Transactions are blocked due to a security alert. Dismiss the alert first.", Toast.LENGTH_LONG).show();
                return;
            }
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            executePayment(prefs, userEmail, amountStr, tvBalance);
        });

        return view;
    }

    private void executePayment(SharedPreferences prefs, String userEmail, String amountStr, TextView tvBalance) {
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isHoneypot = prefs.getBoolean("is_honeypot", false);
        String balanceKey = (isHoneypot ? "fake_savings_" : "savings_") + userEmail;
        String historyKey = (isHoneypot ? "fake_history_" : "history_") + userEmail;

        String balanceStr = prefs.getString(balanceKey, "");
        if (balanceStr.isEmpty() && isHoneypot) {
            balanceStr = prefs.getString("savings_" + userEmail, "124560.75");
        }
        
        double currentBalance = Double.parseDouble(balanceStr.isEmpty() ? "0.00" : balanceStr);

        if (amount > currentBalance) {
            Toast.makeText(getContext(), "Insufficient balance", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update Balance
        double newBalance = currentBalance - amount;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(balanceKey, String.format(Locale.US, "%.2f", newBalance));

        // Update History
        String history = prefs.getString(historyKey, "");
        String entry = "Paid Sarah Connor via QR – ₹" + amountStr + "|";
        editor.putString(historyKey, history + entry);
        editor.apply();

        // Log detailed attacker behavior if in honeypot mode
        if (isHoneypot && getActivity() instanceof FakeMainActivity) {
            ((FakeMainActivity) getActivity()).logAttackerBehavior("📸 FAKE QR PAY: Paid ₹" + amountStr + " to Sarah Connor via QR");
        }

        updateBalanceDisplay(tvBalance, prefs, userEmail);
        Toast.makeText(getContext(), "Payment Successful!", Toast.LENGTH_LONG).show();
        
        if (getActivity() instanceof MainActivity || getActivity() instanceof FakeMainActivity) {
            Fragment homeFragment = isHoneypot ? new FakeHomeFragment() : new HomeFragment();
            if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).loadFragment(homeFragment);
            else ((FakeMainActivity) getActivity()).loadFragment(homeFragment);
        }
    }

    private void updateBalanceDisplay(TextView tvBalance, SharedPreferences prefs, String userEmail) {
        boolean isHoneypot = prefs.getBoolean("is_honeypot", false);
        String balanceKey = (isHoneypot ? "fake_savings_" : "savings_") + userEmail;
        String balance = prefs.getString(balanceKey, "");
        
        if (balance.isEmpty() && isHoneypot) {
            balance = prefs.getString("savings_" + userEmail, "124560.75");
        }
        tvBalance.setText(getString(R.string.available_balance, balance));
    }
}
