import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * The BottomUp Class represents the Bottom Up construction of an R* tree data structure.
 */
public class BottomUp {
    Queue<Record> subset_recs;
    ArrayList<Record> records;
    int leafLevelFINAL;
    int blockID;

    /**
     * Constructor for the BottomUp class. It initializes several class variables.
     */
    public BottomUp() {
        // Get records from the FileHandler and assign them to the records variable
        this.records = FileHandler.getRecords();
        // Initialize subset_recs as a new LinkedList
        this.subset_recs = new LinkedList<>();
        // Calculate the levels of the tree based on the size of the records and assign the result to leafLevelFINAL
        this.leafLevelFINAL = getLevelsOfTree(records.size());
        // Initialize blockID to 2
        this.blockID = 2;
    }

    /**
     * This method constructs the structure.
     */
    public void construct() {
        // Set the BottomUp flag in FileHandler
        FileHandler.setBottomUp(true);
        // Set the current object in the FileHandler
        FileHandler.setBtm(this);
        // Initialize the maximum latitude and longitude values
        double maxLAT = Double.MIN_VALUE;
        double maxLON = Double.MIN_VALUE;
        // Find the maximum latitude and longitude values from the records
        for (Record record : records) {
            subset_recs.add(record.copyRecord());
            maxLAT = Math.max(maxLAT, record.getLAT());
            maxLON = Math.max(maxLON, record.getLON());
        }
        // Initialize the HilbertSort with the subset of records and the maximum latitude or longitude
        HilbertSort sort = new HilbertSort(subset_recs, Math.max(maxLAT, maxLON));
        // Get the sorted list of records from the HilbertSort
        Queue<Record> sortedList = sort.hilbertHelper();
        // Initialize the list of IDs
        ArrayList<Integer> IDs = new ArrayList<>();
        while (!sortedList.isEmpty()) IDs.add(sortedList.remove().getId());
        try {
            RandomAccessFile indexfile = new RandomAccessFile("indexfile.dat", "rw");
            //calculate needed leaf nodes to fit records
            int max_records = FileHandler.calculateMaxBlockNodes();
            int blockSize = FileHandler.getBlockSize();
            //pre-calculate the levels needed to fit all the records and rectangles
            int leaflevel = getLevelsOfTree(IDs.size());
            int iterations = (int) Math.ceil((double) IDs.size() / max_records);
            double[][][] MBRs = new double[iterations][4][2];
            int[] MBRs_ID = new int[iterations];
            //number of records that each leaf node will contain
            ArrayList<Integer> leaf_sizes = new ArrayList<>();
            for (int i = 0; i < iterations; i++) {
                if (i == iterations - 1) {
                    if (IDs.size() - (i * max_records) < Math.floor(max_records * Split.getM())) {
                        int need = (int) (Math.floor(max_records * Split.getM()) - (IDs.size() - (i * max_records)));
                        leaf_sizes.add((int) Math.floor(max_records * Split.getM()));
                        leaf_sizes.set(i - 1, leaf_sizes.get(i - 1) - need);
                    } else leaf_sizes.add(IDs.size() - (i * max_records));
                } else leaf_sizes.add(max_records);
            }
            //iterate through every leaf node needed to be filled
            for (int k = 0; k < iterations; k++) {
                byte[] block = new byte[blockSize];
                System.arraycopy(ConversionToBytes.intToBytes(leaflevel), 0, block, 0, Integer.BYTES);
                System.arraycopy(ConversionToBytes.intToBytes(leaf_sizes.get(k)), 0, block, Integer.BYTES, Integer.BYTES);
                int counter = 3 * Integer.BYTES;
                //add records
                for (int i = 0; i < leaf_sizes.get(k); i++) {
                    Record temp = records.get(IDs.remove(0));
                    System.arraycopy(ConversionToBytes.doubleToBytes(temp.getLAT()), 0, block, counter, Double.BYTES);
                    counter += Double.BYTES;
                    System.arraycopy(ConversionToBytes.doubleToBytes(temp.getLON()), 0, block, counter, Double.BYTES);
                    counter += Double.BYTES;
                    System.arraycopy(ConversionToBytes.intToBytes(temp.getId()), 0, block, counter, Integer.BYTES);
                    counter += Integer.BYTES;
                    Split.calculateMBRpointbypoint(MBRs[k], temp, i == 0, false);
                }
                MBRs_ID[k] = blockID;
                try {
                    indexfile.seek(blockID * blockSize);
                    indexfile.write(block);
                    blockID++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //in case there's only one level, set root.
            if (leafLevelFINAL < 1) FileHandler.setRootMBR(MBRs[0]);
            int max_rectangles = FileHandler.calculateMaxBlockRectangles();
            iterations = (int) Math.ceil((double) MBRs.length / max_rectangles);
            ArrayList<Integer> nonleaf_sizes;
            double[][][] newMBR;
            int[] newMBR_ID;
            FileHandler.setRoot(1);
            //for levels that contain rectangles as entries (>leaf level)
            while (iterations > 1) {
                nonleaf_sizes = new ArrayList<>();
                leaflevel--;
                newMBR = new double[iterations][][];
                newMBR_ID = new int[iterations];
                //calculate amount of blocks needed to contain the entries of the previous level
                for (int i = 0; i < iterations; i++) {
                    if (i == iterations - 1) {
                        if (MBRs.length - (i * max_rectangles) < Math.floor(max_rectangles * Split.getM())) {
                            int need = (int) (Math.floor(max_rectangles * Split.getM()) - (MBRs.length - (i * max_rectangles)));
                            nonleaf_sizes.add((int) Math.floor(max_rectangles * Split.getM()));
                            nonleaf_sizes.set(i - 1, nonleaf_sizes.get(i - 1) - need);
                        } else nonleaf_sizes.add(MBRs.length - (i * max_rectangles));
                    } else nonleaf_sizes.add(max_rectangles);
                }
                int MBR_ID_counter = 0;
                for (int z = 0; z < iterations; z++) {
                    byte[] block = new byte[blockSize];
                    System.arraycopy(ConversionToBytes.intToBytes(leaflevel), 0, block, 0, Integer.BYTES);
                    System.arraycopy(ConversionToBytes.intToBytes(nonleaf_sizes.get(z)), 0, block, Integer.BYTES, Integer.BYTES);
                    int counter = 3 * Integer.BYTES;
                    ArrayList<Double[][]> tempmbr = new ArrayList<>();
                    for (int i = 0; i < nonleaf_sizes.get(z); i++) {
                        Double[][] temp = Split.rectangle_to_points(MBRs[MBR_ID_counter]);
                        try {
                            //since parent is decided, go to entries and fill in the metadata parent pointer
                            indexfile.seek(MBRs_ID[MBR_ID_counter] * blockSize + 2 * Integer.BYTES);
                            indexfile.write(ConversionToBytes.intToBytes(blockID));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //write mbr of children
                        for (int j = 0; j < FileHandler.getDimensions(); j++) {
                            for (int k = 0; k < FileHandler.getDimensions(); k++) {
                                System.arraycopy(ConversionToBytes.doubleToBytes(temp[j][k]), 0, block, counter, Double.BYTES);
                                counter += Double.BYTES;
                            }
                        }
                        tempmbr.add(temp);
                        System.arraycopy(ConversionToBytes.intToBytes(MBRs_ID[MBR_ID_counter]), 0, block, counter, Integer.BYTES);

                        MBR_ID_counter++;
                        counter += Integer.BYTES;
                    }
                    newMBR[z] = Split.calculateMBROfRectangles(tempmbr);
                    newMBR_ID[z] = blockID;
                    try {
                        indexfile.seek(blockID * blockSize);
                        indexfile.write(block);
                        blockID++;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                MBRs = newMBR;
                MBRs_ID = newMBR_ID;
                iterations = (int) Math.ceil((double) MBRs.length / max_rectangles);
            }
            //if tree has more than one level (this takes care of the first level and sets the root mbr)
            if (leafLevelFINAL >= 1) {
                leaflevel--;
                ArrayList<Double[][]> tempmbr = new ArrayList<>();
                byte[] block = new byte[blockSize];
                System.arraycopy(ConversionToBytes.intToBytes(leaflevel), 0, block, 0, Integer.BYTES);
                System.arraycopy(ConversionToBytes.intToBytes(MBRs.length), 0, block, Integer.BYTES, Integer.BYTES);
                System.arraycopy(ConversionToBytes.intToBytes(-1), 0, block, Integer.BYTES * 2, Integer.BYTES);
                int counter = 3 * Integer.BYTES;
                for (int i = 0; i < MBRs.length; i++) {
                    Double[][] temp = Split.rectangle_to_points(MBRs[i]);
                    try {
                        //sets parent pointer in children
                        indexfile.seek(MBRs_ID[i] * blockSize + 2 * Integer.BYTES);
                        indexfile.write(ConversionToBytes.intToBytes(1));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //write mbr of children
                    for (int j = 0; j < FileHandler.getDimensions(); j++) {
                        for (int k = 0; k < FileHandler.getDimensions(); k++) {
                            System.arraycopy(ConversionToBytes.doubleToBytes(temp[j][k]), 0, block, counter, Double.BYTES);
                            counter += Double.BYTES;
                        }
                    }
                    System.arraycopy(ConversionToBytes.intToBytes(MBRs_ID[i]), 0, block, counter, Integer.BYTES);
                    counter += Integer.BYTES;
                    tempmbr.add(temp);
                }
                //write file
                try {
                    indexfile.seek(blockSize);
                    indexfile.write(block);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                FileHandler.setRootMBR(Split.calculateMBROfRectangles(tempmbr));
            } else {
                try {
                    indexfile.seek(2 * blockSize + 2 * Integer.BYTES);
                    indexfile.write(ConversionToBytes.intToBytes(-1));
                    byte[] block = new byte[blockSize];
                    indexfile.seek(2 * blockSize);
                    indexfile.read(block, 0, blockSize);
                    indexfile.seek(blockSize);
                    indexfile.write(block);
                    blockID = 2;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            FileHandler.setNoOfIndexfileBlocks(blockID - 1);
            FileHandler.setLeafLevel(leafLevelFINAL);
            int[] metadata = new int[3];
            int counter = 0;
            metadata[0] = blockSize;
            metadata[1] = FileHandler.getNoOfIndexfileBlocks();
            metadata[2] = leafLevelFINAL;
            for (int i = 0; i < 3; i++) {
                try {
                    indexfile.seek(counter);
                    indexfile.write(ConversionToBytes.intToBytes(metadata[i]));
                    counter += Integer.BYTES;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method calculates the levels of a tree based on the given size.
     *
     * @param size The size of the tree.
     * @return The number of levels in the tree.
     */
    public int getLevelsOfTree(int size) {
        // Calculate the initial size and call the calculateLevels method
        return calculateLevels((int) Math.ceil((double) size / FileHandler.calculateMaxBlockNodes()));
    }

    /**
     * This method recursively calculates the levels of the tree. It calls itself until the size is less than or equal
     * to 1.
     *
     * @param size The size of the tree.
     * @return The number of levels in the tree.
     */
    private int calculateLevels(int size) {
        // If the size is less than or equal to 1, return 0
        if (size <= 1) {
            return 0;
        }
        // Otherwise, add 1 to the result of the recursive call
        return 1 + calculateLevels((int) Math.ceil((double) size / FileHandler.calculateMaxBlockRectangles()));
    }

    public void setleaflevelFINAL(int a) {
        leafLevelFINAL = a;
    }

}
