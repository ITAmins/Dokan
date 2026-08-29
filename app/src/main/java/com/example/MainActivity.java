package com.example;

import android.accounts.AccountManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.core.view.PointerIconCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.ExpenseAdapter;
import com.example.GoogleSheetsSyncManager;
import com.example.MainActivity;
import com.example.MainViewModel;
import com.example.databinding.ActivityMainBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/* loaded from: classes5.dex */
public class MainActivity extends AppCompatActivity {
    private ExpenseAdapter adapter;
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private BakiKhataManager bakiKhataManager;

    public MainViewModel getViewModel() {
        return this.viewModel;
    }

    public BakiKhataManager getBakiKhataManager() {
        return this.bakiKhataManager;
    }
    private List<ExpenseModel> allExpenses = new ArrayList();
    private String selectedExpenseType = ExpenseModel.TYPE_SHOP;
    private String searchFilterText = "";
    private String currentBakiFilter = "ALL";
    private String currentActiveFordiId = null;
    private String inPageHomeExpenseSelectedDate = "";
    private String inPageHomeExpenseFilter = "CURRENT_MONTH";
    private boolean isUpdatingInputs = false;
    private boolean isExpensesExpanded = false;
    private boolean isDashboardFilterThisMonth = false;
    private String currentDashboardFilter = "MONTH";
    private static final int COLLAPSED_EXPENSES_LIMIT = 5;
    private final Handler backupHandler = new Handler(Looper.getMainLooper());
    private final Runnable backupRunnable = new Runnable() { // from class: com.example.MainActivity$$ExternalSyntheticLambda59
        @Override // java.lang.Runnable
        public final void run() {
            MainActivity.this.triggerAutoCloudBackup();
        }
    };

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_MyApplication);
        super.onCreate(savedInstanceState);
        try {
            this.binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(this.binding.getRoot());
            if (this.binding.toolbar != null) {
                setSupportActionBar(this.binding.toolbar);
            }
            this.viewModel = (MainViewModel) new ViewModelProvider(this).get(MainViewModel.class);
            if (this.binding.rvExpenses != null) {
                this.binding.rvExpenses.setLayoutManager(new LinearLayoutManager(this));
            }
            observeViewModel();
            setupListeners();
            setupDashboard();
            setupCloudBackup();
            setupLocalBackup();
            setupBakiKhata();
            setupFordiKhata();
            setupAutocomplete();
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "Fatal error in onCreate: " + t.getMessage(), t);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void filterExpenses() {
        if (this.binding == null) {
            return;
        }
        List<ExpenseModel> filteredList = new ArrayList<>();
        boolean isSearching = this.searchFilterText != null && !this.searchFilterText.trim().isEmpty();
        if (!isSearching) {
            filteredList.addAll(this.allExpenses);
        } else {
            String query = this.searchFilterText.toLowerCase().trim();
            for (ExpenseModel exp : this.allExpenses) {
                String expName = exp.getName() != null ? exp.getName().toLowerCase() : "";
                String expAmount = String.valueOf(exp.getAmount());
                String expTime = exp.getTime() != null ? exp.getTime().toLowerCase() : "";
                if (expName.contains(query) || expAmount.contains(query) || expTime.contains(query)) {
                    filteredList.add(exp);
                }
            }
        }
        
        double shopTotal = 0.0d;
        double homeTotal = 0.0d;
        for (ExpenseModel exp : this.allExpenses) {
            if (exp == null) continue;
            if (exp.isHomeExpense()) {
                homeTotal += exp.getAmount();
            } else {
                shopTotal += exp.getAmount();
            }
        }
        if (this.binding.tvExpenseBreakdownShopHome != null) {
            this.binding.tvExpenseBreakdownShopHome.setText("দোকান ৳" + PdfExporter.formatBengaliNumber(shopTotal) + " • বাড়ি ৳" + PdfExporter.formatBengaliNumber(homeTotal));
        }

        int totalCount = filteredList.size();
        if (this.binding.tvExpensesCountBadge != null) {
            this.binding.tvExpensesCountBadge.setText(PdfExporter.toBengaliDigits(String.valueOf(totalCount)) + "টি");
        }

        boolean isEmpty = filteredList.isEmpty();
        if (isEmpty) {
            this.binding.layoutEmptyState.setVisibility(View.VISIBLE);
            this.binding.rvExpenses.setVisibility(View.GONE);
            if (this.binding.btnToggleExpensesCollapse != null) {
                this.binding.btnToggleExpensesCollapse.setVisibility(View.GONE);
            }
        } else {
            this.binding.layoutEmptyState.setVisibility(View.GONE);
            this.binding.rvExpenses.setVisibility(View.VISIBLE);

            List<ExpenseModel> displayList;
            if (!isSearching && totalCount > COLLAPSED_EXPENSES_LIMIT && !isExpensesExpanded) {
                displayList = new ArrayList<>(filteredList.subList(0, COLLAPSED_EXPENSES_LIMIT));
                if (this.binding.btnToggleExpensesCollapse != null) {
                    this.binding.btnToggleExpensesCollapse.setVisibility(View.VISIBLE);
                    this.binding.btnToggleExpensesCollapse.setText("সবগুলো দেখুন (" + PdfExporter.toBengaliDigits(String.valueOf(totalCount)) + "টি খরচ) ▾");
                }
            } else if (!isSearching && totalCount > COLLAPSED_EXPENSES_LIMIT && isExpensesExpanded) {
                displayList = filteredList;
                if (this.binding.btnToggleExpensesCollapse != null) {
                    this.binding.btnToggleExpensesCollapse.setVisibility(View.VISIBLE);
                    this.binding.btnToggleExpensesCollapse.setText("কম দেখুন (সংক্ষেপ করুন) ▴");
                }
            } else {
                displayList = filteredList;
                if (this.binding.btnToggleExpensesCollapse != null) {
                    this.binding.btnToggleExpensesCollapse.setVisibility(View.GONE);
                }
            }

            this.adapter = new ExpenseAdapter(displayList, new ExpenseAdapter.OnExpenseActionListener() {
                @Override
                public void onEditClick(ExpenseModel expense, int position) {
                    MainActivity.this.showEditExpenseDialog(expense);
                }

                @Override
                public void onDeleteClick(ExpenseModel expense, int position) {
                    MainActivity.this.showDeleteConfirmationDialog(expense);
                }
            });
            this.binding.rvExpenses.setAdapter(this.adapter);
        }
        updateNotebookTextPreview();
    }

    private void observeViewModel() {
        this.viewModel.getExpenses().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda64
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6974lambda$observeViewModel$0$comexampleMainActivity((List) obj);
            }
        });
        this.viewModel.getTotalExpenses().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda65
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6975lambda$observeViewModel$1$comexampleMainActivity((Double) obj);
            }
        });
        this.viewModel.getSabekCash().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda67
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6976lambda$observeViewModel$2$comexampleMainActivity((Double) obj);
            }
        });
        this.viewModel.getAvailableCash().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda68
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6977lambda$observeViewModel$3$comexampleMainActivity((Double) obj);
            }
        });
        this.viewModel.getDailySale().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda69
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6978lambda$observeViewModel$4$comexampleMainActivity((Double) obj);
            }
        });
        this.viewModel.getTotalSale().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda70
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6979lambda$observeViewModel$5$comexampleMainActivity((Double) obj);
            }
        });
        this.viewModel.getCalculationResult().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda71
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6980lambda$observeViewModel$6$comexampleMainActivity((Double) obj);
            }
        });
        this.viewModel.getActiveDateString().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda72
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6981lambda$observeViewModel$7$comexampleMainActivity((String) obj);
            }
        });
        this.viewModel.getActiveDayOfWeek().observe(this, new Observer() { // from class: com.example.MainActivity$$ExternalSyntheticLambda73
            @Override // androidx.lifecycle.Observer
            public final void onChanged(Object obj) {
                MainActivity.this.m6982lambda$observeViewModel$8$comexampleMainActivity((String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$0$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6974lambda$observeViewModel$0$comexampleMainActivity(List list) {
        this.allExpenses = list != null ? list : new ArrayList();
        filterExpenses();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$1$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6975lambda$observeViewModel$1$comexampleMainActivity(Double amount) {
        String bFormatted = "৳ " + PdfExporter.formatBengaliNumber(amount.doubleValue());
        this.binding.tvTotalExpenses.setText(bFormatted);
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$2$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6976lambda$observeViewModel$2$comexampleMainActivity(Double sabek) {
        if (!this.isUpdatingInputs) {
            this.isUpdatingInputs = true;
            if (sabek.doubleValue() == 0.0d) {
                if (this.binding.etSabekCash.getText().length() > 0) {
                    try {
                        double currVal = Double.parseDouble(this.binding.etSabekCash.getText().toString());
                        if (currVal != 0.0d) {
                            this.binding.etSabekCash.setText("");
                        }
                    } catch (Exception e) {
                    }
                }
            } else {
                String strVal = String.valueOf(sabek);
                if (strVal.endsWith(".0")) {
                    strVal = strVal.substring(0, strVal.length() - 2);
                }
                if (!this.binding.etSabekCash.getText().toString().equals(strVal)) {
                    this.binding.etSabekCash.setText(strVal);
                }
            }
            this.isUpdatingInputs = false;
        }
        updateSabekSuggestionUI();
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$3$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6977lambda$observeViewModel$3$comexampleMainActivity(Double cash) {
        if (!this.isUpdatingInputs) {
            this.isUpdatingInputs = true;
            if (cash.doubleValue() == 0.0d) {
                if (this.binding.etAvailableCash.getText().length() > 0) {
                    try {
                        double currVal = Double.parseDouble(this.binding.etAvailableCash.getText().toString());
                        if (currVal != 0.0d) {
                            this.binding.etAvailableCash.setText("");
                        }
                    } catch (Exception e) {
                    }
                }
            } else {
                String strVal = String.valueOf(cash);
                if (strVal.endsWith(".0")) {
                    strVal = strVal.substring(0, strVal.length() - 2);
                }
                if (!this.binding.etAvailableCash.getText().toString().equals(strVal)) {
                    this.binding.etAvailableCash.setText(strVal);
                }
            }
            this.isUpdatingInputs = false;
        }
        this.binding.tvMiniAvailableCash.setText("৳ " + PdfExporter.formatBengaliNumber(cash.doubleValue()));
        updateSabekSuggestionUI();
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$4$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6978lambda$observeViewModel$4$comexampleMainActivity(Double sale) {
        String bFormatted = "৳ " + PdfExporter.formatBengaliNumber(sale.doubleValue());
        this.binding.tvDailySaleAuto.setText(bFormatted);
        this.binding.tvMiniDailySale.setText(bFormatted);
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$5$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6979lambda$observeViewModel$5$comexampleMainActivity(Double totalSale) {
        String bFormatted = "৳ " + PdfExporter.formatBengaliNumber(totalSale.doubleValue());
        this.binding.tvTotalSale.setText(bFormatted);
        this.binding.tvMiniTotalSale.setText(bFormatted);
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$6$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6980lambda$observeViewModel$6$comexampleMainActivity(Double result) {
        updateResultCard(result.doubleValue());
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$7$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6981lambda$observeViewModel$7$comexampleMainActivity(String date) {
        this.binding.tvActiveDateDisplay.setText(formatBengaliLongDate(date));
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$observeViewModel$8$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m6982lambda$observeViewModel$8$comexampleMainActivity(String day) {
        this.binding.tvActiveDayDisplay.setText(", " + day);
        updateNotebookTextPreview();
        updateHeroCard();
    }

    /* JADX WARN: Removed duplicated region for block: B:114:0x027a  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x028a  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x02a3  */
    /* JADX WARN: Removed duplicated region for block: B:31:0x0309  */
    /* JADX WARN: Removed duplicated region for block: B:40:0x0133 A[Catch: Exception -> 0x027d, TRY_LEAVE, TryCatch #1 {Exception -> 0x027d, blocks: (B:38:0x0127, B:40:0x0133, B:49:0x0232, B:51:0x024b), top: B:37:0x0127 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void updateHeroCard() {
        if (this.binding == null || this.viewModel == null) {
            return;
        }
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            String greeting;
            if (hour >= 4 && hour < 12) {
                greeting = "শুভ সকাল, মাওয়া স্টোর";
            } else if (hour >= 12 && hour < 15) {
                greeting = "শুভ দুপুর, মাওয়া স্টোর";
            } else if (hour >= 15 && hour < 18) {
                greeting = "শুভ বিকাল, মাওয়া স্টোর";
            } else if (hour >= 18 && hour < 22) {
                greeting = "শুভ সন্ধ্যা, মাওয়া স্টোর";
            } else {
                greeting = "শুভ রাত্রি, মাওয়া স্টোর";
            }
            if (this.binding.tvHeroGreeting != null) {
                this.binding.tvHeroGreeting.setText(greeting);
            }

            String activeDate = this.viewModel.getActiveDateString().getValue();
            String activeDayOfWeek = this.viewModel.getActiveDayOfWeek().getValue();
            if (activeDate != null && activeDate.length() >= 10) {
                String[] parts = activeDate.split("-");
                if (parts.length == 3) {
                    String dayStr = parts[0];
                    String monthNum = parts[1];
                    String monthName;
                    switch (monthNum) {
                        case "01": monthName = "জানুয়ারি"; break;
                        case "02": monthName = "ফেব্রুয়ারি"; break;
                        case "03": monthName = "মার্চ"; break;
                        case "04": monthName = "এপ্রিল"; break;
                        case "05": monthName = "মে"; break;
                        case "06": monthName = "জুন"; break;
                        case "07": monthName = "জুলাই"; break;
                        case "08": monthName = "আগস্ট"; break;
                        case "09": monthName = "সেপ্টেম্বর"; break;
                        case "10": monthName = "অক্টোবর"; break;
                        case "11": monthName = "নভেম্বর"; break;
                        case "12": monthName = "ডিসেম্বর"; break;
                        default: monthName = monthNum; break;
                    }
                    if (this.binding.tvHeroDateMonth != null) {
                        this.binding.tvHeroDateMonth.setText(monthName);
                    }
                    if (this.binding.tvHeroDateDay != null) {
                        this.binding.tvHeroDateDay.setText(PdfExporter.toBengaliDigits(dayStr));
                    }
                }
            }
            if (this.binding.tvHeroDateDayOfWeek != null) {
                this.binding.tvHeroDateDayOfWeek.setText(activeDayOfWeek != null ? activeDayOfWeek : "");
            }

            AccountingService.DailyAccountingSummary summary = this.viewModel.getDailySummary().getValue();
            if (summary == null && this.viewModel.getActiveDateKey() != null) {
                summary = AccountingService.getInstance(this).calculateDailySummary(this.viewModel.getActiveDateKey());
            }

            Double dailySale = this.viewModel.getDailySale().getValue();
            double dailySaleVal = summary != null ? summary.totalSales : (dailySale != null ? dailySale.doubleValue() : 0.0d);
            if (this.binding.tvHeroDailySale != null) {
                this.binding.tvHeroDailySale.setText("৳ " + PdfExporter.formatBengaliNumber(dailySaleVal));
            }

            Double totalExpenses = this.viewModel.getTotalExpenses().getValue();
            double totalExpVal = summary != null ? summary.totalCashOutflow : (totalExpenses != null ? totalExpenses.doubleValue() : 0.0d);
            if (this.binding.tvHeroDailyExpense != null) {
                this.binding.tvHeroDailyExpense.setText("৳ " + PdfExporter.formatBengaliNumber(totalExpVal));
            }

            Double sabekCash = this.viewModel.getSabekCash().getValue();
            double sabekVal = summary != null ? summary.openingCash : (sabekCash != null ? sabekCash.doubleValue() : 0.0d);
            if (this.binding.tvMiniAvailableCash != null) {
                this.binding.tvMiniAvailableCash.setText("৳ " + PdfExporter.formatBengaliNumber(sabekVal));
            }

            Double availableCash = this.viewModel.getAvailableCash().getValue();
            double availCashVal = summary != null ? summary.actualAvailableCash : (availableCash != null ? availableCash.doubleValue() : 0.0d);
            if (this.binding.tvMiniTotalSale != null) {
                this.binding.tvMiniTotalSale.setText("৳ " + PdfExporter.formatBengaliNumber(availCashVal));
            }
            if (this.binding.tvHeroAvailableCash != null) {
                this.binding.tvHeroAvailableCash.setText("৳ " + PdfExporter.formatBengaliNumber(availCashVal));
            }

            // Detailed Breakdown List
            if (summary != null) {
                if (this.binding.tvSummaryTotalSale != null) {
                    this.binding.tvSummaryTotalSale.setText("৳ " + PdfExporter.formatBengaliNumber(summary.totalSales));
                }
                if (this.binding.tvSummaryCashSale != null) {
                    this.binding.tvSummaryCashSale.setText("৳ " + PdfExporter.formatBengaliNumber(summary.cashSales));
                }
                if (this.binding.tvSummaryBakiSale != null) {
                    this.binding.tvSummaryBakiSale.setText("৳ " + PdfExporter.formatBengaliNumber(summary.creditSales));
                }
                if (this.binding.tvSummaryBakiJoma != null) {
                    this.binding.tvSummaryBakiJoma.setText("৳ " + PdfExporter.formatBengaliNumber(summary.bakiCollection));
                }
                if (this.binding.tvSummaryTotalExpense != null) {
                    this.binding.tvSummaryTotalExpense.setText("৳ " + PdfExporter.formatBengaliNumber(summary.totalCashOutflow));
                }
                if (this.binding.tvSummaryProductBuy != null) {
                    this.binding.tvSummaryProductBuy.setText("৳ " + PdfExporter.formatBengaliNumber(summary.totalPurchases));
                }
                if (this.binding.tvSummaryShopExpense != null) {
                    this.binding.tvSummaryShopExpense.setText("৳ " + PdfExporter.formatBengaliNumber(summary.totalShopExpenses));
                }
                if (this.binding.tvSummaryHomeExpense != null) {
                    this.binding.tvSummaryHomeExpense.setText("৳ " + PdfExporter.formatBengaliNumber(summary.totalHomeExpenses));
                }
            }

            double marginRate = summary != null ? summary.estimatedGrossMarginRate : StorageManager.getInstance(this).getEstimatedGrossMarginRate();
            int marginPercent = (int) Math.round(marginRate * 100.0);
            double profitVal = dailySaleVal * marginRate;

            if (this.binding.tvHeroResult != null) {
                this.binding.tvHeroResult.setText("৳ " + PdfExporter.formatBengaliNumber(profitVal));
                this.binding.tvHeroResult.setTextColor(Color.parseColor("#34D399"));
            }

            if (this.binding.tvHeroStatusBadge != null) {
                this.binding.tvHeroStatusBadge.setText("শতকরা লাভ (" + PdfExporter.toBengaliDigits(String.valueOf(marginPercent)) + "%)");
                this.binding.tvHeroStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#34D399")));
                this.binding.tvHeroStatusBadge.setTextColor(Color.parseColor("#064E3B"));
            }
            if (this.binding.btnHeroChangeMargin != null) {
                this.binding.btnHeroChangeMargin.setText("⚙ লাভ " + PdfExporter.toBengaliDigits(String.valueOf(marginPercent)) + "%");
                this.binding.btnHeroChangeMargin.setOnClickListener(v -> showProfitMarginDialog());
            }
            if (this.binding.tvHeroCompareStatus != null) {
                this.binding.tvHeroCompareStatus.setText("বিক্রি ৳ " + PdfExporter.formatBengaliNumber(dailySaleVal) + " এর ওপর " + PdfExporter.toBengaliDigits(String.valueOf(marginPercent)) + "% হারে লাভ");
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error updating hero card", e);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private String formatBengaliLongDate(String dateStr) {
        String monthName;
        if (dateStr != null) {
            char c = '\n';
            if (dateStr.length() >= 10) {
                try {
                    String[] parts = dateStr.split("-");
                    if (parts.length != 3) {
                        return dateStr;
                    }
                    String day = parts[0];
                    String monthNum = parts[1];
                    String year = parts[2];
                    switch (monthNum.hashCode()) {
                        case 1537:
                            if (monthNum.equals("01")) {
                                c = 0;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1538:
                            if (monthNum.equals("02")) {
                                c = 1;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1539:
                            if (monthNum.equals("03")) {
                                c = 2;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1540:
                            if (monthNum.equals("04")) {
                                c = 3;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1541:
                            if (monthNum.equals("05")) {
                                c = 4;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1542:
                            if (monthNum.equals("06")) {
                                c = 5;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1543:
                            if (monthNum.equals("07")) {
                                c = 6;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1544:
                            if (monthNum.equals("08")) {
                                c = 7;
                                break;
                            }
                            c = 65535;
                            break;
                        case 1545:
                            if (monthNum.equals("09")) {
                                c = '\b';
                                break;
                            }
                            c = 65535;
                            break;
                        case 1567:
                            if (monthNum.equals("10")) {
                                c = '\t';
                                break;
                            }
                            c = 65535;
                            break;
                        case 1568:
                            if (monthNum.equals("11")) {
                                break;
                            }
                            c = 65535;
                            break;
                        case 1569:
                            if (monthNum.equals("12")) {
                                c = 11;
                                break;
                            }
                            c = 65535;
                            break;
                        default:
                            c = 65535;
                            break;
                    }
                    switch (c) {
                        case 0:
                            monthName = "জানুয়ারি";
                            break;
                        case 1:
                            monthName = "ফেব্রুয়ারি";
                            break;
                        case 2:
                            monthName = "মার্চ";
                            break;
                        case 3:
                            monthName = "এপ্রিল";
                            break;
                        case 4:
                            monthName = "মে";
                            break;
                        case 5:
                            monthName = "জুন";
                            break;
                        case 6:
                            monthName = "জুলাই";
                            break;
                        case 7:
                            monthName = "আগস্ট";
                            break;
                        case '\b':
                            monthName = "সেপ্টেম্বর";
                            break;
                        case '\t':
                            monthName = "অক্টোবর";
                            break;
                        case '\n':
                            monthName = "নভেম্বর";
                            break;
                        case 11:
                            monthName = "ডিসেম্বর";
                            break;
                        default:
                            monthName = monthNum;
                            break;
                    }
                    return day + " " + monthName + " " + year;
                } catch (Exception e) {
                    return dateStr;
                }
            }
        }
        return dateStr;
    }

    private void updateResultCard(double result) {
        if (this.binding == null) return;
        AccountingService.DailyAccountingSummary summary = null;
        if (this.viewModel != null) {
            summary = this.viewModel.getDailySummary().getValue();
            if (summary == null && this.viewModel.getActiveDateKey() != null) {
                summary = AccountingService.getInstance(this).calculateDailySummary(this.viewModel.getActiveDateKey());
            }
        }

        double rate = summary != null ? summary.estimatedGrossMarginRate : StorageManager.getInstance(this).getEstimatedGrossMarginRate();
        int ratePercent = (int) Math.round(rate * 100.0);
        double totalSales = summary != null ? summary.totalSales : 0.0d;
        double profitAmt = totalSales * rate;

        this.binding.cardResult.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#ECFDF5")));
        this.binding.tvResultStatus.setText("বিক্রির টাকার শতকরা লাভ");
        this.binding.tvResultStatus.setTextColor(Color.parseColor("#059669"));
        if (this.binding.tvResultRateBadge != null) {
            this.binding.tvResultRateBadge.setText(PdfExporter.toBengaliDigits(String.valueOf(ratePercent)) + "%");
        }
        if (this.binding.tvHeroResult != null) {
            this.binding.tvHeroResult.setText("৳ " + PdfExporter.formatBengaliNumber(profitAmt));
            this.binding.tvHeroResult.setTextColor(Color.parseColor("#059669"));
        }
        if (this.binding.tvResultAmount != null) {
            this.binding.tvResultAmount.setText("৳ " + PdfExporter.formatBengaliNumber(profitAmt));
        }
        this.binding.tvResultMessage.setText("আজকের মোট বিক্রি ৳ " + PdfExporter.formatBengaliNumber(totalSales) + " (মাল কেনা বা ক্যাশ দোকানেরই অংশ)");
        this.binding.tvResultMessage.setTextColor(Color.parseColor("#047857"));
    }

    private void updateNotebookTextPreview() {
        if (this.viewModel != null && this.binding != null) {
            String report = this.viewModel.generateRuledNotebookReport();
            this.binding.tvNotebookReportBody.setText(report);
            List<ExpenseModel> currentExpenses = this.viewModel.getExpenses().getValue();
            this.binding.layoutNotebookExpensesList.removeAllViews();
            if (currentExpenses == null || currentExpenses.isEmpty()) {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("কোনো খরচ নেই");
                tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
                tvEmpty.setTextSize(11.0f);
                tvEmpty.setGravity(1);
                this.binding.layoutNotebookExpensesList.addView(tvEmpty);
            } else {
                for (ExpenseModel expense : currentExpenses) {
                    LinearLayout row = new LinearLayout(this);
                    row.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
                    row.setOrientation(0);
                    row.setPadding(0, 4, 0, 4);
                    TextView tvName = new TextView(this);
                    tvName.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                    tvName.setText(expense.getName());
                    tvName.setTextColor(Color.parseColor("#334155"));
                    tvName.setTextSize(11.0f);
                    tvName.setMaxLines(1);
                    tvName.setEllipsize(TextUtils.TruncateAt.END);
                    TextView tvDots = new TextView(this);
                    tvDots.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
                    tvDots.setText("......");
                    tvDots.setTextColor(Color.parseColor("#CBD5E1"));
                    tvDots.setTextSize(10.0f);
                    TextView tvVal = new TextView(this);
                    tvVal.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
                    tvVal.setText("৳" + PdfExporter.formatBengaliNumber(expense.getAmount()));
                    tvVal.setTextColor(Color.parseColor("#0F172A"));
                    tvVal.setTextSize(11.0f);
                    tvVal.setPadding(4, 0, 0, 0);
                    row.addView(tvName);
                    row.addView(tvDots);
                    row.addView(tvVal);
                    this.binding.layoutNotebookExpensesList.addView(row);
                }
            }
            Double totalExpVal = this.viewModel.getTotalExpenses().getValue();
            double totalExpenses = totalExpVal != null ? totalExpVal.doubleValue() : 0.0d;
            this.binding.tvNotebookTotalExpenses.setText("মোট খরচ: ৳" + PdfExporter.formatBengaliNumber(totalExpenses));
            Double dailySaleVal = this.viewModel.getDailySale().getValue();
            double dailySale = dailySaleVal != null ? dailySaleVal.doubleValue() : 0.0d;
            this.binding.tvNotebookDailySale.setText("আজকের বেচা.. ৳" + PdfExporter.formatBengaliNumber(dailySale));
            Double avCashVal = this.viewModel.getAvailableCash().getValue();
            double avCash = avCashVal != null ? avCashVal.doubleValue() : 0.0d;
            this.binding.tvNotebookAvailableCash.setText("আছে.......... ৳" + PdfExporter.formatBengaliNumber(avCash));
            Double sabekVal = this.viewModel.getSabekCash().getValue();
            double sabekCash = sabekVal != null ? sabekVal.doubleValue() : 0.0d;
            this.binding.tvNotebookSabekCash.setText("সাবেক....... ৳" + PdfExporter.formatBengaliNumber(sabekCash));
            Double totSaleVal = this.viewModel.getTotalSale().getValue();
            double totalSale = totSaleVal != null ? totSaleVal.doubleValue() : 0.0d;
            this.binding.tvNotebookTotalSale.setText("মোট বেচা ৳" + PdfExporter.formatBengaliNumber(totalSale));
            double marginRate = StorageManager.getInstance(this).getEstimatedGrossMarginRate();
            int marginPercent = (int) Math.round(marginRate * 100.0);
            double profitVal = totalSale * marginRate;

            this.binding.cardNotebookResult.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
            this.binding.tvNotebookResultLabel.setText("শতকরা লাভ (" + PdfExporter.toBengaliDigits(String.valueOf(marginPercent)) + "%)");
            this.binding.tvNotebookResultLabel.setTextColor(Color.parseColor("#16A34A"));
            this.binding.tvNotebookResultAmount.setText("৳" + PdfExporter.formatBengaliNumber(profitVal));
            this.binding.tvNotebookResultAmount.setTextColor(Color.parseColor("#15803D"));
            this.binding.ivNotebookResultTrend.setImageResource(R.drawable.ic_trend_up);
            this.binding.ivNotebookResultTrend.setImageTintList(ColorStateList.valueOf(Color.parseColor("#16A34A")));
        }
    }

    public void openExpenseDrawer() {
        if (this.binding == null || this.binding.layoutExpenseDrawer == null) return;
        if (this.binding.layoutExpenseDrawerOverlay != null) {
            this.binding.layoutExpenseDrawerOverlay.setVisibility(View.VISIBLE);
            this.binding.layoutExpenseDrawerOverlay.setAlpha(0f);
            this.binding.layoutExpenseDrawerOverlay.animate().alpha(1f).setDuration(220L).start();
        }
        this.binding.layoutExpenseDrawer.setVisibility(View.VISIBLE);
        this.binding.layoutExpenseDrawer.setTranslationY(dpToPx(400));
        this.binding.layoutExpenseDrawer.animate()
                .translationY(0f)
                .setDuration(260L)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
                .start();

        if (this.binding.tvDrawerSuccessBadge != null) {
            this.binding.tvDrawerSuccessBadge.setVisibility(View.GONE);
        }
        if (this.viewModel != null && this.binding.tvDrawerDate != null) {
            this.binding.tvDrawerDate.setText("আজ (" + this.viewModel.getCurrentFormattedDate() + ")");
        }
        if (this.binding.btnDrawerDateToday != null) {
            this.binding.btnDrawerDateToday.setBackgroundResource(R.drawable.bg_dark_chip_selected_purple);
            this.binding.btnDrawerDateToday.setTextColor(Color.WHITE);
        }
        if (this.binding.btnDrawerDateYesterday != null) {
            this.binding.btnDrawerDateYesterday.setBackgroundResource(R.drawable.bg_dark_chip_unselected);
            this.binding.btnDrawerDateYesterday.setTextColor(Color.parseColor("#94A3B8"));
        }
        if (this.binding.etDrawerExpenseAmount != null) {
            this.binding.etDrawerExpenseAmount.requestFocus();
            this.binding.etDrawerExpenseAmount.postDelayed(() -> {
                try {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null && binding != null && binding.etDrawerExpenseAmount != null) {
                        imm.showSoftInput(binding.etDrawerExpenseAmount, InputMethodManager.SHOW_IMPLICIT);
                    }
                } catch (Exception ignored) {}
            }, 100L);
        }
    }

    public void closeExpenseDrawer() {
        if (this.binding == null || this.binding.layoutExpenseDrawer == null) return;
        if (this.binding.layoutExpenseDrawerOverlay != null) {
            this.binding.layoutExpenseDrawerOverlay.animate()
                    .alpha(0f)
                    .setDuration(200L)
                    .withEndAction(() -> {
                        if (binding != null && binding.layoutExpenseDrawerOverlay != null) {
                            binding.layoutExpenseDrawerOverlay.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
        this.binding.layoutExpenseDrawer.animate()
                .translationY(dpToPx(450))
                .setDuration(220L)
                .setInterpolator(new android.view.animation.AccelerateInterpolator(1.2f))
                .withEndAction(() -> {
                    if (binding != null && binding.layoutExpenseDrawer != null) {
                        binding.layoutExpenseDrawer.setVisibility(View.GONE);
                    }
                })
                .start();

        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception ignored) {}
    }

    public boolean isExpenseDrawerOpen() {
        return this.binding != null && this.binding.layoutExpenseDrawer != null && this.binding.layoutExpenseDrawer.getVisibility() == View.VISIBLE;
    }

    private void saveExpenseFromDrawer() {
        if (this.binding == null) return;
        String name = this.binding.etDrawerExpenseName.getText() != null ? this.binding.etDrawerExpenseName.getText().toString().trim() : "";
        String amountStr = this.binding.etDrawerExpenseAmount.getText() != null ? this.binding.etDrawerExpenseAmount.getText().toString().trim() : "";

        if (amountStr.isEmpty()) {
            this.binding.etDrawerExpenseAmount.setError("টাকার পরিমাণ লিখুন!");
            Toast.makeText(this, "অনুগ্রহ করে টাকার সঠিক পরিমাণ দিন", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty()) {
            this.binding.etDrawerExpenseName.setError("কী জন্য খরচ হয়েছে লিখুন!");
            Toast.makeText(this, "অনুগ্রহ করে খরচের বিবরণ বা নাম লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0.0d) {
                this.binding.etDrawerExpenseAmount.setError("টাকার পরিমাণ শূন্য বা ঋণাত্মক হতে পারবে না!");
                Toast.makeText(this, "টাকার পরিমাণ অবশ্যই শূন্যের চেয়ে বড় হতে হবে", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean success = this.viewModel.addExpense(name, amount, this.selectedExpenseType);
            if (success) {
                this.binding.etDrawerExpenseName.setText("");
                this.binding.etDrawerExpenseAmount.setText("");
                this.binding.etDrawerExpenseName.clearFocus();
                this.binding.etDrawerExpenseAmount.requestFocus();
                
                // Show inline confirmation badge in drawer without closing
                if (this.binding.tvDrawerSuccessBadge != null) {
                    this.binding.tvDrawerSuccessBadge.setVisibility(View.VISIBLE);
                    this.binding.tvDrawerSuccessBadge.setAlpha(0f);
                    this.binding.tvDrawerSuccessBadge.animate().alpha(1f).setDuration(180L).start();
                    this.binding.tvDrawerSuccessBadge.postDelayed(() -> {
                        if (binding != null && binding.tvDrawerSuccessBadge != null) {
                            binding.tvDrawerSuccessBadge.animate().alpha(0f).setDuration(200L).withEndAction(() -> {
                                if (binding != null && binding.tvDrawerSuccessBadge != null) {
                                    binding.tvDrawerSuccessBadge.setVisibility(View.GONE);
                                }
                            }).start();
                        }
                    }, 2500L);
                }
                Toast.makeText(this, "খরচ সফলভাবে যোগ করা হয়েছে", Toast.LENGTH_SHORT).show();
                planAutoCloudBackup();
            }
        } catch (NumberFormatException e) {
            this.binding.etDrawerExpenseAmount.setError("সঠিক সংখ্যা দিন!");
        }
    }

    private void appendToDrawerExpenseAmount(double addition) {
        if (this.binding == null || this.binding.etDrawerExpenseAmount == null) return;
        String current = this.binding.etDrawerExpenseAmount.getText() != null ? this.binding.etDrawerExpenseAmount.getText().toString().trim() : "";
        double val = 0.0;
        if (!current.isEmpty()) {
            try {
                val = Double.parseDouble(current);
            } catch (Exception ignored) {}
        }
        val += addition;
        String formatted = (val == Math.floor(val)) ? String.valueOf((long) val) : String.valueOf(val);
        this.binding.etDrawerExpenseAmount.setText(formatted);
        this.binding.etDrawerExpenseAmount.setSelection(formatted.length());
    }

    private void setupExpenseDrawer() {
        if (this.binding == null) return;

        // Drawer Close Actions
        if (this.binding.btnDrawerClose != null) {
            this.binding.btnDrawerClose.setOnClickListener(v -> closeExpenseDrawer());
        }
        if (this.binding.layoutExpenseDrawerOverlay != null) {
            this.binding.layoutExpenseDrawerOverlay.setOnClickListener(v -> closeExpenseDrawer());
        }

        // Quick amount chips (+20, +50, +100, +200)
        if (this.binding.btnQuickAdd20 != null) {
            this.binding.btnQuickAdd20.setOnClickListener(v -> appendToDrawerExpenseAmount(20.0));
        }
        if (this.binding.btnQuickAdd50 != null) {
            this.binding.btnQuickAdd50.setOnClickListener(v -> appendToDrawerExpenseAmount(50.0));
        }
        if (this.binding.btnQuickAdd100 != null) {
            this.binding.btnQuickAdd100.setOnClickListener(v -> appendToDrawerExpenseAmount(100.0));
        }
        if (this.binding.btnQuickAdd200 != null) {
            this.binding.btnQuickAdd200.setOnClickListener(v -> appendToDrawerExpenseAmount(200.0));
        }

        // Category selection: 🏬 দোকান খরচ, 🏠 সংসার / বাড়ি, 🛒 পণ্য ক্রয়
        View.OnClickListener categoryClickListener = v -> {
            if (binding == null) return;
            if (binding.btnDrawerTypeShop != null) {
                binding.btnDrawerTypeShop.setBackgroundResource(R.drawable.bg_dark_chip_unselected);
                binding.btnDrawerTypeShop.setTextColor(Color.parseColor("#94A3B8"));
            }
            if (binding.btnDrawerTypeHome != null) {
                binding.btnDrawerTypeHome.setBackgroundResource(R.drawable.bg_dark_chip_unselected);
                binding.btnDrawerTypeHome.setTextColor(Color.parseColor("#94A3B8"));
            }
            if (binding.btnDrawerTypeProductBuy != null) {
                binding.btnDrawerTypeProductBuy.setBackgroundResource(R.drawable.bg_dark_chip_unselected);
                binding.btnDrawerTypeProductBuy.setTextColor(Color.parseColor("#94A3B8"));
            }

            if (v.getId() == R.id.btnDrawerTypeShop) {
                selectedExpenseType = ExpenseModel.TYPE_SHOP;
                binding.btnDrawerTypeShop.setBackgroundResource(R.drawable.bg_dark_chip_selected_red);
                binding.btnDrawerTypeShop.setTextColor(Color.WHITE);
            } else if (v.getId() == R.id.btnDrawerTypeHome) {
                selectedExpenseType = ExpenseModel.TYPE_HOME;
                binding.btnDrawerTypeHome.setBackgroundResource(R.drawable.bg_dark_chip_selected_purple);
                binding.btnDrawerTypeHome.setTextColor(Color.WHITE);
            } else if (v.getId() == R.id.btnDrawerTypeProductBuy) {
                selectedExpenseType = ExpenseModel.TYPE_SHOP;
                binding.btnDrawerTypeProductBuy.setBackgroundResource(R.drawable.bg_dark_chip_selected_green);
                binding.btnDrawerTypeProductBuy.setTextColor(Color.WHITE);
                if (binding.etDrawerExpenseName != null && binding.etDrawerExpenseName.getText().toString().isEmpty()) {
                    binding.etDrawerExpenseName.setText("মাল কেনা");
                    binding.etDrawerExpenseName.setSelection(binding.etDrawerExpenseName.getText().length());
                }
            }
        };

        if (this.binding.btnDrawerTypeShop != null) {
            this.binding.btnDrawerTypeShop.setOnClickListener(categoryClickListener);
        }
        if (this.binding.btnDrawerTypeHome != null) {
            this.binding.btnDrawerTypeHome.setOnClickListener(categoryClickListener);
        }
        if (this.binding.btnDrawerTypeProductBuy != null) {
            this.binding.btnDrawerTypeProductBuy.setOnClickListener(categoryClickListener);
        }

        // Date selection
        if (this.binding.layoutDrawerDatePicker != null) {
            this.binding.layoutDrawerDatePicker.setOnClickListener(v -> showDatePickerDialog());
        }
        if (this.binding.btnDrawerChangeDate != null) {
            this.binding.btnDrawerChangeDate.setOnClickListener(v -> showDatePickerDialog());
        }
        if (this.binding.btnDrawerDateToday != null) {
            this.binding.btnDrawerDateToday.setOnClickListener(v -> {
                if (viewModel != null) {
                    viewModel.setDateToToday();
                    if (binding.tvDrawerDate != null) {
                        binding.tvDrawerDate.setText("আজ (" + viewModel.getCurrentFormattedDate() + ")");
                    }
                }
                binding.btnDrawerDateToday.setBackgroundResource(R.drawable.bg_dark_chip_selected_purple);
                binding.btnDrawerDateToday.setTextColor(Color.WHITE);
                if (binding.btnDrawerDateYesterday != null) {
                    binding.btnDrawerDateYesterday.setBackgroundResource(R.drawable.bg_dark_chip_unselected);
                    binding.btnDrawerDateYesterday.setTextColor(Color.parseColor("#94A3B8"));
                }
            });
        }
        if (this.binding.btnDrawerDateYesterday != null) {
            this.binding.btnDrawerDateYesterday.setOnClickListener(v -> {
                if (viewModel != null) {
                    viewModel.moveToPreviousDay();
                    if (binding.tvDrawerDate != null) {
                        binding.tvDrawerDate.setText("গতকাল (" + viewModel.getCurrentFormattedDate() + ")");
                    }
                }
                binding.btnDrawerDateYesterday.setBackgroundResource(R.drawable.bg_dark_chip_selected_purple);
                binding.btnDrawerDateYesterday.setTextColor(Color.WHITE);
                if (binding.btnDrawerDateToday != null) {
                    binding.btnDrawerDateToday.setBackgroundResource(R.drawable.bg_dark_chip_unselected);
                    binding.btnDrawerDateToday.setTextColor(Color.parseColor("#94A3B8"));
                }
            });
        }
        if (this.binding.btnDrawerDateCustom != null) {
            this.binding.btnDrawerDateCustom.setOnClickListener(v -> showDatePickerDialog());
        }

        // Save Button
        if (this.binding.btnDrawerSave != null) {
            this.binding.btnDrawerSave.setOnClickListener(v -> saveExpenseFromDrawer());
        }

        // Keyboard actions
        if (this.binding.etDrawerExpenseAmount != null) {
            this.binding.etDrawerExpenseAmount.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_NEXT || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (binding.etDrawerExpenseName != null) {
                        binding.etDrawerExpenseName.requestFocus();
                    }
                    return true;
                }
                return false;
            });
        }

        if (this.binding.etDrawerExpenseName != null) {
            this.binding.etDrawerExpenseName.setOnItemClickListener((parent, view, position, id) -> {
                Object item = parent.getItemAtPosition(position);
                if (item != null) {
                    binding.etDrawerExpenseName.setText(item.toString());
                    binding.etDrawerExpenseName.setSelection(item.toString().length());
                }
            });

            this.binding.etDrawerExpenseName.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    saveExpenseFromDrawer();
                    return true;
                }
                return false;
            });
        }
    }

    private void setupListeners() {
        // Setup Expense Drawer
        setupExpenseDrawer();

        // 1. Quick action: বিক্রি (Sale) shortcut
        if (this.binding.btnQuickSaleShortcut != null) {
            this.binding.btnQuickSaleShortcut.setOnClickListener(v -> {
                if (this.binding.tabLayout != null && this.binding.tabLayout.getSelectedTabPosition() != 0) {
                    TabLayout.Tab tab = this.binding.tabLayout.getTabAt(0);
                    if (tab != null) tab.select();
                }
                if (this.binding.etAvailableCash != null) {
                    this.binding.etAvailableCash.requestFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(this.binding.etAvailableCash, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                }
                Toast.makeText(MainActivity.this, "নগদ ক্যাশ বা সাবেক লিখে আজকের বিক্রি হিসাব করুন", Toast.LENGTH_SHORT).show();
            });
        }

        // 2. Quick action: বাকি (Baki) shortcut
        if (this.binding.btnQuickBakiShortcut != null) {
            this.binding.btnQuickBakiShortcut.setOnClickListener(v -> {
                if (this.binding.tabLayout != null) {
                    TabLayout.Tab tab = this.binding.tabLayout.getTabAt(2);
                    if (tab != null) {
                        tab.select();
                    }
                }
            });
        }

        // 3. Quick action: জমা (Joma / Receive payment) shortcut
        if (this.binding.btnQuickJomaShortcut != null) {
            this.binding.btnQuickJomaShortcut.setOnClickListener(v -> {
                if (this.bakiKhataManager != null) {
                    if (this.binding.tabLayout != null) {
                        TabLayout.Tab tab = this.binding.tabLayout.getTabAt(2);
                        if (tab != null) tab.select();
                    }
                    this.bakiKhataManager.showReceivePaymentDialog(null);
                } else {
                    if (this.binding.etSabekCash != null) {
                        this.binding.etSabekCash.requestFocus();
                        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.showSoftInput(this.binding.etSabekCash, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                        }
                    }
                }
            });
        }

        // 4. Quick action: খরচ (Expense) button triggers Drawer
        if (this.binding.btnQuickExpenseShortcut != null) {
            this.binding.btnQuickExpenseShortcut.setOnClickListener(v -> openExpenseDrawer());
        }

        // 5. Quick action: ফর্দ (Fordi) shortcut
        if (this.binding.btnQuickFordiShortcut != null) {
            this.binding.btnQuickFordiShortcut.setOnClickListener(v -> {
                if (this.binding.tabLayout != null) {
                    TabLayout.Tab tab = this.binding.tabLayout.getTabAt(3);
                    if (tab != null) {
                        tab.select();
                    }
                }
            });
        }

        // Card button: + খরচ যোগ triggers Drawer
        if (this.binding.btnOpenExpenseDrawerFromCard != null) {
            this.binding.btnOpenExpenseDrawerFromCard.setOnClickListener(v -> openExpenseDrawer());
        }

        // Refresh expenses list
        if (this.binding.btnRefreshExpenses != null) {
            this.binding.btnRefreshExpenses.setOnClickListener(v -> {
                if (this.viewModel != null) {
                    this.viewModel.loadSavedData();
                    updateDashboardUI();
                    Toast.makeText(MainActivity.this, "হিসাব রিলোড হয়েছে", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Export and share buttons in Report tab
        if (this.binding.btnExportPdf != null) {
            this.binding.btnExportPdf.setOnClickListener(v -> triggerPdfExport(true));
        }
        if (this.binding.btnExportJpg != null) {
            this.binding.btnExportJpg.setOnClickListener(v -> triggerJpgExport(true));
        }
        if (this.binding.btnExportCsv != null) {
            this.binding.btnExportCsv.setOnClickListener(v -> triggerCsvExport(true));
        }
        if (this.binding.btnOpenExportCenter != null) {
            this.binding.btnOpenExportCenter.setOnClickListener(v -> showExportCenterDialog());
        }
        if (this.binding.btnShareReport != null) {
            this.binding.btnShareReport.setOnClickListener(v -> shareDailyReport());
        }

        // Notification header button
        if (this.binding.btnNotifications != null) {
            this.binding.btnNotifications.setOnClickListener(v -> {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("বিজ্ঞপ্তি ও আপডেট")
                        .setMessage("• দৈনিক ক্যাশ খাতা সক্রিয় রয়েছে।\n• বকেয়া তাগাদা পাঠাতে 'বাকি' ট্যাবে যান।\n• ডাটা নিরাপদে রাখতে ক্লাউড ব্যাকআপ ব্যবহার করুন।")
                        .setPositiveButton("ঠিক আছে", null)
                        .show();
            });
        }

        // Hero cards interactivity
        if (this.binding.cardHeroDateBadge != null) {
            this.binding.cardHeroDateBadge.setOnClickListener(v -> showDatePickerDialog());
        }
        if (this.binding.tvHeroDailyExpense != null) {
            this.binding.tvHeroDailyExpense.setOnClickListener(v -> openExpenseDrawer());
        }
        View.OnClickListener saleFocusClick = v -> {
            if (this.binding.etAvailableCash != null) {
                this.binding.etAvailableCash.requestFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(this.binding.etAvailableCash, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
        };
        if (this.binding.tvHeroDailySale != null) {
            this.binding.tvHeroDailySale.setOnClickListener(saleFocusClick);
        }
        if (this.binding.tvDailySaleAuto != null) {
            this.binding.tvDailySaleAuto.setOnClickListener(saleFocusClick);
        }
        if (this.binding.tvMiniDailySale != null) {
            this.binding.tvMiniDailySale.setOnClickListener(saleFocusClick);
        }
        if (this.binding.tvMiniTotalSale != null) {
            this.binding.tvMiniTotalSale.setOnClickListener(saleFocusClick);
        }
        if (this.binding.tvTotalSale != null) {
            this.binding.tvTotalSale.setOnClickListener(saleFocusClick);
        }

        View.OnClickListener sabekSuggestClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applySuggestedSabekCash();
            }
        };
        this.binding.btnSuggestSabekCash.setOnClickListener(sabekSuggestClick);
        this.binding.btnApplySabekSuggestion.setOnClickListener(sabekSuggestClick);

        if (this.binding.btnHeaderCloudSync != null) {
            this.binding.btnHeaderCloudSync.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCloudSyncQuickDialog();
                }
            });
        }

        this.binding.btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(MainActivity.this, view);
                popup.getMenu().add(0, 1, 0, "এক্সপোর্ট সেন্টার (PDF, JPG, CSV)");
                popup.getMenu().add(0, 2, 1, "মেমো PDF ডাউনলোড");
                popup.getMenu().add(0, 3, 2, "মেমো JPG ডাউনলোড");
                popup.getMenu().add(0, 4, 3, "CSV এক্সেল ডাউনলোড");
                popup.getMenu().add(0, 5, 4, "আজকের রিপোর্ট শেয়ার");
                popup.getMenu().add(0, 6, 5, "হিসাব রিলোড করুন");
                popup.getMenu().add(0, 7, 6, "হিসাব রিসেট করুন");
                popup.getMenu().add(0, 8, 7, "শতকরা লাভের হার নির্ধারণ (মার্জিন)");
                popup.setOnMenuItemClickListener(new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        if (id == 1) {
                            showExportCenterDialog();
                            return true;
                        } else if (id == 2) {
                            triggerPdfExport(true);
                            return true;
                        } else if (id == 3) {
                            triggerJpgExport(true);
                            return true;
                        } else if (id == 4) {
                            triggerCsvExport(true);
                            return true;
                        } else if (id == 5) {
                            shareDailyReport();
                            return true;
                        } else if (id == 6) {
                            if (viewModel != null) {
                                viewModel.loadSavedData();
                                updateDashboardUI();
                                Toast.makeText(MainActivity.this, "হিসাব হালনাগাদ (রিলোড) হয়েছে", Toast.LENGTH_SHORT).show();
                            }
                            return true;
                        } else if (id == 7) {
                            showClearAllConfirmationDialog();
                            return true;
                        } else if (id == 8) {
                            showProfitMarginDialog();
                            return true;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        });

        if (this.binding.cardResult != null) {
            this.binding.cardResult.setOnClickListener(v -> showProfitMarginDialog());
        }
        if (this.binding.cardNotebookResult != null) {
            this.binding.cardNotebookResult.setOnClickListener(v -> showProfitMarginDialog());
        }

        if (this.binding.btnToggleExpensesCollapse != null) {
            this.binding.btnToggleExpensesCollapse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isExpensesExpanded = !isExpensesExpanded;
                    filterExpenses();
                }
            });
        }

        this.binding.etSearchExpenses.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.this.searchFilterText = s != null ? s.toString() : "";
                MainActivity.this.filterExpenses();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        this.binding.btnPrevDay.setOnClickListener(view -> {
            MainActivity.this.viewModel.moveToPreviousDay();
            MainActivity.this.updateSabekSuggestionUI();
        });

        this.binding.btnNextDay.setOnClickListener(view -> {
            MainActivity.this.viewModel.moveToNextDay();
            MainActivity.this.updateSabekSuggestionUI();
        });

        this.binding.layoutDatePicker.setOnClickListener(view -> {
            MainActivity.this.showDatePickerDialog();
        });

        this.binding.etSabekCash.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (MainActivity.this.isUpdatingInputs) {
                    return;
                }
                String input = s.toString().trim();
                double val = 0.0d;
                if (!input.isEmpty()) {
                    try {
                        val = Double.parseDouble(input);
                    } catch (NumberFormatException e) {
                    }
                }
                MainActivity.this.viewModel.setSabekCash(val);
                MainActivity.this.planAutoCloudBackup();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        this.binding.etAvailableCash.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (MainActivity.this.isUpdatingInputs) {
                    return;
                }
                String input = s.toString().trim();
                double val = 0.0d;
                if (!input.isEmpty()) {
                    try {
                        val = Double.parseDouble(input);
                    } catch (NumberFormatException e) {
                    }
                }
                MainActivity.this.viewModel.setAvailableCash(val);
                MainActivity.this.planAutoCloudBackup();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        if (this.binding.swipeRefreshLayout != null) {
            this.binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    MainActivity.this.m7026lambda$setupListeners$23$comexampleMainActivity();
                }
            });
        }

        // Breakdown toggle
        if (this.binding.btnToggleBreakdown != null) {
            this.binding.btnToggleBreakdown.setOnClickListener(v -> {
                if (this.binding.layoutBreakdownContainer != null) {
                    boolean isVisible = this.binding.layoutBreakdownContainer.getVisibility() == View.VISIBLE;
                    this.binding.layoutBreakdownContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                    this.binding.btnToggleBreakdown.setText(isVisible ? "বিস্তারিত হিসাব ▾" : "সংক্ষেপ করুন ▴");
                }
            });
        }

        // Note counter dialog openers
        if (this.binding.btnOpenNoteCounter != null) {
            this.binding.btnOpenNoteCounter.setOnClickListener(v -> showNoteCountingDialog());
        }
        if (this.binding.cardNoteCountingHint != null) {
            this.binding.cardNoteCountingHint.setOnClickListener(v -> showNoteCountingDialog());
        }

        // Smart calculator opener
        if (this.binding.btnFloatingCalculator != null) {
            this.binding.btnFloatingCalculator.setOnClickListener(v -> showSmartCalculatorDialog());
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$23$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7026lambda$setupListeners$23$comexampleMainActivity() {
        this.viewModel.loadSavedData();
        updateSabekSuggestionUI();
        updateDashboardUI();
        this.binding.swipeRefreshLayout.postDelayed(new Runnable() { // from class: com.example.MainActivity$$ExternalSyntheticLambda34
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m7025lambda$setupListeners$22$comexampleMainActivity();
            }
        }, 600L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$22$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7025lambda$setupListeners$22$comexampleMainActivity() {
        this.binding.swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(this, "হিসাব হালনাগাদ (রিলোড) হয়েছে", 0).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$24$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7027lambda$setupListeners$24$comexampleMainActivity(View v) {
        triggerPdfExport(true);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$25$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7028lambda$setupListeners$25$comexampleMainActivity(View v) {
        shareDailyReport();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$27$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7030lambda$setupListeners$27$comexampleMainActivity(View v) {
        this.binding.swipeRefreshLayout.setRefreshing(true);
        this.viewModel.loadSavedData();
        updateDashboardUI();
        this.binding.swipeRefreshLayout.postDelayed(new Runnable() { // from class: com.example.MainActivity$$ExternalSyntheticLambda76
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.this.m7029lambda$setupListeners$26$comexampleMainActivity();
            }
        }, 600L);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$setupListeners$26$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7029lambda$setupListeners$26$comexampleMainActivity() {
        this.binding.swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(this, "হিসাব হালনাগাদ (রিলোড) হয়েছে", 0).show();
    }

    private File triggerPdfExport(boolean showToast) {
        double result = this.viewModel.getCalculationResult().getValue() != null ? this.viewModel.getCalculationResult().getValue().doubleValue() : 0.0d;
        double totExp = this.viewModel.getTotalExpenses().getValue() != null ? this.viewModel.getTotalExpenses().getValue().doubleValue() : 0.0d;
        double sale = this.viewModel.getDailySale().getValue() != null ? this.viewModel.getDailySale().getValue().doubleValue() : 0.0d;
        double cash = this.viewModel.getAvailableCash().getValue() != null ? this.viewModel.getAvailableCash().getValue().doubleValue() : 0.0d;
        double totSl = this.viewModel.getTotalSale().getValue() != null ? this.viewModel.getTotalSale().getValue().doubleValue() : 0.0d;
        double sabek = this.viewModel.getSabekCash().getValue() != null ? this.viewModel.getSabekCash().getValue().doubleValue() : 0.0d;
        File file = PdfExporter.exportToPdf(this, this.viewModel.getExpenses().getValue(), totExp, sale, cash, totSl, sabek, result, this.viewModel.getCurrentFormattedDate(), this.viewModel.getBengaliDayOfWeek());
        if (file != null && showToast) {
            Toast.makeText(this, "PDF মেমো ডাউনলোড হয়েছে!\nফাইল: " + file.getName(), Toast.LENGTH_LONG).show();
            openExportedFile(file, "application/pdf");
        }
        return file;
    }

    private File triggerJpgExport(boolean showToast) {
        double result = this.viewModel.getCalculationResult().getValue() != null ? this.viewModel.getCalculationResult().getValue().doubleValue() : 0.0d;
        double totExp = this.viewModel.getTotalExpenses().getValue() != null ? this.viewModel.getTotalExpenses().getValue().doubleValue() : 0.0d;
        double sale = this.viewModel.getDailySale().getValue() != null ? this.viewModel.getDailySale().getValue().doubleValue() : 0.0d;
        double cash = this.viewModel.getAvailableCash().getValue() != null ? this.viewModel.getAvailableCash().getValue().doubleValue() : 0.0d;
        double totSl = this.viewModel.getTotalSale().getValue() != null ? this.viewModel.getTotalSale().getValue().doubleValue() : 0.0d;
        double sabek = this.viewModel.getSabekCash().getValue() != null ? this.viewModel.getSabekCash().getValue().doubleValue() : 0.0d;
        File file = ImageMemoExporter.exportDailyCashMemoToJpg(this, this.viewModel.getExpenses().getValue(), totExp, sale, cash, totSl, sabek, result, this.viewModel.getCurrentFormattedDate(), this.viewModel.getBengaliDayOfWeek());
        if (file != null && showToast) {
            Toast.makeText(this, "JPG মেমো ছবি তৈরি হয়েছে!\nফাইল: " + file.getName(), Toast.LENGTH_LONG).show();
            openExportedFile(file, "image/jpeg");
        }
        return file;
    }

    private File triggerCsvExport(boolean showToast) {
        double result = this.viewModel.getCalculationResult().getValue() != null ? this.viewModel.getCalculationResult().getValue().doubleValue() : 0.0d;
        double totExp = this.viewModel.getTotalExpenses().getValue() != null ? this.viewModel.getTotalExpenses().getValue().doubleValue() : 0.0d;
        double sale = this.viewModel.getDailySale().getValue() != null ? this.viewModel.getDailySale().getValue().doubleValue() : 0.0d;
        double cash = this.viewModel.getAvailableCash().getValue() != null ? this.viewModel.getAvailableCash().getValue().doubleValue() : 0.0d;
        double totSl = this.viewModel.getTotalSale().getValue() != null ? this.viewModel.getTotalSale().getValue().doubleValue() : 0.0d;
        double sabek = this.viewModel.getSabekCash().getValue() != null ? this.viewModel.getSabekCash().getValue().doubleValue() : 0.0d;
        File file = CsvExporter.exportDailyCashBookToCsv(this, this.viewModel.getCurrentFormattedDate(), this.viewModel.getBengaliDayOfWeek(), this.viewModel.getExpenses().getValue(), cash, sabek, sale, totSl, totExp, result);
        if (file != null && showToast) {
            Toast.makeText(this, "CSV এক্সেল ফাইল ডাউনলোড হয়েছে!\nফাইল: " + file.getName(), Toast.LENGTH_LONG).show();
            openExportedFile(file, "text/csv");
        }
        return file;
    }

    private void openExportedFile(File file, String mimeType) {
        if (file == null || !file.exists()) return;
        try {
            Uri uri = FileProvider.getUriForFile(this, "com.aistudio.dailycashbook.kxmpzq.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "ফাইলটি ওপেন করুন"));
        } catch (Exception e) {
            Toast.makeText(this, "ফাইল তৈরি হয়েছে (" + file.getName() + ")", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareExportedFile(File file, String mimeType, String subject) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "ফাইল পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        Uri fileUri = FileProvider.getUriForFile(this, "com.aistudio.dailycashbook.kxmpzq.fileprovider", file);
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "শেয়ার করুন"));
    }

    public void showExportCenterDialog() {
        if (isFinishing() || isDestroyed()) return;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_export_center);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final String[] selectedPeriod = {"MONTH"}; // TODAY, WEEK, MONTH, ALL
        final String[] selectedType = {"CASH"};     // CASH, BAKI
        final String[] selectedFormat = {"PDF"};   // PDF, JPG, CSV

        com.google.android.material.button.MaterialButton btnPeriodToday = dialog.findViewById(R.id.btnPeriodToday);
        com.google.android.material.button.MaterialButton btnPeriodWeek = dialog.findViewById(R.id.btnPeriodWeek);
        com.google.android.material.button.MaterialButton btnPeriodMonth = dialog.findViewById(R.id.btnPeriodMonth);
        com.google.android.material.button.MaterialButton btnPeriodAll = dialog.findViewById(R.id.btnPeriodAll);

        com.google.android.material.button.MaterialButton btnTypeCash = dialog.findViewById(R.id.btnTypeCash);
        com.google.android.material.button.MaterialButton btnTypeBaki = dialog.findViewById(R.id.btnTypeBaki);

        com.google.android.material.button.MaterialButton btnFormatPdf = dialog.findViewById(R.id.btnFormatPdf);
        com.google.android.material.button.MaterialButton btnFormatJpg = dialog.findViewById(R.id.btnFormatJpg);
        com.google.android.material.button.MaterialButton btnFormatCsv = dialog.findViewById(R.id.btnFormatCsv);

        View.OnClickListener periodListener = v -> {
            btnPeriodToday.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
            btnPeriodToday.setTextColor(Color.parseColor("#64748B"));
            btnPeriodWeek.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
            btnPeriodWeek.setTextColor(Color.parseColor("#64748B"));
            btnPeriodMonth.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
            btnPeriodMonth.setTextColor(Color.parseColor("#64748B"));
            btnPeriodAll.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
            btnPeriodAll.setTextColor(Color.parseColor("#64748B"));

            if (v.getId() == R.id.btnPeriodToday) {
                selectedPeriod[0] = "TODAY";
                btnPeriodToday.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                btnPeriodToday.setTextColor(Color.WHITE);
            } else if (v.getId() == R.id.btnPeriodWeek) {
                selectedPeriod[0] = "WEEK";
                btnPeriodWeek.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                btnPeriodWeek.setTextColor(Color.WHITE);
            } else if (v.getId() == R.id.btnPeriodMonth) {
                selectedPeriod[0] = "MONTH";
                btnPeriodMonth.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                btnPeriodMonth.setTextColor(Color.WHITE);
            } else {
                selectedPeriod[0] = "ALL";
                btnPeriodAll.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                btnPeriodAll.setTextColor(Color.WHITE);
            }
        };

        btnPeriodToday.setOnClickListener(periodListener);
        btnPeriodWeek.setOnClickListener(periodListener);
        btnPeriodMonth.setOnClickListener(periodListener);
        btnPeriodAll.setOnClickListener(periodListener);

        // Preselect Month
        btnPeriodMonth.setBackgroundResource(R.drawable.bg_filter_tab_selected);
        btnPeriodMonth.setTextColor(Color.WHITE);

        View.OnClickListener typeListener = v -> {
            btnTypeCash.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
            btnTypeCash.setTextColor(Color.parseColor("#64748B"));
            btnTypeBaki.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
            btnTypeBaki.setTextColor(Color.parseColor("#64748B"));

            if (v.getId() == R.id.btnTypeCash) {
                selectedType[0] = "CASH";
                btnTypeCash.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                btnTypeCash.setTextColor(Color.WHITE);
            } else {
                selectedType[0] = "BAKI";
                btnTypeBaki.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                btnTypeBaki.setTextColor(Color.WHITE);
            }
        };
        btnTypeCash.setOnClickListener(typeListener);
        btnTypeBaki.setOnClickListener(typeListener);

        View.OnClickListener formatListener = v -> {
            btnFormatPdf.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
            btnFormatPdf.setTextColor(Color.parseColor("#64748B"));
            btnFormatJpg.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
            btnFormatJpg.setTextColor(Color.parseColor("#64748B"));
            btnFormatCsv.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
            btnFormatCsv.setTextColor(Color.parseColor("#64748B"));

            if (v.getId() == R.id.btnFormatPdf) {
                selectedFormat[0] = "PDF";
                btnFormatPdf.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                btnFormatPdf.setTextColor(Color.WHITE);
            } else if (v.getId() == R.id.btnFormatJpg) {
                selectedFormat[0] = "JPG";
                btnFormatJpg.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                btnFormatJpg.setTextColor(Color.WHITE);
            } else {
                selectedFormat[0] = "CSV";
                btnFormatCsv.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                btnFormatCsv.setTextColor(Color.WHITE);
            }
        };
        btnFormatPdf.setOnClickListener(formatListener);
        btnFormatJpg.setOnClickListener(formatListener);
        btnFormatCsv.setOnClickListener(formatListener);

        View btnClose = dialog.findViewById(R.id.btnExportDialogClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.findViewById(R.id.btnExportDownload).setOnClickListener(v -> {
            File exported = generateReportForExportCenter(selectedPeriod[0], selectedType[0], selectedFormat[0]);
            dialog.dismiss();
            if (exported != null) {
                String mime = "application/pdf";
                if ("JPG".equals(selectedFormat[0])) mime = "image/jpeg";
                else if ("CSV".equals(selectedFormat[0])) mime = "text/csv";
                Toast.makeText(this, "রিপোর্ট ডাউনলোড সম্পন্ন হয়েছে!\nফাইল: " + exported.getName(), Toast.LENGTH_LONG).show();
                openExportedFile(exported, mime);
            }
        });

        dialog.findViewById(R.id.btnExportShare).setOnClickListener(v -> {
            File exported = generateReportForExportCenter(selectedPeriod[0], selectedType[0], selectedFormat[0]);
            dialog.dismiss();
            if (exported != null) {
                String mime = "application/pdf";
                if ("JPG".equals(selectedFormat[0])) mime = "image/jpeg";
                else if ("CSV".equals(selectedFormat[0])) mime = "text/csv";
                shareExportedFile(exported, mime, "মাওয়া স্টোর রিপোর্ট");
            }
        });

        dialog.show();
    }

    private File generateReportForExportCenter(String period, String type, String format) {
        if ("BAKI".equals(type)) {
            StorageManager storage = StorageManager.getInstance(this);
            List<BakiModel> bakiList = storage.loadBakiRecords();
            double totalDue = 0.0;
            for (BakiModel b : bakiList) totalDue += b.getAmount();

            if ("CSV".equals(format)) {
                return CsvExporter.exportBakiDirectoryToCsv(this, bakiList, totalDue);
            } else {
                return PdfExporter.exportBakiReportToPdf(this, bakiList, totalDue);
            }
        }

        // CASH TYPE
        String periodTitle = "মাসিক রিপোর্ট";
        if ("TODAY".equals(period)) periodTitle = "দৈনিক ক্যাশ রিপোর্ট (" + this.viewModel.getCurrentFormattedDate() + ")";
        else if ("WEEK".equals(period)) periodTitle = "সাপ্তাহিক রিপোর্ট";
        else if ("MONTH".equals(period)) periodTitle = "চলতি মাসিক রিপোর্ট";
        else if ("ALL".equals(period)) periodTitle = "সার্বগ্রাহী (সর্বমোট) রিপোর্ট";

        if ("TODAY".equals(period)) {
            if ("PDF".equals(format)) return triggerPdfExport(false);
            if ("JPG".equals(format)) return triggerJpgExport(false);
            if ("CSV".equals(format)) return triggerCsvExport(false);
        }

        List<MainViewModel.DaySummary> summaries = this.viewModel.getSummariesForPeriod(period);
        double totalSale = 0.0, totalExpense = 0.0, totalProfit = 0.0;
        for (MainViewModel.DaySummary ds : summaries) {
            totalSale += ds.computedSale;
            totalExpense += ds.expenses;
            totalProfit += ds.estimatedProfit;
        }

        if ("CSV".equals(format)) {
            return CsvExporter.exportPeriodCashBookToCsv(this, periodTitle, summaries, totalSale, totalExpense, totalProfit);
        } else if ("JPG".equals(format)) {
            return ImageMemoExporter.exportPeriodStatementToJpg(this, periodTitle, totalSale, totalExpense, totalProfit, summaries);
        } else {
            return PdfExporter.exportPeriodReportToPdf(this, periodTitle, totalSale, totalExpense, totalProfit, summaries);
        }
    }

    private void shareDailyReport() {
        File pdfFile = triggerPdfExport(false);
        String textSummary = this.viewModel.generateRuledNotebookReport();
        Intent shareIntent = new Intent("android.intent.action.SEND");
        shareIntent.setType("application/pdf");
        shareIntent.putExtra("android.intent.extra.SUBJECT", "দৈনিক ক্যাশ রিপোর্ট - " + this.viewModel.getCurrentFormattedDate());
        shareIntent.putExtra("android.intent.extra.TEXT", textSummary + "\n\n(অ্যাপ থেকে পাঠানো দৈনিক হিসাব)");
        if (pdfFile != null && pdfFile.exists()) {
            Uri pdfUri = FileProvider.getUriForFile(this, "com.aistudio.dailycashbook.kxmpzq.fileprovider", pdfFile);
            shareIntent.putExtra("android.intent.extra.STREAM", pdfUri);
            shareIntent.addFlags(1);
        } else {
            shareIntent.setType("text/plain");
        }
        startActivity(Intent.createChooser(shareIntent, "দৈনিক খাতা রিপোর্ট শেয়ার করুন"));
    }

    public void showEditExpenseDialog(final ExpenseModel expense) {
        if (expense == null || isFinishing() || isDestroyed()) {
            return;
        }
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_expense);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final AutoCompleteTextView etName = dialog.findViewById(R.id.etEditExpenseName);
        final EditText etAmount = dialog.findViewById(R.id.etEditExpenseAmount);
        TextView tvSubtitle = dialog.findViewById(R.id.tvEditExpenseSubtitle);
        Button btnCancel = dialog.findViewById(R.id.btnCancelEdit);
        Button btnSave = dialog.findViewById(R.id.btnSaveEdit);

        if (tvSubtitle != null) {
            tvSubtitle.setText(expense.getDate() + " • " + expense.getTime());
        }

        if (etName != null) {
            etName.setText(expense.getName());
            etName.setSelection(etName.getText().length());

            List<String> rawSuggestions = StorageManager.getInstance(this).getAllProductSuggestionsWithDefaults();
            final List<ExpenseSuggestion> suggestions = new ArrayList<>();
            for (String raw : rawSuggestions) {
                if (raw != null && !raw.trim().isEmpty()) {
                    String cleanName = raw.trim();
                    suggestions.add(new ExpenseSuggestion(getEmojiForProductName(cleanName), cleanName));
                }
            }
            ArrayAdapter<ExpenseSuggestion> autoAdapter = new ArrayAdapter<ExpenseSuggestion>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(suggestions)) {
                private final List<ExpenseSuggestion> originalList = new ArrayList<>(suggestions);

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    if (view instanceof TextView) {
                        ExpenseSuggestion item = getItem(position);
                        if (item != null) {
                            ((TextView) view).setText(item.name);
                            ((TextView) view).setPadding(24, 20, 24, 20);
                            ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f);
                        }
                    }
                    return view;
                }

                @Override
                public Filter getFilter() {
                    return new Filter() {
                        @Override
                        protected FilterResults performFiltering(CharSequence constraint) {
                            FilterResults results = new FilterResults();
                            List<ExpenseSuggestion> filtered = new ArrayList<>();
                            if (constraint == null || constraint.toString().trim().isEmpty()) {
                                filtered.addAll(originalList);
                            } else {
                                String query = constraint.toString().trim().toLowerCase();
                                for (ExpenseSuggestion item : originalList) {
                                    if (item.name.toLowerCase().contains(query)) {
                                        filtered.add(item);
                                    }
                                }
                            }
                            results.values = filtered;
                            results.count = filtered.size();
                            return results;
                        }

                        @Override
                        protected void publishResults(CharSequence constraint, FilterResults results) {
                            clear();
                            if (results != null && results.count > 0 && results.values instanceof List) {
                                addAll((List<ExpenseSuggestion>) results.values);
                            }
                            notifyDataSetChanged();
                        }

                        @Override
                        public CharSequence convertResultToString(Object resultValue) {
                            if (resultValue instanceof ExpenseSuggestion) {
                                return ((ExpenseSuggestion) resultValue).name;
                            }
                            return super.convertResultToString(resultValue);
                        }
                    };
                }
            };
            etName.setAdapter(autoAdapter);
        }

        if (etAmount != null) {
            String amtStr = (expense.getAmount() == (long) expense.getAmount()) ? String.valueOf((long) expense.getAmount()) : String.valueOf(expense.getAmount());
            etAmount.setText(amtStr);
            etAmount.setSelection(etAmount.getText().length());
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newName = etName != null ? etName.getText().toString().trim() : "";
                    String amtStr = etAmount != null ? etAmount.getText().toString().trim() : "";
                    if (newName.isEmpty()) {
                        Toast.makeText(MainActivity.this, "পণ্যের নাম লিখুন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (amtStr.isEmpty()) {
                        Toast.makeText(MainActivity.this, "টাকার পরিমাণ লিখুন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double newAmount = Double.parseDouble(amtStr);
                        if (newAmount <= 0) {
                            Toast.makeText(MainActivity.this, "সঠিক টাকার পরিমাণ দিন", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        boolean updated = MainActivity.this.viewModel.updateExpense(expense.getId(), newName, newAmount);
                        if (updated) {
                            Toast.makeText(MainActivity.this, "খরচ সফলভাবে পরিবর্তন করা হয়েছে", Toast.LENGTH_SHORT).show();
                            MainActivity.this.planAutoCloudBackup();
                            MainActivity.this.setupAutocomplete();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(MainActivity.this, "পরিবর্তন ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "টাকার পরিমাণ সংখ্যায় লিখুন", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        dialog.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showDeleteConfirmationDialog(final ExpenseModel expense) {
        new MaterialAlertDialogBuilder(this).setTitle((CharSequence) "খরচ মুছে ফেলবেন?").setMessage((CharSequence) ("আপনি কি নিশ্চিতভাবে \"" + expense.getName() + "\" বাবদ ৳ " + PdfExporter.formatBengaliNumber(expense.getAmount()) + " খরচের হিসাব খতিয়ান থেকে মুছে ফেলতে চান?")).setPositiveButton((CharSequence) "হ্যাঁ", new DialogInterface.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda12
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m7038lambda$showDeleteConfirmationDialog$28$comexampleMainActivity(expense, dialogInterface, i);
            }
        }).setNegativeButton((CharSequence) "না", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showDeleteConfirmationDialog$28$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7038lambda$showDeleteConfirmationDialog$28$comexampleMainActivity(ExpenseModel expense, DialogInterface dialog, int which) {
        this.viewModel.deleteExpense(expense.getId());
        Toast.makeText(this, "হিসাবটি মুছে ফেলা হয়েছে", 0).show();
        planAutoCloudBackup();
    }

    private void showClearAllConfirmationDialog() {
        new MaterialAlertDialogBuilder(this).setTitle((CharSequence) "সব ডিলিট করবেন?").setMessage((CharSequence) "আপনি কি নিশ্চিতভাবে এই খাতার সমস্ত খরচ ও বেচার হিসাব ডিলিট করে নতুন খাতা খুলতে চান? এই কাজটি আর ফিরিয়ে আনা যাবে না।").setPositiveButton((CharSequence) "হ্যাঁ", new DialogInterface.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda8
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.this.m7036xaeb3d55b(dialogInterface, i);
            }
        }).setNegativeButton((CharSequence) "না", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showClearAllConfirmationDialog$29$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7036xaeb3d55b(DialogInterface dialog, int which) {
        this.viewModel.clearAllData();
        this.binding.etSabekCash.setText("");
        this.binding.etAvailableCash.setText("");
        Toast.makeText(this, "খাতার সমস্ত হিসাব মুছে ফেলা হয়েছে", 0).show();
        planAutoCloudBackup();
    }

    private void showDatePickerDialog() {
        Calendar c = Calendar.getInstance();
        if (this.viewModel != null) {
            String activeDateKey = this.viewModel.getActiveDateKey();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                Date parsed = sdf.parse(activeDateKey);
                if (parsed != null) {
                    c.setTime(parsed);
                }
            } catch (Exception e) {
            }
        }
        int year = c.get(1);
        int month = c.get(2);
        int day = c.get(5);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda13
            @Override // android.app.DatePickerDialog.OnDateSetListener
            public final void onDateSet(DatePicker datePicker, int i, int i2, int i3) {
                MainActivity.this.m7037lambda$showDatePickerDialog$30$comexampleMainActivity(datePicker, i, i2, i3);
            }
        }, year, month, day);
        datePickerDialog.show();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$showDatePickerDialog$30$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7037lambda$showDatePickerDialog$30$comexampleMainActivity(DatePicker view, int selectedYear, int selectedMonth, int selectedDayOfMonth) {
        this.viewModel.selectDate(selectedYear, selectedMonth, selectedDayOfMonth);
        Toast.makeText(this, "তারিখ পরিবর্তন করে " + selectedDayOfMonth + " মাস " + (selectedMonth + 1) + " করা হয়েছে", 0).show();
    }

    @Override // android.app.Activity
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem clearItem = menu.add(0, 101, 0, "মুছুন");
        clearItem.setIcon(R.drawable.ic_trash);
        try {
            if (clearItem.getIcon() != null) {
                clearItem.getIcon().setTint(Color.parseColor("#EF4444"));
            }
        } catch (Exception e) {
        }
        clearItem.setShowAsAction(2);
        return true;
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 101) {
            showClearAllConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupDashboard() {
        int[][] states = {new int[]{android.R.attr.state_selected}, new int[]{-16842913}};
        int[] colors = {Color.parseColor("#2563EB"), Color.parseColor("#64748B")};
        if (this.binding.tabLayout != null) {
            this.binding.tabLayout.setTabIconTint(new ColorStateList(states, colors));
            this.binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    int pos = tab.getPosition();
                    updateBottomNavVisuals(pos);
                    if (pos == 0) {
                        MainActivity.this.binding.layoutDailyLedger.setVisibility(View.VISIBLE);
                        MainActivity.this.binding.layoutDashboard.setVisibility(View.GONE);
                        MainActivity.this.binding.layoutBakiKhata.setVisibility(View.GONE);
                        MainActivity.this.binding.layoutFordiKhata.setVisibility(View.GONE);
                        MainActivity.this.binding.layoutCloudBackup.setVisibility(View.GONE);
                        return;
                    }
                    if (pos == 1) {
                        MainActivity.this.binding.layoutDailyLedger.setVisibility(View.GONE);
                        MainActivity.this.binding.layoutDashboard.setVisibility(View.VISIBLE);
                        MainActivity.this.binding.layoutBakiKhata.setVisibility(View.GONE);
                        MainActivity.this.binding.layoutFordiKhata.setVisibility(View.GONE);
                        MainActivity.this.binding.layoutCloudBackup.setVisibility(View.GONE);
                        MainActivity.this.updateDashboardUI();
                        return;
                    }
                    if (pos == 2) {
                        MainActivity.this.binding.layoutDailyLedger.setVisibility(View.GONE);
                        MainActivity.this.binding.layoutDashboard.setVisibility(View.GONE);
                        MainActivity.this.binding.layoutBakiKhata.setVisibility(View.VISIBLE);
                        MainActivity.this.binding.layoutFordiKhata.setVisibility(View.GONE);
                        MainActivity.this.binding.layoutCloudBackup.setVisibility(View.GONE);
                        MainActivity.this.updateBakiKhataUI();
                        return;
                    }
                    if (pos == 3) {
                        MainActivity.this.binding.layoutDailyLedger.setVisibility(View.GONE);
                        MainActivity.this.binding.layoutDashboard.setVisibility(View.GONE);
                        MainActivity.this.binding.layoutBakiKhata.setVisibility(View.GONE);
                        MainActivity.this.binding.layoutFordiKhata.setVisibility(View.VISIBLE);
                        MainActivity.this.binding.layoutCloudBackup.setVisibility(View.GONE);
                        MainActivity.this.updateFordiKhataUI();
                        return;
                    }
                    // pos 4: Home Expense & Cloud
                    MainActivity.this.binding.layoutDailyLedger.setVisibility(View.GONE);
                    MainActivity.this.binding.layoutDashboard.setVisibility(View.GONE);
                    MainActivity.this.binding.layoutBakiKhata.setVisibility(View.GONE);
                    MainActivity.this.binding.layoutFordiKhata.setVisibility(View.GONE);
                    MainActivity.this.binding.layoutCloudBackup.setVisibility(View.VISIBLE);
                    MainActivity.this.updateInPageHomeExpensesUI();
                    MainActivity.this.updateCloudBackupUI();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        }

        setupBottomNavigationBar();
        View.OnClickListener dashFilterListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.btnFilterToday != null) {
                    binding.btnFilterToday.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
                    binding.btnFilterToday.setTextColor(Color.parseColor("#64748B"));
                }
                if (binding.btnFilterThisWeek != null) {
                    binding.btnFilterThisWeek.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
                    binding.btnFilterThisWeek.setTextColor(Color.parseColor("#64748B"));
                }
                binding.btnFilterThisMonth.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
                binding.btnFilterThisMonth.setTextColor(Color.parseColor("#64748B"));
                binding.btnFilterAllTime.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
                binding.btnFilterAllTime.setTextColor(Color.parseColor("#64748B"));

                if (v.getId() == R.id.btnFilterToday) {
                    currentDashboardFilter = "TODAY";
                    if (binding.btnFilterToday != null) {
                        binding.btnFilterToday.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                        binding.btnFilterToday.setTextColor(Color.WHITE);
                    }
                } else if (v.getId() == R.id.btnFilterThisWeek) {
                    currentDashboardFilter = "WEEK";
                    if (binding.btnFilterThisWeek != null) {
                        binding.btnFilterThisWeek.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                        binding.btnFilterThisWeek.setTextColor(Color.WHITE);
                    }
                } else if (v.getId() == R.id.btnFilterThisMonth) {
                    currentDashboardFilter = "MONTH";
                    binding.btnFilterThisMonth.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                    binding.btnFilterThisMonth.setTextColor(Color.WHITE);
                } else {
                    currentDashboardFilter = "ALL";
                    binding.btnFilterAllTime.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                    binding.btnFilterAllTime.setTextColor(Color.WHITE);
                }
                updateDashboardUI();
            }
        };

        if (this.binding.btnFilterToday != null) {
            this.binding.btnFilterToday.setOnClickListener(dashFilterListener);
        }
        if (this.binding.btnFilterThisWeek != null) {
            this.binding.btnFilterThisWeek.setOnClickListener(dashFilterListener);
        }
        this.binding.btnFilterThisMonth.setOnClickListener(dashFilterListener);
        this.binding.btnFilterAllTime.setOnClickListener(dashFilterListener);
    }

    private void setupBottomNavigationBar() {
        if (this.binding == null) return;

        if (this.binding.navItemHome != null) {
            this.binding.navItemHome.setOnClickListener(v -> selectBottomTab(0));
        }
        if (this.binding.navItemReport != null) {
            this.binding.navItemReport.setOnClickListener(v -> selectBottomTab(1));
        }
        if (this.binding.navItemBaki != null) {
            this.binding.navItemBaki.setOnClickListener(v -> selectBottomTab(2));
        }
        if (this.binding.btnNavCenterExpense != null) {
            this.binding.btnNavCenterExpense.setOnClickListener(v -> openExpenseDrawer());
        }
        if (this.binding.navItemFordi != null) {
            this.binding.navItemFordi.setOnClickListener(v -> selectBottomTab(3));
        }
        if (this.binding.navItemHomeExpense != null) {
            this.binding.navItemHomeExpense.setOnClickListener(v -> selectBottomTab(4));
        }

        updateBottomNavVisuals(0);
    }

    private void selectBottomTab(int index) {
        if (this.binding == null) return;
        if (this.binding.tabLayout != null) {
            TabLayout.Tab tab = this.binding.tabLayout.getTabAt(index);
            if (tab != null) {
                tab.select();
            }
        }
        updateBottomNavVisuals(index);
    }

    private void updateBottomNavVisuals(int selectedIndex) {
        if (this.binding == null) return;
        int activeColor = Color.parseColor("#2563EB");
        int inactiveColor = Color.parseColor("#64748B");

        // 0: Home
        if (this.binding.ivNavHome != null && this.binding.tvNavHome != null) {
            boolean active = selectedIndex == 0;
            this.binding.ivNavHome.setImageTintList(ColorStateList.valueOf(active ? activeColor : inactiveColor));
            this.binding.tvNavHome.setTextColor(active ? activeColor : inactiveColor);
            this.binding.tvNavHome.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        }
        // 1: Report
        if (this.binding.ivNavReport != null && this.binding.tvNavReport != null) {
            boolean active = selectedIndex == 1;
            this.binding.ivNavReport.setImageTintList(ColorStateList.valueOf(active ? activeColor : inactiveColor));
            this.binding.tvNavReport.setTextColor(active ? activeColor : inactiveColor);
            this.binding.tvNavReport.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        }
        // 2: Baki
        if (this.binding.ivNavBaki != null && this.binding.tvNavBaki != null) {
            boolean active = selectedIndex == 2;
            this.binding.ivNavBaki.setImageTintList(ColorStateList.valueOf(active ? activeColor : inactiveColor));
            this.binding.tvNavBaki.setTextColor(active ? activeColor : inactiveColor);
            this.binding.tvNavBaki.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        }
        // 3: Fordi
        if (this.binding.ivNavFordi != null && this.binding.tvNavFordi != null) {
            boolean active = selectedIndex == 3;
            this.binding.ivNavFordi.setImageTintList(ColorStateList.valueOf(active ? activeColor : inactiveColor));
            this.binding.tvNavFordi.setTextColor(active ? activeColor : inactiveColor);
            this.binding.tvNavFordi.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        }
        // 4: Bari
        if (this.binding.ivNavHomeExpense != null && this.binding.tvNavHomeExpense != null) {
            boolean active = selectedIndex == 4;
            this.binding.ivNavHomeExpense.setImageTintList(ColorStateList.valueOf(active ? activeColor : inactiveColor));
            this.binding.tvNavHomeExpense.setTextColor(active ? activeColor : inactiveColor);
            this.binding.tvNavHomeExpense.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateDashboardUI() {
        double d = 0.0d;
        List<ExpenseModel> rangeExpenses;
        double avgSales;
        double avgSales2;
        double avgSales3;
        List<ExpenseModel> rangeExpenses2;
        String salesStatText;
        String formattedMaxVal;
        String adviseText;
        String matchedCat;
        if (this.viewModel == null || this.binding == null) {
            return;
        }
        List<MainViewModel.DaySummary> filtered = this.viewModel.getSummariesForPeriod(this.currentDashboardFilter);
        double totalSalesSum = 0.0d;
        double totalExpSum = 0.0d;
        for (MainViewModel.DaySummary ds : filtered) {
            totalSalesSum += ds.computedSale;
            totalExpSum += ds.expenses;
        }
        double netProfitSum = totalSalesSum - totalExpSum;
        this.binding.tvDashTotalSales.setText("৳ " + PdfExporter.formatBengaliNumber(totalSalesSum));
        this.binding.tvDashTotalExpenses.setText("৳ " + PdfExporter.formatBengaliNumber(totalExpSum));
        this.binding.tvDashNetProfit.setText("৳ " + PdfExporter.formatBengaliNumber(Math.abs(netProfitSum)));
        if (netProfitSum > 0.0d) {
            d = 0.0d;
            this.binding.cardDashNetStatus.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#ECFDF5")));
            this.binding.tvDashNetPrefix.setText("সার্বিকভাবে নিট লাভ");
            this.binding.tvDashNetPrefix.setTextColor(Color.parseColor("#047857"));
            this.binding.tvDashNetProfit.setTextColor(Color.parseColor("#047857"));
            this.binding.ivDashNetIcon.setImageResource(R.drawable.ic_notebook);
            this.binding.ivDashNetIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#047857")));
            this.binding.ivDashNetIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D1FAE5")));
        } else {
            d = 0.0d;
            ActivityMainBinding activityMainBinding = this.binding;
            if (netProfitSum < 0.0d) {
                activityMainBinding.cardDashNetStatus.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFF1F2")));
                this.binding.tvDashNetPrefix.setText("সার্বিকভাবে নিট ঘাটতি");
                this.binding.tvDashNetPrefix.setTextColor(Color.parseColor("#BE123C"));
                this.binding.tvDashNetProfit.setTextColor(Color.parseColor("#BE123C"));
                this.binding.ivDashNetIcon.setImageResource(R.drawable.ic_trash);
                this.binding.ivDashNetIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#BE123C")));
                this.binding.ivDashNetIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFE4E6")));
            } else {
                activityMainBinding.cardDashNetStatus.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F8FAFC")));
                this.binding.tvDashNetPrefix.setText("সার্বিকভাবে হিসাব সমান");
                this.binding.tvDashNetPrefix.setTextColor(Color.parseColor("#475569"));
                this.binding.tvDashNetProfit.setTextColor(Color.parseColor("#475569"));
                this.binding.ivDashNetIcon.setImageResource(R.drawable.ic_notebook);
                this.binding.ivDashNetIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#475569")));
                this.binding.ivDashNetIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
            }
        }
        int progressValue = 0;
        if (totalSalesSum > d) {
            progressValue = (int) Math.round((totalExpSum / totalSalesSum) * 100.0d);
        }
        if (progressValue > 100) {
            progressValue = 100;
        }
        if (progressValue < 0) {
            progressValue = 0;
        }
        this.binding.progressRatioBar.setProgress(progressValue);
        this.binding.tvDashProgressPercent.setText(PdfExporter.formatBengaliNumber(progressValue) + "% খরচ");
        int remainingValue = 100 - progressValue;
        this.binding.tvDashProgressRemainingPercent.setText(PdfExporter.formatBengaliNumber(remainingValue) + "% লাভ");
        List<ExpenseModel> rangeExpenses3 = new ArrayList<>();
        StorageManager storage = StorageManager.getInstance(getApplication());
        Iterator<MainViewModel.DaySummary> it = filtered.iterator();
        while (it.hasNext()) {
            rangeExpenses3.addAll(storage.loadExpenses(it.next().dateKey));
            remainingValue = remainingValue;
        }
        this.binding.pieChartView.setExpenses(rangeExpenses3);
        this.binding.lineGraphView.setData(filtered);
        if (filtered.isEmpty()) {
            rangeExpenses = rangeExpenses3;
            avgSales = d;
        } else {
            rangeExpenses = rangeExpenses3;
            avgSales = totalSalesSum / filtered.size();
        }
        if (filtered.isEmpty()) {
            avgSales2 = avgSales;
            avgSales3 = d;
        } else {
            avgSales2 = avgSales;
            double avgSales4 = filtered.size();
            avgSales3 = totalExpSum / avgSales4;
        }
        if (filtered.isEmpty()) {
            salesStatText = "• বেচা বনাম খরচ: কোনো রেকর্ড খুঁজে পাওয়া যায়নি। দৈনিক খাতা এন্ট্রি করুন।";
            rangeExpenses2 = rangeExpenses;
        } else {
            double avgExp = avgSales3;
            String formattedAvgSales = PdfExporter.formatBengaliNumber((int) Math.round(avgSales2));
            String formattedAvgExp = PdfExporter.formatBengaliNumber((int) Math.round(avgExp));
            if (avgSales2 > avgExp) {
                rangeExpenses2 = rangeExpenses;
                salesStatText = "• বেচা বনাম খরচ: দিনপ্রতি গড় বিক্রি ৳ " + formattedAvgSales + " যা গড় খরচ ৳ " + formattedAvgExp + " এর তুলনায় বেশি। এটি ইতিবাচক মুনাফা নির্দেশক।";
            } else {
                rangeExpenses2 = rangeExpenses;
                if (avgSales2 < avgExp) {
                    salesStatText = "• বেচা বনাম খরচ: গড় খরচ ৳ " + formattedAvgExp + " এবং গড় বিক্রি ৳ " + formattedAvgSales + "। ব্যবসা প্রবৃদ্ধ করতে খরচ নিয়ন্ত্রণ করা প্রয়োজন।";
                } else {
                    salesStatText = "• বেচা বনাম খরচ: গড় বিক্রি ও গড় খরচ উভয়ই ৳ " + formattedAvgSales + " মূল্যে সমান রয়েছে। ব্রেক-ইভেন স্তর বজায় রয়েছে।";
                }
            }
        }
        this.binding.tvAnalysisSaleStat.setText(salesStatText);
        Map<String, Double> categoryTotals = new HashMap<>();
        categoryTotals.put("বাজার", Double.valueOf(d));
        categoryTotals.put("ভাড়া", Double.valueOf(d));
        categoryTotals.put("পরিবহন", Double.valueOf(d));
        categoryTotals.put("ওষুধ", Double.valueOf(d));
        categoryTotals.put("ব্যাংক", Double.valueOf(d));
        categoryTotals.put("কাঁচামাল", Double.valueOf(d));
        categoryTotals.put("অন্যান্য", Double.valueOf(d));
        Iterator<ExpenseModel> it2 = rangeExpenses2.iterator();
        while (it2.hasNext()) {
            ExpenseModel exp = it2.next();
            String name = exp.getName() != null ? exp.getName().trim() : "";
            if (name.isEmpty()) {
                continue;
            }
            Iterator<ExpenseModel> it3 = it2;
            String nameLower = name.toLowerCase();
            int progressValue2 = progressValue;
            if (nameLower.contains("বাজার") || nameLower.contains("চাল") || nameLower.contains("আটা") || nameLower.contains("ডাল") || nameLower.contains("তেল")) {
                matchedCat = "বাজার";
            } else if (nameLower.contains("ভাড়া") || nameLower.contains("ভাড়া") || nameLower.contains("মেস") || nameLower.contains("দোকান") || nameLower.contains("বাড়ি")) {
                matchedCat = "ভাড়া";
            } else if (nameLower.contains("পরিবহন") || nameLower.contains("বাস") || nameLower.contains("রিকশা") || nameLower.contains("ভ্যান") || nameLower.contains("যাতায়াত") || nameLower.contains("গাড়ি")) {
                matchedCat = "পরিবহন";
            } else if (nameLower.contains("ওষুধ") || nameLower.contains("ঔষধ") || nameLower.contains("ডাক্তার") || nameLower.contains("মেডিকেল") || nameLower.contains("হাসপাতাল")) {
                matchedCat = "ওষুধ";
            } else if (nameLower.contains("ব্যাংক") || nameLower.contains("রকেট") || nameLower.contains("বিকাশ") || nameLower.contains("নগদ") || nameLower.contains("সার্ভিস") || nameLower.contains("ট্যাক্স")) {
                matchedCat = "ব্যাংক";
            } else if (nameLower.contains("কাঁচামাল") || nameLower.contains("সবজি") || nameLower.contains("ফল") || nameLower.contains("মাছ") || nameLower.contains("মাংস") || nameLower.contains("ডিম")) {
                matchedCat = "কাঁচামাল";
            } else {
                matchedCat = name;
            }
            if (!categoryTotals.containsKey(matchedCat)) {
                categoryTotals.put(matchedCat, Double.valueOf(d));
            }
            categoryTotals.put(matchedCat, Double.valueOf(categoryTotals.get(matchedCat).doubleValue() + exp.getAmount()));
            it2 = it3;
            progressValue = progressValue2;
        }
        String maxCategory = "অন্যান্য";
        double maxCatVal = 0.0d;
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue().doubleValue() > maxCatVal) {
                maxCatVal = entry.getValue().doubleValue();
                String maxCategory2 = entry.getKey();
                maxCategory = maxCategory2;
            }
        }
        if (maxCatVal == d) {
            formattedMaxVal = "• সর্বোচ্চ ব্যয়ের খাত: এখনও কোনো খরচের রেকর্ড পাওয়া যায়নি।";
        } else {
            String formattedMaxVal2 = PdfExporter.formatBengaliNumber((int) Math.round(maxCatVal));
            formattedMaxVal = "• সর্বোচ্চ ব্যয়ের খাত: \"" + maxCategory + "\" খাতে সর্বোচ্চ ব্যয় হয়েছে (মোট ৳ " + formattedMaxVal2 + ")। এই খাতে সচেতন বাজেট মেলাতে পারেন।";
        }
        this.binding.tvAnalysisHighestExpense.setText(formattedMaxVal);
        if (maxCategory.equals("বাজার")) {
            adviseText = "• লাভ সাশ্রয়ী পরামর্শ: বাজার ক্রয়ে অপচয় কমাতে এবং অর্থ সাশ্রয় করতে চাল, ডাল ও তেল পাইকারি বাজার হতে বড় মাপে একবারে ক্রয়ের অভ্যাস করুন।";
        } else if (maxCategory.equals("কাঁচামাল")) {
            adviseText = "• লাভ সাশ্রয়ী পরামর্শ: কাঁচামাল সরাসরি আড়ত বা চুক্তিভিত্তিক চাষীদের নিকট হতে সংগ্রহ করতে পারলে উৎপাদন মূল্যে প্রায় ১০-১৫% সাশ্রয় আনা সম্ভব।";
        } else if (maxCategory.equals("অন্যান্য")) {
            adviseText = "• লাভ সাশ্রয়ী পরামর্শ: ফুটকর ও বিবিধ অন্যান্য খরচগুলো সবসময় নিখুঁতভাবে লিখে রাখুন; এটি অপ্রয়োজনীয় ছোট ছোট অপব্যয় সনাক্ত করতে সহায়তা করবে।";
        } else if (netProfitSum < d) {
            adviseText = "• লাভ সাশ্রয়ী পরামর্শ: ড্যাশবোর্ডে ঘাটতি রয়েছে। নতুন কোনো ব্যয়ের পূর্বে সাবেক ক্যাশ তহবিলের উদ্বৃত্ত পুনঃমূল্যায়ন করে লাভ বৃদ্ধি করুন।";
        } else {
            adviseText = "• লাভ সাশ্রয়ী পরামর্শ: অর্জিত নিট মুনাফা সাবেক ক্যাশের সাথে যুক্ত করুন এবং অপ্রয়োজনীয় নগদ উত্তোলন এড়াতে একটি নির্দিষ্ট সাপ্তাহিক বাজেট মেনে চলুন।";
        }
        this.binding.tvAnalysisAdvise.setText(adviseText);
        this.binding.layoutDashHistoryList.removeAllViews();
        boolean isEmpty = filtered.isEmpty();
        ActivityMainBinding activityMainBinding2 = this.binding;
        if (isEmpty) {
            activityMainBinding2.layoutDashHistoryEmpty.setVisibility(0);
            return;
        }
        activityMainBinding2.layoutDashHistoryEmpty.setVisibility(8);
        Iterator<MainViewModel.DaySummary> it4 = filtered.iterator();
        while (it4.hasNext()) {
            final MainViewModel.DaySummary ds2 = it4.next();
            Map<String, Double> categoryTotals2 = categoryTotals;
            String maxCategory3 = maxCategory;
            View row = getLayoutInflater().inflate(R.layout.item_dashboard_history, (ViewGroup) this.binding.layoutDashHistoryList, false);
            TextView tvDate = (TextView) row.findViewById(R.id.tvHistoryDate);
            TextView tvDay = (TextView) row.findViewById(R.id.tvHistoryDay);
            TextView tvPill = (TextView) row.findViewById(R.id.tvHistoryStatusPill);
            String adviseText2 = adviseText;
            TextView tvSales = (TextView) row.findViewById(R.id.tvHistorySales);
            Iterator<MainViewModel.DaySummary> it5 = it4;
            TextView tvExpenses = (TextView) row.findViewById(R.id.tvHistoryExpenses);
            String highestExpenseText = formattedMaxVal;
            View card = row.findViewById(R.id.cardHistoryRow);
            double totalExpSum2 = totalExpSum;
            tvDate.setText(ds2.dateKey);
            tvDay.setText(getBengaliDayFromDateKey(ds2.dateKey));
            tvSales.setText("৳ " + PdfExporter.formatBengaliNumber(ds2.computedSale));
            tvExpenses.setText("৳ " + PdfExporter.formatBengaliNumber(ds2.expenses));
            if (ds2.margin > d) {
                tvPill.setText("লাভ ৳ " + PdfExporter.formatBengaliNumber(ds2.margin));
                tvPill.setTextColor(Color.parseColor("#16A34A"));
                tvPill.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
            } else if (ds2.margin < d) {
                tvPill.setText("ঘাটতি ৳ " + PdfExporter.formatBengaliNumber(Math.abs(ds2.margin)));
                tvPill.setTextColor(Color.parseColor("#DC2626"));
                tvPill.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
            } else {
                tvPill.setText("সমান");
                tvPill.setTextColor(Color.parseColor("#059669"));
                tvPill.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ECFDF5")));
            }
            card.setOnClickListener(new View.OnClickListener() { // from class: com.example.MainActivity$$ExternalSyntheticLambda3
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.this.m7046lambda$updateDashboardUI$32$comexampleMainActivity(ds2, view);
                }
            });
            this.binding.layoutDashHistoryList.addView(row);
            categoryTotals = categoryTotals2;
            maxCategory = maxCategory3;
            adviseText = adviseText2;
            it4 = it5;
            formattedMaxVal = highestExpenseText;
            totalExpSum = totalExpSum2;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$updateDashboardUI$32$com-example-MainActivity, reason: not valid java name */
    public /* synthetic */ void m7046lambda$updateDashboardUI$32$comexampleMainActivity(MainViewModel.DaySummary ds, View v) {
        String[] parts = ds.dateKey.split("-");
        if (parts.length == 3) {
            try {
                int d = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]) - 1;
                int y = Integer.parseInt(parts[2]);
                this.viewModel.selectDate(y, m, d);
                TabLayout.Tab tab = this.binding.tabLayout.getTabAt(0);
                if (tab != null) {
                    tab.select();
                }
                Toast.makeText(this, ds.dateKey + " তারিখের হিসাব খোলা হয়েছে", 0).show();
            } catch (Exception e) {
            }
        }
    }

    private String getBengaliDayFromDateKey(String dateKey) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            Date parsed = sdf.parse(dateKey);
            if (parsed != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(parsed);
                int day = cal.get(7);
                switch (day) {
                    case 1:
                        return " (রবিবার)";
                    case 2:
                        return " (সোমবার)";
                    case 3:
                        return " (মঙ্গলবার)";
                    case 4:
                        return " (বুধবার)";
                    case 5:
                        return " (বৃহস্পতিবার)";
                    case 6:
                        return " (শুক্রবার)";
                    case 7:
                        return " (শনিবার)";
                    default:
                        return "";
                }
            }
        } catch (Exception e) {
        }
        return "";
    }

    public void updateCloudBackupUI() {
        setupCloudBackup();
        updateSupabaseSyncCardUI();
        updateInPageHomeExpensesUI();
        updateHeaderSyncStatusUI();
    }

    private void setupCloudBackup() {
        setupGoogleSheetsSync();
        setupSupabaseSync();
        setupInPageHomeExpenses();
        updateUserProfileHeader();
        updateHeaderSyncStatusUI();
    }

    private void updateHeaderSyncStatusUI() {
        if (this.binding == null || this.binding.btnHeaderCloudSync == null) return;
        SupabaseAuthManager authManager = SupabaseAuthManager.getInstance(this);
        MawaSyncManager syncManager = MawaSyncManager.getInstance(this);

        if (syncManager.isSyncing()) {
            if (this.binding.tvHeaderSyncText != null) {
                this.binding.tvHeaderSyncText.setText("সিঙ্ক হচ্ছে...");
            }
            if (this.binding.ivHeaderSyncIcon != null) {
                this.binding.ivHeaderSyncIcon.setImageResource(R.drawable.ic_cloud);
                this.binding.ivHeaderSyncIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
            }
            this.binding.btnHeaderCloudSync.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DBEAFE")));
        } else if (authManager.isLoggedIn()) {
            if (this.binding.tvHeaderSyncText != null) {
                this.binding.tvHeaderSyncText.setText("ক্লাউড সিঙ্ক");
            }
            if (this.binding.ivHeaderSyncIcon != null) {
                this.binding.ivHeaderSyncIcon.setImageResource(R.drawable.ic_cloud);
                this.binding.ivHeaderSyncIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#059669")));
            }
            this.binding.btnHeaderCloudSync.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ECFDF5")));
        } else {
            if (this.binding.tvHeaderSyncText != null) {
                this.binding.tvHeaderSyncText.setText("অফলাইন");
            }
            if (this.binding.ivHeaderSyncIcon != null) {
                this.binding.ivHeaderSyncIcon.setImageResource(R.drawable.ic_cloud);
                this.binding.ivHeaderSyncIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#64748B")));
            }
            this.binding.btnHeaderCloudSync.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
        }
    }

    private void setupSupabaseSync() {
        if (this.binding == null) return;
        if (this.binding.btnSupabaseAuthAction != null) {
            this.binding.btnSupabaseAuthAction.setOnClickListener(v -> showSupabaseAuthDialog());
        }
        if (this.binding.btnSupabaseSyncNow != null) {
            this.binding.btnSupabaseSyncNow.setOnClickListener(v -> performSupabaseManualSync());
        }

        MawaSyncManager.getInstance(this).addRemoteDataChangeListener(() -> {
            runOnUiThread(() -> {
                if (viewModel != null) {
                    viewModel.loadSavedData();
                }
                updateDashboardUI();
                updateBakiKhataUI();
                updateFordiKhataUI();
                updateHeaderSyncStatusUI();
                updateSupabaseSyncCardUI();
            });
        });

        updateSupabaseSyncCardUI();
    }

    private void updateSupabaseSyncCardUI() {
        if (this.binding == null) return;
        SupabaseAuthManager authManager = SupabaseAuthManager.getInstance(this);
        MawaSyncManager syncManager = MawaSyncManager.getInstance(this);

        boolean loggedIn = authManager.isLoggedIn();
        if (this.binding.tvUserAccountEmail != null) {
            if (loggedIn) {
                String email = authManager.getUserEmail();
                this.binding.tvUserAccountEmail.setText(!TextUtils.isEmpty(email) ? ("লগইন: " + email) : "সক্রিয় ক্লাউড অ্যাকাউন্ট");
            } else {
                this.binding.tvUserAccountEmail.setText("অফলাইন মোড (কোনো একাউন্ট যুক্ত নেই)");
            }
        }

        if (this.binding.tvUserDisplayName != null) {
            if (loggedIn) {
                String name = authManager.getUserName();
                this.binding.tvUserDisplayName.setText(!TextUtils.isEmpty(name) ? name : "মাওয়া স্টোর গ্রাহক (ক্লাউড সক্রিয়)");
            } else {
                this.binding.tvUserDisplayName.setText("মাওয়া স্টোর (অফলাইন)");
            }
        }

        if (this.binding.btnSupabaseAuthAction != null) {
            if (loggedIn) {
                this.binding.btnSupabaseAuthAction.setText("লগআউট");
                this.binding.btnSupabaseAuthAction.setTextColor(Color.parseColor("#EF4444"));
            } else {
                this.binding.btnSupabaseAuthAction.setText("লগইন / রেজিস্টার");
                this.binding.btnSupabaseAuthAction.setTextColor(Color.parseColor("#2563EB"));
            }
        }
    }

    private void performSupabaseManualSync() {
        if (this.binding != null && this.binding.progressSupabaseSync != null) {
            this.binding.progressSupabaseSync.setVisibility(View.VISIBLE);
        }
        if (this.binding != null && this.binding.btnSupabaseSyncNow != null) {
            this.binding.btnSupabaseSyncNow.setEnabled(false);
        }
        updateHeaderSyncStatusUI();

        MawaSyncManager.getInstance(this).syncAsync(new MawaSyncManager.SyncCallback() {
            @Override
            public void onSyncStarted() {
                runOnUiThread(() -> updateHeaderSyncStatusUI());
            }

            @Override
            public void onSyncSuccess(String message) {
                runOnUiThread(() -> {
                    if (binding != null && binding.progressSupabaseSync != null) {
                        binding.progressSupabaseSync.setVisibility(View.GONE);
                    }
                    if (binding != null && binding.btnSupabaseSyncNow != null) {
                        binding.btnSupabaseSyncNow.setEnabled(true);
                    }
                    updateSupabaseSyncCardUI();
                    updateHeaderSyncStatusUI();
                    if (viewModel != null) {
                        viewModel.loadSavedData();
                    }
                    updateDashboardUI();
                    updateBakiKhataUI();
                    updateFordiKhataUI();
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onSyncFailed(String error) {
                runOnUiThread(() -> {
                    if (binding != null && binding.progressSupabaseSync != null) {
                        binding.progressSupabaseSync.setVisibility(View.GONE);
                    }
                    if (binding != null && binding.btnSupabaseSyncNow != null) {
                        binding.btnSupabaseSyncNow.setEnabled(true);
                    }
                    updateSupabaseSyncCardUI();
                    updateHeaderSyncStatusUI();
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showSupabaseAuthDialog() {
        final SupabaseAuthManager authManager = SupabaseAuthManager.getInstance(this);
        if (authManager.isLoggedIn()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("লগআউট নিশ্চিতকরণ")
                    .setMessage("আপনি কি \"" + authManager.getUserEmail() + "\" অ্যাকাউন্ট থেকে লগআউট করতে চান? আপনার লোকাল ডেটা সংরক্ষিত থাকবে।")
                    .setPositiveButton("লগআউট", (dialog, which) -> {
                        authManager.logout();
                        updateSupabaseSyncCardUI();
                        updateHeaderSyncStatusUI();
                        Toast.makeText(MainActivity.this, "লগআউট সম্পন্ন হয়েছে", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
            return;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_supabase_glass_auth);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final TextView btnTabLogin = dialog.findViewById(R.id.btnTabLogin);
        final TextView btnTabRegister = dialog.findViewById(R.id.btnTabRegister);
        final LinearLayout layoutAuthForm = dialog.findViewById(R.id.layoutAuthForm);
        final LinearLayout layoutAuthNameField = dialog.findViewById(R.id.layoutAuthNameField);
        final LinearLayout layoutForgotPasswordView = dialog.findViewById(R.id.layoutForgotPasswordView);
        final LinearLayout layoutAuthModeTabs = dialog.findViewById(R.id.layoutAuthModeTabs);
        final EditText etAuthDisplayName = dialog.findViewById(R.id.etAuthDisplayName);
        final EditText etAuthEmail = dialog.findViewById(R.id.etAuthEmail);
        final EditText etAuthPassword = dialog.findViewById(R.id.etAuthPassword);
        final EditText etForgotEmail = dialog.findViewById(R.id.etForgotEmail);
        final ImageView btnTogglePassVisibility = dialog.findViewById(R.id.btnTogglePassVisibility);
        final TextView btnForgotPassword = dialog.findViewById(R.id.btnForgotPassword);
        final com.google.android.material.button.MaterialButton btnAuthSubmit = dialog.findViewById(R.id.btnAuthSubmit);
        final com.google.android.material.button.MaterialButton btnSendPasswordReset = dialog.findViewById(R.id.btnSendPasswordReset);
        final TextView btnBackToLoginFromForgot = dialog.findViewById(R.id.btnBackToLoginFromForgot);
        final ImageView btnAuthClose = dialog.findViewById(R.id.btnAuthClose);
        final View progressAuthLoading = dialog.findViewById(R.id.progressAuthLoading);

        final boolean[] isRegisterMode = new boolean[]{false};
        final boolean[] isPassVisible = new boolean[]{false};

        Runnable updateTabsUI = () -> {
            if (isRegisterMode[0]) {
                btnTabRegister.setBackgroundResource(R.drawable.bg_dark_chip_selected_purple);
                btnTabRegister.setTextColor(Color.WHITE);
                btnTabLogin.setBackgroundColor(Color.TRANSPARENT);
                btnTabLogin.setTextColor(Color.parseColor("#94A3B8"));
                layoutAuthNameField.setVisibility(View.VISIBLE);
                btnForgotPassword.setVisibility(View.GONE);
                btnAuthSubmit.setText("নতুন অ্যাকাউন্ট তৈরি করুন");
                btnAuthSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7C3AED")));
            } else {
                btnTabLogin.setBackgroundResource(R.drawable.bg_dark_chip_selected_purple);
                btnTabLogin.setTextColor(Color.WHITE);
                btnTabRegister.setBackgroundColor(Color.TRANSPARENT);
                btnTabRegister.setTextColor(Color.parseColor("#94A3B8"));
                layoutAuthNameField.setVisibility(View.GONE);
                btnForgotPassword.setVisibility(View.VISIBLE);
                btnAuthSubmit.setText("লগইন করুন");
                btnAuthSubmit.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
            }
        };

        btnTabLogin.setOnClickListener(v -> {
            isRegisterMode[0] = false;
            updateTabsUI.run();
        });

        btnTabRegister.setOnClickListener(v -> {
            isRegisterMode[0] = true;
            updateTabsUI.run();
        });

        btnTogglePassVisibility.setOnClickListener(v -> {
            isPassVisible[0] = !isPassVisible[0];
            if (isPassVisible[0]) {
                etAuthPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                etAuthPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            etAuthPassword.setSelection(etAuthPassword.getText().length());
        });

        btnForgotPassword.setOnClickListener(v -> {
            layoutAuthForm.setVisibility(View.GONE);
            layoutAuthModeTabs.setVisibility(View.GONE);
            layoutForgotPasswordView.setVisibility(View.VISIBLE);
            String currentEmail = etAuthEmail.getText() != null ? etAuthEmail.getText().toString().trim() : "";
            if (!currentEmail.isEmpty()) {
                etForgotEmail.setText(currentEmail);
            }
        });

        btnBackToLoginFromForgot.setOnClickListener(v -> {
            layoutForgotPasswordView.setVisibility(View.GONE);
            layoutAuthForm.setVisibility(View.VISIBLE);
            layoutAuthModeTabs.setVisibility(View.VISIBLE);
        });

        btnSendPasswordReset.setOnClickListener(v -> {
            String fEmail = etForgotEmail.getText() != null ? etForgotEmail.getText().toString().trim() : "";
            if (fEmail.isEmpty() || !fEmail.contains("@")) {
                etForgotEmail.setError("সঠিক ইমেইল লিখুন");
                etForgotEmail.requestFocus();
                return;
            }
            btnSendPasswordReset.setEnabled(false);
            btnSendPasswordReset.setText("পাঠানো হচ্ছে...");
            authManager.resetPassword(fEmail, new SupabaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess(SupabaseAuthManager.AuthSession session) {
                    runOnUiThread(() -> {
                        btnSendPasswordReset.setEnabled(true);
                        btnSendPasswordReset.setText("পাসওয়ার্ড রিসেট লিংক পাঠান");
                        Toast.makeText(MainActivity.this, "পাসওয়ার্ড রিসেট ইমেইল পাঠানো হয়েছে! আপনার ইনবক্স চেক করুন।", Toast.LENGTH_LONG).show();
                        layoutForgotPasswordView.setVisibility(View.GONE);
                        layoutAuthForm.setVisibility(View.VISIBLE);
                        layoutAuthModeTabs.setVisibility(View.VISIBLE);
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        btnSendPasswordReset.setEnabled(true);
                        btnSendPasswordReset.setText("পাসওয়ার্ড রিসেট লিংক পাঠান");
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        btnAuthClose.setOnClickListener(v -> dialog.dismiss());

        btnAuthSubmit.setOnClickListener(v -> {
            String email = etAuthEmail.getText() != null ? etAuthEmail.getText().toString().trim() : "";
            String pass = etAuthPassword.getText() != null ? etAuthPassword.getText().toString().trim() : "";
            String name = etAuthDisplayName.getText() != null ? etAuthDisplayName.getText().toString().trim() : "";

            if (email.isEmpty() || !email.contains("@")) {
                etAuthEmail.setError("সঠিক ইমেইল ঠিকানা দিন");
                etAuthEmail.requestFocus();
                return;
            }
            if (pass.length() < 6) {
                etAuthPassword.setError("কমপক্ষে ৬ অক্ষরের পাসওয়ার্ড দিন");
                etAuthPassword.requestFocus();
                return;
            }

            btnAuthSubmit.setEnabled(false);
            if (progressAuthLoading != null) progressAuthLoading.setVisibility(View.VISIBLE);

            if (isRegisterMode[0]) {
                authManager.signUp(email, pass, name.isEmpty() ? "দোকানদার" : name, new SupabaseAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(SupabaseAuthManager.AuthSession session) {
                        runOnUiThread(() -> {
                            if (progressAuthLoading != null) progressAuthLoading.setVisibility(View.GONE);
                            btnAuthSubmit.setEnabled(true);
                            dialog.dismiss();
                            updateSupabaseSyncCardUI();
                            updateHeaderSyncStatusUI();
                            Toast.makeText(MainActivity.this, "ক্লাউড অ্যাকাউন্ট সফলভাবে তৈরি হয়েছে", Toast.LENGTH_LONG).show();
                            performSupabaseManualSync();
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            if (progressAuthLoading != null) progressAuthLoading.setVisibility(View.GONE);
                            btnAuthSubmit.setEnabled(true);
                            Toast.makeText(MainActivity.this, "সাইনআপ ব্যর্থ: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } else {
                authManager.signInWithEmail(email, pass, new SupabaseAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(SupabaseAuthManager.AuthSession session) {
                        runOnUiThread(() -> {
                            if (progressAuthLoading != null) progressAuthLoading.setVisibility(View.GONE);
                            btnAuthSubmit.setEnabled(true);
                            dialog.dismiss();
                            updateSupabaseSyncCardUI();
                            updateHeaderSyncStatusUI();
                            Toast.makeText(MainActivity.this, "লগইন সফল হয়েছে", Toast.LENGTH_SHORT).show();
                            performSupabaseManualSync();
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            if (progressAuthLoading != null) progressAuthLoading.setVisibility(View.GONE);
                            btnAuthSubmit.setEnabled(true);
                            Toast.makeText(MainActivity.this, "লগইন ব্যর্থ: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
        });

        dialog.show();
    }

    private void showCloudSyncQuickDialog() {
        SupabaseAuthManager authManager = SupabaseAuthManager.getInstance(this);
        MawaSyncManager syncManager = MawaSyncManager.getInstance(this);
        boolean loggedIn = authManager.isLoggedIn();

        String title = loggedIn ? "ক্লাউড সিঙ্ক সক্রিয়" : "ক্লাউড সিঙ্ক স্ট্যাটাস";
        String message = (loggedIn ? "অ্যাকাউন্ট: " + authManager.getUserEmail() + "\n" : "স্ট্যাটাস: অফলাইন (কোনো ক্লাউড অ্যাকাউন্ট লগইন নেই)\n")
                + "সর্বশেষ সিঙ্ক: " + syncManager.getLastSyncTimeFormatted() + "\n\n"
                + "এখনই ক্লাউডের সাথে আপনার হিসাব সিঙ্ক করতে চান?";

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("এখনই সিঙ্ক করুন", (dialog, which) -> {
                    performSupabaseManualSync();
                })
                .setNeutralButton("ক্লাউড পেজে যান", (dialog, which) -> {
                    if (this.binding != null && this.binding.tabLayout != null) {
                        this.binding.tabLayout.selectTab(this.binding.tabLayout.getTabAt(4));
                    }
                })
                .setNegativeButton("বন্ধ করুন", null)
                .show();
    }

    private void setupInPageHomeExpenses() {
        if (this.binding == null) return;

        if (inPageHomeExpenseSelectedDate == null || inPageHomeExpenseSelectedDate.isEmpty()) {
            inPageHomeExpenseSelectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        }

        if (this.binding.btnInPageHomeExpenseDatePicker != null) {
            this.binding.btnInPageHomeExpenseDatePicker.setText(PdfExporter.toBengaliDigits(inPageHomeExpenseSelectedDate));
            this.binding.btnInPageHomeExpenseDatePicker.setOnClickListener(v -> {
                Calendar c = Calendar.getInstance();
                try {
                    Date d = new SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(inPageHomeExpenseSelectedDate);
                    if (d != null) c.setTime(d);
                } catch (Exception ignored) {}

                DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                    inPageHomeExpenseSelectedDate = String.format(Locale.US, "%02d-%02d-%04d", dayOfMonth, month + 1, year);
                    if (binding.btnInPageHomeExpenseDatePicker != null) {
                        binding.btnInPageHomeExpenseDatePicker.setText(PdfExporter.toBengaliDigits(inPageHomeExpenseSelectedDate));
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                dpd.show();
            });
        }

        if (this.binding.btnInPageSaveHomeExpense != null) {
            this.binding.btnInPageSaveHomeExpense.setOnClickListener(v -> {
                String amtStr = binding.etInPageHomeExpenseAmount.getText() != null ? binding.etInPageHomeExpenseAmount.getText().toString().trim() : "";
                if (amtStr.isEmpty()) {
                    Toast.makeText(this, "খরচের টাকার পরিমাণ লিখুন!", Toast.LENGTH_SHORT).show();
                    binding.etInPageHomeExpenseAmount.requestFocus();
                    return;
                }

                double amt;
                try {
                    amt = Double.parseDouble(amtStr);
                    if (amt <= 0) {
                        Toast.makeText(this, "সঠিক টাকার অঙ্ক লিখুন!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "সঠিক টাকার অঙ্ক লিখুন!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String category = "বাজার ও খাবার খরচ";
                if (binding.chipGroupHomeCategory != null) {
                    int checkedId = binding.chipGroupHomeCategory.getCheckedChipId();
                    if (checkedId == R.id.chipCatBill) {
                        category = "বিল ও ইউটিলিটি";
                    } else if (checkedId == R.id.chipCatRent) {
                        category = "বাসা ভাড়া";
                    } else if (checkedId == R.id.chipCatMed) {
                        category = "ওষুধ ও চিকিৎসা";
                    } else if (checkedId == R.id.chipCatStudy) {
                        category = "সন্তান ও পড়াশোনা";
                    } else if (checkedId == R.id.chipCatShop) {
                        category = "ব্যক্তিগত হাতখরচ";
                    } else if (checkedId == R.id.chipCatOther) {
                        category = "অন্যান্য পারিবারিক খরচ";
                    } else {
                        category = "বাজার ও খাবার খরচ";
                    }
                }

                String detail = binding.etInPageHomeExpenseDesc.getText() != null ? binding.etInPageHomeExpenseDesc.getText().toString().trim() : "";
                String finalName = detail.isEmpty() ? category : (category + " - " + detail);

                String dateKey = (inPageHomeExpenseSelectedDate != null && !inPageHomeExpenseSelectedDate.isEmpty())
                        ? inPageHomeExpenseSelectedDate
                        : new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
                String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                String expId = UUID.randomUUID().toString();

                ExpenseModel exp = new ExpenseModel(expId, finalName, amt, dateKey, timeStr, ExpenseModel.TYPE_HOME, ExpenseModel.TYPE_HOME);
                StorageManager.getInstance(this).addExpense(exp);

                if (viewModel != null) {
                    viewModel.loadSavedData();
                }

                binding.etInPageHomeExpenseAmount.setText("");
                binding.etInPageHomeExpenseDesc.setText("");

                updateInPageHomeExpensesUI();
                updateDashboardUI();
                planAutoCloudBackup();
                Toast.makeText(this, "সংসার খরচ ৳ " + PdfExporter.formatBengaliNumber(amt) + " যুক্ত হয়েছে", Toast.LENGTH_SHORT).show();
            });
        }

        if (this.binding.btnFilterHomeCurrentMonth != null) {
            this.binding.btnFilterHomeCurrentMonth.setOnClickListener(v -> {
                inPageHomeExpenseFilter = "CURRENT_MONTH";
                updateInPageHomeExpensesFilterButtons();
                renderInPageHomeExpensesList();
            });
        }

        if (this.binding.btnFilterHomeAll != null) {
            this.binding.btnFilterHomeAll.setOnClickListener(v -> {
                inPageHomeExpenseFilter = "ALL";
                updateInPageHomeExpensesFilterButtons();
                renderInPageHomeExpensesList();
            });
        }

        updateInPageHomeExpensesFilterButtons();
        updateInPageHomeExpensesUI();
    }

    private void updateInPageHomeExpensesFilterButtons() {
        if (this.binding == null) return;
        boolean isMonth = "CURRENT_MONTH".equals(inPageHomeExpenseFilter);
        if (this.binding.btnFilterHomeCurrentMonth != null) {
            this.binding.btnFilterHomeCurrentMonth.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isMonth ? "#7C3AED" : "#F1F5F9")));
            this.binding.btnFilterHomeCurrentMonth.setTextColor(Color.parseColor(isMonth ? "#FFFFFF" : "#64748B"));
        }
        if (this.binding.btnFilterHomeAll != null) {
            this.binding.btnFilterHomeAll.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(!isMonth ? "#7C3AED" : "#F1F5F9")));
            this.binding.btnFilterHomeAll.setTextColor(Color.parseColor(!isMonth ? "#FFFFFF" : "#64748B"));
        }
    }

    private void updateInPageHomeExpensesUI() {
        if (this.binding == null) return;
        AccountingService accounting = AccountingService.getInstance(this);
        List<ExpenseModel> homeExpenses = accounting.getHomeExpenses();

        AccountingService.MonthlyAccountingSummary mSummary = accounting.calculateCurrentMonthSummary();
        double monthTotal = mSummary.homeExpenses;

        String todayDate = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        double todayTotal = 0;
        for (ExpenseModel exp : homeExpenses) {
            if (exp != null && todayDate.equals(exp.getDate())) {
                todayTotal += exp.getAmount();
            }
        }

        if (this.binding.tvHomeMonthlyTotal != null) {
            this.binding.tvHomeMonthlyTotal.setText("৳ " + PdfExporter.formatBengaliNumber(monthTotal));
        }
        if (this.binding.tvHomeTodayTotal != null) {
            this.binding.tvHomeTodayTotal.setText("৳ " + PdfExporter.formatBengaliNumber(todayTotal));
        }
        if (this.binding.tvHomeEntryCount != null) {
            this.binding.tvHomeEntryCount.setText(PdfExporter.toBengaliDigits(String.valueOf(homeExpenses.size())) + " টি এন্ট্রি");
        }

        renderInPageHomeExpensesList();
    }

    private void renderInPageHomeExpensesList() {
        if (this.binding == null || this.binding.layoutInPageHomeExpenseRows == null) return;
        this.binding.layoutInPageHomeExpenseRows.removeAllViews();

        AccountingService accounting = AccountingService.getInstance(this);
        List<ExpenseModel> homeExpenses = accounting.getHomeExpenses();

        String currentMonthYearSuffix = new SimpleDateFormat("-MM-yyyy", Locale.US).format(new Date());
        List<ExpenseModel> displayList = new ArrayList<>();
        if ("CURRENT_MONTH".equals(inPageHomeExpenseFilter)) {
            for (ExpenseModel exp : homeExpenses) {
                if (exp != null && exp.getDate() != null && exp.getDate().endsWith(currentMonthYearSuffix)) {
                    displayList.add(exp);
                }
            }
        } else {
            displayList.addAll(homeExpenses);
        }

        if (displayList.isEmpty()) {
            if (this.binding.layoutInPageHomeExpenseEmpty != null) {
                this.binding.layoutInPageHomeExpenseEmpty.setVisibility(View.VISIBLE);
            }
            this.binding.layoutInPageHomeExpenseRows.setVisibility(View.GONE);
            return;
        }

        if (this.binding.layoutInPageHomeExpenseEmpty != null) {
            this.binding.layoutInPageHomeExpenseEmpty.setVisibility(View.GONE);
        }
        this.binding.layoutInPageHomeExpenseRows.setVisibility(View.VISIBLE);

        for (int i = 0; i < displayList.size(); i++) {
            final ExpenseModel exp = displayList.get(i);

            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dpToPx(14));
            card.setCardElevation(0);
            card.setStrokeWidth(dpToPx(1));
            card.setStrokeColor(Color.parseColor("#E2E8F0"));
            card.setCardBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, 0, 0, dpToPx(8));
            card.setLayoutParams(cardLp);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

            // Icon badge
            FrameLayout iconFrame = new FrameLayout(this);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dpToPx(38), dpToPx(38));
            iconLp.setMarginEnd(dpToPx(12));
            iconFrame.setLayoutParams(iconLp);
            iconFrame.setBackgroundResource(R.drawable.shape_circle);
            iconFrame.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F3FF")));

            ImageView ivCat = new ImageView(this);
            FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(dpToPx(20), dpToPx(20), Gravity.CENTER);
            ivCat.setLayoutParams(ivLp);
            ivCat.setImageResource(R.drawable.ic_receipt);
            ivCat.setImageTintList(ColorStateList.valueOf(Color.parseColor("#7C3AED")));
            iconFrame.addView(ivCat);
            row.addView(iconFrame);

            // Text Info
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            textLp.setMarginEnd(dpToPx(8));
            textCol.setLayoutParams(textLp);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(exp.getName());
            tvTitle.setTextColor(Color.parseColor("#0F172A"));
            tvTitle.setTextSize(14.0f);
            tvTitle.setTypeface(null, Typeface.BOLD);
            textCol.addView(tvTitle);

            TextView tvDate = new TextView(this);
            String dateFormatted = exp.getDate() != null ? PdfExporter.toBengaliDigits(exp.getDate()) : "";
            String timeFormatted = exp.getTime() != null ? exp.getTime() : "";
            tvDate.setText(dateFormatted + (timeFormatted.isEmpty() ? "" : " • " + timeFormatted));
            tvDate.setTextColor(Color.parseColor("#64748B"));
            tvDate.setTextSize(11.5f);
            tvDate.setPadding(0, dpToPx(2), 0, 0);
            textCol.addView(tvDate);
            row.addView(textCol);

            // Amount
            TextView tvAmt = new TextView(this);
            tvAmt.setText("৳ " + PdfExporter.formatBengaliNumber(exp.getAmount()));
            tvAmt.setTextColor(Color.parseColor("#7C3AED"));
            tvAmt.setTextSize(15.0f);
            tvAmt.setTypeface(null, Typeface.BOLD);
            row.addView(tvAmt);

            // Delete Button
            ImageView ivDel = new ImageView(this);
            LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28));
            delLp.setMarginStart(dpToPx(8));
            ivDel.setLayoutParams(delLp);
            ivDel.setImageResource(R.drawable.ic_trash);
            ivDel.setImageTintList(ColorStateList.valueOf(Color.parseColor("#CBD5E1")));
            ivDel.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            ivDel.setClickable(true);
            ivDel.setFocusable(true);
            ivDel.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("খরচ মুছে ফেলবেন?")
                        .setMessage("আপনি কি নিশ্চিতভাবে এই খরচটি মুছে ফেলতে চান?")
                        .setPositiveButton("মুছুন", (dialog, which) -> {
                            StorageManager.getInstance(MainActivity.this).deleteExpense(exp.getId());
                            if (viewModel != null) viewModel.loadSavedData();
                            updateInPageHomeExpensesUI();
                            updateDashboardUI();
                            planAutoCloudBackup();
                            Toast.makeText(MainActivity.this, "সংসার খরচ মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("বাতিল", null)
                        .show();
            });
            row.addView(ivDel);

            card.addView(row);
            this.binding.layoutInPageHomeExpenseRows.addView(card);
        }
    }

    private void updateUserProfileHeader() {
        if (this.binding == null || this.binding.tvUserProfileHeaderName == null) return;
        this.binding.tvUserProfileHeaderName.setText("মাওয়া স্টোর");
        this.binding.ivUserProfileHeaderIcon.setImageResource(R.drawable.ic_cloud);
        this.binding.ivUserProfileHeaderIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#059669")));
        this.binding.btnUserProfileHeader.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#ECFDF5")));
        this.binding.btnUserProfileHeader.setOnClickListener(v -> {
            if (this.binding.layoutCloudBackup.getVisibility() == View.VISIBLE) {
                this.binding.layoutCloudBackup.setVisibility(View.GONE);
                this.binding.layoutDailyLedger.setVisibility(View.VISIBLE);
                this.binding.tabLayout.selectTab(this.binding.tabLayout.getTabAt(0));
            } else {
                this.binding.layoutDailyLedger.setVisibility(View.GONE);
                this.binding.layoutDashboard.setVisibility(View.GONE);
                this.binding.layoutBakiKhata.setVisibility(View.GONE);
                this.binding.layoutFordiKhata.setVisibility(View.GONE);
                this.binding.layoutCloudBackup.setVisibility(View.VISIBLE);
                this.binding.tabLayout.selectTab(this.binding.tabLayout.getTabAt(4));
            }
        });
    }

    private void setupGoogleSheetsSync() {
        final GoogleSheetsSyncManager sheetsSyncManager = GoogleSheetsSyncManager.getInstance(this);
        this.binding.etGoogleSpreadsheetId.setText(sheetsSyncManager.getSpreadsheetId());
        this.binding.etGoogleSheetGid.setText(sheetsSyncManager.getSheetGid());
        this.binding.etGoogleSheetsUrl.setText(sheetsSyncManager.getSheetsUrl());

        if (sheetsSyncManager.isConnected()) {
            this.binding.tvLastSheetsSyncTime.setText("গুগল শিট ও ক্লাউড সিঙ্ক সক্রিয় রয়েছে");
        } else {
            this.binding.tvLastSheetsSyncTime.setText("সর্বশেষ সিঙ্ক: এখনো সিঙ্ক করা হয়নি");
        }

        // Auto extract ID and GID on text paste/change to eliminate manual typing hassle
        this.binding.etGoogleSpreadsheetId.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.toString().contains("http")) {
                    String pasted = s.toString().trim();
                    String extractedId = GoogleSheetsSyncManager.extractSpreadsheetId(pasted);
                    String extractedGid = GoogleSheetsSyncManager.extractGid(pasted);
                    if (binding.etGoogleSheetGid != null && (binding.etGoogleSheetGid.getText() == null || binding.etGoogleSheetGid.getText().toString().isEmpty() || "0".equals(binding.etGoogleSheetGid.getText().toString()))) {
                        binding.etGoogleSheetGid.setText(extractedGid);
                    }
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        this.binding.btnSaveSheetsUrl.setOnClickListener(v -> {
            String spreadsheetIdOrUrl = this.binding.etGoogleSpreadsheetId.getText() != null ? this.binding.etGoogleSpreadsheetId.getText().toString().trim() : "";
            String sheetGid = this.binding.etGoogleSheetGid.getText() != null ? this.binding.etGoogleSheetGid.getText().toString().trim() : "";
            if (sheetGid.isEmpty()) {
                sheetGid = "0";
                this.binding.etGoogleSheetGid.setText("0");
            }
            String webAppUrl = this.binding.etGoogleSheetsUrl.getText() != null ? this.binding.etGoogleSheetsUrl.getText().toString().trim() : "";

            if (spreadsheetIdOrUrl.isEmpty() && webAppUrl.isEmpty()) {
                spreadsheetIdOrUrl = GoogleSheetsSyncManager.DEFAULT_SPREADSHEET_ID;
                this.binding.etGoogleSpreadsheetId.setText(spreadsheetIdOrUrl);
            }

            sheetsSyncManager.saveSheetConfig(spreadsheetIdOrUrl, sheetGid);
            if (!webAppUrl.isEmpty()) {
                sheetsSyncManager.saveSheetsUrl(webAppUrl);
            }

            this.binding.etGoogleSpreadsheetId.setText(sheetsSyncManager.getSpreadsheetId());
            this.binding.etGoogleSheetGid.setText(sheetsSyncManager.getSheetGid());
            this.binding.tvLastSheetsSyncTime.setText("গুগল শিট আইডি ও গিড সফলভাবে সক্রিয় হয়েছে!");
            Toast.makeText(this, "ডাইরেক্ট গুগল শিট আইডি ও গিড সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show();
        });

        this.binding.btnOpenGoogleSheet.setOnClickListener(v -> {
            String sheetUrl = sheetsSyncManager.getSpreadsheetUrl();
            if (sheetUrl.isEmpty()) {
                Toast.makeText(this, "আগে স্প্রেডশিট আইডি দিয়ে সেভ করুন!", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sheetUrl));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "ব্রাউজারে শিট খুলতে ব্যর্থ: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        this.binding.btnSyncToSheetsNow.setOnClickListener(v -> {
            if (!sheetsSyncManager.isConnected()) {
                Toast.makeText(this, "আগে গুগল স্প্রেডশিট আইডি অথবা ওয়েব অ্যাপ লিংক দিন!", Toast.LENGTH_LONG).show();
                return;
            }
            this.binding.progressCloudAction.setVisibility(View.VISIBLE);
            this.binding.btnSyncToSheetsNow.setEnabled(false);
            this.binding.btnCloudRestore.setEnabled(false);

            sheetsSyncManager.syncData(this, new GoogleSheetsSyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        binding.progressCloudAction.setVisibility(View.GONE);
                        binding.btnSyncToSheetsNow.setEnabled(true);
                        binding.btnCloudRestore.setEnabled(true);
                        binding.tvLastSheetsSyncTime.setText("সর্বশেষ সিঙ্ক: সফল (" + new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date()) + ")");
                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle("সিঙ্ক সফল")
                                .setMessage(message + "\n\nমাওয়া স্টোর এর সকল হিসাব গুগল শিটে নিরাপদে সংরক্ষণ করা হয়েছে।")
                                .setPositiveButton("ঠিক আছে", null)
                                .show();
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        binding.progressCloudAction.setVisibility(View.GONE);
                        binding.btnSyncToSheetsNow.setEnabled(true);
                        binding.btnCloudRestore.setEnabled(true);
                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle("সিঙ্ক ব্যর্থ হয়েছে")
                                .setMessage(error + "\n\nসমাধান: স্প্রেডশিট আইডি বা Apps Script URL সঠিক আছে কিনা এবং শিটের এক্সেস 'Anyone with link' করা রয়েছে কিনা চেক করুন।")
                                .setPositiveButton("ঠিক আছে", null)
                                .show();
                    });
                }
            });
        });

        this.binding.btnCloudRestore.setOnClickListener(v -> {
            if (!sheetsSyncManager.isConnected()) {
                Toast.makeText(this, "আগে গুগল স্প্রেডশিট আইডি বা গিড (GID) সেট করুন!", Toast.LENGTH_LONG).show();
                return;
            }
            new MaterialAlertDialogBuilder(this)
                    .setTitle("গুগল শিট থেকে রিস্টোর")
                    .setMessage("গুগল শিট থেকে ডাটা রিস্টোর করলে অ্যাপে শিটের সংরক্ষিত হিসাবসমূহ লোড হবে। আপনি কি রিস্টোর করতে চান?")
                    .setPositiveButton("হ্যাঁ, রিস্টোর করুন", (dialog, which) -> {
                        binding.progressCloudAction.setVisibility(View.VISIBLE);
                        binding.btnSyncToSheetsNow.setEnabled(false);
                        binding.btnCloudRestore.setEnabled(false);

                        sheetsSyncManager.restoreFromGoogleSheet(this, new GoogleSheetsSyncManager.DataCallback() {
                            @Override
                            public void onSuccess(Map<String, Object> data) {
                                runOnUiThread(() -> {
                                    binding.progressCloudAction.setVisibility(View.GONE);
                                    binding.btnSyncToSheetsNow.setEnabled(true);
                                    binding.btnCloudRestore.setEnabled(true);
                                    if (data != null && !data.isEmpty()) {
                                        StorageManager.getInstance(MainActivity.this).importAllData(data);
                                        viewModel.loadSavedData();
                                        updateDashboardUI();
                                        binding.tvLastSheetsSyncTime.setText("সর্বশেষ রিস্টোর: সফল (" + new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date()) + ")");
                                        new MaterialAlertDialogBuilder(MainActivity.this)
                                                .setTitle("রিস্টোর সফল")
                                                .setMessage("গুগল শিট থেকে মাওয়া স্টোর এর সকল হিসাব সফলভাবে অ্যাপে পুনরুদ্ধার করা হয়েছে।")
                                                .setPositiveButton("ঠিক আছে", null)
                                                .show();
                                    } else {
                                        Toast.makeText(MainActivity.this, "শিটে কোনো ডাটা পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                runOnUiThread(() -> {
                                    binding.progressCloudAction.setVisibility(View.GONE);
                                    binding.btnSyncToSheetsNow.setEnabled(true);
                                    binding.btnCloudRestore.setEnabled(true);
                                    new MaterialAlertDialogBuilder(MainActivity.this)
                                            .setTitle("রিস্টোর ব্যর্থ")
                                            .setMessage(error)
                                            .setPositiveButton("ঠিক আছে", null)
                                            .show();
                                });
                            }
                        });
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
        });

        this.binding.btnSheetsInstruction.setOnClickListener(v -> {
            final String scriptCode = "function doPost(e) {\n  try {\n    var json = JSON.parse(e.postData.contents);\n    var ss = SpreadsheetApp.getActiveSpreadsheet();\n    var ledgerSheet = ss.getSheetByName(\"Daily Cash Book\");\n    if (!ledgerSheet) {\n      ledgerSheet = ss.insertSheet(\"Daily Cash Book\");\n    }\n    ledgerSheet.clear();\n    \n    var headers = [\"তারিখ (Date)\", \"সাবেক ক্যাশ (Opening Cash)\", \"মোট খরচ (Total Expenses)\", \"মোট বেচা (Total Sale)\", \"হাতে থাকা ক্যাশ (Cash in Hand)\", \"লাভ / ঘাটতি (Profit/Loss)\"];\n    ledgerSheet.getRange(1, 1, 1, headers.length).setValues([headers]).setFontWeight(\"bold\").setBackground(\"#D1FAE5\");\n    \n    var rows = [];\n    if (json.summaries && json.summaries.length > 0) {\n      for (var i = 0; i < json.summaries.length; i++) {\n        var s = json.summaries[i];\n        rows.push([\n          s.dateKey,\n          s.sabekCash,\n          s.expenses,\n          s.computedSale,\n          s.availableCash,\n          s.profitOrLoss\n        ]);\n      }\n    }\n    \n    if (rows.length > 0) {\n      ledgerSheet.getRange(2, 1, rows.length, headers.length).setValues(rows);\n    }\n    ledgerSheet.autoResizeColumns(1, headers.length);\n    return ContentService.createTextOutput(JSON.stringify({ \n      status: \"success\", \n      message: \"সফলভাবে \" + rows.length + \" দিনের ডাটা সিঙ্ক হয়েছে!\" \n    })).setMimeType(ContentService.MimeType.JSON);\n  } catch (error) {\n    return ContentService.createTextOutput(JSON.stringify({ \n      status: \"error\", \n      message: error.toString() \n    })).setMimeType(ContentService.MimeType.JSON);\n  }\n}";

            ScrollView scrollView = new ScrollView(this);
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(40, 30, 40, 30);

            TextView txtTitle = new TextView(this);
            txtTitle.setText("মাওয়া স্টোর - গুগল শিট সেটআপ গাইড");
            txtTitle.setTextSize(16.0f);
            txtTitle.setTypeface(null, 1);
            txtTitle.setTextColor(Color.parseColor("#047857"));
            txtTitle.setPadding(0, 0, 0, 16);

            TextView txtSteps = new TextView(this);
            txtSteps.setText("১. একটি গুগল শিট (Google Sheet) তৈরি করুন অথবা বিদ্যমান শিট খুলুন।\n\n২. শিটের শেয়ারিং অপশনে গিয়ে 'Anyone with the link can view' (অথবা Viewer/Editor) এক্সেস দিন।\n\n৩. শিটের লিংক অথবা লিঙ্ক থেকে Spreadsheet ID এবং Sheet GID (যেমন: 0) কপি করে অ্যাপের বক্সে বসান।\n\n৪. (ঐচ্ছিক - ফুল অটো সিঙ্ক): শিটের Extensions -> Apps Script এ গিয়ে নিচের কোডটি পেস্ট করে Web app হিসেবে Deploy করুন (Access: Anyone)। সেই URL টি অ্যাপে বসিয়ে দিন।");
            txtSteps.setTextSize(13.0f);
            txtSteps.setTextColor(Color.parseColor("#334155"));
            txtSteps.setLineSpacing(3.0f, 1.15f);
            txtSteps.setPadding(0, 0, 0, 20);

            Button btnCopy = new Button(this);
            btnCopy.setText("Apps Script কোড কপি করুন");
            btnCopy.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#059669")));
            btnCopy.setTextColor(Color.WHITE);
            btnCopy.setOnClickListener(btn -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Google Sheets Apps Script", scriptCode);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "কোড ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show();
            });

            container.addView(txtTitle);
            container.addView(txtSteps);
            container.addView(btnCopy);
            scrollView.addView(container);

            new MaterialAlertDialogBuilder(this)
                    .setView(scrollView)
                    .setPositiveButton("ঠিক আছে, বুঝলাম", null)
                    .show();
        });
    }

    private void setupLocalBackup() {
        this.binding.btnLocalBackupSave.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_TITLE, "mawa_store_backup.json");
                startActivityForResult(intent, 2001);
            } catch (Exception e) {
                Toast.makeText(this, "ব্যাকআপ উইন্ডো খুলতে ব্যর্থ: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        this.binding.btnLocalBackupShare.setOnClickListener(v -> {
            try {
                Map<String, Object> data = StorageManager.getInstance(this).exportAllData();
                String json = new Gson().toJson(data);
                File cacheDir = getCacheDir();
                File backupFile = new File(cacheDir, "mawa_store_backup.json");
                FileWriter writer = new FileWriter(backupFile);
                writer.write(json);
                writer.flush();
                writer.close();
                Uri fileUri = FileProvider.getUriForFile(this, "com.aistudio.dailycashbook.kxmpzq.fileprovider", backupFile);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("*/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "মাওয়া স্টোর ব্যাকআপ ফাইল শেয়ার করুন"));
            } catch (Exception e) {
                Toast.makeText(this, "শেয়ার করতে ব্যর্থ: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        this.binding.btnLocalBackupRestore.setOnClickListener(v -> {
            String[] options = {
                "ফাইল বেছে নিন (.json / .txt সব ফাইল সাপোর্ট)",
                "সরাসরি ব্যাকআপ কোড পেস্ট করে রিস্টোর"
            };
            new MaterialAlertDialogBuilder(this)
                    .setTitle("হিসাব রিস্টোর করুন")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            // File picker - open to ALL file types without restrictive mime filters
                            try {
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                intent.setType("*/*");
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                startActivityForResult(Intent.createChooser(intent, "ব্যাকআপ ফাইল বেছে নিন"), 2002);
                            } catch (Exception e) {
                                try {
                                    Intent intent2 = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                    intent2.setType("*/*");
                                    intent2.addCategory(Intent.CATEGORY_OPENABLE);
                                    startActivityForResult(intent2, 2002);
                                } catch (Exception ex) {
                                    Toast.makeText(this, "ফাইল উইন্ডো খুলতে ব্যর্থ: " + ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            showPasteJsonRestoreDialog();
                        }
                    })
                    .setNegativeButton("বাতিল", null)
                    .show();
        });
    }

    private void showPasteJsonRestoreDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(12));

        TextView hintTv = new TextView(this);
        hintTv.setText("আপনার ব্যাকআপ JSON টেক্সটটি নিচের বক্সে পেস্ট করুন:");
        hintTv.setTextColor(Color.parseColor("#475569"));
        hintTv.setTextSize(12.0f);
        hintTv.setPadding(0, 0, 0, dpToPx(8));
        layout.addView(hintTv);

        com.google.android.material.textfield.TextInputLayout til = new com.google.android.material.textfield.TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        til.setHint("JSON ব্যাকআপ টেক্সট");
        til.setBoxStrokeColor(Color.parseColor("#059669"));
        til.setHintTextColor(ColorStateList.valueOf(Color.parseColor("#059669")));

        final com.google.android.material.textfield.TextInputEditText etJson = new com.google.android.material.textfield.TextInputEditText(this);
        etJson.setTextSize(12.0f);
        etJson.setMinLines(5);
        etJson.setMaxLines(10);
        etJson.setGravity(Gravity.TOP | Gravity.START);
        til.addView(etJson);
        layout.addView(til);

        MaterialButton btnPaste = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnPaste.setText("ক্লিপবোর্ড থেকে পেস্ট করুন");
        btnPaste.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#059669")));
        btnPaste.setTextColor(Color.parseColor("#059669"));
        btnPaste.setCornerRadius(dpToPx(10));
        LinearLayout.LayoutParams lpPaste = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
        lpPaste.setMargins(0, dpToPx(8), 0, 0);
        btnPaste.setLayoutParams(lpPaste);
        btnPaste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                CharSequence clipText = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (clipText != null) {
                    etJson.setText(clipText.toString());
                    Toast.makeText(this, "ক্লিপবোর্ড থেকে পেস্ট করা হয়েছে", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "ক্লিপবোর্ড খালি!", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnPaste);

        new MaterialAlertDialogBuilder(this)
                .setTitle("কোড পেস্ট করে রিস্টোর")
                .setView(layout)
                .setPositiveButton("রিস্টোর সম্পন্ন করুন", (dialog, which) -> {
                    String input = etJson.getText() != null ? etJson.getText().toString() : "";
                    applyJsonRestore(input);
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private boolean applyJsonRestore(String json) {
        if (json == null || json.trim().isEmpty()) {
            Toast.makeText(this, "ব্যাকআপ ফাইল বা টেক্সট খালি!", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> backupData = new Gson().fromJson(json.trim(), mapType);
            if (backupData != null) {
                StorageManager.getInstance(this).importAllData(backupData);
                this.viewModel.loadSavedData();
                updateDashboardUI();
                updateBakiKhataUI();
                updateFordiKhataUI();
                updateCloudBackupUI();
                setupAutocomplete();
                new MaterialAlertDialogBuilder(this)
                        .setTitle("রিস্টোর সফল হয়েছে")
                        .setMessage("মাওয়া স্টোর এর সকল হিসাব (ক্যাশ খাতা, বাকি খাতা, ফর্দ খাতা) সুন্দরভাবে পুনরুদ্ধার সম্পন্ন হয়েছে!")
                        .setPositiveButton("ঠিক আছে", null)
                        .show();
                return true;
            }
            Toast.makeText(this, "ব্যাকআপ ফাইলের ফরম্যাট সঠিক নয়!", Toast.LENGTH_SHORT).show();
            return false;
        } catch (Exception e) {
            Toast.makeText(this, "রিস্টোর ব্যর্থ: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2001 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                String json = new Gson().toJson(StorageManager.getInstance(this).exportAllData());
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    outputStream.close();
                    Toast.makeText(this, "অভিনন্দন! মাওয়া স্টোরের লোকাল ব্যাকআপ ফাইলটি সংরক্ষিত হয়েছে।", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "ব্যাকআপ ফাইলে লিখতে অনুমতি দেয়া হয়নি।", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "ব্যাকআপ ফাইল সংরক্ষণ ব্যর্থ: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == 2002 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri2 = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri2);
                if (inputStream != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    inputStream.close();
                    applyJsonRestore(sb.toString());
                }
            } catch (Exception e2) {
                Toast.makeText(this, "লোকাল রিস্টোর ব্যর্থ: " + e2.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void planAutoCloudBackup() {
        this.backupHandler.removeCallbacks(this.backupRunnable);
        this.backupHandler.postDelayed(this.backupRunnable, 2000L);
    }

    public void triggerAutoCloudBackup() {
        GoogleSheetsSyncManager sheetsSyncManager = GoogleSheetsSyncManager.getInstance(this);
        if (sheetsSyncManager.isConnected()) {
            sheetsSyncManager.syncData(this, new GoogleSheetsSyncManager.SyncCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        if (binding != null && binding.tvLastSheetsSyncTime != null) {
                            binding.tvLastSheetsSyncTime.setText("সর্বশেষ সিঙ্ক: " + new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date()));
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                }
            });
        }

        SupabaseAuthManager authManager = SupabaseAuthManager.getInstance(this);
        if (authManager.isAuthenticated()) {
            MawaSyncManager.getInstance(this).triggerSync(new MawaSyncManager.SyncListener() {
                @Override
                public void onSyncStatusChanged(MawaSyncManager.SyncStatus status, String message) {
                    runOnUiThread(() -> updateHeaderSyncStatusUI());
                }

                @Override
                public void onSyncCompleted(boolean success, String summary) {
                    runOnUiThread(() -> {
                        updateSupabaseSyncCardUI();
                        updateHeaderSyncStatusUI();
                    });
                }
            });
        }
    }


    private void applySuggestedSabekCash() {
        if (this.viewModel == null || this.binding == null) {
            return;
        }
        double suggested = this.viewModel.getSuggestedSabekCash();
        if (suggested > 0.0d) {
            String strVal = String.valueOf(suggested);
            if (strVal.endsWith(".0")) {
                strVal = strVal.substring(0, strVal.length() - 2);
            }
            this.binding.etSabekCash.setText(strVal);
            this.viewModel.setSabekCash(suggested);
            this.binding.btnSuggestSabekCash.setVisibility(View.GONE);
            Toast.makeText(this, "সাবেক হিসেবে গতকালকের ক্যাশ ৳ " + PdfExporter.formatBengaliNumber(suggested) + " গ্রহণ করা হয়েছে", Toast.LENGTH_SHORT).show();
            planAutoCloudBackup();
        }
    }

    private void updateSabekSuggestionUI() {
        if (this.binding == null || this.viewModel == null) {
            return;
        }
        double suggested = this.viewModel.getSuggestedSabekCash();
        double currentSabek = this.viewModel.getSabekCash().getValue() != null ? this.viewModel.getSabekCash().getValue().doubleValue() : 0.0d;
        if (suggested > 0.0d && currentSabek == 0.0d) {
            this.binding.tvSabekSuggestionText.setText("গতকালকের সমাপনী ক্যাশ: ৳ " + PdfExporter.formatBengaliNumber(suggested));
            this.binding.btnSuggestSabekCash.setVisibility(View.VISIBLE);
        } else {
            this.binding.btnSuggestSabekCash.setVisibility(View.GONE);
        }
    }

    public static String getEmojiForProductName(String name) {
        return "";
    }

    private void setupAutocomplete() {
        if (this.binding == null) {
            return;
        }
        List<String> rawSuggestions = StorageManager.getInstance(this).getAllProductSuggestionsWithDefaults();
        final List<ExpenseSuggestion> suggestions = new ArrayList<>();
        for (String raw : rawSuggestions) {
            if (raw != null && !raw.trim().isEmpty()) {
                String cleanName = raw.trim();
                suggestions.add(new ExpenseSuggestion("", cleanName));
            }
        }
        ArrayAdapter<ExpenseSuggestion> adapter = new ArrayAdapter<ExpenseSuggestion>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(suggestions)) {
            private final List<ExpenseSuggestion> originalList = new ArrayList<>(suggestions);

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ExpenseSuggestion item = getItem(position);
                    if (item != null) {
                        ((TextView) view).setText(item.name);
                        ((TextView) view).setPadding(24, 20, 24, 20);
                        ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.0f);
                    }
                }
                return view;
            }

            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        List<ExpenseSuggestion> filtered = new ArrayList<>();
                        if (constraint == null || constraint.toString().trim().isEmpty()) {
                            filtered.addAll(originalList);
                        } else {
                            String query = constraint.toString().trim().toLowerCase();
                            for (ExpenseSuggestion item : originalList) {
                                if (item.name.toLowerCase().contains(query)) {
                                    filtered.add(item);
                                }
                            }
                        }
                        results.values = filtered;
                        results.count = filtered.size();
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        clear();
                        if (results != null && results.count > 0 && results.values instanceof List) {
                            addAll((List<ExpenseSuggestion>) results.values);
                        }
                        notifyDataSetChanged();
                    }

                    @Override
                    public CharSequence convertResultToString(Object resultValue) {
                        if (resultValue instanceof ExpenseSuggestion) {
                            return ((ExpenseSuggestion) resultValue).name;
                        }
                        return super.convertResultToString(resultValue);
                    }
                };
            }
        };
        if (this.binding.etDrawerExpenseName != null) {
            this.binding.etDrawerExpenseName.setAdapter(adapter);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes5.dex */
    public static class ExpenseSuggestion {
        final String emoji;
        final String name;

        ExpenseSuggestion(String emoji, String name) {
            this.emoji = emoji;
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }

    private void setupBakiKhata() {
        if (this.binding == null) {
            return;
        }
        if (this.bakiKhataManager == null) {
            this.bakiKhataManager = new BakiKhataManager(this, this.binding);
        }
        this.bakiKhataManager.setup();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateBakiKhataUI() {
        if (this.binding == null) {
            return;
        }
        if (this.bakiKhataManager == null) {
            this.bakiKhataManager = new BakiKhataManager(this, this.binding);
            this.bakiKhataManager.setup();
        } else {
            this.bakiKhataManager.updateUI();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable createCircleDrawable(String name) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        int hash = name != null ? name.hashCode() : 0;
        int index = Math.abs(hash) % 5;
        String[] colors = {"#EA580C", "#2563EB", "#059669", "#7C3AED", "#DB2777"};
        shape.setColor(Color.parseColor(colors[index]));
        return shape;
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "B";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length > 0) {
            String first = parts[0];
            if (first.length() > 0) {
                return first.substring(0, 1).toUpperCase();
            }
        }
        return "B";
    }

    private String toBengaliDigits(String input) {
        if (input == null) return "";
        char[] bengaliDigits = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append(bengaliDigits[c - '0']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void setupFordiKhata() {
        if (this.binding == null) {
            return;
        }

        // Sub-tabs (Current Fordi vs History Fordi)
        View.OnClickListener currentTabListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.layoutFordiCurrentContainer.setVisibility(View.VISIBLE);
                binding.layoutFordiHistoryContainer.setVisibility(View.GONE);
                binding.btnFordiTabCurrent.setTextColor(Color.parseColor("#C4B5FD"));
                binding.vIndicatorFordiCurrent.setVisibility(View.VISIBLE);
                binding.btnFordiTabHistory.setTextColor(Color.parseColor("#71717A"));
                binding.vIndicatorFordiHistory.setVisibility(View.INVISIBLE);
            }
        };
        this.binding.btnFordiTabCurrent.setOnClickListener(currentTabListener);
        this.binding.layoutTabFordiCurrent.setOnClickListener(currentTabListener);

        View.OnClickListener historyTabListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.layoutFordiCurrentContainer.setVisibility(View.GONE);
                binding.layoutFordiHistoryContainer.setVisibility(View.VISIBLE);
                binding.btnFordiTabHistory.setTextColor(Color.parseColor("#C4B5FD"));
                binding.vIndicatorFordiHistory.setVisibility(View.VISIBLE);
                binding.btnFordiTabCurrent.setTextColor(Color.parseColor("#71717A"));
                binding.vIndicatorFordiCurrent.setVisibility(View.INVISIBLE);
            }
        };
        this.binding.btnFordiTabHistory.setOnClickListener(historyTabListener);
        this.binding.layoutTabFordiHistory.setOnClickListener(historyTabListener);

        // Simple / Card-less Mode Toggle
        if (this.binding.btnFordiToggleMode != null) {
            boolean isCardless = StorageManager.getInstance(this).isFordiCardlessMode();
            this.binding.btnFordiToggleMode.setText(isCardless ? "✨ সিম্পল" : "📊 টেবিল");
            this.binding.btnFordiToggleMode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean current = StorageManager.getInstance(MainActivity.this).isFordiCardlessMode();
                    boolean updated = !current;
                    StorageManager.getInstance(MainActivity.this).setFordiCardlessMode(updated);
                    binding.btnFordiToggleMode.setText(updated ? "✨ সিম্পল" : "📊 টেবিল");
                    FordiModel active = getActiveFordi();
                    if (active != null) {
                        String q = binding.etFordiSearch != null ? binding.etFordiSearch.getText().toString().trim().toLowerCase() : "";
                        renderFordiTableRows(active, q);
                    }
                    Toast.makeText(MainActivity.this, updated ? "কার্ড-লেস সিম্পল মোড চালু হয়েছে" : "টেবিল মোড চালু হয়েছে", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (this.binding.layoutCardlessAddItem != null) {
            this.binding.layoutCardlessAddItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddFordiItemDialog(getActiveFordi());
                }
            });
        }

        // Quick Action: New Fordi
        this.binding.btnFordiActionNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.btnCreateFordi.performClick();
            }
        });

        // Quick Action: Toggle All
        this.binding.btnFordiActionToggleAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FordiModel active = getActiveFordi();
                if (active == null || active.getItems().isEmpty()) {
                    Toast.makeText(MainActivity.this, "ফর্দে কোনো পণ্য নেই", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean anyUnchecked = false;
                for (FordiItemModel it : active.getItems()) {
                    if (!it.isChecked()) {
                        anyUnchecked = true;
                        break;
                    }
                }
                for (FordiItemModel it : active.getItems()) {
                    it.setChecked(anyUnchecked);
                }
                saveActiveFordi(active);
                Toast.makeText(MainActivity.this, anyUnchecked ? "সব পণ্য টিক দেওয়া হয়েছে" : "সব পণ্য আনটিক করা হয়েছে", Toast.LENGTH_SHORT).show();
            }
        });

        // Top Options Menu
        this.binding.btnFordiOptionsMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final FordiModel active = getActiveFordi();
                if (active == null) return;

                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(MainActivity.this, v);
                popup.getMenu().add(0, 1, 0, "নতুন ফর্দ শুরু করুন");
                popup.getMenu().add(0, 2, 1, "সব পণ্য টিক দিন");
                popup.getMenu().add(0, 3, 2, "সব আনটিক করুন");
                popup.getMenu().add(0, 4, 3, "🖼️ ফর্দ ছবি তৈরি করুন");
                popup.getMenu().add(0, 5, 4, "✈️ ফর্দ শেয়ার করুন");
                popup.getMenu().add(0, 6, 5, "⬇️ CSV ডাউনলোড");
                popup.getMenu().add(0, 7, 6, "বর্তমান ফর্দ মুছে ফেলুন");

                popup.setOnMenuItemClickListener(new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case 1:
                                binding.btnCreateFordi.performClick();
                                return true;
                            case 2:
                                for (FordiItemModel it : active.getItems()) it.setChecked(true);
                                saveActiveFordi(active);
                                return true;
                            case 3:
                                for (FordiItemModel it : active.getItems()) it.setChecked(false);
                                saveActiveFordi(active);
                                return true;
                            case 4:
                                exportFordiAsImage(active);
                                return true;
                            case 5:
                                shareFordiList(active);
                                return true;
                            case 6:
                                exportFordiAsCsv(active);
                                return true;
                            case 7:
                                deleteFordiRecord(active);
                                return true;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        });

        // Quick Add Item Button (Purple pill button in screenshot)
        this.binding.btnQuickAddFordiItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddFordiItemDialog(getActiveFordi());
            }
        });

        this.binding.btnEmptyStateAddFordiItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddFordiItemDialog(getActiveFordi());
            }
        });

        // Export Row Buttons (Screenshot: [ 🖼️ ফর্দ ছবি ] [ ✈️ শেয়ার ] [ ⬇️ CSV ])
        this.binding.btnFordiExportImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FordiModel active = getActiveFordi();
                if (active != null) {
                    exportFordiAsImage(active);
                }
            }
        });

        this.binding.btnFordiShareTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FordiModel active = getActiveFordi();
                if (active != null) {
                    shareFordiList(active);
                }
            }
        });

        this.binding.btnFordiExportCsv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FordiModel active = getActiveFordi();
                if (active != null) {
                    exportFordiAsCsv(active);
                }
            }
        });

        // Search Filter
        this.binding.etFordiSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.this.updateFordiKhataUI();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Create New Fordi List Button
        this.binding.btnCreateFordi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                String banglaDate = new SimpleDateFormat("dd MMMM", new Locale("bn", "BD")).format(new Date());
                String id = UUID.randomUUID().toString();
                FordiModel newFordi = new FordiModel(id, banglaDate + " বাজার ফর্দ", dateStr, new ArrayList<>(), "#F0FDFA");
                StorageManager storage = StorageManager.getInstance(MainActivity.this);
                List<FordiModel> allFordi = storage.loadFordiRecords();
                allFordi.add(0, newFordi);
                storage.saveFordiRecords(allFordi);
                MainActivity.this.currentActiveFordiId = newFordi.getId();

                // Switch to current tab and update UI
                binding.layoutTabFordiCurrent.performClick();
                updateFordiKhataUI();
                triggerAutoCloudBackup();

                // Open add item dialog immediately for fast flow
                showAddFordiItemDialog(newFordi);
            }
        });

        // Post to Daily Accounting Button
        this.binding.btnFordiPostToAccounting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final FordiModel active = getActiveFordi();
                if (active == null) return;
                if (active.isPostedToAccounting()) {
                    Toast.makeText(MainActivity.this, "এই ফর্দটি আগেই হিসাবভুক্ত করা হয়েছে!", Toast.LENGTH_SHORT).show();
                    return;
                }
                double actualTotal = active.getActualTotal();
                if (actualTotal <= 0 && active.getPlannedTotal() > 0) {
                    new MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle("সব পণ্য কেনা নিশ্চিত করুন")
                            .setMessage("আপনি কি ফর্দের সব পণ্য পরিকল্পিত মূল্যে কেনা হিসেবে দৈনিক খরচের খাতায় যোগ করতে চান? (মোট ৳" + PdfExporter.formatBengaliNumber(active.getPlannedTotal()) + ")")
                            .setPositiveButton("হ্যাঁ, সব কিনুন ও যোগ করুন", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    for (FordiItemModel item : active.getItems()) {
                                        item.setChecked(true);
                                    }
                                    saveActiveFordi(active);
                                    confirmAndPostToAccounting(active, null, null);
                                }
                            })
                            .setNegativeButton("বাতিল", null)
                            .show();
                    return;
                }
                confirmAndPostToAccounting(active, null, null);
            }
        });

        updateFordiKhataUI();
    }

    private void showEditFordiItemDialog(final FordiItemModel item, final FordiModel activeFordi) {
        if (item == null || activeFordi == null) return;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_fordi_item);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final EditText etName = dialog.findViewById(R.id.etEditProductName);
        final Spinner spUnit = dialog.findViewById(R.id.spEditProductUnit);
        final EditText etQty = dialog.findViewById(R.id.etEditProductQty);
        final EditText etBuyRate = dialog.findViewById(R.id.etEditProductBuyRate);
        final EditText etSellRate = dialog.findViewById(R.id.etEditProductSellRate);
        final TextView tvTotalCost = dialog.findViewById(R.id.tvEditTotalCost);
        final TextView tvTotalProfit = dialog.findViewById(R.id.tvEditTotalProfit);
        final View btnClose = dialog.findViewById(R.id.btnEditItemClose);
        final View btnDelete = dialog.findViewById(R.id.btnEditItemDelete);
        final View btnSave = dialog.findViewById(R.id.btnEditItemSave);

        final String[] unitLabels = {"কেজি", "লিটার", "গ্রাম", "পিস", "প্যাকেট", "বক্স", "ডজন", "বস্তা", "মি.লি."};
        final String[] unitCodes = {"kg", "liter", "gm", "piece", "packet", "box", "dozen", "sack", "ml"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, unitLabels);
        spUnit.setAdapter(unitAdapter);

        etName.setText(item.getProductName());
        for (int i = 0; i < unitCodes.length; i++) {
            if (unitCodes[i].equalsIgnoreCase(item.getUnit())) {
                spUnit.setSelection(i);
                break;
            }
        }

        double initialQty = item.getPlannedQuantity() > 0 ? item.getPlannedQuantity() : 1.0;
        etQty.setText(initialQty == (long) initialQty ? String.format(Locale.US, "%d", (long) initialQty) : String.format(Locale.US, "%.1f", initialQty));
        if (item.getPurchaseRate() > 0) {
            etBuyRate.setText(item.getPurchaseRate() == (long) item.getPurchaseRate() ? String.format(Locale.US, "%d", (long) item.getPurchaseRate()) : String.format(Locale.US, "%.1f", item.getPurchaseRate()));
        }
        if (item.getSellingRate() > 0) {
            etSellRate.setText(item.getSellingRate() == (long) item.getSellingRate() ? String.format(Locale.US, "%d", (long) item.getSellingRate()) : String.format(Locale.US, "%.1f", item.getSellingRate()));
        }

        Runnable calcPreview = () -> {
            double q = 1.0;
            String qs = etQty.getText().toString().trim();
            if (!qs.isEmpty()) {
                try { q = Double.parseDouble(qs); } catch (Exception ignored) {}
            }
            if (q <= 0) q = 1.0;

            double pr = 0.0;
            String prs = etBuyRate.getText().toString().trim();
            if (!prs.isEmpty()) {
                try { pr = Double.parseDouble(prs); } catch (Exception ignored) {}
            }

            double sr = 0.0;
            String srs = etSellRate.getText().toString().trim();
            if (!srs.isEmpty()) {
                try { sr = Double.parseDouble(srs); } catch (Exception ignored) {}
            }

            double totalCost = q * pr;
            double profit = (sr > pr && pr > 0) ? (q * (sr - pr)) : 0.0;
            tvTotalCost.setText("৳ " + PdfExporter.formatBengaliNumber(totalCost));
            tvTotalProfit.setText("৳ " + PdfExporter.formatBengaliNumber(profit));
        };

        calcPreview.run();

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calcPreview.run();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etQty.addTextChangedListener(watcher);
        etBuyRate.addTextChangedListener(watcher);
        etSellRate.addTextChangedListener(watcher);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            activeFordi.getItems().remove(item);
            saveActiveFordi(activeFordi);
            updateFordiKhataUI();
            dialog.dismiss();
            Toast.makeText(MainActivity.this, item.getProductName() + " মোছা হয়েছে", Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (newName.isEmpty()) {
                etName.setError("পণ্যের নাম লিখুন");
                etName.requestFocus();
                return;
            }
            int unitIdx = spUnit.getSelectedItemPosition();
            String newUnit = unitIdx >= 0 && unitIdx < unitCodes.length ? unitCodes[unitIdx] : "kg";

            double q = 1.0;
            String qs = etQty.getText().toString().trim();
            if (!qs.isEmpty()) {
                try { q = Double.parseDouble(qs); } catch (Exception ignored) {}
            }
            if (q <= 0) q = 1.0;

            double pr = 0.0;
            String prs = etBuyRate.getText().toString().trim();
            if (!prs.isEmpty()) {
                try { pr = Double.parseDouble(prs); } catch (Exception ignored) {}
            }

            double sr = 0.0;
            String srs = etSellRate.getText().toString().trim();
            if (!srs.isEmpty()) {
                try { sr = Double.parseDouble(srs); } catch (Exception ignored) {}
            }

            item.setProductName(newName);
            item.setUnit(newUnit);
            item.setPlannedQuantity(q);
            item.setActualQuantity(q);
            item.setPurchaseRate(pr);
            item.setActualPurchaseRate(pr);
            item.setSellingRate(sr);
            item.recalculate();

            // Save in memory
            StorageManager storage1 = StorageManager.getInstance(MainActivity.this);
            ProductModel prod = storage1.findProductByName(newName);
            if (prod == null) {
                prod = new ProductModel(null, newName, newUnit, pr, sr, "বাজার ফর্দ");
            } else {
                if (pr > 0) prod.setLastPurchasePrice(pr);
                if (sr > 0) prod.setSellingPrice(sr);
                prod.setUnit(newUnit);
            }
            storage1.saveOrUpdateProduct(prod);

            saveActiveFordi(activeFordi);
            updateFordiKhataUI();
            dialog.dismiss();
            Toast.makeText(MainActivity.this, newName + " আপডেট হয়েছে", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private FordiModel getActiveFordi() {
        StorageManager storage = StorageManager.getInstance(this);
        List<FordiModel> allFordi = storage.loadFordiRecords();
        if (allFordi.isEmpty()) {
            String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            String banglaDate = new SimpleDateFormat("dd MMMM", new Locale("bn", "BD")).format(new Date());
            FordiModel initial = new FordiModel(UUID.randomUUID().toString(), banglaDate + " বাজার ফর্দ", dateStr, new ArrayList<>(), "#F0FDFA");
            allFordi.add(initial);
            storage.saveFordiRecords(allFordi);
            this.currentActiveFordiId = initial.getId();
            return initial;
        }
        if (this.currentActiveFordiId != null) {
            for (FordiModel f : allFordi) {
                if (f.getId().equals(this.currentActiveFordiId)) {
                    return f;
                }
            }
        }
        FordiModel first = allFordi.get(0);
        this.currentActiveFordiId = first.getId();
        return first;
    }

    private void saveActiveFordi(FordiModel updatedFordi) {
        StorageManager storage = StorageManager.getInstance(this);
        List<FordiModel> allFordi = storage.loadFordiRecords();
        for (int i = 0; i < allFordi.size(); i++) {
            if (allFordi.get(i).getId().equals(updatedFordi.getId())) {
                allFordi.set(i, updatedFordi);
                break;
            }
        }
        storage.saveFordiRecords(allFordi);
        updateFordiKhataUI();
        triggerAutoCloudBackup();
    }

    private void refreshFordiGrandTotals(FordiModel activeFordi) {
        if (activeFordi == null || this.binding == null) return;
        double plannedSum = activeFordi.getPlannedTotal();
        double checkedSum = activeFordi.getCheckedTotal();
        double profitSum = activeFordi.getPotentialProfit();

        this.binding.tvFordiMainDateSubtitle.setText("মোট ফর্দ: ৳ " + PdfExporter.formatBengaliNumber(plannedSum) + " | টিক করা: ৳ " + PdfExporter.formatBengaliNumber(checkedSum));
        this.binding.tvFordiItemSummaryCount.setText("ফর্দের সর্বমোট ব্যয়:");
        this.binding.tvFordiTableGrandTotal.setText("৳ " + PdfExporter.formatBengaliNumber(plannedSum));

        if (profitSum > 0) {
            this.binding.tvFordiTableProfitPreview.setVisibility(View.VISIBLE);
            this.binding.tvFordiTableProfitPreview.setText("সম্ভাব্য মোট লাভ: ৳ " + PdfExporter.formatBengaliNumber(profitSum));
        } else {
            this.binding.tvFordiTableProfitPreview.setVisibility(View.GONE);
        }

        if (activeFordi.isPostedToAccounting()) {
            this.binding.btnFordiPostToAccounting.setText("আজকের হিসাবে যোগ হয়েছে");
            this.binding.btnFordiPostToAccounting.setEnabled(false);
            this.binding.btnFordiPostToAccounting.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#374151")));
            this.binding.btnFordiPostToAccounting.setTextColor(Color.parseColor("#9CA3AF"));
        } else {
            double costToShow = checkedSum > 0 ? checkedSum : plannedSum;
            this.binding.btnFordiPostToAccounting.setText("হিসাবে যোগ করুন (৳ " + PdfExporter.formatBengaliNumber(costToShow) + ")");
            this.binding.btnFordiPostToAccounting.setEnabled(true);
            this.binding.btnFordiPostToAccounting.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCFBF1")));
            this.binding.btnFordiPostToAccounting.setTextColor(Color.parseColor("#065F46"));
        }
    }

    public void updateFordiKhataUI() {
        if (this.binding == null) {
            return;
        }
        StorageManager storage = StorageManager.getInstance(this);
        List<FordiModel> allFordi = storage.loadFordiRecords();
        if (allFordi.isEmpty()) {
            String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            String banglaDate = new SimpleDateFormat("dd MMMM", new Locale("bn", "BD")).format(new Date());
            FordiModel initial = new FordiModel(UUID.randomUUID().toString(), banglaDate + " বাজার ফর্দ", dateStr, new ArrayList<>(), "#F0FDFA");
            allFordi.add(initial);
            storage.saveFordiRecords(allFordi);
        }

        FordiModel activeFordi = null;
        if (this.currentActiveFordiId != null) {
            for (FordiModel f : allFordi) {
                if (f.getId().equals(this.currentActiveFordiId)) {
                    activeFordi = f;
                    break;
                }
            }
        }
        if (activeFordi == null) {
            activeFordi = allFordi.get(0);
            this.currentActiveFordiId = activeFordi.getId();
        }

        double totalAllPlanned = 0.0d;
        for (FordiModel f : allFordi) {
            totalAllPlanned += f.getPlannedTotal();
        }
        this.binding.tvTotalFordiCount.setText(toBengaliDigits(String.valueOf(allFordi.size())) + " টি");
        this.binding.tvTotalFordiBudget.setText("৳ " + PdfExporter.formatBengaliNumber(totalAllPlanned));

        // Subtab badges
        this.binding.btnFordiTabCurrent.setText("বর্তমান ফর্দ (" + toBengaliDigits(String.valueOf(activeFordi.getItems().size())) + ")");
        this.binding.btnFordiTabHistory.setText("আগের সব ফর্দ (" + toBengaliDigits(String.valueOf(Math.max(0, allFordi.size() - 1))) + ")");

        // Update Header of Main Table Card
        this.binding.tvFordiMainTitle.setText("বাজার ফর্দ");
        this.binding.tvFordiMainDateSubtitle.setText(activeFordi.getTitle() + " • " + activeFordi.getDate() + " | মোট: ৳" + PdfExporter.formatBengaliNumber(activeFordi.getPlannedTotal()) + " • কেনা: ৳" + PdfExporter.formatBengaliNumber(activeFordi.getCheckedTotal()));

        // Green highlight banner for checked items
        double plannedSum = activeFordi.getPlannedTotal();
        double checkedSum = activeFordi.getCheckedTotal();
        int checkedCount = activeFordi.getCheckedItemCount();
        if (checkedCount > 0) {
            this.binding.layoutFordiCheckedBanner.setVisibility(View.VISIBLE);
            this.binding.tvFordiCheckedCount.setText("টিক কৃত পণ্য (" + toBengaliDigits(String.valueOf(checkedCount)) + "টি)");
            this.binding.tvFordiCheckedGrandTotal.setText("৳ " + PdfExporter.formatBengaliNumber(checkedSum));
        } else {
            this.binding.layoutFordiCheckedBanner.setVisibility(View.GONE);
        }

        // Populate Table Rows for Active Fordi
        String query = this.binding.etFordiSearch.getText().toString().trim().toLowerCase();
        renderFordiTableRows(activeFordi, query);

        // Action Button state
        if (activeFordi.isPostedToAccounting()) {
            this.binding.btnFordiPostToAccounting.setText("আজকের হিসাবে যোগ হয়েছে");
            this.binding.btnFordiPostToAccounting.setEnabled(false);
            this.binding.btnFordiPostToAccounting.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D1FAE5")));
            this.binding.btnFordiPostToAccounting.setTextColor(Color.parseColor("#065F46"));
        } else {
            double costToShow = checkedSum > 0 ? checkedSum : plannedSum;
            this.binding.btnFordiPostToAccounting.setText("হিসাবে যোগ করুন (৳ " + PdfExporter.formatBengaliNumber(costToShow) + ")");
            this.binding.btnFordiPostToAccounting.setEnabled(true);
        }

        // Render previous fordis list in history tab
        populateSavedFordiList(allFordi, activeFordi.getId());
    }

    private void renderFordiTableRows(FordiModel activeFordi, String query) {
        if (activeFordi == null || this.binding == null) return;

        List<FordiItemModel> displayItems = new ArrayList<>();
        for (FordiItemModel item : activeFordi.getItems()) {
            if (query == null || query.isEmpty() || item.getProductName().toLowerCase().contains(query)) {
                displayItems.add(item);
            }
        }

        boolean isCardless = StorageManager.getInstance(this).isFordiCardlessMode();
        if (this.binding.layoutFordiTableHeader != null) {
            this.binding.layoutFordiTableHeader.setVisibility(isCardless ? View.GONE : View.VISIBLE);
        }
        if (this.binding.layoutCardlessAddItem != null) {
            this.binding.layoutCardlessAddItem.setVisibility(isCardless ? View.VISIBLE : View.GONE);
        }
        if (this.binding.btnFordiToggleMode != null) {
            this.binding.btnFordiToggleMode.setText(isCardless ? "✨ সিম্পল" : "📊 টেবিল");
        }

        this.binding.layoutFordiTableRows.removeAllViews();
        if (displayItems.isEmpty()) {
            this.binding.layoutFordiEmptyState.setVisibility(View.VISIBLE);
            this.binding.layoutFordiTableRows.setVisibility(View.GONE);
        } else {
            this.binding.layoutFordiEmptyState.setVisibility(View.GONE);
            this.binding.layoutFordiTableRows.setVisibility(View.VISIBLE);

            final FordiModel finalActiveFordi = activeFordi;

            TypedValue selectableBg = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, selectableBg, true);

            for (int i = 0; i < displayItems.size(); i++) {
                final FordiItemModel item = displayItems.get(i);

                if (isCardless) {
                    // Modern Clean Card-less Row (Wrapping product name without cutting off, check circle, details, right-aligned price)
                    LinearLayout cardlessRow = new LinearLayout(this);
                    cardlessRow.setOrientation(LinearLayout.HORIZONTAL);
                    cardlessRow.setGravity(Gravity.CENTER_VERTICAL);
                    cardlessRow.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
                    cardlessRow.setClickable(true);
                    cardlessRow.setFocusable(true);
                    if (selectableBg.resourceId != 0) {
                        cardlessRow.setBackgroundResource(selectableBg.resourceId);
                    }

                    // Check Icon
                    ImageView ivCheck = new ImageView(this);
                    LinearLayout.LayoutParams checkLp = new LinearLayout.LayoutParams(dpToPx(26), dpToPx(26));
                    checkLp.setMarginEnd(dpToPx(10));
                    ivCheck.setLayoutParams(checkLp);
                    ivCheck.setImageResource(item.isChecked() ? R.drawable.ic_check_circle_fill : R.drawable.ic_circle_outline);
                    ivCheck.setImageTintList(ColorStateList.valueOf(Color.parseColor(item.isChecked() ? "#059669" : "#94A3B8")));
                    ivCheck.setClickable(true);
                    ivCheck.setFocusable(true);
                    ivCheck.setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
                    cardlessRow.addView(ivCheck);

                    // Middle: Product Name (wraps freely so long names are completely visible) + Subtitle (Qty, Rate)
                    LinearLayout colText = new LinearLayout(this);
                    colText.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                    textLp.setMarginEnd(dpToPx(8));
                    colText.setLayoutParams(textLp);

                    TextView tvName = new TextView(this);
                    tvName.setText(item.getProductName());
                    tvName.setTextSize(15.0f);
                    tvName.setTypeface(null, Typeface.BOLD);
                    tvName.setSingleLine(false);
                    if (item.isChecked()) {
                        tvName.setPaintFlags(tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        tvName.setTextColor(Color.parseColor("#94A3B8"));
                    } else {
                        tvName.setTextColor(Color.parseColor("#0F172A"));
                    }
                    colText.addView(tvName);

                    // Subtitle details (পরিমাণ • দর)
                    StringBuilder subBuilder = new StringBuilder();
                    if (item.getPlannedQuantity() > 0) {
                        String qStr = item.getPlannedQuantity() == (long) item.getPlannedQuantity() ? String.format(Locale.US, "%d", (long) item.getPlannedQuantity()) : String.format(Locale.US, "%.1f", item.getPlannedQuantity());
                        subBuilder.append(PdfExporter.toBengaliDigits(qStr)).append(" ").append(ProductModel.getBengaliUnitLabel(item.getUnit()));
                    }
                    if (item.getPurchaseRate() > 0) {
                        if (subBuilder.length() > 0) subBuilder.append(" • ");
                        subBuilder.append("ক্রয় ৳").append(PdfExporter.formatBengaliNumber(item.getPurchaseRate()));
                    }
                    if (item.getSellingRate() > 0) {
                        if (subBuilder.length() > 0) subBuilder.append(" • ");
                        subBuilder.append("বিক্রয় ৳").append(PdfExporter.formatBengaliNumber(item.getSellingRate()));
                    }

                    if (subBuilder.length() > 0) {
                        TextView tvSub = new TextView(this);
                        tvSub.setText(subBuilder.toString());
                        tvSub.setTextSize(12.0f);
                        tvSub.setTextColor(Color.parseColor("#64748B"));
                        tvSub.setPadding(0, dpToPx(2), 0, 0);
                        colText.addView(tvSub);
                    }
                    cardlessRow.addView(colText);

                    // Right: Total Amount
                    TextView tvPrice = new TextView(this);
                    LinearLayout.LayoutParams priceLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    tvPrice.setLayoutParams(priceLp);
                    tvPrice.setText("৳ " + PdfExporter.formatBengaliNumber(item.getPlannedTotal()));
                    tvPrice.setTextSize(15.5f);
                    tvPrice.setTypeface(null, Typeface.BOLD);
                    tvPrice.setTextColor(Color.parseColor(item.isChecked() ? "#059669" : "#0F172A"));
                    cardlessRow.addView(tvPrice);

                    // Delete Icon
                    ImageView ivDel = new ImageView(this);
                    LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
                    delLp.setMarginStart(dpToPx(8));
                    ivDel.setLayoutParams(delLp);
                    ivDel.setImageResource(R.drawable.ic_trash);
                    ivDel.setImageTintList(ColorStateList.valueOf(Color.parseColor("#CBD5E1")));
                    ivDel.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
                    ivDel.setClickable(true);
                    ivDel.setFocusable(true);
                    ivDel.setOnClickListener(v -> {
                        finalActiveFordi.getItems().remove(item);
                        saveActiveFordi(finalActiveFordi);
                        renderFordiTableRows(finalActiveFordi, binding.etFordiSearch.getText().toString().trim().toLowerCase());
                        Toast.makeText(MainActivity.this, item.getProductName() + " মোছা হয়েছে", Toast.LENGTH_SHORT).show();
                    });
                    cardlessRow.addView(ivDel);

                    // Click listeners
                    View.OnClickListener toggleCheckListener = v -> {
                        item.setChecked(!item.isChecked());
                        saveActiveFordi(finalActiveFordi);
                        renderFordiTableRows(finalActiveFordi, binding.etFordiSearch.getText().toString().trim().toLowerCase());
                    };
                    ivCheck.setOnClickListener(toggleCheckListener);
                    cardlessRow.setOnClickListener(v -> showEditFordiItemDialog(item, finalActiveFordi));

                    this.binding.layoutFordiTableRows.addView(cardlessRow);

                    // Divider
                    if (i < displayItems.size() - 1) {
                        View divider = new View(this);
                        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
                        divParams.setMargins(dpToPx(42), 0, 0, 0);
                        divider.setLayoutParams(divParams);
                        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
                        this.binding.layoutFordiTableRows.addView(divider);
                    }

                } else {
                    // Standard Table Row Mode
                    LinearLayout rowLayout = new LinearLayout(this);
                    rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                    rowLayout.setGravity(Gravity.CENTER_VERTICAL);
                    rowLayout.setWeightSum(5.5f);
                    rowLayout.setMinimumHeight(dpToPx(44));
                    rowLayout.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));

                    // Col 1: Product Name & Unit (Weight 1.3)
                    LinearLayout colProduct = new LinearLayout(this);
                    colProduct.setOrientation(LinearLayout.HORIZONTAL);
                    colProduct.setGravity(Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams colProductParams = new LinearLayout.LayoutParams(0, -2, 1.3f);
                    colProduct.setLayoutParams(colProductParams);

                    CheckBox cbItem = new CheckBox(this);
                    cbItem.setChecked(item.isChecked());
                    cbItem.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#7C3AED")));
                    LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28));
                    cbParams.setMarginEnd(dpToPx(2));
                    cbItem.setLayoutParams(cbParams);

                    final TextView tvName = new TextView(this);
                    LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1.0f);
                    tvName.setLayoutParams(nameParams);
                    String uLabel = "(" + ProductModel.getBengaliUnitLabel(item.getUnit()) + ")";
                    tvName.setText(item.getProductName() + " " + uLabel);
                    tvName.setTextSize(12.0f);
                    tvName.setSingleLine(false);
                    if (item.isChecked()) {
                        tvName.setPaintFlags(tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        tvName.setTextColor(Color.parseColor("#94A3B8"));
                    } else {
                        tvName.setTextColor(Color.parseColor("#0F172A"));
                    }

                    colProduct.addView(cbItem);
                    colProduct.addView(tvName);
                    rowLayout.addView(colProduct);

                    // Col 2: Direct Inline Quantity (Weight 0.85)
                    final EditText etQty = new EditText(this);
                    LinearLayout.LayoutParams qtyParams = new LinearLayout.LayoutParams(0, dpToPx(34), 0.85f);
                    qtyParams.setMarginEnd(dpToPx(3));
                    etQty.setLayoutParams(qtyParams);
                    etQty.setBackgroundResource(R.drawable.bg_fordi_cell_input);
                    etQty.setGravity(Gravity.CENTER);
                    etQty.setTextSize(11.5f);
                    etQty.setSingleLine(true);
                    etQty.setTextColor(Color.parseColor("#0F172A"));
                    etQty.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    etQty.setText(item.getPlannedQuantity() > 0 ? (item.getPlannedQuantity() == (long) item.getPlannedQuantity() ? String.format(Locale.US, "%d", (long) item.getPlannedQuantity()) : String.format(Locale.US, "%.1f", item.getPlannedQuantity())) : "1");
                    rowLayout.addView(etQty);

                    // Col 3: Direct Inline Purchase Rate (Weight 1.0)
                    final EditText etBuyRate = new EditText(this);
                    LinearLayout.LayoutParams buyParams = new LinearLayout.LayoutParams(0, dpToPx(34), 1.0f);
                    buyParams.setMarginEnd(dpToPx(3));
                    etBuyRate.setLayoutParams(buyParams);
                    etBuyRate.setBackgroundResource(R.drawable.bg_fordi_cell_input);
                    etBuyRate.setGravity(Gravity.CENTER);
                    etBuyRate.setTextSize(11.5f);
                    etBuyRate.setSingleLine(true);
                    etBuyRate.setHint("০");
                    etBuyRate.setTextColor(Color.parseColor("#0F172A"));
                    etBuyRate.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    if (item.getPurchaseRate() > 0) {
                        etBuyRate.setText(item.getPurchaseRate() == (long) item.getPurchaseRate() ? String.format(Locale.US, "%d", (long) item.getPurchaseRate()) : String.format(Locale.US, "%.1f", item.getPurchaseRate()));
                    }
                    rowLayout.addView(etBuyRate);

                    // Col 4: Direct Inline Selling Rate (Weight 1.0)
                    final EditText etSellRate = new EditText(this);
                    LinearLayout.LayoutParams sellParams = new LinearLayout.LayoutParams(0, dpToPx(34), 1.0f);
                    sellParams.setMarginEnd(dpToPx(3));
                    etSellRate.setLayoutParams(sellParams);
                    etSellRate.setBackgroundResource(R.drawable.bg_fordi_cell_input);
                    etSellRate.setGravity(Gravity.CENTER);
                    etSellRate.setTextSize(11.5f);
                    etSellRate.setSingleLine(true);
                    etSellRate.setHint("০");
                    etSellRate.setTextColor(Color.parseColor("#0F172A"));
                    etSellRate.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    if (item.getSellingRate() > 0) {
                        etSellRate.setText(item.getSellingRate() == (long) item.getSellingRate() ? String.format(Locale.US, "%d", (long) item.getSellingRate()) : String.format(Locale.US, "%.1f", item.getSellingRate()));
                    }
                    rowLayout.addView(etSellRate);

                    // Col 5: Total (Weight 0.85, End, Bold)
                    final TextView tvTotal = new TextView(this);
                    LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(0, -2, 0.85f);
                    tvTotal.setLayoutParams(totalParams);
                    tvTotal.setGravity(Gravity.END);
                    tvTotal.setTextSize(12.0f);
                    tvTotal.setTypeface(null, Typeface.BOLD);
                    tvTotal.setSingleLine(true);
                    tvTotal.setTextColor(Color.parseColor(item.isChecked() ? "#059669" : "#0F172A"));
                    tvTotal.setText("৳" + PdfExporter.formatBengaliNumber(item.getPlannedTotal()));
                    rowLayout.addView(tvTotal);

                    // Col 6: Edit Action (Weight 0.25)
                    ImageView ivEdit = new ImageView(this);
                    LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(0, dpToPx(24), 0.25f);
                    ivEdit.setLayoutParams(editParams);
                    ivEdit.setImageResource(R.drawable.ic_edit);
                    ivEdit.setImageTintList(ColorStateList.valueOf(Color.parseColor("#6366F1")));
                    ivEdit.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
                    ivEdit.setClickable(true);
                    ivEdit.setFocusable(true);
                    ivEdit.setOnClickListener(v -> showEditFordiItemDialog(item, finalActiveFordi));
                    rowLayout.addView(ivEdit);

                    // Col 7: Delete Button (Weight 0.25)
                    ImageView ivDelete = new ImageView(this);
                    LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(0, dpToPx(24), 0.25f);
                    ivDelete.setLayoutParams(delParams);
                    ivDelete.setImageResource(R.drawable.ic_trash);
                    ivDelete.setImageTintList(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
                    ivDelete.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
                    ivDelete.setClickable(true);
                    ivDelete.setFocusable(true);
                    rowLayout.addView(ivDelete);

                    // Live Listeners for Direct Inline Updates
                    cbItem.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            item.setChecked(isChecked);
                            if (isChecked) {
                                tvName.setPaintFlags(tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                                tvName.setTextColor(Color.parseColor("#94A3B8"));
                                tvTotal.setTextColor(Color.parseColor("#059669"));
                            } else {
                                tvName.setPaintFlags(tvName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                                tvName.setTextColor(Color.parseColor("#0F172A"));
                                tvTotal.setTextColor(Color.parseColor("#0F172A"));
                            }
                            refreshFordiGrandTotals(finalActiveFordi);
                        }
                    });

                    TextWatcher inlineWatcher = new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            double q = 1.0;
                            String qStr = etQty.getText().toString().trim();
                            if (!qStr.isEmpty()) {
                                try { q = Double.parseDouble(qStr); } catch (Exception ignored) {}
                            }
                            if (q <= 0) q = 1.0;

                            double pr = 0.0;
                            String prStr = etBuyRate.getText().toString().trim();
                            if (!prStr.isEmpty()) {
                                try { pr = Double.parseDouble(prStr); } catch (Exception ignored) {}
                            }

                            double sr = 0.0;
                            String srStr = etSellRate.getText().toString().trim();
                            if (!srStr.isEmpty()) {
                                try { sr = Double.parseDouble(srStr); } catch (Exception ignored) {}
                            }

                            item.setPlannedQuantity(q);
                            item.setActualQuantity(q);
                            item.setPurchaseRate(pr);
                            item.setActualPurchaseRate(pr);
                            item.setSellingRate(sr);
                            item.recalculate();

                            tvTotal.setText("৳" + PdfExporter.formatBengaliNumber(item.getPlannedTotal()));
                            refreshFordiGrandTotals(finalActiveFordi);
                        }

                        @Override
                        public void afterTextChanged(Editable s) {}
                    };

                    etQty.addTextChangedListener(inlineWatcher);
                    etBuyRate.addTextChangedListener(inlineWatcher);
                    etSellRate.addTextChangedListener(inlineWatcher);

                    View.OnFocusChangeListener rateFocusListener = new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if (!hasFocus) {
                                double pr = item.getPurchaseRate();
                                double sr = item.getSellingRate();
                                if (pr > 0 || sr > 0) {
                                    StorageManager storage = StorageManager.getInstance(MainActivity.this);
                                    ProductModel prod = storage.findProductByName(item.getProductName());
                                    if (prod != null) {
                                        if (pr > 0) prod.setLastPurchasePrice(pr);
                                        if (sr > 0) prod.setSellingPrice(sr);
                                        storage.saveOrUpdateProduct(prod);
                                    }
                                }
                            }
                        }
                    };
                    etBuyRate.setOnFocusChangeListener(rateFocusListener);
                    etSellRate.setOnFocusChangeListener(rateFocusListener);

                    ivDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finalActiveFordi.getItems().remove(item);
                            saveActiveFordi(finalActiveFordi);
                            renderFordiTableRows(finalActiveFordi, binding.etFordiSearch.getText().toString().trim().toLowerCase());
                            Toast.makeText(MainActivity.this, item.getProductName() + " মোছা হয়েছে", Toast.LENGTH_SHORT).show();
                        }
                    });

                    this.binding.layoutFordiTableRows.addView(rowLayout);

                    // Divider line between rows
                    if (i < displayItems.size() - 1) {
                        View divider = new View(this);
                        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(-1, dpToPx(1));
                        divider.setLayoutParams(divParams);
                        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
                        this.binding.layoutFordiTableRows.addView(divider);
                    }
                }
            }
        }

        // Summary Row Updates
        double plannedSum = activeFordi.getPlannedTotal();
        double checkedSum = activeFordi.getCheckedTotal();
        int totalCount = activeFordi.getItems().size();
        int checkedCount = activeFordi.getCheckedItemCount();
        double profitSum = activeFordi.getPotentialProfit();

        this.binding.tvFordiItemSummaryCount.setText("সব পণ্য (" + toBengaliDigits(String.valueOf(totalCount)) + "টি)");
        this.binding.tvFordiTableGrandTotal.setText("৳ " + PdfExporter.formatBengaliNumber(plannedSum));

        this.binding.tvFordiCheckedCount.setText("কেনা বাজার (" + toBengaliDigits(String.valueOf(checkedCount)) + "টি)");
        this.binding.tvFordiCheckedGrandTotal.setText("৳ " + PdfExporter.formatBengaliNumber(checkedSum));

        if (profitSum > 0) {
            this.binding.tvFordiTableProfitPreview.setVisibility(View.VISIBLE);
            this.binding.tvFordiTableProfitPreview.setText("সম্ভাব্য মোট লাভ: ৳ " + PdfExporter.formatBengaliNumber(profitSum));
        } else {
            this.binding.tvFordiTableProfitPreview.setVisibility(View.GONE);
        }

        // Action Button state
        if (activeFordi.isPostedToAccounting()) {
            this.binding.btnFordiPostToAccounting.setText("আজকের হিসাবে যোগ হয়েছে");
            this.binding.btnFordiPostToAccounting.setEnabled(false);
            this.binding.btnFordiPostToAccounting.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D1FAE5")));
            this.binding.btnFordiPostToAccounting.setTextColor(Color.parseColor("#065F46"));
        } else {
            double costToShow = checkedSum > 0 ? checkedSum : plannedSum;
            this.binding.btnFordiPostToAccounting.setText("হিসাবে যোগ করুন (৳ " + PdfExporter.formatBengaliNumber(costToShow) + ")");
            this.binding.btnFordiPostToAccounting.setEnabled(true);
            this.binding.btnFordiPostToAccounting.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCFBF1")));
            this.binding.btnFordiPostToAccounting.setTextColor(Color.parseColor("#0D9488"));
        }

        // Populate Other Saved Fordis below main card
        List<FordiModel> allFordi = StorageManager.getInstance(this).loadFordiRecords();
        populateSavedFordiList(allFordi, activeFordi.getId());
    }

    private void populateSavedFordiList(List<FordiModel> allFordi, String activeId) {
        this.binding.layoutFordiList.removeAllViews();
        if (allFordi.size() <= 1) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("কোনো পূর্ববর্তী ফর্দ সংরক্ষিত নেই।");
            emptyTv.setTextSize(13.0f);
            emptyTv.setTextColor(Color.parseColor("#94A3B8"));
            emptyTv.setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(24));
            emptyTv.setGravity(Gravity.CENTER);
            this.binding.layoutFordiList.addView(emptyTv);
            return;
        }

        TextView headerOther = new TextView(this);
        headerOther.setText("পূর্ববর্তী সংরক্ষিত ফর্দসমূহ (" + toBengaliDigits(String.valueOf(allFordi.size() - 1)) + "টি)");
        headerOther.setTextSize(13.0f);
        headerOther.setTypeface(null, Typeface.BOLD);
        headerOther.setTextColor(Color.parseColor("#475569"));
        headerOther.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(8));
        this.binding.layoutFordiList.addView(headerOther);

        for (final FordiModel fordi : allFordi) {
            if (fordi.getId().equals(activeId)) continue;

            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setBackgroundResource(R.drawable.bg_dark_card_neutral);
            LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(-1, -2);
            rootParams.setMargins(0, 0, 0, dpToPx(8));
            root.setLayoutParams(rootParams);
            root.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));

            TextView tvTitle = new TextView(this);
            tvTitle.setText(fordi.getTitle());
            tvTitle.setTextSize(14.0f);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextColor(Color.parseColor("#0F172A"));
            info.addView(tvTitle);

            TextView tvSub = new TextView(this);
            tvSub.setText(fordi.getDate() + " • " + toBengaliDigits(String.valueOf(fordi.getItems().size())) + "টি পণ্য • মোট ৳" + PdfExporter.formatBengaliNumber(fordi.getPlannedTotal()) + " (কেনা: ৳" + PdfExporter.formatBengaliNumber(fordi.getCheckedTotal()) + ")");
            tvSub.setTextSize(11.5f);
            tvSub.setTextColor(Color.parseColor("#64748B"));
            info.addView(tvSub);

            info.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.this.currentActiveFordiId = fordi.getId();
                    MainActivity.this.updateFordiKhataUI();
                    binding.layoutFordiCurrentContainer.setVisibility(View.VISIBLE);
                    binding.layoutFordiHistoryContainer.setVisibility(View.GONE);
                    binding.btnFordiTabCurrent.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7C3AED")));
                    binding.btnFordiTabCurrent.setTextColor(Color.WHITE);
                    binding.btnFordiTabHistory.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
                    binding.btnFordiTabHistory.setTextColor(Color.parseColor("#475569"));
                    Toast.makeText(MainActivity.this, "'" + fordi.getTitle() + "' ফর্দ খোলা হয়েছে", Toast.LENGTH_SHORT).show();
                }
            });

            root.addView(info);

            MaterialButton btnSwitch = new MaterialButton(this);
            btnSwitch.setText("খুলুন");
            btnSwitch.setTextSize(11.0f);
            btnSwitch.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7C3AED")));
            btnSwitch.setTextColor(Color.WHITE);
            btnSwitch.setCornerRadius(dpToPx(8));
            btnSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.this.currentActiveFordiId = fordi.getId();
                    MainActivity.this.updateFordiKhataUI();
                    binding.layoutFordiCurrentContainer.setVisibility(View.VISIBLE);
                    binding.layoutFordiHistoryContainer.setVisibility(View.GONE);
                    binding.btnFordiTabCurrent.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7C3AED")));
                    binding.btnFordiTabCurrent.setTextColor(Color.WHITE);
                    binding.btnFordiTabHistory.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1F5F9")));
                    binding.btnFordiTabHistory.setTextColor(Color.parseColor("#475569"));
                    Toast.makeText(MainActivity.this, "'" + fordi.getTitle() + "' ফর্দ খোলা হয়েছে", Toast.LENGTH_SHORT).show();
                }
            });
            root.addView(btnSwitch);

            MaterialButton btnDel = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btnDel.setStrokeWidth(0);
            LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
            delParams.setMargins(dpToPx(4), 0, 0, 0);
            btnDel.setLayoutParams(delParams);
            btnDel.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_trash));
            btnDel.setIconSize(dpToPx(14));
            btnDel.setIconPadding(0);
            btnDel.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
            btnDel.setIconTint(ColorStateList.valueOf(Color.parseColor("#EF4444")));
            btnDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteFordiRecord(fordi);
                }
            });
            root.addView(btnDel);

            this.binding.layoutFordiList.addView(root);
        }
    }

    private void showAddFordiItemDialog(final FordiModel activeFordi) {
        if (activeFordi == null) return;
        final StorageManager storage = StorageManager.getInstance(this);

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_fordi_item);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final AutoCompleteTextView actName = dialog.findViewById(R.id.etAddProductName);
        final Spinner spUnit = dialog.findViewById(R.id.spAddProductUnit);
        final EditText etQty = dialog.findViewById(R.id.etAddProductQty);
        final EditText etBuyRate = dialog.findViewById(R.id.etAddProductBuyRate);
        final EditText etSellRate = dialog.findViewById(R.id.etAddProductSellRate);
        final TextView tvTotalCost = dialog.findViewById(R.id.tvAddTotalCost);
        final TextView tvTotalProfit = dialog.findViewById(R.id.tvAddTotalProfit);
        final View btnClose = dialog.findViewById(R.id.btnAddItemClose);
        final View btnCancel = dialog.findViewById(R.id.btnAddItemCancel);
        final View btnSubmit = dialog.findViewById(R.id.btnAddItemSubmit);

        final String[] unitLabels = {"কেজি", "লিটার", "গ্রাম", "পিস", "প্যাকেট", "বক্স", "ডজন", "বস্তা", "মি.লি."};
        final String[] unitCodes = {"kg", "liter", "gm", "piece", "packet", "box", "dozen", "sack", "ml"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, unitLabels);
        spUnit.setAdapter(unitAdapter);

        // AutoComplete suggestions
        List<ProductModel> productMemoryList = storage.loadProductMemory();
        List<String> suggestionNames = new ArrayList<>();
        for (ProductModel p : productMemoryList) {
            suggestionNames.add(p.getName());
        }
        ArrayAdapter<String> suggestAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestionNames);
        actName.setAdapter(suggestAdapter);

        actName.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            ProductModel matched = storage.findProductByName(selectedName);
            if (matched != null) {
                if (matched.getLastPurchasePrice() > 0) {
                    etBuyRate.setText(matched.getLastPurchasePrice() == (long) matched.getLastPurchasePrice() ? String.format(Locale.US, "%d", (long) matched.getLastPurchasePrice()) : String.format(Locale.US, "%.1f", matched.getLastPurchasePrice()));
                }
                if (matched.getSellingPrice() > 0) {
                    etSellRate.setText(matched.getSellingPrice() == (long) matched.getSellingPrice() ? String.format(Locale.US, "%d", (long) matched.getSellingPrice()) : String.format(Locale.US, "%.1f", matched.getSellingPrice()));
                }
                String u = matched.getUnit();
                for (int i = 0; i < unitCodes.length; i++) {
                    if (unitCodes[i].equalsIgnoreCase(u)) {
                        spUnit.setSelection(i);
                        break;
                    }
                }
            }
        });

        Runnable calcPreview = () -> {
            double q = 1.0;
            String qs = etQty.getText().toString().trim();
            if (!qs.isEmpty()) {
                try { q = Double.parseDouble(qs); } catch (Exception ignored) {}
            }
            if (q <= 0) q = 1.0;

            double pr = 0.0;
            String prs = etBuyRate.getText().toString().trim();
            if (!prs.isEmpty()) {
                try { pr = Double.parseDouble(prs); } catch (Exception ignored) {}
            }

            double sr = 0.0;
            String srs = etSellRate.getText().toString().trim();
            if (!srs.isEmpty()) {
                try { sr = Double.parseDouble(srs); } catch (Exception ignored) {}
            }

            double totalCost = q * pr;
            double profit = (sr > pr && pr > 0) ? (q * (sr - pr)) : 0.0;
            tvTotalCost.setText("৳ " + PdfExporter.formatBengaliNumber(totalCost));
            tvTotalProfit.setText("৳ " + PdfExporter.formatBengaliNumber(profit));
        };

        calcPreview.run();

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calcPreview.run();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etQty.addTextChangedListener(watcher);
        etBuyRate.addTextChangedListener(watcher);
        etSellRate.addTextChangedListener(watcher);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String name = actName.getText().toString().trim();
                if (name.isEmpty()) {
                    actName.setError("পণ্যের নাম লিখুন");
                    actName.requestFocus();
                    return;
                }

                double qty = 1.0;
                String qStr = etQty.getText().toString().trim();
                if (!qStr.isEmpty()) {
                    try { qty = Double.parseDouble(qStr); } catch (Exception ignored) {}
                }
                if (qty <= 0) qty = 1.0;

                double pRate = 0.0;
                String prStr = etBuyRate.getText().toString().trim();
                if (!prStr.isEmpty()) {
                    try { pRate = Double.parseDouble(prStr); } catch (Exception ignored) {}
                }

                double sRate = 0.0;
                String srStr = etSellRate.getText().toString().trim();
                if (!srStr.isEmpty()) {
                    try { sRate = Double.parseDouble(srStr); } catch (Exception ignored) {}
                }

                int unitIdx = spUnit.getSelectedItemPosition();
                String unit = unitIdx >= 0 && unitIdx < unitCodes.length ? unitCodes[unitIdx] : "kg";

                FordiItemModel newItem = new FordiItemModel(UUID.randomUUID().toString(), name, unit, qty, pRate, sRate);
                activeFordi.getItems().add(newItem);

                // Save product in memory for quick reuse
                ProductModel product = storage.findProductByName(name);
                if (product == null) {
                    product = new ProductModel(null, name, unit, pRate, sRate, "বাজার ফর্দ");
                } else {
                    if (pRate > 0) product.setLastPurchasePrice(pRate);
                    if (sRate > 0) product.setSellingPrice(sRate);
                    product.setUnit(unit);
                }
                storage.saveOrUpdateProduct(product);

                saveActiveFordi(activeFordi);
                updateFordiKhataUI();
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "'" + name + "' ফর্দে যোগ হয়েছে", Toast.LENGTH_SHORT).show();
            });
        }

        dialog.show();
    }

    private void showEditFordiItemDialog(final FordiItemModel item, final FordiModel fordi, final Runnable[] saveState, final Runnable[] populateList) {
        showEditFordiItemDialog(item, fordi);
    }

    private void confirmAndPostToAccounting(final FordiModel fordi, final Runnable[] refreshTotals, final Runnable[] populateList) {
        final double actualTotal = fordi.getActualTotal() > 0 ? fordi.getActualTotal() : fordi.getPlannedTotal();
        int count = fordi.getBoughtItemCount() > 0 ? fordi.getBoughtItemCount() : fordi.getItems().size();
        final String selectedDate = this.viewModel != null ? this.viewModel.getActiveDateKey() : new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());

        new MaterialAlertDialogBuilder(this)
                .setTitle("আজকের কেনা হিসাবে যোগ করবেন?")
                .setMessage("ফর্দ: " + fordi.getTitle() + "\n" +
                        "কেনা পণ্য: " + toBengaliDigits(String.valueOf(count)) + " টি\n" +
                        "মোট ক্রয় মূল্য: ৳ " + PdfExporter.formatBengaliNumber(actualTotal) + "\n\n" +
                        "এটি দৈনিক খাতার '" + selectedDate + "' তারিখে 'পণ্য ক্রয় / মাল কেনা' হিসাবে যোগ হবে।")
                .setPositiveButton("হ্যাঁ, যোগ করুন", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AccountingService accountingService = AccountingService.getInstance(MainActivity.this);
                        boolean success = accountingService.postFordiPurchaseToDailyAccounting(fordi, selectedDate);
                        if (success) {
                            if (refreshTotals != null && refreshTotals[0] != null) refreshTotals[0].run();
                            if (populateList != null && populateList[0] != null) populateList[0].run();
                            updateFordiKhataUI();
                            if (MainActivity.this.viewModel != null) {
                                MainActivity.this.viewModel.loadSavedData();
                            }
                            triggerAutoCloudBackup();
                            Toast.makeText(MainActivity.this, "ফর্দের ৳" + PdfExporter.formatBengaliNumber(actualTotal) + " সফলভাবে আজকের ক্রয় হিসাবে যোগ হয়েছে", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "হিসাব যোগ করা সম্ভব হয়নি বা আগেই যোগ হয়েছে!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void deleteFordiRecord(final FordiModel fordi) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("ফর্দ মুছে ফেলতে চান?")
                .setMessage("আপনি কি নিশ্চিতভাবে '" + fordi.getTitle() + "' ফর্দটি মুছে ফেলতে চান? এটি আর ফিরিয়ে আনা যাবে না।")
                .setPositiveButton("হ্যাঁ, মুছুন", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        StorageManager storage = StorageManager.getInstance(MainActivity.this);
                        List<FordiModel> allFordi = storage.loadFordiRecords();
                        int targetIndex = -1;
                        for (int i = 0; i < allFordi.size(); i++) {
                            if (allFordi.get(i).getId().equals(fordi.getId())) {
                                targetIndex = i;
                                break;
                            }
                        }
                        if (targetIndex != -1) {
                            allFordi.remove(targetIndex);
                            storage.saveFordiRecords(allFordi);
                            if (fordi.getId().equals(MainActivity.this.currentActiveFordiId)) {
                                MainActivity.this.currentActiveFordiId = null;
                            }
                            Toast.makeText(MainActivity.this, "ফর্দটি মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show();
                            updateFordiKhataUI();
                            triggerAutoCloudBackup();
                        }
                    }
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void shareFordiList(FordiModel fordi) {
        StringBuilder sb = new StringBuilder();
        sb.append(fordi.getTitle()).append(" - বাজার ফর্দ\n");
        sb.append("তারিখ: ").append(fordi.getDate()).append("\n");
        sb.append("─────────────────────────\n");
        sb.append("পণ্য      পরিমাণ    ক্রয়→বেচা       মোট\n");
        sb.append("─────────────────────────\n");
        double totalPlanned = 0.0d;
        for (FordiItemModel item : fordi.getItems()) {
            totalPlanned += item.getPlannedTotal();
            String uLabel = ProductModel.getBengaliUnitLabel(item.getUnit());
            String qStr = PdfExporter.formatBengaliNumber(item.getPlannedQuantity()) + uLabel;
            String pRate = PdfExporter.formatBengaliNumber(item.getPurchaseRate());
            String sRate = item.getSellingRate() > 0 ? PdfExporter.formatBengaliNumber(item.getSellingRate()) : "—";
            String totStr = PdfExporter.formatBengaliNumber(item.getPlannedTotal());

            sb.append(item.getProductName()).append("  ")
              .append(qStr).append("  ")
              .append(pRate).append("→").append(sRate).append("  ")
              .append(totStr).append("\n");
        }
        sb.append("─────────────────────────\n");
        sb.append("                     মোট ৳ ").append(PdfExporter.formatBengaliNumber(totalPlanned)).append("\n\n");
        sb.append("— মাওয়া (MAWA) স্মার্ট ক্যাশ খাতা");
        String msg = sb.toString();

        ClipboardManager clipboard = (ClipboardManager) getSystemService("clipboard");
        ClipData clip = ClipData.newPlainText("Shopping List", msg);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "ফর্দটি ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, msg);
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, "ফর্দটি শেয়ার করুন"));
    }

    private void exportFordiAsCsv(FordiModel fordi) {
        if (fordi == null) return;
        StringBuilder csv = new StringBuilder();
        csv.append("ক্র.নং,পণ্যের নাম,পরিমাণ,একক,ক্রয় দর (৳),বিক্রি দর (৳),মোট ক্রয় মূল্য (৳),সম্ভাব্য লাভ (৳),অবস্থা\n");
        int index = 1;
        double totalCost = 0.0;
        double totalProfit = 0.0;

        for (FordiItemModel it : fordi.getItems()) {
            double q = it.getPlannedQuantity() > 0 ? it.getPlannedQuantity() : 1.0;
            double pRate = it.getPurchaseRate();
            double sRate = it.getSellingRate();
            double rowCost = q * pRate;
            double rowProfit = (sRate > pRate && pRate > 0) ? (q * (sRate - pRate)) : 0.0;
            totalCost += rowCost;
            totalProfit += rowProfit;

            String status = it.isChecked() ? "কেনা হয়েছে" : "বাকি আছে";
            String uLabel = ProductModel.getBengaliUnitLabel(it.getUnit());

            csv.append(index++).append(",")
               .append("\"").append(it.getProductName().replace("\"", "\"\"")).append("\",")
               .append(q).append(",")
               .append(uLabel).append(",")
               .append(pRate).append(",")
               .append(sRate).append(",")
               .append(rowCost).append(",")
               .append(rowProfit).append(",")
               .append(status).append("\n");
        }
        csv.append("\n,,,মোট,,,")
           .append(totalCost).append(",")
           .append(totalProfit).append(",\n");

        try {
            File cacheDir = new File(getCacheDir(), "exports");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            String safeTitle = fordi.getTitle().replaceAll("[^a-zA-Z0-9\\u0980-\\u09FF]", "_");
            File csvFile = new File(cacheDir, "Fordi_" + safeTitle + "_" + System.currentTimeMillis() + ".csv");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(csvFile);
            fos.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF}); // UTF-8 BOM for Excel Bengali support
            fos.write(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            fos.flush();
            fos.close();

            Uri fileUri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", csvFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_SUBJECT, fordi.getTitle() + " - CSV ফর্দ");
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "CSV ফর্দ ফাইল শেয়ার করুন"));
        } catch (Exception e) {
            // Fallback to text copy
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("CSV Data", csv.toString()));
            Toast.makeText(this, "CSV টেক্সট ক্লিপবোর্ডে কপি করা হয়েছে", Toast.LENGTH_LONG).show();
        }
    }

    private void exportFordiAsImage(FordiModel fordi) {
        if (fordi == null) return;
        List<FordiItemModel> items = fordi.getItems();
        if (items.isEmpty()) {
            Toast.makeText(this, "ফর্দে কোনো পণ্য নেই", Toast.LENGTH_SHORT).show();
            return;
        }

        int width = 1080;
        int rowHeight = 70;
        int headerHeight = 260;
        int footerHeight = 240;
        int height = headerHeight + (items.size() * rowHeight) + footerHeight;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#090714"));
        canvas.drawRect(0, 0, width, height, bgPaint);

        // Header Background Card
        Paint headerCardPaint = new Paint();
        headerCardPaint.setColor(Color.parseColor("#171328"));
        headerCardPaint.setAntiAlias(true);
        RectF headerRect = new RectF(30, 30, width - 30, headerHeight - 20);
        canvas.drawRoundRect(headerRect, 24, 24, headerCardPaint);

        // Header Border
        Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2);
        borderPaint.setColor(Color.parseColor("#382E5C"));
        borderPaint.setAntiAlias(true);
        canvas.drawRoundRect(headerRect, 24, 24, borderPaint);

        // Title Text
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#FAF5FF"));
        titlePaint.setTextSize(42);
        titlePaint.setFakeBoldText(true);
        titlePaint.setAntiAlias(true);
        canvas.drawText("🛒 " + fordi.getTitle(), 60, 95, titlePaint);

        // Subtitle / Date / App info
        Paint subPaint = new Paint();
        subPaint.setColor(Color.parseColor("#A78BFA"));
        subPaint.setTextSize(26);
        subPaint.setAntiAlias(true);
        canvas.drawText("তারিখ: " + fordi.getDate() + "  |  মাওয়া (MAWA) ক্যাশ খাতা", 60, 140, subPaint);

        // Table Header
        int tableTop = headerHeight;
        Paint thBgPaint = new Paint();
        thBgPaint.setColor(Color.parseColor("#231E3D"));
        RectF thRect = new RectF(30, tableTop, width - 30, tableTop + 54);
        canvas.drawRoundRect(thRect, 12, 12, thBgPaint);

        Paint thTextPaint = new Paint();
        thTextPaint.setColor(Color.parseColor("#C4B5FD"));
        thTextPaint.setTextSize(24);
        thTextPaint.setFakeBoldText(true);
        thTextPaint.setAntiAlias(true);

        canvas.drawText("পণ্য", 70, tableTop + 36, thTextPaint);
        canvas.drawText("পরিমাণ", 450, tableTop + 36, thTextPaint);
        canvas.drawText("ক্রয় দর", 640, tableTop + 36, thTextPaint);
        canvas.drawText("বেচা দর", 800, tableTop + 36, thTextPaint);
        canvas.drawText("মোট (৳)", 940, tableTop + 36, thTextPaint);

        // Rows
        int y = tableTop + 58;
        Paint rowBgEven = new Paint();
        rowBgEven.setColor(Color.parseColor("#120E22"));
        Paint rowBgOdd = new Paint();
        rowBgOdd.setColor(Color.parseColor("#0C091A"));

        Paint rowTextPaint = new Paint();
        rowTextPaint.setColor(Color.parseColor("#FFFFFF"));
        rowTextPaint.setTextSize(26);
        rowTextPaint.setAntiAlias(true);

        Paint rowSubTextPaint = new Paint();
        rowSubTextPaint.setColor(Color.parseColor("#94A3B8"));
        rowSubTextPaint.setTextSize(24);
        rowSubTextPaint.setAntiAlias(true);

        Paint rowCostTextPaint = new Paint();
        rowCostTextPaint.setColor(Color.parseColor("#34D399"));
        rowCostTextPaint.setTextSize(26);
        rowCostTextPaint.setFakeBoldText(true);
        rowCostTextPaint.setAntiAlias(true);

        double totalPlannedCost = 0.0;
        double totalPlannedProfit = 0.0;
        int boughtCount = 0;

        for (int i = 0; i < items.size(); i++) {
            FordiItemModel it = items.get(i);
            double q = it.getPlannedQuantity() > 0 ? it.getPlannedQuantity() : 1.0;
            double pr = it.getPurchaseRate();
            double sr = it.getSellingRate();
            double cost = q * pr;
            double profit = (sr > pr && pr > 0) ? (q * (sr - pr)) : 0.0;
            totalPlannedCost += cost;
            totalPlannedProfit += profit;
            if (it.isChecked()) boughtCount++;

            RectF rRect = new RectF(30, y, width - 30, y + rowHeight);
            canvas.drawRect(rRect, (i % 2 == 0) ? rowBgEven : rowBgOdd);

            // Check icon prefix
            String checkMark = it.isChecked() ? "✓ " : "• ";
            canvas.drawText(checkMark + it.getProductName(), 60, y + 44, rowTextPaint);

            String uLabel = ProductModel.getBengaliUnitLabel(it.getUnit());
            String qStr = (q == (long) q ? String.format(Locale.US, "%d", (long) q) : String.format(Locale.US, "%.1f", q)) + " " + uLabel;
            canvas.drawText(qStr, 450, y + 44, rowSubTextPaint);

            canvas.drawText("৳ " + PdfExporter.formatBengaliNumber(pr), 640, y + 44, rowSubTextPaint);
            canvas.drawText(sr > 0 ? "৳ " + PdfExporter.formatBengaliNumber(sr) : "—", 800, y + 44, rowSubTextPaint);
            canvas.drawText("৳ " + PdfExporter.formatBengaliNumber(cost), 940, y + 44, rowCostTextPaint);

            y += rowHeight;
        }

        // Summary Card at bottom
        int summaryTop = y + 20;
        Paint sumBgPaint = new Paint();
        sumBgPaint.setColor(Color.parseColor("#171328"));
        RectF sumRect = new RectF(30, summaryTop, width - 30, height - 30);
        canvas.drawRoundRect(sumRect, 24, 24, sumBgPaint);
        canvas.drawRoundRect(sumRect, 24, 24, borderPaint);

        Paint sumLabelPaint = new Paint();
        sumLabelPaint.setColor(Color.parseColor("#94A3B8"));
        sumLabelPaint.setTextSize(24);
        sumLabelPaint.setAntiAlias(true);

        Paint sumVal1Paint = new Paint();
        sumVal1Paint.setColor(Color.parseColor("#34D399"));
        sumVal1Paint.setTextSize(34);
        sumVal1Paint.setFakeBoldText(true);
        sumVal1Paint.setAntiAlias(true);

        Paint sumVal2Paint = new Paint();
        sumVal2Paint.setColor(Color.parseColor("#38BDF8"));
        sumVal2Paint.setTextSize(34);
        sumVal2Paint.setFakeBoldText(true);
        sumVal2Paint.setAntiAlias(true);

        canvas.drawText("মোট পণ্য: " + items.size() + " টি (কেনা: " + boughtCount + " টি)", 60, summaryTop + 55, sumLabelPaint);
        canvas.drawText("মোট ক্রয় খরচ:", 60, summaryTop + 110, sumLabelPaint);
        canvas.drawText("৳ " + PdfExporter.formatBengaliNumber(totalPlannedCost), 60, summaryTop + 155, sumVal1Paint);

        canvas.drawText("সম্ভাব্য মোট লাভ:", 550, summaryTop + 110, sumLabelPaint);
        canvas.drawText("৳ " + PdfExporter.formatBengaliNumber(totalPlannedProfit), 550, summaryTop + 155, sumVal2Paint);

        // Share the generated image bitmap
        try {
            File cacheDir = new File(getCacheDir(), "images");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File imageFile = new File(cacheDir, "Fordi_" + System.currentTimeMillis() + ".png");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            Uri fileUri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imageFile);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, fordi.getTitle() + " - বাজার ফর্দ (মাওয়া ক্যাশ খাতা)");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "ফর্দ ছবি শেয়ার করুন"));
        } catch (Exception e) {
            Toast.makeText(this, "ছবি তৈরি করতে সমস্যা হয়েছে: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void openExpenseDrawerWithAmount(double amount) {
        openExpenseDrawer();
        if (this.binding != null && this.binding.etDrawerExpenseAmount != null && amount > 0) {
            String formatted = (amount == Math.floor(amount)) ? String.valueOf((long) amount) : String.valueOf(amount);
            this.binding.etDrawerExpenseAmount.setText(formatted);
            this.binding.etDrawerExpenseAmount.setSelection(formatted.length());
        }
    }

    private void showNoteCountingDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_note_counter, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView btnClose = dialogView.findViewById(R.id.btnNoteCounterClose);
        TextView btnReset = dialogView.findViewById(R.id.btnResetNoteCounts);
        TextView tvTotalAmount = dialogView.findViewById(R.id.tvCountedTotalAmount);
        TextView tvExpectedCash = dialogView.findViewById(R.id.tvSystemCashRef);
        TextView tvDiffStatus = dialogView.findViewById(R.id.tvCashDifferenceBadge);
        LinearLayout layoutRows = dialogView.findViewById(R.id.layoutDenominationsContainer);
        MaterialButton btnApply = dialogView.findViewById(R.id.btnApplyToAvailableCash);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        final int[] denominations = {1000, 500, 200, 100, 50, 20, 10, 5, 2, 1};
        final int[] counts = new int[denominations.length];
        final EditText[] countInputs = new EditText[denominations.length];
        final TextView[] subtotalViews = new TextView[denominations.length];

        AccountingService.DailyAccountingSummary summary = this.viewModel.getDailySummary().getValue();
        if (summary == null && this.viewModel.getActiveDateKey() != null) {
            summary = AccountingService.getInstance(this).calculateDailySummary(this.viewModel.getActiveDateKey());
        }
        final double expectedSystemCash = summary != null ? summary.expectedClosingCash : 0.0d;
        if (tvExpectedCash != null) {
            tvExpectedCash.setText("সিস্টেম ক্যাশ: ৳ " + PdfExporter.formatBengaliNumber(expectedSystemCash));
        }

        Runnable updateTotals = () -> {
            double totalSum = 0.0;
            int totalCount = 0;
            for (int i = 0; i < denominations.length; i++) {
                double sub = denominations[i] * counts[i];
                totalSum += sub;
                totalCount += counts[i];
                if (subtotalViews[i] != null) {
                    subtotalViews[i].setText("৳ " + PdfExporter.formatBengaliNumber(sub));
                }
            }

            if (tvTotalAmount != null) {
                tvTotalAmount.setText("৳ " + PdfExporter.formatBengaliNumber(totalSum));
            }

            if (tvDiffStatus != null) {
                double diff = totalSum - expectedSystemCash;
                if (Math.abs(diff) < 0.01) {
                    tvDiffStatus.setText("হিসাব সমান");
                    tvDiffStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DCFCE7")));
                    tvDiffStatus.setTextColor(Color.parseColor("#15803D"));
                } else if (diff > 0) {
                    tvDiffStatus.setText("৳ " + PdfExporter.formatBengaliNumber(diff) + " বাড়তি");
                    tvDiffStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#DBEAFE")));
                    tvDiffStatus.setTextColor(Color.parseColor("#1D4ED8"));
                } else {
                    tvDiffStatus.setText("৳ " + PdfExporter.formatBengaliNumber(Math.abs(diff)) + " ঘাটতি");
                    tvDiffStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FEE2E2")));
                    tvDiffStatus.setTextColor(Color.parseColor("#DC2626"));
                }
            }
        };

        if (layoutRows != null) {
            layoutRows.removeAllViews();
            for (int i = 0; i < denominations.length; i++) {
                final int idx = i;
                final int denom = denominations[i];

                LinearLayout rowLayout = new LinearLayout(this);
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                rowLayout.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(48)
                );
                rowParams.setMargins(0, 0, 0, dpToPx(6));
                rowLayout.setLayoutParams(rowParams);
                rowLayout.setBackgroundResource(R.drawable.shape_capsule_white);
                rowLayout.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F8FAFC")));
                rowLayout.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));

                // Note Badge
                TextView tvBadge = new TextView(this);
                LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dpToPx(85), ViewGroup.LayoutParams.WRAP_CONTENT);
                tvBadge.setLayoutParams(badgeParams);
                tvBadge.setText("৳ " + PdfExporter.toBengaliDigits(String.valueOf(denom)));
                tvBadge.setTextColor(Color.parseColor("#0F172A"));
                tvBadge.setTextSize(13.5f);
                tvBadge.setTypeface(null, Typeface.BOLD);
                rowLayout.addView(tvBadge);

                // Minus Button
                MaterialButton btnMinus = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                LinearLayout.LayoutParams btnMinusParams = new LinearLayout.LayoutParams(dpToPx(34), dpToPx(34));
                btnMinus.setLayoutParams(btnMinusParams);
                btnMinus.setPadding(0, 0, 0, 0);
                btnMinus.setText("−");
                btnMinus.setTextSize(15f);
                btnMinus.setTextColor(Color.parseColor("#475569"));
                btnMinus.setCornerRadius(dpToPx(8));
                btnMinus.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#CBD5E1")));
                btnMinus.setStrokeWidth(dpToPx(1));
                rowLayout.addView(btnMinus);

                // Count input
                EditText etCount = new EditText(this);
                LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(dpToPx(48), ViewGroup.LayoutParams.MATCH_PARENT);
                inputParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
                etCount.setLayoutParams(inputParams);
                etCount.setInputType(InputType.TYPE_CLASS_NUMBER);
                etCount.setGravity(Gravity.CENTER);
                etCount.setText("0");
                etCount.setTextColor(Color.parseColor("#1E293B"));
                etCount.setTextSize(14f);
                etCount.setTypeface(null, Typeface.BOLD);
                etCount.setBackground(null);
                etCount.setSelectAllOnFocus(true);
                countInputs[idx] = etCount;
                rowLayout.addView(etCount);

                // Plus Button
                MaterialButton btnPlus = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                LinearLayout.LayoutParams btnPlusParams = new LinearLayout.LayoutParams(dpToPx(34), dpToPx(34));
                btnPlus.setLayoutParams(btnPlusParams);
                btnPlus.setPadding(0, 0, 0, 0);
                btnPlus.setText("+");
                btnPlus.setTextSize(15f);
                btnPlus.setTextColor(Color.parseColor("#2563EB"));
                btnPlus.setCornerRadius(dpToPx(8));
                btnPlus.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#93C5FD")));
                btnPlus.setStrokeWidth(dpToPx(1));
                rowLayout.addView(btnPlus);

                // Subtotal text
                TextView tvSubtotal = new TextView(this);
                LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                tvSubtotal.setLayoutParams(subParams);
                tvSubtotal.setGravity(Gravity.END);
                tvSubtotal.setText("৳ ০");
                tvSubtotal.setTextColor(Color.parseColor("#0F172A"));
                tvSubtotal.setTextSize(13.5f);
                tvSubtotal.setTypeface(null, Typeface.BOLD);
                subtotalViews[idx] = tvSubtotal;
                rowLayout.addView(tvSubtotal);

                btnMinus.setOnClickListener(v -> {
                    if (counts[idx] > 0) {
                        counts[idx]--;
                        etCount.setText(String.valueOf(counts[idx]));
                        updateTotals.run();
                    }
                });

                btnPlus.setOnClickListener(v -> {
                    counts[idx]++;
                    etCount.setText(String.valueOf(counts[idx]));
                    updateTotals.run();
                });

                etCount.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        String txt = s.toString().trim();
                        int val = 0;
                        if (!txt.isEmpty()) {
                            try {
                                val = Integer.parseInt(txt);
                            } catch (Exception ignored) {}
                        }
                        if (counts[idx] != val) {
                            counts[idx] = val;
                            updateTotals.run();
                        }
                    }
                });

                layoutRows.addView(rowLayout);
            }
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                for (int i = 0; i < denominations.length; i++) {
                    counts[i] = 0;
                    if (countInputs[i] != null) countInputs[i].setText("0");
                }
                updateTotals.run();
            });
        }

        if (btnApply != null) {
            btnApply.setOnClickListener(v -> {
                double totalSum = 0.0;
                for (int i = 0; i < denominations.length; i++) {
                    totalSum += denominations[i] * counts[i];
                }
                if (this.viewModel != null) {
                    this.viewModel.setAvailableCash(totalSum);
                    if (this.binding != null && this.binding.etAvailableCash != null) {
                        String formatted = (totalSum == Math.floor(totalSum)) ? String.valueOf((long) totalSum) : String.valueOf(totalSum);
                        this.binding.etAvailableCash.setText(formatted);
                    }
                }
                Toast.makeText(this, "গোনা টাকা ৳ " + PdfExporter.formatBengaliNumber(totalSum) + " হাতে ক্যাশে যুক্ত হয়েছে", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        updateTotals.run();
        dialog.show();
    }

    private void showSmartCalculatorDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_smart_calculator, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView btnClose = dialogView.findViewById(R.id.btnCalcClose);
        TabLayout tabCalcMode = dialogView.findViewById(R.id.tabCalcMode);
        TextView tvExpression = dialogView.findViewById(R.id.tvCalcExpression);
        TextView tvResult = dialogView.findViewById(R.id.tvCalcResult);
        LinearLayout layoutStandard = dialogView.findViewById(R.id.layoutStandardCalc);
        LinearLayout layoutRateQty = dialogView.findViewById(R.id.layoutRateQtyCalc);

        // Rate x Qty inputs
        EditText etRatePrice = dialogView.findViewById(R.id.etRatePrice);
        EditText etRateQty = dialogView.findViewById(R.id.etRateQty);

        // Unit chips
        TextView chipKg = dialogView.findViewById(R.id.chipUnitKg);
        TextView chipGram = dialogView.findViewById(R.id.chipUnitGram);
        TextView chipLtr = dialogView.findViewById(R.id.chipUnitLtr);
        TextView chipPcs = dialogView.findViewById(R.id.chipUnitPcs);
        TextView chipDozen = dialogView.findViewById(R.id.chipUnitDozen);
        TextView chipSack = dialogView.findViewById(R.id.chipUnitSack);

        // Presets
        TextView preset250g = dialogView.findViewById(R.id.chipPreset250g);
        TextView preset500g = dialogView.findViewById(R.id.chipPreset500g);
        TextView preset1 = dialogView.findViewById(R.id.chipPreset1);
        TextView preset2 = dialogView.findViewById(R.id.chipPreset2);
        TextView preset5 = dialogView.findViewById(R.id.chipPreset5);

        // Posting buttons
        MaterialButton btnCashSale = dialogView.findViewById(R.id.btnPostToCashSale);
        MaterialButton btnCreditSale = dialogView.findViewById(R.id.btnPostToCreditSale);
        MaterialButton btnExpense = dialogView.findViewById(R.id.btnPostToExpense);
        MaterialButton btnCashDrawer = dialogView.findViewById(R.id.btnPostToCashDrawer);
        MaterialButton btnFordi = dialogView.findViewById(R.id.btnPostToFordi);

        final StringBuilder currentExpr = new StringBuilder();
        final double[] currentFinalResult = new double[]{0.0};
        final double[] unitMultiplier = new double[]{1.0};
        final String[] unitName = new String[]{"কেজি"};

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // Mode Switching via TabLayout
        if (tabCalcMode != null) {
            tabCalcMode.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (tab.getPosition() == 1) {
                        if (layoutRateQty != null) layoutRateQty.setVisibility(View.VISIBLE);
                        if (layoutStandard != null) layoutStandard.setVisibility(View.GONE);
                    } else {
                        if (layoutStandard != null) layoutStandard.setVisibility(View.VISIBLE);
                        if (layoutRateQty != null) layoutRateQty.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }

        // Standard Calculator Logic
        Runnable evaluateAndShow = () -> {
            String expr = currentExpr.toString();
            if (expr.isEmpty()) {
                if (tvExpression != null) tvExpression.setText("০");
                if (tvResult != null) tvResult.setText("৳ ০");
                currentFinalResult[0] = 0.0;
                return;
            }
            if (tvExpression != null) tvExpression.setText(expr);
            try {
                double val = evaluateMathExpression(expr);
                currentFinalResult[0] = val;
                if (tvResult != null) {
                    tvResult.setText("৳ " + PdfExporter.formatBengaliNumber(val));
                }
            } catch (Exception e) {
                if (tvResult != null) tvResult.setText("৳ ...");
            }
        };

        int[] buttonIds = {
                R.id.btnCalc0, R.id.btnCalc1, R.id.btnCalc2, R.id.btnCalc3,
                R.id.btnCalc4, R.id.btnCalc5, R.id.btnCalc6, R.id.btnCalc7,
                R.id.btnCalc8, R.id.btnCalc9, R.id.btnCalc00, R.id.btnCalcDot,
                R.id.btnCalcAdd, R.id.btnCalcSub, R.id.btnCalcMul, R.id.btnCalcDiv,
                R.id.btnCalcPercent
        };

        for (int id : buttonIds) {
            View b = dialogView.findViewById(id);
            if (b instanceof MaterialButton) {
                MaterialButton mb = (MaterialButton) b;
                mb.setOnClickListener(v -> {
                    String btnText = mb.getText().toString();
                    currentExpr.append(btnText);
                    evaluateAndShow.run();
                });
            }
        }

        View btnClear = dialogView.findViewById(R.id.btnCalcClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                currentExpr.setLength(0);
                evaluateAndShow.run();
            });
        }

        View btnBackspace = dialogView.findViewById(R.id.btnCalcBackspace);
        if (btnBackspace != null) {
            btnBackspace.setOnClickListener(v -> {
                if (currentExpr.length() > 0) {
                    currentExpr.deleteCharAt(currentExpr.length() - 1);
                    evaluateAndShow.run();
                }
            });
        }

        View btnEqual = dialogView.findViewById(R.id.btnCalcEqual);
        if (btnEqual != null) {
            btnEqual.setOnClickListener(v -> {
                String expr = currentExpr.toString();
                if (!expr.isEmpty()) {
                    try {
                        double val = evaluateMathExpression(expr);
                        currentFinalResult[0] = val;
                        String formatted = (val == Math.floor(val)) ? String.valueOf((long) val) : String.format(java.util.Locale.US, "%.2f", val);
                        currentExpr.setLength(0);
                        currentExpr.append(formatted);
                        if (tvExpression != null) tvExpression.setText(formatted);
                        if (tvResult != null) tvResult.setText("৳ " + PdfExporter.formatBengaliNumber(val));
                    } catch (Exception ignored) {}
                }
            });
        }

        int[] quickPercentIds = new int[]{
                R.id.btnCalcQuickPlus5, R.id.btnCalcQuickPlus10, R.id.btnCalcQuickPlus15,
                R.id.btnCalcQuickMinus5, R.id.btnCalcQuickMinus10
        };
        String[] quickPercentOps = new String[]{"+5%", "+10%", "+15%", "-5%", "-10%"};
        for (int i = 0; i < quickPercentIds.length; i++) {
            View qb = dialogView.findViewById(quickPercentIds[i]);
            if (qb != null) {
                final String op = quickPercentOps[i];
                qb.setOnClickListener(v -> {
                    if (currentExpr.length() == 0) {
                        currentExpr.append("100");
                    }
                    currentExpr.append(op);
                    evaluateAndShow.run();
                });
            }
        }

        // Rate x Qty Logic
        Runnable calculateRateQty = () -> {
            String pStr = etRatePrice != null ? etRatePrice.getText().toString().trim() : "";
            String qStr = etRateQty != null ? etRateQty.getText().toString().trim() : "";
            double price = 0.0;
            double qty = 0.0;
            if (!pStr.isEmpty()) {
                try { price = Double.parseDouble(pStr); } catch (Exception ignored) {}
            }
            if (!qStr.isEmpty()) {
                try { qty = Double.parseDouble(qStr); } catch (Exception ignored) {}
            }
            double total = price * qty * unitMultiplier[0];
            currentFinalResult[0] = total;

            if (tvExpression != null) {
                if (price > 0 && qty > 0) {
                    tvExpression.setText("দর ৳ " + PdfExporter.formatBengaliNumber(price) + " × " + PdfExporter.formatBengaliNumber(qty) + " " + unitName[0]);
                } else {
                    tvExpression.setText("দর × পরিমাণ হিসাব");
                }
            }
            if (tvResult != null) {
                tvResult.setText("৳ " + PdfExporter.formatBengaliNumber(total));
            }
        };

        TextWatcher rateWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { calculateRateQty.run(); }
        };
        if (etRatePrice != null) etRatePrice.addTextChangedListener(rateWatcher);
        if (etRateQty != null) etRateQty.addTextChangedListener(rateWatcher);

        TextView[] allUnits = {chipKg, chipGram, chipLtr, chipPcs, chipDozen, chipSack};
        Runnable resetUnitChips = () -> {
            for (TextView u : allUnits) {
                if (u != null) {
                    u.setBackgroundResource(R.drawable.bg_filter_tab_unselected);
                    u.setTextColor(Color.parseColor("#64748B"));
                }
            }
        };

        if (chipKg != null) {
            chipKg.setOnClickListener(v -> {
                resetUnitChips.run();
                chipKg.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                chipKg.setTextColor(Color.parseColor("#2563EB"));
                unitMultiplier[0] = 1.0;
                unitName[0] = "কেজি";
                calculateRateQty.run();
            });
        }
        if (chipGram != null) {
            chipGram.setOnClickListener(v -> {
                resetUnitChips.run();
                chipGram.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                chipGram.setTextColor(Color.parseColor("#2563EB"));
                unitMultiplier[0] = 0.001;
                unitName[0] = "গ্রাম";
                calculateRateQty.run();
            });
        }
        if (chipLtr != null) {
            chipLtr.setOnClickListener(v -> {
                resetUnitChips.run();
                chipLtr.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                chipLtr.setTextColor(Color.parseColor("#2563EB"));
                unitMultiplier[0] = 1.0;
                unitName[0] = "লিটার";
                calculateRateQty.run();
            });
        }
        if (chipPcs != null) {
            chipPcs.setOnClickListener(v -> {
                resetUnitChips.run();
                chipPcs.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                chipPcs.setTextColor(Color.parseColor("#2563EB"));
                unitMultiplier[0] = 1.0;
                unitName[0] = "পিস";
                calculateRateQty.run();
            });
        }
        if (chipDozen != null) {
            chipDozen.setOnClickListener(v -> {
                resetUnitChips.run();
                chipDozen.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                chipDozen.setTextColor(Color.parseColor("#2563EB"));
                unitMultiplier[0] = 1.0;
                unitName[0] = "ডজন";
                calculateRateQty.run();
            });
        }
        if (chipSack != null) {
            chipSack.setOnClickListener(v -> {
                resetUnitChips.run();
                chipSack.setBackgroundResource(R.drawable.bg_filter_tab_selected);
                chipSack.setTextColor(Color.parseColor("#2563EB"));
                unitMultiplier[0] = 1.0;
                unitName[0] = "বস্তা";
                calculateRateQty.run();
            });
        }

        // Preset Quantity Quick buttons
        if (preset250g != null) {
            preset250g.setOnClickListener(v -> {
                if (chipGram != null) chipGram.performClick();
                if (etRateQty != null) etRateQty.setText("250");
            });
        }
        if (preset500g != null) {
            preset500g.setOnClickListener(v -> {
                if (chipGram != null) chipGram.performClick();
                if (etRateQty != null) etRateQty.setText("500");
            });
        }
        if (preset1 != null) {
            preset1.setOnClickListener(v -> {
                if (etRateQty != null) etRateQty.setText("1");
            });
        }
        if (preset2 != null) {
            preset2.setOnClickListener(v -> {
                if (etRateQty != null) etRateQty.setText("2");
            });
        }
        if (preset5 != null) {
            preset5.setOnClickListener(v -> {
                if (etRateQty != null) etRateQty.setText("5");
            });
        }

        // Direct Posting Actions
        if (btnCashSale != null) {
            btnCashSale.setOnClickListener(v -> {
                double amount = currentFinalResult[0];
                if (amount <= 0) {
                    Toast.makeText(this, "টাকার পরিমাণ হিসাব করুন!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Double curCash = this.viewModel.getAvailableCash().getValue();
                double total = (curCash != null ? curCash : 0.0) + amount;
                this.viewModel.setAvailableCash(total);
                if (this.binding != null && this.binding.etAvailableCash != null) {
                    String formatted = (total == Math.floor(total)) ? String.valueOf((long) total) : String.valueOf(total);
                    this.binding.etAvailableCash.setText(formatted);
                }
                Toast.makeText(this, "নগদ বিক্রি ৳ " + PdfExporter.formatBengaliNumber(amount) + " ক্যাশে যোগ করা হয়েছে", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        if (btnCreditSale != null) {
            btnCreditSale.setOnClickListener(v -> {
                double amount = currentFinalResult[0];
                if (this.binding != null && this.binding.tabLayout != null) {
                    TabLayout.Tab tab = this.binding.tabLayout.getTabAt(2);
                    if (tab != null) tab.select();
                }
                Toast.makeText(this, "বাকি খাতা নির্বাচন করে ৳ " + PdfExporter.formatBengaliNumber(amount) + " যোগ করুন", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            });
        }

        if (btnExpense != null) {
            btnExpense.setOnClickListener(v -> {
                double amount = currentFinalResult[0];
                openExpenseDrawerWithAmount(amount);
                dialog.dismiss();
            });
        }

        if (btnCashDrawer != null) {
            btnCashDrawer.setOnClickListener(v -> {
                double amount = currentFinalResult[0];
                if (amount <= 0) {
                    Toast.makeText(this, "টাকার পরিমাণ হিসাব করুন!", Toast.LENGTH_SHORT).show();
                    return;
                }
                this.viewModel.setAvailableCash(amount);
                if (this.binding != null && this.binding.etAvailableCash != null) {
                    String formatted = (amount == Math.floor(amount)) ? String.valueOf((long) totalSumSafe(amount)) : String.valueOf(amount);
                    this.binding.etAvailableCash.setText(formatted);
                }
                Toast.makeText(this, "ড্রয়ার ক্যাশ ৳ " + PdfExporter.formatBengaliNumber(amount) + " সেট করা হয়েছে", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        }

        if (btnFordi != null) {
            btnFordi.setOnClickListener(v -> {
                if (this.binding != null && this.binding.tabLayout != null) {
                    TabLayout.Tab tab = this.binding.tabLayout.getTabAt(4);
                    if (tab != null) tab.select();
                }
                dialog.dismiss();
            });
        }

        if (tvResult != null) {
            tvResult.setOnClickListener(v -> {
                double amount = currentFinalResult[0];
                if (amount > 0) {
                    String textToCopy = String.valueOf(amount);
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("Calculator Result", textToCopy);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(this, "কপি করা হয়েছে: ৳ " + PdfExporter.formatBengaliNumber(amount), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        evaluateAndShow.run();
        dialog.show();
    }

    private double totalSumSafe(double val) {
        return val;
    }

    private double evaluateMathExpression(String expr) {
        if (expr == null || expr.trim().isEmpty()) return 0.0;
        final String sanitized = expr.replace("×", "*").replace("÷", "/").replace("−", "-").trim();
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < sanitized.length()) ? sanitized.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) {
                        int beforePos = this.pos;
                        double next = parseTerm();
                        if (beforePos < sanitized.length() && sanitized.substring(beforePos, Math.min(this.pos, sanitized.length())).contains("%")) {
                            x += (x * next);
                        } else {
                            x += next;
                        }
                    } else if (eat('-')) {
                        int beforePos = this.pos;
                        double next = parseTerm();
                        if (beforePos < sanitized.length() && sanitized.substring(beforePos, Math.min(this.pos, sanitized.length())).contains("%")) {
                            x -= (x * next);
                        } else {
                            x -= next;
                        }
                    } else {
                        return x;
                    }
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) {
                        double divisor = parseFactor();
                        x = (divisor != 0) ? (x / divisor) : 0;
                    }
                    else if (eat('%')) {
                        x = x / 100.0;
                    }
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    String numStr = sanitized.substring(startPos, this.pos);
                    try {
                        x = Double.parseDouble(numStr);
                    } catch (Exception e) {
                        x = 0;
                    }
                } else {
                    x = 0;
                }

                if (eat('%')) {
                    x = x / 100.0;
                }
                return x;
            }
        }.parse();
    }

    public void showProfitMarginDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_profit_margin_selector, null);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        com.google.android.material.button.MaterialButton btn5 = dialogView.findViewById(R.id.btnMarginPreset5);
        com.google.android.material.button.MaterialButton btn8 = dialogView.findViewById(R.id.btnMarginPreset8);
        com.google.android.material.button.MaterialButton btn10 = dialogView.findViewById(R.id.btnMarginPreset10);
        com.google.android.material.button.MaterialButton btn12 = dialogView.findViewById(R.id.btnMarginPreset12);
        com.google.android.material.button.MaterialButton btn15 = dialogView.findViewById(R.id.btnMarginPreset15);
        com.google.android.material.button.MaterialButton btn20 = dialogView.findViewById(R.id.btnMarginPreset20);
        EditText etCustom = dialogView.findViewById(R.id.etCustomMarginPercent);
        TextView tvFormula = dialogView.findViewById(R.id.tvMarginPreviewFormula);
        TextView tvProfit = dialogView.findViewById(R.id.tvMarginPreviewProfit);
        View btnCancel = dialogView.findViewById(R.id.btnMarginCancel);
        View btnSave = dialogView.findViewById(R.id.btnMarginSave);

        double currentRate = StorageManager.getInstance(this).getEstimatedGrossMarginRate();
        double currentPercentVal = currentRate * 100.0;
        final double[] selectedPercent = new double[]{currentPercentVal > 0 ? currentPercentVal : 10.0};

        String activeKey = (viewModel != null && viewModel.getActiveDateKey() != null) ? viewModel.getActiveDateKey() : new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        AccountingService.DailyAccountingSummary summary = AccountingService.getInstance(this).calculateDailySummary(activeKey);
        double totalSales = summary != null ? summary.totalSales : 0.0;

        com.google.android.material.button.MaterialButton[] presetButtons = new com.google.android.material.button.MaterialButton[]{btn5, btn8, btn10, btn12, btn15, btn20};
        double[] presetValues = new double[]{5.0, 8.0, 10.0, 12.0, 15.0, 20.0};

        Runnable updatePreview = () -> {
            double p = selectedPercent[0];
            double profit = totalSales * (p / 100.0);
            String pStr = (p == (long) p) ? String.format(Locale.US, "%d", (long) p) : String.format(Locale.US, "%.1f", p);
            if (tvFormula != null) {
                tvFormula.setText("বিক্রি ৳ " + PdfExporter.formatBengaliNumber(totalSales) + " × " + PdfExporter.toBengaliDigits(pStr) + "%");
            }
            if (tvProfit != null) {
                tvProfit.setText("৳ " + PdfExporter.formatBengaliNumber(profit));
            }
            for (int i = 0; i < presetButtons.length; i++) {
                if (presetButtons[i] != null) {
                    boolean isMatch = Math.abs(presetValues[i] - p) < 0.01;
                    if (isMatch) {
                        presetButtons[i].setBackgroundColor(0xFF059669);
                        presetButtons[i].setTextColor(0xFFFFFFFF);
                        presetButtons[i].setStrokeWidth(0);
                    } else {
                        presetButtons[i].setBackgroundColor(0x00000000);
                        presetButtons[i].setTextColor(0xFF334155);
                        presetButtons[i].setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFCBD5E1));
                        presetButtons[i].setStrokeWidth(2);
                    }
                }
            }
        };

        for (int i = 0; i < presetButtons.length; i++) {
            final double val = presetValues[i];
            presetButtons[i].setOnClickListener(v -> {
                selectedPercent[0] = val;
                String pStr = (val == (long) val) ? String.valueOf((long) val) : String.valueOf(val);
                etCustom.setText(pStr);
                etCustom.setSelection(pStr.length());
                updatePreview.run();
            });
        }

        String initialStr = (selectedPercent[0] == (long) selectedPercent[0]) ? String.valueOf((long) selectedPercent[0]) : String.valueOf(selectedPercent[0]);
        etCustom.setText(initialStr);
        updatePreview.run();

        etCustom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    String str = s.toString().trim();
                    if (!str.isEmpty()) {
                        double val = Double.parseDouble(str);
                        if (val >= 0 && val <= 100) {
                            selectedPercent[0] = val;
                            updatePreview.run();
                        }
                    }
                } catch (Exception ignored) {}
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            try {
                String str = etCustom.getText().toString().trim();
                double val = str.isEmpty() ? selectedPercent[0] : Double.parseDouble(str);
                if (val > 0 && val <= 100) {
                    double rate = val / 100.0;
                    StorageManager.getInstance(this).saveEstimatedGrossMarginRate(rate);
                    if (viewModel != null) {
                        viewModel.loadSavedData();
                    }
                    updateResultCard(0);
                    updateHeroCard();
                    updateNotebookTextPreview();
                    updateDashboardUI();
                    String displayPercent = (val == (long) val) ? String.valueOf((long) val) : String.valueOf(val);
                    Toast.makeText(this, "শতকরা লাভের হার " + PdfExporter.toBengaliDigits(displayPercent) + "% নির্ধারিত হয়েছে!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "সঠিক শতাংশ লিখুন (১ থেকে ১০০)", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "সঠিক সংখ্যা লিখুন", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCloudBackupUI();
        updateHeaderSyncStatusUI();
        MawaSyncManager.getInstance(this).startRealtimeSync();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    public void onPause() {
        super.onPause();
        MawaSyncManager.getInstance(this).stopRealtimeSync();
        this.backupHandler.removeCallbacks(this.backupRunnable);
        triggerAutoCloudBackup();
    }
}
