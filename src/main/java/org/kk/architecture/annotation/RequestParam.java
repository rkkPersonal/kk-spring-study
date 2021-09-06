package org.kk.architecture.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Steven
 */
@Target({PARAMETER})
@Retention(RUNTIME)
@Documented
public @interface RequestParam {

    String value() default "";
}
