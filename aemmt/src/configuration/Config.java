package configuration;

//Here we will store all configurations of the VRP and AEMMT needed for the project

public class Config {
    public static final int POP_SIZE = 10;
    public static final int NUM_SUBPOP = 3; 
    public static final int SUBPOP_SIZE = POP_SIZE / NUM_SUBPOP; 
    public static final double VEHICLE_SPEED = 50.0;
    public static final int RANGE_COORDINATES = 100; 
    public static final double ELITISM_RATE = 0.05; 
    public static final double MUTATION_RATE = 0.01;
    public static final int ROUNDS = 1; 
    public static final int SELECTION = 1; // 1 - Roullette; 2 - Tournament
    public static final int CROSSING_TYPE = 1; // 1 - One Point Crossing; 2 - Two Points Crossing
    public static final int ELITISM_SIZE_POP = (int) Math.ceil(SUBPOP_SIZE * ELITISM_RATE); 

    public static final double BEFORE_COMPARISON_RATE = 0.05; 
    public static final int GENERATIONS_BEFORE_COMPARISON = (int) Math.ceil(ROUNDS * BEFORE_COMPARISON_RATE); 

    public static void printConfig() {
        System.out.println("Configurations:");
        System.out.println("POP_SIZE: " + POP_SIZE);
        System.out.println("VEHICLE_SPEED: " + VEHICLE_SPEED);
        System.out.println("RANGE_COORDINATES: " + RANGE_COORDINATES);
        System.out.println("ELITISM_RATE: " + ELITISM_RATE);
        System.out.println("MUTATION_RATE: " + MUTATION_RATE);
        System.out.println("ROUNDS: " + ROUNDS);
        System.out.println("SELECTION: " + SELECTION);
        System.out.println("CROSSING_TYPE: " + CROSSING_TYPE);
        System.out.println("NUM_SUBPOP: " + NUM_SUBPOP);
        System.out.println("SUBPOP_SIZE: " + SUBPOP_SIZE);
        System.out.println("ELITISM_SIZE_POP: " + ELITISM_SIZE_POP);
        System.out.println("BEFORE_COMPARISON_RATE: " + BEFORE_COMPARISON_RATE);
        System.out.println("GENERATIONS_BEFORE_COMPARISON: " + GENERATIONS_BEFORE_COMPARISON);
    }
}
