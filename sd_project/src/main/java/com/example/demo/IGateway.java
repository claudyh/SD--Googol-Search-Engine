//Gateway Interface -------------------
package com.example.demo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;


/**
 * Represents the Gateway interface, which defines the methods available for adding
 * to the queue, searching messages, retrieving links, and obtaining Barrel data.
 */
public interface IGateway extends Remote {
    public void AddToQueue(String message) throws RemoteException;
    public ArrayList<String[]> SearchMessage(String message) throws RemoteException;
    public ArrayList<String[]> GetLinks(String message) throws RemoteException;
    public ArrayList<String[]> BarrelsData() throws RemoteException;
}