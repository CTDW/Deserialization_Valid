package com.seeease.common.plugin.serialValid;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface NotNull{
    String message() default "";
}
