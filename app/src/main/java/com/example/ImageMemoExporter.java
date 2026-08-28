package com.example;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImageMemoExporter {

    private static File getExportDirectory(Context context) {
        File outputDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (outputDir == null) {
            outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        }
        if (outputDir == null) {
            outputDir = context.getFilesDir();
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        return outputDir;
    }

    /**
     * Generate Daily Cash Memo Voucher as a high-quality JPG image
     */
    public static File exportDailyCashMemoToJpg(Context context, List<ExpenseModel> expenses,
                                                double totalExpenses, double dailySale,
                                                double availableCash, double totalSale,
                                                double sabekCash, double result,
                                                String dateStr, String dayOfWeek) {
        int width = 800;
        int expCount = expenses != null ? expenses.size() : 0;
        int height = Math.max(1050, 650 + (expCount * 36) + 260);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Background
        paint.setColor(Color.parseColor("#F8FAFC"));
        canvas.drawRect(0, 0, width, height, paint);

        // Voucher Card Background
        int margin = 24;
        RectF cardRect = new RectF(margin, margin, width - margin, height - margin);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(cardRect, 20, 20, paint);

        // Card Border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.parseColor("#CBD5E1"));
        canvas.drawRoundRect(cardRect, 20, 20, paint);
        paint.setStyle(Paint.Style.FILL);

        // Top Header Banner
        paint.setColor(Color.parseColor("#1E3A8A")); // Royal Navy Blue
        RectF headerRect = new RectF(margin, margin, width - margin, margin + 110);
        canvas.drawRoundRect(headerRect, 20, 20, paint);
        // Cover bottom corners of header to make them square
        canvas.drawRect(margin, margin + 80, width - margin, margin + 110, paint);

        // Header Title
        paint.setColor(Color.WHITE);
        paint.setTextSize(28);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("মাওয়া স্টোর", width / 2f, margin + 46, paint);

        paint.setTextSize(14);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setColor(Color.parseColor("#93C5FD"));
        canvas.drawText("ডিজিটাল ক্যাশ খাতা ও হিসাব ভাউচার", width / 2f, margin + 74, paint);

        paint.setTextSize(12);
        paint.setColor(Color.parseColor("#E2E8F0"));
        String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        String memoSubtitle = "তারিখ: " + (dateStr != null ? dateStr : "") + " (" + (dayOfWeek != null ? dayOfWeek : "") + ") • সময়: " + PdfExporter.toBengaliDigits(timeStr);
        canvas.drawText(memoSubtitle, width / 2f, margin + 96, paint);

        paint.setTextAlign(Paint.Align.LEFT);

        // Quick Badges below header
        int y = margin + 145;
        paint.setColor(Color.parseColor("#F1F5F9"));
        RectF netSaleBadge = new RectF(margin + 20, y - 24, width - margin - 20, y + 44);
        canvas.drawRoundRect(netSaleBadge, 12, 12, paint);

        paint.setColor(Color.parseColor("#15803D"));
        paint.setTextSize(13);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("আজকের নিট বিক্রি (বেচা):", margin + 36, y + 16, paint);

        paint.setTextSize(24);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("৳ " + PdfExporter.formatBengaliNumber(dailySale), width - margin - 36, y + 20, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        // Section: Expense Breakdown
        y += 80;
        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("আজকের খরচের তালিকা:", margin + 20, y, paint);

        y += 24;
        // Table Header
        paint.setColor(Color.parseColor("#F8FAFC"));
        canvas.drawRect(margin + 20, y - 16, width - margin - 20, y + 14, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("নং", margin + 30, y, paint);
        canvas.drawText("বিবরণ ও খাত", margin + 70, y, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("টাকা (৳)", width - margin - 30, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStrokeWidth(1);
        canvas.drawLine(margin + 20, y + 14, width - margin - 20, y + 14, paint);

        y += 32;
        paint.setColor(Color.parseColor("#334155"));
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(13);

        if (expenses != null && !expenses.isEmpty()) {
            int sl = 1;
            for (ExpenseModel exp : expenses) {
                String sNum = PdfExporter.toBengaliDigits(String.valueOf(sl++));
                String name = exp.getName() != null ? exp.getName() : "";
                String typeBadge = "HOME".equalsIgnoreCase(exp.getExpenseType()) ? " [বাড়ি]" : " [দোকান]";
                String amount = "৳ " + PdfExporter.formatBengaliNumber(exp.getAmount());

                canvas.drawText(sNum, margin + 30, y, paint);
                canvas.drawText(name + typeBadge, margin + 70, y, paint);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(amount, width - margin - 30, y, paint);
                paint.setTextAlign(Paint.Align.LEFT);

                paint.setColor(Color.parseColor("#F1F5F9"));
                canvas.drawLine(margin + 20, y + 10, width - margin - 20, y + 10, paint);
                paint.setColor(Color.parseColor("#334155"));

                y += 30;
            }
        } else {
            paint.setColor(Color.parseColor("#94A3B8"));
            canvas.drawText("কোন খরচ যোগ করা হয়নি", margin + 70, y, paint);
            y += 30;
        }

        // Summary Card at bottom
        y = Math.max(y + 20, height - margin - 230);
        RectF sumRect = new RectF(margin + 20, y, width - margin - 20, y + 160);
        paint.setColor(Color.parseColor("#F8FAFC"));
        canvas.drawRoundRect(sumRect, 14, 14, paint);
        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(sumRect, 14, 14, paint);
        paint.setStyle(Paint.Style.FILL);

        int sy = y + 26;
        drawMemoRow(canvas, paint, "মোট খরচের পরিমাণ:", "৳ " + PdfExporter.formatBengaliNumber(totalExpenses), margin + 36, width - margin - 36, sy, false, Color.parseColor("#0F172A"));
        sy += 24;
        drawMemoRow(canvas, paint, "হাতে ক্যাশ (বর্তমান ব্যালেন্স):", "৳ " + PdfExporter.formatBengaliNumber(availableCash), margin + 36, width - margin - 36, sy, false, Color.parseColor("#0F172A"));
        sy += 24;
        drawMemoRow(canvas, paint, "মোট ক্যাশ যোগফল (ক্যাশ + খরচ):", "৳ " + PdfExporter.formatBengaliNumber(totalSale), margin + 36, width - margin - 36, sy, false, Color.parseColor("#0F172A"));
        sy += 24;
        drawMemoRow(canvas, paint, "সাবেক ক্যাশ বাদ:", "- ৳ " + PdfExporter.formatBengaliNumber(sabekCash), margin + 36, width - margin - 36, sy, false, Color.parseColor("#DC2626"));
        sy += 28;

        paint.setColor(Color.parseColor("#CBD5E1"));
        canvas.drawLine(margin + 36, sy - 14, width - margin - 36, sy - 14, paint);

        drawMemoRow(canvas, paint, "দৈনিক নিট বিক্রি:", "৳ " + PdfExporter.formatBengaliNumber(dailySale), margin + 36, width - margin - 36, sy, true, Color.parseColor("#15803D"));

        // Footer note
        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(11);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("মাওয়া স্টোর ডিজিটাল অ্যাপ দ্বারা তৈরিকৃত মেমো ভাউচার", width / 2f, height - margin - 12, paint);

        // Save Bitmap to File
        String cleanDate = dateStr != null ? dateStr.replace("/", "-").replace(" ", "_") : "memo";
        File outputFile = new File(getExportDirectory(context), "MawaStore_Memo_" + cleanDate + ".jpg");

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
            return outputFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate Weekly / Monthly Consolidated Statement as a JPG image
     */
    public static File exportPeriodStatementToJpg(Context context, String periodTitle,
                                                  double totalSale, double totalExpense,
                                                  double totalProfit,
                                                  List<MainViewModel.DaySummary> summaries) {
        int width = 850;
        int rowCount = summaries != null ? summaries.size() : 0;
        int height = Math.max(900, 480 + (rowCount * 34) + 120);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Background
        paint.setColor(Color.parseColor("#F1F5F9"));
        canvas.drawRect(0, 0, width, height, paint);

        int margin = 24;
        RectF cardRect = new RectF(margin, margin, width - margin, height - margin);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(cardRect, 20, 20, paint);

        // Card Border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.parseColor("#CBD5E1"));
        canvas.drawRoundRect(cardRect, 20, 20, paint);
        paint.setStyle(Paint.Style.FILL);

        // Header Banner
        paint.setColor(Color.parseColor("#0F172A"));
        RectF headerRect = new RectF(margin, margin, width - margin, margin + 110);
        canvas.drawRoundRect(headerRect, 20, 20, paint);
        canvas.drawRect(margin, margin + 80, width - margin, margin + 110, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(26);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("মাওয়া স্টোর - " + periodTitle, width / 2f, margin + 46, paint);

        paint.setTextSize(13);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setColor(Color.parseColor("#94A3B8"));
        String exportTime = "রিপোর্ট তৈরির সময়: " + PdfExporter.toBengaliDigits(new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(new Date()));
        canvas.drawText(exportTime, width / 2f, margin + 78, paint);

        // 3-Metric Summary Cards Grid
        int y = margin + 140;
        int cardW = (width - (margin * 2) - 40 - 20) / 3;

        // Card 1: Total Sale
        drawMetricBox(canvas, paint, margin + 20, y, cardW, 80, "মোট বিক্রি", "৳ " + PdfExporter.formatBengaliNumber(totalSale), "#EFF6FF", "#1E40AF");
        // Card 2: Total Expense
        drawMetricBox(canvas, paint, margin + 20 + cardW + 10, y, cardW, 80, "মোট খরচ", "৳ " + PdfExporter.formatBengaliNumber(totalExpense), "#FEF2F2", "#B91C1C");
        // Card 3: Estimated Profit
        drawMetricBox(canvas, paint, margin + 20 + (cardW * 2) + 20, y, cardW, 80, "আনুমানিক লাভ", "৳ " + PdfExporter.formatBengaliNumber(totalProfit), "#ECFDF5", "#047857");

        // Table Title
        y += 110;
        paint.setColor(Color.parseColor("#1E293B"));
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("দৈনিক হিসাবের বিস্তারিত তালিকা:", margin + 20, y, paint);

        // Table Header
        y += 24;
        paint.setColor(Color.parseColor("#F8FAFC"));
        canvas.drawRect(margin + 20, y - 16, width - margin - 20, y + 16, paint);

        paint.setColor(Color.parseColor("#475569"));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("তারিখ", margin + 30, y, paint);
        canvas.drawText("হাতে ক্যাশ (৳)", margin + 170, y, paint);
        canvas.drawText("খরচ (৳)", margin + 330, y, paint);
        canvas.drawText("সাবেক (৳)", margin + 470, y, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("নিট বিক্রি (৳)", width - margin - 150, y, paint);
        canvas.drawText("লাভ (৳)", width - margin - 30, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStrokeWidth(1);
        canvas.drawLine(margin + 20, y + 16, width - margin - 20, y + 16, paint);

        y += 34;
        paint.setTextSize(12);
        paint.setTypeface(Typeface.DEFAULT);

        if (summaries != null && !summaries.isEmpty()) {
            for (MainViewModel.DaySummary ds : summaries) {
                paint.setColor(Color.parseColor("#334155"));
                canvas.drawText(ds.dateKey != null ? ds.dateKey : "-", margin + 30, y, paint);
                canvas.drawText(PdfExporter.formatBengaliNumber(ds.availableCash), margin + 170, y, paint);
                canvas.drawText(PdfExporter.formatBengaliNumber(ds.expenses), margin + 330, y, paint);
                canvas.drawText(PdfExporter.formatBengaliNumber(ds.sabek), margin + 470, y, paint);

                paint.setColor(Color.parseColor("#15803D"));
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(PdfExporter.formatBengaliNumber(ds.computedSale), width - margin - 150, y, paint);

                paint.setColor(Color.parseColor("#0284C7"));
                canvas.drawText(PdfExporter.formatBengaliNumber(ds.estimatedProfit), width - margin - 30, y, paint);
                paint.setTextAlign(Paint.Align.LEFT);

                paint.setColor(Color.parseColor("#F1F5F9"));
                paint.setTypeface(Typeface.DEFAULT);
                canvas.drawLine(margin + 20, y + 10, width - margin - 20, y + 10, paint);

                y += 30;
            }
        } else {
            paint.setColor(Color.parseColor("#94A3B8"));
            canvas.drawText("কোন তথ্য পাওয়া যায়নি", margin + 30, y, paint);
            y += 30;
        }

        // Footer
        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(11);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("মাওয়া স্টোর ডিজিটাল অ্যাপ • সর্বস্বত্ব সংরক্ষিত", width / 2f, height - margin - 12, paint);

        String cleanTitle = periodTitle.replace(" ", "_").replace("/", "-");
        File outputFile = new File(getExportDirectory(context), "MawaStore_Statement_" + cleanTitle + "_" + System.currentTimeMillis() + ".jpg");

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
            return outputFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void drawMetricBox(Canvas canvas, Paint paint, int x, int y, int w, int h, String title, String value, String bgHex, String textHex) {
        RectF rect = new RectF(x, y, x + w, y + h);
        paint.setColor(Color.parseColor(bgHex));
        canvas.drawRoundRect(rect, 10, 10, paint);

        paint.setColor(Color.parseColor(textHex));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(title, x + (w / 2f), y + 26, paint);

        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(value, x + (w / 2f), y + 56, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawMemoRow(Canvas canvas, Paint paint, String label, String value, int x1, int x2, int y, boolean isBold, int color) {
        paint.setColor(color);
        paint.setTextSize(isBold ? 14 : 12.5f);
        paint.setTypeface(isBold ? Typeface.create(Typeface.DEFAULT, Typeface.BOLD) : Typeface.DEFAULT);
        canvas.drawText(label, x1, y, paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(value, x2, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }
}
