package io.zephyr.kernel.shell.commands;

import io.zephyr.kernel.launch.KernelLauncher;
import io.zephyr.kernel.module.ModuleLifecycle;
import picocli.CommandLine;

@CommandLine.Command(name = "start")
public class StartPluginCommand extends PluginLifecycleCommand {
  @CommandLine.Parameters(index = "0..*")
  private String args[];

  @Override
  protected int execute() {
    apply(KernelLauncher.getKernel(), args, ModuleLifecycle.Actions.Activate);
    return 0;
  }
}
