//Queue -------------------
package com.example.demo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Represents a queue for managing URLs.
 */
public class Queue extends UnicastRemoteObject implements IQueue {
    
    ConcurrentLinkedQueue<String> urlQueue;
    

    //Queue
    /**
     * Constructs a new Queue instance.
     * @throws RemoteException if an RMI error occurs.
     */
    public Queue() throws RemoteException {
        super();
        this.urlQueue = new ConcurrentLinkedQueue<>();
    }


    //Insert url
    /**
     * Inserts a URL into the queue.
     * @param url The URL to be inserted.
     */
    @Override
    public synchronized void putQueue(String url) {
        urlQueue.add(url);
        notify();
    }


    //Get url
    /**
     * Removes and returns a URL from the queue.
     * Waits if the queue is empty.
     * @return The URL removed from the queue.
     */
    @Override
    public synchronized String popQueue() {
        while (urlQueue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        return urlQueue.poll();
    }


    //Empty
    /**
     * Checks if the queue is empty.
     * @return true if the queue is empty, false otherwise.
     */
    @Override
    public synchronized boolean emptyQueue() {
        return urlQueue.isEmpty();
    }
}