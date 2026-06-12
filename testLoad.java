public class testLoad {
    public static void main(String[] args) throws Exception {
        Class<?> c = Class.forName("com.formdev.flatlaf.util.LoggingFacade");
        System.out.println("Loaded: " + c.getName());
    }
}
