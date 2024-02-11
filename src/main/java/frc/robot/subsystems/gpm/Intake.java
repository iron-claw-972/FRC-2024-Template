package frc.robot.subsystems.gpm;

import com.revrobotics.CANSparkFlex;
import com.revrobotics.CANSparkLowLevel;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.constants.IntakeConstants;


public class Intake extends SubsystemBase {

    public enum Mode {
        INTAKE(IntakeConstants.INTAKE_POWER), DISABLED(0)
        ;

        private double power;

        Mode(double power) {
            this.power = power;
        }

        public double getPower() {
            return power;
        }
    }

    private final CANSparkFlex motor;
    private final DigitalInput sensor;

    private Mode mode;


    public Intake() {
        motor = new CANSparkFlex(IntakeConstants.MOTOR_ID, CANSparkLowLevel.MotorType.kBrushless);
        sensor = new DigitalInput(IntakeConstants.SENSOR_ID);
        mode = Mode.DISABLED;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean hasNote() {
        return sensor.get();
    }

    @Override
    public void periodic() {
        motor.set(mode.getPower());
    }

    public double getCurrent() {
        return Math.abs(motor.getOutputCurrent());
    }

}
