import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SleepingBarber {
	
	public static void main (String a[]) throws InterruptedException {	
		
		int noOfBarbers=2, customerId=1, noOfCustomers=100, noOfChairs;	//inicializando el número de peluqueros y clientes
		
		Scanner sc = new Scanner(System.in);
		
		System.out.println("Enter the number of barbers(M):");			//entrada M barberos
    	noOfBarbers=sc.nextInt();
    	
    	System.out.println("Enter the number of waiting room"			//entrada N sillas de espera
    			+ " chairs(N):");
    	noOfChairs=sc.nextInt();
    	
//    	System.out.println("Enter the number of customers:");			//Entrar el número de clientes de la tienda.
//    	noOfCustomers=sc.nextInt();
    	
		ExecutorService exec = Executors.newFixedThreadPool(12);		//inicializando con 12 subprocesos como múltiplos de núcleos en la CPU, aquí 6
    	Bshop shop = new Bshop(noOfBarbers, noOfChairs);				//Inicializando la peluquería con el número de peluqueros.
    	Random r = new Random();  										//un número aleatorio para calcular retrasos en la llegada de clientes y cortes de pelo
       	    	
        System.out.println("\nBarber shop opened with "
        		+noOfBarbers+" barber(s)\n");
        
        long startTime  = System.currentTimeMillis();					//hora de inicio del programa
        
        for(int i=1; i<=noOfBarbers;i++) {								//generando el número especificado de hilos para barbero
        	
        	Barber barber = new Barber(shop, i);	
        	Thread thbarber = new Thread(barber);
            exec.execute(thbarber);
        }
        
        for(int i=0;i<noOfCustomers;i++) {								//generador de clientes; generando hilos de clientes
        
            Customer customer = new Customer(shop);
            customer.setInTime(new Date());
            Thread thcustomer = new Thread(customer);
            customer.setcustomerId(customerId++);
            exec.execute(thcustomer);
            
            try {
            	
            	double val = r.nextGaussian() * 2000 + 2000;			//'r':objeto de clase Random, nextGaussian() genera un número con media 2000 y	
            	int millisDelay = Math.abs((int) Math.round(val));		//desviación estándar como 2000, por lo que los clientes llegan a una media de 2000 milisegundos
            	Thread.sleep(millisDelay);								//y desviación estándar de 2000 milisegundos
            }
            catch(InterruptedException iex) {
            
                iex.printStackTrace();
            }
            
        }
        
        exec.shutdown();												//cierra el servicio ejecutor y libera todos los recursos
        exec.awaitTermination(12, SECONDS);								//espera 12 segundos hasta que todos los hilos terminen su ejecución
 
        long elapsedTime = System.currentTimeMillis() - startTime;		//para calcular la hora de finalización del programa
        
        System.out.println("\nBarber shop closed");
        System.out.println("\nTotal time elapsed in seconds"
        		+ " for serving "+noOfCustomers+" customers by "
        		+noOfBarbers+" barbers with "+noOfChairs+
        		" chairs in the waiting room is: "
        		+TimeUnit.MILLISECONDS
        	    .toSeconds(elapsedTime));
        System.out.println("\nTotal customers: "+noOfCustomers+
        		"\nTotal customers served: "+shop.getTotalHairCuts()
        		+"\nTotal customers lost: "+shop.getCustomerLost());
               
        sc.close();
    }
}
 
class Barber implements Runnable {										// Inicializando el peluquero

    Bshop shop;
    int barberId;
 
    public Barber(Bshop shop, int barberId) {
    
        this.shop = shop;
        this.barberId = barberId;
    }
    
    public void run() {
    
        while(true) {
        
            shop.cutHair(barberId);
        }
    }
}

class Customer implements Runnable {

    int customerId;
    Date inTime;
 
    Bshop shop;
 
    public Customer(Bshop shop) {
    
        this.shop = shop;
    }
 
    public int getCustomerId() {										//métodos getter y setter
        return customerId;
    }
 
    public Date getInTime() {
        return inTime;
    }
 
    public void setcustomerId(int customerId) {
        this.customerId = customerId;
    }
 
    public void setInTime(Date inTime) {
        this.inTime = inTime;
    }
 
    public void run() {													//El hilo del cliente va a la tienda para el corte de pelo.
    
        goForHairCut();
    }
    private synchronized void goForHairCut() {							//el cliente se agrega a la lista
    
        shop.add(this);
    }
}
 
class Bshop {

