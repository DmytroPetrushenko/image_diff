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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearcherImageDiff {
    private static final String PATH_OUT_A = "src/main/resources/test1out.jpg";
    private static final String PATH_OUT_B = "src/main/resources/test2out.jpg";
    private final Set<Set<Node>> diffNodeSet = new HashSet<>();
    private final Logger logger = LoggerFactory.getLogger(SearcherImageDiff.class);
    @SneakyThrows
    public void launchFinderImageDiff(String nameA, String nameB) {
        BufferedImage[] bufferedImages = createImageIo(nameA, nameB);
        RTree<Node, Geometry> tree = createRtree(bufferedImages[0], bufferedImages[1]);
        workWithObservable(tree);
        Set<Node> rectangleSet = createRectangleWithRedEdge();
        createImages(bufferedImages[0], bufferedImages[1], rectangleSet);
    }

    @SneakyThrows
    private void createImages(BufferedImage imageA, BufferedImage imageB, Set<Node> rectangleSet) {
        for (Node node : rectangleSet) {
            if (imageA.getWidth() >= node.getX() && imageA.getHeight() >= node.getY()) {
                try {
                    imageA.setRGB(node.getX(), node.getY(), node.getPixel());
                    imageB.setRGB(node.getX(), node.getY(), node.getPixel());
                } catch (ArrayIndexOutOfBoundsException e) {
                        logger.info("max width: " + imageA.getWidth() + ", max height: " + imageA.getHeight()
                            + "node x: " + node.getX()+ "node y: " + node.getY());
                }
            }
        }
        File fileA = new File(PATH_OUT_A);
        ImageIO.write(imageA, "jpg", fileA);
        File fileB = new File(PATH_OUT_B);
        ImageIO.write(imageB, "jpg", fileB);
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
                if (imageA.getRGB(x, y) != imageB.getRGB(x, y)) {
                    Node node = new Node(imageA.getRGB(x, y), x, y);
                    tree = tree.add(node, Geometries.point(x, y));
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
        tree.nearest(Geometries.point(currentNode.getX(), currentNode.getY()), 50, tree.size())
                .map(Entry::value)
                .forEach(nodeList::add);
        return nodeList;
    }
}
