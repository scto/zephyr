package io.zephyr.kernel.command.commands.plugin;

import static org.junit.jupiter.api.Assertions.*;

import io.sunshower.test.common.Tests;
import io.zephyr.kernel.Lifecycle;
import io.zephyr.kernel.Module;
import io.zephyr.kernel.launch.CommandTestCase;
import io.zephyr.kernel.launch.KernelLauncher;
import java.io.File;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StartPluginCommandTest extends CommandTestCase {

  File testPlugin1;
  File testPlugin2;

  @BeforeEach
  void setUp() {

    testPlugin1 =
        Tests.relativeToProjectBuild("kernel-tests:test-plugins:test-plugin-1", "war", "libs");
    testPlugin2 =
        Tests.relativeToProjectBuild("kernel-tests:test-plugins:test-plugin-2", "war", "libs");
  }

  @Test
  void ensureListingPluginsWorks() throws InterruptedException {

    val server = startServer();
    try {
      KernelLauncher.main(
          new String[] {
            "kernel", "start", "-h", Tests.createTemp("hello" + UUID.randomUUID()).getAbsolutePath()
          });
      waitForKernel();
      runRemote("plugin", "install", testPlugin1.getAbsolutePath(), testPlugin2.getAbsolutePath());
      waitForPluginCount(2);
      val kernel = getKernel();
      val module = moduleNamed("test-plugin-1");
      runRemote("plugin", "start", module.getCoordinate().toCanonicalForm());
      waitForPluginState(
          t ->
              t.stream().filter(u -> u.getLifecycle().getState() == Lifecycle.State.Active).count()
                  == 1);

      val running = kernel.getModuleManager().getModules(Lifecycle.State.Active);
      assertEquals(running.size(), 1, "must have one running module");

    } finally {
      KernelLauncher.main(new String[] {"kernel", "stop"});
      server.stop();
    }
  }

  private Module moduleNamed(String s) {
    val modules = getKernel().getModuleManager().getModules();
    for (val module : modules) {
      if (module.getCoordinate().getName().equals(s)) {
        return module;
      }
    }
    throw new NoSuchElementException("No plugin named " + s);
  }
}
