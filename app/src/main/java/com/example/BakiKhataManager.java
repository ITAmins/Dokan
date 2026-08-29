package com.example;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.databinding.ActivityMainBinding;
import com.example.databinding.LayoutBakiMiniAppBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Controller for Baki Khata Mini-App inside MAWA Shop.
 * Handles sub-navigation, ledger calculations, aging analysis, customer profiles, and reminder automation.
 */
public class BakiKhataManager {

    public enum SubTab {
        DASHBOARD,
        CUSTOMERS,
        TRANSACTIONS,
        REPORTS,
        REMINDERS
    }

    private final MainActivity activity;
    private final ActivityMainBinding mainBinding;
    private final LayoutBakiMiniAppBinding binding;
    private SubTab currentSubTab = SubTab.DASHBOARD;

    private String customerFilter = "ALL"; // ALL, DUE, HIGHEST, OVERDUE, PAID
    private String txFilter = "ALL"; // ALL, DUE, JOMA
    private String customerSearchQuery = "";
    private String txSearchQuery = "";

    public BakiKhataManager(MainActivity activity, ActivityMainBinding mainBinding) {
        this.activity = activity;
        this.mainBinding = mainBinding;
        this.binding = mainBinding != null ? mainBinding.bakiApp : null;
    }

    public void setup() {
        if (binding == null) return;

        setupSubNavigation();
        setupDashboardActions();
        setupCustomerDirectoryActions();
        setupTransactionsActions();
        setupReportsActions();
        setupRemindersActions();

        updateUI();
    }

    private void setupSubNavigation() {
        View.OnClickListener tabListener = v -> {
            int id = v.getId();
            if (id == R.id.btnBakiSubTabDashboard) {
                switchSubTab(SubTab.DASHBOARD);
            } else if (id == R.id.btnBakiSubTabCustomers) {
                switchSubTab(SubTab.CUSTOMERS);
            } else if (id == R.id.btnBakiSubTabTransactions) {
                switchSubTab(SubTab.TRANSACTIONS);
            } else if (id == R.id.btnBakiSubTabReports) {
                switchSubTab(SubTab.REPORTS);
            } else if (id == R.id.btnBakiSubTabReminders) {
                switchSubTab(SubTab.REMINDERS);
            }
        };

        if (binding.btnBakiSubTabDashboard != null) binding.btnBakiSubTabDashboard.setOnClickListener(tabListener);
        if (binding.btnBakiSubTabCustomers != null) binding.btnBakiSubTabCustomers.setOnClickListener(tabListener);
        if (binding.btnBakiSubTabTransactions != null) binding.btnBakiSubTabTransactions.setOnClickListener(tabListener);
        if (binding.btnBakiSubTabReports != null) binding.btnBakiSubTabReports.setOnClickListener(tabListener);
        if (binding.btnBakiSubTabReminders != null) binding.btnBakiSubTabReminders.setOnClickListener(tabListener);

        if (binding.btnBakiHeaderPdf != null) {
            binding.btnBakiHeaderPdf.setOnClickListener(v -> exportAllBakiPdf());
        }
        if (binding.btnBakiHeaderAddQuick != null) {
            binding.btnBakiHeaderAddQuick.setOnClickListener(v -> showAddDueDialog(null));
        }
    }

    public void switchSubTab(SubTab tab) {
        this.currentSubTab = tab;
        updateSubTabStyles();
        updateSubViewVisibility();
        updateUI();
    }

    private void updateSubTabStyles() {
        int selectedBg = Color.parseColor("#2563EB");
        int selectedText = Color.parseColor("#FFFFFF");
        int unselectedText = Color.parseColor("#475569");

        applyTabStyle(binding.btnBakiSubTabDashboard, currentSubTab == SubTab.DASHBOARD, selectedBg, selectedText, unselectedText);
        applyTabStyle(binding.btnBakiSubTabCustomers, currentSubTab == SubTab.CUSTOMERS, selectedBg, selectedText, unselectedText);
        applyTabStyle(binding.btnBakiSubTabTransactions, currentSubTab == SubTab.TRANSACTIONS, selectedBg, selectedText, unselectedText);
        applyTabStyle(binding.btnBakiSubTabReports, currentSubTab == SubTab.REPORTS, selectedBg, selectedText, unselectedText);
        applyTabStyle(binding.btnBakiSubTabReminders, currentSubTab == SubTab.REMINDERS, selectedBg, selectedText, unselectedText);
    }

    private void applyTabStyle(MaterialButton btn, boolean isSelected, int selBg, int selText, int unselText) {
        if (btn == null) return;
        if (isSelected) {
            btn.setBackgroundTintList(ColorStateList.valueOf(selBg));
            btn.setTextColor(selText);
            btn.setStrokeColor(ColorStateList.valueOf(selBg));
        } else {
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFFFFF")));
            btn.setTextColor(unselText);
            btn.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#CBD5E1")));
            btn.setStrokeWidth(dpToPx(1));
        }
    }

    private void updateSubViewVisibility() {
        if (binding.layoutBakiSubDashboard != null) {
            binding.layoutBakiSubDashboard.setVisibility(currentSubTab == SubTab.DASHBOARD ? View.VISIBLE : View.GONE);
        }
        if (binding.layoutBakiSubCustomers != null) {
            binding.layoutBakiSubCustomers.setVisibility(currentSubTab == SubTab.CUSTOMERS ? View.VISIBLE : View.GONE);
        }
        if (binding.layoutBakiSubTransactions != null) {
            binding.layoutBakiSubTransactions.setVisibility(currentSubTab == SubTab.TRANSACTIONS ? View.VISIBLE : View.GONE);
        }
        if (binding.layoutBakiSubReports != null) {
            binding.layoutBakiSubReports.setVisibility(currentSubTab == SubTab.REPORTS ? View.VISIBLE : View.GONE);
        }
        if (binding.layoutBakiSubReminders != null) {
            binding.layoutBakiSubReminders.setVisibility(currentSubTab == SubTab.REMINDERS ? View.VISIBLE : View.GONE);
        }
    }

    private void setupDashboardActions() {
        if (binding.gridBakiCustomers != null) binding.gridBakiCustomers.setOnClickListener(v -> switchSubTab(SubTab.CUSTOMERS));
        if (binding.gridBakiAddDue != null) binding.gridBakiAddDue.setOnClickListener(v -> showAddDueDialog(null));
        if (binding.gridBakiReceivePayment != null) binding.gridBakiReceivePayment.setOnClickListener(v -> showReceivePaymentDialog(null));
        if (binding.gridBakiTransactions != null) binding.gridBakiTransactions.setOnClickListener(v -> switchSubTab(SubTab.TRANSACTIONS));
        if (binding.gridBakiStatement != null) binding.gridBakiStatement.setOnClickListener(v -> switchSubTab(SubTab.CUSTOMERS));
        if (binding.gridBakiReminder != null) binding.gridBakiReminder.setOnClickListener(v -> switchSubTab(SubTab.REMINDERS));
        if (binding.gridBakiAging != null) binding.gridBakiAging.setOnClickListener(v -> switchSubTab(SubTab.REPORTS));
        if (binding.gridBakiPdf != null) binding.gridBakiPdf.setOnClickListener(v -> exportAllBakiPdf());

        if (binding.btnBakiQuickAddDue != null) binding.btnBakiQuickAddDue.setOnClickListener(v -> showAddDueDialog(null));
        if (binding.btnBakiQuickReceivePay != null) binding.btnBakiQuickReceivePay.setOnClickListener(v -> showReceivePaymentDialog(null));
        if (binding.btnBakiQuickAddCustomer != null) binding.btnBakiQuickAddCustomer.setOnClickListener(v -> showAddCustomerDialog());

        if (binding.btnBakiSeeAllCustomers != null) binding.btnBakiSeeAllCustomers.setOnClickListener(v -> switchSubTab(SubTab.CUSTOMERS));
        if (binding.btnBakiSeeAllTransactions != null) binding.btnBakiSeeAllTransactions.setOnClickListener(v -> switchSubTab(SubTab.TRANSACTIONS));
    }

