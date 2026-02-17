package com.utagent.terminal;

import java.io.PrintStream;
import java.util.Arrays;

public class ProgressBar {

    private static final char[] SPINNER_CHARS = {'⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'};
    private static final char[] SIMPLE_SPINNER = {'|', '/', '-', '\\'};
    private static final String BLOCK_COMPLETE = "█";
    private static final String BLOCK_INCOMPLETE = "░";
    private static final String BLOCK_PARTIAL = "▓";
    
    private final PrintStream out;
    private final int width;
    private final boolean useUnicode;
    private int spinnerIndex = 0;
    private boolean colorEnabled;
    
    private String currentPhase = "";
    private int totalItems = 0;
    private int completedItems = 0;
    
    public ProgressBar() {
        this(System.out, 40, true);
    }
    
    public ProgressBar(PrintStream out, int width, boolean useUnicode) {
        this.out = out;
        this.width = width;
        this.useUnicode = useUnicode && supportsUnicode();
        this.colorEnabled = AnsiColor.isColorSupported();
    }
    
    public void setColorEnabled(boolean enabled) {
        this.colorEnabled = enabled;
    }
    
    public void setPhase(String phase) {
        this.currentPhase = phase;
    }
    
    public void setTotalItems(int total) {
        this.totalItems = total;
    }
    
    public void setCompletedItems(int completed) {
        this.completedItems = completed;
    }
    
    public void incrementCompletedItems() {
        this.completedItems++;
    }
    
    public void update(int current, int total, String message) {
        double percentage = total > 0 ? (double) current / total : 0.0;
        update(percentage, message);
    }
    
    public void update(double percentage, String message) {
        StringBuilder line = new StringBuilder();
        
        String spinner = getSpinner();
        line.append(spinner).append(" ");
        
        String percentText = String.format("%5.1f%%", percentage * 100);
        if (colorEnabled) {
            if (percentage >= 1.0) {
                percentText = AnsiColor.green(percentText);
            } else if (percentage >= 0.5) {
                percentText = AnsiColor.cyan(percentText);
            }
        }
        line.append(percentText);
        line.append(" ");
        
        String bar = buildBar(percentage);
        line.append(bar);
        line.append(" ");
        
        if (currentPhase != null && !currentPhase.isEmpty()) {
            String phaseText = "[" + currentPhase + "] ";
            if (colorEnabled) {
                phaseText = AnsiColor.colorize(phaseText, AnsiColor.CYAN);
            }
            line.append(phaseText);
        }
        
        if (message != null && !message.isEmpty()) {
            String truncatedMessage = truncateMessage(message, 30);
            if (colorEnabled) {
                truncatedMessage = AnsiColor.dim(truncatedMessage);
            }
            line.append(truncatedMessage);
        }
        
        if (totalItems > 0) {
            line.append(" ").append(completedItems).append("/").append(totalItems);
        }
        
        line.append("\r");
        out.print(line);
    }
    
    public void updateWithPhase(double percentage, String phase, String message) {
        this.currentPhase = phase;
        update(percentage, message);
    }
    
    public void updateWithStats(double percentage, String message, int completed, int total) {
        this.completedItems = completed;
        this.totalItems = total;
        update(percentage, message);
    }
    
    public void complete(String message) {
        update(1.0, message);
        out.println();
    }
    
    public void clear() {
        out.print("\r" + " ".repeat(width + 50) + "\r");
    }
    
    private String buildBar(double percentage) {
        int completeBlocks = (int) (percentage * width);
        int partialBlock = (int) ((percentage * width - completeBlocks) * 4);
        
        StringBuilder bar = new StringBuilder();
        bar.append("[");
        
        for (int i = 0; i < width; i++) {
            if (i < completeBlocks) {
                if (colorEnabled) {
                    bar.append(AnsiColor.colorize(BLOCK_COMPLETE, AnsiColor.GREEN));
                } else {
                    bar.append(BLOCK_COMPLETE);
                }
            } else if (i == completeBlocks && partialBlock > 0) {
                if (colorEnabled) {
                    bar.append(AnsiColor.colorize(BLOCK_PARTIAL, AnsiColor.YELLOW));
                } else {
                    bar.append(BLOCK_PARTIAL);
                }
            } else {
                bar.append(BLOCK_INCOMPLETE);
            }
        }
        
        bar.append("]");
        return bar.toString();
    }
    
    private String getSpinner() {
        char[] chars = useUnicode ? SPINNER_CHARS : SIMPLE_SPINNER;
        char c = chars[spinnerIndex % chars.length];
        spinnerIndex++;
        
        if (colorEnabled) {
            return AnsiColor.cyan(String.valueOf(c));
        }
        return String.valueOf(c);
    }
    
    private String truncateMessage(String message, int maxLength) {
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength - 3) + "...";
    }
    
    private boolean supportsUnicode() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            String codePage = System.getenv("CHCP");
            return codePage != null && codePage.contains("65001");
        }
        return true;
    }
    
    public static ProgressBar create() {
        return new ProgressBar();
    }
    
    public static void printBanner(PrintStream out) {
        if (AnsiColor.isColorSupported()) {
            out.println();
            out.println(AnsiColor.colorize("╔════════════════════════════════════════════════════════════╗", AnsiColor.CYAN));
            out.println(AnsiColor.colorize("║           ", AnsiColor.CYAN) + 
                       AnsiColor.colorize("Java UT Agent - AI Test Generator", AnsiColor.BOLD, AnsiColor.WHITE) +
                       AnsiColor.colorize("                ║", AnsiColor.CYAN));
            out.println(AnsiColor.colorize("║                    ", AnsiColor.CYAN) + 
                       AnsiColor.colorize("Version 1.0.0", AnsiColor.YELLOW) +
                       AnsiColor.colorize("                           ║", AnsiColor.CYAN));
            out.println(AnsiColor.colorize("╚════════════════════════════════════════════════════════════╝", AnsiColor.CYAN));
            out.println();
        } else {
            out.println();
            out.println("==============================================================");
            out.println("           Java UT Agent - AI Test Generator");
            out.println("                    Version 1.0.0");
            out.println("==============================================================");
            out.println();
        }
    }
    
    public static void printSuccess(PrintStream out, String message) {
        if (AnsiColor.isColorSupported()) {
            out.println(AnsiColor.success("✅ " + message));
        } else {
            out.println("[SUCCESS] " + message);
        }
    }
    
    public static void printError(PrintStream out, String message) {
        if (AnsiColor.isColorSupported()) {
            out.println(AnsiColor.error("❌ " + message));
        } else {
            out.println("[ERROR] " + message);
        }
    }
    
    public static void printWarning(PrintStream out, String message) {
        if (AnsiColor.isColorSupported()) {
            out.println(AnsiColor.warning("⚠️  " + message));
        } else {
            out.println("[WARNING] " + message);
        }
    }
    
    public static void printInfo(PrintStream out, String message) {
        if (AnsiColor.isColorSupported()) {
            out.println(AnsiColor.info("ℹ️  " + message));
        } else {
            out.println("[INFO] " + message);
        }
    }
}
