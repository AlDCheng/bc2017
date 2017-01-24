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

public class ArchonBot extends GlobalVars {
	
	// Variable for storing the current round of the game
	public static int currentRound = 0;
	
	// Variables important to self and team recognition
	public static int myID;
	private static Team enemy;
	private static Team allies;	
	private static final float strideRadius = battlecode.common.RobotType.ARCHON.strideRadius;
	private static final float bodyRadius = battlecode.common.RobotType.ARCHON.bodyRadius;

	// Variable storing number of additional victory points needed to win
	public static float victoryPointsToWin;
	
	// Parameters to store location of self (at beginning of round)
	public static MapLocation myLocation;

	// Direction at which the archon traveled last
	private static Direction lastDirection;
	private static MapLocation lastPosition;
	
	// Direction for use each round
	private static Direction myDirection;	
	
	   // Variable to determine after how long archons decide that Alan's code is a piece of shit......
    public static final int giveUpOnRouting = 100;
    
    // Variable to store the amount of time currently in routing....
    public static int roundsRouting = 0;
    
    // Arraylist to store path for routing....    
    public static ArrayList<MapLocation> routingPath;
    
    // Total number of gardeners hired by this archon...
    public static int totalGardenersHired;
     
    // Radius at which archon attempts to disperse
    public static final int disperseRadius = 4;
    
    // Stores the total number of gardeners contained
    public static int numberofGardeners;
    
    // Rotation direction of the archon naturally
    public static boolean rotationDirection;
    
    


	
	// Starting game phase
	
	public static void init() throws GameActionException {
		
		// SYSTEM CHECK Initialization start check
		System.out.println("Archon initialized!");

		// Initialize variables important to self, team, and opponent 
		myID = rc.getID();
		enemy = rc.getTeam().opponent();
		allies = rc.getTeam();
		lastDirection = Move.randomDirection();
		
		numberofGardeners = rc.readBroadcast(BroadcastChannels.GARDENER_NUMBER_CHANNEL);
		
		rotationDirection = false;
		
		myLocation = rc.getLocation();
		totalGardenersHired = 0;
		
	    // Initialize path list and goal location
       	routingPath = new ArrayList<MapLocation>();    	
       	Routing.setRouting(routingPath);
		constructGardeners(2);	
	}
	
	public static void constructGardeners(int maxGardeners) throws GameActionException {		
		
		// Loop to terminate the starting phase of the robot
		boolean checkStatus = true;		
		
		// Variable to store the number of gardeners hired in this phase....
		int hiredGardeners = 0;
		
        // Starting phase loop
        while (hiredGardeners < maxGardeners) {

            // Try/catch blocks stop unhandled exceptions, - print stacktrace upon exception error....
            try {
            	// SYSTEM CHECK - Inform that the archon is attempting to construct a gardener....
            	System.out.println("Attempting to hire a gardener. Can hire a maximum of: " + maxGardeners + ", currently hired: " + hiredGardeners);
            	
            	// Boolean to determine whether or not the archon attempted to hire a gardener this turn or not......
            	boolean hiredGardener = false;  
            	
            	// Update own location
            	myLocation = rc.getLocation();
            	
            	// STore the location that archon wants to go to....
            	MapLocation desiredMove = myLocation;
            	
            	// Initialize information about world......
               	RobotInfo[] enemyRobots = NearbyUnits(enemy, -1);
            	RobotInfo[] alliedRobots = NearbyUnits(allies, -1);
            	BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
            	
            	// Update the current round number.....
            	currentRound = rc.getRoundNum();
  
            	Direction testDirection = new Direction(lastDirection.radians + (float) Math.PI);
            	
            	Direction gardenerDirection = tryHireGardener(testDirection);
            	
            	// If the archon can hire a gardener in a certain direction...
            	if (gardenerDirection != null){
            		// Assert that the archon can actually hire it (i.e. not limited by previous hiring
            		if (rc.canHireGardener(gardenerDirection)){
	            		rc.hireGardener(gardenerDirection);
	            		
	            		// Increment counters.....
	            		hiredGardener = true;
	            		hiredGardeners += 1;
	            		totalGardenersHired += 1;
	            	}
            	}
            	
            	MapLocation disperseLocation = moveAwayfromGardeners(alliedRobots);
            	
            	if (disperseLocation != null){            		
            		desiredMove = disperseLocation;
            	}
            	
            	// Call the function to correct a move and actually move......
            	moveCorrect(desiredMove, rotationDirection, nearbyBullets);       	
      
            	// Update the last position of the robot to get the heading of the archon in the previous turn....
	        	lastPosition =  rc.getLocation();
	            lastDirection = new Direction(myLocation, lastPosition);
                  
	            currentRound += 1;
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception in Starting Phase");
                e.printStackTrace();
            }
        }
        
        // Move to the mainPhase of operations
        idle();
    }
	
	
	// Default state of archon when it is not building anything......
	
