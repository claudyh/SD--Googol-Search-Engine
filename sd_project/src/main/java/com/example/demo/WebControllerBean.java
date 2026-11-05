package com.example.demo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class WebControllerBean extends UnicastRemoteObject implements IWebController {

    @Autowired
    private SimpMessagingTemplate template;

    protected WebControllerBean() throws RemoteException {
        super();
        System.out.println("\n\n\nCREATED BEAN\n\n\n");
    }

    @Override
    public void AdminPageWarning(ArrayList<String[]> adminInfo){

        StringBuilder stringBuilder = new StringBuilder();
        for (String[] info : adminInfo) {
            for (String value : info) {
                stringBuilder.append(value).append(" ");
            }
            stringBuilder.append("\n");
        }
        String adminInfoString = stringBuilder.toString();
        System.out.println(adminInfoString);

        //Store on websockect queue
        template.convertAndSend("/topic/messages", new Message(adminInfoString));
    }
    
}