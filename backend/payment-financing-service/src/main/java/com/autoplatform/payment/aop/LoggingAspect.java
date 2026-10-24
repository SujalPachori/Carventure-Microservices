package com.autoplatform.payment.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(public * com.autoplatform.payment.service..*(..))")
    public void serviceLayer() {
    }

    @Around("serviceLayer()")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();

        if (log.isDebugEnabled()) {
            log.debug(">> {} args={}", signature, Arrays.toString(joinPoint.getArgs()));
        }

        try {
            Object result = joinPoint.proceed();
            log.info("{} completed in {}ms", signature, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.warn("{} failed after {}ms: {}: {}", signature, System.currentTimeMillis() - start,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}
