package com.cardrive.scheduling.aop;

import com.cardrive.scheduling.dto.TestDriveResponse;
import com.cardrive.scheduling.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Cross-cutting audit trail for every state-changing scheduling action
 * (create / confirm / reject / cancel / reschedule). Deliberately implemented
 * as an aspect rather than inline logging calls scattered through
 * TestDriveService, so the "who did what, when, with what outcome" concern
 * stays in one place and can't accidentally be skipped on a new code path.
 *
 * This is intentionally log-based (not persisted) - if you need a queryable
 * audit trail, this is the seam to redirect it into its own audit_log table
 * or the outbox instead.
 */
@Aspect
@Component
@Slf4j
public class AuditAspect {

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        CurrentUser actor = extractCurrentUser(joinPoint.getArgs());
        String method = joinPoint.getSignature().toShortString();

        try {
            Object result = joinPoint.proceed();
            log.info("AUDIT action={} actor={} roles={} method={} outcome=SUCCESS {}",
                    audited.value(), actorUsername(actor), actorRoles(actor), method, summarize(result));
            return result;
        } catch (Throwable ex) {
            log.warn("AUDIT action={} actor={} roles={} method={} outcome=FAILURE error={}",
                    audited.value(), actorUsername(actor), actorRoles(actor), method, ex.getMessage());
            throw ex;
        }
    }

    private CurrentUser extractCurrentUser(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof CurrentUser cu) {
                return cu;
            }
        }
        return null;
    }

    private String actorUsername(CurrentUser actor) {
        return actor != null ? actor.getUsername() : "unknown";
    }

    private String actorRoles(CurrentUser actor) {
        return actor != null ? String.valueOf(actor.getRoles()) : "unknown";
    }

    private String summarize(Object result) {
        if (result instanceof TestDriveResponse tdr) {
            return "testDriveId=%d status=%s".formatted(tdr.id(), tdr.status());
        }
        return "";
    }
}
