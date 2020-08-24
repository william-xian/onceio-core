package top.onceio.core.cache;

/**
 * Interface that defines common cache operations.
 *
 * <b>Note:</b> Due to the generic use of caching, it is recommended that
 * implementations allow storage of <tt>null</tt> values (for example to
 * cache methods that return {@code null}).
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public interface Cache {

    /**
     * Return the cache name.
     */
    String getName();


    /**
     *
     * @param key
     * @param type
     * @param <T>
     * @return 返回指定类型的指定Key的缓存对象，如果没有则返回null值
     */
    <T> T get(Object key, Class<T> type);

    /**
     * Associate the specified value with the specified key in this cache.
     * <p>If the cache previously contained a mapping for this key, the old
     * value is replaced by the specified value.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    void put(Object key, Object value);

    /**
     * Evict the mapping for this key from this cache if it is present.
     *
     * @param key the key whose mapping is to be removed from the cache
     */
    void evict(Object key);

    /**
     * Remove all mappings from the cache.
     */
    void clear();

}
