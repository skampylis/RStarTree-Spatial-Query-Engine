import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

public class UserInterface {
    private final Scanner scanner;
    private int dimensions = 2;
    private String userInput = "";
    private boolean isBuilt = false;
    private boolean isReused = false;

    UserInterface() {
        scanner = new Scanner(System.in);
        menu();
    }

    private void menu() {
        System.out.println("Menu (Choose something from " + "them)\n1) Start,\n2) Settings,\n3) About");
        userInput = "";
        while (!userInput.equals("1") && !userInput.equals("2") && !userInput.equals("3") && !userInput.equals("Start") && !userInput.equals("Settings") && !userInput.equals("About")) {
            System.out.print("Input: ");
            userInput = scanner.nextLine();
        }
        System.out.println();
        switch (userInput) {
            case "1", "Start" -> startMenu();
            case "2", "Settings" -> settingsMenu();
            case "3", "About" -> aboutMenu();
        }
    }

    private void getDimensions() {
        do {
            try {
                System.out.print("\nInsert the dimension number: ");
                dimensions = scanner.nextInt();
            } catch (InputMismatchException e) {
                invalidArgs("dimensions");
                scanner.nextLine();
            }
            if (dimensions == 1) {
                invalidArgs("dimensions");
            }
            if (dimensions == FileHandler.getDimensions()) {
                System.out.println("The dimensions are already " + dimensions + ".");
            }
        } while (dimensions < 2 || dimensions == FileHandler.getDimensions());
        System.out.println();
        scanner.nextLine();
        FileHandler.setDimensions(dimensions);
    }

    Rectangle getRangeQueryRectangle() {
        System.out.println("Insert the rectangle coordinates (they should be only positive and not overlap in the same " + "axis): ");
        ArrayList<Double> coordinates = new ArrayList<>();
        do {
            try {
                switch (coordinates.size()) {
                    case 0 -> System.out.print("Min LAT: ");
                    case 1 -> System.out.print("Min LON: ");
                    case 2 -> System.out.print("Max LAT: ");
                    case 3 -> System.out.print("Max LON: ");
                }
                coordinates.add(scanner.nextDouble());
            } catch (InputMismatchException e) {
                invalidArgs("type");
                scanner.nextLine();
                coordinates.clear();
            }
            if (!coordinates.isEmpty() && coordinates.get(coordinates.size() - 1) < 0.0) {
                invalidArgs("negative");
                scanner.nextLine();
                coordinates.clear();
            }
            if (coordinates.size() == 2 * dimensions) {
                for (int i = 0; i < dimensions; i++) {
                    if (coordinates.get(i).equals(coordinates.get(i + dimensions))) {
                        invalidArgs("overlap");
                        coordinates.clear();
                        scanner.nextLine();
                        break;
                    }
                }
            }
        } while (coordinates.size() != 4);
        scanner.nextLine();
        return new Rectangle(coordinates);
    }

    public int getK() {
        int k = -1;
        do {
            try {
                System.out.print("Insert the k (k should be a positive Integer): ");
                k = scanner.nextInt();
            } catch (InputMismatchException e) {
                invalidArgs("k");
                scanner.nextLine();
            }
        } while (k < 1);
        return k;
    }

    public ArrayList<Double> getPointFromUser() {
        System.out.println("Insert the coordinates of the point (coordinates should be positive Double/Float or Integers) ");
        ArrayList<Double> coordinates = new ArrayList<>();
        do {
            try {
                switch (coordinates.size()) {
                    case 0 -> System.out.print("Lat: ");
                    case 1 -> System.out.print("Lon: ");
                }
                coordinates.add(scanner.nextDouble());
            } catch (InputMismatchException e) {
                invalidArgs("type");
                scanner.nextLine();
                coordinates.clear();
            }
            if (!coordinates.isEmpty() && coordinates.get(coordinates.size() - 1) < 0) {
                invalidArgs("negative");
                scanner.nextLine();
                coordinates.clear();
            }
        } while (coordinates.size() < dimensions);
        scanner.nextLine();
        return coordinates;
    }

