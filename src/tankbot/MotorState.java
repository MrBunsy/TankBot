package tankbot;

import LukesBits.Vector;
import java.awt.event.KeyEvent;
import java.util.Properties;
import net.java.games.input.Component;
import net.java.games.input.Controller;

/**
 *
 * @author Luke
 *
 * Keeps track of desired state of all motors. getMotorCommand() provides a
 * struct-like object which specifies the desired state of a specific motor
 *
 * Call update() regularly to ensure that the state of the motors reflects the
 * state of the joystick.
 */
public class MotorState {

    //absolute max is set in the config file, throttle can only adjust up to absolutemax
    private final float absoluteMaxSpeed, maxBrake;
    private float maxSpeed;
    private final double m1ForwardsAdjust, m1BackwardsAdjust, m0ForwardsAdjust, m0BackwardsAdjust;
    private final Controller joystick;
    private Component xAxis, yAxis, zAxis, throttle, handbrake, trigger;
    private final float deadZone;
    private final Properties properties;

    private final boolean invertX, invertY, invertZ;

    private MotorCommand m0Command = new MotorCommand(0, false, 0), m1Command = new MotorCommand(1, false, 0);

    public MotorState(Controller _joystick, Properties _properties) {

        properties = _properties;

        maxBrake = Float.parseFloat(properties.getProperty("maxBrake", "0.5"));

        absoluteMaxSpeed = Float.parseFloat(properties.getProperty("maxSpeed", "1"));

        //assume full throttle to begin
        this.maxSpeed = absoluteMaxSpeed;

        //how much to adjust the motors so they are the same speed as each other
        m1ForwardsAdjust = 1;
        m1BackwardsAdjust = 1;
        m0ForwardsAdjust = 1;
        m0BackwardsAdjust = 1;

        deadZone = Float.parseFloat(properties.getProperty("deadzone", "0.1"));

        joystick = _joystick;
        //TODO if this is ever distributed beyond me
        //have a fully configurable joystick options

        if (joystick != null) {
            Component[] components = joystick.getComponents();

            for (Component component : components) {
                if (component.isAnalog() && component.getIdentifier().getName().equalsIgnoreCase(properties.getProperty("xAxis", "x"))) {
                    xAxis = component;
                }
                if (component.isAnalog() && component.getIdentifier().getName().equalsIgnoreCase(properties.getProperty("yAxis", "y"))) {
                    yAxis = component;
                }
                if (component.isAnalog() && component.getIdentifier().getName().equalsIgnoreCase(properties.getProperty("zAxis", "rz"))) {
                    //this is if the stick is being rotated
                    zAxis = component;
                }
                if (component.isAnalog() && component.getIdentifier().getName().equalsIgnoreCase(properties.getProperty("throttle", "slider"))) {
                    throttle = component;
                }
                if (component.getIdentifier().getName().equals(properties.getProperty("trigger", "0"))) {
                    trigger = component;
                }
                if (component.getIdentifier().getName().equals(properties.getProperty("handbreak", "1"))) {
                    handbrake = component;
                }
            }
        }

        if (handbrake == null || trigger == null) {
            System.err.println("Could not find trigger or handbreak");
        }
        if (xAxis == null || yAxis == null) {
            System.err.println("Could not find x and y axis");
        }

        if (zAxis == null) {
            System.err.println("Could not find z axis");
        }

        //linux/joydev needs to be true, win7 
        this.invertX = Boolean.parseBoolean(properties.getProperty("invertX", "false"));
        //needs to be true on both
        this.invertY = Boolean.parseBoolean(properties.getProperty("invertY", "true"));
        //false on both
        this.invertZ = Boolean.parseBoolean(properties.getProperty("invertZ", "false"));

    }

    private boolean forwards, backwards, left, right, anticlockwise, clockwise, stop;

