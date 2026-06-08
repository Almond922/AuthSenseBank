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
            if (prefs.getBoolean("transaction_blocked", false)) {
                Toast.makeText(getContext(), "⚠️ Transactions are blocked due to a security alert. Dismiss the alert first.", Toast.LENGTH_LONG).show();
                return;
            }

            String biller = spinner.getSelectedItem().toString();
            String billId = etBillId.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();

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
        String entry = biller + " Bill Paid (" + billId + ") – ₹" + amountStr + "|";
        editor.putString("history_" + userEmail, history + entry);
        editor.apply();

        updateBalanceDisplay(tvBalance, prefs, userEmail);
        Toast.makeText(getContext(), biller + " Bill Paid Successfully!", Toast.LENGTH_LONG).show();
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new HomeFragment());
        }
    }

    private void updateBalanceDisplay(TextView tvBalance, SharedPreferences prefs, String userEmail) {
        String balance = prefs.getString("savings_" + userEmail, "0.00");
        tvBalance.setText(getString(R.string.available_balance, balance));
    }
}
