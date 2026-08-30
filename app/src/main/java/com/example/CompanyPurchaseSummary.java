package com.example;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CompanyPurchaseSummary implements Serializable {
    public static class PurchaseVoucher implements Serializable {
        private String id;
        private String dateFormatted;
        private String timeFormatted;
        private double amount;
        private String purchaseType;
        private String dealerTag;
        private String note;

        public PurchaseVoucher(String id, String dateFormatted, String timeFormatted, double amount, String purchaseType, String dealerTag, String note) {
            this.id = id;
            this.dateFormatted = dateFormatted;
            this.timeFormatted = timeFormatted;
            this.amount = amount;
            this.purchaseType = purchaseType;
            this.dealerTag = dealerTag;
            this.note = note;
        }

        public String getId() {
            return id;
        }

        public String getDateFormatted() {
            return dateFormatted != null ? dateFormatted : "";
        }

        public String getTimeFormatted() {
            return timeFormatted != null ? timeFormatted : "";
        }

        public double getAmount() {
            return amount;
        }

        public String getPurchaseType() {
            return purchaseType != null ? purchaseType : "সরাসরি ক্রয়";
        }

        public String getDealerTag() {
            return dealerTag != null ? dealerTag : "সরাসরি ডিলার";
        }

        public String getNote() {
            return note != null ? note : "";
        }
    }

    private String name;
    private double totalAmount;
    private double sharePercentage;
    private int color;
    private int voucherCount;
    private List<PurchaseVoucher> vouchers;

    public CompanyPurchaseSummary(String name, double totalAmount, double sharePercentage, int color) {
        this.name = name;
        this.totalAmount = totalAmount;
        this.sharePercentage = sharePercentage;
        this.color = color;
        this.vouchers = new ArrayList<>();
        this.voucherCount = 0;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getSharePercentage() {
        return sharePercentage;
    }

    public void setSharePercentage(double sharePercentage) {
        this.sharePercentage = sharePercentage;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getVoucherCount() {
        return vouchers != null ? vouchers.size() : voucherCount;
    }

    public void setVoucherCount(int voucherCount) {
        this.voucherCount = voucherCount;
    }

    public List<PurchaseVoucher> getVouchers() {
        if (vouchers == null) {
            vouchers = new ArrayList<>();
        }
        return vouchers;
    }

    public void addVoucher(PurchaseVoucher voucher) {
        if (this.vouchers == null) {
            this.vouchers = new ArrayList<>();
        }
        this.vouchers.add(voucher);
        this.voucherCount = this.vouchers.size();
    }
}
