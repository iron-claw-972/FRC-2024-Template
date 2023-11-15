package frc.robot.commands.vision;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.constants.miscConstants.VisionConstants;
import frc.robot.constants.swerve.DriveConstants;
import frc.robot.subsystems.Drivetrain;
import frc.robot.util.Vision;

/**
 * Tests the odometry of the robot by driving a certain distance and calculating the error.
 */
public class AcquireGamePiece extends CommandBase {

  private Drivetrain m_drive; 
  private Vision m_vision;

  private PIDController m_rotationPID; 
  private PIDController m_distancePID; 
  
  public AcquireGamePiece(Drivetrain drive, Vision vision) {
    m_drive = drive;
    m_vision = vision;

    m_rotationPID = new PIDController(DriveConstants.kHeadingP, 0, DriveConstants.kHeadingD);
    m_rotationPID.setTolerance(Units.degreesToRadians(3)); //3 degree of tolerance
    m_rotationPID.setSetpoint(0);

    m_distancePID = new PIDController(VisionConstants.kDistanceP, VisionConstants.kDistanceI, VisionConstants.kDistanceD); 
    m_distancePID.setTolerance(0.02); //2 cm of tolerance
    m_distancePID.setSetpoint(0);

    addRequirements(drive);
  }

  @Override
  public void initialize(){
    m_rotationPID.reset();
    m_distancePID.reset();
  }

  @Override
  public void execute() {
    double xOffset = Units.degreesToRadians(m_vision.getHorizontalOffset());
    double angle = m_drive.getPose().getRotation().getRadians()-xOffset;
    double distance = m_vision.getDistance();
    
    double rotationOutput = m_rotationPID.calculate(xOffset); 
    rotationOutput = Math.max(Math.min(rotationOutput, DriveConstants.kMaxAngularSpeed), -DriveConstants.kMaxAngularSpeed);
    double distanceOutput = m_distancePID.calculate(distance); 
    distanceOutput = Math.max(Math.min(distanceOutput, DriveConstants.kMaxSpeed), -DriveConstants.kMaxSpeed);
    double xSpeed = distanceOutput*Math.cos(angle);
    double ySpeed = distanceOutput*Math.sin(angle);

    m_drive.drive(xSpeed, ySpeed, rotationOutput, true, false);
  }

  @Override
  public boolean isFinished() { 
    return m_rotationPID.atSetpoint() && m_distancePID.atSetpoint();
  }

  @Override
  public void end(boolean interrupted) {

  }
}