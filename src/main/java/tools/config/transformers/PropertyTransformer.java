package tools.config.transformers;

import tools.config.TransformationException;

import java.lang.reflect.Field;

/**
 * This insterface represents property transformer, each transformer should implement it.
 *
 * @param <T> Type of returned value
 * @author SoulKeeper
 */
public interface PropertyTransformer<T> {

    /**
     * This method actually transforms value to object instance
     *
     * @param value value that will be transformed
     * @param field value will be assigned to this field
     * @return result of transformation
     * @throws TransformationException if something went wrong
     */
    T transform(String value, Field field) throws TransformationException;
}
