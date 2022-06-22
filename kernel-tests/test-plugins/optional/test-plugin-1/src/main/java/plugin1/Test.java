package plugin1;

import io.zephyr.api.*;

public class Test implements ModuleActivator {
  private ModuleTracker moduleTracker;
  private ServiceRegistration<Service> registration;

  @Override
  public void start(ModuleContext context) {
    System.out.println("Plugin1 starting...");
    moduleTracker = context.trackModules(t -> true);
    moduleTracker.addEventListener(
        (type, event) -> System.out.format("%s: %s", type, event.getTarget()),
        ModuleEvents.INSTALLED);
    registration = context.register(Service.class, new Plugin1Service());
    System.out.println("Plugin1 started...");
  }

  @Override
  public void stop(ModuleContext context) {
    System.out.println("Plugin1 stopping...");
    moduleTracker.close();
    registration.close();
    System.out.println("Plugin1 stopped");
  }
}
