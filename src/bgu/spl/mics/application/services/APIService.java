package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;

/**
 * APIService is in charge of the connection between a client and the store.
 * It informs the store about desired purchases using {@link BookOrderEvent}.
 * This class may not hold references for objects which it is not responsible for:
 * {@link ResourcesHolder}, {@link MoneyRegister}, {@link Inventory}.
 * 
 * You can add private fields and public methods to this class.
 * You MAY change constructor signatures and even add new public constructors.
 */
public class APIIService extends MicroService{

		super("Change_This_Name");
		int i = 3;
		int x= 2
		int j = x + i;
	}

	@Override
	protected void initialize() {
		// TODO Implement this
		
	}

}
