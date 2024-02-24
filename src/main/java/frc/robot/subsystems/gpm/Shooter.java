package frc.robot.subsystems.gpm;

import com.revrobotics.CANSparkFlex;
import com.revrobotics.CANSparkLowLevel.MotorType;
import com.revrobotics.RelativeEncoder;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.units.Angle;
import edu.wpi.first.units.Measure;
import edu.wpi.first.units.Velocity;
import edu.wpi.first.units.Voltage;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.simulation.FlywheelSim;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.constants.ShooterConstants;

public class Shooter extends SubsystemBase {
	// each of the shooter shafts is driven by one Neo Vortex motor
	protected static final DCMotor gearbox = DCMotor.getNeoVortex(1);
	// get the motor freespeed
	private static final double rpmFreeSpeed = Units
			.radiansPerSecondToRotationsPerMinute(Shooter.gearbox.freeSpeedRadPerSec);

	// PID constants. PID system measures RPM and outputs motor power [-1,1]
	private static final double RightP = 0.16572;
	private static final double RightI = 0.0;
	private static final double RightD = 0.0;
	private static final double LeftP = 0.11054;
	private static final double LeftI = 0.0;
	private static final double LeftD = 0.0;

	// FeedForward constants
	private static final double RightS = -3.9311;
	private static final double RightV = 0.17083;
	private static final double RightA = 0.46242;
	private static final double LeftS = -6.9956;
	private static final double LeftV = 0.14629;
	private static final double LeftA = 0.86883;

	/**
	 * Tolerance in RPM.
	 * At 1500 rpm, the simulator gives 1519 rpm.
	 */
	private static final double TOLERANCE = 40;

	// 4-inch Colson wheels
	private static final double MASS_COLSON = 0.245;
	private static final double RADIUS_COLSON = Units.inchesToMeters(2.0);
	private static final double MOI_COLSON = 0.5 * MASS_COLSON * RADIUS_COLSON * RADIUS_COLSON;
	// each motor spins 4 Colson wheels
	private static final double MOI_SHAFT = MOI_COLSON * 4;

	// left motor
	private final CANSparkFlex leftMotor = new CANSparkFlex(ShooterConstants.LEFT_MOTOR_ID, MotorType.kBrushless);
	private final RelativeEncoder leftMotorEncoder = leftMotor.getEncoder();
	/** PID controller uses RPM as input and outputs motor power */
	private final PIDController leftPID = new PIDController(LeftP, LeftI, LeftD);
	private FlywheelSim leftFlywheelSim;
	private double leftMotorSpeedSim;
	private double leftPower = 0.0;

	// right motor
	private final CANSparkFlex rightMotor = new CANSparkFlex(ShooterConstants.RIGHT_MOTOR_ID, MotorType.kBrushless);
	private final RelativeEncoder rightMotorEncoder = rightMotor.getEncoder();
	/** PID controller uses RPM as input and outputs motor power */
	private final PIDController rightPID = new PIDController(RightP, RightI, RightD);
	private FlywheelSim rightFlywheelSim;
	private double rightMotorSpeedSim;
	private double rightPower = 0.0;

	// TODO: TUNE THIS
	private final SimpleMotorFeedforward Leftfeedforward = new SimpleMotorFeedforward(LeftS, LeftV,LeftA);
	private final SimpleMotorFeedforward Rightfeedforward = new SimpleMotorFeedforward(RightS, RightV,RightA);

	public Shooter() {
		// set the RPM tolerance of the PID controllers
		leftPID.setTolerance(TOLERANCE);
		rightPID.setTolerance(TOLERANCE);
		// invert the right motor so +power sends the note out
		rightMotor.setInverted(false);
		leftMotor.setInverted(true);
		// are we simulating?
		if (RobotBase.isSimulation()) {
			leftFlywheelSim = new FlywheelSim(gearbox, 1.0, MOI_SHAFT);
			rightFlywheelSim = new FlywheelSim(gearbox, 1.0, MOI_SHAFT);
		}
	}

