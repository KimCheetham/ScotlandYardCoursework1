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

// TODO implement all methods and pass all tests
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
	int mrXLocationDisplayed;
	int mrXLocationActual;
	int intermediateLocation;
	boolean mrXWin;
	boolean gameOver;
	//Spectator spectator;


	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {
		// TODO
		//ensure rounds and graph cannot be null
		this.rounds = requireNonNull(rounds);
		this.graph = requireNonNull(graph);
		//ensure that rounds is not empty
		if (rounds.isEmpty()) {
			throw new IllegalArgumentException("Empty rounds");
		}
		//ensure that graph is not empty
		if(graph.isEmpty()) {
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
		//List<ScotlandYardPlayer> interim = new ArrayList<>();
		for (PlayerConfiguration configuration : configurations) {
			//check for location duplication
			if (set1.contains(configuration.location))
				throw new IllegalArgumentException("Duplicate location");
			set1.add(configuration.location);
			//check for colour duplication
			if(set2.contains(configuration.colour))
				throw new IllegalArgumentException("Duplicate colour");
			set2.add(configuration.colour);

			//check all ticket types exist
			if(!(configuration.tickets.containsKey(Ticket.TAXI) && configuration.tickets.containsKey(Ticket.BUS) && configuration.tickets.containsKey(Ticket.UNDERGROUND)
					&& configuration.tickets.containsKey(SECRET) && configuration.tickets.containsKey(DOUBLE)))
				throw new IllegalArgumentException("Missing ticket type");

			//check for detectives with secret or double tickets
			if(((configuration.tickets.get(SECRET)>0) || (configuration.tickets.get(DOUBLE)>0)) && (configuration.colour != BLACK))
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
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	//extra function to help with valid move logic
	public HashSet<TicketMove> singleTicketLogic(int secretTicketCount, int undergroundTicketCount, int taxiTicketCount,
												 int busTicketCount, Edge<Integer, Transport> x, ScotlandYardPlayer eg) {
		HashSet<TicketMove> moves = new HashSet<>();
		boolean locationOverlap = false;
		//List<ScotlandYardPlayer> withoutMrX = players;
		//withoutMrX.remove(0);
		//test each ticket count is higher then zero then perform further logic to determine valid moves
		if(secretTicketCount > 0) {
			for(ScotlandYardPlayer egg : players) {
				//first clause of if: if possible move node destination is equal to a player location then move is not valid and will not be added to moves set
				//second clause of if: allows Mr X to move back to his original square when completing a double move
				if((egg.colour() != BLACK && egg.location() == x.destination().value()) && !(x.destination().value() == eg.location())) {
					locationOverlap = true;
				}
			}
			if(!locationOverlap) {
				//if there is no location overlap but MrX can still move back to original location then move is added to moves set
				TicketMove validMove = new TicketMove(eg.colour(), Ticket.SECRET, x.destination().value());
				moves.add(validMove);
			}
		}
		if(undergroundTicketCount > 0) {
			if (Ticket.fromTransport(x.data()) == Ticket.UNDERGROUND) {
				for (ScotlandYardPlayer egg : players) {
					if ((egg.colour()!=BLACK && egg.location() == x.destination().value()) && !(x.destination().value() == eg.location())) {
						locationOverlap = true;
					}
				}
				if(!locationOverlap) {
					TicketMove validMove = new TicketMove(eg.colour(), Ticket.UNDERGROUND, x.destination().value());
					moves.add(validMove);
				}
			}
		}
		if(taxiTicketCount > 0) {
			if(Ticket.fromTransport(x.data()) == Ticket.TAXI) {
				for(ScotlandYardPlayer egg: players) {
					if((egg.colour()!=BLACK && egg.location() == x.destination().value()) && !(x.destination().value() == eg.location())) {
						locationOverlap = true;
					}
				}
				if(!locationOverlap) {
					TicketMove validMove = new TicketMove(eg.colour(), Ticket.TAXI, x.destination().value());
					moves.add(validMove);
				}
			}
		}
		if(busTicketCount > 0) {
			if(Ticket.fromTransport(x.data()) == Ticket.BUS) {
				for(ScotlandYardPlayer egg : players) {
					if((egg.colour() != BLACK && egg.location() == x.destination().value()) && !(x.destination().value() == eg.location())) {
						locationOverlap = true;
					}
				}
				if(!locationOverlap) {
					TicketMove validMove = new TicketMove(eg.colour(), Ticket.BUS, x.destination().value());
					moves.add(validMove);
				}
			}
		}
		return moves;
	}

	public void spectatorMove(Move move) {
		//ScotlandYardView view = this;
		//spectator.onMoveMade(view, move);
	}

	public void playerGameMove(TicketMove move) {
		Ticket usedTicket = move.ticket();
		currentPlayer.removeTicket(usedTicket);
		currentPlayer.location(move.destination());
		if(getCurrentPlayer() != BLACK) {
			players.get(0).addTicket(usedTicket);
			//currentPlayer.location(move.destination());
		}
		else {
			mrXLocationActual = currentPlayer.location();
		}
	}

	public void playerGameDoubleMove(DoubleMove move) {
		playerGameMove(move.firstMove());
		playerGameMove(move.secondMove());
		currentPlayer.removeTicket(DOUBLE);
	}

	@Override
	public void accept(Move move) {
		MoveVisitor visitor = new MoveVisitor() {
			@Override
			public void visit(PassMove move) {
				//spectatorMove(move);
			}
			@Override
			public void visit(TicketMove move) {
				playerGameMove(move);
			}
			@Override
			public void visit(DoubleMove move) {
				playerGameDoubleMove(move);
			}
		};
		if(move == null) {
			throw new NullPointerException("null move");
		}
		move.visit(visitor);
	}

	public HashSet<Move> generateMoves(ScotlandYardPlayer player) {
		HashSet<TicketMove> moves = new HashSet<>();
		HashSet<Move> actualMoves = new HashSet<>();
			//create collection of all possible movement from the current node location
			Collection<Edge<Integer, Transport>> possibleMoves = graph.getEdgesFrom(graph.getNode(currentPlayer.location()));
			//calculate types and numbers of tickets that they player holds
			int undergroundTicketCount = getPlayerTickets(currentPlayer.colour(), Ticket.UNDERGROUND).get();
			int taxiTicketCount = getPlayerTickets(currentPlayer.colour(), Ticket.TAXI).get();
			int busTicketCount = getPlayerTickets(currentPlayer.colour(), Ticket.BUS).get();
			int secretTicketCount = getPlayerTickets(currentPlayer.colour(), Ticket.SECRET).get();
			int doubleTicketCount = getPlayerTickets(currentPlayer.colour(), Ticket.DOUBLE).get();
			//rotate through set of possible moves performing singleTicketLogic function to determine if move is valid
			for (Edge<Integer, Transport> x : possibleMoves) {
				moves = singleTicketLogic(secretTicketCount, undergroundTicketCount, taxiTicketCount, busTicketCount, x, currentPlayer);
				//if valid then move from intermediate set containing types ticketMove to final 'actual' set containing types Move (therefore also allows passMoves to be contained within)
				for (TicketMove sample : moves) {
					Move actual = sample;
					actualMoves.add(actual);
				}
				//if the player is MrX and he has double tickets and there are more than two rounds left in the game then double ticket logic is performed
				if ((currentPlayer.colour() == BLACK) && (doubleTicketCount > 0) && (getRounds().size() >= 2)) {
					HashSet<TicketMove> secondMoves = new HashSet<>();
					//rotate through moves in current moves set
					for (TicketMove firstMove : moves) {
						//calculate possible second moves from first moves
						Collection<Edge<Integer, Transport>> secondPossibleMoves = graph.getEdgesFrom(graph.getNode(x.destination().value()));
						//reduce the ticket count of the correct first move ticket type
						if (Ticket.fromTransport(x.data()) == Ticket.SECRET) {
							secretTicketCount = secretTicketCount - 1;
						}
						if (Ticket.fromTransport(x.data()) == Ticket.UNDERGROUND) {
							undergroundTicketCount = undergroundTicketCount - 1;
						}
						if (Ticket.fromTransport(x.data()) == Ticket.TAXI) {
							taxiTicketCount = taxiTicketCount - 1;
						}
						if (Ticket.fromTransport(x.data()) == Ticket.BUS) {
							busTicketCount = busTicketCount - 1;
						}
						//iterate through possible second moves and determine if they are valid
						for (Edge<Integer, Transport> secondPossibleMove : secondPossibleMoves) {
							secondMoves = singleTicketLogic(secretTicketCount, undergroundTicketCount, taxiTicketCount, busTicketCount, secondPossibleMove, currentPlayer);
							//if valid then move from intermediate set containing types ticketMove to final 'actual' set containing types Move (therefore also allows passMove to be contained within)
							for (TicketMove secondMove : secondMoves) {
								Move doubleMove = new DoubleMove(currentPlayer.colour(), firstMove, secondMove);
								actualMoves.add(doubleMove);
							}
						}
						//return ticket counts to normal as this is only a hypothetical situation and player has not chosen to move yet
						if (Ticket.fromTransport(x.data()) == Ticket.SECRET) {
							secretTicketCount = secretTicketCount + 1;
						}
						if (Ticket.fromTransport(x.data()) == Ticket.UNDERGROUND) {
							undergroundTicketCount = undergroundTicketCount + 1;
						}
						if (Ticket.fromTransport(x.data()) == Ticket.TAXI) {
							taxiTicketCount = taxiTicketCount + 1;
						}
						if (Ticket.fromTransport(x.data()) == Ticket.BUS) {
							busTicketCount = busTicketCount + 1;
						}
					}
				}
			}
			//if there are no possible valid moves then passMove is added to the 'actual' valid move set
			/*if (player.colour() != BLACK && actualMoves.isEmpty()) {
				Move passMove = new PassMove(currentPlayer.colour());
				actualMoves.add(passMove);
			}*/
		return actualMoves;
	}

	public void playerTurn() {
		HashSet<Move> moves = generateMoves(currentPlayer);
		if (currentPlayer.colour() != BLACK && moves.isEmpty()) {
			Move passMove = new PassMove(currentPlayer.colour());
			moves.add(passMove);
		}
		currentPlayer.player().makeMove(this, currentPlayer.location(), moves, this);
	}

	@Override
	public void startRotate(){
		if(currentRound == NOT_STARTED) {
			//if(isGameOver()) {
			//	throw new IllegalStateException("game already over");
			//}
			currentRound = 1;
		}
		else{
			currentRound = currentRound + 1;
		}
		for(ScotlandYardPlayer eg: players) {
			currentPlayer = eg;
			playerTurn();
		}
	}

	@Override
	public Collection<Spectator> getSpectators() {
		//return Collections.unmodifiableCollection(spectators);
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		//returns unmodifiable
		List<Colour> playerColour = new ArrayList<>();
		for(ScotlandYardPlayer eg : players) {
				playerColour.add(eg.colour());
		}
		return Collections.unmodifiableList(playerColour);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		Set<Colour> winners = new HashSet<>();
		if(isGameOver() == false) {
			return Collections.unmodifiableSet(winners);
		}
		if(mrXWin == false) {
			for(ScotlandYardPlayer eg : players) {
				if(eg.colour() != BLACK) {
					winners.add(eg.colour());
				}
			}
		}
		else {
			winners.add(BLACK);
			//return Collections.unmodifiableSet(winners);
		}
		return Collections.unmodifiableSet(winners);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		Optional<Integer> location = Optional.empty();
		if(colour == BLACK) {
			if(currentRound == NOT_STARTED){
				return location = Optional.of(0);
			}
			else if (rounds.get(getCurrentRound() - 1) == true) {
				mrXLocationDisplayed = mrXLocationActual;
			}
			return location = Optional.of(mrXLocationDisplayed);
		}
		else {
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
		Optional<Integer> n = Optional.empty();
		Map<Ticket, Integer> givenPlayerTickets = Collections.emptyMap();
		for(ScotlandYardPlayer givenPlayer : players) {
			if(givenPlayer.colour() == colour)
				givenPlayerTickets = givenPlayer.tickets();
				n = Optional.ofNullable(givenPlayerTickets.get(ticket));
		}
		return n;
	}

	@Override
	public boolean isGameOver() {
		boolean playerMovesLeft = false;
		if(rounds.size() == currentRound) {
			mrXWin = true;
			return true;
		}
		for(ScotlandYardPlayer eg : players) {
			HashSet<Move> moves = generateMoves(eg);
			if(eg.colour() == BLACK && (!(eg.hasTickets(Ticket.UNDERGROUND)) && !(eg.hasTickets(Ticket.BUS)) && !(eg.hasTickets(Ticket.TAXI)) &&(eg.hasTickets(Ticket.SECRET)) && moves.isEmpty())) {
			//if(eg.colour() == BLACK) {

				//if(moves.isEmpty()) {
					mrXWin = false;
					return true;
				//}
			}

			if(eg.colour() != BLACK) {
				//HashSet<Move> moves = generateMoves(eg);
				if(!(moves.isEmpty())) {
					playerMovesLeft = true;
				}
			}
			if(eg.colour() != BLACK && !(eg.hasTickets(Ticket.UNDERGROUND)) && !(eg.hasTickets(Ticket.BUS)) && !(eg.hasTickets(Ticket.TAXI))) {
				mrXWin = true;
				return true;
			}
			if(eg.colour() != BLACK && eg.location() == mrXLocationActual) {
				mrXWin = false;
				return true;
			}
		}
		/*if (playerMovesLeft = false){
			mrXWin = true;
			return true;
		}*/
		return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		//ScotlandYardPlayer currentPlayer;
		//currentPlayer = players.get(0);
		//if(currentRound == NOT_STARTED)
		return currentPlayer.colour();

	}

	@Override
	public int getCurrentRound() {
		return currentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<Integer, Transport>(graph);
	}

}
