package com.example.qualifier;

import org.mapstruct.Qualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom qualifier annotation used to designate a primary service
 * implementation within the application.
 * <p>
 * This annotation helps in distinguishing main service components
 * and can be useful when working with dependency injection frameworks.
 * </p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * {@code
 * @MainService
 * @Service
 * public class UserServiceImpl implements IUserService {
 *     // Implementation details
 * }
 * }
 * </pre>
 *
 * @see org.mapstruct.Qualifier
 */
@Qualifier
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface MainService {
}
