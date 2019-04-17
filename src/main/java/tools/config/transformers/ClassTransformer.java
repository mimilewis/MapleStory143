package tools.config.transformers;

import tools.config.TransformationException;

import java.lang.reflect.Field;

/**
 * Returns the
 * <code>Class</code> object associated with the class or interface with the given string name. The class is
 * not being initialized. <br />
 * Created on: 12.09.2009 15:10:47
 *
 * @author Aquanox
 * @see Class#forName(String)
 * @see Class#forName(String, boolean, ClassLoader)
 */
public class ClassTransformer implements PropertyTransformer<Class<?>> {

    /**
     * Shared instance.
     */
    public static final ClassTransformer SHARED_INSTANCE = new ClassTransformer();

    @Override
    public Class<?> transform(String value, Field field) throws TransformationException {
        try {
            return Class.forName(value, false, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new TransformationException("Cannot find class with name '" + value + "'");
        }
    }
}
