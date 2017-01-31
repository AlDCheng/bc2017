// AI for Archon
package naclbot.units.AI.archon;
import battlecode.common.*;

import naclbot.variables.GlobalVars;
import naclbot.variables.DataVars;
import naclbot.variables.DataVars.*;

import naclbot.variables.BroadcastChannels;
import naclbot.units.interact.iFeed;
import naclbot.units.motion.*;
import naclbot.units.motion.routing.Routing;
import naclbot.units.motion.search.TreeSearch;

import java.util.ArrayList;
import java.util.Arrays;


/* ------------------   Overview ----------------------
 * 
 * AI Controlling the functions of the ScoutBot
 *
 * ~~ Coded by Illiyia (akimn@#mit.edu)
 * 
 * Debug statements all begin with SYSTEM CHECK 
 * 
 ---------------------------------------------------- */

public class KazumaBot extends GlobalVars {
	
	public static Team us = rc.getTeam();
	private static final float strideRadius = battlecode.common.RobotType.ARCHON.strideRadius;
	private static final float bodyRadius = battlecode.common.RobotType.ARCHON.bodyRadius;
	
	private static int remIsBestGirl = 0;
	private static int unitNumber;
	private static final int initMove = 30;
	private static MapLocation initialGoal, lastPosition;
	private static Direction lastDirection = new Direction(0);
	
	private static final float crowdThresh = (float)0.5;
	private static boolean startPoll = false;
	
	public static int archonNumber;
	
	// Starting game phase
	public static void init() throws GameActionException {
		
		// SYSTEM CHECK Initialization start check
		System.out.println("Hai, hai Kazuma Desu");
		
		// Let everyone know where the archon started off......
		rc.broadcast(BroadcastChannels.ARCHON_INITIAL_LOCATION_X, (int) (rc.getLocation().x * 100));
		rc.broadcast(BroadcastChannels.ARCHON_INITIAL_LOCATION_Y, (int) (rc.getLocation().y * 100));
		
		unitNumber = rc.readBroadcast(BroadcastChannels.UNIT_NUMBER_CHANNEL);
        rc.broadcast(BroadcastChannels.UNIT_NUMBER_CHANNEL, unitNumber + 1);
		
		archonNumber = rc.readBroadcast(BroadcastChannels.ARCHON_NUMBER_CHANNEL);
		rc.broadcast(BroadcastChannels.ARCHON_NUMBER_CHANNEL, archonNumber + 1);
		
		// Only have one gardener in play
		int numberofGardenersConstructed = rc.readBroadcast(BroadcastChannels.GARDENERS_CONSTRUCTED_CHANNELS);
		
		// Def not Aqua
        remIsBestGirl = rc.getRoundNum();
        
        int treeNum = 0;
        if (numberofGardenersConstructed <= 0) {
        	treeNum = 1;
        }
        
        // Build gardeners
		constructGardeners(treeNum);
	}
	
