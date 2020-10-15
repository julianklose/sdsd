package de.sdsd.projekt.example.service;

/**
 * The Class Main.
 */
public class Main {
	
	/** The Constant LOCAL. */
	private static final boolean LOCAL = false;

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		try (ExampleService service = new ExampleService(" SDSD SERVICE TOKEN ", LOCAL)) {
			System.out.println("Press ENTER to quit");
			System.in.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
