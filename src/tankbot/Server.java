package tankbot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens for connections over a TCP socket, and then uses pi-blaster to set
 * motors to the requested states
 *
 * @author Luke
 */
public class Server {

    private ServerSocket listener;
//    private Socket socket;
    private final int port;
    private static final int MAX_CONNECTIONS = 1;

    //if true, debug mode spits out commands to pi blaster
    private static final boolean DEBUG_PI_BLASTER = false;

    //this maps which GPIO pins on the Pi control which inputs on the H-bridges for each motor
    public static final MotorPinMap[] motorPinMaps = new MotorPinMap[]{new MotorPinMap(4, 17, 18), new MotorPinMap(21, 22, 23)};

    public static final int NUM_MOTORS = motorPinMaps.length;

    private PrintWriter piBlasterWriter;
    private boolean debug;

    //for storing last motor commands for debug output
    private MotorCommand[] debugMotors = new MotorCommand[]{new MotorCommand(0, false, 0), new MotorCommand(1, false, 0)};
    private static final int DEBUG_BARS = 20;

    public Server(int _port, boolean debug) {

        try {
            //open up the pi blaster file for writing
            this.piBlasterWriter = new PrintWriter(new File("/dev/pi-blaster"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(0);
        }

        this.debug = debug;
        this.port = _port;
        try {
            this.listener = new ServerSocket(port, MAX_CONNECTIONS);
            while (true) {
                //this blocks until a connection is received
                System.out.println("listening on port " + port);
                //this blocks until the socket dies
                try (Socket socket = listener.accept()) {
                    //this blocks until the socket dies
                    System.out.println("Client connected (" + socket.toString() + ")");
                    serviceClient(socket);
                    stopAll();
                }
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

    }

    /**
     * Given a port, deal with instructions received
     *
     * @param socket
     */
    private void serviceClient(Socket socket) {
        ObjectInputStream in;
        try {
            System.out.println(socket.getInetAddress().toString() + " connected");
            in = new ObjectInputStream(socket.getInputStream());
            Object message = null;
            do {
                try {
                    message = in.readObject();
                    input(message);

                } catch (ClassNotFoundException ex) {
                    System.err.println("Data received in unknown format");
                } catch (IOException ex) {
                    System.out.println("Client lost (" + socket.toString() + ")");

                    return;
                }
            } while (message != null);

        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Process messages received over the socket
     *
     * @param message
     */
    private void input(Object message) {
        if (message == null) {
            return;
        }
        if (message.getClass().equals(MotorCommand.class)) {
            MotorCommand motor = (MotorCommand) message;
            //process motor command!
            setMotor(motor);
        }
    }

    /**
     * Set a pin between 0 and 1
     *
     * @param pin
     * @param value
     */
    private void setPin(int pin, float value) {
        this.piBlasterWriter.println(String.format("%d=%.2f", pin, value));
        this.piBlasterWriter.flush();
        if (DEBUG_PI_BLASTER && debug) {
            System.out.println(String.format("%d=%.2f", pin, value));
        }
    }

    /**
     * Set a pin to zero or one
     *
     * @param pin
     * @param high
     */
    private void setPin(int pin, boolean high) {
        this.piBlasterWriter.println(String.valueOf(pin) + "=" + (high ? "1" : "0"));
        this.piBlasterWriter.flush();
        if (DEBUG_PI_BLASTER && this.debug) {
            System.out.println(String.valueOf(pin) + "=" + (high ? "1" : "0"));
        }
    }

    /**
     * Stop all motors
     */
    private void stopAll() {
        setMotor(new MotorCommand(0, false, 0));
        setMotor(new MotorCommand(1, false, 0));
    }

    private void setMotor(MotorCommand motorCommand) {
        if (motorCommand.motor < NUM_MOTORS && motorCommand.speed >= -1 && motorCommand.speed <= 1) {
            //valid motor and valid speed

            if (motorCommand.brake) {
                //set both inputs to ground - still use speed for enable, as we might not be using the full brake
                setPin(motorPinMaps[motorCommand.motor].getIn1GPIO(), false);
                setPin(motorPinMaps[motorCommand.motor].getIn2GPIO(), false);

            } else {

                if (motorCommand.speed < 0) {
                    //going backwards
                    //set in1 low and in2 high
                    setPin(motorPinMaps[motorCommand.motor].getIn1GPIO(), false);
                    setPin(motorPinMaps[motorCommand.motor].getIn2GPIO(), true);

                } else {
                    //forwards, in1 high and in2 low
                    setPin(motorPinMaps[motorCommand.motor].getIn1GPIO(), true);
                    setPin(motorPinMaps[motorCommand.motor].getIn2GPIO(), false);
                }
            }

            setPin(motorPinMaps[motorCommand.motor].getEnableGPIO(), Math.abs(motorCommand.speed));
        }

        if (this.debug && !DEBUG_PI_BLASTER) {
            this.debugMotors[motorCommand.motor] = motorCommand;

            String barString = "\r";
            for (MotorCommand m : this.debugMotors) {

                barString += "[";

                //convert speed into a number between 0 and DEBUG_BARS 
                int bars = Math.round(m.speed * DEBUG_BARS / 2 + DEBUG_BARS / 2);

                for (int i = 0; i < DEBUG_BARS; i++) {
                    if (i <= bars) {
                        barString += "=";
                    } else {
                        barString += " ";
                    }
                }

                barString += "] ";
            }
            System.out.print(barString);
        }
    }
}
