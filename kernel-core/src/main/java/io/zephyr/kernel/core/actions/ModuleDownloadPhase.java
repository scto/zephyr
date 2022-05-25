package io.zephyr.kernel.core.actions;

import io.sunshower.gyre.Scope;
import io.zephyr.common.io.Files;
import io.zephyr.common.io.MonitorableChannels;
import io.zephyr.kernel.concurrency.Task;
import io.zephyr.kernel.concurrency.TaskException;
import io.zephyr.kernel.concurrency.TaskStatus;
import io.zephyr.kernel.core.Kernel;
import io.zephyr.kernel.events.KernelEvents;
import io.zephyr.kernel.io.ChannelTransferListener;
import io.zephyr.kernel.log.Logging;
import io.zephyr.kernel.status.StatusType;
import java.io.File;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.AllArgsConstructor;
import lombok.val;

/** Downloads a file from a URL into a temp directory */
@SuppressWarnings("PMD.UnusedFormalParameter")
public class ModuleDownloadPhase extends Task implements ChannelTransferListener {

  public static final String DOWNLOAD_URL = "MODULE_DOWNLOAD_URL";
  public static final String DOWNLOADED_FILE = "DOWNLOADED_MODULE_FILE";
  public static final String TARGET_DIRECTORY = "MODULE_TARGET_DIRECTORY";
  static final Logger log = Logging.get(ModuleDownloadPhase.class);
  static final ResourceBundle bundle;

  static {
    bundle = log.getResourceBundle();
  }

  private final ThreadLocal<File> targetFile;

  /** */
  public ModuleDownloadPhase(String name) {
    super(name);
    targetFile = new ThreadLocal<>();
  }

  @Override
  public Task.TaskValue run(Scope scope) {
    URL downloadUrl = (URL) parameters().get(DOWNLOAD_URL);
    Kernel kernel = scope.get("SunshowerKernel");
    try {

      fireDownloadInitiated(downloadUrl, kernel);
      scope.set(DOWNLOAD_URL, downloadUrl);
      Path moduleDirectory = scope.get(TARGET_DIRECTORY);
      downloadModule(downloadUrl, moduleDirectory, scope);
      fireDownloadCompleted(downloadUrl, kernel);
    } catch (Exception ex) {
      fireDownloadFailed(downloadUrl, kernel, ex);
      throw new TaskException(ex, TaskStatus.UNRECOVERABLE);
    }
    return null;
  }

  @Override
  public void onTransfer(ReadableByteChannel channel, double progress) {}

  @Override
  public void onComplete(ReadableByteChannel channel) {
    targetFile.remove();
  }

  @Override
  public void onError(ReadableByteChannel channel, Exception ex) {}

  @Override
  public void onCancel(ReadableByteChannel channel) {}

  private void downloadModule(URL downloadUrl, Path moduleDirectory, Scope context)
      throws Exception {
    val targetDirectory = getTargetDirectory(moduleDirectory, context);
    val targetFile = new File(targetDirectory, Files.getFileName(downloadUrl));
    this.targetFile.set(targetFile);
    log.log(Level.INFO, "module.download.beforestart", new Object[] {downloadUrl, targetDirectory});
    doTransfer(downloadUrl, targetFile, context);
  }

  private File getTargetDirectory(Path moduleDirectory, Scope context) {
    val targetDirectory = moduleDirectory.toFile();
    if (!targetDirectory.exists()) {
      log.log(Level.INFO, "module.download.targetdir.creating", targetDirectory);
      if (!(targetDirectory.exists() || targetDirectory.mkdirs())) {
        throw new TaskException(
            String.format(
                "Error creating directory '%s'.  Directory does not exist and cannot be created",
                targetDirectory),
            TaskStatus.UNRECOVERABLE);
      }
    }
    return targetDirectory;
  }

  @SuppressWarnings("PMD.UnusedPrivateMethod")
  private void doTransfer(URL downloadUrl, File targetFile, Scope context) throws Exception {
    val transfer = MonitorableChannels.transfer(downloadUrl, targetFile);
    transfer.addListener(this);
    transfer.call();
    context.set(DOWNLOADED_FILE, targetFile);
  }

  private void fireDownloadFailed(URL downloadUrl, Kernel kernel, Exception ex) {
    log.log(
        Level.SEVERE,
        "Error downloading module from '%s'.  Reason: %s".formatted(downloadUrl, ex),
        ex);
    kernel.dispatchEvent(
        ModulePhaseEvents.MODULE_DOWNLOAD_FAILED,
        KernelEvents.create(downloadUrl, StatusType.FAILED.unresolvable(ex)));
  }

  private void fireDownloadCompleted(URL downloadUrl, Kernel kernel) {
    kernel.dispatchEvent(
        ModulePhaseEvents.MODULE_DOWNLOAD_COMPLETED,
        KernelEvents.create(
            downloadUrl, StatusType.PROGRESSING.resolvable(downloadUrl.toExternalForm())));
  }

  private void fireDownloadInitiated(URL downloadUrl, Kernel kernel) {
    kernel.dispatchEvent(
        ModulePhaseEvents.MODULE_DOWNLOAD_INITIATED,
        KernelEvents.create(
            downloadUrl, StatusType.PROGRESSING.resolvable("beginning module download")));
  }

  @AllArgsConstructor
  public static class TransferData {

    final double progress;
  }
}