	//--------------------------------------------------------------------------------------------------------------
	// Idle state / Main function
	public static void idle() throws GameActionException {	
        while (true) {
        	
            // catch 
            try {
            	// If can win, win
            	Win();
            	
            	//update round number 
            	remIsBestGirl = rc.getRoundNum();

            	// Update own location
            	MapLocation myLocation = rc.getLocation();
            	
            	RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            	BroadcastChannels.broadcastNearestEnemyLocation(enemyRobots, myLocation, unitNumber, myLocation.add(Move.randomDirection(), (float)0.5), remIsBestGirl);
            	
            	// Store the location that archon wants to go to.... it doesnt want to move by default
            	MapLocation desiredMove = myLocation;	
            	
            	// Get the total number of units constructed thus far.....
//            	numberofGardenersConstructed = rc.readBroadcast(BroadcastChannels.GARDENERS_CONSTRUCTED_CHANNELS);
            	int gardenerCount = rc.readBroadcast(BroadcastChannels.GARDENERS_ALIVE_CHANNEL);
            	int soldierCount = rc.readBroadcast(BroadcastChannels.SOLDIERS_ALIVE_CHANNEL);
                int lumberjackCount = rc.readBroadcast(BroadcastChannels.LUMBERJACKS_ALIVE_CHANNEL);
                
        		// Check surroundings
        		boolean crowded = (Aqua.checkBuildRadius((float)30, (float)3, (float)0.5) >= crowdThresh);
        		System.out.println("Gardener Count: " + gardenerCount);
        		
        		// If not crowded...
        		// See if we can start polling for a new gardener.
    			int pollState = rc.readBroadcast(BroadcastChannels.GARDENER_POLL);
    			
    			if ((gardenerCount <= 0) && (remIsBestGirl > 20)) {
                	constructGardeners(1);
                }
    			else if (((!crowded) || (pollState == 1)) && (remIsBestGirl >= 75)) {
        			System.out.println("Polling State: " + pollState);
        			// If available:
        			if (pollState == 2) {
        				constructGardeners(1);
        			}
        			else if ((pollState == 0) && (rc.getTeamBullets() >= 100)) {
        				rc.broadcast(BroadcastChannels.GARDENER_BUILD_FILL, 0);
        				rc.broadcast(BroadcastChannels.GARDENER_POLL, 1);
        				startPoll = true;
        			}
        			else if ((pollState == 1) && (startPoll)) {
        				startPoll = false;
        				int fillState = rc.readBroadcast(BroadcastChannels.GARDENER_BUILD_FILL);
        				
        				System.out.println("Build Fill: " + fillState);
        				
        				if (fillState >= 1) {
        					constructGardeners(1);
        					rc.broadcast(BroadcastChannels.GARDENER_POLL, 2);
        				}
        				else {
        					rc.broadcast(BroadcastChannels.GARDENER_POLL, 0);
        				}
        			}
        		}                
        		// Building gardeners if none exists (i.e. killed)
        		
    			int treeCount = rc.getTreeCount();
        		if (rc.isBuildReady() && (treeCount > 10) && (3*gardenerCount < treeCount)) {
        			constructGardeners(1);
        		}
            	
            	// SYSTEM CHECK - Inform that the archon is attempting to construct a gardener....
            	System.out.println("Currently not doing anything..............." );
            	     	
            	Direction testDirection = new Direction(lastDirection.radians + (float) Math.PI);
            	
            	
            	// Movement----------------------------------------------------------------------------------
            	if (remIsBestGirl > initMove) {
            		Aqua.disperseFromGardeners(myLocation, strideRadius, bodyRadius, testDirection.opposite());
            	}
            	// Move to optimal spot at initialization
            	else {
            		System.out.println("Moving in general direction");
            		if (initialGoal != null) {
            			Direction dir = new Direction (rc.getLocation(), initialGoal);
            			
            			desiredMove = myLocation.add(dir, strideRadius);
            			rc.setIndicatorLine(rc.getLocation(), desiredMove, 255, 0, 0);
            			rc.move(Yuurei.correctAllMove(strideRadius, bodyRadius, false, us, myLocation, desiredMove));
            		}
            	}
            	//-------------------------------------------------------------------------------------------
      
            	// Update the last position of the robot to get the heading of the archon in the previous turn....
            	if (lastPosition != null) {
            		if (lastPosition.distanceTo(rc.getLocation()) > 0.1) {
                		lastPosition =  rc.getLocation();
        	            lastDirection = new Direction(myLocation, lastPosition);
                	}
            	}
	            System.out.println("current round number: " + remIsBestGirl);
	            
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                
                e.printStackTrace();
            }
        }
    }
	
