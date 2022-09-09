package com.knubisoft.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
public class Node {
    private final int pixel;
    private final int x;
    private final int y;

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Node node = (Node) o;

        if (this.getPixel() != node.getPixel()) {
            return false;
        }

        if (this.getX() != node.getX()) {
            return false;
        }

        return this.getY() != 0 ? this.getY() == (node.getY()) : node.getY() == 0;
    }
}
