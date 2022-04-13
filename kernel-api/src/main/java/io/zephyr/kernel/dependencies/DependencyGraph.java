package io.zephyr.kernel.dependencies;

import io.sunshower.gyre.Component;
import io.sunshower.gyre.DirectedGraph;
import io.sunshower.gyre.Graph;
import io.sunshower.gyre.Partition;
import io.zephyr.kernel.Coordinate;
import io.zephyr.kernel.Module;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * this class is generally not required to be thread-safe. Consumers should protect access in
 * concurrent environments
 */
public interface DependencyGraph extends Iterable<Module> {

  Set<UnsatisfiedDependencySet> add(Module a);

  Graph<DirectedGraph.Edge<Coordinate>, Coordinate> getGraph();

  Module latest(Coordinate coordinate);

  Module earliest(Coordinate coordinate);

  Module firstOfLevel(Coordinate coordinate, Comparator<Module> comparator);

  List<Module> getModules(Coordinate coordinate);

  @Getter
  @AllArgsConstructor
  final class UnsatisfiedDependencySet {
    final Coordinate source;
    final Set<Coordinate> dependencies;

    public boolean isSatisfied() {
      return dependencies.isEmpty();
    }

    @Override
    public String toString() {
      return source.toCanonicalForm() + " depends on " + dependencies;
    }
  }

  @Getter
  @AllArgsConstructor
  final class CyclicDependencySet {
    final Coordinate source;
    final Component<DirectedGraph.Edge<Coordinate>, Coordinate> cycles;
  }

  /** @return the size of this graph */
  int size();

  /**
   * @param module
   * @return an unsatisified dependency set
   */
  @NonNull
  UnsatisfiedDependencySet getUnresolvedDependencies(Module module);

  @NonNull
  Set<UnsatisfiedDependencySet> resolveDependencies(Collection<Module> modules);

  @NonNull
  Set<UnsatisfiedDependencySet> getUnresolvedDependencies(Collection<Module> modules);

  @NonNull
  Set<UnsatisfiedDependencySet> addAll(Collection<Module> modules);
  /**
   * @param module the module to remove from this dependency graph. This should only be called if
   *     getDependants() returns an empty set
   */
  void remove(Module module);

  /**
   * @param coordinate the coordinate to resolve
   * @return the module (if it exists).
   */
  Module get(Coordinate coordinate);

  /**
   * @param coordinate the coordinate for which to compute dependent modules
   * @return the set of modules which have the module at <code>coordinate</code> as a dependency
   */
  Collection<Module> getDependents(Coordinate coordinate);

  /**
   * @param coordinate the coordinate to get all of the dependencies for
   * @return the set of dependencies for that coordinate
   */
  Set<Module> getDependencies(Coordinate coordinate);

  /**
   * @param coordinate the coordinate to check for membership in this graph
   * @return true if coordinate exists in this graph (i.e. if and only if add() has been called on
   *     the module with that coordinate)
   */
  boolean contains(Coordinate coordinate);

  Partition<DirectedGraph.Edge<Coordinate>, Coordinate> computeCycles();

  DependencyGraph clone();
}
