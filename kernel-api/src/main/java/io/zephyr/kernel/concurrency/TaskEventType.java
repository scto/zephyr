package io.zephyr.kernel.concurrency;

import io.sunshower.lang.events.EventType;
import io.zephyr.kernel.status.StatusType;

public interface TaskEventType extends EventType {

  StatusType getStatusType();
}
