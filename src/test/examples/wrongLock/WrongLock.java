package test.examples.wrongLock;
/**
 * @author Xuan
 * Created on 2005-1-18
 * 
 * This class simulates the wrong lock bug
 * Method A requests a lock on data while method B request a lock
 * on the class.
 */
public class WrongLock {
    Data data;
    public WrongLock(Data data) {
	this.data =data;
    }
	
    public void A() {

		synchronized (data) {
			int x=data.value;
			data.value++;
			assert (data.value==x+1) : "bug found";
//			if (data.value != (x+1))
//				assert false : "bug found";
		}
    }
	
    public void B() {
		synchronized (this) {
			data.value++;
		}
    }
}
