//Barrel Interface -------------------
package com.example.demo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;


/**
 * Represents the Barrel interface, which defines the methods available for searching words and links.
 */
public interface IBarrel extends Remote {
    public ArrayList<String[]> SearchWords(String words) throws RemoteException;
    public ArrayList<String[]> SearchLinks(String link) throws RemoteException;
    public void LoadFile(int ID) throws RemoteException;
}