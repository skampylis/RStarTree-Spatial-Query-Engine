import java.util.*;

/**
 * This class represents a range query. It uses a priority queue to keep track of the pointers.
 */
public class RangeQuery {
    protected Rectangle rangeRectangle;
    protected final ArrayList<Record> result;
    private final Queue<Integer> pointers;
    protected final int dimensions;

    /**
     * Constructs a new RangeQuery with the specified rectangle.
     *
     * @param rectangle the rectangle
     */
    RangeQuery(Rectangle rectangle) {
        this.dimensions = FileHandler.getDimensions();
        this.rangeRectangle = rectangle;
        result = new ArrayList<>();
        pointers = new LinkedList<>();
        rangeQuery();
    }

    /**
     * Executes the range query. It checks each record in the data file to see if its coordinates are within the range
     * rectangle. If a record's coordinates are within the range rectangle, it is added to the result.
     */
    protected void rangeQuery() {
        try {
            if (FileHandler.getNoOfIndexfileBlocks() > 1) {
                pointers.add(1);
                int blockId, level;
                while (!pointers.isEmpty()) {
                    blockId = pointers.peek();
                    level = FileHandler.getMetaDataOfRectangle(blockId).get(0);
                    if (level != FileHandler.getLeafLevel()) {
                        ArrayList<Rectangle> rectangles = FileHandler.getRectangleEntries(blockId);
                        for (Rectangle rectangle : rectangles) {
                            for (int i = 0; i < dimensions; i++) {
                                if (rangeRectangle.getCoordinates().get(i) > rectangle.getCoordinates().get(i + dimensions) || rangeRectangle.getCoordinates().get(i + dimensions) < rectangle.getCoordinates().get(i)) {
                                    break;
                                }
                                if (i == dimensions - 1) {
                                    pointers.add(rectangle.getChildPointer());
                                }
                            }
                        }
                    } else {
                        ArrayList<Record> records = FileHandler.getRecords(blockId);
                        for (Record record : records) {
                            if (record.getLAT() >= rangeRectangle.getCoordinates().get(0) && record.getLAT() <= rangeRectangle.getCoordinates().get(dimensions) && record.getLON() >= rangeRectangle.getCoordinates().get(1) && record.getLON() <= rangeRectangle.getCoordinates().get(1 + dimensions)) {
                                result.add(record);
                            }
                        }
                    }
                    pointers.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the entries found in the given range.
     */
    void print() {
        System.out.println("\nThere are " + result.size() + " entries found in the given range:\n");
        for (Record record : result) {
            System.out.print("LAT: " + record.getLAT() + ", LON: " + record.getLON() + ", Datafile block: " + FileHandler.getRecord(record.getId()).getRecordLocation().getBlock() + ", Block slot: " + FileHandler.getRecord(record.getId()).getRecordLocation().getSlot());
            if (record.getName() != null && !record.getName().equals("")) {
                System.out.print(", Name: " + record.getName());
            }
            if (record.getNodeId() != 0) {
                System.out.print(", Node ID: " + record.getNodeId());
            }
            System.out.println();
        }
    }

}