// Dodging collisions with bullets
package naclbot;
import battlecode.common.*;

public class BulletDodge extends GlobalVars {
	static boolean willCollideWithMe(BulletInfo bullet) {
	        MapLocation myLocation = rc.getLocation();
	
	        // Get relevant bullet information
	        Direction propagationDirection = bullet.dir;
	        MapLocation bulletLocation = bullet.location;
	
	        // Calculate bullet relations to this robot
	        Direction directionToRobot = bulletLocation.directionTo(myLocation);
	        float distToRobot = bulletLocation.distanceTo(myLocation);
	        float theta = propagationDirection.radiansBetween(directionToRobot);
	
	        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
	        if (Math.abs(theta) > Math.PI/2) {
	            return false;
	        }
	
	        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
	        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
	        // This corresponds to the smallest radius circle centered at our location that would intersect with the
	        // line that is the path of the bullet.
	        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)
	
	        return (perpendicularDist <= rc.getType().bodyRadius);
    }
}