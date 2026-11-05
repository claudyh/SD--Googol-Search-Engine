//Gateway -------------------
package com.example.demo;

import java.rmi.*;
import java.rmi.registry.*;
import java.util.ArrayList;
import java.rmi.server.UnicastRemoteObject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


/**
 * Represents a Gateway that acts as a mediator
 * between the client and the Barrels. The Gateway
 * also creates the url Queue and the RMI connections.
 */
public class Gateway extends UnicastRemoteObject implements IGateway {

    private IQueue urlQueue;
    private IRMIBarrel b;
    private static IRMIClient g;


    //Gateway
    /**
     * Constructs a new Gateway instance.
     * Initiates the RMI for the Queue and the Barrels.
     * @throws RemoteException if an RMI error occurs.
     */
    public Gateway() throws RemoteException {
        try {
            //Create a shared queue for URLs
            urlQueue = new Queue();
            //RMI Queue
            Naming.rebind("RMIQueue", urlQueue);

            //RMI Barrel
            b = new RMIBarrel();
            Naming.rebind("BARREL", b);
            b.addClient(g);

        } catch (Exception e) { System.out.println("Exception: " + e); }
    }


    //Add Queue
    /**
     * Adds a message to the queue.
     * @param message The message to be added.
     * @throws RemoteException if an RMI error occurs.
     */
    @Override
    public void AddToQueue(String message) throws RemoteException {
        urlQueue.putQueue(message);
    }


    //Search Words
    /**
     * Searches for the client message in the Barrels.
     * @param message The message to search for.
     * @return An ArrayList of string arrays containing search results.
     * @throws RemoteException if an RMI error occurs.
     */
    @Override
    public ArrayList<String[]> SearchMessage(String message) throws RemoteException{
        ArrayList<String[]> results = b.BarrelSearchWords(message);
        return results;
    }

    
    //Search urls
    /**
     * Retrieves links associated with an url
     * (sent by the client) from the Barrels.
     * @param message The message (url) to retrieve links for.
     * @return An ArrayList of string arrays containing links.
     * @throws RemoteException if an RMI error occurs.
     */
    @Override
    public ArrayList<String[]> GetLinks(String message) throws RemoteException{
        ArrayList<String[]> results = b.LinksUrl(message);
        return results;
    }


    //Barrels info
    /**
     * Retrieves data about the Barrels.
     * @return An ArrayList of string arrays containing Barrel information.
     * @throws RemoteException if an RMI error occurs.
     */
    @Override
    public ArrayList<String[]> BarrelsData() throws RemoteException{
        ArrayList<String[]> results = b.BarrelsInfo();
        return results;
    }


    //Main
    /**
     * Main method to initiate the Gateway.
     * Also creates the RMI connection with Client.
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {

        //Get RMI port from properties file
        int RMI_PORT= 1099;
        
        try (BufferedReader br = new BufferedReader(new FileReader("properties.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts[0].trim().equals("RMI Port")) { RMI_PORT = Integer.parseInt(parts[1].trim()); }
            }
        } catch (IOException e) { e.printStackTrace(); }

        //RMI Client
        try {
            g = new RMIClient();
            LocateRegistry.createRegistry(RMI_PORT);
            Naming.rebind("GATEWAY", g);

            //Gateway
            Gateway aGateway= new Gateway();

            //Add gateway to RMI
            g.AddGateway(aGateway);

        } catch (Exception e) { System.out.println("Exception: " + e); }

    }
}