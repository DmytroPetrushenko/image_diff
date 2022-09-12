package com.knubisoft.service;

import com.github.davidmoten.rtree.Entry;
import com.github.davidmoten.rtree.RTree;
import com.github.davidmoten.rtree.geometry.Geometries;
import com.github.davidmoten.rtree.geometry.Geometry;
import com.github.davidmoten.rtree.geometry.Rectangle;
import com.knubisoft.model.Node;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;

public class SearcherImageDiff {
    private static final String PATH_OUT_A = "src/main/resources/test1out.jpg";
    private static final String PATH_OUT_B = "src/main/resources/test2out.jpg";
    private final List<Node> rectanglesBorders = new ArrayList<>();
    private final List<Rectangle> rectangles = new ArrayList<>();
    private RTree<Node, Geometry> groupTree;

    @SneakyThrows
    public void launchFinderImageDiff(String nameA, String nameB) {
        BufferedImage[] bufferedImages = createImageIo(nameA, nameB);
        RTree<Node, Geometry> tree = createRtree(bufferedImages[0], bufferedImages[1]);
        creatRectanglesFromTree(tree);
        createImages(bufferedImages[0], bufferedImages[1], rectanglesBorders);
    }

    @SneakyThrows
    private void createImages(BufferedImage imageA,
                              BufferedImage imageB,
                              List<Node> rectanglesBorders) {
        for (Node node : rectanglesBorders) {
            imageA.setRGB(node.getAxeX(), node.getAxeY(), node.getPixel());
            imageB.setRGB(node.getAxeX(), node.getAxeY(), node.getPixel());
        }
        File fileA = new File(PATH_OUT_A);
        ImageIO.write(imageA, "jpg", fileA);
        File fileB = new File(PATH_OUT_B);
        ImageIO.write(imageB, "jpg", fileB);
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
        for (int x = 0; x < imageA.getWidth(); x++) {
            for (int y = 0; y < imageA.getHeight(); y++) {
                if (imageA.getRGB(x, y) != imageB.getRGB(x, y)) {
                    Node node = new Node(imageA.getRGB(x, y), x, y);
                    tree = tree.add(node, Geometries.point(x, y));
                }
            }
        }
        return tree;
    }

    private void creatRectanglesFromTree(RTree<Node, Geometry> tree) {
        tree.entries()
                .forEach(geometryEntry -> getNearest(geometryEntry,tree));
    }

    private void getNearest(Entry<Node, Geometry> entry, RTree<Node, Geometry> tree) {
        Node currentNode = entry.value();

        if (isInOtherRectangles(currentNode)) {
            return;
        }

        groupTree = RTree.create();
        tree.nearest(Geometries
                        .point(currentNode.getAxeX(), currentNode.getAxeY()), 150, tree.size())
                .forEach(this::addEntryInTree);

        Optional<Rectangle> mbrOptional;
        if ((mbrOptional = groupTree.mbr()).isEmpty()) {
            return;
        }
        Rectangle rectangle = mbrOptional.get();

        rectangles.add(rectangle);
        rectanglesBorders.addAll(createRectangle(rectangle.x1(), rectangle.x2(),
                rectangle.y1(), rectangle.y2()));
    }

    private boolean isInOtherRectangles(Node currentNode) {
        Optional<Rectangle> optional = rectangles.stream()
                .filter(rectangle -> isInRectangles(rectangle, currentNode))
                .findFirst();
        return optional.isPresent();
    }

    private boolean isInRectangles(Rectangle rectangle, Node currentNode) {
        int x = currentNode.getAxeX();
        int y = currentNode.getAxeY();
        return rectangle.x1() <= x && x <= rectangle.x2()
                && rectangle.y1() <= y && y <= rectangle.y2();
    }

    private List<Node> createRectangle(double x1, double x2, double y1, double y2) {
        List<Node> horizontalEdges = Stream.of(y1, y2)
                .map(y -> createHorizontalEdge(x1, x2, y))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        List<Node> verticalEdges = Stream.of(x1, x2)
                .map(x -> createVerticalEdge(y1, y2, x))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return Stream.of(horizontalEdges, verticalEdges)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<Node> createHorizontalEdge(double x1, double x2, double y) {
        return IntStream.range((int) x1, (int) x2 + 1)
                .boxed()
                .map(x -> new Node(255, x, (int) y))
                .collect(Collectors.toList());
    }

    private List<Node> createVerticalEdge(double y1, double y2, double x) {
        return IntStream.range((int) y1, (int) y2 + 1)
                .boxed()
                .map(y -> new Node(255, (int) x, y))
                .collect(Collectors.toList());
    }

    private void addEntryInTree(Entry<Node, Geometry> geometryEntry) {
        groupTree = groupTree.add(geometryEntry.value(), geometryEntry.geometry());
    }
}