	@Override
	public void periodic() {
		// PID loop uses RPM
		double leftSpeed = getLeftMotorRPM();
		double rightSpeed = getRightMotorRPM();

		// TODO: having problems: a set and get do not match, so keep powers around for
		// simulation
		leftPower = leftPID.calculate(leftSpeed) + Leftfeedforward.calculate(leftPID.getSetpoint());
		rightPower = rightPID.calculate(rightSpeed) + Rightfeedforward.calculate(rightPID.getSetpoint());

		// set motor powers
		leftMotor.set(leftPower);
		rightMotor.set(rightPower);

		// report some values to the Dashboard
		SmartDashboard.putNumber("left speed", /* shooterRPMToSpeed */ (leftSpeed));
		SmartDashboard.putNumber("right speed", /* shooterRPMToSpeed */ (rightSpeed));
		SmartDashboard.putBoolean("at setpoint?", atSetpoint());
	}

	@Override
	public void simulationPeriodic() {
		// assume the battery voltage is 12 volts
		double voltage = 12.0;

		// leftPower and rightPower are now member variables; cannot read them from
		// simulated encoder

		// set the system inputs
		leftFlywheelSim.setInputVoltage(leftPower * voltage);
		rightFlywheelSim.setInputVoltage(rightPower * voltage);

		// simulate the linear system (sets current motor angular velocity)
		leftFlywheelSim.update(0.020);
		rightFlywheelSim.update(0.020);

		// get the results
		leftMotorSpeedSim = leftFlywheelSim.getAngularVelocityRPM();
		rightMotorSpeedSim = rightFlywheelSim.getAngularVelocityRPM();

		// we would like to set the encoder velocities to those values, but REV does not
		// let us do that
	}

	/**
	 * Close the resources used by this instance.
	 */
	public void close() {
		leftMotor.close();
		rightMotor.close();
	}

	/**
	 * Converts an RPM to a speed at the surface of the shooter wheels.
	 *
	 * @param rpm the RPM to convert
	 * @return the converted value in m/s
	 * @see shooterSpeedToRPM
	 */
	public static double shooterRPMToSpeed(double rpm) {
		return (rpm / 60) * (RADIUS_COLSON * 2 * Math.PI);
	}

	/**
	 * Converts a speed at the surface of the shooter wheels to an RPM value.
	 *
	 * @param speed the speed to convert in m/s
	 * @return the converted value in RPM
	 * @see shooterRPMToSpeed
	 */
	public static double shooterSpeedToRPM(double speed) {
		return (speed * 60) / (RADIUS_COLSON * 2 * Math.PI);
	}

	/**
	 * Sets the speed both shooter motors try to spin up to independantly.
	 *
	 * @param speedLeft  the speed tho left motor will spin to in RPM
	 * @param speedRight the speed the right motor will spin to in RPM
	 */
	public void setTargetRPM(double speedLeft, double speedRight) {
		leftPID.reset();
		leftPID.setSetpoint(speedLeft);

		rightPID.reset();
		rightPID.setSetpoint(speedRight);
	}

	/**
	 * Sets the RPM both shooter motors try to spin up to.
	 *
	 * @param speed speed to spin to in RPM
	 * @see setTargetVelocity
	 */
	public void setTargetRPM(double speed) {
		setTargetRPM(speed, speed);
	}

	/**
	 * Set the target velocity of a note exiting the shooter.
	 *
	 * @param speed speed to spin to in m/s
	 * @see setTargetRPM
	 */
	public void setTargetVelocity(double speed) {
		// convert speed to RPM
		setTargetRPM(shooterSpeedToRPM(speed));
	}

	/**
	 * Checks whether both motor PIDs are at their setpoints.
	 *
	 * @return boolean indicating whether both PIDs are at their setpoints
	 */
	public boolean atSetpoint() {
		return leftPID.atSetpoint() && rightPID.atSetpoint();
	}
	public double getLeftSetpoint(){
		return leftPID.getSetpoint();
	}
	public double getRightSetpoint(){
		return rightPID.getSetpoint();
	}

