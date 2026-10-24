package com.cardrive.scheduling.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Lightweight performance visibility across the whole service layer: logs
 * execution time for every public method in com.cardrive.scheduling.service,
 * and flags anything unexpectedly slow. Useful for spotting a scheduling
 * operation that's quietly holding a transaction/lock open too long (which,
 * per the concurrency design, must stay short).
 */
@Aspect
@Component
@Slf4j
public class ServiceTimingAspect {

    private static final long SLOW_THRESHOLD_MS = 500;

    @Pointcut("execution(public * com.cardrive.scheduling.service..*(..))")
    public void serviceLayer() {
    }

    @Around("serviceLayer()")
    public Object timeExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            String signature = joinPoint.getSignature().toShortString();
            if (elapsed >= SLOW_THRESHOLD_MS) {
                log.warn("SLOW service call {} took {}ms", signature, elapsed);
            } else {
                log.debug("{} completed in {}ms", signature, elapsed);
            }
        }
    }
}
