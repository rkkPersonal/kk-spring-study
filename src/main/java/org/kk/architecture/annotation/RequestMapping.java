package org.kk.architecture.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Steven
 */
@Target({TYPE,METHOD})
@Retention(RUNTIME)
@Documented
public @interface RequestMapping {

    String value() default "";
}
