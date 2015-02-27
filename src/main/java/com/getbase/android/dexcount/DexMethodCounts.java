package com.getbase.android.dexcount;

import static java.lang.System.out;

import com.android.dexdeps.DexData;
import com.android.dexdeps.MethodRef;
import com.android.dexdeps.Output;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeTraverser;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DexMethodCounts {

  private static final ImmutableSet<String> COMMON_PACKAGE_PREFIXES = ImmutableSet.of(
      "com",
      "com.github",
      "com.squareup",
      "com.futuresimple",
      "com.getbase",
      "com.getbase.android",
      "com.google",
      "com.commonsware",
      "com.commonsware.cwac",
      "org",
      "org.chalup",
      "org.apache",
      "org.joda",
      "net",
      "net.sourceforge"
  );

  private static class Node {
    int methods;
    String name;
    Set<Node> children = new HashSet<>();

    public Node(String name) {
      this.name = name;
    }

    public Node() {
      this("<root>");
    }

    public String getName() {
      return name;
    }
  }

  private static String printTree(Node node) {
    return printTree(node, new StringBuilder(), 0).toString();
  }

  private static StringBuilder printTree(Node node, StringBuilder stringBuilder, int indentationLevel) {
    stringBuilder
        .append(Strings.repeat(" ", indentationLevel * 4))
        .append(node.name)
        .append(": ")
        .append(node.methods)
        .append("\n");

    node.children.stream()
        .sorted(Comparator.comparing(Node::getName))
        .forEach(childNode -> printTree(childNode, stringBuilder, indentationLevel + 1));

    return stringBuilder;
  }

  public static Node buildMethodsTree(List<DexData> dexData) {
    Node rootNode = new Node();

    for (DexData data : dexData) {
      for (MethodRef methodRef : data.getMethodRefs()) {
        Node node = rootNode;

        for (String packageNamePiece : Output.packageNameOnly(methodRef.getDeclClassName()).split("\\.")) {
          Node childNode = node.children
              .stream()
              .filter(child -> child.name.equals(packageNamePiece))
              .findFirst()
              .orElseGet(() -> new Node(packageNamePiece));

          node.children.add(childNode);
          node = childNode;
        }

        ++node.methods;
      }
    }

    flatten(rootNode, node -> COMMON_PACKAGE_PREFIXES.contains(node.name));

    Predicate<Node> noMethods = (Node node) -> node.methods == 0;
    Predicate<Node> withSingleChild = (Node node) -> node.children.size() == 1;
    Predicate<Node> withBranchChildren = (Node node) -> node.children.stream().allMatch(childNode -> childNode.methods == 0);

    flatten(rootNode, noMethods.and(withSingleChild.or(withBranchChildren)));

    groupMethodCounts(rootNode);

    return rootNode;
  }

  private static void groupMethodCounts(Node rootNode) {
    treeTraverserOf((Node node) -> node.children)
        .postOrderTraversal(rootNode)
        .toList()
        .forEach(node -> node.methods += node.children.stream().collect(Collectors.summingInt(childNode -> childNode.methods)));
  }

  private static <T> TreeTraverser<T> treeTraverserOf(Function<T, Iterable<T>> childrenFunction) {
    return new TreeTraverser<T>() {
      @Override
      public Iterable<T> children(T root) {
        return childrenFunction.apply(root);
      }
    };
  }

  public static void printMethodUsage(Node rootNode) {
    out.println(printTree(rootNode));
    out.println();
    out.println("Method usage by top level packages:");
    rootNode.children
        .stream()
        .sorted(Comparator.comparing(Node::getName))
        .forEach(topLevelNode -> out.println(topLevelNode.name + ": " + topLevelNode.methods));
    out.println();
    out.println("Total method count: " + rootNode.methods);
  }

  private static void flatten(Node node, Predicate<Node> nodePredicate) {
    node.children = node
        .children
        .stream()
        .flatMap(childNode -> doFlatten(childNode, nodePredicate))
        .collect(Collectors.toCollection(HashSet::new));
  }

  private static Stream<Node> doFlatten(Node node, Predicate<Node> nodePredicate) {
    if (nodePredicate.test(node)) {
      node.children.forEach(child -> child.name = node.name + "." + child.name);
      return node.children.stream().flatMap(childNode -> doFlatten(childNode, nodePredicate));
    } else {
      return Stream.of(node);
    }
  }
}
