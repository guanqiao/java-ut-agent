package com.utagent.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

/**
 * 全局异常处理器，统一处理应用程序中的异常。
 * 提供统一的异常日志记录、错误消息格式化和错误恢复机制。
 */
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Consumer<Throwable> customHandler;
    private boolean verbose;

    public GlobalExceptionHandler() {
        this.verbose = false;
    }

    public GlobalExceptionHandler(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * 设置自定义异常处理器
     */
    public void setCustomHandler(Consumer<Throwable> handler) {
        this.customHandler = handler;
    }

    /**
     * 处理异常，根据异常类型选择适当的处理方式
     */
    public void handleException(Throwable throwable) {
        handleException(throwable, null);
    }

    /**
     * 处理异常，附带上下文信息
     */
    public void handleException(Throwable throwable, String context) {
        if (throwable instanceof UTAgentException utAgentException) {
            handleUTAgentException(utAgentException, context);
        } else if (throwable instanceof IllegalArgumentException) {
            handleIllegalArgumentException((IllegalArgumentException) throwable, context);
        } else if (throwable instanceof IllegalStateException) {
            handleIllegalStateException((IllegalStateException) throwable, context);
        } else {
            handleGenericException(throwable, context);
        }

        if (customHandler != null) {
            customHandler.accept(throwable);
        }
    }

    private void handleUTAgentException(UTAgentException exception, String context) {
        String message = formatMessage(exception, context);
        UTAgentException.ErrorCode errorCode = exception.getErrorCode();

        if (errorCode == UTAgentException.ErrorCode.PARSE_ERROR) {
            logger.error("[Parse Error] {}", message);
        } else if (errorCode == UTAgentException.ErrorCode.GENERATION_ERROR) {
            logger.error("[Generation Error] {}", message);
        } else if (errorCode == UTAgentException.ErrorCode.COVERAGE_ERROR) {
            logger.error("[Coverage Error] {}", message);
        } else if (errorCode == UTAgentException.ErrorCode.CONFIG_ERROR) {
            logger.error("[Config Error] {}", message);
        } else if (errorCode == UTAgentException.ErrorCode.LLM_ERROR) {
            logger.error("[LLM Error] {}", message);
        } else if (errorCode == UTAgentException.ErrorCode.BUILD_ERROR) {
            logger.error("[Build Error] {}", message);
        } else if (errorCode == UTAgentException.ErrorCode.VALIDATION_ERROR) {
            logger.warn("[Validation Error] {}", message);
        } else if (errorCode == UTAgentException.ErrorCode.TIMEOUT_ERROR) {
            logger.error("[Timeout Error] {}", message);
        } else if (errorCode == UTAgentException.ErrorCode.RESOURCE_ERROR) {
            logger.error("[Resource Error] {}", message);
        } else {
            logger.error("[Unknown Error] {}", message);
        }

        if (verbose && exception.getCause() != null) {
            logger.debug("Caused by: ", exception.getCause());
        }
    }

    private void handleIllegalArgumentException(IllegalArgumentException exception, String context) {
        String message = formatMessage(exception, context);
        logger.error("[Invalid Argument] {}", message);
    }

    private void handleIllegalStateException(IllegalStateException exception, String context) {
        String message = formatMessage(exception, context);
        logger.error("[Invalid State] {}", message);
    }

    private void handleGenericException(Throwable throwable, String context) {
        String message = formatMessage(throwable, context);
        logger.error("[Unexpected Error] {}", message, throwable);
    }

    private String formatMessage(Throwable throwable, String context) {
        StringBuilder sb = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            sb.append("[").append(context).append("] ");
        }
        sb.append(throwable.getMessage());
        return sb.toString();
    }

    /**
     * 将异常转换为用户友好的错误消息
     */
    public String toUserFriendlyMessage(Throwable throwable) {
        if (throwable instanceof UTAgentException utAgentException) {
            return toUserFriendlyMessage(utAgentException);
        }
        return "An unexpected error occurred: " + throwable.getMessage();
    }

    private String toUserFriendlyMessage(UTAgentException exception) {
        UTAgentException.ErrorCode errorCode = exception.getErrorCode();

        if (errorCode == UTAgentException.ErrorCode.PARSE_ERROR) {
            return "Failed to parse the source code. Please check if the file is valid Java code.";
        } else if (errorCode == UTAgentException.ErrorCode.GENERATION_ERROR) {
            return "Failed to generate tests. Please try again or check your configuration.";
        } else if (errorCode == UTAgentException.ErrorCode.COVERAGE_ERROR) {
            return "Failed to analyze coverage. Please ensure JaCoCo is properly configured.";
        } else if (errorCode == UTAgentException.ErrorCode.CONFIG_ERROR) {
            return "Configuration error: " + exception.getMessage();
        } else if (errorCode == UTAgentException.ErrorCode.LLM_ERROR) {
            return "LLM service error: " + exception.getMessage();
        } else if (errorCode == UTAgentException.ErrorCode.BUILD_ERROR) {
            return "Build failed. Please check your project configuration.";
        } else if (errorCode == UTAgentException.ErrorCode.VALIDATION_ERROR) {
            return "Validation failed: " + exception.getMessage();
        } else if (errorCode == UTAgentException.ErrorCode.TIMEOUT_ERROR) {
            return "Operation timed out. Please try again or increase the timeout.";
        } else if (errorCode == UTAgentException.ErrorCode.RESOURCE_ERROR) {
            return "Resource error: " + exception.getMessage();
        } else {
            return exception.getMessage();
        }
    }

    /**
     * 获取异常的堆栈跟踪字符串
     */
    public String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * 执行操作并捕获异常
     */
    public <T> T executeWithHandler(ExceptionalSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            handleException(e);
            return defaultValue;
        }
    }

    /**
     * 执行操作并捕获异常（无返回值）
     */
    public void executeWithHandler(ExceptionalRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            handleException(e);
        }
    }

    @FunctionalInterface
    public interface ExceptionalSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface ExceptionalRunnable {
        void run() throws Exception;
    }
}
