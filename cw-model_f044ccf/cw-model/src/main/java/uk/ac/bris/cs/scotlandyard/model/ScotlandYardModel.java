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
public class ScotlandYardModel implements ScotlandYardGame {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private PlayerConfiguration mrX;
	private PlayerConfiguration firstDetective;
	private PlayerConfiguration restOfTheDetectives;
	public List<ScotlandYardPlayer> players = new ArrayList<>();
	public List<Spectator> spectators = new ArrayList<>();
	int currentRound;
	int mrXLocation;


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
		mrXLocation = 0;
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
		//Spectator eg = new Spectator();
		//spectators.add();
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		Set<Move> moves = new HashSet<>();
		Consumer<Move> consumerMove = a -> {
			if(a == null) {
				throw new NullPointerException("Null move made");
			}
			for(Move eg : moves) {
				if(a != eg) {
					throw new NullPointerException("Non-valid move made");
				}
			}
		};
		Player test = null;
		for(ScotlandYardPlayer eg : players) {
			//Move sample = new PassMove(eg.colour());
			//moves.add(sample);
			//Integer x = getPlayerTickets(eg.colour(), Ticket.UNDERGROUND);
			//Node x = new Node(eg.location());
			Collection<Edge<Integer, Transport>> possibleMoves = graph.getEdgesFrom(graph.getNode(eg.location()));
			int undergroundTicketCount = getPlayerTickets(eg.colour(), Ticket.UNDERGROUND).get();
			for(Edge<Integer, Transport> x : possibleMoves) {
				if(undergroundTicketCount > 0) {
					if(x.data() == Transport UNDERGROUND) {
						//Graph<Integer, Transport> floob = new Graph();
						//floob.addNode(x.destination());

						int destination = x.destination.value();
						Move validMove = new TicketMove(eg.colour(), Ticket.UNDERGROUND, destination);
						moves.add(validMove);
					}
				}
			}
			test = eg.player();
			test.makeMove(this, eg.location(), moves, consumerMove);
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
		Set<Colour> empty = new HashSet<>();
		return Collections.unmodifiableSet(empty);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		Optional<Integer> detectiveLocation = Optional.empty();
		if(colour == BLACK) {
			if (rounds.get(currentRound) == true) {
				for (ScotlandYardPlayer eg : players) {
					if (eg.colour() == colour) {
						mrXLocation = eg.location();
					}
				}
			}
			return Optional.of(mrXLocation);
		}
		else {
			for (ScotlandYardPlayer eg : players) {
				if (eg.colour() == colour) {
					detectiveLocation = Optional.of(eg.location());
				}
			}
		}
		return detectiveLocation;
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
		if(rounds.size() > 1) {return false;}
		return true;
	}

	@Override
	public Colour getCurrentPlayer() {
		ScotlandYardPlayer currentPlayer;
		currentPlayer = players.get(0);
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
