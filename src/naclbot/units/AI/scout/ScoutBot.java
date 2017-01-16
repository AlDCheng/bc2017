// AI for soldier under normal control
package naclbot.units.AI.scout;
import java.util.Arrays;

import battlecode.common.*;
import naclbot.units.motion.Move;
import naclbot.variables.GlobalVars;


/* Values to define
 * 
 * SCOUT_CHANNEL offset 0 -> channel for number of scouts
 * SCOUT_MESSAGE_OFFSET -> offset of a report from a scout
 * 
 */


/* Scout Report Format
 * 
 * 
 */

public class ScoutBot extends GlobalVars {
	
	public static int Rem_is_better;
	public static int id;
	public static int scout_number;
	public static Team enemy;
	public static int memory_size = 15;
	public static boolean runAway = false;
	public static RobotInfo[] enemyArchons= new RobotInfo[3];
	public static int[] enemyArchonIDs = new int[3];
	
	
	public static void entry() throws GameActionException {
		System.out.println("I'm a scout!");	
				
        // Important parameters for self
        enemy = rc.getTeam().opponent();
        id = rc.getID();
        scout_number = rc.readBroadcast(SCOUT_CHANNEL);
        memory_size = 15;        
        Rem_is_better = rc.getRoundNum();
        Arrays.fill(enemyArchonIDs, -1);
        
        MapLocation base = updateBase();  
        MapLocation myLocation = rc.getLocation();        
        
        int track_id = -1;              
        int currently_tracked = 0;
        
        // Array to store number of enemies tracked to date
        int[] no_track = new int[3];
        Arrays.fill(no_track, -1);
        int tracked_total = -1;
        
        
        
		System.out.println("My scout_id is: " + scout_number);
     
        // Variables related to the sending and releasing of trees
 
        TreeInfo[] seen_Trees = new TreeInfo[memory_size];
    
        int[] sent_TreesID = new int[memory_size];
        int[] seen_TreesID = new int[memory_size];
        Arrays.fill(seen_TreesID, -1);
        Arrays.fill(sent_TreesID, -2);
        
        int seen_total = 0;
        int sent_total = 0;
        int sent_index = 0;     
        
        // initial starting movement away from Archon
        Direction last_direction = new Direction(myLocation.directionTo(base).radians + (float) Math.PI);
 
        rc.broadcast(SCOUT_CHANNEL, scout_number + 1);

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	boolean sighted = false;
            	boolean hasBroadcasted = false;
            	int index = -1;
            	
            	// Update Location
            	myLocation = rc.getLocation();         
                
            	// Get nearby enemies            	
            	RobotInfo[] robots = NearbyEnemies(enemy);
            	
            	
             	/***********************************************************************************
            	 * *************************** Code for Broadcasting *********************
            	 **********************************************************************************/
            	
            	
            	
            	// Once in a while broadcast to base new information
            	// (editor's note: Rem = round num)
        		if (Rem_is_better % SCOUT_UPDATE_FREQUENCY == 1){
        			base = updateBase();
        			if (robots.length>0){
        				MapLocation nearest = getNearestEnemytoBase(base, robots, enemyArchons, enemyArchonIDs, sighted, index, true);
                 
                    	rc.broadcast(3 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)nearest.x);
                    	rc.broadcast(4 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)nearest.y);

                    	
               			//Broadcast own coordinates and coordinates of nearest robots
            			rc.broadcast(1 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)myLocation.x);
            			rc.broadcast(2 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)myLocation.y);   
                     	rc.broadcast(9 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, id);
                     	System.out.println( "index" + index);
                     	if (index != -1){
                     		rc.broadcast(10 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, 5);
                     		
                     		rc.broadcast(5 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)enemyArchonIDs[index]);
                        	rc.broadcast(6 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)enemyArchons[index].location.x);
                        	rc.broadcast(7 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)enemyArchons[index].location.y);
                        	System.out.println("I TOLD U WHERE THE NORMIE IS PLS KILL HIS ID IS: " + enemyArchonIDs[index] + "xPos: " + enemyArchons[index].location.x + "yPos: "+ enemyArchons[index].location.y);
                     	}
                     	else{
                     		
                     		rc.broadcast(10 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, 1);
                     	}
                    	hasBroadcasted = true;
        			}
        		}
        		
        		// On every offset 2, broadcast the locations of two trees that you have not yet broadcasted before....

        		else if (Rem_is_better % SCOUT_UPDATE_FREQUENCY == 2){
        			
        			broadcastTree (seen_Trees, sent_TreesID, seen_TreesID, seen_total, sent_total, sent_index, memory_size, 2, SCOUT_CHANNEL, scout_number, SCOUT_MESSAGE_OFFSET, hasBroadcasted);       			
        			if (hasBroadcasted){
        				rc.broadcast(10 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, 3);
        			}
                	
                
        	    	
        		}
        		
        		// Regular broadcast
        		if (!hasBroadcasted){
        			rc.broadcast(1 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)myLocation.x);
        			System.out.println("FUCK SUBARU ");
        			
        			rc.broadcast(2 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)myLocation.y);
                	rc.broadcast(SCOUT_TRACKING + scout_number,  track_id);                	           	
        			rc.broadcast(9 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, id);
                	rc.broadcast(10 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, 0);
        		}
        		
        		
        		// Too many enemies nearby will commit sudoku
        		if (robots.length > 3){
                	System.out.println(" OMG WHY DO THEY LIKE EMILIA SO MUCH FUCKING KILL ME");
        			
    				base = updateBase();        			
        			MapLocation nearest = getNearestEnemytoBase(base, robots, enemyArchons, enemyArchonIDs, sighted, index, false);
        			rc.broadcast(1 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)myLocation.x);
                	rc.broadcast(2 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)myLocation.y);              
                	rc.broadcast(3 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)nearest.x);
                	rc.broadcast(4 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, (int)nearest.y);

                	          	
                	rc.broadcast(9 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, id);
                	rc.broadcast(10 + SCOUT_CHANNEL + scout_number * SCOUT_MESSAGE_OFFSET, 2);
                	
                	/***********************************************************************************
                	 * *************************** Code for Movement for next turn *********************
                	 **********************************************************************************/
                	
                	
                	
    			
                	/* Editing out the kms
        			int x = rc.readBroadcast(SCOUT_CHANNEL);
        			rc.broadcast(SCOUT_CHANNEL, x-1);
        			rc.disintegrate();
        			*/ 
                	if (!runAway){	
                		Direction asdf = Move.randomDirection();
                		tryMoveScout(asdf);
                		last_direction = asdf;
                		runAway = true;
                		
                	}
                	else{
	                	if (rc.canMove(last_direction)){
	            			rc.move(last_direction, (float)2.5);
	            		
	            			
	            		}
	            		else{
	            			Direction asdf = Move.randomDirection();
	            			tryMoveScout(asdf);
	            			last_direction = asdf;
            		
	            		}
        			track_id = -1;
                	currently_tracked = 0;
                	}
        		}
            	
                // If there is no current enemy being tracked
        		else if (track_id == -1){
        			
        			runAway = false;
                	
                	int[] already_tracked = getOtherTracked();
                	
                	//If there is a possible robot to be tracked
                	if (robots.length > 0 && getNearestEnemy(myLocation, robots, no_track, already_tracked)!= null){

                	 
                        RobotInfo quandary = getNearestEnemy(myLocation, robots, no_track, already_tracked);
                        track_id = quandary.ID;
                    	last_direction = moveTowards(quandary, myLocation);
                    	rc.broadcast(SCOUT_TRACKING + scout_number,  track_id);
                		System.out.println("I am now tracking an enemy robot with ID: " + track_id);
                    	
                    	
                    	tracked_total+=1;
                    	currently_tracked = 0;
                    	
                	} else{
                		
                		rc.broadcast(SCOUT_TRACKING + scout_number,  -1);
                		if (rc.canMove(last_direction)){
                			rc.move(last_direction, (float)2.5);               		
                		}
                		
                		else{
                			Direction asdf = Move.randomDirection();
                			tryMoveScout(asdf);
                			last_direction = asdf;                		
                		}
                	}
               
                	
                // Otherwise if already tracking 
           
                } else{                	
                	
                	// If the robot to be tracked is visible - move towards visible location to within 5 units
                	if (rc.canSenseRobot(track_id) && currently_tracked < 10){
                		
                    	RobotInfo quandary = rc.senseRobot(track_id);
                    	// if the robot's current location is far from the 
                    	last_direction = moveTowards(quandary, myLocation);
                    	currently_tracked +=1;                	
                          		
                	} else if (currently_tracked >= 10){
                		
                		System.out.println("Switching Targets");
                    	no_track[tracked_total % 3] = track_id;
        				System.out.println(tracked_total);
        				
        				
        				Direction asdf = Move.randomDirection();
            			tryMoveScout(asdf);
            			last_direction = asdf;        			           		
                    	track_id = -1;
                    	currently_tracked = 0;               		
                		                		
                	} else{
                		
                		track_id = -1;
                		currently_tracked = 0;
                		// Go towards last known position for one turn
                   		if (rc.canMove(last_direction)|| rc.isLocationOccupiedByTree(myLocation.add(last_direction, (float)2.5))){
                   			rc.move(last_direction);
                   		}
                   		
                   		if (!rc.hasMoved()){
                     		int i = 0; 
                     	
                     		while(!rc.hasMoved() && i < 10){
                     			Direction adir = Move.randomDirection();
                     			tryMoveScout(adir);
                     			i+=1;
                     			last_direction = adir;
                     		}                    		
                   		}
                	}             	
                }
        		
        		
        		
        		
        		
        		
        		
        		
        		
        		
        		
        		
        		
        		
        		
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Rem_is_better += 1;

                Clock.yield();

            } catch (Exception e) {
                System.out.println("Scout Exception");
                e.printStackTrace();
            }
        }
    }
	public static RobotInfo[] NearbyEnemies(Team nemesis){
		return rc.senseNearbyRobots(-1, nemesis);
	}
	
	public static RobotInfo getNearestEnemy(MapLocation myLocation, RobotInfo[] enemies, int[] ikenai, int[] shinai){
		
		
		// Smallest distance to another robot
		float minimum = 1;
				
		// Index of the closest robot defaults to the first					
		int index = -1;
		for (int i = 0; i < enemies.length; i++){

			float dist = myLocation.distanceTo(enemies[i].location);

			if (dist < minimum ){
				
				if (!arrayContainsInt(ikenai, enemies[i].ID)){
					
					 if (!arrayContainsInt(shinai, enemies[i].ID)){
					minimum = dist;
					index = i;
					System.out.println(enemies[i].ID);
					}
				}
			}			
		}
		if (index != -1){
			return enemies[index];	
		}
		else{
			return null;
		}
		
	}

	public static Direction moveTowards(RobotInfo quandary, MapLocation myLocation) throws GameActionException{
		float gap = myLocation.distanceTo(quandary.location);
    	Direction dir = myLocation.directionTo(quandary.location);
    	Direction perp = new Direction(dir.radians+((float) Math.PI/2));
    	Direction anti_perp = new Direction(dir.radians+((float) Math.PI/2));

    	Direction anti_dir = new Direction(dir.radians+(float) Math.PI);

		if  (gap > 7.5){
			// Move towards target]
			if (rc.canMove(dir) || rc.isLocationOccupiedByTree(myLocation.add(dir, (float)2.5))){							
				rc.move(dir);
				return dir;
			}
			else{Direction dir2 = Move.randomDirection();
     			tryMoveScout(dir);
     			return dir2;
			}
			
		} else if (gap < 2.5) {
			// Move away from target
			if (rc.canMove(anti_dir) || rc.isLocationOccupiedByTree(myLocation.add(anti_dir, (float)2.5))){							
				rc.move(anti_dir);
				return dir;
			}
			else{Direction dir2 = Move.randomDirection();
     			tryMoveScout(dir);
     			return dir2;
			}
			
		} else {
			float nani = (float) Math.random();
			float keikaku =  (float) Math.random() + (float) 1.5;
			if (nani>0.5){
				if (rc.canMove(perp)|| rc.isLocationOccupiedByTree(myLocation.add(perp, (float)2.5))){							
					rc.move(perp, keikaku);
					return perp;
				} else if (rc.canMove(anti_perp)|| rc.isLocationOccupiedByTree(myLocation.add(anti_perp, (float)2.5))){							
					rc.move(anti_perp,keikaku);
					return anti_perp;
				} else{Direction dir2 = Move.randomDirection();
					tryMoveScout(dir);
					return dir2;
				}
			}   else{
				
				if (rc.canMove(anti_perp)|| rc.isLocationOccupiedByTree(myLocation.add(anti_perp, (float)2.5))){							
					rc.move(anti_perp, keikaku);
					return anti_perp;
				} else if (rc.canMove(perp)|| rc.isLocationOccupiedByTree(myLocation.add(perp, (float)2.5))){							
					rc.move(perp, keikaku);
					return perp;
				} else{Direction dir2 = Move.randomDirection();
					tryMoveScout(dir);
					return dir2;
				}
		
				
			}
			
			// Move to a 5 unit distance of the target (either away or towards)
		}
	}
	
	public static MapLocation getNearestEnemytoBase(MapLocation baseLocation, RobotInfo[] enemies, RobotInfo[] enemyArchons, int[] enemyArchonIDs, boolean sighted, int index3, boolean update){
		
		// Smallest distance to another robot
		float minimum = 1000;
		boolean updated = true;			
		// Index of the closest robot defaults to the first					
		int index = 0;
		for (int i = 0; i < enemies.length; i++){
			
			if (enemies[i].type == battlecode.common.RobotType.ARCHON && updated && update){
				if (!arrayContainsInt(enemyArchonIDs, enemies[i].ID)){
					for (int j = 0; j < enemyArchons.length; j++){
						if (enemyArchons[j] == null && updated){
							enemyArchons[j] = enemies[i];
							enemyArchonIDs[j] = enemies[i].ID;
							index3 = j;
							sighted = true;
							updated = false;
							System.out.println("I SEE A NEW ARCHON OMG IT LIKES EMILIA PLZ KILL NORMIE PIECE OF SHIT PLS NAO: " + enemies[i].ID);
							
						}
					}
				}
			}
			
			float dist = baseLocation.distanceTo(enemies[i].location);
			if (dist > minimum){
				minimum = dist;
				index = i;
			}			
		}
		return enemies[index].location;		
	}
	
	// Get location of starting archon
	public static MapLocation updateBase() throws GameActionException{
		
		MapLocation base = new MapLocation(rc.readBroadcast(ARCHON_CHANNEL), rc.readBroadcast(ARCHON_CHANNEL + 1));
		return base;		
		
	}
	public static int[] getOtherTracked() throws GameActionException{
		
		int[] tracked = new int[SCOUT_LIMIT];
		Arrays.fill(tracked, -1);
		
		for (int i = 0; i < SCOUT_LIMIT; i++){
			tracked[i] = rc.readBroadcast(SCOUT_TRACKING + i);		
			
		}
		return tracked;
	}
	
    static boolean tryMoveScout(Direction dir) throws GameActionException {
        return tryMoveScout(dir,50,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMoveScout(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {
    	
    	float testDistance = (float) Math.random() * (float) 2.5;
        // First, try intended direction
        if (rc.canMove(dir, testDistance)) {
            rc.move(dir, testDistance);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck+=1;
        }

        // A move never happened, so return false.
        return false;
    }
    
    public static TreeInfo[] addTrees(){
    	TreeInfo[] nearby_trees = rc.senseNearbyTrees();
    	return nearby_trees;   	
    	 	
    	    	
    }
    public static void broadcastTree (TreeInfo[] seen_Trees, int[] sent_TreesID, int[] seen_TreesID, int seen_total, int sent_total,  int sent_index, int memory_size, int broadcast_limit, int type_channel, int type_number, int type_offset, boolean hasBroadcasted) throws GameActionException {
    	
    	// Update Tree storage and broadcast first two previously unsent trees
    	
    	// seenTrees is the TreeInfo array containing the TreeInfo for all of the trees stored in memory by this particular robot
    	// sent_TreesID is the array/memory that the robot has of all the trees whos info IT has sent
    	// seen_TreesID is the array/memory that the robot has of all the trees that it has seen - acts like a dictionary key system for the seenTrees array
    	// memory_size is the number of entries that the robot stores for each of the datatypes above. For a scout this value is 50
    	// broadcast_limit is the number of trees that this robot can broadcast within its offest limit. This is either one or two
    	// int type_channel, type_number and type_offset are the initial channel for this type of robot,  the number of the type that this robot is and the offset that this type of robot has.
    	
    	// Update trees that are able to be sensed
		TreeInfo[] newTrees = addTrees();
		for(int i = 0; i < newTrees.length; i++){
			
			// Check if the current tree has been seen before or no
			if (!arrayContainsInt(seen_TreesID, newTrees[i].ID)) {
				
				// Add new tree ID to list of stored IDs of seen trees
				seen_TreesID[seen_total % memory_size] = newTrees[i].ID;
				seen_Trees[seen_total % memory_size] = newTrees[i];
				seen_total += 1;     					     					       					
			}			
		}
		
		// Decide the trees to be sent and send
		int sentThisTurn = 0;
		TreeInfo[] toSend = new TreeInfo[broadcast_limit];
		while (sentThisTurn < broadcast_limit){
			for (int i = sent_index % memory_size; i < memory_size; i++){
				if (!arrayContainsInt(sent_TreesID, seen_TreesID[i]) && (sentThisTurn < broadcast_limit)){
					if(seen_Trees[i] != null) {
						toSend[sentThisTurn] = seen_Trees[i]; //Error here?
						sent_TreesID[sent_total % memory_size] = seen_TreesID[i];
						sent_total += 1;
				
						sentThisTurn += 1;
					}
				}
				sent_index += 1;
			}
		}
		
		// Information of first tree to be sent
		if (sentThisTurn > 0){
			hasBroadcasted = true;
			System.out.println("THERE IS DEFINITELY A TREE HERE AND REM IS OBVIOUSLY BEST GIRL: " + toSend[0].ID);
			rc.broadcast(1 + type_channel + type_number * type_offset, toSend[0].ID);
			int asdfg =  (1 + type_channel + type_number * type_offset);
			System.out.println("I am broadcasting on this channel: " + asdfg + "data: "+ toSend[0].ID );
			
        	rc.broadcast(2 + type_channel + type_number * type_offset, (int)toSend[0].location.x);
        	rc.broadcast(3 + type_channel + type_number * type_offset, (int)toSend[0].location.y);
        	rc.broadcast(4 + type_channel + type_number * type_offset, (int)toSend[0].radius);        		
		}
		
		// Information of second tree to be sent
		
		if (sentThisTurn > 1){
			System.out.println("FELIS IS ALSO BEST GIRL: " + toSend[1].ID);
			rc.broadcast(5 + type_channel + type_number * type_offset, toSend[1].ID);
			rc.broadcast(6 + type_channel + type_number * type_offset, (int)toSend[1].location.x);
        	rc.broadcast(7 + type_channel + type_number * type_offset, (int)toSend[1].location.y);
        	rc.broadcast(8 + type_channel + type_number * type_offset, (int)toSend[1].radius);        				
		}
	
		rc.broadcast(9 + type_channel + type_number * type_offset, sentThisTurn);
		
    }
}

