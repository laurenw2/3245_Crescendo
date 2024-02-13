// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import frc.robot.Constants.ControllerConstants;
import frc.robot.commands.CheckOrangeTrippedCommand;
import frc.robot.commands.InShooterCommand;
import frc.robot.commands.IndexShootCommand;
import frc.robot.commands.IntakeHandoffCommand;
import frc.robot.commands.IntakeIndexUntilTrippedCommand;
import frc.robot.commands.IntakeIntoShooterCommand;
import frc.robot.commands.SetShoulderCommand;
import frc.robot.commands.TestFalconIntakeRunForSecs;
import frc.robot.commands.autos.AutoBase;
import frc.robot.commands.autos.AutonomousChooser;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.IndexerSubsystem;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.ShootSubsystem;
import frc.robot.subsystems.ShoulderSubsystem;
import frc.robot.subsystems.TestFalconIntakeSubsystem;

import com.ctre.phoenix.platform.can.AutocacheState;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.commands.FollowPathCommand;
import com.pathplanner.lib.commands.FollowPathHolonomic;
import com.pathplanner.lib.commands.FollowPathRamsete;
import com.pathplanner.lib.commands.PathPlannerAuto;
import com.pathplanner.lib.path.PathPlannerPath;
import com.pathplanner.lib.path.PathPlannerTrajectory;
import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.PIDConstants;
import com.pathplanner.lib.util.ReplanningConfig;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.RunCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.button.Trigger;

public class RobotContainer {
  // subsystems
  private final DriveSubsystem m_robotDrive = new DriveSubsystem();
  private final ShootSubsystem m_shootSubsystem = new ShootSubsystem();
  private final IndexerSubsystem m_indexerSubsystem = new IndexerSubsystem();
  private final IntakeSubsystem m_intakeSubsystem = new IntakeSubsystem();
  private final ShoulderSubsystem m_shoulderSubsystem = new ShoulderSubsystem();
  SendableChooser<Command> autoChooser;
  SendableChooser<Command> m_chooser = new SendableChooser<>();
  
  // controllers
  CommandXboxController m_driverController = new CommandXboxController(ControllerConstants.kDriverControllerPort);
  CommandXboxController m_operatorController = new CommandXboxController(ControllerConstants.kOperatorControllerPort);

  // commands
  SetShoulderCommand shimmyUp = new SetShoulderCommand(m_shoulderSubsystem, "protected");
  SetShoulderCommand shimmyDown = new SetShoulderCommand(m_shoulderSubsystem, "home");
  SequentialCommandGroup handoffCommand = new SequentialCommandGroup(
    new IntakeIndexUntilTrippedCommand(m_intakeSubsystem, m_indexerSubsystem),
   /* new IntakeIntoShooterCommand(m_intakeSubsystem, m_indexerSubsystem),*/
    new InShooterCommand(m_intakeSubsystem, m_indexerSubsystem)
  );
  IntakeHandoffCommand allInOneHandoff = new IntakeHandoffCommand(m_intakeSubsystem, m_indexerSubsystem, m_operatorController);  

  // auto routines
  SequentialCommandGroup testSeq = new SequentialCommandGroup(
        m_robotDrive.getPath("Speaker to Ring 1"),
        new SetShoulderCommand(m_shoulderSubsystem, "protected")
        //,m_robotDrive.getPath("Ring 1 to Speaker")
  );
  //Command auto1 = new AutoBase(m_robotDrive, "DriveStraightSpin", 4, 3);

