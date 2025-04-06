import java.util.ArrayList;

/**
 * This class represents a rectangle in a spatial database. Each rectangle is defined by a set of coordinates and a child
 * pointer.
 *
 */
public class Rectangle {
    private final ArrayList<Double> coordinates;
    private int childPointer;

    /**
     * Constructs a new rectangle with the given coordinates and child pointer.
     *
     * @param coordinates the coordinates of the rectangle
     * @param childPointer the child pointer of the rectangle
     */
    Rectangle(ArrayList<Double> coordinates, int childPointer) {
        this.coordinates = new ArrayList<>(coordinates);
        this.childPointer = childPointer;
    }

    /**
     * Constructs a new rectangle with the given coordinates.
     *
     * @param coordinates the coordinates of the rectangle
     */
    Rectangle(ArrayList<Double> coordinates) {
        this.coordinates = new ArrayList<>(coordinates);
    }

    /**
     * Sorts a list of rectangles by their coordinates.
     *
     * @param a the list of rectangles to sort
     * @param b 0 to sort by latitude, 1 to sort by longitude
     * @param c 0 to sort by lower boundary, 1 to sort by higher boundary
     * @param d the list of child pointers corresponding to the rectangles
     */
    public static void tempSort(ArrayList<Double[][]> a, int b, int c, ArrayList<Integer> d)
    {
        for (int i = 0; i < a.size(); i++) {
            for (int j = a.size() - 1; j > i; j--) {
                if (b == 0) {
                    if (c == 0) {
                        if (a.get(i)[0][0] > a.get(j)[0][0]) {
                            Double[][] tmp = a.get(i);
                            a.set(i, a.get(j));
                            a.set(j, tmp);
                            Integer tmp1 = d.get(i);
                            d.set(i, d.get(j));
                            d.set(j, tmp1);
                        }
                    } else {
                        if (a.get(i)[1][0] > a.get(j)[1][0]) {
                            Double[][] tmp = a.get(i);
                            a.set(i, a.get(j));
                            a.set(j, tmp);
                            Integer tmp1 = d.get(i);
                            d.set(i, d.get(j));
                            d.set(j, tmp1);
                        }
                    }
                } else {
                    if (c == 0) {
                        if (a.get(i)[0][1] > a.get(j)[0][1]) {
                            Double[][] tmp = a.get(i);
                            a.set(i, a.get(j));
                            a.set(j, tmp);
                            Integer tmp1 = d.get(i);
                            d.set(i, d.get(j));
                            d.set(j, tmp1);
                        }
                    } else {
                        if (a.get(i)[1][1] > a.get(j)[1][1]) {
                            Double[][] tmp = a.get(i);
                            a.set(i, a.get(j));
                            a.set(j, tmp);
                            Integer tmp1 = d.get(i);
                            d.set(i, d.get(j));
                            d.set(j, tmp1);
                        }
                    }
                }
            }
        }
    }

    public ArrayList<Double> getCoordinates() {
        return this.coordinates;
    }

    public int getChildPointer() {
        return childPointer;
    }

}