    private void invalidArgs(String desc) {
        switch (desc) {
            case "negative" -> System.out.println("Invalid arguments, coordinates should be >0.");
            case "overlap" ->
                    System.out.println("Invalid arguments, coordinates between the same axis shouldn't overlap.");
            case "type" ->
                    System.out.println("Invalid arguments, coordinates should be Double/Float or Integers with , and not .");
            case "k" -> System.out.println("K should be a positive integer.");
            case "dimensions" -> System.out.println("Dimensions should be a positive Integer greater or equal to 2.");
            case "nodeId" -> System.out.println("Node ID should be a positive long Integer");
        }
    }

    private void startMenu() {
        if (!isBuilt && !isReused) {
            System.out.println("Options:\n1) Build,\n2) Reuse,\n3) Esc\n" + "to build the tree, reuse the old files or return to the main menu respectively.");
        } else
            System.out.println("Options:\n2) Reuse,\n3) Esc\n" + "to reuse the old files or return to the main menu respectively.");
        userInput = "";
        do {
            System.out.print("Input: ");
            userInput = scanner.nextLine();
        } while ((!userInput.equals("1") || isBuilt) && !userInput.equals("2") && !userInput.equals("3") && !userInput.equals("Build") && !userInput.equals("Reuse") && !userInput.equals("ESC"));
        System.out.println();
        if ((userInput.equals("1") || userInput.equals("Build")) && !isBuilt) {
            buildMenu();
        } else if ((userInput.equals("2") || userInput.equals("Reuse")) && !isReused) {
            FileHandler.retrieveOldFileInfo();
            isReused = true;
            treeOptionsMenu();
        } else if (userInput.equals("2") || userInput.equals("Reuse")) {
            treeOptionsMenu();
        } else {
            menu();
        }
    }

    private void settingsMenu() {
        String text = "The default setting of the R* tree are 2 Dimensions. \nType (option or number): " + "\n1) Dimensions,\n2) ESC\n" + "to change their number (of dimensions) or return to the main menu respectively.";
        System.out.println(text);
        userInput = "";
        do {
            if (userInput.equals("Dimensions") || userInput.equals("1")) {
                getDimensions();
                System.out.println(text);
            }
            System.out.print("Input: ");
            userInput = scanner.nextLine();
        } while ((!userInput.equals("ESC") && !userInput.equals("2")));
        System.out.println();
        userInput = "";
        menu();
    }

    private void aboutMenu() {
        System.out.println("The R* tree implementation is developed for the " + "Course of Database Technology.\n" + "Type ESC to return to the main menu.");
        userInput = "";
        while (!userInput.equals("ESC")) {
            System.out.print("Input: ");
            userInput = scanner.nextLine();
        }
        System.out.println();
        menu();
    }

    private void buildMenu() {
        System.out.println("Options (type option or number):\n1) Point by point,\n2) Bottom-up,\n3) ESC\n" + "to build the tree inserting the entries one by one, using the bottom-up approach or return to the " + "Start menu respectively");
        userInput = "";
        do {
            System.out.print("Input: ");
            userInput = scanner.nextLine();
        } while (!userInput.equals("1") && !userInput.equals("2") && !userInput.equals("3") && !userInput.equals("Point by point") && !userInput.equals("Bottom-up") && !userInput.equals("ESC"));
        System.out.println();
        if (userInput.equals("1") || userInput.equals("Point by point")) {
            System.out.println("Parsing data...");
            long start = System.currentTimeMillis();
            FileHandler.createDataFile(dimensions);
            System.out.println("Tree is building...");
            FileHandler.createIndexFile(true);
            FileHandler.readIndexFile();
            long end = System.currentTimeMillis();
            double elapsedTime = (double) (end - start) / 1000;
            System.out.println("Total elapsed time :" + elapsedTime + " seconds\n");
            System.out.println("Tree structure is located in treeOutput.txt\n");
            isBuilt = true;
            treeOptionsMenu();
        } else if (userInput.equals("2") || userInput.equals("Bottom-up")) {
            System.out.println("Parsing data...");
            long start = System.currentTimeMillis();
            FileHandler.createDataFile(dimensions);
            System.out.println("Tree is building...");
            FileHandler.createIndexFile(false);
            BottomUp bottomUp = new BottomUp();
            bottomUp.construct();
            FileHandler.readIndexFile();
            long end = System.currentTimeMillis();
            double elapsedTime = (double) (end - start) / 1000;
            System.out.println("Total elapsed time :" + elapsedTime + " seconds\n");
            System.out.println("Tree structure is located in treeOutput.txt\n");
            isBuilt = true;
            treeOptionsMenu();
        } else {
            startMenu();
        }
    }

