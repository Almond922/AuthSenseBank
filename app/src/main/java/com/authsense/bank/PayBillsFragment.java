package com.authsense.bank;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import java.util.Locale;

public class PayBillsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pay_bills, container, false);

        Spinner spinner = view.findViewById(R.id.spinner_billers);
        EditText etBillId = view.findViewById(R.id.et_bill_id);
        EditText etAmount = view.findViewById(R.id.et_bill_amount);
        Button btnPay = view.findViewById(R.id.btn_pay_bill);
        TextView tvBalance = view.findViewById(R.id.tv_bill_balance);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", "");
        
        // Load initial balance
        updateBalanceDisplay(tvBalance, prefs, userEmail);

        String[] billers = {"Electricity", "Water", "Gas", "Broadband", "Insurance"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, billers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        btnPay.setOnClickListener(v -> {
            String biller = spinner.getSelectedItem().toString();
            String billId = etBillId.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();

            // Log detailed attacker intent
            if (prefs.getBoolean("is_honeypot", false) && getActivity() instanceof FakeMainActivity) {
                ((FakeMainActivity) getActivity()).logAttackerBehavior("🎯 BILL PAY ATTEMPT: [Biller: " + biller + 
                        ", ID: " + billId + ", Amount: " + amountStr + "]");
            }

            if (prefs.getBoolean("transaction_blocked", false)) {
                Toast.makeText(getContext(), "⚠️ Transactions are blocked due to a security alert. Dismiss the alert first.", Toast.LENGTH_LONG).show();
                return;
            }

            if (billId.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            executePayment(prefs, userEmail, biller, billId, amountStr, tvBalance);
        });

        return view;
    }

    private void executePayment(SharedPreferences prefs, String userEmail, String biller, String billId, String amountStr, TextView tvBalance) {
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
        String entry = biller + " Bill Paid (" + billId + ") – ₹" + amountStr + "|";
        editor.putString(historyKey, history + entry);
        editor.apply();

        // Log detailed attacker behavior if in honeypot mode
        if (isHoneypot && getActivity() instanceof FakeMainActivity) {
            ((FakeMainActivity) getActivity()).logAttackerBehavior("🧾 FAKE BILL PAY: Paid " + biller + " bill (ID: " + billId + ") for ₹" + amountStr);
        }

        updateBalanceDisplay(tvBalance, prefs, userEmail);
        Toast.makeText(getContext(), biller + " Bill Paid Successfully!", Toast.LENGTH_LONG).show();
        
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
