package io.zephyr.kernel.concurrency;

import io.sunshower.lang.events.Event;
import io.zephyr.kernel.status.Status;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class TaskEvent implements Event<Task> {

  private final Task target;
  private final Status status;

  public TaskEvent(final Task target, final Status status) {
    this.target = target;
    this.status = status;
  }

  @Override
  public Task getTarget() {
    return target;
  }

  public Status getStatus() {
    return status;
  }
}
