package vrp;

import java.io.*;
import java.util.*;

// Here we will create the BenchMarkReader class
// This class will read the benchmark files and store the information in a ProblemInstance object

public class BenchMarkReader {

    public static ProblemInstance readInstaces(String filePath) throws IOException {
        int numVehicles = 0;
        int vehicleCapacity = 0;
        List<Client> clients = new ArrayList<>();

        try (BufferedReader bufferReader = new BufferedReader(new FileReader(filePath))) {
            String line; // will store each line of the file
            int lineNumber = 0;
            boolean readingClients = false;

            while ((line = bufferReader.readLine()) != null) {
                lineNumber++;
                line = line.trim(); // removing white spaces

                // condition to ignore empty lines and the title of the benchmark
                if (line.isEmpty() || lineNumber == 1) {
                    continue;
                }

                // getting the number of vehicles and the vehicle capacity (line 5 every time)
                if (lineNumber == 5) {
                    String[] values = line.split("\\s+");
                    numVehicles = Integer.parseInt(values[0]);
                    vehicleCapacity = Integer.parseInt(values[1]);
                }

                if (lineNumber == 10) {
                    readingClients = true;
                }

                // Reading the clients
                if (readingClients) {
                    String[] values = line.split("\\s+");

                    try {
                        int id = Integer.parseInt(values[0].trim());
                        double x = Double.parseDouble(values[1].trim());
                        double y = Double.parseDouble(values[2].trim());
                        int demand = Integer.parseInt(values[3].trim());
                        int readyTime = Integer.parseInt(values[4].trim());
                        int dueTime = Integer.parseInt(values[5].trim());
                        int serviceTime = Integer.parseInt(values[6].trim());

                        clients.add(new Client(id, x, y, demand, readyTime, dueTime, serviceTime));

                    } catch (NumberFormatException e) {
                        System.out.println("Error reading the file at line " + lineNumber);
                        e.printStackTrace();
                    }

                }

            }

        } catch (IOException e) {
            System.out.println("Error reading the file");
            e.printStackTrace();
        }

        return new ProblemInstance(numVehicles, vehicleCapacity, clients);

    }
}
