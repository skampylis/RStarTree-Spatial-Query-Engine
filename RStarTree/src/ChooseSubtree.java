import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * This class provides methods to handle the process of choosing a subtree to insert a given record. It also provides
 * methods to determine the best insertion for rectangles and records.
 */
public class ChooseSubtree {

    /**
     * Chooses a subtree to insert a given record. If the tree is empty, a new root is created.
     *
     * @param record       the record to be inserted
     * @param currentBlock the current block in the tree where we are considering to insert the record
     * @return the block where the record should be inserted
     */
    public static int chooseSubtree(Record record, int currentBlock) {
        try {
            int root = FileHandler.getRoot();
            String IndexfilePath = FileHandler.getIndexfilePath();
            int blockSize = FileHandler.getBlockSize();
            int leafLevel = FileHandler.getLeafLevel();
            int noOfIndexfileBlocks = FileHandler.getNoOfIndexfileBlocks();
            int dimensions = FileHandler.getDimensions();
            if (root == -1) {
                noOfIndexfileBlocks++;
                leafLevel++;
                FileHandler.setNoOfIndexfileBlocks(noOfIndexfileBlocks);
                FileHandler.setLeafLevel(leafLevel);
                RandomAccessFile indexfile = new RandomAccessFile(IndexfilePath, "rw");
                indexfile.seek(4);
                indexfile.write(ConversionToBytes.intToBytes(noOfIndexfileBlocks));
                indexfile.seek(8);
                indexfile.write(ConversionToBytes.intToBytes(leafLevel));
                byte[] block = new byte[blockSize];
                System.arraycopy(ConversionToBytes.intToBytes(leafLevel), 0, block, 0, Integer.BYTES);
                System.arraycopy(ConversionToBytes.intToBytes(0), 0, block, Integer.BYTES, Integer.BYTES);
                System.arraycopy(ConversionToBytes.intToBytes(-1), 0, block, 2 * Integer.BYTES, Integer.BYTES);
                indexfile.seek((long) blockSize * noOfIndexfileBlocks);
                indexfile.write(block);
                indexfile.close();
            }
            File file = new File(IndexfilePath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] dataBlock = new byte[blockSize];
            System.arraycopy(bytes, currentBlock * blockSize, dataBlock, 0, blockSize);
            byte[] level = new byte[Integer.BYTES];
            byte[] currentNoOfEntries = new byte[Integer.BYTES];
            byte[] parentPointer = new byte[Integer.BYTES];
            System.arraycopy(dataBlock, 0, level, 0, Integer.BYTES);
            System.arraycopy(dataBlock, Integer.BYTES, currentNoOfEntries, 0, Integer.BYTES);
            System.arraycopy(dataBlock, 2 * Integer.BYTES, parentPointer, 0, Integer.BYTES);
            int tempLevel = ByteBuffer.wrap(level).getInt();
            int tempCurrentNoOfEntries = ByteBuffer.wrap(currentNoOfEntries).getInt();
            ByteBuffer.wrap(parentPointer).getInt();
            if (tempLevel == leafLevel) {
                return currentBlock;
            } else if (tempLevel + 1 == leafLevel) {
                ArrayList<double[][]> rectangles = new ArrayList<>();
                double[][] temp = new double[dimensions][dimensions];
                int[] IDs = new int[tempCurrentNoOfEntries];
                int counter = 12;
                byte[] tempSave = new byte[Double.BYTES];
                byte[] tempSaveID = new byte[Integer.BYTES];
                for (int i = 0; i < tempCurrentNoOfEntries; i++) {
                    for (int j = 0; j < 2; j++) {
                        for (int k = 0; k < dimensions; k++) {
                            System.arraycopy(dataBlock, counter, tempSave, 0, Double.BYTES);
                            temp[j][k] = ByteBuffer.wrap(tempSave).getDouble();
                            counter += Double.BYTES;
                        }
                    }
                    rectangles.add(temp);
                    System.arraycopy(dataBlock, counter, tempSaveID, 0, Integer.BYTES);
                    IDs[i] = ByteBuffer.wrap(tempSaveID).getInt();
                    counter += Integer.BYTES;
                    temp = new double[dimensions][dimensions];
                }
                int result = determine_best_insertion(rectangles, record);
                return chooseSubtree(record, IDs[result]);
            } else {
                ArrayList<double[][]> rectangles = new ArrayList<>();
                double[][] temp = new double[dimensions][dimensions];
                int[] IDs = new int[tempCurrentNoOfEntries];
                int counter = 12;
                byte[] tempSave = new byte[Double.BYTES];
                byte[] tempSaveID = new byte[Integer.BYTES];
                for (int i = 0; i < tempCurrentNoOfEntries; i++) {
                    for (int j = 0; j < 2; j++) {
                        for (int k = 0; k < dimensions; k++) {
                            System.arraycopy(dataBlock, counter, tempSave, 0, Double.BYTES);
                            temp[j][k] = ByteBuffer.wrap(tempSave).getDouble();
                            counter += Double.BYTES;
                        }
                    }
                    rectangles.add(temp);
                    System.arraycopy(dataBlock, counter, tempSaveID, 0, Integer.BYTES);
                    IDs[i] = ByteBuffer.wrap(tempSaveID).getInt();
                    counter += Integer.BYTES;
                    temp = new double[dimensions][dimensions];
                }
                int result = determine_best_insertion_forRectangles(rectangles, record);
                return chooseSubtree(record, IDs[result]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * Calculates the least overlap with siblings, among siblings.
     *
     * @param rectangles the rectangles to calculate the least overlap with
     * @param record     the record for which the least overlap is calculated
     * @return the index of the rectangle with the least overlap
     */
    public static int determine_best_insertion_forRectangles(ArrayList<double[][]> rectangles, Record record) {
        int dimensions = FileHandler.getDimensions();
        double[][] temp1 = new double[(int) Math.pow(2, dimensions)][dimensions];
        double[][] temp3 = new double[(int) Math.pow(2, dimensions)][dimensions];
        double area_diff;
        double area = 0;
        double least_diff = Double.MAX_VALUE;
        int result = 0;
        for (int i = 0; i < rectangles.size(); i++) {
            Split.points_to_rectangle(rectangles.get(i), temp1);
            for (int b = 0; b < temp1.length; b++)
                System.arraycopy(temp1[b], 0, temp3[b], 0, temp1[0].length);
            Split.calculateMBRpointbypoint(temp1, record, false, false);
            area_diff = Split.calcAreaDiff(temp1, temp3);
            if (area_diff < least_diff) {
                least_diff = area_diff;
                result = i;
                area = Split.calcArea(temp1);
            } else if (area_diff == least_diff) {
                double b = Split.calcArea(temp1);
                if (b < area) {
                    area = b;
                    result = i;
                }
            }
        }
        return result;
    }

    /**
     * Calculates the least overlap with siblings, among siblings.
     *
     * @param rectangles the rectangles to calculate the least overlap with
     * @param record     the record for which the least overlap is calculated
     * @return the index of the rectangle with the least overlap
     */
    public static int determine_best_insertion(ArrayList<double[][]> rectangles, Record record) {
        int dimensions = FileHandler.getDimensions();
        double[][] temp1 = new double[(int) Math.pow(2, dimensions)][dimensions];
        double[][] temp2 = new double[(int) Math.pow(2, dimensions)][dimensions];
        double[][] temp3 = new double[(int) Math.pow(2, dimensions)][dimensions];
        double temp_overlap = 0;
        double area_diff = 0;
        double area = 0;
        double least_overlap = Double.MAX_VALUE;
        int result = 0;
        for (int i = 0; i < rectangles.size(); i++) {
            Split.points_to_rectangle(rectangles.get(i), temp1);
            for (int b = 0; b < temp1.length; b++)
                System.arraycopy(temp1[b], 0, temp3[b], 0, temp1[0].length);
            Split.calculateMBRpointbypoint(temp1, record, false, false);
            for (int j = 0; j < rectangles.size(); j++) {
                if (j != i) {
                    Split.points_to_rectangle(rectangles.get(j), temp2);
                    temp_overlap += Split.calcOverlap(temp1, temp2);
                }
            }
            if (temp_overlap < least_overlap) {
                least_overlap = temp_overlap;
                result = i;
                area_diff = Split.calcAreaDiff(temp1, temp3);
                area = Split.calcArea(temp1);
            } else if (temp_overlap == least_overlap) {
                double b = Split.calcAreaDiff(temp1, temp3);
                if (b < area_diff) {
                    area_diff = b;
                    result = i;
                    area = Split.calcArea(temp1);
                }
                if (b == area_diff) {
                    double c = Split.calcArea(temp1);
                    if (c < area) {
                        result = i;
                        area = c;
                    }
                }
            }
            temp_overlap = 0;
        }
        return result;
    }

}