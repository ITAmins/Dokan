package com.example;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LineGraphView extends View {

    public interface OnDaySelectedListener {
        void onDaySelected(MainViewModel.DaySummary summary, int index);
    }

    public static final int MODE_ALL = 0;
    public static final int MODE_SALES = 1;
    public static final int MODE_PURCHASE = 2;

    private int displayMode = MODE_ALL;
    private List<MainViewModel.DaySummary> historyData;
    private float[] cachedXCoords;
    private int selectedIndex = -1;
    private OnDaySelectedListener onDaySelectedListener;

    // Paints
    private final Paint paintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGuideLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPoint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Line paints
    private final Paint paintSalesLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPurchaseLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintExpenseLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintProfitLine = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Fill paints
    private final Paint paintSalesFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPurchaseFill = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LineGraphView(Context context) {
        super(context);
        init();
    }

    public LineGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LineGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.historyData = new ArrayList<>();

        // Background & borders
        paintBg.setColor(Color.parseColor("#F8FAFC"));
        paintBg.setStyle(Paint.Style.FILL);

        paintBorder.setColor(Color.parseColor("#E2E8F0"));
        paintBorder.setStrokeWidth(dpToPx(1.0f));
        paintBorder.setStyle(Paint.Style.STROKE);

        // Grid & guideline
        paintGrid.setColor(Color.parseColor("#E2E8F0"));
        paintGrid.setStrokeWidth(dpToPx(1.0f));
        paintGrid.setStyle(Paint.Style.STROKE);
        paintGrid.setPathEffect(new DashPathEffect(new float[]{dpToPx(4.0f), dpToPx(4.0f)}, 0.0f));

        paintGuideLine.setColor(Color.parseColor("#94A3B8"));
        paintGuideLine.setStrokeWidth(dpToPx(1.5f));
        paintGuideLine.setStyle(Paint.Style.STROKE);
        paintGuideLine.setPathEffect(new DashPathEffect(new float[]{dpToPx(4.0f), dpToPx(3.0f)}, 0.0f));

        // Lines
        // 1. Sales (Green)
        paintSalesLine.setColor(Color.parseColor("#10B981"));
        paintSalesLine.setStrokeWidth(dpToPx(2.8f));
        paintSalesLine.setStyle(Paint.Style.STROKE);
        paintSalesLine.setStrokeCap(Paint.Cap.ROUND);
        paintSalesLine.setStrokeJoin(Paint.Join.ROUND);

        // 2. Purchase (Blue)
        paintPurchaseLine.setColor(Color.parseColor("#3B82F6"));
        paintPurchaseLine.setStrokeWidth(dpToPx(2.8f));
        paintPurchaseLine.setStyle(Paint.Style.STROKE);
        paintPurchaseLine.setStrokeCap(Paint.Cap.ROUND);
        paintPurchaseLine.setStrokeJoin(Paint.Join.ROUND);

        // 3. Expense (Red)
        paintExpenseLine.setColor(Color.parseColor("#EF4444"));
        paintExpenseLine.setStrokeWidth(dpToPx(2.2f));
        paintExpenseLine.setStyle(Paint.Style.STROKE);
        paintExpenseLine.setStrokeCap(Paint.Cap.ROUND);
        paintExpenseLine.setStrokeJoin(Paint.Join.ROUND);

        // 4. Profit (Purple)
        paintProfitLine.setColor(Color.parseColor("#8B5CF6"));
        paintProfitLine.setStrokeWidth(dpToPx(2.2f));
        paintProfitLine.setStyle(Paint.Style.STROKE);
        paintProfitLine.setStrokeCap(Paint.Cap.ROUND);
        paintProfitLine.setStrokeJoin(Paint.Join.ROUND);

        // Text & Point
        paintText.setFakeBoldText(true);
        paintPoint.setStyle(Paint.Style.FILL);
    }

    public void setOnDaySelectedListener(OnDaySelectedListener listener) {
        this.onDaySelectedListener = listener;
    }

    public void setDisplayMode(int mode) {
        this.displayMode = mode;
        invalidate();
    }

    public int getDisplayMode() {
        return displayMode;
    }

    public void setData(List<MainViewModel.DaySummary> summaries) {
        this.historyData = new ArrayList<>();
        if (summaries != null && !summaries.isEmpty()) {
            // Sort chronologically if needed, up to last 31 days or period items
            for (MainViewModel.DaySummary ds : summaries) {
                this.historyData.add(ds);
            }
        }
        if (!this.historyData.isEmpty()) {
            this.selectedIndex = this.historyData.size() - 1; // Default select latest
            if (onDaySelectedListener != null) {
                onDaySelectedListener.onDaySelected(this.historyData.get(selectedIndex), selectedIndex);
            }
        } else {
            this.selectedIndex = -1;
        }
        invalidate();
    }

    public MainViewModel.DaySummary getSelectedDaySummary() {
        if (selectedIndex >= 0 && selectedIndex < historyData.size()) {
            return historyData.get(selectedIndex);
        }
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.historyData.isEmpty() || this.cachedXCoords == null || this.cachedXCoords.length == 0) {
            return super.onTouchEvent(event);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            float tx = event.getX();
            int bestIndex = 0;
            float minDistance = Float.MAX_VALUE;
            for (int i = 0; i < this.cachedXCoords.length; i++) {
                float dist = Math.abs(this.cachedXCoords[i] - tx);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestIndex = i;
                }
            }
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (this.selectedIndex != bestIndex) {
                this.selectedIndex = bestIndex;
                if (onDaySelectedListener != null && selectedIndex >= 0 && selectedIndex < historyData.size()) {
                    onDaySelectedListener.onDaySelected(historyData.get(selectedIndex), selectedIndex);
                }
                invalidate();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        // Draw Card Background
        RectF roundRect = new RectF(2f, 2f, width - 2f, height - 2f);
        canvas.drawRoundRect(roundRect, dpToPx(14.0f), dpToPx(14.0f), paintBg);
        canvas.drawRoundRect(roundRect, dpToPx(14.0f), dpToPx(14.0f), paintBorder);

        if (historyData == null || historyData.isEmpty()) {
            paintText.setTextAlign(Paint.Align.CENTER);
            paintText.setTextSize(dpToPx(13.0f));
            paintText.setColor(Color.parseColor("#94A3B8"));
            canvas.drawText("কোনো হিসাবের ডাটা পাওয়া যায়নি", width / 2.0f, height / 2.0f, paintText);
            return;
        }

        int paddingLeft = dpToPx(24.0f);
        int paddingRight = dpToPx(24.0f);
        int paddingTop = dpToPx(22.0f);
        int paddingBottom = dpToPx(34.0f);

        int chartWidth = width - paddingLeft - paddingRight;
        int chartHeight = height - paddingTop - paddingBottom;
        if (chartWidth <= 0 || chartHeight <= 0) return;

        // Calculate max value across series
        double maxVal = 1000.0d;
        for (MainViewModel.DaySummary sum : historyData) {
            double sale = sum.computedSale;
            double pur = sum.purchases > 0 ? sum.purchases : sum.expenses * 0.7d;
            double exp = sum.expenses;
            double prof = Math.abs(sum.computedSale - sum.expenses);

            if (sale > maxVal) maxVal = sale;
            if (pur > maxVal) maxVal = pur;
            if (exp > maxVal) maxVal = exp;
            if (prof > maxVal) maxVal = prof;
        }
        double maxY = maxVal * 1.25d;

        // Draw horizontal grid lines
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft + chartWidth, paddingTop, paintGrid);
        canvas.drawLine(paddingLeft, paddingTop + (chartHeight / 2.0f), paddingLeft + chartWidth, paddingTop + (chartHeight / 2.0f), paintGrid);
        canvas.drawLine(paddingLeft, paddingTop + chartHeight, paddingLeft + chartWidth, paddingTop + chartHeight, paintGrid);

        int pointsCount = historyData.size();
        float stepX = pointsCount > 1 ? (float) chartWidth / (pointsCount - 1) : chartWidth / 2.0f;

        float[] xCoords = new float[pointsCount];
        float[] ySales = new float[pointsCount];
        float[] yPurchases = new float[pointsCount];
        float[] yExpenses = new float[pointsCount];
        float[] yProfit = new float[pointsCount];

        for (int i = 0; i < pointsCount; i++) {
            MainViewModel.DaySummary ds = historyData.get(i);
            xCoords[i] = (pointsCount > 1) ? (paddingLeft + (i * stepX)) : (paddingLeft + (chartWidth / 2.0f));

            double s = ds.computedSale;
            double p = ds.purchases > 0 ? ds.purchases : (ds.expenses > 0 ? ds.expenses * 0.75d : 0);
            double e = ds.expenses;
            double pr = Math.max(0, s - e);

            ySales[i] = (float) (paddingTop + chartHeight - (chartHeight * (s / maxY)));
            yPurchases[i] = (float) (paddingTop + chartHeight - (chartHeight * (p / maxY)));
            yExpenses[i] = (float) (paddingTop + chartHeight - (chartHeight * (e / maxY)));
            yProfit[i] = (float) (paddingTop + chartHeight - (chartHeight * (pr / maxY)));
        }
        this.cachedXCoords = xCoords;

        // Build Paths with Smooth Bezier Curves
        Path salesPath = buildSmoothPath(xCoords, ySales);
        Path purchasePath = buildSmoothPath(xCoords, yPurchases);
        Path expensePath = buildSmoothPath(xCoords, yExpenses);
        Path profitPath = buildSmoothPath(xCoords, yProfit);

        // Fills
        if (displayMode == MODE_ALL || displayMode == MODE_SALES) {
            Path salesFill = new Path(salesPath);
            salesFill.lineTo(xCoords[pointsCount - 1], paddingTop + chartHeight);
            salesFill.lineTo(xCoords[0], paddingTop + chartHeight);
            salesFill.close();
            paintSalesFill.setShader(new LinearGradient(0, paddingTop, 0, paddingTop + chartHeight,
                    Color.parseColor("#2510B981"), Color.parseColor("#0010B981"), Shader.TileMode.CLAMP));
            canvas.drawPath(salesFill, paintSalesFill);
        }

        if (displayMode == MODE_ALL || displayMode == MODE_PURCHASE) {
            Path purchaseFill = new Path(purchasePath);
            purchaseFill.lineTo(xCoords[pointsCount - 1], paddingTop + chartHeight);
            purchaseFill.lineTo(xCoords[0], paddingTop + chartHeight);
            purchaseFill.close();
            paintPurchaseFill.setShader(new LinearGradient(0, paddingTop, 0, paddingTop + chartHeight,
                    Color.parseColor("#203B82F6"), Color.parseColor("#003B82F6"), Shader.TileMode.CLAMP));
            canvas.drawPath(purchaseFill, paintPurchaseFill);
        }

        // Draw Lines according to Display Mode
        if (displayMode == MODE_ALL) {
            canvas.drawPath(salesPath, paintSalesLine);
            canvas.drawPath(purchasePath, paintPurchaseLine);
            canvas.drawPath(expensePath, paintExpenseLine);
            canvas.drawPath(profitPath, paintProfitLine);
        } else if (displayMode == MODE_SALES) {
            canvas.drawPath(salesPath, paintSalesLine);
        } else if (displayMode == MODE_PURCHASE) {
            canvas.drawPath(purchasePath, paintPurchaseLine);
        }

        // Draw Vertical Guide Line and Dot Indicators if a point is selected
        if (selectedIndex >= 0 && selectedIndex < pointsCount) {
            float selX = xCoords[selectedIndex];
            canvas.drawLine(selX, paddingTop - dpToPx(4.0f), selX, paddingTop + chartHeight, paintGuideLine);

            if (displayMode == MODE_ALL || displayMode == MODE_SALES) {
                drawDot(canvas, selX, ySales[selectedIndex], Color.parseColor("#10B981"));
            }
            if (displayMode == MODE_ALL || displayMode == MODE_PURCHASE) {
                drawDot(canvas, selX, yPurchases[selectedIndex], Color.parseColor("#3B82F6"));
            }
            if (displayMode == MODE_ALL) {
                drawDot(canvas, selX, yExpenses[selectedIndex], Color.parseColor("#EF4444"));
                drawDot(canvas, selX, yProfit[selectedIndex], Color.parseColor("#8B5CF6"));
            }
        }

        // Draw Date Labels on X-axis (Spread evenly to avoid crowding)
        paintText.setTextAlign(Paint.Align.CENTER);
        paintText.setTextSize(dpToPx(10.0f));
        paintText.setColor(Color.parseColor("#64748B"));

        int labelInterval = 1;
        if (pointsCount > 15) labelInterval = 5;
        else if (pointsCount > 8) labelInterval = 3;
        else if (pointsCount > 5) labelInterval = 2;

        for (int i = 0; i < pointsCount; i++) {
            if (i % labelInterval == 0 || i == pointsCount - 1 || i == selectedIndex) {
                String dateLabel = formatDateKey(historyData.get(i).dateKey);
                canvas.drawText(dateLabel, xCoords[i], paddingTop + chartHeight + dpToPx(18.0f), paintText);
            }
        }
    }

    private void drawDot(Canvas canvas, float x, float y, int color) {
        // Outer halo
        paintPoint.setColor(color);
        paintPoint.setAlpha(50);
        canvas.drawCircle(x, y, dpToPx(7.0f), paintPoint);

        // Solid inner dot
        paintPoint.setAlpha(255);
        canvas.drawCircle(x, y, dpToPx(4.0f), paintPoint);

        // White center highlight
        paintPoint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, dpToPx(1.8f), paintPoint);
    }

    private Path buildSmoothPath(float[] x, float[] y) {
        Path path = new Path();
        int n = x.length;
        if (n == 0) return path;
        if (n == 1) {
            path.moveTo(x[0], y[0]);
            path.lineTo(x[0] + 1, y[0]);
            return path;
        }

        path.moveTo(x[0], y[0]);
        for (int i = 0; i < n - 1; i++) {
            float x1 = x[i];
            float y1 = y[i];
            float x2 = x[i + 1];
            float y2 = y[i + 1];

            float cx1 = x1 + (x2 - x1) / 2.0f;
            float cy1 = y1;
            float cx2 = x1 + (x2 - x1) / 2.0f;
            float cy2 = y2;

            path.cubicTo(cx1, cy1, cx2, cy2, x2, y2);
        }
        return path;
    }

    private String formatDateKey(String dateKey) {
        if (dateKey == null || dateKey.isEmpty()) return "";
        try {
            SimpleDateFormat inSdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            Date date = inSdf.parse(dateKey);
            if (date != null) {
                SimpleDateFormat outSdf = new SimpleDateFormat("dd MMM", Locale.US);
                return outSdf.format(date);
            }
        } catch (Exception ignored) {}
        if (dateKey.length() >= 5) {
            return dateKey.substring(0, 5);
        }
        return dateKey;
    }

    private int dpToPx(float dp) {
        return (int) (getResources().getDisplayMetrics().density * dp);
    }
}
