package io.zephyr.kernel;

public interface Coordinate extends Comparable<Coordinate> {

  boolean isResolved();

  String getName();

  String getGroup();

  Version getVersion();

  default String toCanonicalForm() {
    return String.format("%s:%s:%s", getGroup(), getName(), getVersion());
  }

  boolean satisfies(String range);
}
