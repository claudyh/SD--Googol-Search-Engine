//Queue Interface -------------------
package com.example.demo;

import java.rmi.Remote;
import java.rmi.RemoteException;


/**
 * Represents the Queue interface, which defines the methods
 * available for managing a queue of URLs.
 */
public interface IQueue extends Remote{
    public void putQueue(String url) throws RemoteException;
    public String popQueue() throws RemoteException;
    public boolean emptyQueue() throws RemoteException;
}