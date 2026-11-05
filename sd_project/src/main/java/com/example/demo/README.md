## Googol Search Engine

&nbsp;

### 📋 What needs to be installed

---

In order to run the project smoothly, two libraries are required: **json** and **jsoup**. Once properly configured within the current IDE, the project is ready for compilation and execution.

**Note:** In VSCode, the libraries just need to be added to the "Referenced libraries".

&nbsp;

### 📋 How to compile

---

First run:

_./mvnw compile_

The project can be compiled in the following ways:

**1 - Using the user console:** In the user console, execute the command "javac Class.java" for each class. Interface classes do not need to be compiled separately, as they are compiled along with their main class. For example:

_javac Client.java RMIClient.java Gateway.java RMIBarrel.java Queue.java_

**2 - Using an IDE:** Depending on your IDE, you may compile classes by running them. In VSCode, for instance, we compile the classes "Barrel.java" and "Downloader.java" by clicking the run button to avoid conflicts with imported libraries.

**Note:** Before running the "Barrel.java" and "Downloader.java" classes, ensure that the Gateway class is running.

&nbsp;

### 📋 How to run

---

The project can be run in the following ways:

**1 - Using the user console:** In the user console, execute the command "java Class.java" for each class. Interface classes do not need to be run separately. The Gateway class must be the first class to run. Afterward, you may create as many clients as needed on new consoles. Commands:

_java Gateway.java_

_java Client.java_

**2 - Using an IDE:** Simply click on the run button in the IDE. You can do this for "Barrel.java" and "Downloader.java" and create as many barrels and downloaders on different consoles to test the software.

## &nbsp;

**NOTE:** Please note that to execute the commands in the console, you must be in the directory of the code (src).

### 📋 How to run the frontend

---

Finally, to run the webpage, you must run "./mvnw spring-boot:run" after running the gateway.
