package info.kgeorgiy.ja.smirnov.implementor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * Method wrapper.
 * <p>
 * Wrapper is used in {@link Implementor} class
 * @author Andrew Smirnov
 */
public record MyMethod(Method method) {

    /**
     * Get {@code Method}
     * @return {@code Method} of this wrapper
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Equals method
     * @param o another {@code Object}
     * @return true if methods' names and arrays of parameters types are equals
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MyMethod myMethod = (MyMethod) o;
        return method.getName().equals(myMethod.method.getName()) &&
                Arrays.equals(method.getParameterTypes(), myMethod.method.getParameterTypes());
    }

    /**
     * Hashcode method
     * @return hashcode based on method's name and parameter types
     */
    @Override
    public int hashCode() {
        return Objects.hash(method.getName(), Arrays.hashCode(method.getParameterTypes()));
    }
}
