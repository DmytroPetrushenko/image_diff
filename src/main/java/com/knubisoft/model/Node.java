package com.knubisoft.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Node {
    private final int pixel;
    private final int axeX;
    private final int axeY;

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

        if (this.getAxeX() != node.getAxeX()) {
            return false;
        }

        return this.getAxeY() != 0 ? this.getAxeY() == (node.getAxeY()) : node.getAxeY() == 0;
    }
}