	private final AtomicInteger totalHairCuts = new AtomicInteger(0);
	private final AtomicInteger customersLost = new AtomicInteger(0);
	int nchair, noOfBarbers, availableBarbers;
    List<Customer> listCustomer;
    
    Random r = new Random();	 
    
    public Bshop(int noOfBarbers, int noOfChairs){
    
        this.nchair = noOfChairs;														//cantidad de sillas en la sala de espera
        listCustomer = new LinkedList<Customer>();						//lista para almacenar los clientes que llegan
        this.noOfBarbers = noOfBarbers;									//inicializando el número total de barberos
        availableBarbers = noOfBarbers;
    }
 
    public AtomicInteger getTotalHairCuts() {
    	
    	totalHairCuts.get();
    	return totalHairCuts;
    }
    
    public AtomicInteger getCustomerLost() {
    	
    	customersLost.get();
    	return customersLost;
    }
    
    public void cutHair(int barberId)
    {
        Customer customer;
        synchronized (listCustomer) {									//listCustomer es un recurso compartido por lo que se ha sincronizado para evitar cualquier
        															 	//Errores inesperados en la lista cuando varios subprocesos acceden a ella.
            while(listCustomer.size()==0) {
            
                System.out.println("\nBarber "+barberId+" is waiting "
                		+ "for the customer and sleeps in his chair");
                
                try {
                
                    listCustomer.wait();								//El peluquero duerme si no hay clientes en la tienda.
                }
                catch(InterruptedException iex) {
                
                    iex.printStackTrace();
                }
            }
            
            customer = (Customer)((LinkedList<?>)listCustomer).poll();	//toma al primer cliente del encabezado de la lista para cortarle el pelo
            
            System.out.println("Customer "+customer.getCustomerId()+
            		" finds the barber asleep and wakes up "
            		+ "the barber "+barberId);
        }
        
        int millisDelay=0;
                
        try {
        	
        	availableBarbers--; 										//disminuye el recuento de barberos disponibles cuando uno de ellos comienza
        																//Cortar el pelo del cliente y el cliente duerme.
            System.out.println("Barber "+barberId+" cutting hair of "+
            		customer.getCustomerId()+ " so customer sleeps");
        	
            double val = r.nextGaussian() * 2000 + 4000;				//El tiempo necesario para cortar el cabello del cliente tiene una media de 4000 milisegundos y
        	millisDelay = Math.abs((int) Math.round(val));				//y desviación estándar de 2000 milisegundos
        	Thread.sleep(millisDelay);
        	
        	System.out.println("\nCompleted Cutting hair of "+
        			customer.getCustomerId()+" by barber " + 
        			barberId +" in "+millisDelay+ " milliseconds.");
        
        	totalHairCuts.incrementAndGet();
            															//sale por la puerta
            if(listCustomer.size()>0) {									
            	System.out.println("Barber "+barberId+					//El barbero encuentra a un cliente dormido en la sala de espera, lo despierta y
            			" wakes up a customer in the "					//Y luego va a su silla y duerme hasta que llega un cliente.
            			+ "waiting room");		
            }
            
            availableBarbers++;											//El peluquero está disponible para cortar el pelo al próximo cliente.
        }
        catch(InterruptedException iex) {
        
            iex.printStackTrace();
        }
        
    }
 
    public void add(Customer customer) {
    
        System.out.println("\nCustomer "+customer.getCustomerId()+
        		" enters through the entrance door in the the shop at "
        		+customer.getInTime());
 
        synchronized (listCustomer) {
        
            if(listCustomer.size() == nchair) {							//No hay sillas disponibles para el cliente por lo que el cliente abandona la tienda.            
                System.out.println("\nNo chair available "
                		+ "for customer "+customer.getCustomerId()+
                		" so customer leaves the shop");
                
              customersLost.incrementAndGet();
                
                return;
            }
            else if (availableBarbers > 0) {							//Si hay un barbero disponible, el cliente lo despierta y se sienta en
            															//la silla
            	((LinkedList<Customer>)listCustomer).offer(customer);
				listCustomer.notify();
			}
            else {														//Si los barberos están ocupados y hay sillas en la sala de espera, el cliente
            															//se sienta en la silla de la sala de espera
            	((LinkedList<Customer>)listCustomer).offer(customer);
                
            	System.out.println("All barber(s) are busy so "+
            			customer.getCustomerId()+
                		" takes a chair in the waiting room");
                 
                if(listCustomer.size()==1)
                    listCustomer.notify();
            }
        }
    }
}
