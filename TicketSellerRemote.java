import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TicketSellerRemote extends Remote {

	void start() throws RemoteException;
}
