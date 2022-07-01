package io.zephyr.kernel.core;

import io.sunshower.gyre.DirectedGraph;
import io.sunshower.gyre.EdgeFilters;
import io.sunshower.gyre.Graph;
import io.sunshower.gyre.ParallelScheduler;
import io.sunshower.gyre.ReverseSubgraphTransformation;
import io.sunshower.gyre.Scope;
import io.sunshower.gyre.SubgraphTransformation;
import io.sunshower.gyre.TernaryFunction;
import io.zephyr.kernel.Coordinate;
import io.zephyr.kernel.concurrency.DefaultProcess;
import io.zephyr.kernel.concurrency.Process;
import io.zephyr.kernel.concurrency.Task;
import io.zephyr.kernel.concurrency.TaskGraph;
import io.zephyr.kernel.core.actions.plugin.PluginRemoveTask;
import io.zephyr.kernel.core.actions.plugin.PluginStartTask;
import io.zephyr.kernel.core.actions.plugin.PluginStopTask;
import io.zephyr.kernel.module.ModuleLifecycle;
import io.zephyr.kernel.module.ModuleLifecycleChangeGroup;
import io.zephyr.kernel.module.ModuleLifecycleChangeRequest;
import io.zephyr.kernel.module.ModuleLifecycleStatusGroup;
import io.zephyr.kernel.module.ModuleRequest;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import lombok.val;

@SuppressWarnings({
  "PMD.UnusedPrivateMethod",
  "PMD.DataflowAnomalyAnalysis",
  "PMD.AvoidInstantiatingObjectsInLoops"
})
final class DefaultModuleLifecycleStatusChangeGroup implements ModuleLifecycleStatusGroup {

  private final Kernel kernel;
  private final ModuleManager moduleManager;
  private final Process<String> process;
  private final ModuleLifecycleChangeGroup request;

  /**
   * precondition: every task has the same type (start, stop, restart, etc.)
   *
   * @param request
   */
  public DefaultModuleLifecycleStatusChangeGroup(
      final Kernel kernel,
      final ModuleManager moduleManager,
      final ModuleLifecycleChangeGroup request) {
    this.kernel = kernel;
    this.request = request;
    this.moduleManager = moduleManager;
    this.process = createProcess(request);
  }

  private Process<String> createProcess(ModuleLifecycleChangeGroup request) {

    val taskGraph = new TaskGraph<String>();
    val tasks = new HashMap<Coordinate, Task>();

    for (val task : request.getRequests()) {
      val actions = task.getLifecycleActions();
      if (actions.isAtLeast(ModuleLifecycle.Actions.Stop)) {
        addStopAction(task, taskGraph, tasks);
      } else if (actions == ModuleLifecycle.Actions.Activate) {
        addStartAction(task, taskGraph, tasks);
      }
      if (actions.isAtLeast(ModuleLifecycle.Actions.Delete)) {
        for (val stopTask : tasks.entrySet()) {
          val removeTask =
              new PluginRemoveTask("plugin:remove:" + stopTask.getKey().toCanonicalForm(), kernel);
          taskGraph.connect(removeTask, stopTask.getValue(), DirectedGraph.incoming("remove"));
        }
      }
    }
    return new DefaultProcess<>("module:lifecycle:change", true, true, Scope.root(), taskGraph);
  }

  @Override
  public CompletionStage<Process<String>> commit() {
    return kernel.getScheduler().submit(process);
  }

  @Override
  public Process<String> getProcess() {
    return process;
  }

  @Override
  public Set<? extends ModuleRequest> getRequests() {
    return new LinkedHashSet<>(request.getRequests());
  }

  private void addStopAction(
      ModuleLifecycleChangeRequest task, TaskGraph<String> tasks, Map<Coordinate, Task> existing) {
    val reachability =
        new ReverseSubgraphTransformation<DirectedGraph.Edge<Coordinate>, Coordinate>(
                task.getCoordinate())
            .apply(
                moduleManager.getDependencyGraph().getGraph(),
                EdgeFilters.acceptAll(),
                Coordinate::isResolved);
    addAction(reachability, task, tasks, existing, (t, u, v) -> this.pluginStopTask(t, u, kernel));
  }

  private void addStartAction(
      ModuleLifecycleChangeRequest task, TaskGraph<String> tasks, Map<Coordinate, Task> existing) {

    val reachability =
        new SubgraphTransformation<DirectedGraph.Edge<Coordinate>, Coordinate>(task.getCoordinate())
            .apply(
                moduleManager.getDependencyGraph().getGraph(),
                EdgeFilters.acceptAll(),
                Coordinate::isResolved);
    addAction(reachability, task, tasks, existing, this::pluginStartTask);
  }

  private void addAction(
      Graph<DirectedGraph.Edge<Coordinate>, Coordinate> reachability,
      ModuleLifecycleChangeRequest task,
      TaskGraph<String> tasks,
      Map<Coordinate, Task> existing,
      TernaryFunction<Coordinate, ModuleManager, Kernel, Task> ctor) {

    val schedule =
        new ParallelScheduler<DirectedGraph.Edge<Coordinate>, Coordinate>()
            .apply(reachability, EdgeFilters.acceptAll(), Coordinate::isResolved)
            .getTasks();

    Task source;
    if (!existing.containsKey(task.getCoordinate())) {
      source = ctor.apply(task.getCoordinate(), moduleManager, kernel);
      tasks.add(source);
      existing.put(task.getCoordinate(), source);
    } else {
      source = existing.get(task.getCoordinate());
    }

    for (val group : schedule) {
      for (val taskSet : group.getTasks()) {
        final Task actualTask;
        if (!existing.containsKey(taskSet.getValue())) {
          actualTask = ctor.apply(taskSet.getValue(), moduleManager, kernel);
          tasks.add(actualTask);
          existing.put(taskSet.getValue(), actualTask);
        } else {
          actualTask = existing.get(taskSet.getValue());
        }

        if (!(taskSet.getValue().equals(task.getCoordinate())
            || tasks.containsEdge(source, actualTask))) {
          tasks.connect(source, actualTask, DirectedGraph.incoming("depends-on"));
        }
      }
    }
  }

  private Task pluginStopTask(Coordinate coordinate, ModuleManager manager, Kernel kernel) {
    return new PluginStopTask(coordinate, manager, kernel);
  }

  private Task pluginStartTask(Coordinate coordinate, ModuleManager manager, Kernel kernel) {
    return new PluginStartTask(coordinate, manager, kernel);
  }
}
