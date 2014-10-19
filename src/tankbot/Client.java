package tankbot;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

/**
 *
 * @author Luke
 */
public class Client {

    Properties properties;
    private Controller joystick;
    private Socket socket;
    private ObjectOutputStream out;
    //for updating motor state from joystick
    private final Timer motorStateTimer;
    //how often to poll for new motor state to send to pi
    private final static long MOTOR_STATE_UPDATE_PERIOD_MS = 100;
    private final MotorState motorState;

    //semi abstracted so more motors could be added later (eg for turret)
    private final static int NUM_MOTORS = 2;

    private boolean connected = false;

    Client(Properties properties, boolean debug) {

        this.properties = properties;

        this.joystick = null;
        //crudely find a joystick
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

        for (Controller c : controllers) {
            System.out.println(c.getName());
            if (c.getType().equals(Controller.Type.STICK) || c.getType().equals(Controller.Type.GAMEPAD)) {
                joystick = c;
                System.out.println("Found joystick: " + joystick.getName());
            }
        }

        this.motorState = new MotorState(this.joystick, this.properties);

        this.motorStateTimer = new Timer("MotorStateTimer");
        this.motorStateTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                sendLatestMotorState();
            }
        }, (long) 0, MOTOR_STATE_UPDATE_PERIOD_MS);

        if (debug) {
            for (Controller controller : controllers) {
                /* Get the name of the controller */
                System.out.println(controller.getName());
                System.out.println("Type: " + controller.getType().toString());
                /* Get this controllers components (buttons and axis) */
                Component[] components = controller.getComponents();
                System.out.println("Component Count: " + components.length);
                for (int j = 0; j < components.length; j++) {

                    /* Get the components name */
                    System.out.println("Component " + j + ": " + components[j].getName());

                    System.out.println("    Identifier: " + components[j].getIdentifier().getName());
                    System.out.print("    ComponentType: ");
                    if (components[j].isRelative()) {
                        System.out.println("Relative");
                    } else {
                        System.out.println("Absolute");
                    }
                    if (components[j].isAnalog()) {
                        System.out.println(" Analog");
                    } else {
                        System.out.println(" Digital");
                    }
                }
            }
        }

    }

    /**
     * Connect (or reconnect) to an IP and port
     * Synchronized so if we try to connect to somewhere that doesn't exist, we
     * don't try and connect somewhere else in parallel
     * 
     * TODO refactor so if a new connection attempt is made the old one can be aborted, rather than waiting for it to time out
     *
     * @param serverIp
     * @param _port
     */
    public synchronized void connectTo(String serverIp, int _port) {
        System.out.println("Attempting to connect to " + serverIp + ":" + _port);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        socket = GetSocket(serverIp, _port);
        if (socket != null) {
            try {

                out = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("Connected to " + serverIp + ":" + _port);
                this.connected = true;

            } catch (IOException e) {
                System.err.println("Couldn't get I/O for the connection: " + e.getMessage());
            }
        }
    }

    public static Socket GetSocket(String host, int port) {
        InetAddress address = null;
        Socket _socket = null;

        try {
            address = InetAddress.getByName(host);

        } catch (UnknownHostException e) {
            System.out.println("Failed to find " + host + " " + e.getMessage());
        }
        if (address != null) {
            try {
                _socket = new Socket(address, port);
//                _socket = new Socket();
//                _socket.setSoTimeout(100);
//                _socket.connect(new InetSocketAddress(address, port));
//                _socket.setSoTimeout(0);
            } catch (IOException e) {
                System.out.println("Failed to connect to " + host + ":" + port + " " + e.getMessage());
            }

        }
        return _socket;
    }

    private void sendObject(Object o) {
        try {
            if (socket == null) {
                System.out.println("help");
            }
            if (o == null) {
                System.out.println("help2");
            }
            if (out != null && !socket.isOutputShutdown()) {
                out.writeObject(o);
            }
        } catch (IOException ex) {
            System.out.println("Lost connection to " + this.socket.toString());
            this.connected = false;
        }
    }

    public void sendLatestMotorState() {
        this.motorState.update();
        if (this.connected) {
            for (int i = 0; i < NUM_MOTORS; i++) {
                sendObject(this.motorState.getMotorCommand(i));
            }
        }
    }

    public Controller getJoystick() {
        return this.joystick;
    }

    public MotorState getMotorState() {
        return this.motorState;
    }
}
