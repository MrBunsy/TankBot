package tankbot;

import java.io.Serializable;

/**
 * Order to set a specific motor to a specific speed
 * @author Luke
 */
public class MotorCommand implements Serializable{

    //id of motor
    public final int motor;
    //apply brake?
    public final boolean brake;
    //-1 to +1
    public final float speed;

    public MotorCommand(int motor, boolean brake, float speed) {
        this.motor = motor;
        this.brake = brake;
        this.speed = speed;
    }

}
