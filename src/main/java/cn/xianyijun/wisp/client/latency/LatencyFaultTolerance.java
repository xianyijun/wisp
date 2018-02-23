package cn.xianyijun.wisp.client.latency;

/**
 * The interface Latency fault tolerance.
 *
 * @param <T> the type parameter
 */
public interface LatencyFaultTolerance<T> {

    /**
     * Update fault item.
     *
     * @param name                 the name
     * @param currentLatency       the current latency
     * @param notAvailableDuration the not available duration
     */
    void updateFaultItem(final T name, final long currentLatency, final long notAvailableDuration);

    /**
     * Is available boolean.
     *
     * @param name the name
     * @return the boolean
     */
    boolean isAvailable(final T name);


    /**
     * Remove.
     *
     * @param name the name
     */
    void remove(final T name);

    /**
     * Pick one at least t.
     *
     * @return the t
     */
    T pickOneAtLeast();
}
