package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import uk.ac.bris.cs.scotlandyard.ai.ManagedAI;
import uk.ac.bris.cs.scotlandyard.ai.PlayerFactory;
import uk.ac.bris.cs.scotlandyard.model.Colour;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.TicketMove;
import uk.ac.bris.cs.scotlandyard.model.PassMove;
import uk.ac.bris.cs.scotlandyard.model.DoubleMove;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYardView;
import uk.ac.bris.cs.scotlandyard.model.MoveVisitor;

// TODO name the AI
@ManagedAI("Simple Furthest Distance AI")
public class MyAI implements PlayerFactory {

	// TODO create a new player here
	@Override
	public Player createPlayer(Colour colour) {
		return new MyPlayer();
	}

	private static class MyPlayer implements Player {
		@Override
		//AI will pick location that is furthest away from detectives based on the numbers on the nodes.
		public void makeMove(ScotlandYardView view, int location, Set<Move> moves,
				Consumer<Move> callback) {
			//create scoreboard in which to put the move and its score
			Map<Move,Integer> scoreBoard = new HashMap<>();
			List<Colour> players = view.getPlayers();
			List<Integer> playerLocations = new ArrayList<>();
			//create new list that contains all the locations of the players
			for(Colour eg : players) {
			    int playerLocation = view.getPlayerLocation(eg).get();
			    playerLocations.add(playerLocation);
            }
			//create move visitor
			MoveVisitor visitor = new MoveVisitor() {
				@Override
				public void visit(TicketMove move) {
					int locationTotalDifference = 0;
					for(Integer loc : playerLocations) {
						int locationDifference = move.destination() - loc.intValue();
						if (locationDifference < 0) {
							locationDifference = locationDifference * -1;
						}
						locationTotalDifference = locationTotalDifference + locationDifference;
					}
				}
				@Override
				public void visit(DoubleMove move) {
					int locationTotalDifference = 0;
					for(Integer loc : playerLocations) {
						int locationDifference = move.secondMove().destination() - loc.intValue();
						if (locationDifference < 0) {
							locationDifference = locationDifference * -1;
						}
						locationTotalDifference = locationTotalDifference + locationDifference;
					}
				}
			};
			//for each valid move, determine the location difference between the move destination and the different player locations
			for(Move eg : moves) {
				int locationTotalDifference = 0;
				//visit the moves in order to calculate location difference
				eg.visit(visitor);
				//place the move and the score which is the location difference onto the scoreboard
				scoreBoard.put(eg, locationTotalDifference);
			}
			List<Move> listOfMoves = new ArrayList<Move>();
			listOfMoves.addAll(moves);
			Move bestMove = listOfMoves.get(0);
			//determine which of the moves on the scoreboard is the best
			for(Move eg : moves) {
			    if(scoreBoard.get(eg) > scoreBoard.get(bestMove)) {
                    bestMove = eg;
                }
            }
			//player plays the best move
            callback.accept(bestMove);
		}
	}
}
