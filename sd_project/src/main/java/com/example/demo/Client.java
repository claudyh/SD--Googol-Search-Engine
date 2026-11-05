//Client -------------------
package com.example.demo;

import java.rmi.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


/**
 * Represents a Client for interacting with the Googol system.
 */
public class Client{

    //Main
    /**
     * Main method to run the client application. Prints the
     * client menu and connects to the Gateway Client RMI.
     * Gets the searches results grouped by ten and performs
     * checks on the user's input.
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {

        String input;

        try {
            //Client Menu
            System.out.println("\n- - - - - - - - - - - - - - - - - G O O G O L - - - - - - - - - - - - - - - - -\n");
            System.out.println("1- Add Url     2- Search Word     3- Links to URLS     4- Admin Page     5- Exit\n");
            try (Scanner scanner = new Scanner(System.in)) {
                //Get RMI data from properties file
                String RMI_DATA= "localhost";
                    
                try (BufferedReader br = new BufferedReader(new FileReader("properties.txt"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(":");
                        if (parts[0].trim().equals("RMI Address")) { RMI_DATA = parts[1].trim(); }
                        if (parts[0].trim().equals("RMI Port")) { RMI_DATA = RMI_DATA + ":" + Integer.parseInt(parts[1].trim()); }
                    }
                } catch (IOException e) { e.printStackTrace(); }

                //RMI
                IRMIClient g = (IRMIClient) Naming.lookup("rmi://" + RMI_DATA + "/GATEWAY");
    
                while (true) {
                    System.out.print("\n- - - - - - -\nEnter option: ");
                    input= scanner.nextLine();
                    int option= 6;

                    try{
                        option= Integer.parseInt(input);
                    } catch(NumberFormatException e){
                        System.out.println("\nYou must insert a number");
                        continue;
                    }

                    //Check if valid
                    if(option < 1 || option > 5){
                        System.out.println("\nOption not valid");
                        continue;
                    }

                    //Check exit
                    if(option == 5){
                        System.out.println("\nSee you soon!\n");
                        break;
                    } else {

                        //Get new input
                        if(option == 1 || option == 3){
                            System.out.print("Enter url: ");
                            input= scanner.nextLine();

                        } else if(option == 2){
                            System.out.print("Enter words: ");
                            input= scanner.nextLine();
                        }

                        //Get result
                        ArrayList<String[]> result = g.sendMessage(input, option);
                        System.out.print("\n");

                        if(option == 2){

                            System.out.println("Results:\n");

                            //Print result
                            if (result.isEmpty()) {
                                System.out.println("Not found");
                            } else {
                                int count = 0;
                                int number = 0;

                                for (String[] info : result) {
                                    if (number % 10 == 0) { System.out.println("- - -\n"); }
                                    System.out.println(number);
                                    for (String value : info) {
                                        count++;
                                        value = value.replace("\n", " ");
                                        if(count % 4 != 0) { System.out.print(value + "\n"); }
                                    }
                                    System.out.println();
                                    number++;
                                }
                            }
                        } else if(option == 3){
                            System.out.println("Results:");

                            //Print result
                            if (result.isEmpty()) {
                                System.out.println("Not found");
                            } else {
                                int number = 0;

                                for (String[] info : result) {
                                    if (number % 10 == 0) { System.out.println("\n- - -\n"); }
                                    for (String value : info) {
                                        value = value.replace("\n", " ");
                                        System.out.println(value);
                                    }
                                    number++;
                                }
                            }
                        } else{
                            //Print result
                            if (result.isEmpty()) {
                                System.out.println("Not found");
                            } else {
                                for (String[] info : result) {
                                    for (String value : info) {
                                        System.out.print(value + " ");
                                    }
                                    System.out.println();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in Client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}