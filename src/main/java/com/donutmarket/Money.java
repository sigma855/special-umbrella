package com.donutmarket;

import java.util.Locale;

public final class Money {
    private Money() {}

    /** Parses 1500, 1.5k, 2m, 3b ... Throws NumberFormatException if invalid. */
    public static long parse(String in) {
        String s = in.trim().toLowerCase(Locale.ROOT).replace(",", "").replace("$", "");
        double mult = 1;
        if (s.endsWith("k")) mult = 1e3;
        else if (s.endsWith("m")) mult = 1e6;
        else if (s.endsWith("b")) mult = 1e9;
        if (mult != 1) s = s.substring(0, s.length() - 1);
        double v = Double.parseDouble(s) * mult;
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 1 || v > 1e15) throw new NumberFormatException();
        return Math.round(v);
    }

    public static String fmt(long v) {
        if (v >= 1_000_000_000L) return "$" + trim(v / 1e9) + "B";
        if (v >= 1_000_000L) return "$" + trim(v / 1e6) + "M";
        if (v >= 10_000L) return "$" + trim(v / 1e3) + "K";
        return "$" + v;
    }

    private static String trim(double d) {
        String s = String.format(Locale.US, "%.2f", d);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    public static String duration(long ms) {
        long m = Math.max(0, ms / 60000);
        long h = m / 60;
        return h > 0 ? h + "h " + (m % 60) + "m" : m + "m";
    }
}
