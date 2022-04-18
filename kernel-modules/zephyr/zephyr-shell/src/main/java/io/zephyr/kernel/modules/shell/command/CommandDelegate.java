package io.zephyr.kernel.modules.shell.command;

import io.sunshower.checks.SuppressFBWarnings;
import io.zephyr.kernel.log.Logging;
import io.zephyr.kernel.modules.shell.console.Command;
import io.zephyr.kernel.modules.shell.console.CommandContext;
import io.zephyr.kernel.modules.shell.console.CommandRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine;

@Getter
@SuppressFBWarnings
@CommandLine.Command
@SuppressWarnings({"PMD.UnusedPrivateMethod", "PMD.DoNotUseThreads"})
public class CommandDelegate implements Runnable {
  @CommandLine.Parameters(index = "0")
  private String command;

  @CommandLine.Unmatched private String[] arguments;

  static final Logger log = Logging.get(CommandDelegate.class);
  private final DefaultHistory history;
  private final CommandContext context;
  private final CommandRegistry registry;

  static final String[] EMPTY_ARRAY = new String[0];

  public CommandDelegate(
      final CommandRegistry registry, final DefaultHistory history, final CommandContext context) {
    this.history = history;
    this.registry = registry;
    this.context = context;
  }

  @Override
  @SuppressWarnings({"PMD.NullAssignment"})
  public void run() {

    val shell = registry.resolve(command);
    if (shell == null) {
      return;
    }
    val commandLine = new CommandLine(shell, injectionFactory());

    if (arguments == null || arguments.length == 0) {
      arguments = EMPTY_ARRAY;
    }

    val toRun = commandLine.parseArgs(arguments);
    val actualCommand = locateActualCommand(toRun);
    history.add(actualCommand);
    try {
      actualCommand.execute(context);
    } catch (Exception e) {
      log.log(Level.WARNING, "command.delegate.execution.failed", e);
      commandLine.usage(System.out);
    }
    arguments = null;
  }

  private Command locateActualCommand(CommandLine.ParseResult parseArgs) {

    CommandLine.ParseResult parg;
    for (parg = parseArgs; parg.hasSubcommand(); ) {
      parg = parg.subcommand();
    }
    return (Command) parg.commandSpec().userObject();
  }

  private CommandLine.IFactory injectionFactory() {
    return new InjectionFactory();
  }

  @AllArgsConstructor
  static final class InjectionFactory implements CommandLine.IFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <K> K create(Class<K> cls) throws Exception {
      return CommandLine.defaultFactory().create(cls);
    }
  }
}