  public RobotContainer() {
    m_robotDrive.calibrateGyro();
    // default commands
    NamedCommands.registerCommand("Arm Up", new SetShoulderCommand(m_shoulderSubsystem, "protected"));
    m_robotDrive.setDefaultCommand(
        new RunCommand(
            () -> m_robotDrive.drive(
                -MathUtil.applyDeadband(-m_driverController.getLeftY(), ControllerConstants.kDriveDeadband),
                -MathUtil.applyDeadband(-m_driverController.getLeftX(), ControllerConstants.kDriveDeadband),
                -MathUtil.applyDeadband(m_driverController.getRightX(), ControllerConstants.kDriveDeadband),
                true, true),
            m_robotDrive));
    //m_shoulderSubsystem.setDefaultCommand(new RunCommand(() -> m_shoulderSubsystem.manual(m_operatorController), m_shoulderSubsystem));
    //m_intakeSubsystem.setDefaultCommand(new RunCommand(() -> m_intakeSubsystem.manual(m_driverController), m_intakeSubsystem));
    //m_indexerSubsystem.setDefaultCommand(new RunCommand(() -> m_indexerSubsystem.manual(m_driverController), m_indexerSubsystem));
    m_shootSubsystem.setDefaultCommand(new RunCommand(() -> m_shootSubsystem.manual(m_driverController), m_shootSubsystem));
    autoChooser = AutoBuilder.buildAutoChooser();
    //SmartDashboard.putData("Auto Mode", autoChooser);
    SmartDashboard.putData("AutoMode", m_chooser);

  /*autoChooser.addOption("Test Auto", m_robotDrive.getAuto("Test Auto"));
    autoChooser.addOption("Score 1 Speaker", m_robotDrive.getAuto("Score 1 Speaker"));
    autoChooser.addOption("follow Go To Ring 1 path", AutoBuilder.followPath(PathPlannerPath.fromPathFile("Go To Ring 1")));
    autoChooser.addOption("go to ring 1 and intake", AutoBuilder.followPath(PathPlannerPath.fromPathFile("Go To Ring 1")).andThen(new WaitCommand(1)).andThen(AutoBuilder.followPath(PathPlannerPath.fromPathFile("Ring 1 to Speaker")))); 
    autoChooser.addOption("Please God Auto", m_robotDrive.getAuto("Please God Auto"));
    autoChooser.addOption("Please God Path", m_robotDrive.getPath("Please God"));
    autoChooser.addOption("raise arm sequence", 
        AutoBuilder.followPath(PathPlannerPath.fromPathFile("Please God"))
        .andThen(new SetShoulderCommand(m_shoulderSubsystem, "amp"))
        .andThen(AutoBuilder.followPath(PathPlannerPath.fromPathFile("Next Path"))
        ));*/
    m_chooser.addOption("Please God Auto diff", m_robotDrive.getAuto("Please God Auto"));
    m_chooser.addOption("red test", m_robotDrive.getPath("Red Drive Straight"));
    m_chooser.addOption("nikos thing", m_robotDrive.getPath("neeksy"));
    m_chooser.addOption("Score 1 Speaker", m_robotDrive.getAuto("Score 1 Speaker"));
    m_chooser.addOption("Please God Path diff", m_robotDrive.getPath("Please God").andThen
    (
      new SetShoulderCommand(m_shoulderSubsystem, "amp")
    ));
    m_chooser.addOption("score 2 speaker @ speaker only drive", m_robotDrive.getAuto("Score 2 Speaker At Speaker"));
    m_chooser.addOption("test seq", testSeq);
    configureBindings();
  }

  private void configureBindings() {
    new JoystickButton(m_driverController.getHID(), ControllerConstants.setXValue)
        .whileTrue(new RunCommand(
            () -> m_robotDrive.setX(),
            m_robotDrive));

    //shoulder
    new JoystickButton(m_driverController.getHID(), ControllerConstants.shoulderHomeButton).whileTrue(
      new SetShoulderCommand(m_shoulderSubsystem, "home"));

    new JoystickButton(m_driverController.getHID(), ControllerConstants.shoulderAmpButton).whileTrue(
      new SetShoulderCommand(m_shoulderSubsystem, "amp"));

    new JoystickButton(m_driverController.getHID(), ControllerConstants.shoulderProtButton).whileTrue(
      new SetShoulderCommand(m_shoulderSubsystem, "protected"));

    //handoff
    new Trigger(m_driverController.axisGreaterThan(ControllerConstants.intakeAxis, 0.5)).whileTrue(
      handoffCommand
    );

    new JoystickButton(m_driverController.getHID(), ControllerConstants.shootButton).whileTrue(
      new InstantCommand(() -> m_indexerSubsystem.runFast(), m_indexerSubsystem)
    ).whileFalse(
      new InstantCommand(() -> m_indexerSubsystem.stop(), m_indexerSubsystem)
    );


    /*
     * Weird stuff Niko wants:
     * A, B, X --> shoulder.setshoulderstates(each respective mode)
     * LB: if shoulder.getspinupafter, setshouldertodesangle().sequence(whatever he wants that has no spinup)
     *     else if !shoulder.getspinupafter, setshouldertodesangle().sequence(regular shooting sequence command)
     * :)
     */
  }

  public Command getAutonomousCommand() {

    // Create config for trajectory
    //base auto command

            return m_chooser.getSelected();
            //return AutoBuilder.followPath(PathPlannerPath.fromPathFile("Please God"));

           /*return  AutoBuilder.followPath(PathPlannerPath.fromPathFile("Please God"))
        .andThen(new SetShoulderCommand(m_shoulderSubsystem, "amp"))
        .andThen(AutoBuilder.followPath(PathPlannerPath.fromPathFile("Next Path"))
        );*/
    } 

  }

