package com.alpaca.utils;

import java.lang.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Meta-annotation that secures a method or class by requiring an authenticated user.
 *
 * <p>This is a convenience shortcut for {@code @PreAuthorize("isAuthenticated()")}. Applying it to
 * a controller method or class enforces that only authenticated requests are allowed, without
 * repeating the SpEL expression. The annotation is inherited, so a single class-level annotation
 * applies to all contained handler methods.
 *
 * @see org.springframework.security.access.prepost.PreAuthorize
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PreAuthorize("isAuthenticated()")
public @interface IsAuthenticated {}
