//IRMI Barrel -------------------
package com.example.demo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;


/**
 * Represents the IRMIBarrel interface, which defines the methods available for managing Barrels.
 */
public interface IRMIBarrel extends Remote {
    public int NewBarrel(IBarrel barrel) throws RemoteException;
    public void RipBarrel(int idBarrel) throws RemoteException;
    public ArrayList<String[]> BarrelSearchWords(String input) throws RemoteException;
    public ArrayList<String[]> LinksUrl(String message) throws RemoteException;
    public ArrayList<String[]> BarrelsInfo() throws RemoteException;
    public void BarrelNewMessage(int idBarrel) throws RemoteException;
    public void addClient(IRMIClient client) throws RemoteException;
}