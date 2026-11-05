package com.example.demo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface IWebController extends Remote {
    public void AdminPageWarning(ArrayList<String[]> adminInfo) throws RemoteException;
}
