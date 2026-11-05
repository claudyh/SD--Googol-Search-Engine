//IRMI Client -------------------
package com.example.demo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;


/**
 * Represents the IRMIClient interface, which defines the methods
 * available for interacting with the client by RMI.
 */
public interface IRMIClient extends Remote {
    public void AddGateway(Gateway inputGateway) throws RemoteException;
    public ArrayList<String[]> sendMessage(String input, int option) throws RemoteException;
    public void NewServer(IWebController server) throws RemoteException;
    public void AdminPage() throws RemoteException;
    public void addStory(String url) throws RemoteException;
}