    public void update() {

        double m0Speed = 0;
        //boolean m0Forwards=true;
        boolean m0Brake = false;//default to not break and 0 speed so they should coast

        //if break is true then the speed is the break strength
        boolean keypressed = false;
        double m1Speed = 0;
        //boolean m1Forwards=true;
        boolean m1Brake = true;
        //really crude for now
        if (stop) {
            m0Speed = maxBrake;
            m1Speed = maxBrake;
            m0Brake = true;
            m1Brake = true;
            keypressed = true;
        } else if (forwards) {
            m0Speed = absoluteMaxSpeed;
            m1Speed = absoluteMaxSpeed;
            keypressed = true;
        } else if (backwards) {
            m0Speed = -absoluteMaxSpeed;
            m1Speed = -absoluteMaxSpeed;
            keypressed = true;
        } else if (left) {
            m1Speed = absoluteMaxSpeed;
            keypressed = true;
        } else if (right) {
            m0Speed = absoluteMaxSpeed;
            keypressed = true;
        } else if (clockwise) {
            m1Speed = -absoluteMaxSpeed;
            m0Speed = absoluteMaxSpeed;
            keypressed = true;
        } else if (anticlockwise) {
            m1Speed = absoluteMaxSpeed;
            m0Speed = -absoluteMaxSpeed;
            m0Brake = false;
            m1Brake = false;
            keypressed = true;
        }
        if (joystick != null && !keypressed) {

            m1Brake = false;
            m0Brake = false;

            joystick.poll();
            float x = xAxis.getPollData() * (invertX ? -1 : 1);
            float y = yAxis.getPollData() * (invertY ? -1 : 1);
//            float z = zAxis.getPollData() * (invertZ ? -1 : 1);

            if (throttle != null) {
                float t = throttle.getPollData();

                //um, not sure why divided by 2
                this.maxSpeed = (1 - t) * (float) absoluteMaxSpeed / 2;
            }
            //setting my own deadzone because I don't think MS's win7 drivers
            //allow this to be done for the sidewinder anymore
            if (Math.abs(x) < deadZone) {
                x = 0;
            }
            if (Math.abs(y) < deadZone) {
                y = 0;
            }

//            if (Math.abs(z) < deadZone) {
//                z = 0;
//            }
            Vector joy = new Vector(x, y);

            //scale the whole lot up so forwards and backwards are at full speed
//            joy = joy.multiply(Math.sqrt(2));
            double joySize = joy.getMagnitude();
            if (joySize > 1) {
                //the corners can mean we end up with a magnitude greater than one
                //this way, going into the corner is the same as moving in that
                //direction but being the same distance from the centre as the top/bottom/left/right
//                joy = joy.multiply(1 / joySize);
            }

            /*
             this is the fiddly bit - each motor maps to an imaginary axis at 45deg to x,y.
             this way forwards and backwards both motors get the same signal,
             but left or right each motor gets the inverse so we spin on the spot
             */
            Vector m0Axis = (new Vector(1, 1)).getUnit();
            Vector m1Axis = (new Vector(-1, 1)).getUnit();

            m0Speed = m0Axis.dot(joy) * maxSpeed;
            m1Speed = m1Axis.dot(joy) * maxSpeed;
        }

        if (handbrake != null && handbrake.getPollData() != 0f) {
            //handbreak pressed
            m0Speed = maxBrake;
            m1Speed = maxBrake;
            m0Brake = true;
            m1Brake = true;
        }

//            if (trigger != null && trigger.getPollData() != 0f) {
//                client.fire();
//            }
        if (!m0Brake) {
            if (m0Speed > 0) {
                m0Speed *= m0ForwardsAdjust;
            } else {
                m0Speed *= m0BackwardsAdjust;
            }
        }

        if (!m1Brake) {
            if (m1Speed > 0) {
                m1Speed *= m1ForwardsAdjust;
            } else {
                m1Speed *= m1BackwardsAdjust;
            }
        }

        //set latest commands
        this.m0Command = new MotorCommand(0, m0Brake, (float) m0Speed);
        this.m1Command = new MotorCommand(1, m1Brake, (float) m1Speed);
    }

    public void keyDown(KeyEvent evt) {
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_W:
                forwards = true;
                break;
            case KeyEvent.VK_S:
                backwards = true;
                break;
            case KeyEvent.VK_A:
                left = true;
                break;
            case KeyEvent.VK_D:
                right = true;
                break;
            case KeyEvent.VK_Q:
                anticlockwise = true;
                break;
            case KeyEvent.VK_E:
                clockwise = true;
                break;
            case KeyEvent.VK_SPACE:
                stop = true;
                break;
//            case KeyEvent.VK_ENTER:
//                client.fire();
//                break;
//            case KeyEvent.VK_H:
//                client.playSound(client.HELLO_SOUND);
//                break;
        }
    }

    public void keyUp(KeyEvent evt) {
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_W:
                forwards = false;
                break;
            case KeyEvent.VK_S:
                backwards = false;
                break;
            case KeyEvent.VK_A:
                left = false;
                break;
            case KeyEvent.VK_D:
                right = false;
                break;
            case KeyEvent.VK_Q:
                anticlockwise = false;
                break;
            case KeyEvent.VK_E:
                clockwise = false;
                break;
            case KeyEvent.VK_SPACE:
                stop = false;
                break;
        }
    }

    /**
     * Get the latest valid motor command
     *
     * @param motor
     * @return
     */
    public MotorCommand getMotorCommand(int motor) {
        switch (motor) {
            case 0:
                return this.m0Command;
            case 1:
                return this.m1Command;
            default:
                return null;
        }
    }

    /**
     * Get the maximum speed as limited by the current throttle
     *
     * @return
     */
    public float getMaxSpeed() {
        return this.maxSpeed;
    }

    /**
     * Returns true if the throttle is controlled by the joystick
     *
     * @return
     */
    public boolean hasThrottle() {
        return this.throttle != null;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }
//    public void setMotors(int _m0Speed, boolean _m0Forwards, boolean _m0Break, int _m1Speed, boolean _m1Forwards, boolean _m1Break) {
//        MotorSpeedCommand motorSpeed = new MotorSpeedCommand(_m0Speed, _m0Forwards, _m0Break, _m1Speed, _m1Forwards, _m1Break);
//        sendObject(motorSpeed);
//    }
//
//    //simplified version, with +/- allowed for speed
//    public void setMotors(int _m0Speed, boolean _m0Break, int _m1Speed, boolean _m1Break) {
//        sendObject(new MotorSpeedCommand(_m0Speed, _m0Break, _m1Speed, _m1Break));
//
//        //also adjust the UI
//    }
//
//    public void updateUI(int m0Speed, int m1Speed, boolean handbreak, int throttle) {
//        window.updateMotorState(m0Speed, m1Speed, handbreak, throttle);
//    }
}
