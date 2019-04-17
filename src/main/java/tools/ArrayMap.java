package tools;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

/**
 * Provides a strongly-typed map of keys to values.
 *
 * @param <K> The type of the keys.
 * @param <V> The type of the values.
 * @author Frz
 * @version 1.0
 * @since Revision 589
 */
public class ArrayMap<K, V> extends AbstractMap<K, V> implements Serializable {

    public static final long serialVersionUID = 9179541993413738569L;
    private final ArrayList<Entry<K, V>> list;
    private transient Set<? extends java.util.Map.Entry<K, V>> entries = null;

    /**
     * Class constructor
     */
    public ArrayMap() {
        list = new ArrayList<>();
    }

    /**
     * Class constructor.
     *
     * @param map The
     *            <code>java.util.Map</code> containing keys and values to
     *            import.
     */
    public ArrayMap(Map<K, V> map) {
        list = new ArrayList<>();
        putAll(map);
    }

    /**
     * Class constructor.
     *
     * @param initialCapacity The initial size of the ArrayMap.
     */
    public ArrayMap(int initialCapacity) {
        list = new ArrayList<>(initialCapacity);
    }

    /**
     * Returns a set of entries in this ArrayList.
     *
     * @return The entries in a
     * <code>java.util.Set</code> instance.
     */
    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        if (entries == null) {
            entries = new AbstractSet<Entry<K, V>>() {

                @Override
                public void clear() {
                    throw new UnsupportedOperationException();
                }

                @NotNull
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return list.iterator();
                }

                @Override
                public int size() {
                    return list.size();
                }
            };
        }
        return (Set<java.util.Map.Entry<K, V>>) entries;
    }

    /**
     * Puts a key/value pair into the ArrayMap.
     *
     * @param key   The key of
     *              <code>value</code>
     * @param value The value to insert into the ArrayMap.
     * @return <code>null</code> if no entry was replaced, the value replaced
     * otherwise.
     */
    @Override
    public V put(K key, V value) {
        int size = list.size();
        Entry<K, V> entry = null;
        int i;
        if (key == null) {
            for (i = 0; i < size; i++) {
                entry = (list.get(i));
                if (entry.getKey() == null) {
                    break;
                }
            }
        } else {
            for (i = 0; i < size; i++) {
                entry = (list.get(i));
                if (key.equals(entry.getKey())) {
                    break;
                }
            }
        }
        V oldValue = null;
        if (i < size) {
            oldValue = entry.getValue();
            entry.setValue(value);
        } else {
            list.add(new Entry<>(key, value));
        }
        return oldValue;
    }

    /**
     * Provides a strongly typed mapping of a key to a value.
     *
     * @param <K> The type of the key.
     * @param <V> The type of the value.
     * @author Frz
     * @version 1.0
     * @since Revision 589
     */
    public static class Entry<K, V> implements Map.Entry<K, V>, Serializable {

        public static final long serialVersionUID = 9179541993413738569L;
        protected final K key;
        protected V value;

        /**
         * Class constructor
         *
         * @param key   Name of the key
         * @param value The value.
         */
        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Gets the key.
         *
         * @return The key.
         */
        @Override
        public K getKey() {
            return key;
        }

        /**
         * Gets the value.
         *
         * @return The value.
         */
        @Override
        public V getValue() {
            return value;
        }

        /**
         * Sets a new value.
         *
         * @return The old value.
         */
        @Override
        public V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        /**
         * Compares two Entries for equality.
         *
         * @return <code>True</code> if the two Entries are equal,
         * <code>False</code> otherwise.
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) o;
            return (key == null ? e.getKey() == null : key.equals(e.getKey()))
                    && (value == null ? e.getValue() == null : value.equals(e.getValue()));
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            int keyHash = (key == null ? 0 : key.hashCode());
            int valueHash = (value == null ? 0 : value.hashCode());
            return keyHash ^ valueHash;
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return key + "=" + value;
        }
    }
}
