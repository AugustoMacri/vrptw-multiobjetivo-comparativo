package vrp;

//Here we will create the Client class
//This class will store the information of each client

public class Client {
    private int id;
    private double x;
    private double y;
    private int demand;
    private int readyTime;
    private int dueTime;
    private int serviceTime;
    private double distanceFromDepot;

    public Client(int id, double x, double y, int demand, int readyTime, int dueTime, int serviceTime) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.demand = demand;
        this.readyTime = readyTime;
        this.dueTime = dueTime;
        this.serviceTime = serviceTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getDemand() {
        return demand;
    }

    public void setDemand(int demand) {
        this.demand = demand;
    }

    public int getReadyTime() {
        return readyTime;
    }

    public void setReadyTime(int readyTime) {
        this.readyTime = readyTime;
    }

    public int getDueTime() {
        return dueTime;
    }

    public void setDueTime(int dueTime) {
        this.dueTime = dueTime;
    }

    public int getServiceTime() {
        return serviceTime;
    }

    public void setServiceTime(int serviceTime) {
        this.serviceTime = serviceTime;
    }

    public double getDistanceFromDepot() {
        return distanceFromDepot;
    }

    public void setDistanceFromDepot(double distanceFromDepot) {
        this.distanceFromDepot = distanceFromDepot;
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", demand=" + demand +
                ", readyTime=" + readyTime +
                ", dueTime=" + dueTime +
                ", serviceTime=" + serviceTime +
                ", distanceFromDepot=" + distanceFromDepot +
                '}';
    }
}