	/**
	 * Gets the RPM of the left motor, checking whether this is a simulation.
	 *
	 * @return the left motor's RPM
	 * @see getRightMotorRPM
	 * @see getLeftMotorSpeed
	 * @see atSetpoint
	 */
	public double getLeftMotorRPM() {
		// REV does not let us set the encoder velocity in a simulation, so use
		// simulated value
		if (RobotBase.isSimulation()) {
			return leftMotorSpeedSim;
		} else {
			return leftMotorEncoder.getVelocity();
		}
	}

	/**
	 * Gets the RPM of the right motor, checking whether this is a simulation.
	 *
	 * @return the right motor's RPM
	 * @see getLeftMotorRPM
	 * @see getRightMotorSpeed
	 * @see atSetpoint
	 */
	public double getRightMotorRPM() {
		// REV does not let us set the encoder velocity in a simulation, so use
		// simulated value
		if (RobotBase.isSimulation()) {
			return rightMotorSpeedSim;
		} else {
			return rightMotorEncoder.getVelocity();
		}
	}

	/**
	 * Gets the speed at the surface of the left wheels.
	 *
	 * @return the left wheel's speed in m/s
	 * @see getLeftMotorSpeed
	 * @see getRightMotorRPM
	 * @see atSetpoint
	 */
	public double getLeftMotorSpeed() {
		return shooterRPMToSpeed(getLeftMotorRPM());
	}

	/**
	 * Gets the speed at the surface of the right wheels.
	 *
	 * @return the right wheel's speed in m/s
	 * @see getLeftMotorSpeed
	 * @see getRightMotorRPM
	 * @see atSetpoint
	 */
	public double getRightMotorSpeed() {
		return shooterRPMToSpeed(getRightMotorRPM());
	}

	/**
	 * Gets the difference in RPM between the left motor and right motor.
	 * 
	 * @return the left motor's RPM - the right motor-s RPM
	 * @see getMotorSpeedDifference
	 * @see getLeftMotorRPM
	 * @see getRightMotorRPM
	 * @see atSetpoint
	 */
	public double getMotorRPMDifference() {
		return getLeftMotorRPM() - getRightMotorRPM();
	}

	/**
	 * Gets the difference in speed at the circumference of the motor's wheels
	 * between the left motor and right motor.
	 * 
	 * @return the left motor's speed - the right motor's speed
	 * @see getMotorRPMDifference
	 * @see getLeftMotorSpeed
	 * @see getRightMotorSpeed
	 * @see atSetpoint
	 */
	public double getMotorSpeedDifference() {
		return getLeftMotorSpeed() - getRightMotorSpeed();
	}

	public void setVoltage(Measure<Voltage> voltage){
		leftMotor.setVoltage(voltage.magnitude());
		rightMotor.setVoltage(voltage.magnitude());
	}

	public Measure<Angle> getLeftShooterPosition(){
		return edu.wpi.first.units.Units.Rotations.of(leftMotorEncoder.getPosition());
	}
	public Measure<Velocity<Angle>> getLeftShooterSpeed(){
		return edu.wpi.first.units.Units.RevolutionsPerSecond.of(getLeftMotorRPM()/60);
	}
	public Measure<Voltage> getLeftShooterVoltage(){
		return edu.wpi.first.units.Units.Volts.of(leftMotor.getBusVoltage() * leftMotor.getAppliedOutput());
	}
	public Measure<Angle> getRightShooterPosition(){
		return edu.wpi.first.units.Units.Rotations.of(rightMotorEncoder.getPosition());
	}
	public Measure<Velocity<Angle>> getRightShooterSpeed(){
		return edu.wpi.first.units.Units.RevolutionsPerSecond.of(getRightMotorRPM()/60);
	}
	public Measure<Voltage> getRightShooterVoltage(){
		return edu.wpi.first.units.Units.Volts.of(rightMotor.getBusVoltage() * rightMotor.getAppliedOutput());
	}
	public void setRightp(double p){
		rightPID.setP(p);
	}
		public void setRighti(double p){
		rightPID.setI(p);
	}
		public void setRightd(double p){
		rightPID.setD(p);
	}
		public void setLeftp(double p){
		leftPID.setP(p);
	}
		public void setLefti(double p){
		leftPID.setI(p);
	}
		public void setLeftd(double p){
		leftPID.setD(p);
	}
}
