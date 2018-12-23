
package bgu.spl.mics;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;




/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only private fields and methods can be added to this class.
*/
public class MessageBusImpl<T> implements MessageBus {

	 private static class SingeltonHolder 
	 {
		 private static MessageBusImpl INSTANCE = new MessageBusImpl();
	 }
	
	
	private ConcurrentHashMap<MicroService, BlockingQueue <Message>> listOfQueues = new ConcurrentHashMap<MicroService, BlockingQueue <Message>>();  //string is key ( micro service)
	private ConcurrentHashMap<Class<? extends Event<?>>, BlockingQueue<MicroService>> subsToEvents = new ConcurrentHashMap<Class<? extends Event<?>>, BlockingQueue<MicroService>>();
	private ConcurrentHashMap<Class<? extends Broadcast>, BlockingQueue<MicroService>> subsToBroadcasts = new ConcurrentHashMap<Class<? extends Broadcast>, BlockingQueue<MicroService>>();
	//private ConcurrentHashMap<Class<? extends Event<?>> , BlockingQueue<MicroService>> isSubForEventEmpty = new ConcurrentHashMap< Class<? extends Event<?>> , BlockingQueue<MicroService>>();
	private ConcurrentHashMap<Event<?>, Future<?>> futures = new ConcurrentHashMap<Event<?>, Future<?>>();
	
	
	

	
	

	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) 
	{
		 
		 if(type != null && m != null) 
		 {
			 if(eventExist(type)) 
			 {
				 subsToEvents.get(type).add(m);
				 
			 }
			 else 
			 {
				 
				 subsToEvents.put(type, new LinkedBlockingQueue <MicroService>());
				 subsToEvents.get(type).add(m);
				
			 }
			 System.out.println(subsToEvents.get(type).size());
		 
		 }
		 
		 

	}

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) 
	{
		if(type != null && m != null) 
		{
			if(BroadcastExist(type))
			{
				subsToBroadcasts.get(type).add(m);
				
			}
			else 
			{
				subsToBroadcasts.put(type, new LinkedBlockingQueue <MicroService>());
				subsToBroadcasts.get(type).add(m);
				
			}
		}
		

	}

	@Override
	public <T> void complete(Event<T> e, T result) 
	{
	
		Future future=(Future)futures.get(e);
		future.resolve(result);
		

	}

	@Override
	public void sendBroadcast(Broadcast b) 
	{
		if(subsToBroadcasts.get(b) != null) 
		{
		    for(MicroService m : subsToBroadcasts.get(b)) 
		    {
			    listOfQueues.get(m).add(b);	
		    }
		}
		

	}
 
	
	@Override
	public <T> Future<T> sendEvent(Event<T> e) 
	{
		 
		Future<T> future = new Future<T>();
		
		if(subsToEvents.get(e) == null) 
		{
			return null;
		}
		
		synchronized (subsToEvents.get(e)) 
		{
			if(!subsToEvents.get(e).isEmpty()) 
			{
			    MicroService m = subsToEvents.get(e).poll(); //to check what happens when theres more threads then micros in the list  
			    listOfQueues.get(m).add(e);
			    futures.put(e, future);
			    subsToEvents.get(e).add(m);
			}
			
			else
				return null;
		}
	
		
		
		return future;
		
		
	}
		

	@Override
	public void register(MicroService m) 
	{
		if(m != null && !isRegistered(m) ) 
		{
			//System.out.println(listOfQueues.size());
		    listOfQueues.put(m, new LinkedBlockingQueue <Message>() );
		   // System.out.println(listOfQueues.size());
		}

	}

	@Override
	public void unregister(MicroService m) 
	{
		///thread safety problem: sending msg while deleting micro service
			
		
		    if(m != null && isRegistered(m)) 
		    {
                deleteMicroServiceFromSubs(m);
			
                for(Message msg: listOfQueues.get(m)) 
			    {
				    if(futures.containsKey(msg)) 
				    {
					    futures.remove(msg);
					    listOfQueues.get(m).remove(msg);
				
				    }
			        else
					    listOfQueues.remove(msg);
				
			    }
            
		    }
		
		

	}

	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException 
	{
		//if(!isRegistered(m))
				//throw new IllegalStateException();
		
		//else 
		//{
		Message ms = null;	
		synchronized (this) 
		{
			try 
			{
				wait();
			}
			catch(InterruptedException ex) 
			{
				ms =  listOfQueues.get(m).take();
				notifyAll();
			}
			
		}
		
			return ms;
			
		//}
		
		
		
		
	
	}
	
	public static MessageBusImpl getInstance() 
	{
		return SingeltonHolder.INSTANCE ;
	}
	
	private boolean isRegistered(MicroService m) 
	{
		if(listOfQueues.get(m) != null) 
		{
			
			return true;
		}
		else
			
			return false;
	}
	
	private <T> boolean eventExist(Class<? extends Event<T>> type) 
	{
		if(subsToEvents.containsKey(type)) 
		{
			return true;
		}
		
		else
			return false;
		
	}
	
	private boolean BroadcastExist(Class<? extends Broadcast> type)
	{ 
		if(subsToBroadcasts.containsKey(type)) 
		{
			return true;
		}
		else
			return false;
	}
	
	private void deleteMicroServiceFromSubs(MicroService m) 
	{
		for (Map.Entry<Class<? extends Event<?>>, BlockingQueue<MicroService>> entry : subsToEvents.entrySet()) 
		{
			if(entry.getValue().contains(m)) 
			{
				entry.getValue().remove(m);
			}
		}
		
		for (Map.Entry<Class<? extends Broadcast>, BlockingQueue<MicroService>> entry : subsToBroadcasts.entrySet()) 
		{
			if(entry.getValue().contains(m)) 
			{
				entry.getValue().remove(m);
			}
		}
		
	
		
		
	}
	
	
	
	

	

}