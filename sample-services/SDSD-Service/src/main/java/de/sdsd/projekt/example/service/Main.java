package de.sdsd.projekt.example.service;

public class Main {
	private static final boolean LOCAL = false;

	public static void main(String[] args) {
		try (ExampleService service = new ExampleService(" SDSD SERVICE TOKEN ", LOCAL)) {
			System.out.println("Press ENTER to quit");
			System.in.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
