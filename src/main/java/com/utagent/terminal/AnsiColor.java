package com.utagent.terminal;

public enum AnsiColor {
    RESET("\u001B[0m"),
    BLACK("\u001B[30m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    MAGENTA("\u001B[35m"),
    CYAN("\u001B[36m"),
    WHITE("\u001B[37m"),
    BRIGHT_BLACK("\u001B[90m"),
    BRIGHT_RED("\u001B[91m"),
    BRIGHT_GREEN("\u001B[92m"),
    BRIGHT_YELLOW("\u001B[93m"),
    BRIGHT_BLUE("\u001B[94m"),
    BRIGHT_MAGENTA("\u001B[95m"),
    BRIGHT_CYAN("\u001B[96m"),
    BRIGHT_WHITE("\u001B[97m"),
    
    BG_BLACK("\u001B[40m"),
    BG_RED("\u001B[41m"),
    BG_GREEN("\u001B[42m"),
    BG_YELLOW("\u001B[43m"),
    BG_BLUE("\u001B[44m"),
    BG_MAGENTA("\u001B[45m"),
    BG_CYAN("\u001B[46m"),
    BG_WHITE("\u001B[47m"),
    
    BOLD("\u001B[1m"),
    DIM("\u001B[2m"),
    ITALIC("\u001B[3m"),
    UNDERLINE("\u001B[4m"),
    BLINK("\u001B[5m"),
    REVERSE("\u001B[7m"),
    HIDDEN("\u001B[8m"),
    
    CURSOR_SAVE("\u001B[s"),
    CURSOR_RESTORE("\u001B[u"),
    CURSOR_HIDE("\u001B[?25l"),
    CURSOR_SHOW("\u001B[?25h"),
    CLEAR_LINE("\u001B[2K"),
    CLEAR_SCREEN("\u001B[2J"),
    CURSOR_HOME("\u001B[H");
    
    private final String code;
    
    AnsiColor(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public static boolean isColorSupported() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            String term = System.getenv("TERM");
            String wtSession = System.getenv("WT_SESSION");
            return term != null || wtSession != null;
        }
        return true;
    }
    
    public static String colorize(String text, AnsiColor... colors) {
        if (!isColorSupported()) {
            return text;
        }
        
        StringBuilder sb = new StringBuilder();
        for (AnsiColor color : colors) {
            sb.append(color.getCode());
        }
        sb.append(text);
        sb.append(RESET.getCode());
        return sb.toString();
    }
    
    public static String green(String text) {
        return colorize(text, GREEN);
    }
    
    public static String red(String text) {
        return colorize(text, RED);
    }
    
    public static String yellow(String text) {
        return colorize(text, YELLOW);
    }
    
    public static String blue(String text) {
        return colorize(text, BLUE);
    }
    
    public static String cyan(String text) {
        return colorize(text, CYAN);
    }
    
    public static String bold(String text) {
        return colorize(text, BOLD);
    }
    
    public static String dim(String text) {
        return colorize(text, DIM);
    }
    
    public static String success(String text) {
        return colorize(text, GREEN, BOLD);
    }
    
    public static String error(String text) {
        return colorize(text, RED, BOLD);
    }
    
    public static String warning(String text) {
        return colorize(text, YELLOW);
    }
    
    public static String info(String text) {
        return colorize(text, CYAN);
    }
}
