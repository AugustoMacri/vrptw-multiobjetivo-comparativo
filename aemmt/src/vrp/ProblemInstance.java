package vrp;

import java.util.List;

// Here we will create the ProblemInstance class
// This class will store the information of the problem instance

public class ProblemInstance {
    private int numVehicles;
    private int vehicleCapacity;
    private List<Client> clients;

    public ProblemInstance(int numVehicles, int vehicleCapacity, List<Client> clients) {
        this.numVehicles = numVehicles;
        this.vehicleCapacity = vehicleCapacity;
        this.clients = clients;
    }

    public int getNumVehicles() {
        return numVehicles;
    }

    public void setNumVehicles(int numVehicles) {
        this.numVehicles = numVehicles;
    }

    public int getVehicleCapacity() {
        return vehicleCapacity;
    }

    public void setVehicleCapacity(int vehicleCapacity) {
        this.vehicleCapacity = vehicleCapacity;
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

}
