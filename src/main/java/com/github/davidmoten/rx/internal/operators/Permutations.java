package com.github.davidmoten.rx.internal.operators;

import java.util.Iterator;
import java.util.List;

public final class Permutations {

    private Permutations() {
        // prevent instantiation
    }

    public static <T> Iterable<Swap<T>> iterable(final List<T> list) {
        return new Iterable<Swap<T>>() {
            @Override
            public Iterator<Swap<T>> iterator() {
                return new PermutationsSwapIterator<T>(list);
            }
        };
    }

    public static <T> Iterator<Swap<T>> iterator(List<T> list) {
        return new PermutationsSwapIterator<T>(list);
    }

    private static class PermutationsSwapIterator<T> implements Iterator<Swap<T>> {

        /**
         * After
         * http://www.cut-the-knot.org/Curriculum/Combinatorics/JohnsonTrotter.
         * shtml
         * 
         */
        private final T[] values;
        private final DirectedReference[] references;

        @SuppressWarnings("unchecked")
        public PermutationsSwapIterator(List<T> list) {
            this.values = (T[]) list.toArray();
            this.references = new DirectedReference[list.size()];
            for (int i = 0; i < this.references.length; i++) {
                this.references[i] = new DirectedReference(-1, i);
            }
        }

        private boolean isMobile(int index) {
            if (index == 0 && references[index].direction == -1) {
                return false;
            } else if (index == references.length - 1 && references[index].direction == 1) {
                return false;
            } else if (references[index
                    + references[index].direction].reference > references[index].reference) {
                return false;
            } else {
                return true;
            }
        }

        @Override
        public boolean hasNext() {
            for (int i = 0; i < references.length; i++) {
                if (isMobile(i)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Swap<T> next() {
            // find the largest mobile reference "chosen"
            int chosen = Integer.MIN_VALUE;
            int chosenIndex = -1;
            for (int i = 0; i < references.length; i++) {
                if (isMobile(i) && references[i].reference > chosen) {
                    chosen = references[i].reference;
                    chosenIndex = i;
                }
            }

            // swaps it in the indicated direction
            int neighbourIndex = chosenIndex + references[chosenIndex].direction;
            Swap<T> swap = new Swap<T>(values[references[chosenIndex].reference],
                    values[references[neighbourIndex].reference]);

            DirectedReference tmp = references[chosenIndex];
            references[chosenIndex] = references[neighbourIndex];
            references[neighbourIndex] = tmp;

            // reverse the direction of all references larger than "chosen"
            for (int i = 0; i < references.length; i++) {
                if (references[i].reference > chosen) {
                    references[i].reverse();
                }
            }

            return swap;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("cannot remove from permutations");
        }

    }

    public static class Swap<T> {
        private final T left;
        private final T right;

        Swap(T left, T right) {
            this.left = left;
            this.right = right;
        }

        public T left() {
            return left;
        }

        public T right() {
            return right;
        }

        @Override
        public String toString() {
            return "Swap [left=" + left + ", right=" + right + "]";
        }

    }

    private static class DirectedReference {
        private final int reference;
        private int direction;

        DirectedReference(int direction, int reference) {
            this.direction = direction;
            this.reference = reference;
        }

        public void reverse() {
            this.direction = -this.direction;
        }

        @Override
        public String toString() {
            String val = String.valueOf(reference);
            String result = direction == -1 ? "<" + val : val + ">";
            return result + String.valueOf(reference);
        }
    }

}