	public static void idle() throws GameActionException {

		
        while (true) {
        	
            // catch 
            try {            	
            	// SYSTEM CHECK - Inform that the archon is attempting to construct a gardener....
            	System.out.println("Currently not doing anything..............." );
            	
            	// Update own location
            	myLocation = rc.getLocation();
            	
            	// STore the location that archon wants to go to....
            	MapLocation desiredMove = myLocation;
            	
            	// Initialize information about world......
               	RobotInfo[] enemyRobots = NearbyUnits(enemy, -1);
            	RobotInfo[] alliedRobots = NearbyUnits(allies, -1);
            	BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
            	
            	// Update the current round number.....
            	currentRound = rc.getRoundNum();
  
            	Direction testDirection = new Direction(lastDirection.radians + (float) Math.PI);

            	MapLocation disperseLocation = moveAwayfromGardeners(alliedRobots);
            	
            	if (disperseLocation != null){            		
            		desiredMove = disperseLocation;
            	}
            	
            	// Call the function to correct a move and actually move......
            	moveCorrect(desiredMove, rotationDirection, nearbyBullets);       	
      
            	// Update the last position of the robot to get the heading of the archon in the previous turn....
	        	lastPosition =  rc.getLocation();
	            lastDirection = new Direction(myLocation, lastPosition);
                  
	            currentRound += 1;           	
            	
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                
                e.printStackTrace();
            }
        }
    }
        
	// Function to get the archon to move away from gardeners to give them more space....
	
	private static MapLocation moveAwayfromGardeners(RobotInfo alliedRobots[]){
		
		// Placeholder variables to store the minimum distance to a friendly gardener and the gardener's data
		float minimum = Integer.MAX_VALUE;
		RobotInfo nearestRobot = null;
		
		// ITerate through all the allied robots....
		for (RobotInfo alliedRobot: alliedRobots){
			
			// Assert that the robot is a gardener, has the minimal found distance and is within bounds to make the archon disperse...
			if (alliedRobot.type == battlecode.common.RobotType.GARDENER && myLocation.distanceTo(alliedRobot.location) < minimum 
					&& myLocation.distanceTo(alliedRobot.location) < disperseRadius){
				nearestRobot = alliedRobot;
				minimum = myLocation.distanceTo(alliedRobot.location);
				
			}
		}
		// If there is a gardener nearby, attempt to move away from it
		if (nearestRobot != null){
			// Get the distance away that the robot is
			float distanceTo = myLocation.distanceTo(nearestRobot.location);
			
			// The direction that the robot needs to travel to get away from said robot
			Direction moveAway = new Direction(nearestRobot.location, myLocation);
			
			// Attempt to move directly away from it to a point that is exactly one disperseRadius from the robot
			return myLocation.add(moveAway,disperseRadius - distanceTo);
		}
		// If no gardeners are close enough to warrant walking away, return nothing....
		else{
			return null;
		}
	}
	
	// Find a direction to hire a direction or return null if there isn't one found...
	
	private static Direction tryHireGardener(Direction tryDirection){
		
		// Iterate over 36 different angles, starting near the inputed direction and diverging away to see if a gardener can be built in any one of those locations..
		for (int i = 0; i <= 18; i++){
			
			Direction newHireDirection = new Direction(tryDirection.radians + i * (float) (Math.PI/18));
			if (rc.canHireGardener(newHireDirection)){
				return newHireDirection;
			}
			newHireDirection = new Direction(tryDirection.radians - i * (float) (Math.PI/18));
			if (rc.canHireGardener(newHireDirection)){
				return newHireDirection;			
			}			
		}
		return null;
	}
					
	private static RobotInfo[] NearbyUnits(Team team, float distance){
		
		return rc.senseNearbyRobots(myLocation, distance, team);
	}
	
	// Copy of the code from the scoutbot - correct movement after finding a location......
	
	private static void moveCorrect(MapLocation desiredMove, boolean rotationDirection, BulletInfo[] nearbyBullets) throws GameActionException{
		
		// Correct desiredMove to within one soldier  stride location of where the robot is right now....
    	if(myLocation.distanceTo(desiredMove) > strideRadius){
    		
        	Direction desiredDirection = new Direction(myLocation, desiredMove);	
        	
        	desiredMove = myLocation.add(desiredDirection, strideRadius);
    	}
    	// Make the robot bounce of walls if they are too far......
    	if (!rc.canMove(desiredMove)){
    		MapLocation newLocation = Yuurei.correctOutofBoundsError(desiredMove, myLocation, bodyRadius, strideRadius, rotationDirection);
    		
    		myDirection = new Direction(myLocation, newLocation);
    		
    		desiredMove = newLocation;
    	}
    	
    	// Check if the initial desired move can be completed and wasn't out of bounds/corrected by the above function
    	if(!rc.canMove(desiredMove)){          		
    	
			MapLocation newLocation = Yuurei.attemptRandomMove(myLocation, desiredMove, strideRadius);
			
			desiredMove = newLocation;
    	}     	
    	
    	// --------------------------- DODGING ------------------------ //
    	
    	// Placeholder Variable for any dodge that the dodge function creates....
    	MapLocation dodgeLocation;
    	
    	// Currently does nothing may use later XD
    	boolean canDodge = false;
    	
    	// SYSTEM CHECK - Make sure that the dodge function is called...
    	// System.out.println("Calling Dodge Function....");
    	
    	// Call the dodge function
    	dodgeLocation = Yuurei.attemptDodge(desiredMove, myLocation, nearbyBullets, strideRadius, bodyRadius, -1, rotationDirection, canDodge);
    	    			
    	// If there is a location that the unit can dodge to..
    	if (dodgeLocation != null){
    		desiredMove = dodgeLocation;
    	}
    	
    	// See whether or not the robot can move to the current desired move, and move if it does
    	if(rc.canMove(desiredMove)){
    		rc.move(desiredMove);
    	}           	
    	else{
    		// SYSTEM CHECK - Make sure that the robot didn't move because it didn't want to....
    		// System.out.println("This robot did not move because it did not want to....");
    	}    	
	}
	
	private static void getInitialWalls(){
		
		
		
		
		
	}
}