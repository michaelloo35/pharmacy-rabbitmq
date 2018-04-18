package utils;

public class Printer {
    public void printColored(Color color, String text) {
        System.out.println(color + text + Color.RESET);
    }
}
