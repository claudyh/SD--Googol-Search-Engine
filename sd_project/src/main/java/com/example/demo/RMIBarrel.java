//RMI Barrel -------------------
package com.example.demo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Represents an RMI bridge between the Gateway and the Barrels.
 */
public class RMIBarrel extends UnicastRemoteObject implements IRMIBarrel {

    private static IRMIClient c;
    private HashMap<Integer, Integer> barrelMessages;
    private HashMap<Integer, Integer> barrelSearches;
    private HashMap<Integer, HashSet<Long>> barrelTimes;
    private HashMap<Integer, IBarrel> activeBarrels;
    private ArrayList<Integer> crashedBarrels;
    private int indexBarrel;
    private ScheduledExecutorService scheduler;


    //RMI Barrel
    /**
     * Constructs a new RMIBarrel instance and creates
     * the data structures to store Barrel's infos.
     * @throws RemoteException if an RMI error occurs.
     */
    public RMIBarrel() throws RemoteException {
        super();
        this.barrelMessages = new HashMap<>();
        this.barrelSearches = new HashMap<>();
        this.barrelTimes = new HashMap<>();
        this.activeBarrels = new HashMap<>();
        this.crashedBarrels = new ArrayList<>();
        this.indexBarrel = 0;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.startMonitoring();
    }


    //Client
    /**
     * Add RMI client.
     * @param client The RMI client to be added.
     * @throws RemoteException if an RMI error occurs.
     */
    public void addClient(IRMIClient client) throws RemoteException {
        c= client;
    }


    //Save barrel
    /**
     * Adds a new Barrel to the "activeBarrels" hashmap
     * if there are no crashed barrels. Otherwise, it
     * restores a random crashed Barrel.
     * @param barrel The Barrel to be added.
     * @return The ID of the added Barrel.
     * @throws RemoteException if an RMI error occurs.
     */
    @Override
    public int NewBarrel(IBarrel barrel) throws RemoteException {
        
        //Current ID
        int ID= indexBarrel;

        try{
            //Check if there's crashed barrels
            if(crashedBarrels.isEmpty()){
                //New barrel ---

                //Increase barrel number
                indexBarrel += 1;
                //Add new barrel
                activeBarrels.put(ID, barrel);
                //Add to searches and messages
                barrelSearches.put(ID, 0);
                barrelMessages.put(ID, 0);
                System.out.println("BARREL ADDED! ID: " + ID);

                //Sincronize with existing barrels
                checkBarrelMulticast();

            } else{
                //Restore status ---

                //Choose random crashed barrel
                Random random = new Random();
                int randomIndex = random.nextInt(crashedBarrels.size());
                ID = crashedBarrels.get(randomIndex);
                //Restore barrel
                crashedBarrels.remove(Integer.valueOf(ID));
                activeBarrels.put(ID, barrel);
                System.out.println("BARREL RESTORED! ID: " + ID);
            }
        } catch (Exception e) { e.printStackTrace(); }
        
        //Alert changes
        c.AdminPage();
        return ID;
    }


