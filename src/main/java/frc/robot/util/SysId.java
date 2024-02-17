// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.util;

import com.ctre.phoenix6.controls.PositionDutyCycle;
import com.ctre.phoenix6.hardware.TalonFX;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector.SingleArgConstructor;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.units.Measure;
import edu.wpi.first.units.Units;
import edu.wpi.first.units.Voltage;
import edu.wpi.first.wpilibj.sysid.SysIdRoutineLog;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Subsystem;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine.Config;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine.Direction;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine.Mechanism;
import frc.robot.constants.swerve.DriveConstants;

import java.util.function.Consumer;

import com.ctre.phoenix6.SignalLogger;

/** Add your docs here. */
public class SysId {


    SysIdRoutine sysIdRoutine;

    public SysId(String name, TalonFX talon, TalonFX talon2, Subsystem subsystem, Rotation2d angle){
        this(name, talon, talon2,subsystem,new Config(),angle);
    }
    
    public SysId(String name,TalonFX talon, Subsystem subsystem, Config config){
        this(name, talon, null,subsystem,config,null);
    }
    
    public SysId(String name,TalonFX talon, TalonFX talon2, Subsystem subsystem, Config config, Rotation2d angle){
        sysIdRoutine = new SysIdRoutine(
            config,
            new Mechanism(
                x ->{
                    if (talon2 !=null){
                        talon2.setControl(new PositionDutyCycle(angle.getRotations()*DriveConstants.kModuleConstants.angleGearRatio));
                    }
                    talon.setVoltage(x.magnitude());
                },
                x ->{
                    x.motor(name).angularPosition(Units.Revolutions.of(talon.getPosition().getValue()));
                    x.motor(name).angularVelocity(Units.RevolutionsPerSecond.of(talon.getVelocity().getValue()));
                    x.motor(name).voltage(Units.Volts.of(talon.getMotorVoltage().getValue()));
                },
                subsystem,
                name
            )
        );
    }
    public SysId(String name, TalonFX[] talons, TalonFX[] angleTalons, Subsystem subsystem, Config config, Rotation2d[] angle){
        sysIdRoutine = new SysIdRoutine(
            config,
            new Mechanism(
                x ->{
                    if (angleTalons !=null){
                        for (int i =0; i<4;i++){ 
                        angleTalons[i].setControl(new PositionDutyCycle(angle[i].getRotations()*DriveConstants.kModuleConstants.angleGearRatio));
                    }
                }
                    for (TalonFX motor: talons){
                        motor.setVoltage(x.magnitude());
                    }
                    
                },
                x ->{
                    for (int i = 0; i<talons.length;i++){
                    x.motor(name).angularPosition(Units.Revolutions.of(talons[i].getPosition().getValue()));
                    x.motor(name).angularVelocity(Units.RevolutionsPerSecond.of(talons[i].getVelocity().getValue()));
                    x.motor(name).voltage(Units.Volts.of(talons[i].getMotorVoltage().getValue()));
                }},
                subsystem,
                name
            )
        );
    }
    public SysId(String name, Consumer<Measure<Voltage>> driveConsumer,Consumer<SysIdRoutineLog> logConsumer, Subsystem subsystem, Config config, Rotation2d angle){
        sysIdRoutine = new SysIdRoutine(
            config,
            new Mechanism(
                driveConsumer,
                logConsumer,
                subsystem,
                name
            )
        );
    }
    public SysId(String name, Consumer<Measure<Voltage>> driveConsumer, Subsystem subsystem, Config config, Rotation2d angle){
        this(name,driveConsumer,null,subsystem,config,angle);
    }

    public Command runQuasisStatic(Direction direction){
        return sysIdRoutine.quasistatic(direction);
    }
    public Command runDynamic(Direction direction){
        return sysIdRoutine.dynamic(direction);
    }
}
