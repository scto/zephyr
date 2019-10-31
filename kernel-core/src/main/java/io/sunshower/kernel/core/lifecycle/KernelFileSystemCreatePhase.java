package io.sunshower.kernel.core.lifecycle;

import io.sunshower.kernel.core.SunshowerKernel;
import io.sunshower.kernel.process.*;
import io.sunshower.kernel.process.Process;
import io.sunshower.kernel.status.Status;
import io.sunshower.kernel.status.StatusType;
import java.net.URI;
import java.nio.file.FileSystems;
import java.util.Collections;
import lombok.val;

public class KernelFileSystemCreatePhase
    extends AbstractPhase<KernelProcessEvent, KernelProcessContext> {

  final Object lock = new Object();
  static final String FILE_SYSTEM_ROOT = "droplet://kernel";

  enum EventType implements KernelProcessEvent {}

  public KernelFileSystemCreatePhase() {
    super(EventType.class);
  }

  @Override
  protected void doExecute(
      Process<KernelProcessEvent, KernelProcessContext> process, KernelProcessContext context) {
    synchronized (lock) {
      try {
        val fs = FileSystems.newFileSystem(URI.create(FILE_SYSTEM_ROOT), Collections.emptyMap());
        ((SunshowerKernel) context.getKernel()).setFileSystem(fs);
      } catch (Exception e) {
        val status = new Status(StatusType.FAILED, "failed", false);
        process.addStatus(status);
        throw status.toException();
      }
    }
  }
}