    //Remove barrel
    /**
     * Removes a Barrel from the "activeBarrels" hashmap
     * and add's it to the "crashedBarrels" array.
     * @param idBarrel The ID of the Barrel to be removed.
     * @throws RemoteException if an RMI error occurs.
     */
    @Override
    public void RipBarrel(int idBarrel) throws RemoteException {
        try{
            //Add to crash barrels
            crashedBarrels.add(idBarrel);
            //Remove from active
            activeBarrels.remove(idBarrel);
            System.out.println("BARREL DIED! ID: " + idBarrel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Alert changes
        c.AdminPage();
    }


    //Search message
    /**
     * Searches words in the active Barrels. Chooses the active
     * barrel with less searches and gets the information from
     * it. It also stores the time taken by the Barrel to search.
     * @param message The words to search.
     * @return An ArrayList containing the search results.
     * @throws RemoteException if an RMI error occurs.
     */
    @Override
    public ArrayList<String[]> BarrelSearchWords(String message) throws RemoteException {

        //Initialize string array
        ArrayList<String[]> result = new ArrayList<>();

        if(!activeBarrels.isEmpty()){

            //Choose barrel with less searches
            List<Integer> keyList = new ArrayList<>(activeBarrels.keySet());
            //Sort by searches (ascending)
            keyList.sort(Comparator.comparingInt(barrelSearches::get));
            //Get barrel id
            int ID = keyList.get(0);

            //Get barrel and add search
            IBarrel currentBarrel= activeBarrels.get(ID);
            barrelSearches.put(ID, barrelSearches.get(ID) + 1);
            System.out.println("Searching with barrel " + ID);

            //Count time
            long startTime = System.currentTimeMillis();

            //Search word
            try {
                //Debug ---
                //try { Thread.sleep((long) (Math.random() * 10000)); } catch (InterruptedException e) { }
                result = currentBarrel.SearchWords(message);
            } catch (RemoteException e) {
                //If Barrel crashed, search with another barrel
                result = BarrelSearchWords(message);
            }

            //Save time
            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            
            if(barrelTimes.containsKey(ID)){
                //Add time
                HashSet<Long> times = barrelTimes.get(ID);
                times.add(elapsedTime);
                barrelTimes.put(ID, times);
            } else{
                //First barrel time
                HashSet<Long> times = new HashSet<>();
                times.add(elapsedTime);
                barrelTimes.put(ID, times);
            }

        } else{
            System.err.print("No barrels active\n");
        }

        //Alert changes
        c.AdminPage();
        return result;
    }


    //Search links
    /**
     * Searches links in the active Barrels. Chooses the active
     * barrel with less searches and gets the links from it.
     * @param message The link to search.
     * @return An ArrayList containing the search results.
     * @throws RemoteException if an RMI error occurs.
     */
    @Override
    public ArrayList<String[]> LinksUrl(String message) throws RemoteException {

        //Initialize string array
        ArrayList<String[]> result = new ArrayList<>();

        if(!activeBarrels.isEmpty()){

            //Choose barrel with less searches
            List<Integer> keyList = new ArrayList<>(activeBarrels.keySet());
            //Sort by searches (ascending)
            keyList.sort(Comparator.comparingInt(barrelSearches::get));
            //Get barrel id
            int ID = keyList.get(0);

            //Get barrel and add search
            IBarrel currentBarrel= activeBarrels.get(ID);
            barrelSearches.put(ID, barrelSearches.get(ID) + 1);
            System.out.println("Searching with barrel " + ID);

            //Search links
            result = currentBarrel.SearchLinks(message);
        } else{
            System.err.print("No barrels active\n");
        }

        //Alert changes
        c.AdminPage();
        return result;
    }


    //Search barrels info
    /**
     * Retrieves information about which Barrels are active and
     * the average time of searches by each one.
     * @return An ArrayList containing the Barrel information.
     * @throws RemoteException if an RMI error occurs.
     */
    @Override
    public ArrayList<String[]> BarrelsInfo() throws RemoteException {

        //Initialize string array
        ArrayList<String[]> result = new ArrayList<>();

        //Active barrels
        if(!activeBarrels.isEmpty()){

            //There's active barrels
            for (Integer barrelId : activeBarrels.keySet()) {
                
                //Get barrel times
                HashSet<Long> times = barrelTimes.get(barrelId);
    
                //Calculate average time
                double averageTime = 0;
                if (times != null && !times.isEmpty()) {
                    long totalTime = 0;
                    for (Long time : times) {
                        totalTime += time;
                    }
                    averageTime = (double) totalTime / times.size();
                }
    
                //Add barrel info
                String[] barrelInfo = new String[]{"Barrel ID: " + barrelId, "Average Time: " + averageTime};
                result.add(barrelInfo);
            }
        } else{

            //No active barrels
            result.add(new String[] {"No barrels active"});
        }

        return result;
    }


    //Barrel number messages
    /**
     * Increases the message count for the specified Barrel ID.
     * @param idBarrel The ID of the Barrel for which a new message is received.
     * @throws RemoteException If a remote communication error occurs.
     */
    @Override
    public void BarrelNewMessage(int idBarrel) throws RemoteException{
        barrelMessages.put(idBarrel, barrelMessages.get(idBarrel) + 1);
    }


    //Start Monitoring
    /**
     * Starts monitoring the messages of active Barrels every 15 seconds.
     */
    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(this::checkBarrelMulticast, 0, 15, TimeUnit.SECONDS);
    }


    //Do Multicast checks
    /**
     * Checks if every barrel has the same number of messages gotten by multicast.
     * If multiple Barrels are active, it checks if any Barrel has fewer messages
     * compared to the Barrel with the maximum number of messages. If so, it loads
     * the missing messages to that Barrel.
     */
    private void checkBarrelMulticast() {
        System.out.println("Barrel Messages:");
        int maxMessages = 0;
        int maxID = -1;

        //Check if active barrels is not empty
        if(activeBarrels.size() > 1){

            //There's active barrels
            for (Integer barrelId : activeBarrels.keySet()) {
                System.out.println("Barrel ID: " + barrelId + ", Messages: " + barrelMessages.get(barrelId));

                //Store max number of messages
                if(barrelMessages.get(barrelId) > maxMessages){
                    maxID= barrelId;
                    maxMessages= barrelMessages.get(barrelId);
                }
            }

            for (Integer barrelId : activeBarrels.keySet()) {
                if(barrelMessages.get(barrelId) < maxMessages){
                    System.out.println("Barrel " + barrelId + " has less messages!!!");
                    IBarrel missingBarrel = activeBarrels.get(barrelId);
                    //Load missing messages
                    try{
                        missingBarrel.LoadFile(maxID);
                    } catch(RemoteException e){
                        System.err.println(e);
                    }
                    barrelMessages.put(barrelId, maxMessages);
                }
            }
        }
    }
}