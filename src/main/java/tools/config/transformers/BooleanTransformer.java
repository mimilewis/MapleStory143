package tools.config.transformers;

import tools.config.TransformationException;

import java.lang.reflect.Field;

/**
 * This class implements basic boolean transfromer.
 * <p/>
 * Boolean can be represented by "true/false" (case doen't matter) or "1/0". In other cases
 * {@link com.aionlightning.commons.configuration.TransformationException} is thrown
 *
 * @author SoulKeeper
 */
public class BooleanTransformer implements PropertyTransformer<Boolean> {

    /**
     * Shared instance of this transformer, it's thread safe so no need to create multiple instances
     */
    public static final BooleanTransformer SHARED_INSTANCE = new BooleanTransformer();

    /**
     * Transforms string to boolean.
     *
     * @param value value that will be transformed
     * @param field value will be assigned to this field
     * @return Boolean object that represents transformed value
     * @throws TransformationException if something goes wrong
     */
    @Override
    public Boolean transform(String value, Field field) throws TransformationException {
        // We should have error here if value is not correct, default
        // "Boolean.parseBoolean" returns false if string
        // is not "true" ignoring case
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return false;
        } else {
            throw new TransformationException("Invalid boolean string: " + value);
        }
    }
}
