import com.oocourse.spec1.main.Runner;

public class MainClass {

    public static void main(String[] args) {
        try {
            Runner runner = new Runner(Person.class, Network.class, Tag.class);
            runner.run();
        } catch (NoSuchMethodException e) {
            System.err.println("Error: Required constructor not found in Person, Network, or Tag.");
            e.printStackTrace();
        } catch (SecurityException e) {
            System.err.println("Error: Security violation accessing constructors.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred during execution:");
            e.printStackTrace();
        }
    }
}