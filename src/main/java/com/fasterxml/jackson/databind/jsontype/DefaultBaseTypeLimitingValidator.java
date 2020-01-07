package com.fasterxml.jackson.databind.jsontype;

import javax.naming.Referenceable;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * {@link PolymorphicTypeValidator} that will only allow polymorphic handling if
 * the base type is NOT one of potential dangerous base types (see {@link #isUnsafeBaseType}
 * for specific list of such base types).
 *<p>
 * This is implementation is the default one used for annotation-based polymorphic deserialization.
 * Default Typing requires explicit registration of validator; while this implementation may
 * be used users are recommended to either use a custom implementation or sub-class this
 * implementation and override either {@link #validateSubClassName} or
 * {@link #validateSubType} to implement use-case specific validation.
 *<p>
 * Note that when using potentially unsafe base type like {@link java.lang.Object} a custom
 * implementation (or subtype with override) is needed. Most commonly subclasses would
 * override both {@link #isUnsafeBaseType} and {@link #isSafeSubType}: former to allow
 * all (or just more) base types, and latter to add actual validation of subtype.
 */
public class DefaultBaseTypeLimitingValidator
    extends PolymorphicTypeValidator
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    @Override
    public Validity validateBaseType(DatabindContext ctxt, JavaType baseType)
            throws JsonMappingException
    {
        // Immediately block potentially unsafe base types
        if (isUnsafeBaseType(ctxt, baseType)) {
            // and give bit more meaningful exception message too
            ctxt.reportBadDefinition(baseType, String.format(
"Configured `PolymorphicTypeValidator` (of type %s)"+
" denies resolution of all subtypes of base type %s as using too generic base type can open a security"+
" hole without checks on subtype: please configure a custom `PolymorphicTypeValidator` for this use case",
ClassUtil.classNameOf(getClass()), ClassUtil.classNameOf(baseType.getRawClass())));
            return Validity.DENIED;
        }
        // otherwise indicate that type may be ok (so further calls are made --
        // does not matter with base implementation but allows easier sub-classing)
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubClassName(DatabindContext ctxt,
            JavaType baseType, String subClassName) {
        // return INDETERMINATE just for easier sub-classing
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubType(DatabindContext ctxt, JavaType baseType,
            JavaType subType)
    {
        return isSafeSubType(ctxt, baseType, subType)
                ? Validity.ALLOWED
                : Validity.DENIED;
    }

    /**
     * Helper method called to determine if the given base type is known to be
     * problematic regarding possible "gadget types".
     * Currently includes following types:
     *<ul>
     *  <li>{@link java.lang.Object}</li>
     *  <li>{@link java.io.Closeable}</li>
     *  <li>{@link java.io.Serializable}</li>
     *  <li>{@link java.lang.AutoCloseable}</li>
     *  <li>{@link java.lang.Cloneable}</li>
     *  <li>{@link java.util.logging.Handler}</li>
     *  <li>{@link javax.naming.Referenceable}</li>
     *  <li>{@link javax.sql.DataSource}</li>
     *</ul>
     * which are JDK-included super types of at least one gadget type (not necessarily
     * included in JDK)
     *
     * @param ctxt Processing context (to give access to configuration)
     * @param baseType Base type to test
     *
     * @param True if base type is considered unsafe
     */
    protected boolean isUnsafeBaseType(DatabindContext ctxt, JavaType baseType)
    {
        final Class<?> rawBase = baseType.getRawClass();
        return (rawBase == Object.class)
                || (rawBase == java.io.Closeable.class)
                || (rawBase == java.io.Serializable.class)
                || (rawBase == AutoCloseable.class)
                || (rawBase == Cloneable.class)
                || (rawBase == java.util.logging.Handler.class)
                || (rawBase == Referenceable.class)
                || (rawBase == DataSource.class) 
            ;
    }

    /**
     * Helper called to determine whether given actual subtype is considered safe
     * to process: this will only be called if subtype was considered acceptable
     * earlier.
     *
     * @param ctxt Processing context (to give access to configuration)
     * @param baseType Base type of sub type (validated earlier)
     * @param subType Sub type to test
     *
     * @param True if sub type is considered safe for processing; false if not
     */
    protected boolean isSafeSubType(DatabindContext ctxt,
            JavaType baseType, JavaType subType)
    {
        return true;
    }
}
