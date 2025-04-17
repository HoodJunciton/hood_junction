package com.thehoodjunction.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void servicePointcut() {
    }

    @Around("servicePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodId = UUID.randomUUID().toString().substring(0, 8);
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        
        Object[] args = joinPoint.getArgs();
        
        // Log method entry
        logMethodEntry(methodId, fullMethodName, args);
        
        long startTime = System.currentTimeMillis();
        Object result = null;
        
        try {
            // Execute the method
            result = joinPoint.proceed();
            return result;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // Log method exit
            logMethodExit(methodId, fullMethodName, result, duration);
        }
    }
    
    @AfterThrowing(pointcut = "servicePointcut()", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable exception) {
        String methodId = UUID.randomUUID().toString().substring(0, 8);
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String fullMethodName = className + "." + methodName;
        
        // Log method exception
        logMethodException(methodId, fullMethodName, exception);
    }
    
    private void logMethodEntry(String methodId, String methodName, Object[] args) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n┌─────────────────────────────────────────────────────────────────────────────────┐\n");
        logMessage.append(String.format("│ 🔹 METHOD ENTRY [%s] %s                                               \n", methodId, methodName));
        logMessage.append("├─────────────────────────────────────────────────────────────────────────────────┤\n");
        
        if (args != null && args.length > 0) {
            logMessage.append("│ 📥 Parameters:                                                                  \n");
            for (int i = 0; i < args.length; i++) {
                String argValue = args[i] == null ? "null" : args[i].toString();
                // Truncate if too long
                if (argValue.length() > 80) {
                    argValue = argValue.substring(0, 80) + "...";
                }
                logMessage.append(String.format("│   arg[%d]: %s                                                     \n", i, argValue));
            }
        } else {
            logMessage.append("│ 📥 Parameters: none                                                             \n");
        }
        
        logMessage.append("└─────────────────────────────────────────────────────────────────────────────────┘");
        log.debug(logMessage.toString());
    }
    
    private void logMethodExit(String methodId, String methodName, Object result, long duration) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n┌─────────────────────────────────────────────────────────────────────────────────┐\n");
        logMessage.append(String.format("│ 🔹 METHOD EXIT [%s] %s (%d ms)                                         \n", methodId, methodName, duration));
        logMessage.append("├─────────────────────────────────────────────────────────────────────────────────┤\n");
        
        if (result != null) {
            String resultStr = result.toString();
            // Truncate if too long
            if (resultStr.length() > 100) {
                resultStr = resultStr.substring(0, 100) + "...";
            }
            logMessage.append(String.format("│ 📤 Result: %s                                                        \n", resultStr));
        } else {
            logMessage.append("│ 📤 Result: void/null                                                           \n");
        }
        
        logMessage.append("└─────────────────────────────────────────────────────────────────────────────────┘");
        log.debug(logMessage.toString());
    }
    
    private void logMethodException(String methodId, String methodName, Throwable exception) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n┌─────────────────────────────────────────────────────────────────────────────────┐\n");
        logMessage.append(String.format("│ 🔥 METHOD EXCEPTION [%s] %s                                            \n", methodId, methodName));
        logMessage.append("├─────────────────────────────────────────────────────────────────────────────────┤\n");
        logMessage.append(String.format("│ ❌ Exception: %s                                                       \n", exception.getClass().getSimpleName()));
        logMessage.append(String.format("│ 📝 Message: %s                                                         \n", exception.getMessage()));
        logMessage.append("└─────────────────────────────────────────────────────────────────────────────────┘");
        log.error(logMessage.toString());
    }
}