    private void setupCustomerDirectoryActions() {
        if (binding.etBakiCustomerSearch != null) {
            binding.etBakiCustomerSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    customerSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                    renderCustomerDirectory();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (binding.btnBakiAddNewCustomerFromList != null) {
            binding.btnBakiAddNewCustomerFromList.setOnClickListener(v -> showAddCustomerDialog());
        }

        View.OnClickListener filterListener = v -> {
            int id = v.getId();
            if (id == R.id.btnBakiCustFilterAll) customerFilter = "ALL";
            else if (id == R.id.btnBakiCustFilterDue) customerFilter = "DUE";
            else if (id == R.id.btnBakiCustFilterHighest) customerFilter = "HIGHEST";
            else if (id == R.id.btnBakiCustFilterOverdue) customerFilter = "OVERDUE";
            else if (id == R.id.btnBakiCustFilterPaid) customerFilter = "PAID";
            updateCustomerFilterStyles();
            renderCustomerDirectory();
        };

        if (binding.btnBakiCustFilterAll != null) binding.btnBakiCustFilterAll.setOnClickListener(filterListener);
        if (binding.btnBakiCustFilterDue != null) binding.btnBakiCustFilterDue.setOnClickListener(filterListener);
        if (binding.btnBakiCustFilterHighest != null) binding.btnBakiCustFilterHighest.setOnClickListener(filterListener);
        if (binding.btnBakiCustFilterOverdue != null) binding.btnBakiCustFilterOverdue.setOnClickListener(filterListener);
        if (binding.btnBakiCustFilterPaid != null) binding.btnBakiCustFilterPaid.setOnClickListener(filterListener);
    }

    private void updateCustomerFilterStyles() {
        int selBg = Color.parseColor("#2563EB");
        int selText = Color.parseColor("#FFFFFF");
        int unselText = Color.parseColor("#475569");

        applyTabStyle(binding.btnBakiCustFilterAll, "ALL".equals(customerFilter), selBg, selText, unselText);
        applyTabStyle(binding.btnBakiCustFilterDue, "DUE".equals(customerFilter), selBg, selText, unselText);
        applyTabStyle(binding.btnBakiCustFilterHighest, "HIGHEST".equals(customerFilter), selBg, selText, unselText);
        applyTabStyle(binding.btnBakiCustFilterOverdue, "OVERDUE".equals(customerFilter), selBg, selText, unselText);
        applyTabStyle(binding.btnBakiCustFilterPaid, "PAID".equals(customerFilter), selBg, selText, unselText);
    }

    private void setupTransactionsActions() {
        if (binding.etBakiTxSearch != null) {
            binding.etBakiTxSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    txSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                    renderTransactionsFeed();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        View.OnClickListener txFilterListener = v -> {
            int id = v.getId();
            if (id == R.id.btnBakiTxFilterAll) txFilter = "ALL";
            else if (id == R.id.btnBakiTxFilterDue) txFilter = "DUE";
            else if (id == R.id.btnBakiTxFilterJoma) txFilter = "JOMA";
            updateTxFilterStyles();
            renderTransactionsFeed();
        };

        if (binding.btnBakiTxFilterAll != null) binding.btnBakiTxFilterAll.setOnClickListener(txFilterListener);
        if (binding.btnBakiTxFilterDue != null) binding.btnBakiTxFilterDue.setOnClickListener(txFilterListener);
        if (binding.btnBakiTxFilterJoma != null) binding.btnBakiTxFilterJoma.setOnClickListener(txFilterListener);
    }

    private void updateTxFilterStyles() {
        int selBg = Color.parseColor("#2563EB");
        int selText = Color.parseColor("#FFFFFF");
        int unselText = Color.parseColor("#475569");

        applyTabStyle(binding.btnBakiTxFilterAll, "ALL".equals(txFilter), selBg, selText, unselText);
        applyTabStyle(binding.btnBakiTxFilterDue, "DUE".equals(txFilter), Color.parseColor("#DC2626"), selText, unselText);
        applyTabStyle(binding.btnBakiTxFilterJoma, "JOMA".equals(txFilter), Color.parseColor("#059669"), selText, unselText);
    }

    private void setupReportsActions() {
        if (binding.btnBakiDownloadReportPdf != null) {
            binding.btnBakiDownloadReportPdf.setOnClickListener(v -> exportAllBakiPdf());
        }
    }

    private void setupRemindersActions() {
        // Render reminders list on UI update
    }

    public void updateUI() {
        if (binding == null) return;

        StorageManager storage = StorageManager.getInstance(activity);
        List<BakiModel> allBaki = storage.loadBakiRecords();

        double totalDue = 0.0;
        double totalCollected = 0.0;
        int overdueCount = 0;
        int activeDueCustomers = 0;
        BakiModel highestDueCustomer = null;

        for (BakiModel b : allBaki) {
            if (b.getAmount() > 0) {
                totalDue += b.getAmount();
                activeDueCustomers++;
                if (isOverdue(b.getDueDate())) {
                    overdueCount++;
                }
                if (highestDueCustomer == null || b.getAmount() > highestDueCustomer.getAmount()) {
                    highestDueCustomer = b;
                }
            }
            if (b.getTransactions() != null) {
                for (BakiTransaction tx : b.getTransactions()) {
                    if ("JOMA".equalsIgnoreCase(tx.getType())) {
                        totalCollected += tx.getAmount();
                    }
                }
            }
        }

        // Hero Stats
        if (binding.tvBakiHeroTotalDue != null) {
            binding.tvBakiHeroTotalDue.setText(String.format(Locale.getDefault(), "৳ %,.0f", totalDue));
        }
        if (binding.tvBakiHeroCustomerCount != null) {
            binding.tvBakiHeroCustomerCount.setText(toBengaliDigits(String.valueOf(allBaki.size())) + " জন");
        }
        if (binding.tvBakiHeroTotalCollected != null) {
            binding.tvBakiHeroTotalCollected.setText(String.format(Locale.getDefault(), "৳ %,.0f", totalCollected));
        }
        if (binding.tvBakiHeroActiveDueCount != null) {
            binding.tvBakiHeroActiveDueCount.setText(toBengaliDigits(String.valueOf(activeDueCustomers)) + " জন");
        }
        if (binding.tvBakiHeroOverdueCount != null) {
            binding.tvBakiHeroOverdueCount.setText("মেয়াদোত্তীর্ণ: " + toBengaliDigits(String.valueOf(overdueCount)));
        }

        // Smart Insight Card
        if (binding.tvBakiSmartInsightText != null) {
            if (allBaki.isEmpty()) {
                binding.tvBakiSmartInsightText.setText("কোনো গ্রাহকের বাকি হিসাব খোলা নেই। নতুন বাকি যোগ করতে '+ বাকি দিন' চাপুন।");
            } else if (highestDueCustomer != null && highestDueCustomer.getAmount() > 0) {
                String topName = highestDueCustomer.getCustomerName();
                String topAmt = PdfExporter.formatBengaliNumber(highestDueCustomer.getAmount());
                String msg = "সর্বোচ্চ বকেয়া: " + topName + " (৳ " + topAmt + ")";
                if (overdueCount > 0) {
                    msg += " • " + toBengaliDigits(String.valueOf(overdueCount)) + " জনের নির্ধারিত তারিখ অতিক্রান্ত হয়েছে";
                }
                binding.tvBakiSmartInsightText.setText(msg);
            } else {
                binding.tvBakiSmartInsightText.setText("সমস্ত গ্রাহকের বাকি পরিশোধিত! কোনো সক্রিয় বকেয়া পাওনা নেই।");
            }
        }

        // Render sections based on active tab
        renderDashboardTopCustomers(allBaki);
        renderDashboardRecentTransactions(allBaki);
        renderCustomerDirectory();
        renderTransactionsFeed();
        renderReportsAndAging(allBaki, totalDue, totalCollected);
        renderRemindersList(allBaki);
    }

    private void renderDashboardTopCustomers(List<BakiModel> allBaki) {
        if (binding.layoutBakiTopCustomersList == null) return;
        binding.layoutBakiTopCustomersList.removeAllViews();

        List<BakiModel> sorted = new ArrayList<>(allBaki);
        Collections.sort(sorted, (o1, o2) -> Double.compare(o2.getAmount(), o1.getAmount()));

        int count = 0;
        for (BakiModel item : sorted) {
            if (item.getAmount() <= 0) continue;
            binding.layoutBakiTopCustomersList.addView(createCustomerCardView(item, false));
            count++;
            if (count >= 3) break;
        }

        if (count == 0) {
            TextView empty = new TextView(activity);
            empty.setText("কোনো বকেয়া বাকি নেই");
            empty.setTextColor(Color.parseColor("#64748B"));
            empty.setTextSize(12.5f);
            empty.setPadding(0, dpToPx(6), 0, dpToPx(6));
            binding.layoutBakiTopCustomersList.addView(empty);
        }
    }

    private void renderDashboardRecentTransactions(List<BakiModel> allBaki) {
        if (binding.layoutBakiRecentTransactionsList == null) return;
        binding.layoutBakiRecentTransactionsList.removeAllViews();

        List<CombinedTx> allTx = getAllCombinedTransactions(allBaki);
        int count = 0;
        for (CombinedTx ctx : allTx) {
            binding.layoutBakiRecentTransactionsList.addView(createTransactionItemView(ctx));
            count++;
            if (count >= 4) break;
        }

        if (count == 0) {
            TextView empty = new TextView(activity);
            empty.setText("কোনো লেনদেন রেকর্ড নেই");
            empty.setTextColor(Color.parseColor("#64748B"));
            empty.setTextSize(12.5f);
            empty.setPadding(0, dpToPx(6), 0, dpToPx(6));
            binding.layoutBakiRecentTransactionsList.addView(empty);
        }
    }

    private void renderCustomerDirectory() {
        if (binding.layoutBakiCustomerCardsList == null) return;
        binding.layoutBakiCustomerCardsList.removeAllViews();

        StorageManager storage = StorageManager.getInstance(activity);
        List<BakiModel> allBaki = storage.loadBakiRecords();
        List<BakiModel> filtered = new ArrayList<>();

        for (BakiModel b : allBaki) {
            boolean matchesSearch = customerSearchQuery.isEmpty()
                    || (b.getCustomerName() != null && b.getCustomerName().toLowerCase().contains(customerSearchQuery))
                    || (b.getPhone() != null && b.getPhone().toLowerCase().contains(customerSearchQuery))
                    || (b.getDetails() != null && b.getDetails().toLowerCase().contains(customerSearchQuery));
            if (!matchesSearch) continue;

            if ("DUE".equals(customerFilter)) {
                if (b.getAmount() > 0) filtered.add(b);
            } else if ("PAID".equals(customerFilter)) {
                if (b.getAmount() <= 0) filtered.add(b);
            } else if ("OVERDUE".equals(customerFilter)) {
                if (b.getAmount() > 0 && isOverdue(b.getDueDate())) filtered.add(b);
            } else {
                filtered.add(b);
            }
        }

        if ("HIGHEST".equals(customerFilter)) {
            Collections.sort(filtered, (o1, o2) -> Double.compare(o2.getAmount(), o1.getAmount()));
        }

        if (filtered.isEmpty()) {
            if (binding.layoutBakiCustomerEmptyState != null) binding.layoutBakiCustomerEmptyState.setVisibility(View.VISIBLE);
            binding.layoutBakiCustomerCardsList.setVisibility(View.GONE);
        } else {
            if (binding.layoutBakiCustomerEmptyState != null) binding.layoutBakiCustomerEmptyState.setVisibility(View.GONE);
            binding.layoutBakiCustomerCardsList.setVisibility(View.VISIBLE);
            for (BakiModel b : filtered) {
                binding.layoutBakiCustomerCardsList.addView(createCustomerCardView(b, true));
            }
        }
    }

    private void renderTransactionsFeed() {
        if (binding.layoutBakiAllTransactionsList == null) return;
        binding.layoutBakiAllTransactionsList.removeAllViews();

        StorageManager storage = StorageManager.getInstance(activity);
        List<BakiModel> allBaki = storage.loadBakiRecords();
        List<CombinedTx> allTx = getAllCombinedTransactions(allBaki);
        List<CombinedTx> filtered = new ArrayList<>();

        for (CombinedTx ctx : allTx) {
            boolean matchesSearch = txSearchQuery.isEmpty()
                    || (ctx.customer.getCustomerName() != null && ctx.customer.getCustomerName().toLowerCase().contains(txSearchQuery))
                    || (ctx.tx.getNote() != null && ctx.tx.getNote().toLowerCase().contains(txSearchQuery))
                    || (ctx.tx.getDate() != null && ctx.tx.getDate().toLowerCase().contains(txSearchQuery));
            if (!matchesSearch) continue;

            if ("DUE".equals(txFilter)) {
                if ("BAKI".equalsIgnoreCase(ctx.tx.getType())) filtered.add(ctx);
            } else if ("JOMA".equals(txFilter)) {
                if ("JOMA".equalsIgnoreCase(ctx.tx.getType())) filtered.add(ctx);
            } else {
                filtered.add(ctx);
            }
        }

        if (filtered.isEmpty()) {
            if (binding.layoutBakiTxEmptyState != null) binding.layoutBakiTxEmptyState.setVisibility(View.VISIBLE);
            binding.layoutBakiAllTransactionsList.setVisibility(View.GONE);
        } else {
            if (binding.layoutBakiTxEmptyState != null) binding.layoutBakiTxEmptyState.setVisibility(View.GONE);
            binding.layoutBakiAllTransactionsList.setVisibility(View.VISIBLE);
            for (CombinedTx ctx : filtered) {
                binding.layoutBakiAllTransactionsList.addView(createTransactionItemView(ctx));
            }
        }
    }

    private void renderReportsAndAging(List<BakiModel> allBaki, double totalDue, double totalCollected) {
        if (binding.tvReportTotalDue != null) {
            binding.tvReportTotalDue.setText(String.format(Locale.getDefault(), "৳ %,.0f", totalDue));
        }
        if (binding.tvReportTotalCollected != null) {
            binding.tvReportTotalCollected.setText(String.format(Locale.getDefault(), "৳ %,.0f", totalCollected));
        }

        // Calculate Aging
        double aging1to7Amt = 0.0;
        int aging1to7Count = 0;
        double aging8to30Amt = 0.0;
        int aging8to30Count = 0;
        double aging30plusAmt = 0.0;
        int aging30plusCount = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long nowMs = cal.getTimeInMillis();

        for (BakiModel b : allBaki) {
            if (b.getAmount() <= 0) continue;
            long recordDateMs = nowMs;
            try {
                if (b.getDate() != null && !b.getDate().isEmpty()) {
                    Date d = sdf.parse(b.getDate());
                    if (d != null) recordDateMs = d.getTime();
                }
            } catch (Exception ignored) {}

            long diffDays = (nowMs - recordDateMs) / (1000L * 60L * 60L * 24L);
            if (diffDays <= 7) {
                aging1to7Amt += b.getAmount();
                aging1to7Count++;
            } else if (diffDays <= 30) {
                aging8to30Amt += b.getAmount();
                aging8to30Count++;
            } else {
                aging30plusAmt += b.getAmount();
                aging30plusCount++;
            }
        }

        if (binding.tvAging1to7 != null) {
            binding.tvAging1to7.setText(String.format(Locale.getDefault(), "৳ %,.0f (%s জন)", aging1to7Amt, toBengaliDigits(String.valueOf(aging1to7Count))));
        }
        if (binding.tvAging8to30 != null) {
            binding.tvAging8to30.setText(String.format(Locale.getDefault(), "৳ %,.0f (%s জন)", aging8to30Amt, toBengaliDigits(String.valueOf(aging8to30Count))));
        }
        if (binding.tvAging30plus != null) {
            binding.tvAging30plus.setText(String.format(Locale.getDefault(), "৳ %,.0f (%s জন)", aging30plusAmt, toBengaliDigits(String.valueOf(aging30plusCount))));
        }

        // Debtors Ranking list
        if (binding.layoutBakiDebtorsRanking != null) {
            binding.layoutBakiDebtorsRanking.removeAllViews();
            List<BakiModel> sorted = new ArrayList<>(allBaki);
            Collections.sort(sorted, (o1, o2) -> Double.compare(o2.getAmount(), o1.getAmount()));

            int rank = 1;
            for (BakiModel b : sorted) {
                if (b.getAmount() <= 0) continue;

                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dpToPx(6), 0, dpToPx(6));

                TextView tvRank = new TextView(activity);
                tvRank.setText("#" + toBengaliDigits(String.valueOf(rank)));
                tvRank.setTextSize(12.0f);
                tvRank.setTypeface(null, Typeface.BOLD);
                tvRank.setTextColor(rank == 1 ? Color.parseColor("#DC2626") : Color.parseColor("#64748B"));
                tvRank.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(32), ViewGroup.LayoutParams.WRAP_CONTENT));
                row.addView(tvRank);

                TextView tvName = new TextView(activity);
                tvName.setText(b.getCustomerName());
                tvName.setTextSize(13.0f);
                tvName.setTypeface(null, Typeface.BOLD);
                tvName.setTextColor(Color.parseColor("#0F172A"));
                tvName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
                row.addView(tvName);

                TextView tvAmt = new TextView(activity);
                tvAmt.setText(String.format(Locale.getDefault(), "৳ %,.0f", b.getAmount()));
                tvAmt.setTextSize(13.0f);
                tvAmt.setTypeface(null, Typeface.BOLD);
                tvAmt.setTextColor(Color.parseColor("#DC2626"));
                row.addView(tvAmt);

                binding.layoutBakiDebtorsRanking.addView(row);
                rank++;
                if (rank > 10) break;
            }

            if (rank == 1) {
                TextView empty = new TextView(activity);
                empty.setText("কোনো বকেয়া বাকিদার নেই");
                empty.setTextColor(Color.parseColor("#64748B"));
                empty.setTextSize(12.0f);
                empty.setPadding(0, dpToPx(4), 0, dpToPx(4));
                binding.layoutBakiDebtorsRanking.addView(empty);
            }
        }
    }

    private void renderRemindersList(List<BakiModel> allBaki) {
        if (binding.layoutBakiRemindersList == null) return;
        binding.layoutBakiRemindersList.removeAllViews();

        List<BakiModel> dueList = new ArrayList<>();
        for (BakiModel b : allBaki) {
            if (b.getAmount() > 0) dueList.add(b);
        }

        if (dueList.isEmpty()) {
            if (binding.layoutBakiRemindersEmptyState != null) binding.layoutBakiRemindersEmptyState.setVisibility(View.VISIBLE);
            binding.layoutBakiRemindersList.setVisibility(View.GONE);
        } else {
            if (binding.layoutBakiRemindersEmptyState != null) binding.layoutBakiRemindersEmptyState.setVisibility(View.GONE);
            binding.layoutBakiRemindersList.setVisibility(View.VISIBLE);

            for (BakiModel item : dueList) {
                MaterialCardView card = new MaterialCardView(activity);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, dpToPx(8));
                card.setLayoutParams(params);
                card.setRadius(dpToPx(14));
                card.setCardElevation(dpToPx(0.5f));
                card.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
                card.setStrokeColor(Color.parseColor("#E2E8F0"));
                card.setStrokeWidth(dpToPx(1));
                card.setContentPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

                LinearLayout main = new LinearLayout(activity);
                main.setOrientation(LinearLayout.VERTICAL);

                // Top row: Info & Amount
                LinearLayout top = new LinearLayout(activity);
                top.setOrientation(LinearLayout.HORIZONTAL);
                top.setGravity(Gravity.CENTER_VERTICAL);

                LinearLayout left = new LinearLayout(activity);
                left.setOrientation(LinearLayout.VERTICAL);
                left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

                TextView tvName = new TextView(activity);
                tvName.setText(item.getCustomerName());
                tvName.setTextSize(14.0f);
                tvName.setTypeface(null, Typeface.BOLD);
                tvName.setTextColor(Color.parseColor("#0F172A"));
                left.addView(tvName);

                TextView tvPhone = new TextView(activity);
                tvPhone.setText(item.getPhone() != null && !item.getPhone().isEmpty() ? item.getPhone() : "নম্বর নেই");
                tvPhone.setTextSize(11.5f);
                tvPhone.setTextColor(Color.parseColor("#64748B"));
                left.addView(tvPhone);

                top.addView(left);

                TextView tvAmt = new TextView(activity);
                tvAmt.setText(String.format(Locale.getDefault(), "৳ %,.0f", item.getAmount()));
                tvAmt.setTextSize(15.0f);
                tvAmt.setTypeface(null, Typeface.BOLD);
                tvAmt.setTextColor(Color.parseColor("#DC2626"));
                top.addView(tvAmt);

                main.addView(top);

                // Buttons row: WhatsApp, SMS, Copy
                LinearLayout btnRow = new LinearLayout(activity);
                btnRow.setOrientation(LinearLayout.HORIZONTAL);
                btnRow.setGravity(Gravity.END);
                btnRow.setPadding(0, dpToPx(8), 0, 0);

                MaterialButton btnCopy = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                btnCopy.setText("কপি");
                btnCopy.setTextSize(11.0f);
                btnCopy.setPadding(dpToPx(8), 0, dpToPx(8), 0);
                btnCopy.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(34)));
                btnCopy.setCornerRadius(dpToPx(8));
                btnCopy.setOnClickListener(v -> copyReminderMessage(item));
                btnRow.addView(btnCopy);

                MaterialButton btnSms = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                btnSms.setText("মেসেজ");
                btnSms.setTextSize(11.0f);
                btnSms.setPadding(dpToPx(8), 0, dpToPx(8), 0);
                LinearLayout.LayoutParams smsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(34));
                smsParams.setMargins(dpToPx(6), 0, 0, 0);
                btnSms.setLayoutParams(smsParams);
                btnSms.setCornerRadius(dpToPx(8));
                btnSms.setOnClickListener(v -> sendSmsReminder(item));
                btnRow.addView(btnSms);

                MaterialButton btnWa = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonStyle);
                btnWa.setText("WhatsApp তাগাদা");
                btnWa.setTextSize(11.0f);
                btnWa.setPadding(dpToPx(10), 0, dpToPx(10), 0);
                LinearLayout.LayoutParams waParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(34));
                waParams.setMargins(dpToPx(6), 0, 0, 0);
                btnWa.setLayoutParams(waParams);
                btnWa.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#25D366")));
                btnWa.setCornerRadius(dpToPx(8));
                btnWa.setOnClickListener(v -> sendWhatsAppReminder(item));
                btnRow.addView(btnWa);

                main.addView(btnRow);
                card.addView(main);
                binding.layoutBakiRemindersList.addView(card);
            }
        }
    }

    // =========================================================================
    // UI COMPONENT BUILDERS (Customer Cards, Tx items)
    // =========================================================================

    private View createCustomerCardView(BakiModel item, boolean showFullActions) {
        MaterialCardView card = new MaterialCardView(activity);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(16));
        card.setCardElevation(dpToPx(0.5f));
        card.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
        card.setStrokeColor(Color.parseColor(item.getAmount() > 0 ? "#FEE2E2" : "#E2E8F0"));
        card.setStrokeWidth(dpToPx(1));
        card.setContentPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

        LinearLayout mainLayout = new LinearLayout(activity);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        // Top Row: Avatar, Name & Phone, Due Amount
        LinearLayout topRow = new LinearLayout(activity);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        // Avatar
        TextView avatar = new TextView(activity);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
        avatarParams.setMargins(0, 0, dpToPx(10), 0);
        avatar.setLayoutParams(avatarParams);
        avatar.setGravity(Gravity.CENTER);
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(15.0f);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setBackground(createCircleDrawable(item.getCustomerName()));
        avatar.setText(getInitials(item.getCustomerName()));
        topRow.addView(avatar);

        // Text Info Container
        LinearLayout textContainer = new LinearLayout(activity);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView txtName = new TextView(activity);
        txtName.setText(item.getCustomerName());
        txtName.setTextSize(14.5f);
        txtName.setTypeface(null, Typeface.BOLD);
        txtName.setTextColor(Color.parseColor("#0F172A"));
        textContainer.addView(txtName);

        if (item.getPhone() != null && !item.getPhone().trim().isEmpty()) {
            TextView txtPhone = new TextView(activity);
            txtPhone.setText(item.getPhone());
            txtPhone.setTextSize(12.0f);
            txtPhone.setTextColor(Color.parseColor("#2563EB"));
            txtPhone.setPadding(0, dpToPx(1), 0, 0);
            txtPhone.setOnClickListener(v -> makeCustomerCall(item));
            textContainer.addView(txtPhone);
        }

        if (item.getDetails() != null && !item.getDetails().trim().isEmpty()) {
            TextView txtDetails = new TextView(activity);
            txtDetails.setText(item.getDetails());
            txtDetails.setTextSize(11.0f);
            txtDetails.setTextColor(Color.parseColor("#64748B"));
            txtDetails.setPadding(0, dpToPx(2), 0, 0);
            textContainer.addView(txtDetails);
        }

        // Date & Overdue Badge Row
        LinearLayout metaRow = new LinearLayout(activity);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setGravity(Gravity.CENTER_VERTICAL);
        metaRow.setPadding(0, dpToPx(2), 0, 0);

        TextView txtDate = new TextView(activity);
        txtDate.setText(item.getDate() != null ? item.getDate() : "আজ");
        txtDate.setTextSize(10.5f);
        txtDate.setTextColor(Color.parseColor("#94A3B8"));
        metaRow.addView(txtDate);

        if (item.getDueDate() != null && !item.getDueDate().trim().isEmpty()) {
            boolean overdue = isOverdue(item.getDueDate());
            TextView txtDueDate = new TextView(activity);
            txtDueDate.setText(overdue ? " • মেয়াদ শেষ (" + item.getDueDate() + ")" : " • মেয়াদ: " + item.getDueDate());
            txtDueDate.setTextSize(10.5f);
            txtDueDate.setTextColor(Color.parseColor(overdue ? "#DC2626" : "#D97706"));
            txtDueDate.setTypeface(null, overdue ? Typeface.BOLD : Typeface.NORMAL);
            metaRow.addView(txtDueDate);
        }

        textContainer.addView(metaRow);
        topRow.addView(textContainer);

        // Right side: Due Amount & Tx count
        LinearLayout amountContainer = new LinearLayout(activity);
        amountContainer.setOrientation(LinearLayout.VERTICAL);
        amountContainer.setGravity(Gravity.END);

        TextView txtAmount = new TextView(activity);
        txtAmount.setText(String.format(Locale.getDefault(), "৳ %,.0f", item.getAmount()));
        txtAmount.setTextSize(16.0f);
        txtAmount.setTypeface(null, Typeface.BOLD);
        txtAmount.setTextColor(Color.parseColor(item.getAmount() > 0 ? "#DC2626" : "#059669"));
        amountContainer.addView(txtAmount);

        int txCount = item.getTransactions() != null ? item.getTransactions().size() : 0;
        if (txCount > 0) {
            TextView txtTxCount = new TextView(activity);
            txtTxCount.setText(toBengaliDigits(String.valueOf(txCount)) + "টি লেনদেন");
            txtTxCount.setTextSize(10.0f);
            txtTxCount.setTextColor(Color.parseColor("#94A3B8"));
            amountContainer.addView(txtTxCount);
        }

        topRow.addView(amountContainer);
        mainLayout.addView(topRow);

        // Divider
        View divider = new View(activity);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
        divParams.setMargins(0, dpToPx(10), 0, dpToPx(8));
        divider.setLayoutParams(divParams);
        mainLayout.addView(divider);

        // Action Buttons Row
        HorizontalScrollView actionScroll = new HorizontalScrollView(activity);
        actionScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout actionRow = new LinearLayout(activity);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setGravity(Gravity.CENTER_VERTICAL);

        // 1. Pay (জমা নিন) Button
        MaterialButton btnPay = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonStyle);
        btnPay.setText("জমা নিন");
        btnPay.setTextSize(11.0f);
        btnPay.setPadding(dpToPx(10), 0, dpToPx(10), 0);
        LinearLayout.LayoutParams btnPayParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(34));
        btnPayParams.setMargins(0, 0, dpToPx(6), 0);
        btnPay.setLayoutParams(btnPayParams);
        btnPay.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#059669")));
        btnPay.setTextColor(Color.WHITE);
        btnPay.setCornerRadius(dpToPx(8));
        btnPay.setOnClickListener(v -> showReceivePaymentDialog(item));
        actionRow.addView(btnPay);

        // 2. Add Due (+ বাকি) Button
        MaterialButton btnAddDue = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnAddDue.setText("+ বাকি");
        btnAddDue.setTextSize(11.0f);
        btnAddDue.setPadding(dpToPx(8), 0, dpToPx(8), 0);
        LinearLayout.LayoutParams btnAddDueParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(34));
        btnAddDueParams.setMargins(0, 0, dpToPx(6), 0);
        btnAddDue.setLayoutParams(btnAddDueParams);
        btnAddDue.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EA580C")));
        btnAddDue.setStrokeWidth(dpToPx(1));
        btnAddDue.setTextColor(Color.parseColor("#EA580C"));
        btnAddDue.setCornerRadius(dpToPx(8));
        btnAddDue.setOnClickListener(v -> showAddDueDialog(item));
        actionRow.addView(btnAddDue);

        // 3. Ledger History (খতিয়ান ও স্টেটমেন্ট) Button
        MaterialButton btnLedger = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnLedger.setText("খতিয়ান");
        btnLedger.setTextSize(11.0f);
        btnLedger.setPadding(dpToPx(8), 0, dpToPx(8), 0);
        LinearLayout.LayoutParams btnLedgerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(34));
        btnLedgerParams.setMargins(0, 0, dpToPx(6), 0);
        btnLedger.setLayoutParams(btnLedgerParams);
        btnLedger.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#6366F1")));
        btnLedger.setStrokeWidth(dpToPx(1));
        btnLedger.setTextColor(Color.parseColor("#6366F1"));
        btnLedger.setCornerRadius(dpToPx(8));
        btnLedger.setIcon(ContextCompat.getDrawable(activity, R.drawable.ic_notebook));
        btnLedger.setIconSize(dpToPx(12));
        btnLedger.setIconTint(ColorStateList.valueOf(Color.parseColor("#6366F1")));
        btnLedger.setOnClickListener(v -> showCustomerProfileAndLedgerDialog(item));
        actionRow.addView(btnLedger);

        // 4. Call (কল) Button
        MaterialButton btnCall = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnCall.setText("কল");
        btnCall.setTextSize(11.0f);
        btnCall.setPadding(dpToPx(8), 0, dpToPx(8), 0);
        LinearLayout.LayoutParams btnCallParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(34));
        btnCallParams.setMargins(0, 0, dpToPx(6), 0);
        btnCall.setLayoutParams(btnCallParams);
        btnCall.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        btnCall.setStrokeWidth(dpToPx(1));
        btnCall.setTextColor(Color.parseColor("#2563EB"));
        btnCall.setCornerRadius(dpToPx(8));
        btnCall.setIcon(ContextCompat.getDrawable(activity, R.drawable.ic_phone_call));
        btnCall.setIconSize(dpToPx(12));
        btnCall.setIconTint(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        btnCall.setOnClickListener(v -> makeCustomerCall(item));
        actionRow.addView(btnCall);

        // 5. WhatsApp / Reminder (তাগাদা) Button
        MaterialButton btnShare = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnShare.setText("তাগাদা");
        btnShare.setTextSize(11.0f);
        btnShare.setPadding(dpToPx(8), 0, dpToPx(8), 0);
        LinearLayout.LayoutParams btnShareParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(34));
        btnShareParams.setMargins(0, 0, dpToPx(6), 0);
        btnShare.setLayoutParams(btnShareParams);
        btnShare.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#D97706")));
        btnShare.setStrokeWidth(dpToPx(1));
        btnShare.setTextColor(Color.parseColor("#D97706"));
        btnShare.setCornerRadius(dpToPx(8));
        btnShare.setIcon(ContextCompat.getDrawable(activity, R.drawable.ic_share));
        btnShare.setIconSize(dpToPx(12));
        btnShare.setIconTint(ColorStateList.valueOf(Color.parseColor("#D97706")));
        btnShare.setOnClickListener(v -> sendWhatsAppReminder(item));
        actionRow.addView(btnShare);

        if (showFullActions) {
            // 6. Delete Button
            MaterialButton btnDelete = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnDelete.setText("");
            LinearLayout.LayoutParams btnDeleteParams = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(34));
            btnDelete.setLayoutParams(btnDeleteParams);
            btnDelete.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EF4444")));
            btnDelete.setStrokeWidth(dpToPx(1));
            btnDelete.setCornerRadius(dpToPx(8));
            btnDelete.setIcon(ContextCompat.getDrawable(activity, R.drawable.ic_trash));
            btnDelete.setIconSize(dpToPx(14));
            btnDelete.setIconPadding(0);
            btnDelete.setIconTint(ColorStateList.valueOf(Color.parseColor("#EF4444")));
            btnDelete.setOnClickListener(v -> deleteBakiRecord(item));
            actionRow.addView(btnDelete);
        }

        actionScroll.addView(actionRow);
        mainLayout.addView(actionScroll);

        card.addView(mainLayout);
        return card;
    }

    private View createTransactionItemView(CombinedTx ctx) {
        boolean isJoma = "JOMA".equalsIgnoreCase(ctx.tx.getType());

        MaterialCardView txCard = new MaterialCardView(activity);
        LinearLayout.LayoutParams tcParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tcParams.setMargins(0, 0, 0, dpToPx(8));
        txCard.setLayoutParams(tcParams);
        txCard.setRadius(dpToPx(12));
        txCard.setCardElevation(dpToPx(0.5f));
        txCard.setCardBackgroundColor(Color.parseColor(isJoma ? "#F0FDF4" : "#FEF2F2"));
        txCard.setStrokeColor(Color.parseColor(isJoma ? "#BBF7D0" : "#FECACA"));
        txCard.setStrokeWidth(dpToPx(1));
        txCard.setContentPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

        LinearLayout txRow = new LinearLayout(activity);
        txRow.setOrientation(LinearLayout.HORIZONTAL);
        txRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout left = new LinearLayout(activity);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView tvCustName = new TextView(activity);
        tvCustName.setText(ctx.customer.getCustomerName());
        tvCustName.setTextSize(13.5f);
        tvCustName.setTypeface(null, Typeface.BOLD);
        tvCustName.setTextColor(Color.parseColor("#0F172A"));
        left.addView(tvCustName);

        TextView tvType = new TextView(activity);
        tvType.setText((isJoma ? "টাকা জমা: " : "বাকি যোগ: ") + (ctx.tx.getNote() != null ? ctx.tx.getNote() : ""));
        tvType.setTextSize(11.5f);
        tvType.setTextColor(Color.parseColor(isJoma ? "#16A34A" : "#DC2626"));
        left.addView(tvType);

        TextView tvTime = new TextView(activity);
        tvTime.setText(ctx.tx.getDate() + (ctx.tx.getTime() != null ? " " + ctx.tx.getTime() : ""));
        tvTime.setTextSize(10.5f);
        tvTime.setTextColor(Color.parseColor("#64748B"));
        left.addView(tvTime);

        txRow.addView(left);

        LinearLayout right = new LinearLayout(activity);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);

        TextView tvAmt = new TextView(activity);
        tvAmt.setText((isJoma ? "- ৳ " : "+ ৳ ") + PdfExporter.formatBengaliNumber(ctx.tx.getAmount()));
        tvAmt.setTextSize(14.0f);
        tvAmt.setTypeface(null, Typeface.BOLD);
        tvAmt.setTextColor(Color.parseColor(isJoma ? "#16A34A" : "#DC2626"));
        right.addView(tvAmt);

        TextView tvBalAfter = new TextView(activity);
        tvBalAfter.setText("অবশিষ্ট: ৳ " + PdfExporter.formatBengaliNumber(ctx.tx.getBalanceAfter()));
        tvBalAfter.setTextSize(10.0f);
        tvBalAfter.setTextColor(Color.parseColor("#64748B"));
        right.addView(tvBalAfter);

        txRow.addView(right);
        txCard.addView(txRow);

        txCard.setOnClickListener(v -> showCustomerProfileAndLedgerDialog(ctx.customer));
        return txCard;
    }

    // =========================================================================
    // MODAL DIALOG FLOWS (Add Due, Receive Payment, New Customer, Profile & Ledger)
    // =========================================================================

    public void showAddDueDialog(BakiModel preselectedCustomer) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_baki_due, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .create();

        MaterialAutoCompleteTextView actvName = dialogView.findViewById(R.id.actvDlgDueCustomerName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etDlgDuePhone);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etDlgDueAmount);
        MaterialAutoCompleteTextView actvDetails = dialogView.findViewById(R.id.actvDlgDueDetails);
        TextInputEditText etDueDate = dialogView.findViewById(R.id.etDlgDueDate);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnDlgSaveDue);
        TextInputLayout tilName = dialogView.findViewById(R.id.tilDlgDueCustomerName);
        TextInputLayout tilAmount = dialogView.findViewById(R.id.tilDlgDueAmount);

        StorageManager storage = StorageManager.getInstance(activity);
        List<BakiModel> allBaki = storage.loadBakiRecords();

        // Autocomplete for customer names
        Set<String> nameSet = new HashSet<>();
        for (BakiModel b : allBaki) {
            if (b.getCustomerName() != null && !b.getCustomerName().trim().isEmpty()) {
                nameSet.add(b.getCustomerName().trim());
            }
        }
        ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(nameSet));
        actvName.setAdapter(nameAdapter);

        // Product suggestions autocomplete
        List<String> productSuggestions = storage.getAllProductSuggestionsWithDefaults();
        ArrayAdapter<String> prodAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, productSuggestions);
        actvDetails.setAdapter(prodAdapter);

        // Pre-fill if preselected
        if (preselectedCustomer != null) {
            actvName.setText(preselectedCustomer.getCustomerName());
            if (preselectedCustomer.getPhone() != null) etPhone.setText(preselectedCustomer.getPhone());
        }

        // Date picker for due date
        etDueDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(activity, (view, year, month, dayOfMonth) -> {
                String dateStr = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                etDueDate.setText(dateStr);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        CheckBox cbAddToDailySale = dialogView.findViewById(R.id.cbDlgDueAddToDailySale);

        btnSave.setOnClickListener(v -> {
            String name = actvName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String amtStr = etAmount.getText().toString().trim();
            String details = actvDetails.getText().toString().trim();
            String dueDate = etDueDate.getText().toString().trim();
            boolean addToDailySale = cbAddToDailySale != null && cbAddToDailySale.isChecked();

            if (name.isEmpty()) {
                tilName.setError("খরিদ্দারের নাম লিখুন");
                return;
            }
            tilName.setError(null);

            if (amtStr.isEmpty()) {
                tilAmount.setError("বাকির টাকার পরিমাণ লিখুন");
                return;
            }
            tilAmount.setError(null);

            try {
                double amount = Double.parseDouble(amtStr);
                if (amount <= 0) {
                    tilAmount.setError("সঠিক টাকার পরিমাণ লিখুন");
                    return;
                }

                List<BakiModel> list = storage.loadBakiRecords();
                String curDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                String curTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

                BakiModel existing = null;
                for (BakiModel b : list) {
                    if (b.getCustomerName() != null && b.getCustomerName().trim().equalsIgnoreCase(name)) {
                        existing = b;
                        break;
                    }
                }

                if (existing != null) {
                    double newTotal = existing.getAmount() + amount;
                    existing.setAmount(newTotal);
                    if (!phone.isEmpty()) existing.setPhone(phone);
                    if (!dueDate.isEmpty()) existing.setDueDate(dueDate);
                    if (!details.isEmpty()) existing.setDetails(details);
                    existing.setDate(curDate);

                    BakiTransaction tx = new BakiTransaction(UUID.randomUUID().toString(), curDate, curTime, "BAKI", amount, details.isEmpty() ? "বাকি যোগ" : details, newTotal, addToDailySale);
                    existing.addTransaction(tx);
                } else {
                    String id = UUID.randomUUID().toString();
                    BakiModel newRecord = new BakiModel(id, name, phone, amount, curDate, dueDate, details);
                    BakiTransaction tx = new BakiTransaction(UUID.randomUUID().toString(), curDate, curTime, "BAKI", amount, details.isEmpty() ? "নতুন বাকি শুরু" : details, amount, addToDailySale);
                    newRecord.addTransaction(tx);
                    list.add(0, newRecord);
                }

                // If details contains product, save to product suggestions
                if (!details.isEmpty()) {
                    storage.saveProductSuggestion(details);
                }

                storage.saveBakiRecords(list);
                Toast.makeText(activity, "৳ " + PdfExporter.formatBengaliNumber(amount) + " বাকি হিসাবে যোগ করা হয়েছে!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                updateUI();
                activity.triggerAutoCloudBackup();
            } catch (Exception e) {
                tilAmount.setError("সঠিক সংখ্যা হতে হবে");
            }
        });

        dialog.show();
    }

    public void showReceivePaymentDialog(BakiModel preselectedCustomer) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_receive_baki_payment, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .create();

        MaterialCardView cardInfo = dialogView.findViewById(R.id.cardDlgPayCustomerInfo);
        TextView tvCustName = dialogView.findViewById(R.id.tvDlgPayCustomerName);
        TextView tvCurDue = dialogView.findViewById(R.id.tvDlgPayCurrentDue);
        TextInputLayout tilSelect = dialogView.findViewById(R.id.tilDlgPayCustomerSelect);
        MaterialAutoCompleteTextView actvSelect = dialogView.findViewById(R.id.actvDlgPayCustomerSelect);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etDlgPayAmount);
        TextInputEditText etNote = dialogView.findViewById(R.id.etDlgPayNote);
        CheckBox cbDailyCash = dialogView.findViewById(R.id.cbDlgPayAddToDailyCash);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnDlgConfirmPay);

        MaterialButton btnCash = dialogView.findViewById(R.id.btnPayMethodCash);
        MaterialButton btnBkash = dialogView.findViewById(R.id.btnPayMethodBkash);
        MaterialButton btnNagad = dialogView.findViewById(R.id.btnPayMethodNagad);
        MaterialButton btnBank = dialogView.findViewById(R.id.btnPayMethodBank);

        final String[] paymentMethod = {"ক্যাশ"};

        View.OnClickListener methodListener = v -> {
            int id = v.getId();
            if (id == R.id.btnPayMethodCash) {
                paymentMethod[0] = "ক্যাশ";
                applyMethodTabStyles(btnCash, btnBkash, btnNagad, btnBank, 0);
            } else if (id == R.id.btnPayMethodBkash) {
                paymentMethod[0] = "বিকাশ";
                applyMethodTabStyles(btnCash, btnBkash, btnNagad, btnBank, 1);
            } else if (id == R.id.btnPayMethodNagad) {
                paymentMethod[0] = "নগদ";
                applyMethodTabStyles(btnCash, btnBkash, btnNagad, btnBank, 2);
            } else if (id == R.id.btnPayMethodBank) {
                paymentMethod[0] = "ব্যাংক";
                applyMethodTabStyles(btnCash, btnBkash, btnNagad, btnBank, 3);
            }
        };

        btnCash.setOnClickListener(methodListener);
        btnBkash.setOnClickListener(methodListener);
        btnNagad.setOnClickListener(methodListener);
        btnBank.setOnClickListener(methodListener);

        StorageManager storage = StorageManager.getInstance(activity);
        List<BakiModel> allBaki = storage.loadBakiRecords();

        final BakiModel[] selectedCustomer = {preselectedCustomer};

        if (preselectedCustomer != null) {
            cardInfo.setVisibility(View.VISIBLE);
            tilSelect.setVisibility(View.GONE);
            tvCustName.setText("খরিদ্দার: " + preselectedCustomer.getCustomerName());
            tvCurDue.setText("বর্তমান বকেয়া: ৳ " + PdfExporter.formatBengaliNumber(preselectedCustomer.getAmount()));
            etAmount.setText(String.format(Locale.US, "%.0f", preselectedCustomer.getAmount()));
        } else {
            cardInfo.setVisibility(View.GONE);
            tilSelect.setVisibility(View.VISIBLE);

            List<String> customerNames = new ArrayList<>();
            for (BakiModel b : allBaki) {
                if (b.getAmount() > 0) {
                    customerNames.add(b.getCustomerName() + " (৳ " + PdfExporter.formatBengaliNumber(b.getAmount()) + ")");
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, customerNames);
            actvSelect.setAdapter(adapter);

            actvSelect.setOnItemClickListener((parent, view, position, id) -> {
                String selectedStr = (String) parent.getItemAtPosition(position);
                for (BakiModel b : allBaki) {
                    if (selectedStr.startsWith(b.getCustomerName())) {
                        selectedCustomer[0] = b;
                        etAmount.setText(String.format(Locale.US, "%.0f", b.getAmount()));
                        break;
                    }
                }
            });
        }

        btnConfirm.setOnClickListener(v -> {
            if (selectedCustomer[0] == null) {
                String enteredName = actvSelect.getText().toString().trim();
                for (BakiModel b : allBaki) {
                    if (b.getCustomerName().equalsIgnoreCase(enteredName)) {
                        selectedCustomer[0] = b;
                        break;
                    }
                }
            }

            if (selectedCustomer[0] == null) {
                Toast.makeText(activity, "অনুগ্রহ করে সঠিক গ্রাহক নির্বাচন করুন", Toast.LENGTH_SHORT).show();
                return;
            }

            String amtStr = etAmount.getText().toString().trim();
            if (amtStr.isEmpty()) {
                Toast.makeText(activity, "জমার পরিমাণ লিখুন", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double val = Double.parseDouble(amtStr);
                if (val <= 0) {
                    Toast.makeText(activity, "সঠিক জমার পরিমাণ লিখুন", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<BakiModel> list = storage.loadBakiRecords();
                for (BakiModel b : list) {
                    if (b.getId().equals(selectedCustomer[0].getId())) {
                        double newAmt = Math.max(0.0, b.getAmount() - val);
                        b.setAmount(newAmt);
                        String curDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                        String curTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                        b.setDate(curDate);

                        String note = etNote.getText().toString().trim();
                        String fullNote = (paymentMethod[0].equals("ক্যাশ") ? "নগদ জমা" : paymentMethod[0] + " মারফত জমা");
                        if (!note.isEmpty()) fullNote += " (" + note + ")";

                        BakiTransaction tx = new BakiTransaction(UUID.randomUUID().toString(), curDate, curTime, "JOMA", val, fullNote, newAmt);
                        b.addTransaction(tx);
                        break;
                    }
                }

                // Add to daily cash book if checked
                if (cbDailyCash.isChecked() && activity.getViewModel() != null) {
                    double curAvail = activity.getViewModel().getAvailableCash().getValue() != null ? activity.getViewModel().getAvailableCash().getValue().doubleValue() : 0.0;
                    activity.getViewModel().setAvailableCash(curAvail + val);
                }

                storage.saveBakiRecords(list);
                Toast.makeText(activity, "৳ " + PdfExporter.formatBengaliNumber(val) + " টাকা জমা নেওয়া হয়েছে!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                updateUI();
                activity.triggerAutoCloudBackup();
            } catch (Exception e) {
                Toast.makeText(activity, "সঠিক সংখ্যা লিখুন", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void applyMethodTabStyles(MaterialButton btn1, MaterialButton btn2, MaterialButton btn3, MaterialButton btn4, int selectedIdx) {
        MaterialButton[] btns = {btn1, btn2, btn3, btn4};
        for (int i = 0; i < btns.length; i++) {
            if (btns[i] == null) continue;
            if (i == selectedIdx) {
                btns[i].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#059669")));
                btns[i].setTextColor(Color.WHITE);
                btns[i].setStrokeColor(ColorStateList.valueOf(Color.parseColor("#059669")));
            } else {
                btns[i].setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                btns[i].setTextColor(Color.parseColor("#475569"));
                btns[i].setStrokeColor(ColorStateList.valueOf(Color.parseColor("#CBD5E1")));
                btns[i].setStrokeWidth(dpToPx(1));
            }
        }
    }

    public void showAddCustomerDialog() {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_customer, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .create();

        TextInputEditText etName = dialogView.findViewById(R.id.etDlgNewCustName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etDlgNewCustPhone);
        TextInputEditText etInitDue = dialogView.findViewById(R.id.etDlgNewCustInitialDue);
        TextInputEditText etAddress = dialogView.findViewById(R.id.etDlgNewCustAddress);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnDlgSaveNewCustomer);
        TextInputLayout tilName = dialogView.findViewById(R.id.tilDlgNewCustName);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String initDueStr = etInitDue.getText().toString().trim();
            String address = etAddress.getText().toString().trim();

            if (name.isEmpty()) {
                tilName.setError("নাম লিখুন");
                return;
            }
            tilName.setError(null);

            double initDue = 0.0;
            if (!initDueStr.isEmpty()) {
                try {
                    initDue = Double.parseDouble(initDueStr);
                } catch (Exception ignored) {}
            }

            StorageManager storage = StorageManager.getInstance(activity);
            List<BakiModel> list = storage.loadBakiRecords();
            String curDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            String curTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

            String id = UUID.randomUUID().toString();
            BakiModel newRecord = new BakiModel(id, name, phone, initDue, curDate, "", address);
            if (initDue > 0) {
                BakiTransaction tx = new BakiTransaction(UUID.randomUUID().toString(), curDate, curTime, "BAKI", initDue, "পূর্বের বকেয়া হিসাব", initDue);
                newRecord.addTransaction(tx);
            }
            list.add(0, newRecord);
            storage.saveBakiRecords(list);

            Toast.makeText(activity, "নতুন কাস্টমার প্রোফাইল তৈরি হয়েছে!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            updateUI();
            activity.triggerAutoCloudBackup();
        });

        dialog.show();
    }

    public void showCustomerProfileAndLedgerDialog(BakiModel customer) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_customer_profile_statement, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .create();

        TextView tvAvatar = dialogView.findViewById(R.id.tvDlgProfileAvatar);
        TextView tvName = dialogView.findViewById(R.id.tvDlgProfileName);
        TextView tvPhone = dialogView.findViewById(R.id.tvDlgProfilePhone);
        TextView tvDue = dialogView.findViewById(R.id.tvDlgProfileDueAmount);
        TextView tvDueDate = dialogView.findViewById(R.id.tvDlgProfileDueDate);
        TextView tvOverdueStatus = dialogView.findViewById(R.id.tvDlgProfileOverdueStatus);
        TextView tvTxCount = dialogView.findViewById(R.id.tvDlgProfileTxCount);
        LinearLayout layoutTxHistory = dialogView.findViewById(R.id.layoutDlgProfileTxHistory);

        MaterialButton btnAddDue = dialogView.findViewById(R.id.btnDlgProfileAddDue);
        MaterialButton btnPay = dialogView.findViewById(R.id.btnDlgProfileReceivePay);
        MaterialButton btnCall = dialogView.findViewById(R.id.btnDlgProfileCall);
        MaterialButton btnWhatsApp = dialogView.findViewById(R.id.btnDlgProfileWhatsApp);
        MaterialButton btnPdf = dialogView.findViewById(R.id.btnDlgProfileDownloadPdf);
        MaterialButton btnClose = dialogView.findViewById(R.id.btnDlgProfileClose);

        tvAvatar.setText(getInitials(customer.getCustomerName()));
        tvAvatar.setBackground(createCircleDrawable(customer.getCustomerName()));
        tvName.setText(customer.getCustomerName());
        tvPhone.setText(customer.getPhone() != null && !customer.getPhone().isEmpty() ? customer.getPhone() : "মোবাইল নম্বর নেই");
        tvDue.setText(String.format(Locale.getDefault(), "৳ %,.0f", customer.getAmount()));
        tvDueDate.setText("শেষ লেনদেন: " + (customer.getDate() != null ? customer.getDate() : "আজ"));

        if (customer.getDueDate() != null && !customer.getDueDate().isEmpty()) {
            boolean overdue = isOverdue(customer.getDueDate());
            tvOverdueStatus.setText(overdue ? "মেয়াদোত্তীর্ণ বকেয়া (" + customer.getDueDate() + ")" : "নির্ধারিত মেয়াদ: " + customer.getDueDate());
            tvOverdueStatus.setVisibility(View.VISIBLE);
        } else {
            tvOverdueStatus.setVisibility(View.GONE);
        }

        // Render Transaction History
        List<BakiTransaction> txList = customer.getTransactions();
        if (txList != null && !txList.isEmpty()) {
            tvTxCount.setText(toBengaliDigits(String.valueOf(txList.size())) + "টি এন্ট্রি");
            for (int i = txList.size() - 1; i >= 0; i--) {
                BakiTransaction tx = txList.get(i);
                boolean isJoma = "JOMA".equalsIgnoreCase(tx.getType());

                LinearLayout txRow = new LinearLayout(activity);
                txRow.setOrientation(LinearLayout.HORIZONTAL);
                txRow.setGravity(Gravity.CENTER_VERTICAL);
                txRow.setBackgroundColor(Color.parseColor(isJoma ? "#F0FDF4" : "#FEF2F2"));
                txRow.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                p.setMargins(0, 0, 0, dpToPx(6));
                txRow.setLayoutParams(p);

                LinearLayout left = new LinearLayout(activity);
                left.setOrientation(LinearLayout.VERTICAL);
                left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

                TextView tvType = new TextView(activity);
                tvType.setText((isJoma ? "জমা: " : "বাকি: ") + (tx.getNote() != null ? tx.getNote() : ""));
                tvType.setTextSize(12.0f);
                tvType.setTypeface(null, Typeface.BOLD);
                tvType.setTextColor(Color.parseColor(isJoma ? "#16A34A" : "#DC2626"));
                left.addView(tvType);

                TextView tvTime = new TextView(activity);
                tvTime.setText(tx.getDate() + (tx.getTime() != null ? " " + tx.getTime() : ""));
                tvTime.setTextSize(10.5f);
                tvTime.setTextColor(Color.parseColor("#64748B"));
                left.addView(tvTime);
                txRow.addView(left);

                LinearLayout right = new LinearLayout(activity);
                right.setOrientation(LinearLayout.VERTICAL);
                right.setGravity(Gravity.END);

                TextView tvAmt = new TextView(activity);
                tvAmt.setText((isJoma ? "- ৳ " : "+ ৳ ") + PdfExporter.formatBengaliNumber(tx.getAmount()));
                tvAmt.setTextSize(13.0f);
                tvAmt.setTypeface(null, Typeface.BOLD);
                tvAmt.setTextColor(Color.parseColor(isJoma ? "#16A34A" : "#DC2626"));
                right.addView(tvAmt);

                TextView tvBal = new TextView(activity);
                tvBal.setText("ব্যালেন্স: ৳ " + PdfExporter.formatBengaliNumber(tx.getBalanceAfter()));
                tvBal.setTextSize(10.0f);
                tvBal.setTextColor(Color.parseColor("#64748B"));
                right.addView(tvBal);
                txRow.addView(right);

                layoutTxHistory.addView(txRow);
            }
        } else {
            tvTxCount.setText("১টি এন্ট্রি");
            TextView empty = new TextView(activity);
            empty.setText("প্রাথমিক হিসাব: ৳ " + PdfExporter.formatBengaliNumber(customer.getAmount()));
            empty.setTextColor(Color.parseColor("#64748B"));
            empty.setTextSize(12.0f);
            empty.setPadding(0, dpToPx(8), 0, dpToPx(8));
            layoutTxHistory.addView(empty);
        }

        btnAddDue.setOnClickListener(v -> {
            dialog.dismiss();
            showAddDueDialog(customer);
        });

        btnPay.setOnClickListener(v -> {
            dialog.dismiss();
            showReceivePaymentDialog(customer);
        });

        btnCall.setOnClickListener(v -> makeCustomerCall(customer));
        btnWhatsApp.setOnClickListener(v -> sendWhatsAppReminder(customer));

        btnPdf.setOnClickListener(v -> {
            File pdf = PdfExporter.exportCustomerLedgerToPdf(activity, customer);
            openPdfFile(pdf);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // =========================================================================
    // REMINDERS & PHONE CALLS & SHARE HELPERS
    // =========================================================================

    public void makeCustomerCall(BakiModel item) {
        if (item.getPhone() != null && !item.getPhone().trim().isEmpty()) {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + item.getPhone().trim()));
            activity.startActivity(callIntent);
        } else {
            final TextInputEditText etPhone = new TextInputEditText(activity);
            etPhone.setInputType(EditorInfo.TYPE_CLASS_PHONE);
            TextInputLayout til = new TextInputLayout(activity, null, com.google.android.material.R.attr.textInputOutlinedStyle);
            til.setHint("মোবাইল নম্বর লিখুন");
            til.addView(etPhone);
            LinearLayout container = new LinearLayout(activity);
            container.setPadding(dpToPx(20), dpToPx(10), dpToPx(20), dpToPx(10));
            container.addView(til);

            new MaterialAlertDialogBuilder(activity)
                    .setTitle("মোবাইল নম্বর যুক্ত করুন")
                    .setMessage("গ্রাহক '" + item.getCustomerName() + "' এর কোনো ফোন নম্বর সংরক্ষিত নেই।")
                    .setView(container)
                    .setPositiveButton("সংরক্ষণ ও কল", (dialog, which) -> {
                        String ph = etPhone.getText().toString().trim();
                        if (!ph.isEmpty()) {
                            StorageManager storage = StorageManager.getInstance(activity);
                            List<BakiModel> list = storage.loadBakiRecords();
                            for (BakiModel b : list) {
                                if (b.getId().equals(item.getId())) {
                                    b.setPhone(ph);
                                    break;
                                }
                            }
                            storage.saveBakiRecords(list);
                            updateUI();
                            Intent callIntent = new Intent(Intent.ACTION_DIAL);
                            callIntent.setData(Uri.parse("tel:" + ph));
                            activity.startActivity(callIntent);
                        }
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
        }
    }

    public void sendWhatsAppReminder(BakiModel item) {
        String msg = generateBengaliReminderMessage(item);
        if (item.getPhone() != null && !item.getPhone().trim().isEmpty()) {
            String cleanPhone = item.getPhone().trim().replaceAll("[^0-9]", "");
            if (cleanPhone.startsWith("01")) {
                cleanPhone = "88" + cleanPhone;
            }
            try {
                Intent waIntent = new Intent(Intent.ACTION_VIEW);
                waIntent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + Uri.encode(msg)));
                activity.startActivity(waIntent);
                return;
            } catch (Exception ignored) {}
        }

        // Fallback to share intent
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
        sendIntent.setType("text/plain");
        activity.startActivity(Intent.createChooser(sendIntent, "তাগাদা পাঠান"));
    }

    public void sendSmsReminder(BakiModel item) {
        String msg = generateBengaliReminderMessage(item);
        if (item.getPhone() != null && !item.getPhone().trim().isEmpty()) {
            try {
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + item.getPhone().trim()));
                smsIntent.putExtra("sms_body", msg);
                activity.startActivity(smsIntent);
                return;
            } catch (Exception ignored) {}
        }
        copyReminderMessage(item);
    }

    public void copyReminderMessage(BakiModel item) {
        String msg = generateBengaliReminderMessage(item);
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Baki Reminder", msg);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(activity, "তাগাদা মেসেজ ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show();
    }

    private String generateBengaliReminderMessage(BakiModel item) {
        String msg = "মাওয়া স্টোর - বকেয়া তাগাদা\n\n"
                + "জনাব " + item.getCustomerName() + ",\n"
                + "আপনার নিকট মাওয়া স্টোর এর মোট বকেয়া পাওনার পরিমাণ: ৳ " + PdfExporter.formatBengaliNumber(item.getAmount()) + " টাকা।\n";

        if (item.getDueDate() != null && !item.getDueDate().trim().isEmpty()) {
            msg += "পরিশোধের নির্ধারিত তারিখ: " + item.getDueDate() + "\n";
        }
        if (item.getDetails() != null && !item.getDetails().trim().isEmpty()) {
            msg += "মালের বিবরণ: " + item.getDetails() + "\n";
        }

        msg += "\nঅনুগ্রহ করে বকেয়া টাকা পরিশোধ করে আমাদের ব্যবসা পরিচালনায় সহযোগিতা করুন।\n\n"
                + "ধন্যবাদান্তে,\n"
                + "মাওয়া স্টোর\n"
                + "প্রো: মোঃ আবুল কাশেম\n"
                + "ফেনী রোড, দাগনভূঞা, ফেনী।";
        return msg;
    }

    public void exportAllBakiPdf() {
        StorageManager storage = StorageManager.getInstance(activity);
        List<BakiModel> allBaki = storage.loadBakiRecords();
        if (allBaki.isEmpty()) {
            Toast.makeText(activity, "পিডিএফ তৈরির জন্য কোনো বাকি হিসাব পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
            return;
        }
        double total = 0.0;
        for (BakiModel m : allBaki) total += m.getAmount();
        File pdf = PdfExporter.exportBakiReportToPdf(activity, allBaki, total);
        openPdfFile(pdf);
    }

    private void openPdfFile(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(activity, "পিডিএফ ফাইল তৈরি হতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(intent, "পিডিএফ ওপেন করুন"));
        } catch (Exception e) {
            Toast.makeText(activity, "পিডিএফ ওপেন করতে কোনো ভিউয়ার পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteBakiRecord(BakiModel item) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle("হিসাব মুছে ফেলবেন?")
                .setMessage("আপনি কি নিশ্চিতভাবে '" + item.getCustomerName() + "' এর এই বাকি হিসাবটি সম্পূর্ণ মুছে ফেলতে চান? এটি আর ফিরিয়ে আনা যাবে না।")
                .setPositiveButton("হ্যাঁ, মুছুন", (dialogInterface, i) -> {
                    StorageManager storage = StorageManager.getInstance(activity);
                    List<BakiModel> bakiList = storage.loadBakiRecords();
                    int targetIndex = -1;
                    for (int j = 0; j < bakiList.size(); j++) {
                        if (bakiList.get(j).getId().equals(item.getId())) {
                            targetIndex = j;
                            break;
                        }
                    }
                    if (targetIndex != -1) {
                        bakiList.remove(targetIndex);
                        storage.saveBakiRecords(bakiList);
                        Toast.makeText(activity, "হিসাবটি মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show();
                        updateUI();
                        activity.triggerAutoCloudBackup();
                    }
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    private boolean isOverdue(String dueDateStr) {
        if (dueDateStr == null || dueDateStr.trim().isEmpty()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date due = sdf.parse(dueDateStr.trim());
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date today = cal.getTime();
            return due != null && due.before(today);
        } catch (Exception e) {
            return false;
        }
    }

    private GradientDrawable createCircleDrawable(String seed) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        int[] palette = {
                Color.parseColor("#2563EB"), Color.parseColor("#7C3AED"),
                Color.parseColor("#DC2626"), Color.parseColor("#EA580C"),
                Color.parseColor("#059669"), Color.parseColor("#0D9488"),
                Color.parseColor("#4F46E5"), Color.parseColor("#9333EA")
        };
        int hash = seed != null ? Math.abs(seed.hashCode()) : 0;
        shape.setColor(palette[hash % palette.length]);
        return shape;
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "গ";
        String trimmed = name.trim();
        return trimmed.substring(0, Math.min(1, trimmed.length()));
    }

    private String toBengaliDigits(String input) {
        if (input == null) return "";
        char[] banglaDigits = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append(banglaDigits[c - '0']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private int dpToPx(float dp) {
        return (int) (dp * activity.getResources().getDisplayMetrics().density);
    }

    private List<CombinedTx> getAllCombinedTransactions(List<BakiModel> allBaki) {
        List<CombinedTx> list = new ArrayList<>();
        for (BakiModel b : allBaki) {
            if (b.getTransactions() != null) {
                for (BakiTransaction tx : b.getTransactions()) {
                    list.add(new CombinedTx(b, tx));
                }
            }
        }
        Collections.sort(list, (o1, o2) -> {
            long t1 = o1.tx.getUpdatedAt();
            long t2 = o2.tx.getUpdatedAt();
            return Long.compare(t2, t1);
        });
        return list;
    }

    public static class CombinedTx {
        public final BakiModel customer;
        public final BakiTransaction tx;

        public CombinedTx(BakiModel customer, BakiTransaction tx) {
            this.customer = customer;
            this.tx = tx;
        }
    }
}
