package com.tatkalx.utility;

/**
 * Terminal formatting utilities for banners, section headers, and aligned separators.
 */
public final class ConsoleFormatter {

    private ConsoleFormatter() {
    }

    public static void printBanner(String title) {
        System.out.println("================================================================================");
        System.out.println("  " + title);
        System.out.println("================================================================================");
    }

    public static void printSubHeader(String title) {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("  " + title);
        System.out.println("--------------------------------------------------------------------------------");
    }

    public static void printDivider() {
        System.out.println("--------------------------------------------------------------------------------");
    }
}
