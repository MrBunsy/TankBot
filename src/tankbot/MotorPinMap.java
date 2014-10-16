package tankbot;

/**
 * Maps which GPIO lines connect to the ports of the H-bridge for a specific
 * motor
 *
 * @author Luke
 */
public class MotorPinMap {

    private final int enable, in1, in2;

    public MotorPinMap(int enable, int in1, int in2) {
        this.enable = enable;
        this.in1 = in1;
        this.in2 = in2;
    }

    public int getEnableGPIO() {
        return this.enable;
    }

    public int getIn1GPIO() {
        return this.in1;
    }

    public int getIn2GPIO() {
        return this.in2;
    }

}
