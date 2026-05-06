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

public class RechargeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recharge, container, false);

        EditText etPhone = view.findViewById(R.id.et_mobile_number);
        Spinner spinner = view.findViewById(R.id.spinner_operators);
        EditText etAmount = view.findViewById(R.id.et_recharge_amount);
        Button btnRecharge = view.findViewById(R.id.btn_recharge_now);
        TextView tvBalance = view.findViewById(R.id.tv_recharge_balance);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", "");

        updateBalanceDisplay(tvBalance, prefs, userEmail);

        String[] operators = {"Airtel", "Jio", "Vi", "BSNL"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, operators);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        btnRecharge.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            String operator = spinner.getSelectedItem().toString();
            String amountStr = etAmount.getText().toString().trim();

            if (phone.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
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
            String entry = operator + " Recharge (" + phone + ") – ₹" + amountStr + "|";
            editor.putString("history_" + userEmail, history + entry);
            editor.apply();

            updateBalanceDisplay(tvBalance, prefs, userEmail);
            Toast.makeText(getContext(), "Recharge Successful!", Toast.LENGTH_LONG).show();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new HomeFragment());
            }
        });

        return view;
    }

    private void updateBalanceDisplay(TextView tvBalance, SharedPreferences prefs, String userEmail) {
        String balance = prefs.getString("savings_" + userEmail, "0.00");
        tvBalance.setText(getString(R.string.available_balance, balance));
    }
}