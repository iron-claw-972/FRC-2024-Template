package frc.robot.commands.auto_comm;

import edu.wpi.first.wpilibj2.command.*;
import frc.robot.commands.DoNothing;
import frc.robot.commands.gpm.PrepareShooter;
import frc.robot.subsystems.gpm.Shooter;

import java.util.ArrayList;
import java.util.Arrays;

public class AutoShootCommand extends ParallelCommandGroup {

    private static final double SHOOTER_WAIT_TIME = 1.0;

    public AutoShootCommand(Shooter shooter, double rpm, Command... commands) {
        super();

        var commandList = new ArrayList<>();
        commandList.add(new PrepareShooter(shooter, rpm));
        commandList.add(new WaitCommand(SHOOTER_WAIT_TIME));
        commandList.addAll(Arrays.asList(commands));

        addCommands(new SequentialCommandGroup(
                new ParallelRaceGroup(
                        new WaitCommand(15.0 - SHOOTER_WAIT_TIME),
                        new SequentialCommandGroup(commandList.toArray(new Command[]{}))
                ),
                new ConditionalCommand(
                        new PrepareShooter(shooter, 0),
                        new DoNothing(),
                        () -> shooter.getLeftMotorSpeed() > 0 && shooter.getRightMotorSpeed() > 0
                )
        ));
    }
}
