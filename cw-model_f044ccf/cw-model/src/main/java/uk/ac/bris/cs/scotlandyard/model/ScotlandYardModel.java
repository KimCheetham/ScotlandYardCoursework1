package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.DOUBLE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.SECRET;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Graph;
import java.util.Map;

public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private PlayerConfiguration mrX;
	private PlayerConfiguration firstDetective;
	private PlayerConfiguration restOfTheDetectives;
	public List<ScotlandYardPlayer> players = new ArrayList<>();
	public List<Spectator> spectators = new ArrayList<>();
	int currentRound;
	ScotlandYardPlayer currentPlayer;
	ScotlandYardPlayer playingPlayer;
	int mrXLocationDisplayed;
	int mrXLocationActual;
	int intermediateLocation;
	boolean mrXWin;
	HashSet<Move> validMoves;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
							 PlayerConfiguration mrX, PlayerConfiguration firstDetective,
							 PlayerConfiguration... restOfTheDetectives) {
		//ensure rounds and graph cannot be null
		this.rounds = requireNonNull(rounds);
		this.graph = requireNonNull(graph);
		//ensure that rounds is not empty
		if (rounds.isEmpty()) {
			throw new IllegalArgumentException("Empty rounds");
		}
		//ensure that graph is not empty
		if (graph.isEmpty()) {
			throw new IllegalArgumentException("Empty graph");
		}
		//ensure that MrX is black
		if (mrX.colour != BLACK) { // or mr.colour.isDetective()
			throw new IllegalArgumentException("MrX should be Black");
		}
		//put players into temporary list so that we can perform checks
		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
		for (PlayerConfiguration configuration : restOfTheDetectives)
			configurations.add(requireNonNull(configuration));
		configurations.add(0, firstDetective);
		configurations.add(0, mrX);
		//iterate over list to check validity
		Set<Integer> set1 = new HashSet<>();
		Set<Colour> set2 = new HashSet<>();
		for (PlayerConfiguration configuration : configurations) {
			//check for location duplication
			if (set1.contains(configuration.location))
				throw new IllegalArgumentException("Duplicate location");
			set1.add(configuration.location);
			//check for colour duplication
			if (set2.contains(configuration.colour))
				throw new IllegalArgumentException("Duplicate colour");
			set2.add(configuration.colour);

			//check all ticket types exist
			if (!(configuration.tickets.containsKey(Ticket.TAXI) && configuration.tickets.containsKey(Ticket.BUS) && configuration.tickets.containsKey(Ticket.UNDERGROUND)
					&& configuration.tickets.containsKey(SECRET) && configuration.tickets.containsKey(DOUBLE)))
				throw new IllegalArgumentException("Missing ticket type");

			//check for detectives with secret or double tickets
			if (((configuration.tickets.get(SECRET) > 0) || (configuration.tickets.get(DOUBLE) > 0)) && (configuration.colour != BLACK))
				throw new IllegalArgumentException("Detective holds secret or double card");

			//create list of players
			ScotlandYardPlayer player = new ScotlandYardPlayer(configuration.player, configuration.colour,
					configuration.location, configuration.tickets);
			players.add(player);
		}
		//initialise current round as game not started and MrX location as unknown ie 0
		currentRound = NOT_STARTED;
		currentPlayer = players.get(0);
		mrXLocationActual = 0;
		mrXLocationDisplayed = 0;
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		//adds spectator provided to list of spectators if it is not null and if it has not already been registered
		if (spectator == null) {
			throw new NullPointerException("null spectator registered");
		}
		boolean alreadyRegistered = false;
		for (Spectator eg : spectators) {
			if (eg == spectator) {
				alreadyRegistered = true;
			}
		}
		if (alreadyRegistered == true) {
			throw new IllegalArgumentException("spectator already registered");
		}
		spectators.add(spectator);
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		//removes spectator provided from list of spectators if it is not null and the spectator is present in the list
		if (spectator == null) {
			throw new NullPointerException("null spectator unregistered");
		}
		boolean spectatorInSpectators = false;
		for (Spectator eg : spectators) {
			if (spectator == eg) {
				spectatorInSpectators = true;
			}
		}
		if (spectatorInSpectators == false) {
			throw new IllegalArgumentException("spectator not in list");
		}
		spectators.remove(spectator);
	}

	public HashSet<TicketMove> locationOverlapCheck(Edge<Integer, Transport> x, ScotlandYardPlayer eg, HashSet<TicketMove> moves, Ticket type) {
		//ensure that the destination of the move is not the same as any current detective location
		boolean locationOverlap = false;
		for (ScotlandYardPlayer otherPlayer : players) {
			//if possible move node destination is equal to a player location then move is not valid and will not be added to moves set
			//unless the player is black as black is able to return to original location and players can land on black
			if(otherPlayer.colour() != BLACK && otherPlayer.location() == x.destination().value()) {
				locationOverlap = true;
			}
		}
		if (!locationOverlap) {
			//if there is no location overlap but MrX can still move back to original location then move is added to moves set
			TicketMove validMove = new TicketMove(eg.colour(), type, x.destination().value());
			moves.add(validMove);
		}
		return moves;
	}

	public HashSet<TicketMove> singleTicketLogic(int secretTicketCount, int undergroundTicketCount, int taxiTicketCount,
												 int busTicketCount, Edge<Integer, Transport> x, ScotlandYardPlayer eg) {
		HashSet<TicketMove> moves = new HashSet<>();
		boolean locationOverlap = false;
		//test each ticket count is higher than zero then perform further logic to determine valid moves
		if (secretTicketCount > 0) {
			moves = locationOverlapCheck(x, eg, moves, Ticket.SECRET);
		}
		if (undergroundTicketCount > 0) {
			if (Ticket.fromTransport(x.data()) == Ticket.UNDERGROUND) {
				moves = locationOverlapCheck(x, eg, moves, Ticket.UNDERGROUND);
			}
		}
		if (taxiTicketCount > 0) {
			if (Ticket.fromTransport(x.data()) == Ticket.TAXI) {
				moves = locationOverlapCheck(x, eg, moves, Ticket.TAXI);
			}
		}
		if (busTicketCount > 0) {
			if (Ticket.fromTransport(x.data()) == Ticket.BUS) {
				moves = locationOverlapCheck(x, eg, moves, Ticket.BUS);
			}
		}
		return moves;
	}

	public void spectatorMove(Move move) {
		//notify spectators that given move has been made
		for(Spectator eg : spectators) {
			eg.onMoveMade(this, move);
		}
	}

	public void playerGameMove(TicketMove move) {
		//perform the movement of the player associated with the given move
		playingPlayer.location(move.destination());
		//update mrXLocationActual and mrXLocationDisplayed dependant on whether round is hidden or reaveal
		if(playingPlayer.colour() == BLACK) {
			mrXLocationActual = playingPlayer.location();
			if (rounds.get(getCurrentRound() - 1) == true) {
				mrXLocationDisplayed = mrXLocationActual;
			}
		}
	}

	public void ticketSwaps(TicketMove move) {
		//remove used tickets from player and if player is not MrX then the tickets are given to MrX
		Ticket usedTicket = move.ticket();
		playingPlayer.removeTicket(usedTicket);
		if(playingPlayer.colour() != BLACK) {
			players.get(0).addTicket(usedTicket);
		}
	}

	public void playerGameDoubleMove(DoubleMove move) {
		roundIncrement();
		playerGameMove(move.firstMove());
		TicketMove hidden1 = hideDestination(move.firstMove());
		roundIncrement();
		TicketMove hidden2 = hideDestination(move.secondMove());
		DoubleMove hidden = new DoubleMove(playingPlayer.colour(), hidden1, hidden2);
		roundDecrement();
		roundDecrement();
		playingPlayer.removeTicket(Ticket.DOUBLE);
		for(Spectator eg : spectators) {
			eg.onMoveMade(this, hidden);
		}
		ticketSwaps(move.firstMove());
		roundIncrement();
		spectateRoundStarted();
		spectatorMove(hidden1);
		ticketSwaps(move.secondMove());
		roundIncrement();
		playerGameMove(move.secondMove());
		spectateRoundStarted();
		spectatorMove(hidden2);
	}

	public TicketMove hideDestination(TicketMove move) {
		//hide the destination of the ticket if the player is MrX and the round is a hidden round
		TicketMove hiddenDestination;
		if (playingPlayer.colour() == BLACK) {
			if(rounds.get(getCurrentRound() - 1) == true) {
				hiddenDestination = move;
			} else {
				hiddenDestination = new TicketMove(move.colour(), move.ticket(), mrXLocationDisplayed);
			}
		} else {
			hiddenDestination = move;
		}
		return hiddenDestination;
	}

	public void roundIncrement() {
		//add one to the current round value
		if(currentRound == NOT_STARTED) {
			currentRound = 1;
		} else{
			currentRound = currentRound + 1;
		}
	}

	public void spectateRoundStarted() {
		//notify spectators that a new round has begun
		for(Spectator eg : spectators) {
			eg.onRoundStarted(this, currentRound);
		}
	}

	public void roundDecrement() {
		// remove one from the current round value
		if(currentRound == 1) {
			currentRound = NOT_STARTED;
		} else{
			currentRound = currentRound - 1;
		}
	}

	@Override
	public void accept(Move move) {
		//create a visitor that will be used to deal with the different ticket types
		MoveVisitor visitor = new MoveVisitor() {
			@Override
			public void visit(PassMove move) {
				//PassMoves only require to be spectated
				spectatorMove(move);
			}
			@Override
			public void visit(TicketMove move) {
				//round is incremented if the player is black
				if(playingPlayer.colour() == BLACK) {
					roundIncrement();
				}
				//the move is then made and all ticket swaps performed
				playerGameMove(move);
				ticketSwaps(move);
				//spectators are then notified if a new round has started
				if(playingPlayer.colour() == BLACK) {
					spectateRoundStarted();
				}
				//the destination of the move is hidden if required then spectators notified of move being made
				TicketMove hidden = hideDestination(move);
				spectatorMove(hidden);
			}
			@Override
			public void visit(DoubleMove move) {
				playerGameDoubleMove(move);
			}
		};
		//if the player has chosen a null move then throw an exception
		if (move == null) {
			throw new NullPointerException("null move");
		}
		if(!validMoves.contains(move)) {
			throw new IllegalArgumentException("IllegalMoveMade");
		}
		validMoves.clear();
		//if there is no next player then change the current player back to MrX, visit the move and start end of round actions
		if(!nextPlayer()) {
			currentPlayer = players.get(0);
			move.visit(visitor);
			endOfRound();
		} else {
			//if there is a next player then visit the move, if the game is over at this point then notify spectators of game over
			move.visit(visitor);
			if(isGameOver() == true && playingPlayer.colour() != BLACK) {
				for(Spectator eg : spectators) {
					eg.onGameOver(this, getWinningPlayers());
				}
			} else {
				//if game is not over then player next players turn
				playerTurn();
			}
		}
	}

	public void endOfRound() {
		//if game is not over then notify spectators of round complete otherwise notify spectators game is over
		if(!isGameOver()) {
			for (Spectator eg : spectators) {
				eg.onRotationComplete(this);
			}
		} else {
			for(Spectator eg : spectators) {
				eg.onGameOver(this, getWinningPlayers());
			}
		}
	}

	public boolean nextPlayer() {
		//determines if there is a next player, if so, playingPlayer is set to currentPlayer and currentPlayer is updated
		int i = players.indexOf(currentPlayer);
		if (i == players.size() - 1) {
			playingPlayer = currentPlayer;
			return false;
		} else {
			playingPlayer = currentPlayer;
			currentPlayer = players.get(i + 1);
			return true;
		}
	}

	public HashSet<Move> generateMoves(ScotlandYardPlayer player) {
		HashSet<TicketMove> moves = new HashSet<>();
		HashSet<Move> actualMoves = new HashSet<>();
		//create collection of all possible movement from the current node location
		Collection<Edge<Integer, Transport>> possibleMoves = graph.getEdgesFrom(graph.getNode(player.location()));
		//calculate types and numbers of tickets that they player holds
		int undergroundTicketCount = getPlayerTickets(player.colour(), Ticket.UNDERGROUND).get();
		int taxiTicketCount = getPlayerTickets(player.colour(), Ticket.TAXI).get();
		int busTicketCount = getPlayerTickets(player.colour(), Ticket.BUS).get();
		int secretTicketCount = getPlayerTickets(player.colour(), Ticket.SECRET).get();
		int doubleTicketCount = getPlayerTickets(player.colour(), Ticket.DOUBLE).get();
		//rotate through set of possible moves performing singleTicketLogic function to determine if move is valid
		for (Edge<Integer, Transport> x : possibleMoves) {
			moves.addAll(singleTicketLogic(secretTicketCount, undergroundTicketCount, taxiTicketCount, busTicketCount, x, player));
			//if valid then move from intermediate set containing types ticketMove to final 'actual' set containing types Move (therefore also allows passMoves to be contained within)
			for (TicketMove sample : moves) {
				Move actual = sample;
				actualMoves.add(actual);
			}
		}
		//if the player is MrX and he has double tickets and there are more than two rounds left in the game then double ticket logic is performed
			if ((player.colour() == BLACK) && (doubleTicketCount > 0) && (rounds.size() - currentRound >= 2)) {
				HashSet<TicketMove> secondMoves = new HashSet<>();
				//rotate through moves in current moves set
				for (TicketMove firstMove : moves) {
					//calculate possible second moves from first moves
					Collection<Edge<Integer, Transport>> secondPossibleMoves = graph.getEdgesFrom(graph.getNode(firstMove.destination()));
					//reduce the ticket count of the correct first move ticket type
					if (firstMove.ticket() == Ticket.SECRET) {
						secretTicketCount = secretTicketCount - 1;
					}
					if (firstMove.ticket() == Ticket.UNDERGROUND) {
						undergroundTicketCount = undergroundTicketCount - 1;
					}
					if (firstMove.ticket() == Ticket.TAXI) {
						taxiTicketCount = taxiTicketCount - 1;
					}
					if (firstMove.ticket() == Ticket.BUS) {
						busTicketCount = busTicketCount - 1;
					}
					//iterate through possible second moves and determine if they are valid
					for (Edge<Integer, Transport> secondPossibleMove : secondPossibleMoves) {
						secondMoves = singleTicketLogic(secretTicketCount, undergroundTicketCount, taxiTicketCount, busTicketCount, secondPossibleMove, player);
						//if valid then move from intermediate set containing types ticketMove to final 'actual' set containing types Move (therefore also allows passMove to be contained within)
						for (TicketMove secondMove : secondMoves) {
							Move doubleMove = new DoubleMove(player.colour(), firstMove, secondMove);
							actualMoves.add(doubleMove);
						}
					}
					//return ticket counts to normal as this is only a hypothetical situation and player has not chosen to move yet
					if (firstMove.ticket() == Ticket.SECRET) {
						secretTicketCount = secretTicketCount + 1;
					}
					if (firstMove.ticket() == Ticket.UNDERGROUND) {
						undergroundTicketCount = undergroundTicketCount + 1;
					}
					if (firstMove.ticket() == Ticket.TAXI) {
						taxiTicketCount = taxiTicketCount + 1;
					}
					if (firstMove.ticket() == Ticket.BUS) {
						busTicketCount = busTicketCount + 1;
					}
				}
			}
		return actualMoves;
	}

	public void playerTurn() {
		//generated a set of valid moves, including PassMove if needed then prompts player to move
		validMoves = generateMoves(currentPlayer);
		if (currentPlayer.colour() != BLACK && validMoves.isEmpty()) {
			Move passMove = new PassMove(currentPlayer.colour());
			validMoves.add(passMove);
		}
		currentPlayer.player().makeMove(this, currentPlayer.location(), validMoves, this);
	}

	@Override
	public void startRotate() {
		//if game is already over throw an exception
		if (currentRound == NOT_STARTED) {
			if(isGameOver()) {
				throw new IllegalStateException("game already over");
			}
		}
		//set current player to MrX and then play his turn
		for (ScotlandYardPlayer eg : players) {
			if (eg.colour() == BLACK) {
				currentPlayer = eg;
				playingPlayer = eg;
			}
		}
		playerTurn();
	}

	@Override
	public Collection<Spectator> getSpectators() {
		//return unmodifiable collection of all the spectators
		return Collections.unmodifiableCollection(spectators);
	}

	@Override
	public List<Colour> getPlayers() {
		//returns unmodifiable list of all player colours
		List<Colour> playerColour = new ArrayList<>();
		for (ScotlandYardPlayer eg : players) {
			playerColour.add(eg.colour());
		}
		return Collections.unmodifiableList(playerColour);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		//returns unmodifiable set of winning players, either MrX or all the detectives
		Set<Colour> winners = new HashSet<>();
		if (isGameOver() == false) {
			return Collections.unmodifiableSet(winners);
		}
		if (mrXWin == false) {
			for (ScotlandYardPlayer eg : players) {
				if (eg.colour() != BLACK) {
					winners.add(eg.colour());
				}
			}
		} else {
			winners.add(BLACK);
		}
		return Collections.unmodifiableSet(winners);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		//returns Optional of given players location
		Optional<Integer> location = Optional.empty();
		if (colour == BLACK) {
			//if player is MrX then return his displayed location
			if (currentRound == NOT_STARTED) {
				return location = Optional.of(0);
			}
			return location = Optional.of(mrXLocationDisplayed);
		} else {
			for (ScotlandYardPlayer eg : players) {
				if (eg.colour() == colour) {
					location = Optional.of(eg.location());
				}
			}
		}
		return location;
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		//returns Optional of number of tickets of the given type that the player of the given colour currently has
		Optional<Integer> n = Optional.empty();
		Map<Ticket, Integer> givenPlayerTickets = Collections.emptyMap();
		for (ScotlandYardPlayer givenPlayer : players) {
			if (givenPlayer.colour() == colour)
				givenPlayerTickets = givenPlayer.tickets();
			n = Optional.ofNullable(givenPlayerTickets.get(ticket));
		}
		return n;
	}

	@Override
	public boolean isGameOver() {
		//determines if the current game state means game is over and returns a boolean
		boolean playerMovesLeft = false;
		//if the rounds have run out then game is over and MrX has won
		if (rounds.size() == currentRound && players.size() - 1 == players.indexOf(playingPlayer)) {
			mrXWin = true;
			return true;
		}
		for(ScotlandYardPlayer eg : players) {
			HashSet<Move> moves = generateMoves(eg);
			if (eg.colour() == BLACK) {
				//if MrX has no valid moves then game is over and detectives have won
				if(moves.isEmpty() && currentPlayer.colour() == BLACK) {
					mrXWin = false;
					return true;
				}
			} else {
				if (!(moves.isEmpty())) {
					playerMovesLeft = true;
				}
				//if a detective occupies the same location as MrX then game is over and detectives have won
				if (eg.location() == mrXLocationActual) {
					mrXWin = false;
					return true;
				}
			}
		}
		//if no players have any moves left then game is over and MrX has won
		if(playerMovesLeft == false){
			mrXWin = true;
			return true;
		}
		return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		//returns colour of the current player
		return currentPlayer.colour();
	}

	@Override
	public int getCurrentRound() {
		//returns an integer representing the current round
		return currentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		//returns an unmodifiable list of all the rounds
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		//returns an immutable graph representing the game board
		return new ImmutableGraph<Integer, Transport>(graph);
	}
}