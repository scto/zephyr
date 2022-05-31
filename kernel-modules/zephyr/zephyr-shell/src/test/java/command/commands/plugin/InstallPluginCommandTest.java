package command.commands.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zephyr.kernel.modules.shell.ShellTestCase;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InstallPluginCommandTest extends ShellTestCase {

  @Test
  @SneakyThrows
  void ensureInstallingSinglePluginWithNoDependenciesWorks() {
    installAndWaitForModuleCount(1, TestPlugins.TEST_PLUGIN_1);
    val module = moduleNamed("test-plugin-1");
    val clazz = Class.forName("plugin1.Test", true, module.getClassLoader());
    assertEquals(module.getClassLoader(), clazz.getClassLoader(), "must have same classloader");
  }

  @Test
  @SneakyThrows
  void ensureInstallingDependentModuleProducesCorrectResults() {
    try {
      installAndWaitForModuleCount(1, TestPlugins.TEST_PLUGIN_1);
      val module = moduleNamed("test-plugin-1");
      val clazz = Class.forName("plugin1.Test", true, module.getClassLoader());
      assertEquals(module.getClassLoader(), clazz.getClassLoader(), "must have same classloader");

      installAndWaitForModuleCount(2, TestPlugins.TEST_PLUGIN_2);
      val snd = moduleNamed("test-plugin-2");

      val clazz2 = Class.forName("plugin1.Test", true, module.getClassLoader());
      assertEquals(module.getClassLoader(), clazz2.getClassLoader(), "must have same classloader");

      val p2class = Class.forName("testproject2.Test", true, snd.getClassLoader());
      assertEquals(snd.getClassLoader(), p2class.getClassLoader(), "must have same classloader");

      Class.forName("plugin1.Test", true, snd.getClassLoader());
    } finally {
      removeAll();
    }
  }
}
