package com.cardrive.scheduling.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method as a business-significant action that must be
 * audit-logged: who did it, what they acted on, and what the outcome was.
 * Picked up by {@link AuditAspect}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {
    /** Short action name, e.g. "TEST_DRIVE_CONFIRM". */
    String value();
}
