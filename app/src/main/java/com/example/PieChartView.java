package com.example;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PieChartView extends View {

    public interface OnSliceClickListener {
        void onSliceClicked(PieSlice slice, int index);
    }

    public static class PieSlice {
        public String name;
        public double value;
        public int color;
        public float startAngle;
        public float sweepAngle;
        public double percentage;
        public int count;

        public PieSlice(String name, double value, int color) {
            this.name = name;
            this.value = value;
            this.color = color;
        }
    }

    private final List<PieSlice> slices = new ArrayList<>();
    private final RectF rectF = new RectF();
    private int selectedSliceIndex = -1;
    private double grandTotal = 0.0d;
    private OnSliceClickListener onSliceClickListener;

    private final Paint paintArc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintCenter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSubText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGlow = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int[] PALETTE = new int[]{
            Color.parseColor("#F59E0B"), // Amber/Orange
            Color.parseColor("#3B82F6"), // Blue
            Color.parseColor("#10B981"), // Emerald
            Color.parseColor("#8B5CF6"), // Purple
            Color.parseColor("#EC4899"), // Pink
            Color.parseColor("#06B6D4"), // Cyan
            Color.parseColor("#F97316"), // Orange
            Color.parseColor("#6366F1"), // Indigo
            Color.parseColor("#14B8A6"), // Teal
            Color.parseColor("#64748B")  // Slate
    };

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintCenter.setColor(Color.WHITE);
        paintCenter.setStyle(Paint.Style.FILL);

        paintText.setFakeBoldText(true);
        paintText.setTextAlign(Paint.Align.CENTER);

        paintSubText.setTextAlign(Paint.Align.CENTER);
        paintSubText.setColor(Color.parseColor("#64748B"));

        paintGlow.setStyle(Paint.Style.STROKE);
        paintGlow.setStrokeWidth(dpToPx(4.0f));
    }

    public void setOnSliceClickListener(OnSliceClickListener listener) {
        this.onSliceClickListener = listener;
    }

    public List<PieSlice> getSlices() {
        return slices;
    }

    public void setCompanyPurchases(List<CompanyPurchaseSummary> summaries) {
        this.slices.clear();
        this.selectedSliceIndex = -1;
        this.grandTotal = 0.0d;

        if (summaries != null) {
            for (CompanyPurchaseSummary s : summaries) {
                this.grandTotal += s.getTotalAmount();
            }
        }

        if (this.grandTotal > 0.0d && summaries != null) {
            float currentAngle = -90.0f;
            for (int i = 0; i < summaries.size(); i++) {
                CompanyPurchaseSummary cs = summaries.get(i);
                float sweep = (float) ((cs.getTotalAmount() / grandTotal) * 360.0d);
                if (sweep < 0.5f) sweep = 0.5f;

                PieSlice slice = new PieSlice(cs.getName(), cs.getTotalAmount(), cs.getColor());
                slice.startAngle = currentAngle;
                slice.sweepAngle = sweep;
                slice.percentage = cs.getSharePercentage();
                slice.count = cs.getVoucherCount();
                this.slices.add(slice);

                currentAngle += sweep;
            }
        }
        invalidate();
    }

    public void setExpenses(List<ExpenseModel> expenses) {
        this.slices.clear();
        this.selectedSliceIndex = -1;
        this.grandTotal = 0.0d;

        if (expenses == null || expenses.isEmpty()) {
            invalidate();
            return;
        }

        Map<String, Double> totals = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();

        for (ExpenseModel exp : expenses) {
            String name = exp.getName() != null ? exp.getName().trim() : "অন্যান্য";
            if (name.isEmpty()) name = "অন্যান্য";
            double amt = exp.getAmount();

            totals.put(name, totals.getOrDefault(name, 0.0d) + amt);
            counts.put(name, counts.getOrDefault(name, 0) + 1);
            grandTotal += amt;
        }

        if (grandTotal > 0.0d) {
            // Sort by amount descending
            List<Map.Entry<String, Double>> list = new ArrayList<>(totals.entrySet());
            Collections.sort(list, (a, b) -> Double.compare(b.getValue(), a.getValue()));

            float currentAngle = -90.0f;
            int colorIdx = 0;
            for (Map.Entry<String, Double> entry : list) {
                float sweep = (float) ((entry.getValue() / grandTotal) * 360.0d);
                int color = PALETTE[colorIdx % PALETTE.length];
                colorIdx++;

                PieSlice slice = new PieSlice(entry.getKey(), entry.getValue(), color);
                slice.startAngle = currentAngle;
                slice.sweepAngle = sweep;
                slice.percentage = (entry.getValue() / grandTotal) * 100.0d;
                slice.count = counts.getOrDefault(entry.getKey(), 1);
                this.slices.add(slice);

                currentAngle += sweep;
            }
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.slices.isEmpty()) {
            return super.onTouchEvent(event);
        }

        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_DOWN) {
            float tx = event.getX();
            float ty = event.getY();
            float centerX = getWidth() / 2.0f;
            float centerY = getHeight() / 2.0f;

            double dist = Math.sqrt(Math.pow(tx - centerX, 2) + Math.pow(ty - centerY, 2));
            int size = Math.min(getWidth(), getHeight());
            int radius = (size / 2) - dpToPx(16f);
            int innerRadius = (int) (radius * 0.58f);

            if (dist <= radius && dist >= innerRadius * 0.5f) {
                double touchAngle = Math.toDegrees(Math.atan2(ty - centerY, tx - centerX));
                if (touchAngle < 0) touchAngle += 360.0;

                for (int i = 0; i < slices.size(); i++) {
                    PieSlice s = slices.get(i);
                    float start = s.startAngle;
                    while (start < 0) start += 360f;
                    start = start % 360f;
                    float end = (start + s.sweepAngle);

                    boolean inside = false;
                    if (end <= 360f) {
                        inside = touchAngle >= start && touchAngle <= end;
                    } else {
                        inside = touchAngle >= start || touchAngle <= (end % 360f);
                    }

                    if (inside) {
                        this.selectedSliceIndex = i;
                        invalidate();
                        if (event.getAction() == MotionEvent.ACTION_UP && onSliceClickListener != null) {
                            onSliceClickListener.onSliceClicked(s, i);
                        }
                        return true;
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float centerX = width / 2.0f;
        float centerY = height / 2.0f;
        int size = Math.min(width, height);
        int radius = (size / 2) - dpToPx(16f);
        int innerRadius = (int) (radius * 0.58f);

        if (slices.isEmpty() || grandTotal <= 0) {
            // Draw empty placeholder ring
            paintArc.setStyle(Paint.Style.STROKE);
            paintArc.setStrokeWidth(dpToPx(24f));
            paintArc.setColor(Color.parseColor("#E2E8F0"));
            canvas.drawCircle(centerX, centerY, (radius + innerRadius) / 2.0f, paintArc);

            paintText.setColor(Color.parseColor("#94A3B8"));
            paintText.setTextSize(dpToPx(13.0f));
            canvas.drawText("ক্রয়ের তথ্য নেই", centerX, centerY + dpToPx(4.0f), paintText);
            return;
        }

        // Draw Slices
        for (int i = 0; i < slices.size(); i++) {
            PieSlice slice = slices.get(i);
            boolean isSelected = (i == selectedSliceIndex);

            float currentRadius = isSelected ? radius + dpToPx(6.0f) : radius;
            rectF.set(centerX - currentRadius, centerY - currentRadius, centerX + currentRadius, centerY + currentRadius);

            paintArc.setStyle(Paint.Style.FILL);
            paintArc.setColor(slice.color);
            canvas.drawArc(rectF, slice.startAngle, slice.sweepAngle, true, paintArc);

            if (isSelected) {
                paintGlow.setColor(Color.WHITE);
                canvas.drawArc(rectF, slice.startAngle, slice.sweepAngle, true, paintGlow);
            }
        }

        // Draw Center Circle (Donut Hole)
        canvas.drawCircle(centerX, centerY, innerRadius, paintCenter);

        // Center Summary Text
        if (selectedSliceIndex >= 0 && selectedSliceIndex < slices.size()) {
            PieSlice sel = slices.get(selectedSliceIndex);
            paintSubText.setTextSize(dpToPx(10.0f));
            paintSubText.setColor(Color.parseColor("#64748B"));
            String selName = sel.name.length() > 8 ? sel.name.substring(0, 7) + ".." : sel.name;
            canvas.drawText(selName, centerX, centerY - dpToPx(10.0f), paintSubText);

            paintText.setColor(sel.color);
            paintText.setTextSize(dpToPx(13.5f));
            canvas.drawText(formatCompact(sel.value), centerX, centerY + dpToPx(6.0f), paintText);

            paintSubText.setTextSize(dpToPx(9.5f));
            paintSubText.setColor(Color.parseColor("#0F172A"));
            canvas.drawText(String.format(java.util.Locale.US, "%.1f%% শেয়ার", sel.percentage), centerX, centerY + dpToPx(18.0f), paintSubText);
        } else {
            paintSubText.setTextSize(dpToPx(10.0f));
            paintSubText.setColor(Color.parseColor("#64748B"));
            canvas.drawText("মোট ক্রয়", centerX, centerY - dpToPx(8.0f), paintSubText);

            paintText.setColor(Color.parseColor("#0F172A"));
            paintText.setTextSize(dpToPx(14.0f));
            canvas.drawText("৳" + formatCompact(grandTotal), centerX, centerY + dpToPx(8.0f), paintText);

            paintSubText.setTextSize(dpToPx(9.0f));
            paintSubText.setColor(Color.parseColor("#94A3B8"));
            canvas.drawText(slices.size() + "টি খাত", centerX, centerY + dpToPx(20.0f), paintSubText);
        }
    }

    private String formatCompact(double val) {
        if (Math.abs(val) < 1000.0d) {
            return String.format(java.util.Locale.US, "%.0f", val);
        }
        if (Math.abs(val) < 100000.0d) {
            return String.format(java.util.Locale.US, "%.1fk", val / 1000.0d);
        }
        return String.format(java.util.Locale.US, "%.1fL", val / 100000.0d);
    }

    private int dpToPx(float dp) {
        return (int) (getResources().getDisplayMetrics().density * dp);
    }
}
