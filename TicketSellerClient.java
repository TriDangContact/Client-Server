import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TicketSellerClient {

	private TicketSellerClient() {}

	public static void main(String[] args) {

		String host = (args.length < 1) ? null : args[0];
		try {
			Registry registry = LocateRegistry.getRegistry(host);
			TicketSellerRemote stub = (TicketSellerRemote) registry.lookup("TicketSellerRemote");
			stub.start();
			//stub.join();		
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
	}
}
