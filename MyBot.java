import hlt.*;

import java.util.*;

public class MyBot {

  public static void main(final String[] args) {
    final Networking networking = new Networking();
    final GameMap gameMap = networking.initialize("Tamagocchi");

    // We now have 1 full minute to analyse the initial map.
    final String initialMapIntelligence =
            "width: " + gameMap.getWidth() +
            "; height: " + gameMap.getHeight() +
            "; players: " + gameMap.getAllPlayers().size() +
            "; planets: " + gameMap.getAllPlanets().size();
    Log.log(initialMapIntelligence);

    final ArrayList<Move> moveList = new ArrayList<>();
    for (;;) {
      moveList.clear();
      networking.updateMap(gameMap);

      for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
        if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
          continue;
        }
        int me = ship.getOwner();
        int i = 0;
        int count = 0;
        for (final Planet planet : gameMap.getAllPlanets().values()) {
          if(planet != null)
            count++;
        }
        Planet[] nearbyPlanets = new Planet[count];
        double[] distance = new double[count];
        for (final Planet planet : gameMap.getAllPlanets().values()) {
          if(planet != null) {
            nearbyPlanets[i] = gameMap.getPlanet(i);
            distance[i] = planet.getDistanceTo(ship);
            i++;
          }
        }

        //sorting planet array by distance
        for(i = 1; i < count; i++) {
          if(nearbyPlanets[i] != null) {
            double tmp = distance[i];
            Planet tmpPlanet = nearbyPlanets[i];
            int j = i - 1;
              while(j >= 0 && distance[j] > tmp) {
                nearbyPlanets[j+1] = nearbyPlanets[j];
                distance[j+1] = distance[j];
                j--;
              }
            nearbyPlanets[j+1] = tmpPlanet;
            distance[j+1] = tmp;
          }
        }

        for (i = 0; i < count; i++) {
          if(nearbyPlanets[i] != null) {
            if (nearbyPlanets[i].isOwned()) {
              if(nearbyPlanets[i].getOwner() != me || nearbyPlanets[i].isFull()) {
                //attack docked ships
                if(nearbyPlanets[i].getOwner() != me) {
                  int ownerOfShip = nearbyPlanets[i].getOwner();
                  List<Integer> dockedShips = nearbyPlanets[i].getDockedShips();
                  for(final int shipId : dockedShips) {
                    Ship enemy = gameMap.getShip(ownerOfShip, shipId);
                    final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, enemy, Constants.MAX_SPEED, true, Constants.MAX_NAVIGATION_CORRECTIONS, Math.PI/180.0 );
                      if (newThrustMove != null) {
                        moveList.add(newThrustMove);
                      }
                      break;
                  }
                  break;
                }
                else
                  continue;
              }
            }

            if (ship.canDock(nearbyPlanets[i])) {
              moveList.add(new DockMove(ship, nearbyPlanets[i]));
              break;
            }

            final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, nearbyPlanets[i], Constants.MAX_SPEED);
              if (newThrustMove != null) {
                moveList.add(newThrustMove);
              }
              break;
          }
        }
      }


        Networking.sendMoves(moveList);
    }
  }
}