	//--------------------------------------------------------------------------------------------------------------
	// Function to build gardeners
	public static void constructGardeners(int maxGardeners) throws GameActionException {	
		
		// If can win, win
    	Win();
		
		// Loop to terminate the starting phase of the robot
		boolean checkStatus = true;		
		
		// Variable to store the number of gardeners hired in this phase....
		int hiredGardeners = 0;
		
		MapLocation lastBuilt = null;
		
        // Starting phase loop
        while ((hiredGardeners < maxGardeners)) {

            // Try/catch blocks stop unhandled exceptions, - print stacktrace upon exception error....
            try {
            	// Why not just win?
            	Win();
            	
            	//--INITIALIZATION--
            	// Update own location
            	MapLocation myLocation = rc.getLocation();
            	
            	// Store the location that archon wants to go to....
            	MapLocation desiredMove = myLocation;
            	
            	// Update the current round number.....
            	remIsBestGirl = rc.getRoundNum();
            	
            	// Scan surrounding first:
            	boolean crowded = (Aqua.checkBuildRadius((float)30, (float)2.5, (float)0.5) >= crowdThresh);
            	
            	// SYSTEM CHECK - Print out the gardener limit and the current number of gardeners constructed
//            	System.out.println("Gardener Limit: " + getGardenerLimit(remIsBestGirl) + ", current constructed number: " + numberofGardenersConstructed);
            	
            	// On turn 1
            	if (remIsBestGirl == 1){
            		lastDirection = Aqua.getInitialWalls(myLocation);
            		initialGoal = rc.getLocation().add(lastDirection, 10);
            		
            		// Broadcast density for gardeners
//                    float treeDensity = Aqua.calculateTreeDensity();
//                    System.out.println("Tree Density: " + treeDensity);
            		System.out.println("Empty Density: " + Aqua.emptyDensity);
                    
                    int broadcastStart = BroadcastChannels.ARCHONS_TREE_DENSITY_CHANNEL + 2*archonNumber;
                    int myID = rc.getID();
                    System.out.println("My ID: " + myID);
                    rc.broadcast(broadcastStart, myID);
                    rc.broadcastFloat(broadcastStart+1, Aqua.emptyDensity);
            	}
            	// Get the total number of gardeners constructed thus far.....
            	int numberofGardenersConstructed = rc.readBroadcast(BroadcastChannels.GARDENERS_CONSTRUCTED_CHANNELS);
            	
            	rc.setIndicatorDot(myLocation.add(lastDirection,10), 155, 135, 244);
            	
            	// SYSTEM CHECK - Inform that the archon is attempting to construct a gardener....
            	System.out.println("Attempting to hire a gardener. Can hire a maximum of: " + maxGardeners + ", currently hired: " + hiredGardeners);
            	
            	// Boolean to determine whether or not the archon attempted to hire a gardener this turn or not......
            	boolean hiredGardener = false;  
            	
            	Direction testDirection = new Direction(0);
            	Direction gardenerDirection = new Direction(0);
            	
            	// Build if not crowded
            	if (!crowded || (remIsBestGirl <= initMove)) {
//            		testDirection = new Direction(lastDirection.radians + (float) Math.PI);
            		if (lastBuilt != null) {
            			testDirection = new Direction(rc.getLocation(), lastBuilt); 
            		}
            		else {
            			testDirection = new Direction(lastDirection.radians + (float) Math.PI);
            		}
                	gardenerDirection = Aqua.tryHireGardener(testDirection);
            	}
  
            	// If the archon can hire a gardener in a certain direction...
            	if (gardenerDirection != null){
            		// Assert that the archon can actually hire it (i.e. not limited by previous hiring
            		if (rc.canHireGardener(gardenerDirection)){
	            		rc.hireGardener(gardenerDirection);
	            		
	            		// Increment counters.....
	            		hiredGardener = true;
	            		hiredGardeners += 1;
	            		
	            		// Update broadcasted counter
	            		rc.broadcast(BroadcastChannels.GARDENERS_CONSTRUCTED_CHANNELS, numberofGardenersConstructed+1);
	            		rc.broadcast(BroadcastChannels.GARDENER_POLL, 0);
	            		
	            		// Reset polling 
	            		
	            		//update last built location
	            		lastBuilt = rc.getLocation();              	
	            	}
            	}
            	else if(rc.readBroadcast(BroadcastChannels.GARDENER_POLL) == 2) {
            		return;
            	}
            	
            	// Movement----------------------------------------------------------------------------------
            	if (remIsBestGirl > initMove) {
            		Aqua.disperseFromGardeners(myLocation, strideRadius, bodyRadius, testDirection.opposite());
            	}
            	// Move to optimal spot at initialization
            	else {
            		System.out.println("Moving in general direction");
            		if (initialGoal != null) {
            			Direction dir = new Direction (rc.getLocation(), initialGoal);
            			
            			desiredMove = myLocation.add(dir, strideRadius);
            			rc.move(Yuurei.correctAllMove(strideRadius, bodyRadius, false, us, myLocation, desiredMove));
            		}
            	}
            	//-------------------------------------------------------------------------------------------
            	
            	// Update the last position of the robot to get the heading of the archon in the previous turn....
            	if (lastPosition != null) {
            		if (lastPosition.distanceTo(rc.getLocation()) > 0.1) {
                		lastPosition =  rc.getLocation();
        	            lastDirection = new Direction(myLocation, lastPosition);
                	}
            	}
	            
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception in Gardener Building Phase");
                e.printStackTrace();
            }
        }
        
        // Move to the mainPhase of operations
        idle();
    }
}