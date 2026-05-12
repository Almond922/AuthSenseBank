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
            if (prefs.getBoolean("transaction_blocked", false)) {
                Toast.makeText(getContext(), "⚠️ Transactions are blocked due to a security alert. Dismiss the alert first.", Toast.LENGTH_LONG).show();
                return;
            }

            String name = etName.getText().toString().trim();
            String acc = etAcc.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();

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

        double currentBalance = Double.parseDouble(prefs.getString("savings_" + userEmail, "0.00"));

        if (amount > currentBalance) {
            Toast.makeText(getContext(), "Insufficient balance", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update Balance
        double newBalance = currentBalance - amount;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("savings_" + userEmail, String.format(Locale.US, "%.2f", newBalance));

        // Update History
        String history = prefs.getString("history_" + userEmail, "");
        String entry = "Sent to " + name + " (" + acc + ") – ₹" + amountStr + "|";
        editor.putString("history_" + userEmail, history + entry);
        editor.apply();

        updateBalanceDisplay(tvBalance, prefs, userEmail);
        Toast.makeText(getContext(), "Transfer Successful!", Toast.LENGTH_LONG).show();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new HomeFragment());
        }
    }

    private void updateBalanceDisplay(TextView tvBalance, SharedPreferences prefs, String userEmail) {
        String balance = prefs.getString("savings_" + userEmail, "0.00");
        tvBalance.setText(getString(R.string.available_balance, balance));
    }
}
