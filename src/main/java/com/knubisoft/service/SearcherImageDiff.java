package com.knubisoft.service;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.knubisoft.model.Node;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.SneakyThrows;

public class SearcherImageDiff {
    private final Set<Set<Node>> diffNodeSet = new HashSet<>();
    private Set<Node> setNodeA = new LinkedHashSet<>();
    private Set<Node> setNodeB = new LinkedHashSet<>();
    @SneakyThrows
    public void launchFinderImageDiff(String nameA, String nameB) {
        BufferedImage[] bufferedImages = createImageIo(nameA, nameB);
        RTree<Node, Geometry> tree = createRtree(bufferedImages[0], bufferedImages[1]);
        workWithObservable(tree);
        Set<Node> redEdges = createRectangleWithRedEdge();
        redEdges.forEach(this::putRedNodeToSetNodes);
        createImages();
    }

    private void createImages() {};

    private void putRedNodeToSetNodes(Node node) {
        setNodeA = setNodeA.stream()
                .filter(nodeA -> nodeA.getX() != node.getX() && nodeA.getY() != node.getY())
                .collect(Collectors.toSet());
        setNodeA.add(node);
    }

    private Set<Node> createRectangleWithRedEdge() {
         return diffNodeSet.stream()
                .map(this::creatRedEdge)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private Set<Node> creatRedEdge(Set<Node> set) {
        Integer maxX = set.stream().map(Node::getX).max(Comparator.naturalOrder()).get();
        Integer minX = set.stream().map(Node::getX).min(Comparator.naturalOrder()).get();
        Integer maxY = set.stream().map(Node::getY).max(Comparator.naturalOrder()).get();
        Integer minY = set.stream().map(Node::getY).min(Comparator.naturalOrder()).get();
        List<Node> edgeLeft = createEdge(minX, minY, maxY);
        List<Node> edgeRight = createEdge(maxX, minY, maxY);
        List<Node> edgeBottom = createEdge(minY, minX, maxX);
        List<Node> edgeTop = createEdge(maxY, minX, maxX);
        return Stream.of(edgeLeft, edgeRight, edgeTop, edgeBottom)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private List<Node> createEdge(Integer line, Integer minPoint, Integer maxPoint) {
        return IntStream.range(minPoint, maxPoint)
                .boxed()
                .map(num -> new Node(255, line, num))
                .collect(Collectors.toList());
    }

    @SneakyThrows
    private BufferedImage[] createImageIo(String nameA, String nameB) {
        File fileA = Paths.get(nameA).toFile();
        File fileB = Paths.get(nameB).toFile();
        BufferedImage imageA = ImageIO.read(fileA);
        BufferedImage imageB = ImageIO.read(fileB);
        return new BufferedImage[]{imageA, imageB};
    }

    private RTree<Node, Geometry> createRtree(BufferedImage imageA, BufferedImage imageB) {
        RTree<Node, Geometry> tree = RTree.maxChildren(6).create();

        if (imageA.getHeight() != imageB.getHeight() || imageA.getWidth() != imageB.getWidth()) {
            throw new RuntimeException("Images dimensions are different!");
        }
        for (int x = 0; x <imageA.getWidth(); x++) {
            for (int y = 0; y < imageA.getHeight(); y++) {
                Node nodeA = new Node(imageA.getRGB(x, y), x, y);
                Node nodeB = new Node(imageB.getRGB(x, y), x, y);
                setNodeA.add(nodeA);
                setNodeB.add(nodeB);
                if (imageA.getRGB(x, y) != imageB.getRGB(x, y)) {
                    tree = tree.add(nodeA, Geometries.point(x, y));
                }
            }
        }
        return tree;
    }

    private void workWithObservable(RTree<Node, Geometry> tree) {
        tree.entries().map(entry -> getNearest(entry, tree))
                .filter(Objects::nonNull)
                .forEach(diffNodeSet::add);
    }

    private Set<Node> getNearest(Entry<Node, Geometry> entry, RTree<Node, Geometry> tree) {
        final Set<Node> nodeList = new HashSet<>();
        Node currentNode = entry.value();
        Optional<Node> optional = diffNodeSet.stream()
                .flatMap(Collection::stream)
                .filter(node -> node.equals(currentNode))
                .findFirst();
        if (optional.isPresent()) {
            return null;
        }
        tree.nearest(Geometries.point(currentNode.getX(), currentNode.getY()), 5, tree.size())
                .map(Entry::value)
                .forEach(nodeList::add);
        return nodeList;
    }
}
