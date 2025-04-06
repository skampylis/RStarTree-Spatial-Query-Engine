import java.util.*;

/**
 * This class represents a skyline query in a spatial database. The query is performed on a set of skyline pairs and a
 * result set is produced. The result set is sorted in descending order by latitude.
 *
 */
public class SkylineQuery {
    private final PriorityQueue<SkylinePair> pointers;
    private final ArrayList<Record> result;
    private final int dimensions;

    /**
     * Constructs a new skyline query. The dimensions of the query are determined by the dimensions of the file handler.
     * The query is immediately performed upon construction.
     */
    SkylineQuery() {
        this.dimensions = FileHandler.getDimensions();
        pointers = new PriorityQueue<>((Comparator.comparingDouble(o -> Math.abs(o.getCoordinates().get(0)) + Math.abs(o.getCoordinates().get(1)))));
        result = new ArrayList<>();
        this.skylineQuery();
    }

    /**
     * Performs the skyline query. The query is performed by checking each record in the database and comparing it to the
     * current result set. If a record dominates any of the records in the result set, the dominated records are removed
     * from the result set and the dominating record is added to the result set. The result set is always sorted in
     * descending order by latitude.
     */
    private void skylineQuery() {
        try {
            if (FileHandler.getNoOfIndexfileBlocks() > 1) {
                pointers.add(new SkylinePair(1, new ArrayList<>()));
                int blockId, level;
                while (!pointers.isEmpty()) {
                    blockId = pointers.peek().getId();
                    pointers.remove();
                    level = FileHandler.getMetaDataOfRectangle(blockId).get(0);
                    if (level != FileHandler.getLeafLevel()) {
                        ArrayList<Rectangle> rectangles = FileHandler.getRectangleEntries(blockId);
                        outerLoop:
                        for (Rectangle rectangle : rectangles) {
                            if (!result.isEmpty()) {
                                for (Record record : result) {
                                    if (record.getLON() <= rectangle.getCoordinates().get(0) && record.getLON() <= rectangle.getCoordinates().get(dimensions) && record.getLON() <= rectangle.getCoordinates().get(1) && record.getLON() <= rectangle.getCoordinates().get(1 + dimensions)) {
                                        continue outerLoop;
                                    }
                                }
                            }
                            pointers.add(new SkylinePair(rectangle.getChildPointer(), rectangle.getCoordinates()));
                        }
                    } else {
                        ArrayList<Record> records = FileHandler.getRecords(blockId);
                        for (Record record : records) {
                            boolean condition = false;
                            if (result.isEmpty()) {
                                condition = true;
                            } else {
                                Iterator<Record> iterator = result.iterator();
                                while (iterator.hasNext()) {
                                    Record record1 = iterator.next();
                                    if (record1.getLON() <= record.getLON() && record1.getLAT() <= record.getLAT()) {
                                        condition = false;
                                        break;
                                    } else if (record.getLON() < record1.getLON() && record.getLAT() < record1.getLAT()) {
                                        condition = true;
                                        iterator.remove();
                                    } else {
                                        condition = true;
                                    }
                                }
                            }
                            if (condition) {
                                result.add(record);
                                result.sort((r1, r2) -> Double.compare(r2.getLAT(), r1.getLAT()));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the result set of the skyline query. Each record in the result set is printed on a separate line. The
     * latitude, longitude, datafile block, block slot, name (if present), and node ID (if present) of each record are
     * printed.
     */
    void print() {
        System.out.println("There are " + result.size() + " entries in the skyline: ");
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
