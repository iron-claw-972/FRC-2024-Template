package frc.robot.constants;

public class ShooterConstants {

    //Motor IDs for the Shooter
    public static final int bottomMotorID = 3;
    public static final int topMotorID = 6;

    //Constants for the Shooter PID
    public static final double kP = 0.1;
    public static final double kI = 0;
    public static final double kD = 0;

    //Constants of the Shooter RPM
    public static final double kShooterFreeRPM = 6784;
    public static final double kShooterTargetRPM = 3000;
    public static final double kShooterToleranceRPM = 50;

    //Constants for the Shooter Feedforward
    public static final double kS = 0.5;
    public static final double kV = 12.0 / kShooterFreeRPM; 


}
