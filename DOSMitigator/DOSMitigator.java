package net.floodlightcontroller.dosmitigator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

public class DOSMitigator implements IFloodlightModule, IOFMessageListener {

	protected static Logger logger; 
	protected int counter = 0;
	protected Timer timer;
	protected IOFSwitch sw1; 
	protected Map<IOFSwitch, Integer> switchMap;
	protected IFloodlightProviderService iFloodlightProviderService;

	@Override
	public String getName() {
		return DOSMitigator.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}
	
	public void incrementCounter(IOFSwitch sw) {
		Integer ctr = switchMap.get(sw);
		ctr++;
		switchMap.put(sw, ctr);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
				
		if(switchMap.containsKey(sw)) {
			incrementCounter(sw); 
		} else {
			logger.info("adding new switch to switchMap: {}", sw.getId());
			switchMap.put(sw, 1);
		}
		
		counter++;
		sw1 = sw;
		
		return Command.CONTINUE;

	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> col = 
				new ArrayList<Class<? extends IFloodlightService>>();
		col.add(IFloodlightProviderService.class);
		return col;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		switchMap = new HashMap<>();
		counter = 0;
		timer = new Timer();
		timer.schedule(new BandwidthConsumptionMonitor(), 0, 1000);
		logger = LoggerFactory.getLogger(DOSMitigator.class);
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
	}
	
	private class BandwidthConsumptionMonitor extends TimerTask { 
		public void run() { 
			for(IOFSwitch sw: switchMap.keySet()) {
				//logs for testing and debugging
				//logger.info("switch {} count: {}", sw.getId(), switchMap.get(sw));
				
	            if (switchMap.get(sw) > 100) {
					//logs for testing and debugging
					//logger.info("number of switches in switchMap: {}", switchMap.keySet().size());

	            	logger.info("Disconnecting switch with dpid {} now", sw.getId());
	            	
	            	String removeSwitch = "echo floodlight | sudo -S ovs-vsctl --if-exists del-br s" + sw.getId().getLong(); 
	            	
	            	try {
	                    Process process = Runtime.getRuntime().exec(new 
	                    		String[]{"bash","-c",removeSwitch}); 
	                    BufferedReader stdInput = new BufferedReader(new  
	                    		InputStreamReader(process.getInputStream())); 
	                    BufferedReader stdError = new BufferedReader(new 
	                    		InputStreamReader(process.getErrorStream()));
	                    
	                    //read the output from the command 
	                    System.out.println("Here is the standard output of the command:\n"); 
	                    String s; 
	                    while ((s = stdInput.readLine()) != null) { 
	                    	System.out.println(s);
	                    }
	                    
	                    //read the command errorstream
	                    System.out.println("Here is the standard output of the command error stream:\n"); 
	                    
	                    while ((s = stdError.readLine()) != null) { 
	                    	System.out.println(s);
	                    }
	            	} catch(IOException e) {
	            		e.printStackTrace();
	            	}
	            	
	            	switchMap.remove(sw);
	            }
			}
			for(IOFSwitch sw : switchMap.keySet()) {
				switchMap.put(sw, 0);
			}
      counter = 0;
		}                   		 
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

}