    private void treeOptionsMenu() {
        System.out.println("Options (type option or number):\n1) Insert,\n2) Delete,\n3) Range Query,\n4) K-nn Query,\n" + "5) Skyline query,\n6) Linear search Range Query, \n7) Linear search K-nn Query, \n8) ESC");
        userInput = "";
        do {
            System.out.print("Input: ");
            userInput = scanner.nextLine();
        } while (!userInput.equals("1") && !userInput.equals("2") && !userInput.equals("3") && !userInput.equals("4") && !userInput.equals("5") && !userInput.equals("6") && !userInput.equals("7") && !userInput.equals("8") && !userInput.equals("Insert") && !userInput.equals("Delete") && !userInput.equals("Range Query") && !userInput.equals("K-nn Query") && !userInput.equals("Skyline Query") && !userInput.equals("Linear search Range Query") && !userInput.equals("Linear search K-nn Query") && !userInput.equals("ESC"));
        System.out.println();
        switch (userInput) {
            case "1", "Insert" -> {
                insertMenu();
                FileHandler.readIndexFile();
            }
            case "2", "Delete" -> {
                ArrayList<Double> coords = getPointFromUser();
                Delete.delete(coords.get(0), coords.get(1));
                FileHandler.readIndexFile();
            }
            case "3", "Range Query" -> {
                RangeQuery rangeQuery = new RangeQuery(getRangeQueryRectangle());
                rangeQuery.print();
            }
            case "4", "K-nn Query" -> {
                KnnQuery knnQuery = new KnnQuery(getK(), getPointFromUser());
                knnQuery.print();
            }
            case "5", "Skyline Query" -> {
                SkylineQuery skylineQuery = new SkylineQuery();
                skylineQuery.print();
            }
            case "6", "Linear search Range Query" -> {
                LinearSearchRangeQuery rangeQuery = new LinearSearchRangeQuery(getRangeQueryRectangle());
                rangeQuery.print();
            }
            case "7", "Linear search K-nn Query" -> {
                LinearSearchKnnQuery knnQuery = new LinearSearchKnnQuery(getK(), getPointFromUser());
                knnQuery.print();
            }
            default -> startMenu();
        }
        System.out.print("\nPress ENTER to continue");
        scanner.nextLine();
        treeOptionsMenu();
    }

    private void insertMenu() {
        ArrayList<Double> coords = getPointFromUser();
        long nodeId = 0;
        String name;
        System.out.print("Insert the new node Id (SEE FROM treeOutput.txt): ");
        do {
            try {
                nodeId = scanner.nextLong();
            } catch (InputMismatchException e) {
                invalidArgs("nodeId");
                scanner.nextLine();
            }
            if (nodeId < 0) {
                invalidArgs("nodeId");
                scanner.nextLine();
                nodeId = 0;
            }
        } while (nodeId == 0);
        scanner.nextLine();
        System.out.print("Insert the new node Name or press ENTER if you don't want to name it: ");
        name = scanner.nextLine();
        Insert.datafileRecordInsert(new Record(coords, nodeId, name));
        if (name.isEmpty()) {
            System.out.println("The node with LAT: " + coords.get(0) + ", LON: " + coords.get(1) + " and ID: " + nodeId + " was successfully inserted.");
        } else {
            System.out.println("The node with LAT: " + coords.get(0) + ", LON: " + coords.get(1) + " ,ID: " + nodeId + " and Name: " + name + " was successfully inserted.");
        }
    }

}