package io.zephyr.kernel.core.actions.plugin;

import io.sunshower.gyre.Scope;
import io.zephyr.kernel.Coordinate;
import io.zephyr.kernel.Lifecycle;
import io.zephyr.kernel.concurrency.Task;
import io.zephyr.kernel.core.Kernel;
import io.zephyr.kernel.core.ModuleManager;
import io.zephyr.kernel.core.Modules;
import java.io.IOException;
import java.util.logging.Logger;
import lombok.val;

public class PluginStartTask extends Task implements ModuleLifecycleTask {
  private final Kernel kernel;
  private final Coordinate coordinate;
  private final ModuleManager manager;

  static final Logger log = Logger.getLogger(PluginStartTask.class.getName());

  public PluginStartTask(Coordinate coordinate, ModuleManager manager, Kernel kernel) {
    super("plugin:start:" + coordinate.toCanonicalForm());
    this.manager = manager;
    this.kernel = kernel;
    this.coordinate = coordinate;
  }

  @Override
  @SuppressWarnings({
    "PMD.AvoidBranchingStatementAsLastInLoop",
    "PMD.DataflowAnomalyAnalysis",
    "PMD.AvoidInstantiatingObjectsInLoops"
  })
  public TaskValue run(Scope scope) {
    val module = manager.getModule(coordinate);
    try {
      Modules.start(module, kernel);
    } catch (IOException ex) {
      module.getLifecycle().setState(Lifecycle.State.Failed);
    }
    return null;
  }

  @Override
  public Coordinate getCoordinate() {
    return coordinate;
  }
}
