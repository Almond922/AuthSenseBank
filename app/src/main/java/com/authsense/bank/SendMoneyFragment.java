package com.authsense.bank;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import java.util.Locale;

public class SendMoneyFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send_money, container, false);

        EditText etName = view.findViewById(R.id.et_recipient_name);
        EditText etAcc = view.findViewById(R.id.et_acc_number);
        EditText etAmount = view.findViewById(R.id.et_amount);
        Button btnTransfer = view.findViewById(R.id.btn_transfer);
        TextView tvBalance = view.findViewById(R.id.tv_send_balance);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", "");
        
        updateBalanceDisplay(tvBalance, prefs, userEmail);

        btnTransfer.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String acc = etAcc.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();

            // Log detailed attacker intent regardless of validation
            if (prefs.getBoolean("is_honeypot", false) && getActivity() instanceof FakeMainActivity) {
                ((FakeMainActivity) getActivity()).logAttackerBehavior("🎯 TRANSACTION ATTEMPT: [Name: " + name + 
                        ", Acc: " + acc + ", Amount: " + amountStr + "]");
            }

            if (prefs.getBoolean("transaction_blocked", false)) {
                Toast.makeText(getContext(), "⚠️ Transactions are blocked due to a security alert. Dismiss the alert first.", Toast.LENGTH_LONG).show();
                return;
            }

            if (name.isEmpty() || acc.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            executeTransfer(prefs, userEmail, name, acc, amountStr, tvBalance);
        });

        return view;
    }

    private void executeTransfer(SharedPreferences prefs, String userEmail, String name, String acc, String amountStr, TextView tvBalance) {
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
            // Initialize fake balance with real balance for the first time
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
        String entry = "Sent to " + name + " (" + acc + ") – ₹" + amountStr + "|";
        editor.putString(historyKey, history + entry);
        editor.apply();

        // Log detailed attacker behavior if in honeypot mode
        if (isHoneypot && getActivity() instanceof FakeMainActivity) {
            ((FakeMainActivity) getActivity()).logAttackerBehavior("💸 FAKE TRANSFER: Sent ₹" + amountStr + " to " + name + " (Acc: " + acc + ")");
        }

        updateBalanceDisplay(tvBalance, prefs, userEmail);
        Toast.makeText(getContext(), "Transfer Successful!", Toast.LENGTH_LONG).show();
        
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
