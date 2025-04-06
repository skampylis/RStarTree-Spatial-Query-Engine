import java.util.ArrayList;

/**
 * This class represents a record in a spatial database. Each record has a latitude, a longitude, an ID, a name, a node
 * ID, and a list of coordinates. The record also has a location, which is represented by a block number and a slot
 * number.
 */
public class Record {
    private double LAT;
    private double LON;
    private RecordLocation recordLocation;
    private int id;
    private String name;
    private long nodeId = 0;
    private ArrayList<Double> coords;


    /**
     * Constructs a new record with the given latitude, longitude, block, slot, ID, and name.
     *
     * @param LAT    the latitude of the record
     * @param LON    the longitude of the record
     * @param block  the block number of the record
     * @param slot   the slot number of the record
     * @param id     the ID of the record
     * @param nodeId the node ID of the record
     */
    public Record(double LAT, double LON, int block, long slot, int id, long nodeId) {
        this.LAT = LAT;
        this.LON = LON;
        recordLocation = new RecordLocation(block, slot);
        this.id = id;
        this.nodeId = nodeId;
    }


    /**
     * Constructs a new record with the given latitude, longitude, block, slot, ID, and name.
     *
     * @param LAT the latitude of the record
     * @param LON the longitude of the record
     * @param id  the ID of the record
     */
    public Record(double LAT, double LON, int id) {
        this.LAT = LAT;
        this.LON = LON;
        this.id = id;
    }

    /**
     * Constructs a new record with the given latitude, longitude, block, slot, ID, and name.
     *
     * @param LAT    the latitude of the record
     * @param LON    the longitude of the record
     * @param block  the block number of the record
     * @param slot   the slot number of the record
     * @param id     the ID of the record
     * @param name   the name of the record
     * @param nodeId the node ID of the record
     */
    public Record(double LAT, double LON, int block, long slot, int id, String name, long nodeId) {
        this.LAT = LAT;
        this.LON = LON;
        recordLocation = new RecordLocation(block, slot);
        this.id = id;
        this.name = name;
        this.nodeId = nodeId;
    }

    /**
     * Constructs a new record with the given coordinates, node ID, and name. If the name is an empty string, the name
     * of the record is not set.
     *
     * @param coords the coordinates of the record
     * @param nodeId the node ID of the record
     * @param name   the name of the record
     */
    public Record(ArrayList<Double> coords, long nodeId, String name) {
        this.nodeId = nodeId;
        this.coords = new ArrayList<>(coords);
        if (!name.equals("")) {
            this.name = name;
        }
    }

    /**
     * Creates a new record that is a copy of this record. The new record has the same latitude, longitude, ID, and
     * coordinates as this record.
     *
     * @return the new record
     */
    public Record copyRecord() {
        Record new_rec = new Record(this.getLAT(), this.getLON(), this.getId());
        return new_rec;
    }

    public RecordLocation getRecordLocation() {
        return recordLocation;
    }

    public double getLAT() {
        return LAT;
    }

    public double getLON() {
        return LON;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setLAT(double LAT) {
        this.LAT = LAT;
    }

    public void setLON(double LON) {
        this.LON = LON;
    }

    public long getNodeId() {
        return nodeId;
    }

    public ArrayList<Double> getCoords() {
        return coords;
    }

    /**
     * Sorts an array of records by latitude or longitude.
     *
     * @param a the array of records to sort
     * @param b 0 to sort by latitude, 1 to sort by longitude
     */
    public static void tempSort(ArrayList<Record> a, int b) {
        for (int i = 0; i < a.size(); i++) {
            for (int j = a.size() - 1; j > i; j--) {
                if (b == 0) {
                    if (a.get(i).getLAT() > a.get(j).getLAT()) {
                        Record tmp = a.get(i);
                        a.set(i, a.get(j));
                        a.set(j, tmp);
                    }
                } else {
                    if (a.get(i).getLON() > a.get(j).getLON()) {
                        Record tmp = a.get(i);
                        a.set(i, a.get(j));
                        a.set(j, tmp);
                    }
                }
            }
        }
    }

}