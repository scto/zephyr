package io.zephyr.api;

import io.sunshower.lang.events.EventSource;
import java.io.Closeable;

/**
 * todo: implement relevant event types
 *
 * @param <T>
 */
public interface RequirementRegistration<T> extends EventSource, Closeable {}
