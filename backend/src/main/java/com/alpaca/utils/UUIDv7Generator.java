package com.alpaca.utils;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import java.util.UUID;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.stereotype.Component;

/**
 * Custom Hibernate Identifier Generator that produces UUID v7.
 *
 * <p>This generator uses the <a href="https://github.com/cowtowncoder/java-uuid-generator">JUG
 * library</a> to create time-ordered, epoch-based UUIDs.
 *
 * <p>UUID v7 structure provides:
 *
 * <ul>
 *   <li>48-bit Unix Timestamp (milliseconds precision).
 *   <li>Monotonic sequence counter (to handle multiple generations within the same millisecond).
 *   <li>Random data for uniqueness.
 * </ul>
 *
 * <p>This implementation is thread-safe and ensures monotonicity by using a static generator
 * instance.
 */
@Component
public class UUIDv7Generator implements IdentifierGenerator {

    /**
     * Singleton instance of the JUG TimeBasedEpochGenerator.
     *
     * <p>It is crucial to keep this instance static/shared. If instantiated per call, the internal
     * counter for sub-millisecond sorting would reset, defeating the purpose of the monotonic
     * logic.
     */
    private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();

    /**
     * Generates a new UUID v7.
     *
     * @param session The session from which the request originates.
     * @param object The entity or object for which the id is being generated.
     * @return A new {@link UUID} (v7) instance.
     * @throws HibernateException Indicates trouble generating the identifier.
     */
    @Override
    public UUID generate(SharedSessionContractImplementor session, Object object)
            throws HibernateException {
        return GENERATOR.generate();
    }

    /**
     * Convenience method to generate a UUID v7 without requiring a Hibernate session or entity.
     *
     * <p>Useful when UUIDs need to be created outside of JPA entity hydration — for example, in
     * service or use-case layers that need a deterministic, time-ordered identifier before
     * persisting.
     *
     * @return a new time-ordered UUID v7
     */
    public UUID generate() {
        return GENERATOR.generate();
    }
}
