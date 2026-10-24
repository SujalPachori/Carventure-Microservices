package com.carventure.authservice.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("within(com.carventure.authservice.controller..*)")
    public void controllerLayer() {
    }

    @Pointcut("within(com.carventure.authservice.service..*)")
    public void serviceLayer() {
    }

    @Before("controllerLayer()")
    public void logControllerEntry(JoinPoint joinPoint) {
        log.info("==> {}.{}() invoked",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
    }

    @AfterReturning("controllerLayer()")
    public void logControllerExit(JoinPoint joinPoint) {
        log.info("<== {}.{}() completed",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
    }

    @Around("serviceLayer()")
    public Object logServiceExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String signature = joinPoint.getSignature().toShortString();
        try {
            Object result = joinPoint.proceed();
            log.info("{} executed in {} ms", signature, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.error("{} threw {} after {} ms", signature, ex.getClass().getSimpleName(),
                    System.currentTimeMillis() - start);
            throw ex;
        }
    }

    @AfterThrowing(pointcut = "controllerLayer() || serviceLayer()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Throwable ex) {
        log.error("Exception in {}.{}(): {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                ex.getMessage());
    }
}
