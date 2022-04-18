package io.zephyr.kernel.core;

import io.sunshower.lang.events.EventSource;

@SuppressWarnings("PMD.FinalizeOverloaded")
public interface KernelMember extends EventSource {

  void initialize(Kernel kernel);

  void finalize(Kernel kernel);
}
