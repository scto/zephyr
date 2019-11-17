package io.sunshower.kernel.core.actions;

import io.sunshower.gyre.Scope;
import io.sunshower.kernel.Lifecycle;
import io.sunshower.kernel.Module;
import io.sunshower.kernel.concurrency.Task;
import io.sunshower.kernel.core.*;
import io.sunshower.kernel.dependencies.CyclicDependencyException;
import io.sunshower.kernel.dependencies.DependencyGraph;
import io.sunshower.kernel.dependencies.UnresolvedDependencyException;
import io.sunshower.kernel.events.EventType;
import io.sunshower.kernel.events.Events;
import io.sunshower.kernel.log.Logging;
import io.sunshower.kernel.memento.core.PluginCaretaker;
import java.nio.file.FileSystem;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.val;

@SuppressWarnings({
  "PMD.UnusedPrivateMethod",
  "PMD.UnusedFormalParameter",
  "PMD.DataflowAnomalyAnalysis"
})
public class WritePluginDescriptorPhase extends Task {
  static final Logger log = Logging.get(WritePluginDescriptorPhase.class);

  public WritePluginDescriptorPhase(String name) {
    super(name);
  }

  @Override
  public TaskValue run(Scope scope) {
    final Set<Module> installedPlugins =
        scope.get(ModuleInstallationCompletionPhase.INSTALLED_PLUGINS);

    if (installedPlugins == null || installedPlugins.isEmpty()) {
      log.log(Level.INFO, "plugin.phase.noplugins");
      return null;
    }

    log.log(Level.INFO, "plugin.phase.writingplugins", installedPlugins.size());

    val kernel = scope.<SunshowerKernel>get("SunshowerKernel");

    //    ServiceLoader<PluginCaretaker> caretakers =
    //        ServiceLoader.load(PluginCaretaker.class, kernel.getClassLoader());
    //
    //    val caretaker = caretakers.findFirst();
    //    if (caretaker.isEmpty()) {
    //      log.log(Level.WARNING, "plugin.phase.nocaretakers");
    //      throw new TaskException(TaskStatus.UNRECOVERABLE);
    //    }

    PluginCaretaker actualCaretaker = null;
    val moduleManager = kernel.getModuleManager();

    log.log(Level.INFO, "plugin.phase.resolvingplugins");

    val dependencyGraph = moduleManager.getDependencyGraph();

    checkForUnresolvedDependencies(dependencyGraph, installedPlugins);
    checkForCyclicDependencies(dependencyGraph, installedPlugins);
    resolvePlugins(moduleManager, installedPlugins);

    saveAll(kernel.getFileSystem(), moduleManager, actualCaretaker, installedPlugins);
    kernel.dispatchEvent(
        PluginEvents.PLUGIN_SET_INSTALLATION_COMPLETE, Events.create(installedPlugins));

    return null;
  }

  private void resolvePlugins(ModuleManager moduleManager, Set<Module> installedPlugins) {
    for (val module : installedPlugins) {
      val defaultModule = (DefaultModule) module;
      moduleManager.getModuleLoader().install(defaultModule);
      module.getLifecycle().setState(Lifecycle.State.Resolved);
    }
  }

  private void checkForCyclicDependencies(
      DependencyGraph dependencyGraph, Set<Module> installedPlugins) {
    val prospective = dependencyGraph.clone();
    prospective.addAll(installedPlugins);
    val partition = prospective.computeCycles();
    if (partition.isCyclic()) {
      val ex = new CyclicDependencyException();
      for (val cycle : partition.getElements()) {
        if (cycle.isCyclic()) {
          ex.addComponent(cycle);
          for (val el : cycle.getElements()) {
            val coord = el.getSnd();
            for (val plugin : installedPlugins) {
              if (coord.equals(plugin.getCoordinate())) {
                val fs = plugin.getFileSystem();
                if (fs != null) {
                  try {
                    fs.close();
                  } catch (Exception x) {
                    // not much to do here
                    ex.addSuppressed(x);
                  }
                }
              }
            }
          }
        }
      }

      throw ex;
    }
    log.info("plugin.phase.nocycles");

    dependencyGraph.addAll(installedPlugins);
  }

  private void checkForUnresolvedDependencies(
      DependencyGraph dependencyGraph, Collection<Module> installedPlugins) {
    val results = dependencyGraph.getUnresolvedDependencies(installedPlugins);
    val unsatisfied = new HashSet<DependencyGraph.UnsatisfiedDependencySet>();
    for (val unresolvedDependency : results) {

      if (!unresolvedDependency.isSatisfied()) {
        val plugin = unresolvedDependency.getSource();
        for (val installedPlugin : installedPlugins) {
          if (plugin.equals(installedPlugin.getCoordinate())) {
            try {
              val fs = installedPlugin.getFileSystem();
              if (fs != null) {
                fs.close();
              }
            } catch (Exception ex) {
              log.log(Level.WARNING, "failed to close plugin filesystem {0}", plugin);
            }
          }
        }
        unsatisfied.add(unresolvedDependency);
      }
    }

    if (!unsatisfied.isEmpty()) {
      log.warning("plugin.phase.unresolveddependencies");
      throw new UnresolvedDependencyException("unresolved dependencies detected", unsatisfied);
    }
  }

  private void saveAll(
      FileSystem fileSystem,
      ModuleManager moduleManager,
      PluginCaretaker actualCaretaker,
      Set<Module> installedPlugins) {}
}
