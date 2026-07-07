package com.alpaca.resources.utility;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = CustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
    String username() default "admin";

    String email() default "advisor@advisor.com";

    int id() default 1;
}
