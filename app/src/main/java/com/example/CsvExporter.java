package com.example;

import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CsvExporter {

    private static File getExportDirectory(Context context) {
        File outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (outputDir == null) {
            outputDir = context.getFilesDir();
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        return outputDir;
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Export single day cash book and expenses to CSV
     */
    public static File exportDailyCashBookToCsv(Context context, String dateStr, String dayOfWeek,
                                                List<ExpenseModel> expenses, double availableCash,
                                                double sabekCash, double dailySale, double totalSale,
                                                double totalExpenses, double result) {
        String cleanDate = dateStr != null ? dateStr.replace("/", "-").replace(" ", "_") : "daily";
        File file = new File(getExportDirectory(context), "MawaStore_Daily_Cash_" + cleanDate + ".csv");

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(osw)) {

            // UTF-8 BOM so Microsoft Excel renders Bengali correctly
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            writer.println("মাওয়া স্টোর - দৈনিক ক্যাশ হিসাব বিবরণী");
            writer.println("তারিখ," + escapeCsv(dateStr) + ",বার," + escapeCsv(dayOfWeek));
            writer.println("রিপোর্ট প্রস্তুতের সময়," + escapeCsv(new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(new Date())));
            writer.println();

            // Summary Section
            writer.println("--- আর্থিক সারসংক্ষেপ ---");
            writer.println("বিবরণ,পরিমাণ (টাকা)");
            writer.println("হাতে ক্যাশ (বর্তমান ব্যালেন্স)," + availableCash);
            writer.println("আজকের মোট খরচ," + totalExpenses);
            writer.println("মোট ক্যাশ যোগফল (হাতে ক্যাশ + খরচ)," + totalSale);
            writer.println("সাবেক ক্যাশ (পূর্বের ব্যালেন্স)," + sabekCash);
            writer.println("আজকের নিট বিক্রি (বিক্রি = মোট ক্যাশ - সাবেক)," + dailySale);
            writer.println("আনুমানিক লাভ," + result);
            writer.println();

            // Expenses Breakdown Table
            writer.println("--- আজকের খরচের বিবরণী ---");
            writer.println("ক্রমিক নং,বিবরণ / পণ্যের নাম,খরচের খাত,পরিমাণ (টাকা),সময়");
            if (expenses != null && !expenses.isEmpty()) {
                int sl = 1;
                for (ExpenseModel exp : expenses) {
                    String category = "HOME".equalsIgnoreCase(exp.getExpenseType()) ? "বাড়ি" : "দোকান";
                    writer.println(sl++ + "," +
                            escapeCsv(exp.getName()) + "," +
                            escapeCsv(category) + "," +
                            exp.getAmount() + "," +
                            escapeCsv(exp.getTime() != null ? exp.getTime() : ""));
                }
            } else {
                writer.println("1,কোন খরচ লিপিবদ্ধ নেই,-,0,-");
            }

            writer.flush();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Export Weekly / Monthly / Multi-day consolidated statement to CSV
     */
    public static File exportPeriodCashBookToCsv(Context context, String periodTitle,
                                                 List<MainViewModel.DaySummary> summaries,
                                                 double totalPeriodSale, double totalPeriodExpense,
                                                 double totalPeriodProfit) {
        String cleanTitle = periodTitle.replace(" ", "_").replace("/", "-");
        File file = new File(getExportDirectory(context), "MawaStore_Report_" + cleanTitle + "_" + System.currentTimeMillis() + ".csv");

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(osw)) {

            // UTF-8 BOM
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            writer.println("মাওয়া স্টোর - " + escapeCsv(periodTitle) + " আর্থিক খতিয়ান");
            writer.println("প্রস্তুতির তারিখ," + escapeCsv(new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(new Date())));
            writer.println();

            writer.println("--- সামগ্রিক সারসংক্ষেপ ---");
            writer.println("মোট দিন সংখ্যা," + (summaries != null ? summaries.size() : 0));
            writer.println("মোট বিক্রি," + totalPeriodSale);
            writer.println("মোট খরচ," + totalPeriodExpense);
            writer.println("মোট আনুমানিক লাভ," + totalPeriodProfit);
            writer.println();

            writer.println("--- দৈনিক হিসাব বিবরণী তালিকা ---");
            writer.println("তারিখ,হাতে ক্যাশ (৳),খরচ (৳),সাবেক ক্যাশ (৳),নিট বিক্রি (৳),আনুমানিক লাভ (৳)");
            if (summaries != null && !summaries.isEmpty()) {
                for (MainViewModel.DaySummary ds : summaries) {
                    writer.println(escapeCsv(ds.dateKey) + "," +
                            ds.availableCash + "," +
                            ds.expenses + "," +
                            ds.sabek + "," +
                            ds.computedSale + "," +
                            ds.estimatedProfit);
                }
            } else {
                writer.println("কোন রেকর্ড পাওয়া যায়নি,0,0,0,0,0");
            }

            writer.flush();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Export Baki Khata Directory to CSV
     */
    public static File exportBakiDirectoryToCsv(Context context, List<BakiModel> bakiList, double totalDue) {
        File file = new File(getExportDirectory(context), "MawaStore_Baki_Khata_" + System.currentTimeMillis() + ".csv");

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(osw)) {

            // UTF-8 BOM
            fos.write(0xEF);
            fos.write(0xBB);
            fos.write(0xBF);

            writer.println("মাওয়া স্টোর - বাকির খাতা ও খরিদ্দার তালিকা");
            writer.println("তারিখ," + escapeCsv(new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(new Date())));
            writer.println("মোট গ্রাহক সংখ্যা," + (bakiList != null ? bakiList.size() : 0));
            writer.println("সর্বমোট বকেয়া পাওনা (৳)," + totalDue);
            writer.println();

            writer.println("ক্রমিক নং,খরিদ্দারের নাম,মোবাইল নম্বর,ঠিকানা/নোট,সর্বশেষ লেনদেনের তারিখ,মেয়াদ,বকেয়া টাকার পরিমাণ (৳)");
            if (bakiList != null && !bakiList.isEmpty()) {
                int sl = 1;
                for (BakiModel b : bakiList) {
                    writer.println(sl++ + "," +
                            escapeCsv(b.getCustomerName()) + "," +
                            escapeCsv(b.getPhone() != null ? b.getPhone() : "") + "," +
                            escapeCsv(b.getDetails() != null ? b.getDetails() : "") + "," +
                            escapeCsv(b.getDate() != null ? b.getDate() : "") + "," +
                            escapeCsv(b.getDueDate() != null ? b.getDueDate() : "") + "," +
                            b.getAmount());
                }
            } else {
                writer.println("1,কোন গ্রাহক পাওয়া যায়নি,-,-,-,-,0");
            }

            writer.flush();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
